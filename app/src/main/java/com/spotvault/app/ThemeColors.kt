package com.spotvault.app

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.shape.GenericShape
import kotlin.math.min

enum class SpotVaultButtonShapeSize { Small, Medium, Large, Dialog }

/**
 * Shattered portal silhouette — jagged edges that read as breaking apart
 * while leaving a wide readable center for labels.
 */
fun wildButtonShape(size: SpotVaultButtonShapeSize = SpotVaultButtonShapeSize.Medium): GenericShape {
    val scale = when (size) {
        SpotVaultButtonShapeSize.Small -> 0.75f
        SpotVaultButtonShapeSize.Large -> 1.2f
        SpotVaultButtonShapeSize.Dialog -> 1.3f
        else -> 1f
    }
    return GenericShape { sz, _ ->
        val w = sz.width
        val h = sz.height
        val j = (7f * scale).coerceAtMost(h * 0.14f)

        // Clockwise shattered outline (top → right → bottom → left)
        moveTo(j * 1.4f, 0f)
        lineTo(w * 0.18f, j * 0.35f)
        lineTo(w * 0.32f, 0f)
        lineTo(w * 0.45f, j * 0.55f)
        lineTo(w * 0.58f, 0f)
        lineTo(w * 0.72f, j * 0.3f)
        lineTo(w * 0.86f, 0f)
        lineTo(w - j * 0.4f, j * 0.9f)
        lineTo(w, j * 1.6f)

        lineTo(w - j * 0.45f, h * 0.28f)
        lineTo(w, h * 0.4f)
        lineTo(w - j * 0.7f, h * 0.52f)
        lineTo(w, h * 0.64f)
        lineTo(w - j * 0.35f, h * 0.78f)
        lineTo(w, h - j * 1.4f)

        lineTo(w - j * 1.2f, h)
        lineTo(w * 0.82f, h - j * 0.4f)
        lineTo(w * 0.68f, h)
        lineTo(w * 0.55f, h - j * 0.55f)
        lineTo(w * 0.42f, h)
        lineTo(w * 0.28f, h - j * 0.3f)
        lineTo(w * 0.14f, h)
        lineTo(j * 0.35f, h - j * 0.85f)
        lineTo(0f, h - j * 1.5f)

        lineTo(j * 0.5f, h * 0.72f)
        lineTo(0f, h * 0.58f)
        lineTo(j * 0.75f, h * 0.46f)
        lineTo(0f, h * 0.34f)
        lineTo(j * 0.4f, h * 0.2f)
        lineTo(0f, j * 1.3f)
        close()
    }
}

fun wildButtonPreviewShape(): Shape = wildButtonShape(SpotVaultButtonShapeSize.Medium)

/** Organic leaf-pod silhouette — pointed tip, soft lobes, stem pinch at bottom. */
fun leafButtonShape(size: SpotVaultButtonShapeSize = SpotVaultButtonShapeSize.Medium): GenericShape {
    val scale = when (size) {
        SpotVaultButtonShapeSize.Small -> 0.82f
        SpotVaultButtonShapeSize.Large -> 1.14f
        SpotVaultButtonShapeSize.Dialog -> 1.22f
        else -> 1f
    }
    return GenericShape { sz, _ ->
        val w = sz.width
        val h = sz.height
        val stem = (5f * scale).coerceAtMost(w * 0.06f)
        moveTo(w * 0.5f, h - stem)
        cubicTo(w * 0.12f, h * 0.88f, w * 0.04f, h * 0.58f, w * 0.1f, h * 0.34f)
        cubicTo(w * 0.06f, h * 0.12f, w * 0.28f, stem, w * 0.46f, stem * 1.2f)
        cubicTo(w * 0.5f, stem * 0.35f, w * 0.54f, stem * 1.2f, w * 0.72f, stem)
        cubicTo(w * 0.94f, h * 0.12f, w * 0.9f, h * 0.34f, w * 0.88f, h * 0.58f)
        cubicTo(w * 0.96f, h * 0.88f, w * 0.62f, h - stem * 0.4f, w * 0.5f, h - stem)
        close()
    }
}

fun leafButtonPreviewShape(): Shape = leafButtonShape(SpotVaultButtonShapeSize.Medium)

