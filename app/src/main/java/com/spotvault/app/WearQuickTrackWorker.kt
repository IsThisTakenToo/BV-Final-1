package com.spotvault.app

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters

/** Quick Track triggered from a paired watch needs to start TimerService as a foreground
 * service (quickActiveTrackPin's own startForegroundService(TimerService) call) — but
 * WearActionListenerService.onMessageReceived() runs with no foreground-service-start exemption
 * of its own (it can be woken from a fully-stopped process solely to deliver the MessageEvent),
 * so on API 31+ that call was silently hitting ForegroundServiceStartNotAllowedException, caught
 * by quietSaveTacticalPin's own try/catch, and falling back to a one-off notification instead of
 * TimerService's real persistent tracking notification — which then only actually appeared once
 * the user opened the app and MainActivity.resumePinnedTimerService() retried it from a genuine
 * foreground context.
 *
 * This mirrors AutoParkWorker's own already-proven fix for the identical problem: an expedited
 * WorkManager job gets a temporary exemption to start a foreground service that a plain background
 * Service call doesn't. Quick Pin and Found don't need this — quietSaveTacticalPin never starts a
 * new foreground service (only stops one via a plain startService() to an already-running
 * TimerService), and ActiveTrackingHelper.clearActiveTracking() only stops one too. */
class WearQuickTrackWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        val dao = AppDatabase.getDatabase(context).locationDao()
        when (quickActiveTrackPin(context, dao, prefs, quickTrackOption())) {
            is QuietSaveResult.Saved -> WearActionNotifications.post(
                context,
                notificationId = WearActionNotifications.ID_QUICK_TRACK,
                title = "Tracking started from your watch",
                body = "Your spot is in the Vault."
            )
            QuietSaveResult.Failed -> WearActionNotifications.post(
                context,
                notificationId = WearActionNotifications.ID_QUICK_TRACK,
                title = "Save failed",
                body = "Couldn't save the spot your watch sent. Please try again."
            )
            QuietSaveResult.NeedsNotificationPermission -> Unit
        }
        return Result.success()
    }
}

private const val UNIQUE_WORK_NAME = "wear_quick_track"

fun enqueueWearQuickTrack(context: Context) {
    val request = OneTimeWorkRequestBuilder<WearQuickTrackWorker>()
        .apply {
            // See AutoParkWorker's own enqueue for the same gate — expedited work needs
            // getForegroundInfo() overridden pre-S, which this worker doesn't do.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }
        }
        .build()
    // REPLACE, not KEEP — a double-tap on the watch's Quick Track button (see the tile's own
    // debounce, which already blocks most of these) should still only ever result in one active
    // session, not two queued attempts racing each other.
    WorkManager.getInstance(context).enqueueUniqueWork(
        UNIQUE_WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        request
    )
}
