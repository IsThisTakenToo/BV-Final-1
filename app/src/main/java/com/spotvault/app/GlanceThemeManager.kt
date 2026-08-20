package com.spotvault.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.RowScope
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider
import androidx.datastore.preferences.core.Preferences

/**
 * Glance-observed theme fingerprint. Custom color saves often leave `color_theme == "custom"`
 * unchanged, so widgets must observe these keys (not only SharedPreferences) to recompose and
 * repaint when primary/accent ints change.
 */
object WidgetGlanceThemeKeys {
    val THEME_CACHE = androidx.datastore.preferences.core.stringPreferencesKey("theme_cache_key")
    val PRIMARY = androidx.datastore.preferences.core.intPreferencesKey("theme_primary")
    val ACCENT = androidx.datastore.preferences.core.intPreferencesKey("theme_accent")
    val SURFACE = androidx.datastore.preferences.core.intPreferencesKey("theme_surface")
    val ON_PRIMARY = androidx.datastore.preferences.core.intPreferencesKey("theme_on_primary")
    val ON_ACCENT = androidx.datastore.preferences.core.intPreferencesKey("theme_on_accent")
    val ON_SURFACE = androidx.datastore.preferences.core.intPreferencesKey("theme_on_surface")
    val MUTED = androidx.datastore.preferences.core.intPreferencesKey("theme_muted")
    val BUTTON_STYLE = androidx.datastore.preferences.core.stringPreferencesKey("theme_button_style")

    fun write(glancePrefs: androidx.datastore.preferences.core.MutablePreferences, theme: GlanceWidgetTheme) {
        glancePrefs[THEME_CACHE] = theme.cacheKey()
        glancePrefs[PRIMARY] = theme.palette.primary
        glancePrefs[ACCENT] = theme.palette.accent
        glancePrefs[SURFACE] = theme.palette.surface
        glancePrefs[ON_PRIMARY] = theme.palette.onPrimary
        glancePrefs[ON_ACCENT] = theme.palette.onAccent
        glancePrefs[ON_SURFACE] = theme.palette.onSurface
        glancePrefs[MUTED] = theme.palette.muted
        glancePrefs[BUTTON_STYLE] = theme.buttonStyle
    }
}

/**
 * Reads active accent colors, dark surface gradients, and button corner shape from
 * [SpotVaultPrefs] (same persistence layer as existing home-screen widgets) and
 * exposes Glance styling helpers.
 */
data class GlanceWidgetTheme(
    val palette: WidgetPalette,
    val buttonStyle: String,
    val buttonCornerRadius: Dp,
    val gradientTopArgb: Int,
    val gradientBottomArgb: Int,
    val containerCornerRadius: Dp = 16.dp
) {
    /** Stable key so Glance [remember] blocks invalidate when any theme input changes. */
    fun cacheKey(): String = buildString {
        append(palette.primary).append(':')
        append(palette.accent).append(':')
        append(palette.surface).append(':')
        append(palette.onPrimary).append(':')
        append(palette.onAccent).append(':')
        append(buttonStyle).append(':')
        append(gradientTopArgb).append(':')
        append(gradientBottomArgb)
    }

    fun accentProvider(): ColorProvider = ColorProvider(Color(palette.accent))
    fun onSurfaceProvider(): ColorProvider = ColorProvider(Color(palette.onSurface))
    fun mutedProvider(): ColorProvider = ColorProvider(Color(palette.muted))
    fun surfaceProvider(): ColorProvider = ColorProvider(Color(palette.surface))
    fun primaryProvider(): ColorProvider = ColorProvider(Color(palette.primary))

    fun actionLabelColor(fgArgb: Int): ColorProvider {
        val argb = WidgetThemeHelper.actionLabelColor(fgArgb, buttonStyle)
        return ColorProvider(Color(argb))
    }
}

object GlanceThemeManager {

    private const val PREFS_NAME = "SpotVaultPrefs"

    fun load(context: Context): GlanceWidgetTheme {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return buildTheme(
            palette = WidgetThemeHelper.paletteFromPrefs(prefs),
            buttonStyle = WidgetThemeHelper.buttonStyleFromPrefs(prefs)
        )
    }