/** Dog-tag / field placard — clipped corners with a top-center bore notch. */
fun tacticalButtonShape(size: SpotVaultButtonShapeSize = SpotVaultButtonShapeSize.Medium): GenericShape {
    val scale = when (size) {
        SpotVaultButtonShapeSize.Small -> 0.84f
        SpotVaultButtonShapeSize.Large -> 1.16f
        SpotVaultButtonShapeSize.Dialog -> 1.24f
        else -> 1f
    }
    return GenericShape { sz, _ ->
        val w = sz.width
        val h = sz.height
        val cut = (min(w, h) * 0.2f * scale).coerceIn(h * 0.12f, h * 0.42f)
        val notchW = w * 0.11f
        val notchD = cut * 0.55f
        moveTo(cut * 0.55f, 0f)
        lineTo(w * 0.5f - notchW, 0f)
        quadraticTo(w * 0.5f, notchD, w * 0.5f + notchW, 0f)
        lineTo(w - cut * 0.55f, 0f)
        lineTo(w, cut * 0.55f)
        lineTo(w, h - cut)
        lineTo(w - cut, h)
        lineTo(cut, h)
        lineTo(0f, h - cut)
        lineTo(0f, cut * 0.55f)
        close()
    }
}

fun tacticalButtonPreviewShape(): Shape = tacticalButtonShape(SpotVaultButtonShapeSize.Medium)

/** Flat-top hex bolt — six-sided vault plate with angled sides. */
fun hexButtonShape(size: SpotVaultButtonShapeSize = SpotVaultButtonShapeSize.Medium): GenericShape {
    val inset = when (size) {
        SpotVaultButtonShapeSize.Small -> 0.19f
        SpotVaultButtonShapeSize.Large -> 0.13f
        SpotVaultButtonShapeSize.Dialog -> 0.12f
        else -> 0.16f
    }
    return GenericShape { sz, _ ->
        val w = sz.width
        val h = sz.height
        val left = w * inset
        val right = w * (1f - inset)
        moveTo(left, 0f)
        lineTo(right, 0f)
        lineTo(w, h * 0.5f)
        lineTo(right, h)
        lineTo(left, h)
        lineTo(0f, h * 0.5f)
        close()
    }
}

fun hexButtonPreviewShape(): Shape = hexButtonShape(SpotVaultButtonShapeSize.Medium)

/** Tidal scallop — gentle crest-and-trough waves on top and bottom edges. */
fun waveButtonShape(size: SpotVaultButtonShapeSize = SpotVaultButtonShapeSize.Medium): GenericShape {
    val amp = when (size) {
        SpotVaultButtonShapeSize.Small -> 0.065f
        SpotVaultButtonShapeSize.Large -> 0.048f
        SpotVaultButtonShapeSize.Dialog -> 0.042f
        else -> 0.055f
    }
    return GenericShape { sz, _ ->
        val w = sz.width
        val h = sz.height
        val crest = h * amp
        val topY = h * 0.14f
        val botY = h * 0.86f
        moveTo(0f, topY)
        cubicTo(w * 0.24f, topY - crest, w * 0.76f, topY - crest, w, topY)
        lineTo(w, botY)
        cubicTo(w * 0.76f, botY + crest, w * 0.24f, botY + crest, 0f, botY)
        close()
    }
}

fun waveButtonPreviewShape(): Shape = waveButtonShape(SpotVaultButtonShapeSize.Medium)

/** Hoists whichever seasonal item(s) cover the current calendar month to the very front of
 * [items], pushes seasonal items for OTHER months to the very end, and leaves everything with
 * no [activeMonthsOf] tag (the standard, non-holiday options) in its original relative order in
 * the middle. Lets Halloween/Christmas picks surface on their own each season without
 * cluttering the picker the rest of the year. [currentMonth] defaults to the real device month
 * (1-12, matching [java.time.LocalDate.getMonthValue]) but is overridable for testing/preview.
 * A list rather than a single month so a theme can span more than one — Christmas runs
 * November-December, not just December. */
fun <T> getSeasonallySortedItems(
    items: List<T>,
    currentMonth: Int = java.time.LocalDate.now().monthValue,
    activeMonthsOf: (T) -> List<Int>?
): List<T> {
    val standard = items.filter { activeMonthsOf(it) == null }
    val matching = items.filter { activeMonthsOf(it)?.contains(currentMonth) == true }
    val nonMatching = items.filter { val m = activeMonthsOf(it); m != null && currentMonth !in m }
    return matching + standard + nonMatching
}

/** [activeMonths] (1-12 each) marks a seasonal color theme — see [getSeasonallySortedItems].
 * Null for the year-round standard presets. */
data class ColorThemeOption(val id: String, val primary: Color, val accent: Color, val activeMonths: List<Int>? = null)

/** Short labels shown under preset color swatches in Settings (custom stays unlabeled). */
val PresetThemeDisplayNames = mapOf(
    "purple_teal" to "Neon",
    "camo_hunt" to "Camo",
    "forest_nature" to "Forest",
    "neon_pink" to "Pink",
    "yin_yang" to "Yin Yang",
    "crimson_gold" to "Sunset",
    "gold_cobalt" to "Cobalt",
    "halloween" to "Halloween",
    "christmas" to "Christmas"
)

