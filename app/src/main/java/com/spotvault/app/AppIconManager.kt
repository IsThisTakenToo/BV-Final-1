package com.spotvault.app

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager

/** Every selectable launcher icon — each maps to one `<activity-alias>` declared in
 * AndroidManifest.xml, all pointing at the same MainActivity. Exactly one is enabled at a
 * time; switching disables the rest so the home screen only ever shows one icon. */
enum class AppIcon(val id: String, val label: String, val aliasSuffix: String) {
    DEFAULT("default", "Default", "IconDefault"),
    DARK("dark", "Dark Mode", "IconDark"),
    EIGHT_BIT("eight_bit", "8-Bit", "Icon8Bit"),
    EIGHT_BIT_DARK("eight_bit_dark", "8-Bit Dark", "Icon8BitDark"),
    NEON("neon", "Neon", "IconNeon"),
    MINIMAL("minimal", "Minimal Line", "IconMinimal"),
    GLASS("glass", "Glass", "IconGlass"),
    EMBER("ember", "Ember Glow", "IconEmber"),
    PASTEL("pastel", "Pastel", "IconPastel");

    fun backgroundRes(): Int = when (this) {
        DEFAULT -> R.drawable.ic_launcher_background
        DARK -> R.drawable.ic_launcher_bg_dark
        EIGHT_BIT -> R.drawable.ic_launcher_bg_8bit
        EIGHT_BIT_DARK -> R.drawable.ic_launcher_bg_8bit_dark
        NEON -> R.drawable.ic_launcher_bg_neon
        MINIMAL -> R.drawable.ic_launcher_bg_minimal
        GLASS -> R.drawable.ic_launcher_bg_glass
        EMBER -> R.drawable.ic_launcher_bg_ember
        PASTEL -> R.drawable.ic_launcher_bg_pastel
    }

    fun foregroundRes(): Int = when (this) {
        DEFAULT -> R.drawable.ic_spotvault_mark
        DARK -> R.drawable.ic_launcher_fg_dark
        EIGHT_BIT -> R.drawable.ic_launcher_fg_8bit
        EIGHT_BIT_DARK -> R.drawable.ic_launcher_fg_8bit_dark
        NEON -> R.drawable.ic_launcher_fg_neon
        MINIMAL -> R.drawable.ic_launcher_fg_minimal
        GLASS -> R.drawable.ic_launcher_fg_glass
        EMBER -> R.drawable.ic_launcher_fg_ember
        PASTEL -> R.drawable.ic_launcher_fg_pastel
    }

    companion object {
        fun fromId(id: String?): AppIcon {
            if (id == "gold") return EMBER
            return entries.firstOrNull { it.id == id } ?: DEFAULT
        }
    }
}

object AppIconManager {
    private const val PREF_KEY = "app_icon"

    fun currentIconId(prefs: SharedPreferences): String =
        premiumGatedId(prefs, AppIcon.fromId(prefs.getString(PREF_KEY, AppIcon.DEFAULT.id)).id, PremiumFreeTier.freeAppIconId)

    /** Re-applies the launcher alias when a stored id no longer matches (e.g. gold → ember). */
    fun migrateLegacyIconIfNeeded(context: Context, prefs: SharedPreferences) {
        val raw = prefs.getString(PREF_KEY, null) ?: return
        val icon = AppIcon.fromId(raw)
        if (raw != icon.id) {
            applyIcon(context, prefs, icon)
        }
    }

    /** Cleans up any icon alias left stuck enabled from a switch that didn't fully land (rare with
     * the current all-DONT_KILL_APP sequence in [applyIcon], but cheap insurance — e.g. leftover
     * residue from testing an earlier, more aggressive version of this code). Safe to call on
     * every launch: a no-op when things are already clean (the common case), DONT_KILL_APP
     * throughout so it can never itself cause the process to restart mid-use. */
    suspend fun reconcileIconState(context: Context, prefs: SharedPreferences) {
        val pm = context.packageManager
        val classNamespace = MainActivity::class.java.name.substringBeforeLast('.')
        fun component(candidate: AppIcon) =
            ComponentName(context.packageName, "$classNamespace.${candidate.aliasSuffix}")

        val expected = AppIcon.fromId(prefs.getString(PREF_KEY, AppIcon.DEFAULT.id))

        fun isEnabled(candidate: AppIcon): Boolean {
            val state = pm.getComponentEnabledSetting(component(candidate))
            return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && candidate == AppIcon.DEFAULT)
        }