    /**
     * Prefer colors written into Glance Preferences by [WidgetThemeHelper.syncGlanceState]
     * so custom wheel saves invalidate composition even when `color_theme` stays `"custom"`.
     * Falls back to SharedPreferences when Glance keys are missing (first paint).
     */
    fun load(context: Context, glancePrefs: Preferences): GlanceWidgetTheme {
        val primary = glancePrefs[WidgetGlanceThemeKeys.PRIMARY]
        val accent = glancePrefs[WidgetGlanceThemeKeys.ACCENT]
        if (primary != null && accent != null) {
            val palette = WidgetPalette(
                primary = primary,
                accent = accent,
                surface = glancePrefs[WidgetGlanceThemeKeys.SURFACE] ?: WidgetThemeHelper.blendSurfaceForWidget(primary),
                onSurface = glancePrefs[WidgetGlanceThemeKeys.ON_SURFACE] ?: 0xFFECE6F5.toInt(),
                muted = glancePrefs[WidgetGlanceThemeKeys.MUTED] ?: 0xFFA89BB8.toInt(),
                onPrimary = glancePrefs[WidgetGlanceThemeKeys.ON_PRIMARY] ?: WidgetThemeHelper.readableOnForWidget(primary),
                onAccent = glancePrefs[WidgetGlanceThemeKeys.ON_ACCENT] ?: WidgetThemeHelper.readableOnForWidget(accent)
            )
            val buttonStyle = glancePrefs[WidgetGlanceThemeKeys.BUTTON_STYLE]
                ?: WidgetThemeHelper.buttonStyleFromPrefs(
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                )
            return buildTheme(palette, buttonStyle)
        }
        return load(context)
    }

    private fun buildTheme(palette: WidgetPalette, buttonStyle: String): GlanceWidgetTheme {
        val (top, bottom) = gradientColorsForPalette(palette)
        return GlanceWidgetTheme(
            palette = palette,
            buttonStyle = buttonStyle,
            buttonCornerRadius = cornerRadiusForStyle(buttonStyle),
            gradientTopArgb = top,
            gradientBottomArgb = bottom
        )
    }

    /** Hardcoded fallback so a theme-load failure never blanks a Glance widget. */
    fun defaultTheme(): GlanceWidgetTheme {
        return buildTheme(
            palette = WidgetPalette(
                primary = 0xFF6B2FFF.toInt(),
                accent = 0xFF00F0FF.toInt(),
                surface = 0xFF1A1228.toInt(),
                onSurface = 0xFFECE6F5.toInt(),
                muted = 0xFFA89BB8.toInt(),
                onPrimary = 0xFFFFFFFF.toInt(),
                onAccent = 0xFF150030.toInt()
            ),
            buttonStyle = "rounded"
        )
    }

    fun cornerRadiusForStyle(buttonStyle: String): Dp = when (buttonStyle) {
        "capsule" -> 24.dp
        "chamfer" -> 6.dp
        "wild" -> 14.dp
        "leaf" -> 18.dp
        "tactical" -> 4.dp
        "hex" -> 8.dp
        "wave" -> 16.dp
        else -> 12.dp
    }

    private fun gradientColorsForPalette(palette: WidgetPalette): Pair<Int, Int> {
        val top = blendArgb(palette.primary, palette.surface, 0.28f)
        val bottom = blendArgb(palette.surface, 0xFF050508.toInt(), 0.72f)
        return top to bottom
    }

    private fun blendArgb(base: Int, overlay: Int, overlayWeight: Float): Int {
        val w = overlayWeight.coerceIn(0f, 1f)
        val inv = 1f - w
        val r = (android.graphics.Color.red(base) * inv + android.graphics.Color.red(overlay) * w).toInt()
        val g = (android.graphics.Color.green(base) * inv + android.graphics.Color.green(overlay) * w).toInt()
        val b = (android.graphics.Color.blue(base) * inv + android.graphics.Color.blue(overlay) * w).toInt()
        return android.graphics.Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    fun createGradientBitmap(widthPx: Int, heightPx: Int, theme: GlanceWidgetTheme, density: Float = 2.75f): Bitmap {
        val (safeW, safeH) = WidgetThemeHelper.clampWidgetBitmapDims(widthPx, heightPx)
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = safeW.toFloat()
        val h = safeH.toFloat()
        val radius = theme.containerCornerRadius.value * density
        val rect = RectF(0f, 0f, w, h)
        val clip = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(clip)
        canvas.drawRect(rect, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, h,
                theme.gradientTopArgb,
                theme.gradientBottomArgb,
                Shader.TileMode.CLAMP
            )
        })
        canvas.drawRect(
            RectF(0f, 0f, w, h * 0.35f),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, h * 0.35f,
                    android.graphics.Color.argb(28, 255, 255, 255),
                    android.graphics.Color.argb(0, 255, 255, 255),
                    Shader.TileMode.CLAMP
                )
            }
        )
        canvas.restore()

        val borderColor = blendArgb(theme.palette.primary, 0xFFFFFFFF.toInt(), 0.42f)
        canvas.drawPath(
            clip,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.25f * density
                color = android.graphics.Color.argb(
                    90,
                    android.graphics.Color.red(borderColor),
                    android.graphics.Color.green(borderColor),
                    android.graphics.Color.blue(borderColor)
                )
            }
        )
        return bitmap
    }

    fun themedContainerModifier(theme: GlanceWidgetTheme): GlanceModifier =
        GlanceModifier
            .fillMaxSize()
            .cornerRadius(theme.containerCornerRadius)
}

