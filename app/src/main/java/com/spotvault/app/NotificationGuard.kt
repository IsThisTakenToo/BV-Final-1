package com.spotvault.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

object NotificationGuard {
    private const val REQUEST_CODE = 1001
    /** Align with Watchdog's 15-minute cadence — 2-minute wakes for multi-hour airport/hotel
     * tracks stacked years of unnecessary startForegroundService attempts. */
    private const val INTERVAL_MS = 15 * 60 * 1000L

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntent(context) ?: return
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 30_000L,
            INTERVAL_MS,
            pendingIntent
        )
        scheduleWatchdog(context)
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        pendingIntent(context)?.let { alarmManager.cancel(it) }
        cancelWatchdog(context)
    }

    private fun pendingIntent(context: Context): PendingIntent? {
        val intent = Intent(context, NotificationGuardReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
