package com.spotvault.app

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

/** Pushes the phone's active-tracking state to any paired Wear OS watch, for
 * QuickActionTileService's own idle/tracking UI swap over there (Quick Pin + Quick Track vs.
 * Navigate + Found). Free feature, no premium gate — called from every point tracking starts or
 * stops (ActiveTrackingHelper.clearTrackingPrefs, quickActiveTrackPin's and quietSaveTacticalPin's
 * own success paths, MainActivity's startPinnedNotificationForSpot), plus once on every
 * MainActivity.onResume() as a catch-up in case a push was missed while the watch was
 * unpaired/off, or state changed through some path this list doesn't cover — fine, since this is
 * best-effort and the tile just briefly shows a stale state until the next successful push. */
object TrackingWearSync {
    // Must match QuickActionPaths.TRACKING_STATUS / KEY_IS_TRACKING / KEY_TARGET_LAT /
    // KEY_TARGET_LNG / KEY_DISTANCE_UNIT in wear/src/main/java/com/spotvault/wear/QuickActionPaths.kt
    // exactly — duplicated rather than shared because :app has no dependency on :wear. A mismatch
    // fails silently (WearDataListenerService's manifest intent-filter path prefix just never
    // matches), so keep both sides in sync by hand.
    private const val PATH_TRACKING_STATUS = "/droppinvault/tracking_status"
    private const val KEY_IS_TRACKING = "is_tracking"
    private const val KEY_TARGET_LAT = "target_lat"
    private const val KEY_TARGET_LNG = "target_lng"
    private const val KEY_DISTANCE_UNIT = "distance_unit"

    fun pushTrackingState(context: Context) {
        val isTracking = ActiveTrackingHelper.isActive(context)
        val request = PutDataMapRequest.create(PATH_TRACKING_STATUS).apply {
            dataMap.putBoolean(KEY_IS_TRACKING, isTracking)
            // CompassActivity's whole target — only meaningful (and only sent) while a track is
            // actually active, matching pinnedCoordinates() itself returning null otherwise.
            if (isTracking) {
                val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
                ActiveTrackingHelper.pinnedCoordinates(context)?.let { (lat, lng) ->
                    dataMap.putDouble(KEY_TARGET_LAT, lat)
                    dataMap.putDouble(KEY_TARGET_LNG, lng)
                }
                dataMap.putString(KEY_DISTANCE_UNIT, loadDistanceUnitFromPrefs(prefs))
            }
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        // Fire-and-forget — DataClient itself already persists/retries delivery once the watch is
        // reachable, this call just needs to hand it off. Failure here (no paired watch at all,
        // Play services unavailable) is silently fine.
        Wearable.getDataClient(context.applicationContext).putDataItem(request)
    }
}
