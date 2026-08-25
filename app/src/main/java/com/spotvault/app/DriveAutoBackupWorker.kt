package com.spotvault.app

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

private const val MAX_DRIVE_AUTO_BACKUP_ATTEMPTS = 5

/** Silent weekly backup to the signed-in Google account's Drive appDataFolder — the "kept safe
 * even if you never open Settings" path described in [WelcomeOnboardingScreen]'s Drive step. Runs
 * on unmetered Wi-Fi only, so it can never surprise the user with mobile data usage — but does
 * NOT require charging: the backup payload (a JSON+photos zip, usually well under what a single
 * app update download costs) isn't heavy enough to justify silently skipping weeks in a row for
 * anyone whose charging and Wi-Fi time don't happen to overlap, which is common enough that
 * requiring both made the "automatic weekly backup" promise unreliable in practice. This is
 * deliberately a separate worker from the existing SAF-based [AutoBackupWorker] — they write to
 * different destinations and one user can have either, both, or neither enabled. */
class DriveAutoBackupWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("drive_connected", false)) return Result.success()

        val tokenResult = DriveSyncManager.silentAccessToken(context)
        val accessToken = tokenResult.getOrElse { e ->
            if (e is DriveReauthRequiredException) {
                // Revoked/expired consent won't fix itself by silently retrying next week —
                // clearing this flips GoogleDriveBackupSection back to "not connected" the next
                // time Settings is opened, so the user actually finds out and can reconnect,
                // instead of "Last backup" just quietly stopping while the toggle still reads on.
                prefs.edit().putBoolean("drive_connected", false).apply()
                return Result.success()
            }
            // Transient Play Services / network failures should retry, not look "healthy".
            val message = e.message ?: e.javaClass.simpleName
            prefs.edit().putString("drive_last_backup_error", prefsSafeError(message)).apply()
            return if (runAttemptCount >= MAX_DRIVE_AUTO_BACKUP_ATTEMPTS) Result.success() else Result.retry()
        }

        return try {
            val db = AppDatabase.getDatabase(context)
            val dao = db.locationDao()
            val vehicleDao = db.vehicleDao()
            val spotPhotoDao = db.spotPhotoDao()
            val tagDao = db.tagDao()

            // Same "skip if nothing changed" fingerprint check as the SAF auto-backup worker —
            // no reason to re-upload an identical zip every week.
            val fingerprint = VaultBackupManager.computeFingerprint(dao, vehicleDao, spotPhotoDao, tagDao, prefs)
            if (fingerprint == prefs.getString("drive_last_backup_fingerprint", null)) {
                return Result.success()
            }

            // uploadBackup() persists drive_last_backup_success/fingerprint itself on success,
            // so every caller (this worker, onboarding's connect, Settings' "Back Up Now") gets
            // consistent bookkeeping from one place — nothing left to write here.
            val result = DriveSyncManager.uploadBackup(context, dao, vehicleDao, spotPhotoDao, tagDao, prefs, accessToken)
            val failure = result.exceptionOrNull()
            if (result.isSuccess) {
                prefs.edit().remove("drive_last_backup_error").apply()
                Result.success()
            } else if (failure is DriveSyncConflictException) {
                // Another device/session backed up different data since this one last synced —
                // retrying won't resolve that, only the user choosing a side can (Settings surfaces
                // this flag as a conflict banner, same RESTORE_FROM_DRIVE/OVERWRITE_DRIVE_BACKUP
                // choice as the connect-time conflict). Not treated as an error state either —
                // "Last backup" stays whatever it last legitimately was rather than showing a scary
                // failure message for something that isn't a failure so much as a decision pending.
                android.util.Log.w("DriveAutoBackup", "Drive sync conflict detected — awaiting user resolution")
                prefs.edit().putBoolean("drive_sync_conflict_pending", true).apply()
                Result.success()
            } else {
                val message = failure?.message ?: "Drive upload failed"
                android.util.Log.e("DriveAutoBackup", "Drive auto-backup failed", failure)
                prefs.edit().putString("drive_last_backup_error", prefsSafeError(message)).apply()
                if (runAttemptCount >= MAX_DRIVE_AUTO_BACKUP_ATTEMPTS) Result.success() else Result.retry()
            }
        } catch (e: Exception) {
            android.util.Log.e("DriveAutoBackup", "Drive auto-backup crashed", e)
            prefs.edit().putString("drive_last_backup_error", prefsSafeError(e.message ?: e.javaClass.simpleName)).apply()
            if (runAttemptCount >= MAX_DRIVE_AUTO_BACKUP_ATTEMPTS) Result.success() else Result.retry()
        }
    }
}

fun scheduleDriveAutoBackup(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.UNMETERED)
        .build()
    val request = PeriodicWorkRequestBuilder<DriveAutoBackupWorker>(7, TimeUnit.DAYS)
        .setConstraints(constraints)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "drive_auto_backup",
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

fun cancelDriveAutoBackup(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork("drive_auto_backup")
}
