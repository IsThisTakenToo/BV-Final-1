package com.spotvault.wear

import android.content.Context
import android.content.SharedPreferences

/** Thin wrapper around local SharedPreferences — the watch's own cached copy of the phone's
 * active color theme and button style (see ThemeWearSync.kt on the phone side), kept in sync by
 * [WearDataListenerService]. Defaults match the tile's own original hardcoded palette, so it still
 * looks reasonable in the brief window before the first sync ever completes (or if the watch is
 * never paired at all). */
object ThemeStateStore {
    private const val PREFS_NAME = "droppinvault_wear_prefs"
    private const val KEY_PRIMARY = "theme_primary"
    private const val KEY_ACCENT = "theme_accent"
    private const val KEY_SURFACE = "theme_surface"
    private const val KEY_ON_PRIMARY = "theme_on_primary"
    private const val KEY_ON_ACCENT = "theme_on_accent"
    private const val KEY_ON_SURFACE = "theme_on_surface"
    private const val KEY_BUTTON_STYLE = "theme_button_style"

    private const val DEFAULT_PRIMARY = 0xFF3B82F6.toInt()
    private const val DEFAULT_ACCENT = 0xFF2DD4BF.toInt()
    private const val DEFAULT_SURFACE = 0xFF8B5CF6.toInt()
    private const val DEFAULT_ON_COLOR = 0xFFFFFFFF.toInt()

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun primary(context: Context): Int = prefs(context).getInt(KEY_PRIMARY, DEFAULT_PRIMARY)
    fun accent(context: Context): Int = prefs(context).getInt(KEY_ACCENT, DEFAULT_ACCENT)
    fun surface(context: Context): Int = prefs(context).getInt(KEY_SURFACE, DEFAULT_SURFACE)
    fun onPrimary(context: Context): Int = prefs(context).getInt(KEY_ON_PRIMARY, DEFAULT_ON_COLOR)
    fun onAccent(context: Context): Int = prefs(context).getInt(KEY_ON_ACCENT, DEFAULT_ON_COLOR)
    fun onSurface(context: Context): Int = prefs(context).getInt(KEY_ON_SURFACE, DEFAULT_ON_COLOR)

    /** One of ButtonStyleOptions' ids from the phone's own ThemeColors.kt ("classic", "capsule",
     * "chamfer", "wild", "leaf", "tactical", "hex", "wave"). Only "capsule" maps to anything
     * visually distinct here — ProtoLayout has no equivalent for the rest's custom silhouettes,
     * so QuickActionTileService falls all of them back to its own default rounded look. */
    fun buttonStyle(context: Context): String = prefs(context).getString(KEY_BUTTON_STYLE, "classic") ?: "classic"

    fun setThemeState(
        context: Context,
        primary: Int,
        accent: Int,
        surface: Int,
        onPrimary: Int,
        onAccent: Int,
        onSurface: Int,
        buttonStyle: String
    ) {
        prefs(context).edit()
            .putInt(KEY_PRIMARY, primary)
            .putInt(KEY_ACCENT, accent)
            .putInt(KEY_SURFACE, surface)
            .putInt(KEY_ON_PRIMARY, onPrimary)
            .putInt(KEY_ON_ACCENT, onAccent)
            .putInt(KEY_ON_SURFACE, onSurface)
            .putString(KEY_BUTTON_STYLE, buttonStyle)
            .apply()
    }
}
