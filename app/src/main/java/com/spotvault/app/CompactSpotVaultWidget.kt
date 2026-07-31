package com.spotvault.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.os.SystemClock

class CompactSpotVaultWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
            val isPinned = prefs.getBoolean("is_pinned", false)
            val lat = prefs.getFloat("lat", 0f)
            val lng = prefs.getFloat("lng", 0f)
            val endTime = prefs.getLong("timer_end_time", 0L)
            val isAlarmRinging = prefs.getBoolean("is_alarm_ringing", false)

            val views = RemoteViews(context.packageName, R.layout.widget_spot_vault_compact)

            if (isPinned) {
                if (endTime > System.currentTimeMillis()) {
                    views.setChronometer(
                        R.id.widget_chronometer,
                        SystemClock.elapsedRealtime() + (endTime - System.currentTimeMillis()),
                        null,
                        true
                    )
                    views.setChronometerCountDown(R.id.widget_chronometer, true)
                    views.setViewVisibility(R.id.widget_chronometer, View.VISIBLE)
                    views.setTextViewText(R.id.widget_status, "Time Left")
                } else {
                    views.setViewVisibility(R.id.widget_chronometer, View.GONE)
                    views.setTextViewText(R.id.widget_status, "Saved")
                }

                views.setViewVisibility(R.id.widget_found_btn, View.VISIBLE)
                views.setViewVisibility(R.id.widget_nav_btn, View.VISIBLE)

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
                views.setViewVisibility(R.id.widget_chronometer, View.GONE)
                views.setTextViewText(R.id.widget_status, "No active pin")
                views.setViewVisibility(R.id.widget_found_btn, View.INVISIBLE)
                views.setViewVisibility(R.id.widget_nav_btn, View.INVISIBLE)
            }

            val openIntent = Intent(context, MainActivity::class.java)
            val openPendingIntent = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_status, openPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
