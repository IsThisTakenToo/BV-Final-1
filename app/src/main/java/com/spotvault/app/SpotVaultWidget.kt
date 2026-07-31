package com.spotvault.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import java.io.File

class SpotVaultWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
            val isPinned = prefs.getBoolean("is_pinned", false)
            val photoPath = prefs.getString("photo_path", "") ?: ""
            val ocrText = prefs.getString("ocr_text", "") ?: ""
            val note = prefs.getString("note", "") ?: ""
            val lat = prefs.getFloat("lat", 0f)
            val lng = prefs.getFloat("lng", 0f)
            val isAlarmRinging = prefs.getBoolean("is_alarm_ringing", false)

            val views = RemoteViews(context.packageName, R.layout.widget_spot_vault)

            if (isPinned) {
                views.setTextViewText(R.id.widget_title, "Spot Pinned")
                
                val endTime = prefs.getLong("timer_end_time", 0L)
                val subtitle = buildString {
                    if (endTime > System.currentTimeMillis()) {
                        val remainingMs = endTime - System.currentTimeMillis()
                        val timeStr = if (remainingMs < 60000) "${remainingMs / 1000}s" else "${remainingMs / 60000}m"
                        append("⏱️ $timeStr ")
                    }
                    if (ocrText.isNotEmpty()) append("Sign: $ocrText")
                    if (isEmpty()) append("This spot is saved.")
                }
                views.setTextViewText(R.id.widget_subtitle, subtitle)
                
                if (note.isNotEmpty()) {
                    views.setTextViewText(R.id.widget_note, "Note: $note")
                    views.setViewVisibility(R.id.widget_note, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_note, View.GONE)
                }
                
                if (photoPath.isNotEmpty()) {
                    try {
                        val file = File(photoPath)
                        if (file.exists()) {
                            val exif = ExifInterface(photoPath)
                            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                            val matrix = Matrix()
                            when (orientation) {
                                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                            }
                            val original = BitmapFactory.decodeFile(photoPath)
                            val scaled = android.graphics.Bitmap.createScaledBitmap(original, 150, 150 * original.height / original.width, true)
                            val bitmap = android.graphics.Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
                            views.setImageViewBitmap(R.id.widget_image, bitmap)
                            views.setInt(R.id.widget_image, "setScaleType", android.widget.ImageView.ScaleType.CENTER_CROP.ordinal)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        views.setImageViewResource(R.id.widget_image, R.drawable.ic_spotvault_mark)
                        views.setInt(R.id.widget_image, "setScaleType", android.widget.ImageView.ScaleType.FIT_CENTER.ordinal)
                    }
                } else {
                    views.setImageViewResource(R.id.widget_image, R.drawable.ic_spotvault_mark)
                    views.setInt(R.id.widget_image, "setScaleType", android.widget.ImageView.ScaleType.FIT_CENTER.ordinal)
                }

                views.setViewVisibility(R.id.widget_actions_layout, View.VISIBLE)

                // Navigate Intent
                val mapUri = Uri.parse("google.navigation:q=$lat,$lng&mode=w")
                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                val mapPendingIntent = PendingIntent.getActivity(context, 0, mapIntent, PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_nav_btn, mapPendingIntent)

                // Found Intent
                val clearIntent = Intent(context, ClearNotificationReceiver::class.java)
                val clearPendingIntent = PendingIntent.getBroadcast(context, 0, clearIntent, PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_found_btn, clearPendingIntent)
                
                if (isAlarmRinging) {
                    views.setViewVisibility(R.id.widget_silence_btn, View.VISIBLE)
                    val silenceIntent = Intent(context, TimerService::class.java).apply { action = "SILENCE_ALARM" }
                    val silencePendingIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        PendingIntent.getForegroundService(context, 3, silenceIntent, PendingIntent.FLAG_IMMUTABLE)
                    } else {
                        PendingIntent.getService(context, 3, silenceIntent, PendingIntent.FLAG_IMMUTABLE)
                    }
                    views.setOnClickPendingIntent(R.id.widget_silence_btn, silencePendingIntent)
                } else {
                    views.setViewVisibility(R.id.widget_silence_btn, View.GONE)
                }
            } else {
                views.setViewVisibility(R.id.widget_silence_btn, View.GONE)
                views.setTextViewText(R.id.widget_title, "BeaconVault")
                views.setTextViewText(R.id.widget_subtitle, "No active pin")
                views.setViewVisibility(R.id.widget_note, View.GONE)
                views.setImageViewResource(R.id.widget_image, R.drawable.ic_spotvault_mark)
                views.setInt(R.id.widget_image, "setScaleType", android.widget.ImageView.ScaleType.FIT_CENTER.ordinal)
                views.setViewVisibility(R.id.widget_actions_layout, View.GONE)
            }

            // Open app on widget click
            val openIntent = Intent(context, MainActivity::class.java)
            val openPendingIntent = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_image, openPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_title, openPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
