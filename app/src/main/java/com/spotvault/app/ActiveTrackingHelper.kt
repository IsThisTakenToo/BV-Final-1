package com.spotvault.app

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** Shared active-tracking state used by the home screen, notifications, and quick-action widget. */
object ActiveTrackingHelper {

    private const val PREFS_NAME = "SpotVaultPrefs"

    fun isActive(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("is_pinned", false)

    fun pinnedCoordinates(context: Context): Pair<Double, Double>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("is_pinned", false)) return null
        val lat = prefs.getFloat("lat", 0f).toDouble()
        val lng = prefs.getFloat("lng", 0f).toDouble()
        return lat to lng
    }

    fun clearActiveTracking(context: Context, refreshWidgets: Boolean = true) {
        clearTrackingPrefs(context)
        // Refresh WHILE TimerService is still a foreground service (process priority elevated).
        // Stopping the service first let the process drop priority mid-repaint — which is why
        // Track (starts a foreground service) updated the widget but Found often did not.
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        WidgetThemeHelper.bumpWidgetRevision(prefs)
        if (refreshWidgets) {
            WidgetThemeHelper.refreshAllWidgets(context)
        }
        stopTrackingServiceAndNotifications(context)
    }

    /** Clears tracking prefs only — caller may refresh widgets before [stopTrackingServiceAndNotifications]. */
    fun clearTrackingPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val committed = prefs.edit()
            .putBoolean("is_pinned", false)
            .remove("photo_path")
            .remove("lat")
            .remove("lng")
            .remove("location_details")
            .remove("category")
            .remove("timer_end_time")
            .remove("current_address")
            .remove(PINNED_VEHICLE_ID_PREF)
            .putBoolean("is_alarm_ringing", false)
            .commit()

        if (!committed) {
            // Must stay .commit() (sync) — .apply() is async and can race a widget refresh
            // that still reads is_pinned=true.
            prefs.edit()
                .putBoolean("is_pinned", false)
                .remove("photo_path")
                .remove("lat")
                .remove("lng")
                .remove("location_details")
                .remove("category")
                .remove("timer_end_time")
                .remove("current_address")
                .remove(PINNED_VEHICLE_ID_PREF)
                .putBoolean("is_alarm_ringing", false)
                .commit()
        }
    }

    fun stopTrackingServiceAndNotifications(context: Context) {
        context.startService(
            Intent(context, TimerService::class.java).apply { action = TimerService.ACTION_STOP }
        )
        NotificationGuard.cancel(context)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(1)
        manager.cancel(2)
    }

    fun launchNavigation(context: Context): Boolean {
        val coords = pinnedCoordinates(context)
        if (coords == null) {
            Toast.makeText(context, "Location coordinates not available", Toast.LENGTH_SHORT).show()
            return false
        }
        val (lat, lng) = coords
        if (lat == 0.0 && lng == 0.0) {
            Toast.makeText(context, "Location coordinates not available", Toast.LENGTH_SHORT).show()
            return false
        }
        val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=w")
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No navigation app found", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