fun normalizeButtonStyle(raw: String?): String = when (raw) {
    "wild", "wacky" -> "wild"
    "capsule", "chamfer", "leaf", "tactical", "hex", "wave" -> raw
    "classic", null -> "classic"
    else -> "classic"
}

const val VAULT_DISPLAY_NAME_PREF = "vault_display_name"
const val DEFAULT_VAULT_DISPLAY_NAME = "DropPin Vault"
const val MAX_VAULT_DISPLAY_NAME_LENGTH = 16

fun normalizeVaultDisplayName(raw: String?): String {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return DEFAULT_VAULT_DISPLAY_NAME
    return trimmed.take(MAX_VAULT_DISPLAY_NAME_LENGTH)
}

fun loadVaultDisplayNameFromPrefs(prefs: android.content.SharedPreferences): String {
    val saved = prefs.getString(VAULT_DISPLAY_NAME_PREF, null)
    // Anyone who saved a custom name before the BeaconVault -> DropPin Vault rename is stuck
    // on the old brand name forever otherwise — a saved value that still exactly matches the
    // pre-rename default was never a deliberate user choice, so treat it as unset.
    if (saved == "BeaconVault") return DEFAULT_VAULT_DISPLAY_NAME
    return normalizeVaultDisplayName(saved)
}

const val APP_LOCK_ENABLED_PREF = "app_lock_enabled"

const val HAS_CUSTOM_THEME_PREF = "has_custom_theme"

const val VAULT_VIEW_MODE_PREF = "vault_view_mode"

enum class VaultViewMode { LIST, GRID }

fun loadVaultViewMode(prefs: android.content.SharedPreferences): VaultViewMode {
    return when (prefs.getString(VAULT_VIEW_MODE_PREF, "list")) {
        "grid" -> VaultViewMode.GRID
        else -> VaultViewMode.LIST
    }
}

fun saveVaultViewMode(prefs: android.content.SharedPreferences, mode: VaultViewMode) {
    prefs.edit().putString(VAULT_VIEW_MODE_PREF, if (mode == VaultViewMode.GRID) "grid" else "list").apply()
}

fun loadButtonStyleFromPrefs(prefs: android.content.SharedPreferences): String {
    val raw = prefs.getString("button_style", "classic") ?: "classic"
    if (raw == "wacky") {
        prefs.edit().putString("button_style", "classic").apply()
        return "classic"
    }
    return premiumGatedId(prefs, normalizeButtonStyle(raw), PremiumFreeTier.freeButtonStyleId)
}

fun spotVaultButtonShape(size: SpotVaultButtonShapeSize = SpotVaultButtonShapeSize.Medium): Shape {
    return when (ThemeState.buttonStyle) {
        "capsule" -> RoundedCornerShape(percent = 50)
        "chamfer" -> when (size) {
            SpotVaultButtonShapeSize.Small -> CutCornerShape(10.dp)
            SpotVaultButtonShapeSize.Large -> CutCornerShape(18.dp)
            SpotVaultButtonShapeSize.Dialog -> CutCornerShape(20.dp)
            else -> CutCornerShape(14.dp)
        }
        "wild" -> if (size == SpotVaultButtonShapeSize.Small) {
            RoundedCornerShape(14.dp)
        } else {
            wildButtonShape(size)
        }
        "leaf" -> leafButtonShape(size)
        "tactical" -> tacticalButtonShape(size)
        "hex" -> hexButtonShape(size)
        "wave" -> waveButtonShape(size)
        else -> when (size) {
            SpotVaultButtonShapeSize.Small -> RoundedCornerShape(12.dp)
            SpotVaultButtonShapeSize.Large -> RoundedCornerShape(20.dp)
            SpotVaultButtonShapeSize.Dialog -> RoundedCornerShape(28.dp)
            else -> RoundedCornerShape(16.dp)
        }
    }
}

fun spotVaultDialogShape(): RoundedCornerShape = RoundedCornerShape(28.dp)

fun spotVaultThemeButtonShape(size: SpotVaultButtonShapeSize = SpotVaultButtonShapeSize.Medium): RoundedCornerShape {
    return when (size) {
        SpotVaultButtonShapeSize.Small -> RoundedCornerShape(12.dp)
        SpotVaultButtonShapeSize.Large -> RoundedCornerShape(20.dp)
        SpotVaultButtonShapeSize.Dialog -> RoundedCornerShape(28.dp)
        else -> RoundedCornerShape(16.dp)
    }
}