/** Paints the widget's themed card background (rounded gradient + hairline border) sized to the
 * widget's real current bounds via [LocalSize] — only accurate under [SizeMode.Exact]. Without
 * this the container painted nothing at all, so resizing the widget just left a transparent void
 * below whatever content it happened to wrap around, with the home screen wallpaper showing
 * through instead of a proper card. */
@Composable
fun GlanceThemedBackground(
    theme: GlanceWidgetTheme,
    modifier: GlanceModifier = GlanceModifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val widthPx = WidgetThemeHelper.dpToPx(context, LocalSize.current.width.value).coerceAtLeast(1)
    val heightPx = WidgetThemeHelper.dpToPx(context, LocalSize.current.height.value).coerceAtLeast(1)
    val gradient = remember(widthPx, heightPx, theme.cacheKey()) {
        GlanceThemeManager.createGradientBitmap(widthPx, heightPx, theme)
    }
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            provider = ImageProvider(gradient),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize()
        )
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            content()
        }
    }
}

@Composable
fun GlanceThemedActionButton(
    label: String,
    theme: GlanceWidgetTheme,
    backgroundArgb: Int,
    foregroundArgb: Int,
    icon: WidgetThemeHelper.QuickActionIcon,
    modifier: GlanceModifier = GlanceModifier,
    outlined: Boolean = false,
    borderColorArgb: Int? = null,
    compactLabel: Boolean = true,
    /** Prefer readable label size — shrinks the icon and gives text more of the button. */
    labelEmphasis: Boolean = false,
    /** Quick Pin / Quick Track — larger icon + larger label than Snap / Pin. */
    primaryAction: Boolean = false,
    /** Premium dashboard widget — richer gradients, icon badges, and label polish. */
    premiumPolish: Boolean = false,
    /** Forces the label's own color regardless of what [WidgetThemeHelper.widgetButtonTextColor]
     * would otherwise auto-compute from the background's luminance — that auto-contrast logic is
     * exactly why label color could vary button-to-button in a way that didn't match; this lets a
     * specific button's words override it while its icon keeps the normal computed color. */
    labelColorOverrideArgb: Int? = null,
    /** Observed Glance revision — ties button bitmap cache to [WidgetThemeHelper.syncGlanceState]. */
    themeRevision: Long = 0L,
    slotWidthPx: Int? = null,
    buttonHeightDp: Dp = 40.dp,
    expandVertically: Boolean = true,
    onClick: Action
) {
    val context = LocalContext.current
    val textColor = WidgetThemeHelper.widgetButtonTextColor(foregroundArgb, theme.buttonStyle, outlined, backgroundArgb)
    val themeKey = theme.cacheKey()
    val widthPx = slotWidthPx ?: WidgetThemeHelper.dpToPx(context, 96f)
    val heightPx = WidgetThemeHelper.dpToPx(context, buttonHeightDp.value)
    val bitmap = remember(
        label,
        themeKey,
        themeRevision,
        backgroundArgb,
        textColor,
        icon,
        outlined,
        borderColorArgb,
        compactLabel,
        labelEmphasis,
        primaryAction,
        premiumPolish,
        labelColorOverrideArgb,
        widthPx,
        heightPx
    ) {
        WidgetThemeHelper.renderQuickActionButtonBitmap(
            context = context,
            label = if (compactLabel) label else label.uppercase(),
            bgColor = backgroundArgb,
            fgColor = textColor,
            buttonStyle = theme.buttonStyle,
            widthPx = widthPx,
            heightPx = heightPx,
            icon = icon,
            outlined = outlined,
            borderColor = borderColorArgb,
            labelEmphasis = labelEmphasis,
            primaryAction = primaryAction,
            premiumPolish = premiumPolish,
            labelColorArgb = labelColorOverrideArgb
        )
    }
    val boxModifier = if (expandVertically) {
        modifier.fillMaxHeight()
    } else {
        modifier.height(buttonHeightDp)
    }
    Box(
        modifier = boxModifier.clickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = label,
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}

/** Equal-width slot inside a Glance [Row] — [defaultWeight] only exists in [RowScope]. */
fun RowScope.widgetActionSlot(): GlanceModifier =
    GlanceModifier.defaultWeight().fillMaxHeight().padding(horizontal = 3.dp)
