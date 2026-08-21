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
        return prefs.getCoord("lat") to prefs.getCoord("lng")
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

/**
 * Active-tracking / motion coords used to be stored as Float (~meter error). Prefer a string
 * double; still accept legacy float values so existing pins keep working.
 */
fun android.content.SharedPreferences.getCoord(key: String, default: Double = 0.0): Double {
    getString(key, null)?.toDoubleOrNull()?.let { return it }
    return try {
        getFloat(key, default.toFloat()).toDouble()
    } catch (_: ClassCastException) {
        default
    }
}

fun android.content.SharedPreferences.Editor.putCoord(key: String, value: Double): android.content.SharedPreferences.Editor {
    remove(key)
    return putString(key, value.toString())
}

/** Prefs/notification copy of notes — full notepad stays in Room only. */
const val PINNED_PREFS_NOTES_MAX_CHARS = 800
const val PINNED_PREFS_ADDRESS_MAX_CHARS = 400
/** Matches backup import — browse still loads full title/address columns. */
const val SPOT_TITLE_MAX_CHARS = 200
const val SPOT_ADDRESS_MAX_CHARS = PINNED_PREFS_ADDRESS_MAX_CHARS
const val SPOT_CITY_STATE_MAX_CHARS = 80

fun prefsSafeLocationDetails(notes: String): String = notes.take(PINNED_PREFS_NOTES_MAX_CHARS)

fun prefsSafeAddress(address: String): String = address.take(PINNED_PREFS_ADDRESS_MAX_CHARS)

fun prefsSafeTitle(title: String): String = title.take(SPOT_TITLE_MAX_CHARS)

/** SAF/Drive error strings shown in Settings — keep prefs XML bounded. */
const val PREFS_ERROR_MAX_CHARS = 512

fun prefsSafeError(message: String): String = message.take(PREFS_ERROR_MAX_CHARS)

/** Vault / Favorites Hub search — paste/voice must not force a 3k-row full browse + Bundle bloat. */
const val VAULT_SEARCH_QUERY_MAX_CHARS = 80

fun prefsSafeSearchQuery(query: String): String = query.take(VAULT_SEARCH_QUERY_MAX_CHARS)

/** Pinned tracking photo path in prefs / Glance — absolute paths stay short; restore can inflate. */
const val PINNED_PREFS_PHOTO_PATH_MAX_CHARS = 1_024

fun prefsSafePhotoPath(path: String): String = path.take(PINNED_PREFS_PHOTO_PATH_MAX_CHARS)

/** Ringtone picker / restore URI stored in prefs — keep XML and MediaPlayer.create bounded. */
const val ALARM_SOUND_URI_MAX_CHARS = 2_048

fun prefsSafeAlarmSoundUri(uri: String): String = uri.take(ALARM_SOUND_URI_MAX_CHARS)

/** Soft ceiling when copying a gallery pick into app storage before compress. */
const val MAX_GALLERY_IMPORT_BYTES = 40L * 1024 * 1024

fun java.io.InputStream.copyToLimited(out: java.io.OutputStream, maxBytes: Long): Long {
    val buffer = ByteArray(64 * 1024)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) {
            throw java.io.IOException("Picked media exceeds the ${maxBytes / (1024 * 1024)} MB limit")
        }
        out.write(buffer, 0, read)
    }
    return total
}
