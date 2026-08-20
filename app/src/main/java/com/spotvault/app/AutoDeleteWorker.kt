package com.spotvault.app

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Every selectable auto-delete retention window. */
enum class AutoDeleteInterval(val id: String, val label: String, val days: Long) {
    TWO_DAYS("2d", "Older than 2 days", 2),
    WEEK("7d", "Older than 1 week", 7),
    MONTH("30d", "Older than 30 days", 30),
    QUARTER("90d", "Older than 90 days", 90),
    HALF_YEAR("180d", "Older than 6 months", 180),
    YEAR("365d", "Older than 1 year", 365);

    companion object {
        fun fromId(id: String?): AutoDeleteInterval = entries.firstOrNull { it.id == id } ?: MONTH
    }
}

/** Confirmation step for turning Auto Delete on — picking a retention window is required before
 * it can take effect, so switching the toggle on can never silently start deleting spots on
 * whatever interval happened to be last selected (or the 30-day default) without the user
 * actively choosing one first. Turning it back off doesn't need this — only enabling it does. */
@Composable
fun AutoDeleteIntervalPickerDialog(
    initialInterval: AutoDeleteInterval,
    onConfirm: (AutoDeleteInterval) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(initialInterval) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpotVaultColors.Surface,
        titleContentColor = SpotVaultColors.OnSurface,
        textContentColor = SpotVaultColors.Muted,
        title = { Text("Turn On Auto Delete") },
        text = {
            Column {
                Text(
                    "Spots older than the interval you pick move to Recently Deleted automatically. Favorites and archived spots are never touched.",
                    fontSize = 12.sp,
                    color = SpotVaultColors.Muted,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                AutoDeleteInterval.entries.forEach { interval ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selected = interval }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == interval,
                            onClick = { selected = interval },
                            colors = RadioButtonDefaults.colors(selectedColor = SpotVaultColors.Teal)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(interval.label, color = SpotVaultColors.OnSurface)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("Turn On", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SpotVaultColors.Muted)
            }
        }
    )
}

/** Runs once a day. Soft-deletes old spots when Auto Delete is enabled (favorites/archived/
 * wishlist never touched). Always hard-purges Recently Deleted older than 30 days so photo
 * files don't linger forever for users who rarely open the app. Soft-delete still goes through
 * Recently Deleted first — enabling Auto Delete can't silently destroy recovery. */
class AutoDeleteWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)

        return try {
            val db = AppDatabase.getDatabase(context)
            val dao = db.locationDao()

            // Hard-purge Recently Deleted older than 30 days even when Auto Delete is off —
            // MainActivity.onCreate used to be the only place this ran, so widget-/BT-only users
            // who rarely open the app kept deleted photo files on disk indefinitely.
            val recentlyDeletedCutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
            val coverPaths = dao.getDeletedCoverPathsOlderThan(recentlyDeletedCutoff)
            coverPaths.forEach { spot ->
                if (spot.imagePath.isNotEmpty()) {
                    runCatching { java.io.File(spot.imagePath).delete() }
                }
            }
            dao.getExtraPhotoPathsDeletedOlderThan(recentlyDeletedCutoff).forEach { path ->
                runCatching { java.io.File(path).delete() }
            }
            if (coverPaths.isNotEmpty()) {
                dao.purgeDeletedOlderThan(recentlyDeletedCutoff)
                db.tagDao().recomputeAllUsageCounts()
            }

            if (prefs.getBoolean("auto_delete_enabled", false)) {
                val interval = AutoDeleteInterval.fromId(prefs.getString("auto_delete_interval", AutoDeleteInterval.MONTH.id))
                val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(interval.days)
                val candidateIds = dao.getAutoDeleteCandidateIds(cutoff)
                candidateIds.forEach { dao.softDeleteSpot(it) }
                prefs.edit().putLong("auto_delete_last_run", System.currentTimeMillis()).apply()
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

fun scheduleAutoDelete(context: Context) {
    val request = PeriodicWorkRequestBuilder<AutoDeleteWorker>(1, TimeUnit.DAYS).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "auto_delete",
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

/** Soft-delete is gated by the auto_delete_enabled pref inside [AutoDeleteWorker] — do not cancel
 * the unique work itself, or the daily Recently Deleted hard-purge stops too. */
@Suppress("UNUSED_PARAMETER")
fun cancelAutoDelete(context: Context) {
    // Intentionally left as a no-op for WorkManager cancellation. Kept so existing call sites
    // that "turn Auto Delete off" still compile; the worker remains scheduled for the 30-day purge.
}
