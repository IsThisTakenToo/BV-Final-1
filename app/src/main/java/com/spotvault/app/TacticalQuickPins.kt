package com.spotvault.app

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/** Title on vault cards — spots saved with a "$label\n$date" title (the "datetime" naming
 * format) show just the label on the card; the timestamp is already shown separately. */
fun vaultSpotDisplayTitle(spot: LocationSpot): String =
    spot.title.substringBefore('\n').trim().ifBlank { "Saved spot" }

data class QuickPinDef(
    val id: String,
    val emoji: String,
    val label: String,
    val details: String,
    val section: String,
    val sortOrder: Int
)

/** The Quick Pin every "instant" one-tap surface fires — home screen button, Everyday widget,
 * and the Quick Pin Quick Settings tile. Deliberately not configurable: this used to be resolved
 * through a per-surface pref key (`prefs.getString(settingKey, null)`, matched against the
 * user's custom Quick Pin list), but nothing in the app ever actually wrote that pref — there
 * was no picker UI for it. Any install that had a value saved there from some earlier build
 * would silently keep using it forever with no way to see or change it from current Settings,
 * which is exactly how a one-tap "Quick Pin" ended up permanently saving as "Hotel / Lodging"
 * for no visible reason. Always the same generic pin now, full stop. */
val GENERIC_QUICK_PIN = QuickPinDef(
    id = "default_quick_pin",
    emoji = "📍",
    label = "Quick Pin",
    details = "Quick Pin",
    section = "General",
    sortOrder = 0
)

/** Fixed identity for the one-tap "Quick Track" action — deliberately independent of whatever
 * Quick Pin default the user has configured, so it never inherits an unrelated label like
 * "Parked Truck" and always reads as its own thing. */
fun quickTrackOption(): QuickPinDef = QuickPinDef(
    id = "quick_track_default",
    emoji = "🎯",
    label = "Quick Track",
    details = "Quick Track",
    section = "General",
    sortOrder = 0
)

sealed class QuietSaveResult {
    data class Saved(val spotId: Int) : QuietSaveResult()
    data object Failed : QuietSaveResult()
    /** Active-track save needs POST_NOTIFICATIONS; caller should request then retry. */
    data object NeedsNotificationPermission : QuietSaveResult()
}

/** True if the device has any location provider (GPS or network) turned on. Doesn't require a
 * location permission — this is purely the system Location Services toggle. */
fun isLocationServicesEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        ?: return true
    return try {
        manager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    } catch (_: Exception) {
        true
    }
}

/** True if location can actually be fetched from a background context (no foreground Activity/
 * Service of ours involved) — API 29+ requires ACCESS_BACKGROUND_LOCATION for this specifically,
 * separate from and in addition to fine/coarse location. [CarBluetoothReceiver]'s disconnect
 * handler, [AutoParkWorker], and [MotionBookmarkWorker] all run exactly this kind of background
 * context (a BroadcastReceiver with no UI, or a WorkManager job with no foreground promotion) —
 * none of them previously checked this before attempting a fetch, so a user who granted only
 * "While using the app" got every location call silently denied by the OS on every single
 * disconnect, with [resolveCurrentLocation] falling through all its fallbacks to null and
 * WorkManager retrying indefinitely — wasted battery on a call that could never succeed, with the
 * Settings screen's "Armed" status being the only (easy to miss) sign anything was wrong. */
fun hasBackgroundLocationPermission(context: Context): Boolean =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

private suspend fun awaitLocationWithTimeout(
    fused: com.google.android.gms.location.FusedLocationProviderClient,
    priority: Int,
    timeoutMs: Long
): android.location.Location? {
    val token = CancellationTokenSource()
    return try {
        withTimeoutOrNull(timeoutMs) {
            fused.getCurrentLocation(priority, token.token).await()
        }
    } catch (_: Exception) {
        null
    } finally {
        // Always stop the underlying request — on timeout it would otherwise keep the GPS
        // radio pinned in the background for no reason.
        token.cancel()
    }
}

/** Up to this many fixes get collected for averaging, and no more than this long is spent
 * waiting on them — a Quick Pin still has to feel instant, so this trades some of the accuracy
 * ceiling for a hard latency budget rather than waiting for a "perfect" set of samples. */
private const val AVERAGING_MAX_SAMPLES = 5
private const val AVERAGING_WINDOW_MS = 4_000L
private const val AVERAGING_INTERVAL_MS = 700L

