package com.spotvault.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity

const val MOTION_AUTOPARK_ENABLED_PREF = "motion_autopark_enabled"
private const val MOTION_WATCH_MAC_KEY = "motion_watch_mac"
private const val ACTION_MOTION_TRANSITION = "com.spotvault.app.ACTION_MOTION_TRANSITION"

private const val BOOKMARK_LAT_KEY = "motion_bookmark_lat"
private const val BOOKMARK_LNG_KEY = "motion_bookmark_lng"
private const val BOOKMARK_TIMESTAMP_KEY = "motion_bookmark_timestamp"
private const val BOOKMARK_MAC_KEY = "motion_bookmark_mac"
private const val BOOKMARK_FETCH_TOKEN_KEY = "motion_bookmark_fetch_token"

/** How long a cached "I just saw you leave the vehicle" location is trusted to still be relevant
 * once the Bluetooth disconnect actually confirms it. Long enough to cover walking across a large
 * parking structure or mall lot; short enough to reject an unrelated bookmark left over from an
 * earlier, different trip (e.g. a bus ride hours earlier that also triggered a transition). */
private const val BOOKMARK_TTL_MILLIS = 20L * 60 * 1000

fun isMotionAutoParkEnabled(prefs: SharedPreferences): Boolean =
    prefs.getBoolean(MOTION_AUTOPARK_ENABLED_PREF, false)

fun hasActivityRecognitionPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun motionTransitionPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, MotionActivityReceiver::class.java).apply { action = ACTION_MOTION_TRANSITION }
    return PendingIntent.getBroadcast(context, 30, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
}

/**
 * Starts watching for "you just left the vehicle" signals after a registered car's Bluetooth
 * connects: [DetectedActivity.IN_VEHICLE] exiting (usually the fastest, most confident signal —
 * Play Services already has high confidence you were driving) plus [DetectedActivity.WALKING]/
 * [DetectedActivity.ON_FOOT] entering as backups in case the vehicle-exit signal is missed.
 *
 * None of these transitions save anything by themselves — see [MotionBookmarkWorker]. The actual
 * save only ever happens off the existing, already-reliable Bluetooth-disconnect path in
 * [CarBluetoothReceiver]; this system only supplies that path a better, earlier location fix when
 * one is available, and otherwise gets out of the way entirely.
 */
