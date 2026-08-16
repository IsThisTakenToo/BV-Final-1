package com.spotvault.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Path
import android.graphics.RectF
import android.widget.RemoteViews
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.max
import kotlin.math.min

data class WidgetPalette(
    val primary: Int,
    val accent: Int,
    val surface: Int,
    val onSurface: Int,
    val muted: Int,
    val onPrimary: Int,
    val onAccent: Int,
    val danger: Int = 0xFFFF4E64.toInt()
)

object WidgetThemeHelper {

    const val WIDGET_REVISION_PREF = "widget_state_revision"

    // refreshAllWidgets is called from many synchronous, non-suspend call sites — some of them
    // hot paths like TimerService's once-a-second countdown tick, and one (ClearNotificationReceiver)
    // a BroadcastReceiver with a strict main-thread execution budget. It used to block the caller
    // via runBlocking; nothing actually depends on it finishing before continuing, so it's fired
    // off on this scope instead of blocking whichever thread happened to call it.
    private val widgetRefreshScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
    )

    fun buttonStyleFromPrefs(prefs: SharedPreferences): String =
        loadButtonStyleFromPrefs(prefs)

    /** The theme id widgets actually render with — "gold_cobalt" when widget theme sync is off,
     * otherwise whatever the app's active color theme is. Shared by [paletteFromPrefs] and
     * anything that needs to know the effective theme id itself (e.g. whether it's Yin Yang). */
    fun effectiveThemeId(prefs: SharedPreferences): String {
        if (!prefs.getBoolean("widget_theme_sync", true)) return "gold_cobalt"
        val normalized = normalizeColorThemeId(prefs.getString("color_theme", "gold_cobalt"))
        return premiumGatedId(prefs, normalized, PremiumFreeTier.freeColorThemeIds, "gold_cobalt")
    }

    fun paletteFromPrefs(prefs: SharedPreferences): WidgetPalette =
        paletteForTheme(effectiveThemeId(prefs), prefs)

    private data class ThemeColorBundle(
        val primary: Int,
        val accent: Int,
        val surface: Int,
        val onSurface: Int = 0xFFECE6F5.toInt(),
        val muted: Int = 0xFFA89BB8.toInt()
    )

    private fun paletteForTheme(theme: String, prefs: SharedPreferences): WidgetPalette {
        val colors = when (theme) {
            "purple_teal" -> ThemeColorBundle(0xFF6B2FFF.toInt(), 0xFF00F0FF.toInt(), 0xFF1A1228.toInt())
            "camo_hunt" -> ThemeColorBundle(0xFF5C6B2F.toInt(), 0xFFFF6A00.toInt(), 0xFF1D2016.toInt())
            "forest_nature" -> ThemeColorBundle(0xFF2E7D4F.toInt(), 0xFF29B6F6.toInt(), 0xFF12291D.toInt())
            "neon_pink" -> ThemeColorBundle(0xFFFF008C.toInt(), 0xFF00F0FF.toInt(), 0xFF241019.toInt())
            // primary must stay visibly lighter than surface — this is used as a gradient/border
            // color for foreground elements (the widget "no photo" placeholder card, quick-action
            // buttons) meant to pop against the dark backdrop, the same role every other theme's
            // primary plays. It was originally a near-black gray here, which — blended with an
            // already-dark surface — rendered those elements almost perfectly invisible.
            "yin_yang" -> ThemeColorBundle(
                primary = 0xFF9A9A9A.toInt(),
                accent = 0xFFE8E8E8.toInt(),
                surface = 0xFF141414.toInt(),
                onSurface = 0xFFF0F0F0.toInt(),
                muted = 0xFF8A8A8A.toInt()
            )
            "crimson_gold" -> ThemeColorBundle(0xFFFF2E63.toInt(), 0xFFFFD100.toInt(), 0xFF29151B.toInt())
            "gold_cobalt" -> ThemeColorBundle(0xFF2962FF.toInt(), 0xFFFFD100.toInt(), 0xFF10182C.toInt())
            "halloween" -> ThemeColorBundle(0xFFFF7A1A.toInt(), 0xFF9B30FF.toInt(), 0xFF1A1128.toInt())
            "christmas" -> ThemeColorBundle(0xFFE0233D.toInt(), 0xFF1FA34D.toInt(), 0xFF102218.toInt())
            "custom" -> {
                val p = prefs.getInt("custom_primary_color", 0xFF6B2FFF.toInt())
                val a = prefs.getInt("custom_accent_color", 0xFF00F0FF.toInt())
                ThemeColorBundle(p, a, blendSurface(p))
            }
            else -> ThemeColorBundle(0xFF6B2FFF.toInt(), 0xFF00F0FF.toInt(), 0xFF1A1228.toInt())
        }
        return WidgetPalette(
            primary = colors.primary,
            accent = colors.accent,
            surface = colors.surface,
            onSurface = colors.onSurface,
            muted = colors.muted,
            onPrimary = onPrimaryFor(colors.primary, prefs, theme),
            onAccent = onAccentFor(colors.accent, prefs, theme)
        )
    }

    private fun onPrimaryFor(primary: Int, prefs: SharedPreferences, themeId: String): Int {
        if (themeId == "custom") {
            textModeToArgb(prefs.getString("custom_on_primary_mode", "auto") ?: "auto")?.let { return it }
        }
        return readableOn(primary)
    }

    private fun onAccentFor(accent: Int, prefs: SharedPreferences, themeId: String): Int {
        if (themeId == "custom") {
            textModeToArgb(prefs.getString("custom_on_accent_mode", "auto") ?: "auto")?.let { return it }
        }
        return readableOn(accent)
    }

    private fun blendSurface(primary: Int): Int {
        val r = (Color.red(primary) * 0.12f + 0x0C).toInt().coerceIn(0, 255)
        val g = (Color.green(primary) * 0.12f + 0x07).toInt().coerceIn(0, 255)
        val b = (Color.blue(primary) * 0.12f + 0x14).toInt().coerceIn(0, 255)
        return android.graphics.Color.rgb(r, g, b)
    }

    /** Public wrappers for Glance theme hydration from observed widget state. */
    fun blendSurfaceForWidget(primary: Int): Int = blendSurface(primary)
    fun readableOnForWidget(color: Int): Int = readableOn(color)

    private fun readableOn(color: Int): Int {
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f
        val luminance = 0.299f * r + 0.587f * g + 0.114f * b
        return if (luminance > 0.62f) 0xFF150030.toInt() else Color.WHITE
    }

    private fun setBackgroundColor(views: RemoteViews, viewId: Int, color: Int) {
        views.setInt(viewId, "setBackgroundColor", color or 0xFF000000.toInt())
    }

    enum class QuickActionIcon { PIN, PIN_DROP, CAMERA, SHARE, TRACK, FOUND, NAVIGATE, COMPASS, MAP }

    /** Small icon chip for native Glance widget buttons. */
    fun renderQuickActionIconBitmap(
        context: Context,
        icon: QuickActionIcon,
        color: Int,
        sizeDp: Float = 24f
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = dpToPx(context, sizeDp).coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawQuickActionIcon(canvas, icon, sizePx / 2f, sizePx / 2f, sizePx * 0.72f, color)
        return bitmap
    }

    /** Bitmap action chip for Glance widgets — icon, two-line label, user button shape, optional outline. */
    fun renderQuickActionButtonBitmap(
        context: Context,
        label: String,
        bgColor: Int,
        fgColor: Int,
        buttonStyle: String,
        widthPx: Int,
        heightPx: Int,
        icon: QuickActionIcon,
        outlined: Boolean = false,
        borderColor: Int? = null,
        labelEmphasis: Boolean = false,
        primaryAction: Boolean = false,
        premiumPolish: Boolean = false,
        /** Overrides just the label text's color, independent of [fgColor] (which still colors
         * the icon and the premium icon badge). Some buttons need dark text on a bright accent
         * background but should keep the icon in the normal foreground color. */
        labelColorArgb: Int? = null
    ): Bitmap {
        val labelColor = labelColorArgb ?: fgColor
        val density = context.resources.displayMetrics.density
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (buttonStyle != "wild" && !outlined) {
            drawPhysicalButtonShadow(canvas, widthPx, heightPx, bgColor, buttonStyle, density)
        }
        drawButtonBackground(canvas, widthPx, heightPx, bgColor, buttonStyle, density, premiumPolish)
        if (outlined && borderColor != null) {
            drawButtonOutline(canvas, widthPx, heightPx, borderColor, buttonStyle, density)
        }
        if (buttonStyle != "wild") {
            drawButtonPolish(canvas, widthPx, heightPx, buttonStyle, density, richer = true, primaryAction = primaryAction || premiumPolish)
            if (!outlined) {
                drawPhysicalButtonRim(canvas, widthPx, heightPx, bgColor, buttonStyle, density)
            }
        }

        val insets = buttonContentInsets(widthPx, heightPx, buttonStyle, density)
        val contentW = insets.right - insets.left
        val contentH = insets.bottom - insets.top
        val singleWord = !label.contains(' ')

        if (labelEmphasis && singleWord && !primaryAction) {
            // Snap / Pin — icon beside label with a premium badge. Was capped at 28dp max
            // against a bold single-word label that can render considerably larger than that,
            // which read as an icon too small/faint to register next to its own text.
            val iconSize = (contentH * 0.64f).coerceIn(20f * density, 38f * density)
            val gap = 6f * density
            // Snap/Pin's slots are narrow enough that the label naturally fills them, but Found/
            // Navigate reuse this same layout in a full-width row — without a cap here, the text
            // greedily grew to fill that entire remaining width while the icon stayed capped
            // small, which is exactly what read as "stretched": a tiny icon stranded at the left
            // edge next to oversized text. Capping the label's width relative to the button's own
            // height (not its width) keeps text sized proportionately to the icon regardless of
            // how wide the container is, and centering the icon+label pair as a group — rather
            // than anchoring the icon left and letting text fill to the right edge — keeps both
            // balanced in a wide button instead of pinned off to one side.
            val labelWidthCap = (contentH * 4.2f).coerceAtMost(contentW - iconSize - gap)
            val labelSizing = measureQuickActionLabel(
                label = label,
                maxWidth = labelWidthCap,
                maxHeight = contentH,
                density = density,
                emphasize = true,
                primaryAction = false,
                premiumPolish = premiumPolish
            )
            val groupWidth = iconSize + gap + labelSizing.width
            val groupLeft = insets.left + (contentW - groupWidth) / 2f
            val iconCx = groupLeft + iconSize * 0.5f
            val iconCy = (insets.top + insets.bottom) / 2f
            drawQuickActionIcon(canvas, icon, iconCx, iconCy, iconSize, fgColor, premiumPolish)
            drawQuickActionLabel(
                canvas = canvas,
                label = label,
                fgColor = labelColor,
                left = groupLeft + iconSize + gap,
                right = groupLeft + iconSize + gap + labelSizing.width,
                top = insets.top,
                bottom = insets.bottom,
                density = density,
                buttonStyle = buttonStyle,
                emphasize = true,
                primaryAction = false,
                premiumPolish = premiumPolish
            )
        } else {
            // Quick Pin / Quick Track (or default): stacked. Primary actions get bigger icon + text.
            val iconFrac = when {
                primaryAction -> if (premiumPolish) 0.36f else 0.34f
                labelEmphasis -> 0.30f
                else -> 0.46f
            }
            val iconSize = (min(contentW, contentH) * iconFrac).coerceIn(
                11f * density,
                when {
                    primaryAction && premiumPolish -> 44f * density
                    primaryAction -> 40f * density
                    labelEmphasis -> 34f * density
                    else -> 52f * density
                }
            )
            val iconTopMargin = contentH * (if (primaryAction) 0.02f else if (labelEmphasis) 0.04f else 0.08f)
            val iconCy = insets.top + iconTopMargin + iconSize * 0.5f
            val iconCx = (insets.left + insets.right) / 2f
            drawQuickActionIcon(canvas, icon, iconCx, iconCy, iconSize, fgColor, premiumPolish)

            val labelTop = (insets.top + iconTopMargin + iconSize + (if (primaryAction) 1.5f else if (labelEmphasis) 3f else 5f) * density)
                .coerceAtMost(insets.bottom - 3f * density)
            drawQuickActionLabel(
                canvas = canvas,
                label = label,
                fgColor = fgColor,
                left = insets.left,
                right = insets.right,
                top = labelTop,
                bottom = insets.bottom,
                density = density,
                buttonStyle = buttonStyle,
                emphasize = labelEmphasis || primaryAction,
                primaryAction = primaryAction,
                premiumPolish = premiumPolish
            )
        }
        return bitmap
    }

    private fun blendArgb(base: Int, overlay: Int, overlayWeight: Float): Int {
        val w = overlayWeight.coerceIn(0f, 1f)
        val inv = 1f - w
        return Color.rgb(
            (Color.red(base) * inv + Color.red(overlay) * w).toInt().coerceIn(0, 255),
            (Color.green(base) * inv + Color.green(overlay) * w).toInt().coerceIn(0, 255),
            (Color.blue(base) * inv + Color.blue(overlay) * w).toInt().coerceIn(0, 255)
        )
    }

    private fun drawPhysicalButtonShadow(
        canvas: Canvas,
        width: Int,
        height: Int,
        color: Int,
        buttonStyle: String,
        density: Float
    ) {
        val path = buttonShapePath(width, height, buttonStyle, density) ?: return
        val wall = blendArgb(color, Color.BLACK, 0.42f)
        val cast = blendArgb(color, Color.BLACK, 0.62f)
        canvas.save()
        canvas.translate(0f, 4f * density)
        canvas.drawPath(
            path,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.argb(92, 0, 0, 0)
            }
        )
        canvas.translate(0f, -1.4f * density)
        canvas.drawPath(
            path,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.argb(82, Color.red(cast), Color.green(cast), Color.blue(cast))
            }
        )
        canvas.translate(0f, -1.2f * density)
        canvas.drawPath(
            path,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = wall
            }
        )
        canvas.restore()
    }

    private fun drawPhysicalButtonRim(
        canvas: Canvas,
        width: Int,
        height: Int,
        color: Int,
        buttonStyle: String,
        density: Float
    ) {
        val path = buttonShapePath(width, height, buttonStyle, density) ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        val light = blendArgb(color, Color.WHITE, 0.52f)
        val dark = blendArgb(color, Color.BLACK, 0.48f)
        canvas.drawPath(
            path,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.85f * density
                shader = android.graphics.LinearGradient(
                    0f, 0f, w, h,
                    intArrayOf(
                        Color.argb(240, Color.red(light), Color.green(light), Color.blue(light)),
                        Color.argb(140, Color.red(color), Color.green(color), Color.blue(color)),
                        Color.argb(220, Color.red(dark), Color.green(dark), Color.blue(dark))
                    ),
                    floatArrayOf(0f, 0.45f, 1f),
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
        )
    }

    private data class ContentInsets(val left: Float, val top: Float, val right: Float, val bottom: Float)

    private fun buttonContentInsets(width: Int, height: Int, buttonStyle: String, density: Float): ContentInsets {
        val w = width.toFloat()
        val h = height.toFloat()
        return when (buttonStyle) {
            "wild" -> ContentInsets(w * 0.14f, h * 0.24f, w * 0.86f, h * 0.82f)
            "leaf" -> ContentInsets(w * 0.14f, h * 0.16f, w * 0.86f, h * 0.90f)
            "tactical" -> ContentInsets(w * 0.12f, h * 0.14f, w * 0.88f, h * 0.90f)
            "chamfer" -> ContentInsets(w * 0.10f, h * 0.12f, w * 0.90f, h * 0.92f)
            "capsule" -> ContentInsets(w * 0.10f, h * 0.14f, w * 0.90f, h * 0.92f)
            "hex" -> ContentInsets(w * 0.14f, h * 0.14f, w * 0.86f, h * 0.90f)
            "wave" -> ContentInsets(w * 0.08f, h * 0.18f, w * 0.92f, h * 0.88f)
            else -> ContentInsets(w * 0.08f, h * 0.12f, w * 0.92f, h * 0.92f)
        }
    }

    private fun drawButtonPolish(
        canvas: Canvas,
        width: Int,
        height: Int,
        buttonStyle: String,
        density: Float,
        richer: Boolean = false,
        primaryAction: Boolean = false
    ) {
        if (buttonStyle == "wild") return
        val path = buttonShapePath(width, height, buttonStyle, density) ?: return
        canvas.save()
        canvas.clipPath(path)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(
            RectF(0f, 0f, w, h * 0.14f),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.LinearGradient(
                    0f, 0f, 0f, h * 0.14f,
                    Color.argb(if (richer) 70 else 58, 255, 255, 255),
                    Color.argb(0, 255, 255, 255),
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
        )
        canvas.drawRect(
            RectF(0f, h * 0.78f, w, h),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.LinearGradient(
                    0f, h * 0.78f, 0f, h,
                    Color.argb(0, 0, 0, 0),
                    Color.argb(if (primaryAction) 78 else 68, 0, 0, 0),
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
        )
        canvas.restore()
    }

    private fun drawButtonOutline(
        canvas: Canvas,
        width: Int,
        height: Int,
        borderColor: Int,
        buttonStyle: String,
        density: Float
    ) {
        val path = buttonShapePath(width, height, buttonStyle, density) ?: return
        canvas.drawPath(
            path,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.25f * density
                color = borderColor
            }
        )
    }

    private fun buttonShapePath(width: Int, height: Int, buttonStyle: String, density: Float): Path? {
        val w = width.toFloat()
        val h = height.toFloat()
        return when (buttonStyle) {
            "wild" -> {
                val j = 5f * density
                Path().apply {
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
            "leaf" -> {
                val stem = h * 0.04f
                Path().apply {
                    moveTo(w * 0.5f, h - stem)
                    cubicTo(w * 0.12f, h * 0.88f, w * 0.04f, h * 0.58f, w * 0.1f, h * 0.34f)
                    cubicTo(w * 0.06f, h * 0.12f, w * 0.28f, stem, w * 0.46f, stem * 1.2f)
                    cubicTo(w * 0.5f, stem * 0.35f, w * 0.54f, stem * 1.2f, w * 0.72f, stem)
                    cubicTo(w * 0.94f, h * 0.12f, w * 0.9f, h * 0.34f, w * 0.88f, h * 0.58f)
                    cubicTo(w * 0.96f, h * 0.88f, w * 0.62f, h - stem * 0.4f, w * 0.5f, h - stem)
                    close()
                }
            }
            "tactical" -> {
                val cut = (min(width, height) * 0.2f).coerceIn(h * 0.12f, h * 0.42f)
                val notchW = w * 0.11f
                val notchD = cut * 0.55f
                Path().apply {
                    moveTo(cut * 0.55f, 0f)
                    lineTo(w * 0.5f - notchW, 0f)
                    quadTo(w * 0.5f, notchD, w * 0.5f + notchW, 0f)
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
            "chamfer" -> {
                val cut = 16f * density
                Path().apply {
                    moveTo(cut, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h - cut)
                    lineTo(w - cut, h)
                    lineTo(0f, h)
                    lineTo(0f, cut)
                    close()
                }
            }
            "capsule" -> {
                Path().apply {
                    addRoundRect(RectF(0f, 0f, w, h), h / 2f, h / 2f, Path.Direction.CW)
                }
            }
            "hex" -> {
                val inset = 0.16f
                val left = w * inset
                val right = w * (1f - inset)
                Path().apply {
                    moveTo(left, 0f)
                    lineTo(right, 0f)
                    lineTo(w, h * 0.5f)
                    lineTo(right, h)
                    lineTo(left, h)
                    lineTo(0f, h * 0.5f)
                    close()
                }
            }
            "wave" -> {
                val crest = h * 0.055f
                val topY = h * 0.14f
                val botY = h * 0.86f
                Path().apply {
                    moveTo(0f, topY)
                    cubicTo(w * 0.24f, topY - crest, w * 0.76f, topY - crest, w, topY)
                    lineTo(w, botY)
                    cubicTo(w * 0.76f, botY + crest, w * 0.24f, botY + crest, 0f, botY)
                    close()
                }
            }
            else -> {
                Path().apply {
                    addRoundRect(RectF(0f, 0f, w, h), 14f * density, 14f * density, Path.Direction.CW)
                }
            }
        }
    }

    internal fun drawQuickActionIcon(
        canvas: Canvas,
        icon: QuickActionIcon,
        cx: Float,
        cy: Float,
        size: Float,
        color: Int,
        premiumPolish: Boolean = false
    ) {
        // Previously drew a dark translucent disc behind the icon whenever it was white, plus a
        // second, separate disc for any premiumPolish button regardless of icon color — meant as
        // a contrast aid, but it read as a literal "black circle" behind the icon instead,
        // actively hurting readability (especially once an icon is intentionally colored dark to
        // sit on a bright background, where a dark disc behind it is exactly backwards). Removed
        // outright rather than only silencing it for dark icons.
        val useIconShadow = color == Color.WHITE
        // Rasterizes the exact same Material icon glyph used on the in-app home screen for the
        // equivalent action, instead of a hand-drawn approximation — guarantees the widget icon
        // is pixel-faithful to what a user already recognizes from the app itself, and sidesteps
        // any more rounds of "that still doesn't quite look right" on a from-scratch redraw.
        val vector = when (icon) {
            QuickActionIcon.PIN -> Icons.Default.LocationOn // same as the in-app "Pin" button
            QuickActionIcon.PIN_DROP -> Icons.Default.PinDrop // same as the in-app "Quick Pin" button
            QuickActionIcon.CAMERA -> Icons.Default.CameraAlt // same as "Snap"
            QuickActionIcon.SHARE -> Icons.Default.Share
            QuickActionIcon.TRACK -> Icons.Default.Route // same as "Quick Track"
            QuickActionIcon.FOUND -> Icons.Default.CheckCircle
            QuickActionIcon.NAVIGATE -> Icons.Default.Navigation
            QuickActionIcon.COMPASS -> Icons.Default.Explore
            QuickActionIcon.MAP -> Icons.Default.Map
        }
        drawMaterialIcon(canvas, vector, cx, cy, size, color, useIconShadow)
    }

    private val vectorIconPathCache = mutableMapOf<ImageVector, android.graphics.Path>()

    /** Converts a Compose [ImageVector]'s path data into a fillable [android.graphics.Path],
     * walking every [VectorPath] in its node tree (Material icons are a single path in practice,
     * but this doesn't assume that). Cached per icon since the same handful of icons are
     * rasterized repeatedly across every button/theme/size combination. */
    private fun androidPathFor(imageVector: ImageVector): android.graphics.Path =
        vectorIconPathCache.getOrPut(imageVector) {
            val composePath = ComposePath()
            fun walk(nodes: Iterable<VectorNode>) {
                for (node in nodes) {
                    when (node) {
                        is VectorGroup -> walk(node)
                        is VectorPath -> composePath.addPath(PathParser().addPathNodes(node.pathData).toPath())
                    }
                }
            }
            walk(imageVector.root)
            composePath.asAndroidPath()
        }

    private fun drawMaterialIcon(canvas: Canvas, imageVector: ImageVector, cx: Float, cy: Float, size: Float, color: Int, withShadow: Boolean = false) {
        val path = androidPathFor(imageVector)
        val viewportSize = max(imageVector.viewportWidth, imageVector.viewportHeight)
        val scale = if (viewportSize > 0f) size / viewportSize else 1f
        canvas.save()
        canvas.translate(cx - imageVector.viewportWidth * scale / 2f, cy - imageVector.viewportHeight * scale / 2f)
        canvas.scale(scale, scale)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
            if (withShadow) {
                // Same contrast boost the hand-drawn icons used to get for free from their own
                // Paint's setShadowLayer — a white glyph can otherwise wash out against a light
                // or busy button background.
                setShadowLayer(1.6f / scale, 0f, 0.8f / scale, Color.argb(180, 0, 0, 0))
            }
        }
        canvas.drawPath(path, fillPaint)
        canvas.restore()
    }

    private data class QuickActionLabelSizing(val textSize: Float, val lines: List<String>, val width: Float)

    /** Shared sizing pass behind both [measureQuickActionLabel] and [drawQuickActionLabel] — same
     * shrink-to-fit rules (max start size, two-line splitting, minimum floor) either way, so a
     * pre-measurement (used to lay out an icon+label group before drawing either) can never
     * disagree with what actually gets drawn. */
    private fun sizeQuickActionLabel(
        label: String,
        maxWidth: Float,
        maxHeight: Float,
        density: Float,
        emphasize: Boolean,
        primaryAction: Boolean,
        premiumPolish: Boolean
    ): QuickActionLabelSizing {
        val effectiveMaxWidth = maxWidth.coerceAtLeast(1f)
        val effectiveMaxHeight = (maxHeight * 0.96f).coerceAtLeast(1f)
        val parts = label.split(" ", limit = 2)
        val twoLine = parts.size == 2 && parts[1].isNotBlank()
        val lines = if (twoLine) listOf(parts[0], parts[1]) else listOf(label)

        val maxStartSize = when {
            primaryAction && twoLine && premiumPolish -> 68f * density
            primaryAction && twoLine -> 64f * density
            primaryAction && premiumPolish -> 62f * density
            primaryAction -> 58f * density
            emphasize && !twoLine && premiumPolish -> 38f * density
            emphasize && !twoLine -> 34f * density
            emphasize -> 40f * density
            twoLine -> 42f * density
            else -> 46f * density
        }
        var textSize = maxStartSize
        val minSize = when {
            primaryAction && premiumPolish -> 16f * density
            primaryAction -> 15f * density
            emphasize && premiumPolish -> 11f * density
            emphasize -> 10f * density
            else -> 8.5f * density
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            letterSpacing = when {
                premiumPolish && primaryAction -> 0.05f
                premiumPolish && emphasize -> 0.03f
                primaryAction -> 0.04f
                emphasize -> 0.02f
                else -> 0f
            }
        }

        fun fits(size: Float): Boolean {
            paint.textSize = size
            val fm = paint.fontMetrics
            val lineH = fm.descent - fm.ascent
            val totalH = lineH * lines.size + (if (twoLine) lineH * 0.06f else 0f)
            if (totalH > effectiveMaxHeight) return false
            return lines.all { paint.measureText(it) <= effectiveMaxWidth }
        }

        while (textSize > minSize && !fits(textSize)) {
            textSize -= 0.4f * density
        }
        textSize = textSize.coerceAtLeast(minSize)
        paint.textSize = textSize
        val width = lines.maxOf { paint.measureText(it) }
        return QuickActionLabelSizing(textSize, lines, width)
    }

    /** Measures what [drawQuickActionLabel] would render without drawing it — used to lay out an
     * icon+label pair as a centered group before either is actually drawn (see the
     * labelEmphasis-single-word branch in [renderQuickActionButtonBitmap]). */
    private fun measureQuickActionLabel(
        label: String,
        maxWidth: Float,
        maxHeight: Float,
        density: Float,
        emphasize: Boolean = false,
        primaryAction: Boolean = false,
        premiumPolish: Boolean = false
    ): QuickActionLabelSizing =
        sizeQuickActionLabel(label, maxWidth, maxHeight, density, emphasize, primaryAction, premiumPolish)

    private fun drawQuickActionLabel(
        canvas: Canvas,
        label: String,
        fgColor: Int,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        density: Float,
        buttonStyle: String,
        emphasize: Boolean = false,
        primaryAction: Boolean = false,
        premiumPolish: Boolean = false
    ) {
        val maxWidth = (right - left).coerceAtLeast(1f)
        val maxHeight = ((bottom - top) * 0.96f).coerceAtLeast(1f)
        val sizing = sizeQuickActionLabel(label, maxWidth, maxHeight, density, emphasize, primaryAction, premiumPolish)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fgColor
            // A true black-weight font file, not Typeface.DEFAULT+BOLD — that combination makes
            // Android synthetically thicken the regular weight by skewing/re-stroking it rather
            // than drawing an actual bold glyph, which is exactly what reads as mushy/"cheap" at
            // the large sizes these labels render at. "sans-serif-black" is a real bundled system
            // font family (no asset to ship), so this is a strict quality upgrade at zero cost.
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            textSize = sizing.textSize
            if (buttonStyle == "wild" || fgColor == Color.WHITE) {
                setShadowLayer(2f, 0f, 1f * density, Color.argb(190, 0, 0, 0))
            } else if (emphasize || primaryAction || premiumPolish) {
                val lum = Color.red(fgColor) * 0.299f + Color.green(fgColor) * 0.587f + Color.blue(fgColor) * 0.114f
                if (lum > 160f) {
                    setShadowLayer(
                        when {
                            premiumPolish && primaryAction -> 2.2f * density
                            primaryAction -> 1.8f * density
                            premiumPolish -> 1.6f * density
                            emphasize -> 1.4f * density
                            else -> 2.2f * density
                        },
                        0f,
                        1f * density,
                        Color.argb(
                            when {
                                premiumPolish && primaryAction -> 150
                                primaryAction -> 130
                                premiumPolish -> 120
                                emphasize -> 100
                                else -> 150
                            },
                            0, 0, 0
                        )
                    )
                }
            }
            letterSpacing = when {
                premiumPolish && primaryAction -> 0.05f
                premiumPolish && emphasize -> 0.03f
                primaryAction -> 0.04f
                emphasize -> 0.02f
                else -> 0f
            }
        }

        val fm = paint.fontMetrics
        val lineH = fm.descent - fm.ascent
        val totalH = lineH * sizing.lines.size
        var y = top + (maxHeight - totalH) / 2f - fm.ascent
        val cx = (left + right) / 2f
        sizing.lines.forEach { line ->
            canvas.drawText(line, cx, y, paint)
            y += lineH
        }
    }

    private fun drawButtonBackground(
        canvas: Canvas,
        width: Int,
        height: Int,
        color: Int,
        buttonStyle: String,
        density: Float,
        premiumPolish: Boolean = false
    ) {
        fun fillPaint(): Paint {
            val top = blendArgb(color, Color.WHITE, 0.16f)
            val bottom = blendArgb(color, Color.BLACK, 0.20f)
            return Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(top, color, color, bottom),
                    floatArrayOf(0f, 0.12f, 0.78f, 1f),
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
        }
        fun drawPhysicalFill(path: Path) {
            canvas.drawPath(path, fillPaint())
        }
        when (buttonStyle) {
            "wild" -> {
                val w = width.toFloat()
                val h = height.toFloat()
                val j = 5f * density
                val path = Path().apply {
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
                canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = Color.argb(255, 8, 6, 18)
                })
                canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color
                    alpha = 190
                })
                canvas.drawRoundRect(
                    RectF(w * 0.12f, h * 0.2f, w * 0.88f, h * 0.8f),
                    8f * density,
                    8f * density,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.argb(200, 0, 0, 0) }
                )
                val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2.6f * density
                    this.color = Color.WHITE
                    alpha = 220
                    pathEffect = DashPathEffect(floatArrayOf(12f * density, 7f * density), 0f)
                }
                canvas.drawPath(path, dash)
            }
            "leaf" -> {
                val stem = height * 0.04f
                val path = Path().apply {
                    moveTo(width * 0.5f, height - stem)
                    cubicTo(width * 0.12f, height * 0.88f, width * 0.04f, height * 0.58f, width * 0.1f, height * 0.34f)
                    cubicTo(width * 0.06f, height * 0.12f, width * 0.28f, stem, width * 0.46f, stem * 1.2f)
                    cubicTo(width * 0.5f, stem * 0.35f, width * 0.54f, stem * 1.2f, width * 0.72f, stem)
                    cubicTo(width * 0.94f, height * 0.12f, width * 0.9f, height * 0.34f, width * 0.88f, height * 0.58f)
                    cubicTo(width * 0.96f, height * 0.88f, width * 0.62f, height - stem * 0.4f, width * 0.5f, height - stem)
                    close()
                }
                drawPhysicalFill(path)
            }
            "tactical" -> {
                val cut = (min(width, height) * 0.2f).coerceIn(height * 0.12f, height * 0.42f)
                val notchW = width * 0.11f
                val notchD = cut * 0.55f
                val path = Path().apply {
                    moveTo(cut * 0.55f, 0f)
                    lineTo(width * 0.5f - notchW, 0f)
                    quadTo(width * 0.5f, notchD, width * 0.5f + notchW, 0f)
                    lineTo(width - cut * 0.55f, 0f)
                    lineTo(width.toFloat(), cut * 0.55f)
                    lineTo(width.toFloat(), height - cut)
                    lineTo(width - cut, height.toFloat())
                    lineTo(cut, height.toFloat())
                    lineTo(0f, height - cut)
                    lineTo(0f, cut * 0.55f)
                    close()
                }
                drawPhysicalFill(path)
            }
            "chamfer" -> {
                val cut = 16f * density
                val path = Path().apply {
                    moveTo(cut, 0f)
                    lineTo(width.toFloat(), 0f)
                    lineTo(width.toFloat(), height - cut)
                    lineTo(width - cut, height.toFloat())
                    lineTo(0f, height.toFloat())
                    lineTo(0f, cut)
                    close()
                }
                drawPhysicalFill(path)
            }
            "capsule" -> {
                val path = Path().apply {
                    addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), height / 2f, height / 2f, Path.Direction.CW)
                }
                drawPhysicalFill(path)
            }
            "hex" -> {
                val inset = 0.16f
                val left = width * inset
                val right = width * (1f - inset)
                val path = Path().apply {
                    moveTo(left, 0f)
                    lineTo(right, 0f)
                    lineTo(width.toFloat(), height * 0.5f)
                    lineTo(right, height.toFloat())
                    lineTo(left, height.toFloat())
                    lineTo(0f, height * 0.5f)
                    close()
                }
                drawPhysicalFill(path)
            }
            "wave" -> {
                val crest = height * 0.055f
                val topY = height * 0.14f
                val botY = height * 0.86f
                val path = Path().apply {
                    moveTo(0f, topY)
                    cubicTo(width * 0.24f, topY - crest, width * 0.76f, topY - crest, width.toFloat(), topY)
                    lineTo(width.toFloat(), botY)
                    cubicTo(width * 0.76f, botY + crest, width * 0.24f, botY + crest, 0f, botY)
                    close()
                }
                drawPhysicalFill(path)
            }
            else -> {
                val path = Path().apply {
                    addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 14f * density, 14f * density, Path.Direction.CW)
                }
                drawPhysicalFill(path)
            }
        }
    }

    private fun renderButtonBitmap(
        context: Context,
        label: String,
        bgColor: Int,
        fgColor: Int,
        buttonStyle: String,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (buttonStyle != "wild") {
            drawPhysicalButtonShadow(canvas, widthPx, heightPx, bgColor, buttonStyle, density)
        }
        drawButtonBackground(canvas, widthPx, heightPx, bgColor, buttonStyle, density, premiumPolish = false)
        if (buttonStyle != "wild") {
            drawButtonPolish(canvas, widthPx, heightPx, buttonStyle, density, richer = true)
            drawPhysicalButtonRim(canvas, widthPx, heightPx, bgColor, buttonStyle, density)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fgColor
            textSize = 13f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            if (fgColor == Color.WHITE) {
                setShadowLayer(2f, 0f, 1f * density, Color.argb(190, 0, 0, 0))
            }
        }
        val textY = heightPx / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(label, widthPx / 2f, textY, paint)
        return bitmap
    }

    internal fun widgetButtonTextColor(
        fgColor: Int,
        buttonStyle: String,
        outlined: Boolean = false,
        backgroundArgb: Int? = null
    ): Int {
        if (buttonStyle == "wild") return Color.WHITE
        if (!outlined && backgroundArgb != null) {
            return widgetFilledButtonLabelColor(backgroundArgb, fgColor)
        }
        return fgColor
    }

    /** Match in-app [solidButtonLabelColor]: white on purple fills, dark ink on teal fills. */
    private fun widgetFilledButtonLabelColor(backgroundArgb: Int, fallbackFg: Int): Int {
        val lum = Color.red(backgroundArgb) * 0.299f +
            Color.green(backgroundArgb) * 0.587f +
            Color.blue(backgroundArgb) * 0.114f
        val isTealFill = Color.green(backgroundArgb) > 160 &&
            Color.blue(backgroundArgb) > 160 &&
            lum > 120f
        return if (isTealFill) 0xFF0A0F1A.toInt() else fallbackFg
    }

    private fun applyWidgetButton(
        context: Context,
        views: RemoteViews,
        viewId: Int,
        label: String,
        bgColor: Int,
        fgColor: Int,
        buttonStyle: String,
        widthPx: Int,
        heightPx: Int
    ) {
        val textColor = widgetButtonTextColor(fgColor, buttonStyle)
        views.setImageViewBitmap(
            viewId,
            renderButtonBitmap(context, label, bgColor, textColor, buttonStyle, widthPx, heightPx)
        )
        views.setContentDescription(viewId, label)
    }

    fun widgetSizeDp(appWidgetManager: AppWidgetManager, appWidgetId: Int): Pair<Int, Int> {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 40)
        return width to height
    }

    fun dpToPx(context: Context, dp: Float): Int =
        (dp * context.resources.displayMetrics.density).toInt()

    fun actionLabelColor(fgColor: Int, buttonStyle: String, outlined: Boolean = false, backgroundArgb: Int? = null): Int =
        widgetButtonTextColor(fgColor, buttonStyle, outlined, backgroundArgb)

    fun bumpWidgetRevision(prefs: SharedPreferences) {
        prefs.edit().putLong(WIDGET_REVISION_PREF, System.currentTimeMillis()).commit()
    }

    /** Fire-and-forget refresh — safe to call from any thread, including hot paths. */
    fun refreshAllWidgets(context: Context) {
        val appContext = context.applicationContext
        widgetRefreshScope.launch {
            refreshAllWidgetsAwait(appContext)
        }
    }

    /** Blocking refresh — used where the caller wants to know the widgets were told to repaint
     * before continuing (e.g. right before finishing a settings screen). */
    fun refreshAllWidgetsNow(context: Context) {
        runBlocking(Dispatchers.IO) {
            refreshAllWidgetsAwait(context)
        }
    }

    // Spread across ~1.6s rather than one/two quick attempts — some OEM launchers only pick up a
    // RemoteViews change if it lands after their own resize/animation work has settled, so a
    // single early burst of retries can still all land too soon on a slow/loaded device.
    private val REFRESH_PASS_DELAYS_MS = longArrayOf(0, 250, 600, 1100, 1600)

    /**
     * Repaints every placed instance of both widgets, retried across [REFRESH_PASS_DELAYS_MS],
     * using two mechanisms each pass:
     *  1. [GlanceAppWidget.updateAll] — the documented Glance API, re-invokes provideGlance().
     *  2. A real [AppWidgetManager.ACTION_APPWIDGET_UPDATE] broadcast straight to each receiver —
     *     the same entry point Android itself uses, as a fallback in case Glance's own internal
     *     id bookkeeping is what's stale on a given launcher.
     */
    suspend fun syncGlanceState(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        val isPremium = isPremiumUnlocked(prefs)
        val isTracking = ActiveTrackingHelper.isActive(appContext)
        val autoParkEnabled = isAutoParkEnabled(prefs)
        val motionEnabled = isMotionAutoParkEnabled(prefs)
        val photoPath = if (isTracking) prefs.getString("photo_path", "") ?: "" else ""
        val address = if (isTracking) prefs.getString("current_address", "") ?: "" else ""
        val coords = if (isTracking) ActiveTrackingHelper.pinnedCoordinates(appContext) else null
        val lat = coords?.first ?: 0.0
        val lng = coords?.second ?: 0.0
        val revision = System.currentTimeMillis()
        // Snapshot theme into Glance prefs so custom color changes (same "custom" theme id)
        // still change observed state and force every widget to repaint.
        val themeSnapshot = runCatching { GlanceThemeManager.load(appContext) }.getOrElse {
            GlanceThemeManager.defaultTheme()
        }

        runCatching {
            val manager = androidx.glance.appwidget.GlanceAppWidgetManager(appContext)
            val everydayIds = manager.getGlanceIds(EverydayQuickActionsWidget::class.java)
            everydayIds.forEach { glanceId ->
                androidx.glance.appwidget.state.updateAppWidgetState(appContext, glanceId) { glancePrefs ->
                    glancePrefs[EverydayQuickActionsWidget.KEY_IS_TRACKING] = isTracking
                    glancePrefs[EverydayQuickActionsWidget.KEY_REVISION] = revision
                    WidgetGlanceThemeKeys.write(glancePrefs, themeSnapshot)
                }
            }
        }.onFailure { android.util.Log.e("WidgetSync", "Sync Everyday failed", it) }

        runCatching {
            val manager = androidx.glance.appwidget.GlanceAppWidgetManager(appContext)
            val premiumIds = manager.getGlanceIds(PremiumGlanceWidget::class.java)
            premiumIds.forEach { glanceId ->
                androidx.glance.appwidget.state.updateAppWidgetState(appContext, glanceId) { glancePrefs ->
                    glancePrefs[PremiumGlanceWidget.KEY_IS_PREMIUM] = isPremium
                    glancePrefs[PremiumGlanceWidget.KEY_IS_TRACKING] = isTracking
                    glancePrefs[PremiumGlanceWidget.KEY_AUTO_PARK_ENABLED] = autoParkEnabled
                    glancePrefs[PremiumGlanceWidget.KEY_MOTION_ENABLED] = motionEnabled
                    glancePrefs[PremiumGlanceWidget.KEY_TRACKING_PHOTO_PATH] = photoPath
                    glancePrefs[PremiumGlanceWidget.KEY_TRACKING_ADDRESS] = address
                    glancePrefs[PremiumGlanceWidget.KEY_TRACKING_LAT] = lat
                    glancePrefs[PremiumGlanceWidget.KEY_TRACKING_LNG] = lng
                    glancePrefs[PremiumGlanceWidget.KEY_REVISION] = revision
                    WidgetGlanceThemeKeys.write(glancePrefs, themeSnapshot)
                }
            }
        }.onFailure { android.util.Log.e("WidgetSync", "Sync Premium failed", it) }

        runCatching {
            val manager = androidx.glance.appwidget.GlanceAppWidgetManager(appContext)
            val favoritesIds = manager.getGlanceIds(VaultFavoritesWidget::class.java)
            favoritesIds.forEach { glanceId ->
                androidx.glance.appwidget.state.updateAppWidgetState(appContext, glanceId) { glancePrefs ->
                    glancePrefs[VaultFavoritesWidget.KEY_REVISION] = revision
                    WidgetGlanceThemeKeys.write(glancePrefs, themeSnapshot)
                }
            }
        }.onFailure { android.util.Log.e("WidgetSync", "Sync VaultFavorites failed", it) }

        runCatching {
            val manager = androidx.glance.appwidget.GlanceAppWidgetManager(appContext)
            val tagFilterIds = manager.getGlanceIds(TagFilterWidget::class.java)
            tagFilterIds.forEach { glanceId ->
                // Only revision/theme are touched here — KEY_TAG_ID/KEY_TAG_NAME are per-instance
                // choices written by TagFilterWidgetConfigActivity and must survive every sync
                // pass untouched, the same way each widget's own settings persist through this.
                androidx.glance.appwidget.state.updateAppWidgetState(appContext, glanceId) { glancePrefs ->
                    glancePrefs[TagFilterWidget.KEY_REVISION] = revision
                    WidgetGlanceThemeKeys.write(glancePrefs, themeSnapshot)
                }
            }
        }.onFailure { android.util.Log.e("WidgetSync", "Sync TagFilter failed", it) }
    }

    suspend fun refreshAllWidgetsAwait(context: Context) {
        val appContext = context.applicationContext
        // Same trigger point as every widget repaint (Track/Found/Pin, auto-park, timer expiry,
        // etc.) — anything that changes "is_pinned" already calls this, so the Quick Track tile
        // piggybacks on it instead of needing its own scattered set of call sites. Quick Pin has
        // no state to refresh here — it's a one-shot action tile, not a toggle.
        runCatching { QuickTrackTileService.requestTileUpdate(appContext) }
            .onFailure { android.util.Log.e("WidgetRefresh", "Tile update request failed", it) }
        REFRESH_PASS_DELAYS_MS.forEachIndexed { index, delayMs ->
            if (index > 0) kotlinx.coroutines.delay(delayMs - REFRESH_PASS_DELAYS_MS[index - 1])
            syncGlanceState(appContext)
            runCatching { EverydayQuickActionsWidget().updateAll(appContext) }
                .onFailure { android.util.Log.e("WidgetRefresh", "updateAll(Everyday) failed", it) }
            runCatching { PremiumGlanceWidget().updateAll(appContext) }
                .onFailure { android.util.Log.e("WidgetRefresh", "updateAll(Premium) failed", it) }
            runCatching { VaultFavoritesWidget().updateAll(appContext) }
                .onFailure { android.util.Log.e("WidgetRefresh", "updateAll(VaultFavorites) failed", it) }
            runCatching { TagFilterWidget().updateAll(appContext) }
                .onFailure { android.util.Log.e("WidgetRefresh", "updateAll(TagFilter) failed", it) }
            forceSystemWidgetUpdate(appContext, EverydayQuickActionsWidgetReceiver::class.java)
            forceSystemWidgetUpdate(appContext, PremiumGlanceWidgetReceiver::class.java)
            forceSystemWidgetUpdate(appContext, VaultFavoritesWidgetReceiver::class.java)
            forceSystemWidgetUpdate(appContext, TagFilterWidgetReceiver::class.java)
        }
    }

    /** Sends a real system APPWIDGET_UPDATE broadcast directly to [receiverClass] for every
     * currently-placed instance — bypasses Glance entirely so it can't be defeated by Glance's
     * own id-caching quirks. */
    private fun forceSystemWidgetUpdate(context: Context, receiverClass: Class<*>) {
        runCatching {
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, receiverClass))
            if (ids.isEmpty()) return@runCatching
            context.sendBroadcast(
                Intent(context, receiverClass).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
            )
        }.onFailure {
            android.util.Log.e("WidgetRefresh", "forceSystemWidgetUpdate failed for $receiverClass", it)
        }
    }

    fun commitThemeChange(prefs: SharedPreferences, context: Context, block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).commit()
        bumpWidgetRevision(prefs)
        // Fire-and-forget — this runs directly inside Compose onClick/onCheckedChange callbacks
        // on the main thread, so it must never block (refreshAllWidgetsNow's runBlocking would
        // freeze the settings screen for the duration of the two-pass refresh).
        refreshAllWidgets(context.applicationContext)
    }
}