        val strays = AppIcon.entries.filter { it != expected && isEnabled(it) }
        if (strays.isEmpty() && isEnabled(expected)) return

        android.util.Log.w(
            "AppIconManager",
            "Reconciling icon state: expected=${expected.id}, found ${strays.size} stray alias(es) enabled"
        )
        if (!isEnabled(expected)) {
            runCatching {
                pm.setComponentEnabledSetting(component(expected), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            }.onFailure { android.util.Log.e("AppIconManager", "Reconcile: failed to enable ${expected.id}", it) }
        }
        strays.forEach { candidate ->
            runCatching {
                pm.setComponentEnabledSetting(component(candidate), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }.onFailure { android.util.Log.e("AppIconManager", "Reconcile: failed to disable stray ${candidate.id}", it) }
        }
    }

    /** Enables the chosen icon's alias and disables every other one. Every call uses
     * DONT_KILL_APP — the caller does its own delayed self-kill afterward (see the Settings
     * confirm handler) to actually restart the process; this never lets Android kill it as a
     * side effect of a flag change.
     *
     * A later version of this deliberately dropped DONT_KILL_APP on the final disable call, on
     * the theory that an OS-triggered kill (rather than a manual self-kill) was needed to refresh
     * the launcher's resolution cache. Comparing against a known-working backup of this file and
     * reproducing the failure live (with Logcat access to a real device) disproved that: the
     * OS-triggered-kill version left PackageManagerService's own state 100% correct — confirmed
     * directly against its own log, not just this app's readback — while still producing
     * endlessly accumulating duplicate icons in the launcher, reproduced on both Samsung's One UI
     * and Nova Launcher. The enable/disable flags were never the bug. Something about how Android
     * tears down the process specifically via that "drop DONT_KILL_APP" pathway is what launchers
     * mishandle, and this plain DONT_KILL_APP-everywhere version doesn't have that problem.
     *
     * Returns false if verification finds a mismatch, or if any call threw outright — some OEM
     * PackageManager implementations are known to silently no-op this instead of throwing. */
    fun applyIcon(context: Context, prefs: SharedPreferences, icon: AppIcon): Boolean {
        val pm = context.packageManager
        // The manifest's ".IconDefault"-style relative alias names resolve against the
        // Gradle `namespace` (where MainActivity is actually compiled), not `applicationId`
        // (what context.packageName returns) — this project's build.gradle sets them to two
        // different values, so deriving the class-name prefix from a real compiled class
        // keeps this correct even if either one changes later.
        val classNamespace = MainActivity::class.java.name.substringBeforeLast('.')
        fun component(candidate: AppIcon) =
            ComponentName(context.packageName, "$classNamespace.${candidate.aliasSuffix}")

        // Enable the target alias FIRST, before disabling any of the others — with DONT_KILL_APP
        // throughout, nothing here should trigger a kill on its own, but if the OS ever did kill
        // mid-sequence anyway, the worst this ordering leaves behind is two icons briefly enabled
        // at once, never zero (no launcher entry point at all until reinstalled).
        var succeeded = true
        runCatching {
            pm.setComponentEnabledSetting(component(icon), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        }.onFailure {
            succeeded = false
            android.util.Log.e("AppIconManager", "Failed to enable alias for ${icon.id}", it)
        }
        AppIcon.entries.filter { it != icon }.forEach { candidate ->
            runCatching {
                pm.setComponentEnabledSetting(component(candidate), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }.onFailure {
                succeeded = false
                android.util.Log.e("AppIconManager", "Failed to disable alias for ${candidate.id}", it)
            }
        }
        prefs.edit().putString(PREF_KEY, icon.id).apply()

        // Not throwing doesn't guarantee the change actually took effect — some OEM PackageManager
        // implementations are known to silently no-op a component-state change under certain
        // restrictions instead of throwing. Reading every alias's actual resulting state right
        // back is the only way to tell "it worked" from "it silently didn't."
        AppIcon.entries.forEach { candidate ->
            val state = pm.getComponentEnabledSetting(component(candidate))
            val effectivelyEnabled = state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && candidate == AppIcon.DEFAULT)
            val expectedEnabled = candidate == icon
            if (effectivelyEnabled != expectedEnabled) {
                succeeded = false
                android.util.Log.e(
                    "AppIconManager",
                    "Mismatch for ${candidate.id}: expected enabled=$expectedEnabled, actual state=$state (effectivelyEnabled=$effectivelyEnabled)"
                )
            }
        }
        return succeeded
    }
}