/** Lets [collectAveragedLocation] finish early when GPS is clearly performing well, instead of
 * always waiting out the full sample count/window regardless of how good the fixes already
 * arriving are. [EARLY_EXIT_MIN_SAMPLES] still requires more than one sample first — a real
 * averaging benefit, not just trusting one lucky fix — and [EARLY_EXIT_ACCURACY_METERS] is
 * deliberately tighter than [ACCEPTABLE_ACCURACY_METERS] (half of it), so this only short-circuits
 * when the fix is unambiguously excellent, not merely adequate; anything less clean keeps
 * collecting toward the full budget exactly as before. */
private const val EARLY_EXIT_MIN_SAMPLES = 3
private const val EARLY_EXIT_ACCURACY_METERS = 5f

/** Combines several fixes into one, weighted by each fix's own reported accuracy (tighter fixes
 * count more) — an accuracy-weighted mean pulls toward the true position faster than a plain
 * average when the samples aren't all equally trustworthy. The result's accuracy is reported as
 * the best individual sample's, which is the honest (if conservative) bound for the average. */
private fun weightedAverageLocation(samples: List<android.location.Location>): android.location.Location {
    var weightSum = 0.0
    var latSum = 0.0
    var lngSum = 0.0
    samples.forEach { loc ->
        val accuracy = if (loc.hasAccuracy() && loc.accuracy > 0f) loc.accuracy else 1f
        val weight = 1.0 / (accuracy.toDouble() * accuracy.toDouble())
        weightSum += weight
        latSum += loc.latitude * weight
        lngSum += loc.longitude * weight
    }
    return android.location.Location(samples.first().provider).apply {
        latitude = latSum / weightSum
        longitude = lngSum / weightSum
        accuracy = samples.minOf { if (it.hasAccuracy()) it.accuracy else Float.MAX_VALUE }
        time = samples.last().time
    }
}

/** Collects up to [AVERAGING_MAX_SAMPLES] fixes over [AVERAGING_WINDOW_MS] and, if at least one
 * cleared [ACCEPTABLE_ACCURACY_METERS], returns their accuracy-weighted average instead of
 * trusting whichever single fix arrives first — GPS jitter near buildings/multipath is roughly
 * zero-mean noise around the true position, so averaging several independent fixes narrows the
 * drop point meaningfully more than one fix alone. If nothing acceptable arrived, falls back to
 * the single best (lowest-accuracy-value) raw sample seen — same degrade-gracefully contract as
 * the rest of [resolveCurrentLocation]'s callers: never silently return nothing just because
 * every sample was noisy. */
private suspend fun collectAveragedLocation(
    fused: com.google.android.gms.location.FusedLocationProviderClient,
    priority: Int
): android.location.Location? {
    // The callback fires on the main looper below, while this coroutine (and its polling loop)
    // typically runs on a background dispatcher — synchronizedList avoids a data race between
    // the callback's writes and the loop's reads of the same list.
    val samples = java.util.Collections.synchronizedList(mutableListOf<android.location.Location>())
    val request = LocationRequest.Builder(priority, AVERAGING_INTERVAL_MS)
        .setMaxUpdates(AVERAGING_MAX_SAMPLES)
        .build()

    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            samples.addAll(result.locations)
        }
    }

    try {
        fused.requestLocationUpdates(request, callback, android.os.Looper.getMainLooper())
        withTimeoutOrNull(AVERAGING_WINDOW_MS) {
            while (samples.size < AVERAGING_MAX_SAMPLES) {
                delay(150)
                // Previously this only ever stopped early via the outer timeout or hitting
                // AVERAGING_MAX_SAMPLES — under a clear sky, where the very first sample can
                // already be an excellent fix, every single Pin/Snap/Quick action still paid the
                // full ~3.5-4s to collect all 5 samples anyway, for no accuracy benefit left to
                // gain. Checking the running average after each new sample lets a clean GPS lock
                // finish in ~2-2.5s instead, while multipath/indoor conditions (where the extra
                // samples actually help) still collect toward the full budget exactly as before.
                if (samples.size >= EARLY_EXIT_MIN_SAMPLES) {
                    val soFar = synchronized(samples) { ArrayList(samples) }
                    val runningAverage = weightedAverageLocation(soFar)
                    if (runningAverage.hasAccuracy() && runningAverage.accuracy <= EARLY_EXIT_ACCURACY_METERS) break
                }
            }
        }
    } catch (_: Exception) {
        return null
    } finally {
        fused.removeLocationUpdates(callback)
    }

    // removeLocationUpdates() is asynchronous — a callback already queued on the main looper can
    // still land after this point, so individually-synchronized add/size calls aren't enough to
    // make iterating the list below safe on their own. Snapshotting under the list's own lock
    // (the documented-correct way to iterate a Collections.synchronizedList) avoids a
    // ConcurrentModificationException from that race.
    val snapshot: List<android.location.Location> = synchronized(samples) { ArrayList(samples) }
    val acceptableSamples = snapshot.filter { it.isAcceptable() }
    if (acceptableSamples.isNotEmpty()) return weightedAverageLocation(acceptableSamples)
    // Nothing cleared the accuracy bar (e.g. every sample landed in the 10-20m range) — average
    // whatever was collected anyway, rather than picking a single best sample and discarding the
    // rest. weightedAverageLocation already weights each sample by 1/accuracy², so the least-bad
    // ones still count for more; a weighted average of several independent-but-mediocre fixes is
    // still a better estimate of the true position than any one of them alone.
    if (snapshot.isNotEmpty()) return weightedAverageLocation(snapshot)
    return null
}

