package com.spotvault.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

/**
 * Re-arms Motion & Fitness's watch for every Bluetooth-linked, currently-connected vehicle after
 * a reboot or app update. [BootReceiver] only resumes [TimerService] directly (a single, fast
 * SharedPreferences check) — this needs a Room query plus a Bluetooth reflection check per
 * vehicle, real work that doesn't belong inside a BroadcastReceiver's strict onReceive() time
 * budget (same reasoning [CarBluetoothReceiver]'s own doc comment gives for deferring to
 * [AutoParkWorker] instead of running inline).
 *
 * Without this, a reboot could silently leave Motion & Fitness unarmed for an entire trip: the
 * watch's registration with Play Services' ActivityRecognitionClient isn't guaranteed to survive
 * a reboot, while the "armed for this MAC" SharedPreferences state ([currentMotionWatchMac])
 * does — so the Settings screen kept showing "Armed" with nothing actually listening underneath
 * it, recovering only once that vehicle's Bluetooth happened to disconnect and reconnect again.
 */
class MotionWatchRearmWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        if (!isAutoParkEnabled(prefs) || !isMotionAutoParkEnabled(prefs)) return Result.success()

        val vehicleDao = AppDatabase.getDatabase(context.applicationContext).vehicleDao()
        vehicleDao.getActiveList()
            .mapNotNull { it.bluetoothMac?.takeIf { mac -> mac.isNotBlank() } }
            .forEach { mac -> armMotionWatchIfAlreadyConnected(context.applicationContext, mac) }

        return Result.success()
    }
}

fun enqueueMotionWatchRearm(context: Context) {
    WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<MotionWatchRearmWorker>().build())
}
