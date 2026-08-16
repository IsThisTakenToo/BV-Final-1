package com.spotvault.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

data class VaultFavoriteEntry(
    val spot: LocationSpot,
    val vehicleName: String?
)

/** Bitmap rendering for the Vault Favorites Glance widget. */
object VaultFavoritesRenderer {

    fun renderEmptyBitmap(
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
        val pad = 8f * density
        val plateRect = RectF(pad, pad, w - pad, h * 0.52f)
        PremiumWidgetRenderer.drawNoPhotoSpotPlaceholder(canvas, context, plateRect, theme, density)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.palette.onSurface
            textSize = (h * 0.125f).coerceIn(13f * density, 18f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.palette.muted
            textSize = (h * 0.095f).coerceIn(10f * density, 14f * density)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("No favorites yet", w / 2f, h * 0.66f, titlePaint)
        canvas.drawText("Star spots in your Vault to see them here", w / 2f, h * 0.78f, bodyPaint)
        return bitmap
    }

    fun renderFavoriteRowBitmap(
        context: Context,
        entry: VaultFavoriteEntry,
        theme: GlanceWidgetTheme,
        widthPx: Int,
        heightPx: Int
    ): Bitmap = PremiumWidgetRenderer.renderSpotRowBitmap(
        context = context,
        spot = entry.spot,
        vehicleName = entry.vehicleName,
        theme = theme,
        widthPx = widthPx,
        heightPx = heightPx,
        favoriteStar = true,
        fallbackTitle = "Favorite spot"
    )
}
