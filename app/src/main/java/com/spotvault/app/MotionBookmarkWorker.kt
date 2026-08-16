package com.spotvault.app

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters

private const val UNIQUE_WORK_NAME = "motion_bookmark_fetch"

/**
 * Runs when a watched "left the vehicle" transition fires. Fetches a fresh location and caches it
 * as a bookmark — it never saves anything to the vault itself. Runs as WorkManager (not inline in
 * [MotionActivityReceiver]) because a real GPS fix can take several seconds, longer than a
 * BroadcastReceiver's execution budget safely allows.
 */
class MotionBookmarkWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        if (!isAutoParkEnabled(prefs) || !isMotionAutoParkEnabled(prefs)) return Result.success()
        // Same reasoning as AutoParkWorker's check — this also runs from a background context
        // (a transition broadcast, no foreground presence), so the fetch below would otherwise be
        // silently denied on API 29+ without ACCESS_BACKGROUND_LOCATION. This bookmark is only
        // ever a nice-to-have (see below), so skipping outright is strictly correct here, not
        // just a battery-saving nicety.
        if (!hasBackgroundLocationPermission(context)) return Result.success()

        val mac = currentMotionWatchMac(prefs) ?: return Result.success()

        // Claimed before the slow GPS fetch below — see markMotionBookmarkFetchStarted's doc for
        // why this can't just rely on WorkManager's REPLACE-policy cancellation alone.
        val fetchToken = markMotionBookmarkFetchStarted(prefs)

        val location = resolveCurrentLocation(context.applicationContext, prefs)
        if (location == null) {
            // This bookmark is only ever a nice-to-have — the real save on disconnect falls back
            // to its own live GPS fetch with no bookmark at all, exactly like Motion & Fitness
            // being off. Retrying forever (e.g. stuck with no fix at all in an underground
            // garage, for as long as the watch stays armed) isn't worth it for something this
            // disposable; giving up after a few attempts and letting the real save handle it
            // is strictly simpler and no less correct.
            return if (runAttemptCount < 3) Result.retry() else Result.success()
        }

        if (!isMotionBookmarkFetchCurrent(prefs, fetchToken)) {
            // A newer transition superseded this attempt while the GPS fetch was in flight — this
            // fix is stale now, so discard it rather than clobber whatever the current attempt
            // already wrote (or is still about to write).
            return Result.success()
        }

        saveMotionBookmark(prefs, mac, location.first, location.second)
        return Result.success()
    }
}

fun enqueueMotionBookmarkFetch(context: Context) {
    val request = OneTimeWorkRequestBuilder<MotionBookmarkWorker>()
        .apply {
            // Expedited work runs via a compat foreground-service path pre-S, which requires a
            // CoroutineWorker to override getForegroundInfo() — this worker doesn't (it's a
            // silent background fetch, nothing to show a notification for), so requesting
            // expedited pre-S would fail outright. The priority boost is only requested where
            // it doesn't carry that requirement.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }
        }
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        UNIQUE_WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        request
    )
}
