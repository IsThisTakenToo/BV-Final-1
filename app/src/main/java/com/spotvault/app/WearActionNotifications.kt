package com.spotvault.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/** Shared by WearActionListenerService (Quick Pin, Quick Share, Navigate, Found — all handled
 * inline there) and WearQuickTrackWorker (Quick Track, which needs WorkManager's expedited-job
 * exemption to start TimerService as a foreground service — see that worker's own doc for why). */
object WearActionNotifications {
    private const val CHANNEL_ID = "wear_quick_actions"
    // Separate, higher-importance channel for the notifications that need a tap to actually
    // finish something (Navigate, Quick Share, missing location permission) — a plain DEFAULT
    // channel doesn't reliably show a heads-up banner, so a tap-required notification could sit
    // silently in the shade while the user is looking at their watch, not their phone, with no
    // reason to check it. Confirmations (Pin saved, Tracking started, Marked as found) stay on
    // the plain channel since there's nothing left to do once they're seen.
    private const val ACTION_NEEDED_CHANNEL_ID = "wear_action_needed"

    // Distinct per notification *kind* so one never silently clobbers an unrelated, possibly
    // still-unread one — e.g. a routine "Pin saved" confirmation replacing an unseen "Location
    // access needed" alert just because both used to share one hardcoded id. Reusing the same id
    // across repeat posts of the *same* kind is still intentional (a newer "Save failed" should
    // replace a stale one for the same action, not stack indefinitely).
    const val ID_QUICK_PIN = 9100
    const val ID_QUICK_TRACK = 9101
    const val ID_FOUND = 9102
    const val ID_LOCATION_PERMISSION = 9103
    const val ID_QUICK_SHARE = 9104
    const val ID_NAVIGATE = 9105

    fun post(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        contentIntent: Intent? = null,
        requestCode: Int = 0,
        actionNeeded: Boolean = false
    ) {
        // Same check TacticalQuickPins.kt's own hasNotificationPermission() does — posting
        // without it on API 33+ risks a SecurityException rather than a graceful no-op, so this
        // has to be checked explicitly rather than just trusting NotificationManagerCompat to
        // handle it silently.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannels(context)
        val effectiveIntent = contentIntent ?: run {
            // FLAG_ACTIVITY_NEW_TASK is required here (unlike a normal Activity-launched intent)
            // since both callers post from a background Service/Worker context, not an Activity.
            context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = effectiveIntent?.let {
            PendingIntent.getActivity(
                context,
                requestCode,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val channelId = if (actionNeeded) ACTION_NEEDED_CHANNEL_ID else CHANNEL_ID
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(if (actionNeeded) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .apply { if (pendingIntent != null) setContentIntent(pendingIntent) }
            .build()
        androidx.core.app.NotificationManagerCompat.from(context)
            .notify(notificationId, notification)
    }

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Watch Quick Actions",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Confirms Quick Pin/Track/Share/Navigate/Found actions triggered from your watch"
                    }
                )
            }
            // A channel's importance can't be changed after creation (Android silently ignores a
            // second createNotificationChannel() call with a different importance for the same
            // id) — this needs its own channel id rather than trying to bump CHANNEL_ID's.
            if (manager.getNotificationChannel(ACTION_NEEDED_CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        ACTION_NEEDED_CHANNEL_ID,
                        "Watch Action Needed",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Needs a tap to finish something started from your watch (Navigate, Quick Share, location access)"
                    }
                )
            }
        }
    }
}
