package com.spotvault.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("is_pinned", false)) {
                val serviceIntent = Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_RESUME
                }
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // See MotionWatchRearmWorker's doc — Play Services' ActivityRecognitionClient
            // registration isn't guaranteed to survive this reboot even though the "armed"
            // SharedPreferences state does.
            enqueueMotionWatchRearm(context)
        }
    }
}