fun startMotionWatch(context: Context, prefs: SharedPreferences, mac: String) {
    if (!isMotionAutoParkEnabled(prefs) || !hasActivityRecognitionPermission(context)) return
    // A bookmark still sitting here belongs to whatever walking/vehicle-exit transition fired
    // *before* this (re-)connect — e.g. a gas-station stop under the TTL. Without clearing it,
    // that stale fix would silently outlive getting back in the car and be handed to the *next*
    // real disconnect as if it were where the car actually got parked, since nothing else ever
    // invalidates a bookmark on reconnect — only consuming it (a real disconnect) or the TTL do.
    clearMotionBookmark(prefs)
    val transitions = listOf(
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.IN_VEHICLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build(),
        ActivityTransition.Builder()
            .setActivityType(DetectedActivity.ON_FOOT)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build()
    )
    val request = ActivityTransitionRequest(transitions)
    try {
        ActivityRecognition.getClient(context)
            .requestActivityTransitionUpdates(request, motionTransitionPendingIntent(context))
        prefs.edit().putString(MOTION_WATCH_MAC_KEY, mac.uppercase()).apply()
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}

fun stopMotionWatch(context: Context, prefs: SharedPreferences) {
    try {
        ActivityRecognition.getClient(context).removeActivityTransitionUpdates(motionTransitionPendingIntent(context))
    } catch (e: Exception) {
        e.printStackTrace()
    }
    prefs.edit().remove(MOTION_WATCH_MAC_KEY).apply()
    clearMotionBookmark(prefs)
}

internal fun currentMotionWatchMac(prefs: SharedPreferences): String? = prefs.getString(MOTION_WATCH_MAC_KEY, null)

/** Claims this [MotionBookmarkWorker] attempt as the current one, returning a token to check
 * against right before it writes its result. `MotionBookmarkWorker` is enqueued as unique work
 * with `ExistingWorkPolicy.REPLACE`, so a transition firing while an earlier fetch is still
 * mid-flight (a multi-second GPS call) cancels that earlier attempt — but cancellation isn't
 * guaranteed to stop a suspended, non-cooperative call immediately, so the superseded attempt
 * could otherwise still finish afterward and overwrite a fresher bookmark with a stale fix.
 * Recording the latest token here and having each attempt verify it's still current right before
 * writing (see [isMotionBookmarkFetchCurrent]) settles that regardless of which attempt happens
 * to finish first — whichever one is actually latest always wins. */
fun markMotionBookmarkFetchStarted(prefs: SharedPreferences): Long {
    val token = System.currentTimeMillis()
    prefs.edit().putLong(BOOKMARK_FETCH_TOKEN_KEY, token).apply()
    return token
}

/** True only if no newer [markMotionBookmarkFetchStarted] call has happened since [token] was
 * issued — i.e. this attempt is still the most recent one and is safe to actually write. */
fun isMotionBookmarkFetchCurrent(prefs: SharedPreferences, token: Long): Boolean =
    prefs.getLong(BOOKMARK_FETCH_TOKEN_KEY, -1L) == token

/** Caches a just-fetched location as the candidate parking spot — not yet saved anywhere
 * permanent. [MotionBookmarkWorker] calls this every time a watched transition fires; each call
 * simply overwrites the previous bookmark, so the freshest/best fix wins if multiple transitions
 * fire in sequence (e.g. vehicle-exit while still in a garage, then walking once out in the open). */
fun saveMotionBookmark(prefs: SharedPreferences, mac: String, lat: Double, lng: Double) {
    prefs.edit()
        .putString(BOOKMARK_MAC_KEY, mac.uppercase())
        .putFloat(BOOKMARK_LAT_KEY, lat.toFloat())
        .putFloat(BOOKMARK_LNG_KEY, lng.toFloat())
        .putLong(BOOKMARK_TIMESTAMP_KEY, System.currentTimeMillis())
        .apply()
}

fun clearMotionBookmark(prefs: SharedPreferences) {
    prefs.edit()
        .remove(BOOKMARK_MAC_KEY)
        .remove(BOOKMARK_LAT_KEY)
        .remove(BOOKMARK_LNG_KEY)
        .remove(BOOKMARK_TIMESTAMP_KEY)
        .apply()
}

/**
 * Called from [CarBluetoothReceiver] the moment a linked car's Bluetooth actually disconnects —
 * the confirmed, always-reliable trigger. Returns a cached location only if it belongs to the
 * same MAC that just disconnected and is still within [BOOKMARK_TTL_MILLIS]; always clears the
 * bookmark afterward (consumed either way) so a stale or mismatched one can never leak into a
 * later, unrelated trip. Returning null is always safe: the caller falls back to a live GPS fetch,
 * exactly what happens today when Motion & Fitness is off.
 */
fun consumeValidMotionBookmark(prefs: SharedPreferences, disconnectedMac: String): Pair<Double, Double>? {
    val bookmarkMac = prefs.getString(BOOKMARK_MAC_KEY, null)
    val timestamp = prefs.getLong(BOOKMARK_TIMESTAMP_KEY, -1L)
    val lat = prefs.getFloat(BOOKMARK_LAT_KEY, Float.NaN)
    val lng = prefs.getFloat(BOOKMARK_LNG_KEY, Float.NaN)
    clearMotionBookmark(prefs)

    if (bookmarkMac == null || timestamp <= 0L || lat.isNaN() || lng.isNaN()) return null
    if (bookmarkMac != disconnectedMac.uppercase()) return null
    if (System.currentTimeMillis() - timestamp > BOOKMARK_TTL_MILLIS) return null

    return lat.toDouble() to lng.toDouble()
}
