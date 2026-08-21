package com.spotvault.app

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

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
        } else {
            // Idle installs used to keep this unique periodic work forever — cancel when nothing
            // is pinned so years of 15-minute no-op wakes stop.
            cancelWatchdog(context)
        }
        return Result.success()
    }
}

private const val WATCHDOG_UNIQUE_WORK = "SpotVaultWatchdog"

/** Arms the 15-minute revive worker while Active Tracking is on. */
fun scheduleWatchdog(context: Context) {
    val workRequest = PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WATCHDOG_UNIQUE_WORK,
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
}

fun cancelWatchdog(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(WATCHDOG_UNIQUE_WORK)
}
