package com.spotvault.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters

const val AUTO_PARK_VEHICLE_ID_KEY = "auto_park_vehicle_id"
const val AUTO_PARK_MAC_KEY = "auto_park_mac"
const val AUTO_PARK_CACHED_LAT_KEY = "auto_park_cached_lat"
const val AUTO_PARK_CACHED_LNG_KEY = "auto_park_cached_lng"

/** Reported in every doWork() outcome (real disconnect or manual test alike) via output Data —
 * the real trigger has nobody watching it, but "Test Auto-Park Now" does, and its whole point is
 * telling the user WHY nothing showed up in the Vault instead of a blind "queued" toast that
 * looks identical whether the save succeeded or silently no-opped for any of half a dozen reasons. */
const val AUTO_PARK_OUTCOME_KEY = "auto_park_outcome"
const val AUTO_PARK_OUTCOME_SAVED = "saved"
const val AUTO_PARK_OUTCOME_DISABLED = "disabled"
const val AUTO_PARK_OUTCOME_NO_BACKGROUND_LOCATION = "no_background_location"
const val AUTO_PARK_OUTCOME_NO_VEHICLE = "no_vehicle"
const val AUTO_PARK_OUTCOME_VEHICLE_ARCHIVED = "vehicle_archived"
const val AUTO_PARK_OUTCOME_QUIET_ZONE_SKIP = "quiet_zone_skip"
const val AUTO_PARK_OUTCOME_STILL_CONNECTED = "still_connected"
const val AUTO_PARK_OUTCOME_FAILED = "failed"

// Fully qualified, not a plain "Result" — at file (non-class) scope that resolves to
// kotlin.Result<T> instead of androidx.work.ListenableWorker.Result, which doWork() itself
// only gets to use unqualified because CoroutineWorker's class scope shadows the stdlib one.
private fun outcomeResult(outcome: String): androidx.work.ListenableWorker.Result =
    androidx.work.ListenableWorker.Result.success(Data.Builder().putString(AUTO_PARK_OUTCOME_KEY, outcome).build())

private const val UNIQUE_WORK_NAME_PREFIX = "auto_park_bluetooth_disconnect"

/** Past this many attempts, doWork() stops retrying a total GPS failure and saves with the
 * "No GPS signal" placeholder instead — retrying forever (WorkManager's default backoff climbs to
 * every ~5h) risked a save finally landing hours later, potentially miles from where the car
 * actually is by the time it succeeds. WorkManager's own backoff means 3 attempts land within
 * about a minute of each other, so this fails fast rather than lingering. */
private const val MAX_LOCATION_RETRY_ATTEMPTS = 3

/** Marks [spotId] as the most recent auto-park save for [mac] — read back by
 * [AutoParkReconnectCleanupWorker] the moment that same vehicle reconnects. No-op without a MAC
 * (the manual "Test Auto-Park Now" trigger has no disconnect/reconnect cycle to correlate
 * against, so its save is never subject to this cleanup). */
private fun markPendingAutoParkSpot(prefs: SharedPreferences, mac: String?, spotId: Int) {
    if (mac == null) return
    prefs.edit().putInt(AUTO_PARK_PENDING_SPOT_PREF_PREFIX + mac.uppercase(), spotId).apply()
}

/**
 * Performs GPS fix, quiet-zone checks, and [quickActiveTrackPin] after a car BT disconnect.
 *
 * Uses expedited WorkManager where available (API 31+) so OEM background limits are less
 * likely to defer the job — Samsung/Xiaomi/etc. may still kill work unless battery
 * optimization is disabled (see Automatic Parking settings).
 *
 * All database access (including resolving which vehicle the disconnected MAC belongs to)
 * happens here rather than in [CarBluetoothReceiver] — WorkManager gives this a real execution
 * budget, unlike a BroadcastReceiver's main-thread onReceive().
 */
class AutoParkWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        if (!isAutoParkEnabled(prefs)) return outcomeResult(AUTO_PARK_OUTCOME_DISABLED)
        // Defensive, not just the earlier check in CarBluetoothReceiver — this worker is also
        // reachable via enqueueAutoParkWorkForVehicle (the "Test Auto-Park Now" button), and
        // WorkManager can run this well after enqueue, by which point the app may no longer have
        // any foreground presence. Result.success() rather than retry() — missing permission
        // won't fix itself, so retrying is just wasted battery for a guaranteed-to-fail attempt.
        if (!hasBackgroundLocationPermission(context)) return outcomeResult(AUTO_PARK_OUTCOME_NO_BACKGROUND_LOCATION)

        val disconnectedMac = inputData.getString(AUTO_PARK_MAC_KEY)
        val vehicleId = inputData.getInt(AUTO_PARK_VEHICLE_ID_KEY, -1).takeIf { it > 0 }
        val db = AppDatabase.getDatabase(context.applicationContext)
        val vehicleDao = db.vehicleDao()
        migrateLegacyCarVehicleIfNeeded(context.applicationContext, prefs, vehicleDao)

        val vehicle = when {
            vehicleId != null -> vehicleDao.getById(vehicleId)
            disconnectedMac != null -> {
                vehicleDao.findByBluetoothMac(disconnectedMac)
                    ?: legacyVehicleFromPrefs(prefs, disconnectedMac)
            }
            else -> {
                val legacyMac = loadAutoParkCarMac(prefs) ?: return outcomeResult(AUTO_PARK_OUTCOME_NO_VEHICLE)
                vehicleDao.findByBluetoothMac(legacyMac) ?: legacyVehicleFromPrefs(prefs, legacyMac)
            }
        } ?: return outcomeResult(AUTO_PARK_OUTCOME_NO_VEHICLE)

        if (vehicle.isArchived) return outcomeResult(AUTO_PARK_OUTCOME_VEHICLE_ARCHIVED)

        val cachedLat = inputData.getDouble(AUTO_PARK_CACHED_LAT_KEY, Double.NaN)
        val cachedLng = inputData.getDouble(AUTO_PARK_CACHED_LNG_KEY, Double.NaN)
        val location = if (!cachedLat.isNaN() && !cachedLng.isNaN()) {
            cachedLat to cachedLng
        } else {
            resolveCurrentLocation(context, prefs) ?: run {
                // Never fall through to (0,0) — that used to write a real Vault pin in the Gulf
                // of Guinea whenever GPS stayed unavailable after retries.
                if (runAttemptCount < MAX_LOCATION_RETRY_ATTEMPTS) return Result.retry()
                return outcomeResult(AUTO_PARK_OUTCOME_FAILED)
            }
        }

        val decision = evaluateAutoParkQuietZones(
            lat = location.first,
            lng = location.second,
            zones = loadAutoParkQuietZones(prefs)
        )
        if (decision == AutoParkQuietZoneDecision.SKIP) {
            return outcomeResult(AUTO_PARK_OUTCOME_QUIET_ZONE_SKIP)
        }

        // Re-checked here, this late, not just at the top of doWork() — both of these can go
        // stale during the multi-second GPS fetch above. isAutoParkEnabled could have been
        // switched off from Settings while this save was mid-flight. isMacCurrentlyConnected
        // covers the disconnect→reconnect race specifically: CarBluetoothReceiver already calls
        // cancelAutoParkWork() on reconnect, but WorkManager cancellation isn't guaranteed to
        // stop an already-running, non-cooperative suspend chain in time (cancelAutoParkWork's
        // own doc already acknowledged this as a known gap) — without this check, a user who got
        // back in the car within a few seconds could still get a save + a live Active Tracking
        // notification for a car they never actually left.
        if (!isAutoParkEnabled(prefs)) return outcomeResult(AUTO_PARK_OUTCOME_DISABLED)
        if (disconnectedMac != null && isMacCurrentlyConnected(prefs, disconnectedMac)) return outcomeResult(AUTO_PARK_OUTCOME_STILL_CONNECTED)

        val dao = db.locationDao()
        val option = autoParkOptionForVehicle(vehicle)

        // "Auto-Park Mode" in Settings — Quiet Save skips the live Active Tracking notification
        // entirely, for anyone whose car disconnects often enough that a sticky notification every
        // time became its own nuisance. Short-circuits before the SAVE_SILENT quiet-zone flag
        // below on purpose: that flag only exists to mute the Active Tracking path, which this
        // mode never takes, so there's nothing for it to do here.
        if (isAutoParkQuietSaveMode(prefs)) {
            return when (val result = quietSaveTacticalPin(context.applicationContext, dao, prefs, option, vehicleId = vehicle.id, resolvedLocation = location)) {
                is QuietSaveResult.Saved -> {
                    markPendingAutoParkSpot(prefs, disconnectedMac, result.spotId)
                    outcomeResult(AUTO_PARK_OUTCOME_SAVED)
                }
                else -> outcomeResult(AUTO_PARK_OUTCOME_FAILED)
            }
        }

        if (decision == AutoParkQuietZoneDecision.SAVE_SILENT) {
            prefs.edit().putBoolean(PINNED_NOTIFICATION_SILENT_PREF, true).apply()
        }

        return when (
            val result = quickActiveTrackPin(
                context = context.applicationContext,
                dao = dao,
                prefs = prefs,
                option = option,
                vehicleId = vehicle.id,
                resolvedLocation = location
            )
        ) {
            is QuietSaveResult.Saved -> {
                markPendingAutoParkSpot(prefs, disconnectedMac, result.spotId)
                outcomeResult(AUTO_PARK_OUTCOME_SAVED)
            }
            QuietSaveResult.Failed -> {
                // Nothing will ever start TimerService for this attempt — it's the only place
                // that reads and clears PINNED_NOTIFICATION_SILENT_PREF (set above for a
                // SAVE_SILENT quiet zone), so leaving it set here would silently mute the next,
                // completely unrelated active-tracking notification instead of just this one.
                prefs.edit().remove(PINNED_NOTIFICATION_SILENT_PREF).apply()
                outcomeResult(AUTO_PARK_OUTCOME_FAILED)
            }
            QuietSaveResult.NeedsNotificationPermission -> {
                // There's no UI here to prompt for POST_NOTIFICATIONS — falling back to a quiet
                // save means the pin still lands in the vault (just without the persistent
                // tracking notification, which needs that permission anyway) instead of this
                // auto-park silently saving nothing every time the permission is denied.
                val fallback = quietSaveTacticalPin(context.applicationContext, dao, prefs, option, vehicleId = vehicle.id, resolvedLocation = location)
                // quietSaveTacticalPin never starts TimerService either (it only ever stops one,
                // if one was already running) — same orphaned-flag risk as the Failed case above.
                prefs.edit().remove(PINNED_NOTIFICATION_SILENT_PREF).apply()
                when (fallback) {
                    is QuietSaveResult.Saved -> {
                        markPendingAutoParkSpot(prefs, disconnectedMac, fallback.spotId)
                        outcomeResult(AUTO_PARK_OUTCOME_SAVED)
                    }
                    else -> outcomeResult(AUTO_PARK_OUTCOME_FAILED)
                }
            }
        }
    }
}