/** Only trusts the cached fix as an instant shortcut if it's both fresh (GPS/fused was
 * realistically still active moments ago, not "whatever was cached this morning") and
 * genuinely accurate (a coarse cell/wifi fix can be "fresh" and still be 100s of meters off) —
 * never a substitute for a real fix, only a way to skip waiting on one that's redundant. */
private const val FAST_PATH_MAX_AGE_MS = 3_000L
private const val FAST_PATH_MAX_ACCURACY_METERS = 10f

private suspend fun recentAccurateCachedLocation(
    fused: com.google.android.gms.location.FusedLocationProviderClient
): android.location.Location? = try {
    val cached = fused.lastLocation.await() ?: return null
    val ageMs = System.currentTimeMillis() - cached.time
    val accurateEnough = cached.hasAccuracy() && cached.accuracy <= FAST_PATH_MAX_ACCURACY_METERS
    if (ageMs in 0..FAST_PATH_MAX_AGE_MS && accurateEnough) cached else null
} catch (_: Exception) {
    null
}

/** A fix looser than this isn't trusted outright — e.g. a "high accuracy" request can still
 * hand back a degraded fix quickly (no internet for AGPS assistance data, GPS multipath near a
 * building), and its accuracy field is the only way to tell that happened. 10m — tight enough to
 * actually distinguish a specific vehicle in a crowded parking lot (20m previously wasn't), still
 * achievable from a real GPS/GNSS fix in reasonable conditions without needing to wait
 * indefinitely for it (see the tightened fallback timeouts below). */
private const val ACCEPTABLE_ACCURACY_METERS = 10f

private fun android.location.Location?.isAcceptable(): Boolean =
    this != null && hasAccuracy() && accuracy <= ACCEPTABLE_ACCURACY_METERS

private fun betterFixOf(
    a: android.location.Location?,
    b: android.location.Location?
): android.location.Location? {
    if (a == null) return b
    if (b == null) return a
    val aAccuracy = if (a.hasAccuracy()) a.accuracy else Float.MAX_VALUE
    val bAccuracy = if (b.hasAccuracy()) b.accuracy else Float.MAX_VALUE
    return if (aAccuracy <= bAccuracy) a else b
}

/** Second-tier (balanced-power) fallback timeout — kept short deliberately. Tightening
 * ACCEPTABLE_ACCURACY_METERS to 10m makes the first tier less likely to clear the bar on the
 * first try than the old 20m did, so this budget shrank to compensate: worst case is now
 * AVERAGING_WINDOW_MS (4s) + this (3s) = 7s total before falling back to the best sample seen,
 * rather than letting the tightened bar silently make every save take longer. */
private const val BALANCED_FALLBACK_TIMEOUT_MS = 3_000L

/** Best-effort accurate fix with free, no-API fallbacks: an instant reuse of an already-fresh,
 * already-accurate cached fix if one exists (see [recentAccurateCachedLocation]), otherwise a
 * fresh high-accuracy fix (what actually fixes "sometimes 15-20 yards off", now to within 10m) —
 * accepted only if its own reported accuracy backs that up, since a degraded fix can still arrive
 * quickly and look successful — then a faster network/wifi-assisted fix if GPS can't lock in time
 * (e.g. indoors), falling back to whichever of the two fixes was more accurate if neither cleared
 * the 10m bar (bounded to ~7s total — see [BALANCED_FALLBACK_TIMEOUT_MS] — so a tighter accuracy
 * bar never means an open-ended wait), and only then the last cached fix regardless of age/
 * accuracy — so a button press degrades gracefully instead of hanging or silently saving nothing. */