val ButtonStyleOptions = listOf(
    Triple("classic", "Rounded", "Soft corners — default"),
    Triple("capsule", "Capsule", "Sleek full-pill buttons"),
    Triple("chamfer", "Chamfer", "Cut-corner vault geometry"),
    Triple("wild", "Wild Portal", "Shattered evaporating frame — wild, readable"),
    Triple("leaf", "Wildwood", "Leaf-pod silhouette with stem"),
    Triple("tactical", "Tactical", "Dog-tag plate with bore notch"),
    Triple("hex", "Hex Plate", "Six-sided vault bolt head"),
    Triple("wave", "Tidal", "Scalloped crest-and-trough edges")
)

data class SpotVaultColorScheme(
    val Void: Color,
    val Deep: Color,
    val Surface: Color,
    val Elevated: Color,
    val Glass: Color,
    val Primary: Color,
    val PrimaryBright: Color,
    val PrimaryDeep: Color,
    val Teal: Color,
    val TealSoft: Color,
    val TealDeep: Color,
    val Ink: Color,
    val OnSurface: Color,
    val Muted: Color,
    val Outline: Color,
    val Danger: Color,
    val id: String
)

val PurpleTealColors = SpotVaultColorScheme(
    Void = Color(0xFF0C0714),
    Deep = Color(0xFF130B21),
    Surface = Color(0xFF1A1228),
    Elevated = Color(0xFF241938),
    Glass = Color(0xFF1E152D),
    Primary = Color(0xFF6B2FFF),
    PrimaryBright = Color(0xFF9D75FF),
    PrimaryDeep = Color(0xFF4A1BBF),
    Teal = Color(0xFF00F0FF),
    TealSoft = Color(0xFF85F8FF),
    TealDeep = Color(0xFF00B8CC),
    Ink = Color(0xFF0A0F1A),
    OnSurface = Color(0xFFF0EAF8),
    Muted = Color(0xFFA39CB4),
    Outline = Color(0xFF3B2E52),
    Danger = Color(0xFFFF4E64),
    id = "purple_teal"
)

val NeonPinkColors = SpotVaultColorScheme(
    Void = Color(0xFF12060D),
    Deep = Color(0xFF1B0912),
    Surface = Color(0xFF241019),
    Elevated = Color(0xFF321522),
    Glass = Color(0xFF2B121D),
    Primary = Color(0xFFFF008C),
    PrimaryBright = Color(0xFFFF5CB8),
    PrimaryDeep = Color(0xFFB80066),
    Teal = Color(0xFF00F0FF),
    TealSoft = Color(0xFF85F8FF),
    TealDeep = Color(0xFF00B8CC),
    Ink = Color(0xFF12040B),
    OnSurface = Color(0xFFFCEAF3),
    Muted = Color(0xFFB99AAC),
    Outline = Color(0xFF4A2438),
    Danger = Color(0xFFFF4E64),
    id = "neon_pink"
)

// Void/Deep/Surface/Elevated/Glass are the "black" side of the palette — the backgrounds. Primary,
// PrimaryBright, and PrimaryDeep exist to be visible ON TOP of those backgrounds (every shared
// component that highlights a border, icon, or badge with them assumes they're bright, the same
// way Purple/Teal's PrimaryBright glows against ITS dark backgrounds) — so on a black-and-white
// theme they have to land on the *white* side of the spectrum, not a second, slightly-less-dark
// gray. Originally all three were near-black here, which made GlassSurface's card border, the
// Vault's "no photo" placeholder, and anything else leaning on PrimaryBright for contrast render
// almost invisibly against Void.
val YinYangColors = SpotVaultColorScheme(
    Void = Color(0xFF050505),
    Deep = Color(0xFF0C0C0C),
    Surface = Color(0xFF141414),
    Elevated = Color(0xFF1E1E1E),
    Glass = Color(0xFF181818),
    Primary = Color(0xFFD8D8D8),
    PrimaryBright = Color(0xFFFFFFFF),
    PrimaryDeep = Color(0xFF4A4A4A),
    Teal = Color(0xFFE8E8E8),
    TealSoft = Color(0xFFF7F7F7),
    TealDeep = Color(0xFFC4C4C4),
    Ink = Color(0xFF0A0A0A),
    OnSurface = Color(0xFFF0F0F0),
    Muted = Color(0xFF8A8A8A),
    Outline = Color(0xFF333333),
    Danger = Color(0xFFFF5555),
    id = "yin_yang"
)

/** Maps retired preset ids to their replacements (e.g. Citrus → Yin Yang). */
fun normalizeColorThemeId(id: String?): String = when (id) {
    "lime_magenta" -> "yin_yang"
    null, "" -> "gold_cobalt"
    else -> id
}