/** Preferred entry point — used by [CarBluetoothReceiver], which only knows the MAC address
 * that just disconnected (no database access happens on that receiver's main thread).
 *
 * [cachedLocation], when supplied, comes from [consumeValidMotionBookmark] — a location fix
 * captured earlier by Motion & Fitness detection, already validated as fresh and belonging to
 * this exact vehicle. Omitting it (the default) falls back to a live GPS fetch inside
 * [AutoParkWorker.doWork], which is exactly what always happens when Motion & Fitness is off. */
fun enqueueAutoParkWork(context: Context, mac: String, cachedLocation: Pair<Double, Double>? = null) {
    val dataBuilder = Data.Builder().putString(AUTO_PARK_MAC_KEY, mac)
    cachedLocation?.let { (lat, lng) ->
        dataBuilder.putDouble(AUTO_PARK_CACHED_LAT_KEY, lat)
        dataBuilder.putDouble(AUTO_PARK_CACHED_LNG_KEY, lng)
    }
    val request = OneTimeWorkRequestBuilder<AutoParkWorker>()
        .setInputData(dataBuilder.build())
        .apply {
            // See MotionBookmarkWorker's enqueue for why this is gated — expedited work needs
            // getForegroundInfo() overridden pre-S, which this worker doesn't do.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }
        }
        .build()
    // Scoped per-MAC rather than one shared name — CarBluetoothReceiver enqueues this for every
    // Bluetooth device (headphones, watches, anything), not just linked vehicles. With a single
    // shared name and REPLACE, an unrelated device disconnecting moments after the car would
    // cancel the car's still-running auto-park (mid GPS fetch) and replace it with a job for a
    // MAC that matches no vehicle — silently dropping the real save.
    WorkManager.getInstance(context).enqueueUniqueWork(
        "${UNIQUE_WORK_NAME_PREFIX}_${mac.uppercase()}",
        ExistingWorkPolicy.REPLACE,
        request
    )
}

/** Cancels a pending/in-flight auto-park save for [mac] — called when that device reconnects
 * shortly after disconnecting (e.g. the user got back in the car), so a stale save isn't still
 * running or queued from the disconnect that no longer reflects reality. A no-op if the worker
 * already finished, since [enqueueAutoParkWork]'s save itself doesn't check connection state. */
fun cancelAutoParkWork(context: Context, mac: String) {
    WorkManager.getInstance(context).cancelUniqueWork("${UNIQUE_WORK_NAME_PREFIX}_${mac.uppercase()}")
}

/** Past this many attempts, a cleanup that still hasn't found a pending spot gives up rather than
 * retrying forever — see the "wallet bounce" reasoning in [AutoParkReconnectCleanupWorker.doWork]. */
private const val MAX_CLEANUP_RETRY_ATTEMPTS = 2

