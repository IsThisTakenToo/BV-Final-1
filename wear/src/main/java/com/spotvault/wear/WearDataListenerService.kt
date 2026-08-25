package com.spotvault.wear

import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/** Receives the active-tracking-state and theme-state pushes from the phone (see
 * TrackingWearSync.kt/ThemeWearSync.kt on the phone side) and caches them locally in
 * [TrackingStateStore]/[ThemeStateStore] for [QuickActionTileService] to read.
 * Manifest-registered (not a live addListener()) so this fires even if the watch app process isn't
 * already running — the whole point of the static Data Layer registration. */
class WearDataListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        var sawUpdate = false
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            when (event.dataItem.uri.path) {
                QuickActionPaths.TRACKING_STATUS -> {
                    val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val tracking = map.getBoolean(QuickActionPaths.KEY_IS_TRACKING, false)
                    TrackingStateStore.setTracking(this, tracking)
                    if (tracking && map.containsKey(QuickActionPaths.KEY_TARGET_LAT)) {
                        TrackingStateStore.setTargetCoordinates(
                            this,
                            map.getDouble(QuickActionPaths.KEY_TARGET_LAT),
                            map.getDouble(QuickActionPaths.KEY_TARGET_LNG),
                            map.getString(QuickActionPaths.KEY_DISTANCE_UNIT) ?: "mi"
                        )
                    } else if (!tracking) {
                        TrackingStateStore.clearTargetCoordinates(this)
                    }
                    sawUpdate = true
                }
                QuickActionPaths.THEME_STATE -> {
                    val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                    ThemeStateStore.setThemeState(
                        this,
                        primary = map.getInt(QuickActionPaths.KEY_PRIMARY),
                        accent = map.getInt(QuickActionPaths.KEY_ACCENT),
                        surface = map.getInt(QuickActionPaths.KEY_SURFACE),
                        onPrimary = map.getInt(QuickActionPaths.KEY_ON_PRIMARY),
                        onAccent = map.getInt(QuickActionPaths.KEY_ON_ACCENT),
                        onSurface = map.getInt(QuickActionPaths.KEY_ON_SURFACE),
                        buttonStyle = map.getString(QuickActionPaths.KEY_BUTTON_STYLE) ?: "classic"
                    )
                    sawUpdate = true
                }
            }
        }
        dataEvents.release()
        // Nudges the tile to redraw immediately with the new state instead of waiting for
        // whatever polling interval the system would otherwise use.
        if (sawUpdate) {
            TileService.getUpdater(this).requestUpdate(QuickActionTileService::class.java)
        }
    }
}
