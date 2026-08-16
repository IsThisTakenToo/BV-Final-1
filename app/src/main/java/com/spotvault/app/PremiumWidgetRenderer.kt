package com.spotvault.app

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.ExifInterface
import android.graphics.Matrix
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/** Bitmap rendering for the premium Glance widget — history rows, toggles, hero photo, lock card. */
object PremiumWidgetRenderer {

    enum class ChipIcon { BLUETOOTH, MOTION, SHARE, MAP }

    fun renderToggleChipBitmap(
        context: Context,
        label: String,
        active: Boolean,
        themeColorArgb: Int,
        onThemeColorArgb: Int,
        surfaceArgb: Int,
        mutedArgb: Int,
        widthPx: Int,
        heightPx: Int,
        icon: ChipIcon = if (label.contains("BT", ignoreCase = true) || label.contains("Bluetooth", ignoreCase = true)) {
            ChipIcon.BLUETOOTH
        } else {
            ChipIcon.MOTION
        },
        /** Overrides the default "white when active" icon color — Quick Share is rendered
         * permanently active to reuse this chip's visual style as a plain button, not a real
         * on/off toggle, and white read poorly against its gold accent fill. */
        iconColorOverrideArgb: Int? = null
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val side = min(w, h) * 0.96f
        val left = (w - side) / 2f
        val top = (h - side) / 2f
        val radius = side * 0.26f
        val rect = RectF(left, top, left + side, top + side)

        val fillPaint = if (active) {
            val top = blendArgb(themeColorArgb, Color.WHITE, 0.16f)
            val bottom = blendArgb(themeColorArgb, Color.BLACK, 0.20f)
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    rect.left, rect.top, rect.left, rect.bottom,
                    intArrayOf(top, themeColorArgb, themeColorArgb, bottom),
                    floatArrayOf(0f, 0.12f, 0.78f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
        } else {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    rect.left, rect.top, rect.left, rect.bottom,
                    blendArgb(surfaceArgb, Color.WHITE, 0.06f),
                    blendArgb(surfaceArgb, Color.BLACK, 0.10f),
                    Shader.TileMode.CLAMP
                )
            }
        }