fun loadColorThemeFromPrefs(prefs: android.content.SharedPreferences): String {
    val raw = prefs.getString("color_theme", "gold_cobalt") ?: "gold_cobalt"
    val normalized = normalizeColorThemeId(raw)
    if (normalized != raw) {
        prefs.edit().putString("color_theme", normalized).apply()
    }
    return premiumGatedId(prefs, normalized, PremiumFreeTier.freeColorThemeIds, "gold_cobalt")
}

val CrimsonGoldColors = SpotVaultColorScheme(
    Void = Color(0xFF150A0D),
    Deep = Color(0xFF1F0F14),
    Surface = Color(0xFF29151B),
    Elevated = Color(0xFF391D25),
    Glass = Color(0xFF321A20),
    Primary = Color(0xFFFF2E63),
    PrimaryBright = Color(0xFFFF7A99),
    PrimaryDeep = Color(0xFFC2003F),
    Teal = Color(0xFFFFD100),
    TealSoft = Color(0xFFFFE566),
    TealDeep = Color(0xFFCCA700),
    Ink = Color(0xFF160A0C),
    OnSurface = Color(0xFFFCEAEE),
    Muted = Color(0xFFC29CA6),
    Outline = Color(0xFF522733),
    Danger = Color(0xFFFF3D3D),
    id = "crimson_gold"
)

val GoldCobaltColors = SpotVaultColorScheme(
    Void = Color(0xFF060A14),
    Deep = Color(0xFF0A1020),
    Surface = Color(0xFF10182C),
    Elevated = Color(0xFF17213A),
    Glass = Color(0xFF141C32),
    Primary = Color(0xFF2962FF),
    PrimaryBright = Color(0xFF6B94FF),
    PrimaryDeep = Color(0xFF1739B0),
    Teal = Color(0xFFFFD100),
    TealSoft = Color(0xFFFFE566),
    TealDeep = Color(0xFFCCA700),
    Ink = Color(0xFF0B0F1A),
    OnSurface = Color(0xFFEAF0FF),
    Muted = Color(0xFF9AA8C2),
    Outline = Color(0xFF29365A),
    Danger = Color(0xFFFF4E4E),
    id = "gold_cobalt"
)

val CamoHuntColors = SpotVaultColorScheme(
    Void = Color(0xFF0C0D07),
    Deep = Color(0xFF14150E),
    Surface = Color(0xFF1D2016),
    Elevated = Color(0xFF272C1C),
    Glass = Color(0xFF212519),
    Primary = Color(0xFF5C6B2F),
    PrimaryBright = Color(0xFF93A653),
    PrimaryDeep = Color(0xFF394321),
    Teal = Color(0xFFFF6A00),
    TealSoft = Color(0xFFFFA35C),
    TealDeep = Color(0xFFCC5500),
    Ink = Color(0xFF0A0B06),
    OnSurface = Color(0xFFEDEAD6),
    Muted = Color(0xFFA9A488),
    Outline = Color(0xFF40482F),
    Danger = Color(0xFFFF3B30),
    id = "camo_hunt"
)

val ForestNatureColors = SpotVaultColorScheme(
    Void = Color(0xFF071410),
    Deep = Color(0xFF0C1F17),
    Surface = Color(0xFF12291D),
    Elevated = Color(0xFF1B3727),
    Glass = Color(0xFF163020),
    Primary = Color(0xFF2E7D4F),
    PrimaryBright = Color(0xFF6FCF97),
    PrimaryDeep = Color(0xFF1B5C36),
    Teal = Color(0xFF29B6F6),
    TealSoft = Color(0xFF81D4FA),
    TealDeep = Color(0xFF0288D1),
    Ink = Color(0xFF08150F),
    OnSurface = Color(0xFFE8F5E9),
    Muted = Color(0xFF9FC2AC),
    Outline = Color(0xFF2C4A38),
    Danger = Color(0xFFFF5252),
    id = "forest_nature"
)

val HalloweenColors = SpotVaultColorScheme(
    Void = Color(0xFF0A0612),
    Deep = Color(0xFF120B1C),
    Surface = Color(0xFF1A1128),
    Elevated = Color(0xFF241A34),
    Glass = Color(0xFF1E1430),
    Primary = Color(0xFFFF7A1A),
    PrimaryBright = Color(0xFFFFA94D),
    PrimaryDeep = Color(0xFFB84E00),
    Teal = Color(0xFF9B30FF),
    TealSoft = Color(0xFFC48CFF),
    TealDeep = Color(0xFF6A1FB8),
    Ink = Color(0xFF0A0610),
    OnSurface = Color(0xFFF5EAFF),
    Muted = Color(0xFFAFA0C4),
    Outline = Color(0xFF3A2A52),
    Danger = Color(0xFF39FF6A),
    id = "halloween"
)