suspend fun resolveCurrentLocation(context: Context, prefs: SharedPreferences): Pair<Double, Double>? {
    LocationAcquisitionStatus.begin()
    try {
        val fused = LocationServices.getFusedLocationProviderClient(context)

        recentAccurateCachedLocation(fused)?.let { return it.latitude to it.longitude }

        // Fail fast if the system-level Location Services toggle is off entirely (not a
        // permission problem — the user has simply turned GPS/location off device-wide). Every
        // fetch below would just run out its full timeout waiting for a fix that can never
        // arrive — up to ~7s combined — when the answer is already knowable instantly via one
        // synchronous check. Still falls through to fetchLastKnownLocation: a previously-cached
        // fix, if Play Services happens to still have one, costs nothing to check regardless.
        if (!isLocationServicesEnabled(context)) {
            return fetchLastKnownLocation(context)
        }

        var bestFix: android.location.Location? = null

        if (prefs.getBoolean("high_accuracy_gps", true)) {
            val highAccuracyFix = collectAveragedLocation(fused, Priority.PRIORITY_HIGH_ACCURACY)
            if (highAccuracyFix.isAcceptable()) return highAccuracyFix!!.latitude to highAccuracyFix.longitude
            bestFix = betterFixOf(bestFix, highAccuracyFix)
        }

        val balancedFix = awaitLocationWithTimeout(fused, Priority.PRIORITY_BALANCED_POWER_ACCURACY, BALANCED_FALLBACK_TIMEOUT_MS)
        if (balancedFix.isAcceptable()) return balancedFix!!.latitude to balancedFix.longitude
        bestFix = betterFixOf(bestFix, balancedFix)

        // Neither tier met the accuracy bar — use whichever fix was better rather than nothing,
        // even if it's only in the 10-20m range. A degraded-but-real fix beats losing the pin.
        bestFix?.let { return it.latitude to it.longitude }

        return fetchLastKnownLocation(context)
    } finally {
        LocationAcquisitionStatus.end()
    }
}

data class GeocodedAddress(val full: String, val city: String, val state: String)

private fun isInstantQuickActionId(optionId: String): Boolean =
    optionId == "quick_track_default" || optionId == "default_quick_pin"

/** Title for one-tap Quick Pin / Quick Track saves — address when available, otherwise city/state
 * + short coords. Not private — [resolveStuckSpotAddresses] and the Vault's manual "Refresh
 * Address" action (HistoryVaultDialog.kt) both reuse this same fallback chain when re-resolving a
 * spot that's stuck showing raw coordinates. */
fun quickActionSpotTitle(geocoded: GeocodedAddress?, lat: Double, lng: Double): String {
    if (lat == 0.0 && lng == 0.0) return "No GPS signal"
    val shortCoords = String.format(Locale.US, "%.4f, %.4f", lat, lng)
    val full = geocoded?.full?.trim().orEmpty()
    if (full.isNotEmpty() && !full.startsWith("Lat:", ignoreCase = true)) return full
    val cityState = listOf(geocoded?.city?.trim().orEmpty(), geocoded?.state?.trim().orEmpty())
        .filter { it.isNotEmpty() }
        .joinToString(", ")
    return if (cityState.isNotEmpty()) "$cityState · $shortCoords" else shortCoords
}

/** True when [title] is still exactly the raw-coordinate fallback [quickActionSpotTitle] produces
 * when geocoding never resolved a real address — either the legacy "Lat: x, Lng: y" text, or the
 * bare "%.4f, %.4f" short-coords string. Lets a re-geocode attempt safely overwrite a placeholder
 * title without ever touching a title the user (or Snap/Pin's OCR) actually set. */
fun isPlaceholderCoordinateTitle(title: String, lat: Double, lng: Double): Boolean {
    val trimmed = title.trim()
    if (trimmed.startsWith("Lat:", ignoreCase = true)) return true
    return trimmed == String.format(Locale.US, "%.4f, %.4f", lat, lng)
}

/** On API 33+, [android.location.Geocoder]'s old synchronous getFromLocation() is deprecated
 * and, in practice, unreliable — geocoding moved to an async system service under the hood, and
 * the blocking compatibility shim can return an empty result even when the service would have
 * succeeded a moment later. This uses the real async callback API on 33+ (falling back to the
 * blocking call pre-33, where it's still fine) and retries once on a transient miss, so a spot
 * doesn't get permanently stuck as "Lat: x, Lng: y" over what was really just a one-off hiccup. */