        if (active) {
            canvas.save()
            canvas.translate(0f, 2.8f * density)
            canvas.drawRoundRect(rect, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(90, 0, 0, 0)
            })
            canvas.translate(0f, -1.2f * density)
            canvas.drawRoundRect(rect, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = blendArgb(themeColorArgb, Color.BLACK, 0.42f)
            })
            canvas.restore()
        }

        canvas.drawRoundRect(rect, radius, radius, fillPaint)

        canvas.drawRoundRect(
            rect,
            radius,
            radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = if (active) 2.1f * density else 1.6f * density
                shader = LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    intArrayOf(
                        Color.argb(if (active) 240 else 180, Color.red(blendArgb(themeColorArgb, Color.WHITE, 0.52f)), Color.green(blendArgb(themeColorArgb, Color.WHITE, 0.52f)), Color.blue(blendArgb(themeColorArgb, Color.WHITE, 0.52f))),
                        Color.argb(if (active) 220 else 160, Color.red(blendArgb(themeColorArgb, Color.BLACK, 0.48f)), Color.green(blendArgb(themeColorArgb, Color.BLACK, 0.48f)), Color.blue(blendArgb(themeColorArgb, Color.BLACK, 0.48f)))
                    ),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
        )

        canvas.save()
        canvas.clipRect(RectF(rect.left, rect.top, rect.right, rect.top + side * 0.16f))
        canvas.drawRoundRect(
            rect, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(if (active) 62 else 28, 255, 255, 255)
            }
        )
        canvas.restore()
        canvas.save()
        canvas.clipRect(RectF(rect.left, rect.top + side * 0.78f, rect.right, rect.bottom))
        canvas.drawRoundRect(
            rect, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(if (active) 72 else 36, 0, 0, 0)
            }
        )
        canvas.restore()

        val iconColor = iconColorOverrideArgb ?: if (active) Color.WHITE else themeColorArgb
        val cx = left + side / 2f
        val cy = top + side / 2f
        val iconSize = side * 0.58f
        when (icon) {
            ChipIcon.BLUETOOTH -> drawBluetoothIcon(canvas, cx, cy, iconSize, iconColor)
            ChipIcon.MOTION -> drawMotionIcon(canvas, context, cx, cy, iconSize, iconColor)
            ChipIcon.SHARE -> WidgetThemeHelper.drawQuickActionIcon(canvas, WidgetThemeHelper.QuickActionIcon.SHARE, cx, cy, iconSize, iconColor)
            ChipIcon.MAP -> WidgetThemeHelper.drawQuickActionIcon(canvas, WidgetThemeHelper.QuickActionIcon.MAP, cx, cy, iconSize, iconColor)
        }

        if (active) {
            canvas.drawCircle(
                left + side - 7.5f * density,
                top + 7.5f * density,
                4.2f * density,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(230, 255, 255, 255)
                }
            )
            canvas.drawCircle(
                left + side - 7.5f * density,
                top + 7.5f * density,
                4.2f * density,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1f * density
                    color = Color.argb(120, 0, 0, 0)
                }
            )
        }
        return bitmap
    }

    private fun drawBluetoothIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = size * 0.12f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val r = size * 0.42f
        val path = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r * 0.55f, cy - r * 0.35f)
            lineTo(cx, cy)
            lineTo(cx + r * 0.55f, cy + r * 0.35f)
            lineTo(cx, cy + r)
            moveTo(cx, cy - r)
            lineTo(cx, cy + r)
            moveTo(cx - r * 0.55f, cy - r * 0.35f)
            lineTo(cx, cy)
            lineTo(cx - r * 0.55f, cy + r * 0.35f)
        }
        canvas.drawPath(path, stroke)
    }

    /** Same glyph as the home screen's Motion & Fitness toggle (Icons.AutoMirrored.Filled.
     * DirectionsWalk) — rasterized from a matching vector drawable resource rather than a
     * hand-drawn Canvas path, so the widget's icon is the same shape as the in-app one instead
     * of an approximation of it. [size] matches the BT/Share icons' own computed size exactly,
     * since all three come from the same iconSize calculation in [renderToggleChipBitmap]. */
    private fun drawMotionIcon(canvas: Canvas, context: Context, cx: Float, cy: Float, size: Float, color: Int) {
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_motion_walk)?.mutate()
            ?: return
        drawable.setTint(color)
        val half = (size / 2f).toInt().coerceAtLeast(1)
        canvas.save()
        canvas.translate(cx - half, cy - half)
        drawable.setBounds(0, 0, half * 2, half * 2)
        drawable.draw(canvas)
        canvas.restore()
    }

    fun renderHistoryRowBitmap(
        context: Context,
        entry: PremiumHistoryEntry,
        theme: GlanceWidgetTheme,
        widthPx: Int,
        heightPx: Int,
        userLat: Double? = null,
        userLng: Double? = null,
        distanceUnit: String = DEFAULT_DISTANCE_UNIT
    ): Bitmap = renderSpotRowBitmap(
        context = context,
        spot = entry.spot,
        vehicleName = entry.vehicleName,
        theme = theme,
        widthPx = widthPx,
        heightPx = heightPx,
        favoriteStar = false,
        fallbackTitle = "Saved spot",
        userLat = userLat,
        userLng = userLng,
        distanceUnit = distanceUnit
    )

    /** Shared vault spot row — premium Recent list and Vault Favorites widget. */
    fun renderSpotRowBitmap(
        context: Context,
        spot: LocationSpot,
        vehicleName: String?,
        theme: GlanceWidgetTheme,
        widthPx: Int,
        heightPx: Int,
        favoriteStar: Boolean = false,
        fallbackTitle: String = "Saved spot",
        userLat: Double? = null,
        userLng: Double? = null,
        distanceUnit: String = DEFAULT_DISTANCE_UNIT
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val pad = 4f * density
        val thumb = (h - pad * 2f).coerceIn(28f * density, 140f * density)

        val rowRect = RectF(pad, pad * 0.5f, w - pad, h - pad * 0.5f)
        canvas.drawRoundRect(
            rowRect,
            10f * density,
            10f * density,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = blendArgb(theme.palette.surface, theme.palette.primary, 0.08f)
            }
        )
        canvas.drawRoundRect(
            rowRect,
            10f * density,
            10f * density,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 0.9f * density
                color = Color.argb(55, Color.red(theme.palette.primary), Color.green(theme.palette.primary), Color.blue(theme.palette.primary))
            }
        )

        val thumbLeft = pad + 4f * density
        val thumbTop = (h - thumb) / 2f
        val thumbRect = RectF(thumbLeft, thumbTop, thumbLeft + thumb, thumbTop + thumb)
        drawSpotThumbnail(canvas, context, spot.imagePath, thumbRect, theme, density)
        if (favoriteStar) {
            drawFavoriteStarBadge(canvas, thumbRect.right - 7f * density, thumbRect.top + 7f * density, 5.5f * density, theme.palette.accent)
        }

        val chevronSize = 5f * density
        val chevronCx = rowRect.right - chevronSize * 0.9f
        canvas.drawPath(
            Path().apply {
                moveTo(chevronCx - chevronSize * 0.35f, h / 2f - chevronSize * 0.6f)
                lineTo(chevronCx + chevronSize * 0.45f, h / 2f)
                lineTo(chevronCx - chevronSize * 0.35f, h / 2f + chevronSize * 0.6f)
            },
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.4f * density
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = Color.argb(150, Color.red(theme.palette.muted), Color.green(theme.palette.muted), Color.blue(theme.palette.muted))
            }
        )

        val textLeft = thumbRect.right + 8f * density
        val textRight = chevronCx - chevronSize * 0.9f
        val textWidth = (textRight - textLeft).coerceAtLeast(1f)

        val titleText = spot.title.substringBefore('\n').ifBlank { fallbackTitle }
        val metaParts = buildList {
            vehicleName?.takeIf { it.isNotBlank() }?.let { add(it) }
            add(formatSpotDate(spot.timestamp))
        }
        val metaLine = metaParts.joinToString(" · ")
        // Street name + city only — the full street address (with state/zip) was too long to be
        // useful at a glance in a compact widget row.
        val addressLine = run {
            val parts = spot.address.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            when {
                parts.size >= 2 -> "${parts[0]}, ${parts[1]}"
                parts.size == 1 -> parts[0]
                else -> spot.city.ifBlank { spot.state }
            }
        }
        val distanceLabel = if (userLat != null && userLng != null) {
            formatSpotDistanceLabel(userLat, userLng, spot.lat, spot.lng, distanceUnit)
        } else null

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.palette.onSurface
            textSize = (h * 0.185f).coerceIn(12f * density, 21f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.palette.muted
            textSize = (h * 0.145f).coerceIn(10.5f * density, 16.5f * density)
        }
        val addressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, Color.red(theme.palette.onSurface), Color.green(theme.palette.onSurface), Color.blue(theme.palette.onSurface))
            textSize = (h * 0.165f).coerceIn(12.5f * density, 19f * density)
        }
        val distancePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.palette.accent
            textSize = (h * 0.13f).coerceIn(9.5f * density, 14f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val titleFm = titlePaint.fontMetrics
        val metaFm = metaPaint.fontMetrics
        val addressFm = addressPaint.fontMetrics
        val titleLineH = titleFm.descent - titleFm.ascent
        val metaLineH = metaFm.descent - metaFm.ascent
        val addressLineH = addressFm.descent - addressFm.ascent
        val showAddress = addressLine.isNotBlank() && (titleLineH + metaLineH + addressLineH) <= (h - pad * 2f)

        val totalTextH = titleLineH + metaLineH + (if (showAddress) addressLineH else 0f)
        var y = (h - totalTextH) / 2f - titleFm.ascent

        // Distance sits at the end of the title line, right-aligned — the title truncates a
        // little sooner to make room rather than the label overlapping or crowding a new line.
        val distanceWidth = if (distanceLabel != null) distancePaint.measureText(distanceLabel) + 6f * density else 0f
        val titleTextWidth = (textWidth - distanceWidth).coerceAtLeast(1f)

        canvas.drawText(ellipsize(titleText, titlePaint, titleTextWidth), textLeft, y, titlePaint)
        if (distanceLabel != null) {
            canvas.drawText(distanceLabel, textRight, y, distancePaint)
        }
        y += titleFm.descent - metaFm.ascent
        canvas.drawText(ellipsize(metaLine, metaPaint, textWidth), textLeft, y, metaPaint)
        if (showAddress) {
            y += metaFm.descent - addressFm.ascent
            canvas.drawText(ellipsize(addressLine, addressPaint, textWidth), textLeft, y, addressPaint)
        }
        return bitmap
    }

    internal fun drawFavoriteStarBadge(canvas: Canvas, cx: Float, cy: Float, outerR: Float, color: Int) {
        val path = Path()
        val innerR = outerR * 0.45f
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = Math.toRadians(-90.0 + i * 36.0)
            val x = cx + (r * kotlin.math.cos(angle)).toFloat()
            val y = cy + (r * kotlin.math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    fun renderTrackingPhotoBitmap(
        context: Context,
        photoPath: String,
        theme: GlanceWidgetTheme,
        widthPx: Int,
        heightPx: Int,
        address: String = "",
        lat: Double = 0.0,
        lng: Double = 0.0,
        overlayColorArgb: Int = Color.WHITE
    ): Bitmap? {
        if (photoPath.isBlank()) return null
        val source = loadScaledPhoto(photoPath, widthPx, heightPx) ?: return null
        val density = context.resources.displayMetrics.density
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val radius = 14f * density
        val rect = RectF(0f, 0f, w, h)
        val clip = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clip)
        val scale = maxOf(w / source.width, h / source.height)
        val drawW = source.width * scale
        val drawH = source.height * scale
        val left = (w - drawW) / 2f
        val top = (h - drawH) / 2f
        canvas.drawBitmap(source, null, RectF(left, top, left + drawW, top + drawH), Paint(Paint.ANTI_ALIAS_FLAG))
        canvas.drawRect(
            RectF(0f, h * 0.48f, w, h),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, h * 0.48f, 0f, h,
                    Color.argb(0, 0, 0, 0),
                    Color.argb(200, 0, 0, 0),
                    Shader.TileMode.CLAMP
                )
            }
        )
        drawTrackingAddressOverlay(canvas, w, h, density, address, lat, lng, overlayColorArgb)
        canvas.restore()
        canvas.drawPath(
            clip,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.2f * density
                color = Color.argb(100, Color.red(theme.palette.primary), Color.green(theme.palette.primary), Color.blue(theme.palette.primary))
            }
        )
        source.recycle()
        return bitmap
    }

    fun renderLockedPremiumBitmap(
        context: Context,
        theme: GlanceWidgetTheme,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        canvas.drawRoundRect(
            RectF(0f, 0f, w, h),
            16f * density,
            16f * density,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, h,
                    blendArgb(theme.palette.primary, theme.palette.surface, 0.35f),
                    theme.palette.surface,
                    Shader.TileMode.CLAMP
                )
            }
        )
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.palette.onSurface
            textSize = 13f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.palette.muted
            textSize = 9.5f * density
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Premium Widget", w / 2f, h * 0.42f, titlePaint)
        canvas.drawText("Unlock Premium in Settings", w / 2f, h * 0.58f, subPaint)
        return bitmap
    }

    fun renderSectionLabelBitmap(
        context: Context,
        label: String,
        colorArgb: Int,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            textSize = (heightPx * 0.68f).coerceIn(12f * density, 20f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.06f
        }
        canvas.drawText(label.uppercase(), 4f * density, heightPx - 5f * density, paint)
        return bitmap
    }

    fun renderSortIconBitmap(context: Context, colorArgb: Int, sizePx: Int, isOldest: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx.coerceAtLeast(1), sizePx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            strokeWidth = sizePx * 0.1f
            strokeCap = Paint.Cap.ROUND
        }
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val w = sizePx * 0.5f
        val h = sizePx * 0.4f
        
        if (isOldest) {
            // Sort ascending (oldest first) — bars grow top-to-bottom, standard ascending glyph.
            canvas.drawLine(cx - w/2, cy - h/2, cx, cy - h/2, paint)
            canvas.drawLine(cx - w/2, cy, cx + w/4, cy, paint)
            canvas.drawLine(cx - w/2, cy + h/2, cx + w/2, cy + h/2, paint)
        } else {
            // Sort descending (newest first) — bars shrink top-to-bottom, standard descending glyph.
            canvas.drawLine(cx - w/2, cy - h/2, cx + w/2, cy - h/2, paint)
            canvas.drawLine(cx - w/2, cy, cx + w/4, cy, paint)
            canvas.drawLine(cx - w/2, cy + h/2, cx, cy + h/2, paint)
        }
        return bitmap
    }

    fun renderVaultIconBitmap(context: Context, colorArgb: Int, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx.coerceAtLeast(1), sizePx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.08f
        }
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val r = sizePx * 0.35f
        canvas.drawCircle(cx, cy, r, paint)
        canvas.drawCircle(cx, cy, r * 0.4f, paint)
        canvas.drawLine(cx, cy - r, cx, cy - r * 0.4f, paint)
        canvas.drawLine(cx, cy + r, cx, cy + r * 0.4f, paint)
        canvas.drawLine(cx - r, cy, cx - r * 0.4f, cy, paint)
        canvas.drawLine(cx + r, cy, cx + r * 0.4f, cy, paint)
        return bitmap
    }

    /** Full-width "Open Vault" footer row — a clear, low-effort way in from the widget to the
     * full history, distinct from tapping an individual recent spot. */
    fun renderVaultFooterButtonBitmap(
        context: Context,
        theme: GlanceWidgetTheme,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val radius = (h * 0.4f).coerceAtMost(12f * density)
        val rect = RectF(0.75f, 0.75f, w - 0.75f, h - 0.75f)
        canvas.drawRoundRect(
            rect, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, h,
                    blendArgb(theme.palette.primary, theme.palette.surface, 0.22f),
                    blendArgb(theme.palette.surface, theme.palette.primary, 0.10f),
                    Shader.TileMode.CLAMP
                )
            }
        )
        canvas.drawRoundRect(
            rect, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.15f * density
                color = Color.argb(140, Color.red(theme.palette.primary), Color.green(theme.palette.primary), Color.blue(theme.palette.primary))
            }
        )
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (h * 0.48f).coerceIn(12f * density, 18f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.04f
        }
        val label = "Open Vault"
        val labelWidth = textPaint.measureText(label)
        val arrowGap = 8f * density
        val arrowSize = (h * 0.16f).coerceIn(6f * density, 10f * density)
        val totalWidth = labelWidth + arrowGap + arrowSize
        val startX = w / 2f - totalWidth / 2f + labelWidth / 2f
        val textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, startX, textY, textPaint)

        val arrowCx = startX + labelWidth / 2f + arrowGap + arrowSize / 2f
        val arrowPath = Path().apply {
            moveTo(arrowCx - arrowSize * 0.3f, h / 2f - arrowSize * 0.5f)
            lineTo(arrowCx + arrowSize * 0.4f, h / 2f)
            lineTo(arrowCx - arrowSize * 0.3f, h / 2f + arrowSize * 0.5f)
        }
        canvas.drawPath(
            arrowPath,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.6f * density
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = Color.WHITE
            }
        )
        return bitmap
    }

    /** Shown in the tracking state in place of the hero photo when no photo was attached. */
    fun renderTrackingInfoCardBitmap(
        context: Context,
        theme: GlanceWidgetTheme,
        address: String,
        lat: Double,
        lng: Double,
        widthPx: Int,
        heightPx: Int,
        logoColorArgb: Int = Color.WHITE,
        overlayColorArgb: Int = Color.WHITE
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val radius = 14f * density
        val rect = RectF(0f, 0f, w, h)

        // Soft map-like atmosphere: diagonal theme wash + subtle concentric rings.
        canvas.drawRoundRect(
            rect, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, w, h,
                    blendArgb(theme.palette.primary, theme.palette.surface, 0.42f),
                    blendArgb(theme.palette.accent, theme.palette.surface, 0.22f),
                    Shader.TileMode.CLAMP
                )
            }
        )
        canvas.drawRoundRect(
            rect, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, h,
                    Color.argb(40, 255, 255, 255),
                    Color.argb(0, 0, 0, 0),
                    Shader.TileMode.CLAMP
                )
            }
        )
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.4f * density
            color = Color.argb(55, Color.red(theme.palette.onSurface), Color.green(theme.palette.onSurface), Color.blue(theme.palette.onSurface))
        }
        val cx = w * 0.5f
        val cy = h * 0.38f
        val maxRing = min(w, h) * 0.34f
        for (i in 1..3) {
            canvas.drawCircle(cx, cy, maxRing * (i / 3f), ringPaint)
        }
        canvas.drawCircle(
            cx, cy, maxRing * 0.12f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(70, Color.red(theme.palette.accent), Color.green(theme.palette.accent), Color.blue(theme.palette.accent)) }
        )
        canvas.drawCircle(cx, cy, maxRing * 0.055f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.palette.accent })
        WidgetThemeHelper.drawQuickActionIcon(
            canvas,
            WidgetThemeHelper.QuickActionIcon.PIN,
            cx,
            cy,
            (maxRing * 0.42f).coerceIn(18f * density, 36f * density),
            logoColorArgb
        )

        canvas.drawRoundRect(
            rect, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.2f * density
                color = Color.argb(110, Color.red(theme.palette.accent), Color.green(theme.palette.accent), Color.blue(theme.palette.accent))
            }
        )

        canvas.drawRect(
            RectF(0f, h * 0.55f, w, h),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, h * 0.55f, 0f, h,
                    Color.argb(0, 0, 0, 0),
                    Color.argb(180, Color.red(theme.palette.surface), Color.green(theme.palette.surface), Color.blue(theme.palette.surface)),
                    Shader.TileMode.CLAMP
                )
            }
        )
        drawTrackingAddressOverlay(canvas, w, h, density, address, lat, lng, overlayColorArgb)
        return bitmap
    }

    private fun drawTrackingAddressOverlay(
        canvas: Canvas,
        w: Float,
        h: Float,
        density: Float,
        address: String,
        lat: Double,
        lng: Double,
        colorArgb: Int
    ) {
        val label = address.ifBlank {
            if (lat != 0.0 || lng != 0.0) String.format(Locale.US, "%.5f, %.5f", lat, lng) else "Tracking active"
        }
        val maxWidth = w * 0.9f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            textSize = (h * 0.13f).coerceIn(15f * density, 24f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        while (paint.textSize > 12f * density && paint.measureText(label) > maxWidth) {
            paint.textSize -= 0.5f * density
        }
        val lines = wrapAddressLines(label, paint, maxWidth, maxLines = 2)
        val lineHeight = paint.descent() - paint.ascent()
        val blockH = lineHeight * lines.size + (if (lines.size > 1) 3f * density else 0f)
        var y = h - 10f * density - blockH - paint.ascent()
        for (line in lines) {
            canvas.drawText(line, w / 2f, y, paint)
            y += lineHeight + 3f * density
        }
    }

    private fun wrapAddressLines(text: String, paint: Paint, maxWidth: Float, maxLines: Int): List<String> {
        if (paint.measureText(text) <= maxWidth || maxLines <= 1) {
            return listOf(ellipsize(text, paint, maxWidth))
        }
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size <= 1) return listOf(ellipsize(text, paint, maxWidth))
        var bestSplit = words.size / 2
        var bestScore = Float.MAX_VALUE
        for (i in 1 until words.size) {
            val a = words.subList(0, i).joinToString(" ")
            val b = words.subList(i, words.size).joinToString(" ")
            val score = kotlin.math.abs(paint.measureText(a) - paint.measureText(b))
            if (score < bestScore) {
                bestScore = score
                bestSplit = i
            }
        }
        val first = words.subList(0, bestSplit).joinToString(" ")
        val second = words.subList(bestSplit, words.size).joinToString(" ")
        return listOf(ellipsize(first, paint, maxWidth), ellipsize(second, paint, maxWidth))
    }
    private fun drawSpotThumbnail(
        canvas: Canvas,
        context: Context,
        imagePath: String,
        rect: RectF,
        theme: GlanceWidgetTheme,
        density: Float
    ) {
        val corner = 8f * density
        val thumb = loadScaledPhoto(imagePath, rect.width().toInt().coerceAtLeast(1), rect.height().toInt().coerceAtLeast(1))
        if (thumb != null) {
            canvas.drawRoundRect(rect, corner, corner, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.palette.surface
            })
            canvas.save()
            val clip = Path().apply { addRoundRect(rect, corner, corner, Path.Direction.CW) }
            canvas.clipPath(clip)
            canvas.drawBitmap(thumb, null, rect, Paint(Paint.ANTI_ALIAS_FLAG))
            canvas.restore()
            thumb.recycle()
        } else {
            drawNoPhotoSpotPlaceholder(canvas, context, rect, theme, density)
        }
    }

    /** Themed thumbnail when a vault entry has no photo — launcher icon centered on the
     * gradient background (Quick Pin, Quick Track, pin-only saves, etc.). */
    internal fun drawNoPhotoSpotPlaceholder(
        canvas: Canvas,
        context: Context,
        rect: RectF,
        theme: GlanceWidgetTheme,
        density: Float
    ) {
        val corner = 8f * density
        canvas.drawRoundRect(
            rect, corner, corner,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    blendArgb(theme.palette.primary, theme.palette.surface, 0.32f),
                    blendArgb(theme.palette.accent, theme.palette.surface, 0.14f),
                    Shader.TileMode.CLAMP
                )
            }
        )

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(28, 255, 255, 255)
            strokeWidth = 0.9f * density
        }
        val cx = rect.centerX()
        val cy = rect.centerY()
        canvas.drawLine(rect.left + 4f * density, cy, rect.right - 4f * density, cy, gridPaint)
        canvas.drawLine(cx, rect.top + 4f * density, cx, rect.bottom - 4f * density, gridPaint)
        canvas.drawCircle(cx, cy, rect.width() * 0.28f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.9f * density
            color = Color.argb(22, 255, 255, 255)
        })

        val iconSize = (min(rect.width(), rect.height()) * 0.62f).toInt().coerceIn(8, 256)
        loadLauncherIconBitmap(context, iconSize)?.let { icon ->
            val half = iconSize / 2f
            val iconRect = RectF(cx - half, cy - half, cx + half, cy + half)
            canvas.drawBitmap(icon, null, iconRect, Paint(Paint.ANTI_ALIAS_FLAG))
            if (!icon.isRecycled) icon.recycle()
        }

        canvas.drawRoundRect(
            rect, corner, corner,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1f * density
                color = Color.argb(70, Color.red(theme.palette.primary), Color.green(theme.palette.primary), Color.blue(theme.palette.primary))
            }
        )
    }

    /** Bitmap for in-app vault thumbnails — matches widget no-photo placeholder styling. */
    fun renderNoPhotoThumbnailBitmap(context: Context, widthPx: Int, heightPx: Int): Bitmap {
        val theme = GlanceThemeManager.load(context)
        val density = context.resources.displayMetrics.density
        val w = widthPx.coerceAtLeast(1)
        val h = heightPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawNoPhotoSpotPlaceholder(canvas, context, RectF(0f, 0f, w.toFloat(), h.toFloat()), theme, density)
        return bitmap
    }

    private fun loadLauncherIconBitmap(context: Context, sizePx: Int): Bitmap? {
        val safeSize = sizePx.coerceAtLeast(1)
        return try {
            val pm = context.packageManager
            val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
            val appIcon = AppIcon.fromId(AppIconManager.currentIconId(prefs))
            val classNamespace = MainActivity::class.java.name.substringBeforeLast('.')
            val component = ComponentName(context.packageName, "$classNamespace.${appIcon.aliasSuffix}")
            val drawable = pm.getActivityIcon(component)
            val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
            val iconCanvas = Canvas(bitmap)
            drawable.setBounds(0, 0, safeSize, safeSize)
            drawable.draw(iconCanvas)
            bitmap
        } catch (_: Exception) {
            try {
                val drawable = context.packageManager.getApplicationIcon(context.packageName)
                val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
                val iconCanvas = Canvas(bitmap)
                drawable.setBounds(0, 0, safeSize, safeSize)
                drawable.draw(iconCanvas)
                bitmap
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun loadScaledPhoto(path: String, maxW: Int, maxH: Int): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outWidth / sample > maxW * 1.5 || bounds.outHeight / sample > maxH * 1.5) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bmp = BitmapFactory.decodeFile(path, opts) ?: return null
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }
            if (!matrix.isIdentity) {
                val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                if (rotated != bmp) bmp.recycle()
                bmp = rotated
            }
            bmp
        } catch (_: Exception) {
            null
        }
    }

    private fun formatSpotDate(timestamp: Long): String =
        SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(Date(timestamp))

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text, 0, end) + paint.measureText("…") > maxWidth) end--
        return if (end <= 0) "…" else text.substring(0, end) + "…"
    }

    private fun blendArgb(base: Int, overlay: Int, overlayWeight: Float): Int {
        val w = overlayWeight.coerceIn(0f, 1f)
        val inv = 1f - w
        val r = (Color.red(base) * inv + Color.red(overlay) * w).toInt()
        val g = (Color.green(base) * inv + Color.green(overlay) * w).toInt()
        val b = (Color.blue(base) * inv + Color.blue(overlay) * w).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }
}

/** Shared locked-state content for every widget that requires Premium — Everyday Quick Actions
 * (the 4x1) is the one widget that stays free; Active Tracking, Vault Favorites, and Tag Filter
 * all render this instead of live content when Premium isn't unlocked. Tapping it opens Premium
 * Unlocks directly, same as the original Active Tracking widget's own lock card this was lifted
 * from — [widthDp]/[heightDp] are passed in rather than assumed, since each widget has its own
 * size-calculation functions. */
@Composable
fun PremiumLockedWidgetContent(theme: GlanceWidgetTheme, widthDp: Float, heightDp: Float) {
    val context = LocalContext.current
    val widthPx = WidgetThemeHelper.dpToPx(context, widthDp.coerceAtLeast(160f))
    val heightPx = WidgetThemeHelper.dpToPx(context, heightDp.coerceAtLeast(120f))
    val bitmap = remember(theme.cacheKey(), widthPx, heightPx) {
        PremiumWidgetRenderer.renderLockedPremiumBitmap(context, theme, widthPx, heightPx)
    }
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(PremiumWidgetIntents.openPremiumSettings(context))),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "Unlock Premium Widget",
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}