val ChristmasColors = SpotVaultColorScheme(
    Void = Color(0xFF060F0B),
    Deep = Color(0xFF0A1810),
    Surface = Color(0xFF102218),
    Elevated = Color(0xFF163021),
    Glass = Color(0xFF12281B),
    Primary = Color(0xFFE0233D),
    PrimaryBright = Color(0xFFFF6B7F),
    PrimaryDeep = Color(0xFFA10E24),
    Teal = Color(0xFF1FA34D),
    TealSoft = Color(0xFF6FDB8F),
    TealDeep = Color(0xFF0E7A32),
    Ink = Color(0xFF06120C),
    OnSurface = Color(0xFFF0FFF4),
    Muted = Color(0xFF9DC2AC),
    Outline = Color(0xFF244A34),
    Danger = Color(0xFFFF4E4E),
    id = "christmas"
)

/** [activeMonths] (1-12 each) marks a seasonal option — see [getSeasonallySortedItems]. Null
 * for the year-round standard styles. */
data class VaultIconOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val activeMonths: List<Int>? = null
)

val VaultIconStyles = listOf(
    VaultIconOption("classic", "Classic Vault", "Six-spoke vault wheel"),
    VaultIconOption("hex", "Hex Vault", "Hexagonal chamber door"),
    VaultIconOption("bank", "Bank Safe", "Safe dial with handle"),
    VaultIconOption("wacky", "Chaos Portal", "Wobbly star portal dial"),
    VaultIconOption("camo", "Scope Vault", "Rugged compass & reticle dial"),
    VaultIconOption("leaf", "Wildwood Vault", "Foliage ring growth dial"),
    VaultIconOption("sonar", "Sonar Vault", "Expanding ripple scan rings"),
    VaultIconOption("maglock", "Mag Lock", "Magnetic bolts pulse and seal the vault iris"),
    VaultIconOption("halloween", "Haunted Vault", "Harvest moon crypt with a witch-hat jack-o'-lantern dial and candy-corn ring", activeMonths = listOf(10)),
    VaultIconOption("christmas", "Winter Vault", "Glossy ornament dial with aurora, holly bolts, and a North Star lock", activeMonths = listOf(11, 12))
)

/** WCAG-standard relative luminance from sRGB channels in [0f, 1f] — gamma-corrects each channel
 * before weighting, the same formula Compose's own Color.luminance() implements. Kept as plain
 * math (no Compose Color dependency) so the widget path (raw ARGB ints via RemoteViews, which
 * can't use @Composable getters) and the in-app Compose path share one implementation, rather
 * than silently drifting apart the way they had — the widget path used a completely different
 * (non-WCAG, perceptual-luma) formula and threshold than the in-app one before this. */
private fun srgbChannelToLinear(c: Float): Float =
    if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()

fun relativeLuminance(r: Float, g: Float, b: Float): Float =
    0.2126f * srgbChannelToLinear(r) + 0.7152f * srgbChannelToLinear(g) + 0.0722f * srgbChannelToLinear(b)

/** WCAG contrast ratio between two relative-luminance values — always >= 1, higher means more
 * contrast: (L_lighter + 0.05) / (L_darker + 0.05). */
fun contrastRatio(l1: Float, l2: Float): Float {
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}

/** True if [lightLum] gives higher actual contrast against [backgroundLum] than [darkLum] does —
 * NOT a fixed luminance cutoff. A flat 0.4 or 0.5 threshold (what this used to be) picks the
 * wrong option for backgrounds roughly in the 0.18-0.4 luminance band: the true WCAG crossover
 * point, where contrast(background, white) == contrast(background, black), works out to
 * ln(21)/2 - ish math landing near 0.179, not a round number. */
fun lightWinsContrast(backgroundLum: Float, lightLum: Float, darkLum: Float): Boolean =
    contrastRatio(backgroundLum, lightLum) >= contrastRatio(backgroundLum, darkLum)

fun androidx.compose.ui.graphics.Color.toArgbInt(): Int = toArgb()

fun buildCustomColorScheme(primary: Color, accent: Color): SpotVaultColorScheme {
    val base = PurpleTealColors
    val outline = lerp(primary.copy(alpha = 0.35f), Color(0xFF1A1228), 0.72f)
    return base.copy(
        Primary = primary,
        PrimaryBright = lerp(primary, Color.White, 0.28f),
        PrimaryDeep = lerp(primary, Color.Black, 0.32f),
        Teal = accent,
        TealSoft = lerp(accent, Color.White, 0.35f),
        TealDeep = lerp(accent, Color.Black, 0.28f),
        Outline = outline,
        id = "custom"
    )
}

