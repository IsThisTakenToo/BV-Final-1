package com.spotvault.wear

/** Data Layer path constants shared between the watch and phone sides of this feature.
 * Namespaced under "/droppinvault/" rather than bare paths, to avoid any future collision with
 * some other library's own Data Layer usage in either process.
 *
 * :app has no dependency on :wear (a phone module shouldn't depend on a watch module), so these
 * exact string values are duplicated on the phone side rather than shared via this object:
 * TRACKING_STATUS in app/src/main/java/com/spotvault/app/TrackingWearSync.kt, and QUICK_PIN /
 * QUICK_TRACK / QUICK_SHARE / NAVIGATE / FOUND in
 * app/src/main/java/com/spotvault/app/WearActionListenerService.kt.
 * If any of these values ever change here, update those two files to match — nothing else enforces
 * it, and a mismatch fails silently (the message/data event just never matches the manifest
 * intent-filter path prefix on the other side). */
object QuickActionPaths {
    const val TRACKING_STATUS = "/droppinvault/tracking_status"
    const val THEME_STATE = "/droppinvault/theme_state"
    const val QUICK_PIN = "/droppinvault/quick_pin"
    const val QUICK_TRACK = "/droppinvault/quick_track"
    const val QUICK_SHARE = "/droppinvault/quick_share"
    const val NAVIGATE = "/droppinvault/navigate"
    const val FOUND = "/droppinvault/found"

    /** Keys inside the PutDataMapRequest at [TRACKING_STATUS]. Lat/lng/unit are only meaningful
     * when [KEY_IS_TRACKING] is true — CompassActivity reads them to know what it's pointing at. */
    const val KEY_IS_TRACKING = "is_tracking"
    const val KEY_TARGET_LAT = "target_lat"
    const val KEY_TARGET_LNG = "target_lng"
    const val KEY_DISTANCE_UNIT = "distance_unit"

    /** Keys inside the PutDataMapRequest at [THEME_STATE] — plain ARGB Ints, the same
     * WidgetPalette shape the phone's own home-screen widgets already render with. */
    const val KEY_PRIMARY = "primary"
    const val KEY_ACCENT = "accent"
    const val KEY_SURFACE = "surface"
    const val KEY_ON_PRIMARY = "on_primary"
    const val KEY_ON_ACCENT = "on_accent"
    const val KEY_ON_SURFACE = "on_surface"
    const val KEY_BUTTON_STYLE = "button_style"
}
