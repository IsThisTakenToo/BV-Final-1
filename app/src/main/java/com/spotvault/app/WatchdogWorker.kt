package com.spotvault.app

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class WatchdogWorker(private val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_pinned", false)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    NotificationGuard.cancel(context)
                    return Result.success()
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
        }
        return Result.success()
    }
}