val LocalSpotVaultColors = staticCompositionLocalOf { PurpleTealColors }

object ThemeState {
    var currentTheme by mutableStateOf("gold_cobalt")
    var isAmoled by mutableStateOf(false)
    var vaultIconStyle by mutableStateOf("classic")
    var compassStyle by mutableStateOf("classic")
    var buttonStyle by mutableStateOf("classic")
    var reduceAnimations by mutableStateOf(false)
    /** Set once at launch from ActivityManager.isLowRamDevice — splash particle budgets only. */
    var lowRamDevice by mutableStateOf(false)
    var customPrimaryArgb by mutableIntStateOf(0xFF6B2FFF.toInt())
    var customAccentArgb by mutableIntStateOf(0xFF00F0FF.toInt())
    var textScale by mutableStateOf(1.15f)
    var fontFamilyId by mutableStateOf(AppFontOptions.DEFAULT_ID)
    var backgroundPatternId by mutableStateOf(BackgroundPattern.GRADIENT_MESH.id)

    /** Whether the user has the Dynamic Color toggle on — independent of [currentTheme]/the
     * "color_theme" pref entirely, so turning it off always cleanly reverts to whichever preset
     * or custom theme was already selected, with no extra state to restore. */
    var dynamicColorEnabled by mutableStateOf(false)
    /** False below Android 12 (no system Material You API) or if extraction ever fails. */
    var dynamicColorAvailable by mutableStateOf(false)
    var dynamicPrimaryArgb by mutableIntStateOf(0xFF2962FF.toInt())
    var dynamicAccentArgb by mutableIntStateOf(0xFFFFD100.toInt())

    fun customScheme(): SpotVaultColorScheme =
        buildCustomColorScheme(Color(customPrimaryArgb), Color(customAccentArgb))

    fun dynamicScheme(): SpotVaultColorScheme =
        buildCustomColorScheme(Color(dynamicPrimaryArgb), Color(dynamicAccentArgb)).copy(id = "dynamic")
}

fun isDynamicColorAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Reads the OS's Material You palette — extracted from the current wallpaper by the system
 * itself, the same mechanism every other dynamic-color app on the device uses — and stores it
 * on [ThemeState]. Safe to call unconditionally on every app start/resume; a cheap no-op below
 * Android 12, and errs safely to "unavailable" rather than a half-applied theme if extraction
 * ever throws. */
fun refreshDynamicColors(context: android.content.Context) {
    if (!isDynamicColorAvailable()) {
        ThemeState.dynamicColorAvailable = false
        return
    }
    try {
        val dynamic = androidx.compose.material3.dynamicDarkColorScheme(context)
        ThemeState.dynamicPrimaryArgb = dynamic.primary.toArgbInt()
        ThemeState.dynamicAccentArgb = dynamic.secondary.toArgbInt()
        ThemeState.dynamicColorAvailable = true
    } catch (e: Exception) {
        e.printStackTrace()
        ThemeState.dynamicColorAvailable = false
    }
}

/** Every selectable app-wide font — all built into stock Android (Roboto/Noto family
 * variants plus the system script faces), so picking one never needs a network fetch or any
 * external dependency; it's always available and always free. */
object AppFontOptions {
    const val DEFAULT_ID = "sans_serif"

    data class Option(
        val id: String,
        val label: String,
        val systemFamilyName: String,
        val weight: androidx.compose.ui.text.font.FontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    )

    // The five "sans-serif-*" weight variants (medium/black/light/thin) used to be looked up by
    // that literal family name via DeviceFontFamilyName — which only renders distinctly on
    // devices whose system font config actually registers each as its own family (stock AOSP
    // Roboto does). Plenty of OEM skins (Samsung One UI, MIUI, etc.) don't redeclare all of
    // those names and silently fall back to the same base face for every one of them, which is
    // exactly the "they all look the same" bug reported. Requesting the base "sans-serif" family
    // with an explicit FontWeight instead lets Skia synthesize the weight (faux-bold/faux-thin)
    // when a distinct static font isn't available, so it reliably renders differently everywhere
    // rather than silently no-op'ing on some devices. Condensed/serif/monospace/cursive are true
    // distinct system families (not weight variants of the same face), so those keep resolving
    // by name — they were never the ones the user reported as identical.
    val all = listOf(
        Option("sans_serif", "Classic Sans", "sans-serif", androidx.compose.ui.text.font.FontWeight.Normal),
        Option("sans_serif_medium", "Modern Medium", "sans-serif", androidx.compose.ui.text.font.FontWeight.Medium),
        Option("sans_serif_condensed", "Condensed", "sans-serif-condensed"),
        Option("sans_serif_black", "Bold Impact", "sans-serif", androidx.compose.ui.text.font.FontWeight.Black),
        Option("sans_serif_light", "Clean Light", "sans-serif", androidx.compose.ui.text.font.FontWeight.Light),
        Option("sans_serif_thin", "Ultra Thin", "sans-serif", androidx.compose.ui.text.font.FontWeight.Thin),
        Option("serif", "Classic Serif", "serif"),
        Option("serif_monospace", "Serif Mono", "serif-monospace"),
        Option("monospace", "Code Mono", "monospace"),
        Option("cursive", "Cursive Script", "cursive")
    )

