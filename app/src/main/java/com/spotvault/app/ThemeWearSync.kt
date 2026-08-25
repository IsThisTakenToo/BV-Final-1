package com.spotvault.app

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

/** Pushes the app's active color theme and button style to any paired Wear OS watch, so
 * QuickActionTileService's buttons match the phone instead of a fixed hardcoded palette. Reuses
 * WidgetThemeHelper.paletteFromPrefs()/buttonStyleFromPrefs() wholesale — the exact same computed
 * colors the home-screen widgets already render with, including respecting the existing
 * "widget_theme_sync"/"widget_layout_sync" toggles (if the user already opted widgets out of
 * mirroring the app theme, the watch tile now follows that same choice rather than introducing a
 * separate one). Called from WidgetThemeHelper.commitThemeChange() — the single chokepoint every
 * color theme / button style change in the app already funnels through — plus once on every
 * MainActivity.onResume() and BeaconVaultApplication cold start as a catch-up, mirroring
 * TrackingWearSync's own reasoning.
 *
 * ProtoLayout (the Tile framework) only supports simple rounded-rectangle corners — none of the
 * app's fancier button shapes (Wild Portal's shattered-glass silhouette, Wildwood's leaf-pod,
 * Hex Plate, etc.) can be replicated on a watch tile. Only "capsule" maps to something visually
 * distinct there (a fully rounded pill); every other style falls back to the tile's own default
 * rounded look — see QuickActionTileService's own use of [KEY_BUTTON_STYLE]. */
object ThemeWearSync {
    // Must match QuickActionPaths.THEME_STATE / KEY_PRIMARY / KEY_ACCENT / KEY_SURFACE /
    // KEY_ON_PRIMARY / KEY_ON_ACCENT / KEY_ON_SURFACE / KEY_BUTTON_STYLE in
    // wear/src/main/java/com/spotvault/wear/QuickActionPaths.kt exactly — duplicated rather than
    // shared because :app has no dependency on :wear. A mismatch fails silently
    // (WearDataListenerService's manifest intent-filter path prefix just never matches), so keep
    // both sides in sync by hand.
    private const val PATH_THEME_STATE = "/droppinvault/theme_state"
    private const val KEY_PRIMARY = "primary"
    private const val KEY_ACCENT = "accent"
    private const val KEY_SURFACE = "surface"
    private const val KEY_ON_PRIMARY = "on_primary"
    private const val KEY_ON_ACCENT = "on_accent"
    private const val KEY_ON_SURFACE = "on_surface"
    private const val KEY_BUTTON_STYLE = "button_style"

    fun pushThemeState(context: Context, prefs: SharedPreferences) {
        val palette = WidgetThemeHelper.paletteFromPrefs(prefs)
        val buttonStyle = WidgetThemeHelper.buttonStyleFromPrefs(prefs)
        val request = PutDataMapRequest.create(PATH_THEME_STATE).apply {
            dataMap.putInt(KEY_PRIMARY, palette.primary)
            dataMap.putInt(KEY_ACCENT, palette.accent)
            dataMap.putInt(KEY_SURFACE, palette.surface)
            dataMap.putInt(KEY_ON_PRIMARY, palette.onPrimary)
            dataMap.putInt(KEY_ON_ACCENT, palette.onAccent)
            dataMap.putInt(KEY_ON_SURFACE, palette.onSurface)
            dataMap.putString(KEY_BUTTON_STYLE, buttonStyle)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        // Fire-and-forget — same reasoning as TrackingWearSync: DataClient persists/retries
        // delivery on its own, and there's nothing useful to surface for a failed background sync.
        Wearable.getDataClient(context.applicationContext).putDataItem(request)
    }
}
