package com.spotvault.app

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ClearNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_pinned", false)
            .remove("photo_path")
            .remove("lat")
            .remove("lng")
            .remove("location_details")
            .remove("category")
            .remove("timer_end_time")
            .putBoolean("is_alarm_ringing", false)
            .apply()

        val serviceIntent = Intent(context, TimerService::class.java).apply {
            action = "STOP"
        }
        context.startService(serviceIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(1)
        manager.cancel(2)
        
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(context, SpotVaultWidget::class.java))
        for (id in appWidgetIds) {
            SpotVaultWidget.updateAppWidget(context, appWidgetManager, id)
        }
        val compactWidgetIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(context, CompactSpotVaultWidget::class.java))
        for (id in compactWidgetIds) {
            CompactSpotVaultWidget.updateAppWidget(context, appWidgetManager, id)
        }
    }
}
