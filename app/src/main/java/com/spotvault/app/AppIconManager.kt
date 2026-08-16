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

    /** Enables the chosen icon's alias and disables every other one. This changes what's
     * registered as the launcher entry point, which is disruptive enough that Android
     * typically ends the running process once it's applied — expected, not a bug. */
    fun applyIcon(context: Context, prefs: SharedPreferences, icon: AppIcon) {
        val pm = context.packageManager
        // The manifest's ".IconDefault"-style relative alias names resolve against the
        // Gradle `namespace` (where MainActivity is actually compiled), not `applicationId`
        // (what context.packageName returns) — this project's build.gradle sets them to two
        // different values, so deriving the class-name prefix from a real compiled class
        // keeps this correct even if either one changes later.
        val classNamespace = MainActivity::class.java.name.substringBeforeLast('.')
        fun component(candidate: AppIcon) =
            ComponentName(context.packageName, "$classNamespace.${candidate.aliasSuffix}")

        // Enable the target alias FIRST, before disabling any of the others — this method's own
        // doc note above says Android typically kills the process as soon as a component's
        // enabled state changes, which can happen after any single call in this sequence, not
        // just the last one. Disabling the old aliases before the new one is enabled means a
        // kill partway through could leave every alias disabled at once — no launcher entry
        // point at all, the app gone from the home screen/app drawer until reinstalled. With the
        // target enabled first, the worst a partial run leaves behind is two icons briefly
        // enabled at once, not zero.
        runCatching {
            pm.setComponentEnabledSetting(component(icon), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        }.onFailure {
            android.util.Log.e("AppIconManager", "Failed to enable alias for ${icon.id}", it)
            // The pref write below happens unconditionally regardless of whether this actually
            // succeeded, so without a visible signal here the Settings picker would show the new
            // icon as selected while the real home-screen icon silently never changed — with no
            // way to tell why short of reading Logcat.
            android.widget.Toast.makeText(
                context,
                "Couldn't switch the app icon on this device. Try restarting your phone and picking it again.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
        AppIcon.entries.filter { it != icon }.forEach { candidate ->
            runCatching {
                pm.setComponentEnabledSetting(component(candidate), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }.onFailure {
                android.util.Log.e("AppIconManager", "Failed to disable alias for ${candidate.id}", it)
            }
        }

        // Not throwing doesn't guarantee the change actually took effect — some OEM PackageManager
        // implementations are known to silently no-op a component-state change under certain
        // restrictions instead of throwing. Reading every alias's actual resulting state right
        // back is the only way to tell "it worked" from "it silently didn't" — logged for every
        // alias so a mismatch (the target not landing on ENABLED, or more than one alias ending up
        // enabled at once) is visible in Logcat even though this doesn't change the actual result.
        AppIcon.entries.forEach { candidate ->
            val state = pm.getComponentEnabledSetting(component(candidate))
            val effectivelyEnabled = state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && candidate == AppIcon.DEFAULT)
            val expectedEnabled = candidate == icon
            if (effectivelyEnabled != expectedEnabled) {
                android.util.Log.e(
                    "AppIconManager",
                    "Mismatch for ${candidate.id}: expected enabled=$expectedEnabled, actual state=$state (effectivelyEnabled=$effectivelyEnabled)"
                )
            }
        }
        prefs.edit().putString(PREF_KEY, icon.id).apply()
    }
}
