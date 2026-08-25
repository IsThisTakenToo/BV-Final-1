package com.spotvault.wear

import android.content.Context
import android.content.SharedPreferences

/** Thin wrapper around local SharedPreferences — the watch's own cached copy of the phone's
 * active-tracking state (and, while tracking, the target spot's coordinates), kept in sync by
 * [WearDataListenerService] and optimistically flipped by [QuickActionTileService] itself the
 * instant Quick Track / Found is tapped (corrected by the next real DataClient push if that guess
 * turns out wrong). Never treated as anything more than a rendering hint — the phone's
 * ActiveTrackingHelper.isActive()/pinnedCoordinates() stay the real source of truth, since
 * WearActionListenerService resolves the phone's own current state directly rather than trusting
 * whatever the watch last sent. [targetLat]/[targetLng] are what [CompassActivity] points at. */
object TrackingStateStore {
    private const val PREFS_NAME = "droppinvault_wear_prefs"
    private const val KEY_TRACKING = "is_tracking"
    private const val KEY_TARGET_LAT = "target_lat"
    private const val KEY_TARGET_LNG = "target_lng"
    private const val KEY_DISTANCE_UNIT = "distance_unit"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isTracking(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TRACKING, false)

    fun setTracking(context: Context, tracking: Boolean) {
        prefs(context).edit().putBoolean(KEY_TRACKING, tracking).apply()
    }

    /** Null when there's no known target (never synced yet, or the last sync had tracking off). */
    fun targetCoordinates(context: Context): Pair<Double, Double>? {
        val p = prefs(context)
        if (!p.contains(KEY_TARGET_LAT) || !p.contains(KEY_TARGET_LNG)) return null
        val lat = java.lang.Double.longBitsToDouble(p.getLong(KEY_TARGET_LAT, 0L))
        val lng = java.lang.Double.longBitsToDouble(p.getLong(KEY_TARGET_LNG, 0L))
        return lat to lng
    }

    fun distanceUnit(context: Context): String =
        prefs(context).getString(KEY_DISTANCE_UNIT, "mi") ?: "mi"

    fun setTargetCoordinates(context: Context, lat: Double, lng: Double, unit: String) {
        prefs(context).edit()
            .putLong(KEY_TARGET_LAT, java.lang.Double.doubleToRawLongBits(lat))
            .putLong(KEY_TARGET_LNG, java.lang.Double.doubleToRawLongBits(lng))
            .putString(KEY_DISTANCE_UNIT, unit)
            .apply()
    }

    fun clearTargetCoordinates(context: Context) {
        prefs(context).edit()
            .remove(KEY_TARGET_LAT)
            .remove(KEY_TARGET_LNG)
            .apply()
    }
}