suspend fun reverseGeocodeAddress(context: Context, lat: Double, lng: Double): GeocodedAddress {
    repeat(2) { attempt ->
        // Without a network (e.g. underground parking), the async GeocodeListener can fail to
        // fire onGeocode/onError at all rather than erroring quickly, so this call could otherwise
        // hang indefinitely instead of falling through to the Lat/Lng fallback below.
        val result = kotlinx.coroutines.withTimeoutOrNull(5_000L) { buildGeocodedAddress(context, lat, lng) }
        if (result != null) return result
        if (attempt == 0) kotlinx.coroutines.delay(700)
    }
    val formatLat = String.format(Locale.US, "%.4f", lat)
    val formatLng = String.format(Locale.US, "%.4f", lng)
    return GeocodedAddress(full = "Lat: $formatLat, Lng: $formatLng", city = "", state = "")
}

private suspend fun buildGeocodedAddress(context: Context, lat: Double, lng: Double): GeocodedAddress? {
    val address = try {
        fetchFirstAddress(context, lat, lng)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } ?: return null

    val streetNum = address.subThoroughfare ?: ""
    val streetName = address.thoroughfare ?: ""
    val street = if (streetNum.isNotEmpty() && streetName.isNotEmpty()) {
        "$streetNum $streetName"
    } else {
        streetName
    }
    // Rural pins routinely have no `locality` at all (no city limits out there) — locality alone
    // used to leave city permanently blank for those spots even though the geocoder had perfectly
    // good county/township data to fall back to. County, then township/neighborhood, is the same
    // fallback order Android's own Address docs describe as the next-most-specific fields below
    // locality.
    val city = address.locality?.takeIf { it.isNotBlank() }
        ?: address.subAdminArea?.takeIf { it.isNotBlank() }
        ?: address.subLocality?.takeIf { it.isNotBlank() }
        ?: ""
    val state = address.adminArea ?: ""
    // getAddressLine(0) is Android's own locally-formatted line — the geocoder already knows
    // each country's real address order/conventions (many don't even use a US-style
    // street-number/street-name/city/state,ZIP layout). Manually concatenating street+city+state
    // is a US-shaped assumption that reads wrong outside the US — adminArea (state/province) is
    // routinely blank or not how the address would actually be written there. Preferred as the
    // primary source now, with the manual concatenation only as a fallback for the rare geocoder
    // backend that doesn't populate address lines at all.
    var addressStr = address.getAddressLine(0) ?: ""
    if (addressStr.isEmpty()) {
        addressStr = listOf(street, city, state).filter { it.isNotEmpty() }.joinToString(", ")
    }
    if (addressStr.isEmpty() || addressStr.contains("null")) return null
    return GeocodedAddress(full = addressStr, city = city, state = state)
}

private suspend fun fetchFirstAddress(context: Context, lat: Double, lng: Double): android.location.Address? {
    // Some devices (no Google Play services, some AOSP/China builds) have no geocoder backend at
    // all — isPresent() is a cheap synchronous check, so skip straight to the Lat/Lng fallback
    // instead of waiting out a call (or the outer 5s timeout) that can only ever fail there.
    if (!android.location.Geocoder.isPresent()) return null
    val geocoder = android.location.Geocoder(context, Locale.getDefault())
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            try {
                geocoder.getFromLocation(lat, lng, 1, object : android.location.Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<android.location.Address>) {
                        if (cont.isActive) cont.resume(addresses.firstOrNull())
                    }
                    override fun onError(errorMessage: String?) {
                        if (cont.isActive) cont.resume(null)
                    }
                })
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
    } else {
        @Suppress("DEPRECATION")
        geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
    }
}

private fun buildQuickPinSpot(
    option: QuickPinDef,
    lat: Double,
    lng: Double,
    timestamp: Long,
    vehicleId: Int? = null,
    photoPath: String = ""
): LocationSpot {
    // Only two shapes of option ever reach here: the fixed Quick Pin/Quick Track ids (their title
    // gets replaced by a real address once the async geocode in the caller resolves) and
    // Auto-Park's per-vehicle id, which keeps this "label + timestamp" title as-is. There used to
    // be a per-install "Auto-Pin Naming" format picker in Settings that branched this — Quick
    // Pin/Quick Track were always hardcoded exempt from it, and its other two formats were only
    // ever reachable through a custom-preset picker UI that never existed (see GENERIC_QUICK_PIN's
    // doc for the matching history on that), so in practice it only ever changed Auto-Park's
    // title format, invisibly, from a screen with no mention of Auto-Park. Removed; this is the
    // "datetime" format every install already got by default.
    val autoTitle = if (isInstantQuickActionId(option.id)) {
        quickActionSpotTitle(null, lat, lng)
    } else {
        "${option.label}\n${java.text.SimpleDateFormat("MMM d, h:mm a", Locale.US).format(java.util.Date(timestamp))}"
    }
    return LocationSpot(
        imagePath = photoPath,
        title = autoTitle,
        locationDetails = option.details,
        timestamp = timestamp,
        lat = lat,
        lng = lng,
        address = if (lat == 0.0 && lng == 0.0) "No GPS signal — location wasn't captured" else "",
        isFavorite = false,
        vehicleId = vehicleId
    )
}