    fun optionFor(id: String?): Option = all.firstOrNull { it.id == id } ?: all.first()

    fun familyFor(id: String?): androidx.compose.ui.text.font.FontFamily =
        optionFor(id).let { familyFromSystemName(it.systemFamilyName, it.weight) }

    @OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
    private fun familyFromSystemName(
        systemFamilyName: String,
        weight: androidx.compose.ui.text.font.FontWeight
    ): androidx.compose.ui.text.font.FontFamily =
        androidx.compose.ui.text.font.FontFamily(
            androidx.compose.ui.text.font.Font(androidx.compose.ui.text.font.DeviceFontFamilyName(systemFamilyName), weight = weight)
        )
}

data class SettingsCategoryMeta(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val keywords: List<String>
)

val SettingsCategories = listOf(
    SettingsCategoryMeta("autopark", "Automatic Parking", "Save a pin when your car Bluetooth disconnects", Icons.Default.Bluetooth,
        listOf("automatic parking", "auto park", "bluetooth", "car", "quiet zone", "background location", "motion", "fitness", "activity recognition", "walking")),
    SettingsCategoryMeta("vehicles", "Vehicles", "Manage vehicles, colors, and Bluetooth links", Icons.Default.DirectionsCar,
        listOf("vehicle", "vehicles", "car", "truck", "motorcycle", "default vehicle", "bluetooth")),
    SettingsCategoryMeta("gps", "Location & Precision", "Accuracy, screen wake, and distance units", Icons.Default.LocationOn,
        listOf("gps", "camera", "sharing", "accuracy", "screen on", "distance")),
    SettingsCategoryMeta("quickpin", "Quick-Pin Behavior", "Configure one-tap saving and auto-naming rules", Icons.Default.PushPin,
        listOf("quick pin", "quick track", "auto close", "auto-close", "naming", "manage quick pins", "home screen", "widget", "instant pin", "instant track")),
    SettingsCategoryMeta("vault", "Vault", "Snapshot stats, custom tags, and default save settings", Icons.Default.Inventory2,
        listOf("vault", "snapshot", "custom tag", "default tab", "delete confirm", "save defaults")),
    SettingsCategoryMeta("appearance", "Appearance & Themes", "Colors, icons, fonts, and animations", Icons.Default.Palette,
        listOf("theme", "color", "red", "amber", "green", "contrast", "motion", "button style", "vault icon", "vault name", "font", "splash")),
    SettingsCategoryMeta("timer", "Sounds & Alerts", "Timer and notification behavior", Icons.Default.Notifications,
        listOf("timer", "notification", "alert", "expire", "sound", "haptic", "vibration", "vibrate", "feel", "pattern", "click", "feedback")),
    SettingsCategoryMeta("widgets", "Widgets & Home Screen", "Launcher widgets that mirror your vault", Icons.Default.Widgets,
        listOf("widget", "launcher", "refresh", "sync")),
    SettingsCategoryMeta("premium", "Premium Unlocks", "Explore available upgrades and features", Icons.Default.Star,
        listOf("premium", "pro", "upgrade", "unlock")),
    SettingsCategoryMeta("data", "Backup, Data & Privacy", "Export, import, and clear app data", Icons.Default.Storage,
        listOf("backup", "export", "import", "restore", "privacy", "delete all", "app lock", "biometric", "fingerprint", "face", "pin", "security", "lock", "gpx", "waypoint", "gps app", "garmin")),
    SettingsCategoryMeta("help", "Help & Guide", "How everything works, explained simply", Icons.AutoMirrored.Filled.HelpOutline,
        listOf("help", "guide", "how to", "tutorial", "faq", "explain", "quick pin", "widget", "vault")),
    SettingsCategoryMeta("about", "About & Credits", "App version and open-source icon credits", Icons.Default.Info,
        listOf("about", "credits", "license", "licenses", "version", "open source", "attribution", "font awesome", "tabler", "material"))
)
