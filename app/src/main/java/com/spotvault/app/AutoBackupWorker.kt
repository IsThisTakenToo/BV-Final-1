package com.spotvault.app

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_AUTO_BACKUP_ATTEMPTS = 5

class AutoBackupWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("auto_backup_enabled", false)) return Result.success()
        val treeUriString = prefs.getString("auto_backup_tree_uri", null) ?: return Result.success()
        val treeUri = Uri.parse(treeUriString)

        fun giveUp(message: String): Result {
            android.util.Log.e("AutoBackup", message)
            prefs.edit().putString("auto_backup_last_error", message).apply()
            // Cap retries so a revoked SAF tree does not burn battery forever.
            return if (runAttemptCount >= MAX_AUTO_BACKUP_ATTEMPTS) Result.success() else Result.retry()
        }

        return try {
            val treeDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: return giveUp("Backup folder is no longer available")
            if (!treeDoc.canWrite()) return giveUp("Backup folder is not writable")

            val db = AppDatabase.getDatabase(context)
            val dao = db.locationDao()
            val vehicleDao = db.vehicleDao()
            val spotPhotoDao = db.spotPhotoDao()
            val tagDao = db.tagDao()

            // Nothing changed since the last successful backup — skip writing an identical zip
            // so the chosen cloud folder doesn't grow forever with duplicates of the same data.
            val fingerprint = VaultBackupManager.computeFingerprint(dao, vehicleDao, spotPhotoDao, tagDao, prefs)
            if (fingerprint == prefs.getString("auto_backup_last_fingerprint", null)) {
                return Result.success()
            }

            // Distinct prefix from the manual "Export Vault Backup" button's default filename
            // ("DropPinVault_Backup_...") — sharing one meant a manual export saved into the same
            // auto-backup folder could get silently deleted by pruneOldBackups below once it
            // aged past the keep-count, the same as any other auto-generated file.
            val fileName = "DropPinVault_AutoBackup_${SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())}.zip"
            val newFile = treeDoc.createFile("application/zip", fileName)
                ?: return giveUp("Could not create backup file in the chosen folder")

            val result = VaultBackupManager.exportBackup(context, dao, vehicleDao, spotPhotoDao, tagDao, prefs, newFile.uri)

            if (result.isSuccess) {
                prefs.edit()
                    .putLong("auto_backup_last_success", System.currentTimeMillis())
                    .putString("auto_backup_last_fingerprint", fingerprint)
                    .remove("auto_backup_last_error")
                    .apply()
                pruneOldBackups(treeDoc, prefs.getInt("auto_backup_keep_count", 5))
                Result.success()
            } else {
                newFile.delete()
                val message = result.exceptionOrNull()?.message ?: "Export failed"
                android.util.Log.e("AutoBackup", "SAF auto-backup failed", result.exceptionOrNull())
                prefs.edit().putString("auto_backup_last_error", message).apply()
                if (runAttemptCount >= MAX_AUTO_BACKUP_ATTEMPTS) Result.success() else Result.retry()
            }
        } catch (e: Exception) {
            android.util.Log.e("AutoBackup", "SAF auto-backup crashed", e)
            prefs.edit().putString("auto_backup_last_error", e.message ?: e.javaClass.simpleName).apply()
            if (runAttemptCount >= MAX_AUTO_BACKUP_ATTEMPTS) Result.success() else Result.retry()
        }
    }

    private val backupFilenameTimestamp = Regex("""AutoBackup_(\d{4}-\d{2}-\d{2}_\d{4})\.zip$""")
    private val backupFilenameFormat = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US)

    /** Keeps only the most recent N auto-backups so the chosen destination doesn't grow
     * forever either — same "years of use" thinking as everywhere else. Sorted by the timestamp
     * embedded in the filename (which this worker controls and always sets correctly) rather
     * than [DocumentFile.lastModified] — many cloud-backed SAF providers (Drive, OneDrive, etc.)
     * report 0 or otherwise unreliable modification times, which could otherwise prune the
     * actual newest backups instead of the oldest. */
    private fun pruneOldBackups(treeDoc: DocumentFile, keepCount: Int) {
        val backups = treeDoc.listFiles()
            .filter { it.name?.startsWith("DropPinVault_AutoBackup_") == true }
            .sortedByDescending { file ->
                val match = backupFilenameTimestamp.find(file.name ?: "")
                val parsed = match?.let { runCatching { backupFilenameFormat.parse(it.groupValues[1])?.time }.getOrNull() }
                parsed ?: file.lastModified()
            }
        if (backups.size > keepCount) {
            backups.drop(keepCount).forEach { it.delete() }
        }
    }
}

fun scheduleAutoBackup(context: Context, frequency: String) {
    val (amount, unit) = if (frequency == "daily") {
        1L to java.util.concurrent.TimeUnit.DAYS
    } else {
        7L to java.util.concurrent.TimeUnit.DAYS
    }
    val request = androidx.work.PeriodicWorkRequestBuilder<AutoBackupWorker>(amount, unit).build()
    androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "auto_backup",
        androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

fun cancelAutoBackup(context: Context) {
    androidx.work.WorkManager.getInstance(context).cancelUniqueWork("auto_backup")
}