private const val FALLBACK_PARKED_CHANNEL_ID = "auto_park_fallback"
private const val FALLBACK_PARKED_NOTIFICATION_ID = 9201

/** Posted only when starting the live "Active Tracking" foreground service gets rejected outright
 * — Android 12+ (tightened further in 14/API 34) can throw ForegroundServiceStartNotAllowedException
 * when a background-only caller with zero foreground presence (AutoParkWorker's Bluetooth-disconnect
 * trigger, running with no Activity/visible UI at all) tries to start a new foreground service
 * without a qualifying exemption. The spot itself is already saved in the Vault by the time this can
 * ever run — this exists purely so the user isn't left with zero indication a park was recorded when
 * the OS silently blocks the live tracking notification. A plain, non-ongoing notification rather
 * than a foreground service, so it isn't subject to the same restriction. Creates its own channel
 * on demand rather than assuming TimerService's already exist — TimerService.onCreate() (where
 * those are normally set up) never got to run if starting it just failed. */
private fun postFallbackParkedNotification(context: Context) {
    // Same user-picked "alarm_sound_uri" every other alert in the app uses — this channel used to
    // never call setSound() at all, silently falling back to the system's default notification
    // sound. Hash-suffixed like MainActivity's TIMER_ALERT channel: a channel's sound is locked in
    // the moment it's first created and never updates in place, so picking a new Alert Sound needs
    // a new channel id, not just a new value passed to setSound() on the old one.
    val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
    val soundUriStr = prefs.getString("alarm_sound_uri", null)
    val channelId = if (!soundUriStr.isNullOrEmpty()) {
        "${FALLBACK_PARKED_CHANNEL_ID}_${soundUriStr.hashCode()}"
    } else {
        FALLBACK_PARKED_CHANNEL_ID
    }
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channel = android.app.NotificationChannel(
            channelId,
            "Parking Confirmations",
            android.app.NotificationManager.IMPORTANCE_HIGH
        )
        val soundUri = if (!soundUriStr.isNullOrEmpty()) {
            android.net.Uri.parse(soundUriStr)
        } else {
            android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        }
        channel.setSound(
            soundUri,
            android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()
        )
        manager.createNotificationChannel(channel)
    }
    val openIntent = android.content.Intent(context, MainActivity::class.java).apply {
        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val openPendingIntent = android.app.PendingIntent.getActivity(
        context,
        26,
        openIntent,
        android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
    )
    val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_menu_camera)
        .setContentTitle("Car Parked")
        .setContentText("Tap to view in DropPin Vault")
        .setAutoCancel(true)
        .setContentIntent(openPendingIntent)
        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
        .build()
    try {
        // POST_NOTIFICATIONS is already confirmed granted by this point — quickActiveTrackPin
        // only ever reaches this catch block after its own hasNotificationPermission() check
        // already passed. The try/catch here is only a backstop for the permission being revoked
        // in the narrow window between that check and this call.
        androidx.core.app.NotificationManagerCompat.from(context).notify(FALLBACK_PARKED_NOTIFICATION_ID, notification)
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}

