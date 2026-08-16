package com.spotvault.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

private const val CONNECTED_MAC_KEY = "connected_mac"

/**
 * Resolves whether a just-connected Bluetooth MAC belongs to a registered vehicle, then arms
 * [startMotionWatch] if so. Runs as WorkManager (not inline in [CarBluetoothReceiver]) purely to
 * keep database access off the receiver's main thread, matching [AutoParkWorker]'s pattern —
 * every other Bluetooth device the phone connects to (headphones, speakers) simply resolves to
 * no vehicle here and does nothing.
 */
class MotionWatchStartWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        if (!isAutoParkEnabled(prefs) || !isMotionAutoParkEnabled(prefs)) return Result.success()

        val connectedMac = inputData.getString(CONNECTED_MAC_KEY) ?: return Result.success()
        val db = AppDatabase.getDatabase(context.applicationContext)
        val vehicleDao = db.vehicleDao()
        migrateLegacyCarVehicleIfNeeded(context.applicationContext, prefs, vehicleDao)

        val vehicle = vehicleDao.findByBluetoothMac(connectedMac) ?: run {
            val legacyMac = loadAutoParkCarMac(prefs)?.uppercase()
            if (legacyMac != connectedMac.uppercase()) return Result.success()
            null
        }
        if (vehicle?.isArchived == true) return Result.success()

        startMotionWatch(context.applicationContext, prefs, connectedMac)
        return Result.success()
    }
}

fun enqueueMotionWatchStart(context: Context, connectedMac: String) {
    val request = OneTimeWorkRequestBuilder<MotionWatchStartWorker>()
        .setInputData(Data.Builder().putString(CONNECTED_MAC_KEY, connectedMac).build())
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        "motion_watch_start",
        androidx.work.ExistingWorkPolicy.REPLACE,
        request
    )
}
