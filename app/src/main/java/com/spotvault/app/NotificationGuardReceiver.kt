package com.spotvault.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Keeps the pinned-beacon foreground notification alive until Found Beacon is pressed. */
class NotificationGuardReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_pinned", false)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    NotificationGuard.cancel(context)
                    return
                }
            }
            val serviceIntent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_RESUME
            }
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            NotificationGuard.cancel(context)
        }
    }
}