private fun hasNotificationPermission(context: Context): Boolean {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

/** Saves a tactical pin immediately — no modal, no camera flow. Ends any active pin.
 * [resolvedLocation], when supplied, skips this function's own GPS fetch entirely and uses that
 * fix directly — [AutoParkWorker] already resolves one of its own to evaluate quiet zones before
 * ever reaching this call, and re-fetching a second time here wasted up to another ~7s (and a
 * second GPS/network round of battery) on every single auto-park save for no benefit, since the
 * two fetches were never actually correlated with each other anyway. */
suspend fun quietSaveTacticalPin(
    context: Context,
    dao: LocationDao,
    prefs: SharedPreferences,
    option: QuickPinDef,
    vehicleId: Int? = null,
    photoPath: String = "",
    resolvedLocation: Pair<Double, Double>? = null
): QuietSaveResult = withContext(Dispatchers.IO) {
    // No GPS/network fix (e.g. underground parking) never discards the save — same fallback
    // processPhotoAndPin already relies on. Photo and pin still land in the Vault immediately;
    // they just carry a 0,0 placeholder location instead of blocking or getting lost.
    val (lat, lng) = resolvedLocation ?: resolveCurrentLocation(context, prefs) ?: (0.0 to 0.0)
    val timestamp = System.currentTimeMillis()

    // Quiet save ends active tracking — never leave a pin running alongside it. Was_pinned is
    // read before clearing it, so the STOP intent below only fires when a session was actually
    // running (matches how the "Found" notification action ends one — see
    // ClearNotificationReceiver — so the persistent notification doesn't outlive its prefs).
    val wasActivelyTracking = prefs.getBoolean("is_pinned", false)
    prefs.edit()
        .putBoolean("is_pinned", false)
        .remove("photo_path")
        .remove("lat")
        .remove("lng")
        .apply()
    if (wasActivelyTracking) {
        context.startService(
            android.content.Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_STOP
            }
        )
    }

    val resolvedVehicleId = vehicleId ?: run {
        val vehicleDao = AppDatabase.getDatabase(context.applicationContext).vehicleDao()
        resolveVehicleForQuickSave(vehicleDao)
    }

    // Every Quick Pin creates its own dated Vault entry now — same as Quick Track — instead of
    // silently overwriting a previous "Parked Truck" spot in place. Smart Deduplication (opt-in,
    // off by default) is the one exception: when it's on and a recent spot already sits within
    // the merge radius, this folds into it instead of creating another entry.
    val mergeTarget = findDeduplicationMergeTarget(dao, prefs, lat, lng)
    val savedId: Int
    if (mergeTarget != null) {
        savedId = mergeDeduplicatedSpot(
            dao = dao,
            target = mergeTarget,
            newTimestamp = timestamp,
            newImagePath = photoPath,
            newLocationDetails = option.details,
            newVehicleId = resolvedVehicleId
        ).id
        // Coordinates didn't move, so the merge target's address is still correct — no need to
        // re-geocode or risk the block below overwriting its title/notes with generic ones.
    } else {
        val spot = buildQuickPinSpot(option, lat, lng, timestamp, resolvedVehicleId, photoPath)
        savedId = dao.insertSpotAndGetId(spot).toInt()

        if (prefs.getBoolean("auto_fetch_address", true) && (lat != 0.0 || lng != 0.0)) {
            val geocoded = reverseGeocodeAddress(context, lat, lng)
            val spotToUpdate = dao.getSpotById(savedId)
            if (spotToUpdate != null) {
                dao.updateSpot(
                    spotToUpdate.copy(
                        address = geocoded.full,
                        locationDetails = option.details,
                        city = geocoded.city,
                        state = geocoded.state,
                        title = if (isInstantQuickActionId(option.id)) {
                            quickActionSpotTitle(geocoded, lat, lng)
                        } else {
                            spotToUpdate.title
                        }
                    )
                )
            }
        }
    }

    WidgetThemeHelper.refreshAllWidgets(context)
    QuietSaveResult.Saved(savedId)
}

/**
 * One-tap Active Tracking pin — same result as Pin Beacon → Active Tracking,
 * without photo/OCR or a countdown timer. See [quietSaveTacticalPin]'s doc for why
 * [resolvedLocation] exists.
 */