/** Cleans up an already-*finished* auto-park save for [mac] the moment its vehicle reconnects —
 * [cancelAutoParkWork] above only stops a save that's still pending/running, so it does nothing
 * for the far more common "gas station" case: engine off for a couple minutes (long enough for
 * the save to complete), then back on. Reconnecting is proof the user got back in the car without
 * ever needing to "find" it, meaning that save was just an intermediate engine-stop, not where
 * the car actually ended up parked — so this discards it, leaving only the save from whichever
 * disconnect turns out to be the last one before the vehicle stays gone. Runs as WorkManager, not
 * inline in [CarBluetoothReceiver]'s onReceive(), for the same reason every other DB-touching step
 * here does. Soft-deletes (lands in Recently Deleted, still recoverable) rather than a hard
 * delete, in case this heuristic is ever wrong about which pin was the "real" one. */
class AutoParkReconnectCleanupWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        val mac = inputData.getString(AUTO_PARK_MAC_KEY) ?: return Result.success()
        val key = AUTO_PARK_PENDING_SPOT_PREF_PREFIX + mac.uppercase()
        val spotId = prefs.getInt(key, -1)
        if (spotId <= 0) {
            // Nothing pending yet — but a "wallet bounce" (door open-close in a few seconds to
            // grab something forgotten) can easily beat AutoParkWorker's own GPS fetch + geocode
            // to the finish line, since this cleanup has nothing to do but a single pref read.
            // Giving up immediately here would mean the completed-but-not-yet-marked save from
            // that disconnect survives untouched even though the user was back in the car within
            // seconds. Checking whether a save is still actually enqueued/running for this exact
            // MAC — rather than guessing off a fixed delay — retries only when there's real
            // evidence one is still in flight, and gives up right away otherwise (e.g. auto-park
            // skipped this disconnect entirely: a quiet zone, the feature being off, no linked
            // vehicle).
            val saveStillInFlight = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork("${UNIQUE_WORK_NAME_PREFIX}_${mac.uppercase()}")
                .get()
                .any { !it.state.isFinished }
            return if (saveStillInFlight && runAttemptCount < MAX_CLEANUP_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.success()
            }
        }
        prefs.edit().remove(key).apply()
        val dao = AppDatabase.getDatabase(context.applicationContext).locationDao()
        val spot = dao.getSpotById(spotId)
        dao.softDeleteSpot(spotId)
        // Active Tracking mode starts TimerService for this spot — soft-delete alone left a live
        // tracking notification for a pin that now sits in Recently Deleted (gas-station reconnect).
        if (spot != null && prefs.getBoolean("is_pinned", false)) {
            val pinnedLat = prefs.getCoord("lat")
            val pinnedLng = prefs.getCoord("lng")
            if (kotlin.math.abs(pinnedLat - spot.lat) < 1e-5 &&
                kotlin.math.abs(pinnedLng - spot.lng) < 1e-5
            ) {
                ActiveTrackingHelper.clearActiveTracking(context.applicationContext)
            }
        }
        return Result.success()
    }
}

fun enqueueAutoParkReconnectCleanup(context: Context, mac: String) {
    val request = OneTimeWorkRequestBuilder<AutoParkReconnectCleanupWorker>()
        .setInputData(Data.Builder().putString(AUTO_PARK_MAC_KEY, mac).build())
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        "auto_park_reconnect_cleanup_${mac.uppercase()}",
        ExistingWorkPolicy.REPLACE,
        request
    )
}

/** Exposed so a caller (the "Test Auto-Park Now" button) can observe this exact job's WorkInfo
 * and read back its [AUTO_PARK_OUTCOME_KEY] once finished, instead of firing it blind. */
fun autoParkManualTestWorkName(vehicleId: Int): String = "${UNIQUE_WORK_NAME_PREFIX}_manual_$vehicleId"

/** Used when the caller already has a resolved vehicle (e.g. a manual "simulate auto-park"
 * debug/test action from within the app, which does run with normal database access). */
fun enqueueAutoParkWorkForVehicle(context: Context, vehicleId: Int) {
    val request = OneTimeWorkRequestBuilder<AutoParkWorker>()
        .setInputData(
            Data.Builder()
                .putInt(AUTO_PARK_VEHICLE_ID_KEY, vehicleId)
                .build()
        )
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }
        }
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        autoParkManualTestWorkName(vehicleId),
        ExistingWorkPolicy.REPLACE,
        request
    )
}

private fun legacyVehicleFromPrefs(prefs: SharedPreferences, mac: String): Vehicle? {
    val legacyMac = loadAutoParkCarMac(prefs)?.uppercase() ?: return null
    if (legacyMac != mac.uppercase()) return null
    return Vehicle(
        id = 0,
        name = loadAutoParkCarName(prefs)?.takeIf { it.isNotBlank() } ?: "My Car",
        colorArgb = VehicleColorOptions.first(),
        iconKey = "car",
        bluetoothMac = legacyMac,
        bluetoothName = loadAutoParkCarName(prefs),
        createdAt = System.currentTimeMillis()
    )
}