suspend fun quickActiveTrackPin(
    context: Context,
    dao: LocationDao,
    prefs: SharedPreferences,
    option: QuickPinDef,
    vehicleId: Int? = null,
    photoPath: String = "",
    resolvedLocation: Pair<Double, Double>? = null
): QuietSaveResult = withContext(Dispatchers.IO) {

    // Same reasoning as quietSaveTacticalPin: no fix (underground parking, dead zone) falls
    // back to a 0,0 placeholder rather than losing the save outright.
    val (lat, lng) = resolvedLocation ?: resolveCurrentLocation(context, prefs) ?: (0.0 to 0.0)

    // Ask for notification permission before persisting, so a retry after grant
    // does not insert a second pin.
    if (!hasNotificationPermission(context)) {
        return@withContext QuietSaveResult.NeedsNotificationPermission
    }

    val vehicleDao = AppDatabase.getDatabase(context.applicationContext).vehicleDao()
    val resolvedVehicleId = vehicleId ?: resolveVehicleForQuickSave(vehicleDao)
    val pinnedVehicle = resolvedVehicleId?.let { vehicleDao.getById(it) }

    val timestamp = System.currentTimeMillis()

    prefs.edit()
        .putBoolean("is_pinned", true)
        .putString("photo_path", photoPath)
        .putFloat("lat", lat.toFloat())
        .putFloat("lng", lng.toFloat())
        .putString("location_details", option.details)
        .putString("current_address", "Loading address...")
        .remove("timer_end_time")
        .also { editor -> applyPinnedVehiclePrefs(editor, pinnedVehicle) }
        .commit()

    // Smart Deduplication (opt-in, off by default): merge into a recent nearby spot instead of
    // inserting a new one. See quietSaveTacticalPin's matching comment for why this is the one
    // exception to "every Quick action creates its own dated entry."
    val mergeTarget = findDeduplicationMergeTarget(dao, prefs, lat, lng)
    val savedId: Int
    if (mergeTarget != null) {
        val merged = mergeDeduplicatedSpot(
            dao = dao,
            target = mergeTarget,
            newTimestamp = timestamp,
            newImagePath = photoPath,
            newLocationDetails = option.details,
            newVehicleId = resolvedVehicleId
        )
        savedId = merged.id
        // Coordinates didn't move, so the merge target's address is still correct — push it into
        // the live-tracking prefs now instead of leaving "Loading address..." stuck, since the
        // geocode below is skipped for merges.
        prefs.edit()
            .putString("current_address", merged.address)
            .putString("location_details", merged.locationDetails)
            .apply()
    } else {
        val spot = buildQuickPinSpot(option, lat, lng, timestamp, resolvedVehicleId, photoPath)
        savedId = dao.insertSpotAndGetId(spot).toInt()
    }

    // Start the persistent notification/foreground service before anything else that can be
    // slow or get cancelled (geocoding can retry for over a second — see
    // reverseGeocodeAddress). Once startForegroundService fires, the notification survives
    // independently of this coroutine, so a caller that tears down early (the transparent
    // relay activity behind the home-screen widget's Quick Track button, or a WorkManager
    // worker hitting its time limit) can never skip it.
    NotificationGuard.schedule(context)
    // Every other startForegroundService call site in the app (BootReceiver,
    // NotificationGuardReceiver, WatchdogWorker) wraps this in try/catch — this one didn't. It
    // matters most here specifically: this is the exact path AutoParkWorker funnels through with
    // zero foreground UI presence, relying entirely on its expedited-work exemption (API 31+) to
    // avoid ForegroundServiceStartNotAllowedException. If that exemption ever doesn't hold (quota
    // exhausted, OEM quirk, edge case), this would otherwise crash the calling coroutine instead
    // of just leaving the spot saved without a live tracking notification.
    try {
        androidx.core.content.ContextCompat.startForegroundService(
            context,
            android.content.Intent(context, TimerService::class.java).apply {
                putExtra("TIME_MS", 0L)
                putExtra("PHOTO_PATH", "")
                putExtra("LAT", lat)
                putExtra("LNG", lng)
            }
        )
    } catch (e: Exception) {
        e.printStackTrace()
        // See postFallbackParkedNotification's own doc — the OS rejected the live tracking
        // notification outright (most likely ForegroundServiceStartNotAllowedException on
        // Android 12+), so this is the only remaining way to tell the user a park was recorded.
        if (hasNotificationPermission(context)) {
            postFallbackParkedNotification(context)
        }
    }

    if (prefs.getBoolean("auto_fetch_address", true) && (lat != 0.0 || lng != 0.0)) {
        if (mergeTarget == null) {
            val geocoded = reverseGeocodeAddress(context, lat, lng)
            prefs.edit()
                .putString("current_address", geocoded.full)
                .putString("location_details", option.details)
                .apply()
            val spotToUpdate = dao.getSpotById(savedId)
            if (spotToUpdate != null) {
                dao.updateSpot(
                    spotToUpdate.copy(
                        address = geocoded.full,
                        locationDetails = option.details,
                        city = geocoded.city,
                        state = geocoded.state,
                        title = if (isInstantQuickActionId(option.id)) {
                            quickActionSpotTitle(geocoded, lat, lng)
                        } else {
                            spotToUpdate.title
                        }
                    )
                )
            }
        }
        context.startService(
            android.content.Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_UPDATE_DETAILS
            }
        )
    }

    WidgetThemeHelper.bumpWidgetRevision(prefs)
    // In-app Quick Track needs an immediate refresh. Widget relay paths pass refreshWidgetsAfter
    // after the overlay dismisses and will refresh again — a duplicate update is harmless.
    WidgetThemeHelper.refreshAllWidgets(context)

    QuietSaveResult.Saved(savedId)
}

