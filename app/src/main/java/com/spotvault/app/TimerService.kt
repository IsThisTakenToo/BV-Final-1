package com.spotvault.app

import android.media.MediaPlayer
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.CountDownTimer
import android.os.IBinder
import android.os.Build
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Keep pinned-notification note payload small — full notes live in Room, not prefs/binder. */
private const val PINNED_NOTIFICATION_NOTES_MAX_CHARS = 400

/** TIMER_ALERT channel id, varying by every setting that changes what the channel actually needs
 * to do — a channel's sound/vibration is locked in the moment Android first creates it and never
 * updates for an existing id on reinstall or a later settings change, so a distinct id per
 * configuration (same trick already used for the sound URI below) is the only way toggling either
 * one actually takes effect. Shared by MainActivity.createNotificationChannel() (creates it) and
 * TimerService.fireTenMinuteAlert() (posts to it) — both must compute the exact same id, or the
 * notification silently fails to post to a channel that was never created. */
fun timerAlertChannelId(prefs: android.content.SharedPreferences): String {
    val soundUriStr = prefs.getString("alarm_sound_uri", null)?.let { prefsSafeAlarmSoundUri(it) }
    val vibrationEnabled = prefs.getBoolean("vibration_enabled", true)
    val baseId = "TIMER_ALERT"
    return buildString {
        append(baseId)
        if (!soundUriStr.isNullOrEmpty()) append("_${soundUriStr.hashCode()}")
        append(if (vibrationEnabled) "_v1" else "_v0")
    }
}

/** Prefer the user's ringtone URI when it looks safe; otherwise the system default alarm. */
fun resolveAlarmSoundUri(prefs: android.content.SharedPreferences): android.net.Uri {
    val fallback = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
    val raw = prefs.getString("alarm_sound_uri", null)?.let { prefsSafeAlarmSoundUri(it) }
    if (raw.isNullOrEmpty()) return fallback
    val uri = runCatching { android.net.Uri.parse(raw) }.getOrNull() ?: return fallback
    val scheme = uri.scheme?.lowercase()
    // Ringtone picker yields content:; reject exotic schemes that can point at huge binaries.
    if (scheme != "content" && scheme != "android.resource") return fallback
    return uri
}

/** Looping expiry alarm — falls back to the default alarm tone if create() fails on a bad URI. */
fun startLoopingAlarmPlayer(context: android.content.Context, prefs: android.content.SharedPreferences): MediaPlayer? {
    val preferred = resolveAlarmSoundUri(prefs)
    val fallback = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
    return try {
        val player = MediaPlayer.create(context, preferred)
            ?: if (preferred != fallback) MediaPlayer.create(context, fallback) else null
        player?.also {
            it.isLooping = true
            it.start()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/** Creates the current TIMER_ALERT channel (sound + vibration) and deletes the previous id. */
fun ensureTimerAlertChannel(
    manager: NotificationManager,
    prefs: android.content.SharedPreferences
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val soundUriStr = prefs.getString("alarm_sound_uri", null)?.let { prefsSafeAlarmSoundUri(it) }
    val vibrationEnabled = prefs.getBoolean("vibration_enabled", true)
    val channelId = timerAlertChannelId(prefs)

    val alertChannel = android.app.NotificationChannel(
        channelId,
        "Timer Alert",
        NotificationManager.IMPORTANCE_HIGH
    )
    val audioAttributes = android.media.AudioAttributes.Builder()
        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
        .build()

    if (!soundUriStr.isNullOrEmpty()) {
        alertChannel.setSound(android.net.Uri.parse(soundUriStr), audioAttributes)
    } else {
        alertChannel.setSound(
            android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),
            audioAttributes
        )
    }
    alertChannel.enableVibration(vibrationEnabled)
    manager.createNotificationChannel(alertChannel)

    val previousAlertChannelId = prefs.getString("timer_alert_channel_id", null)
    if (!previousAlertChannelId.isNullOrEmpty() && previousAlertChannelId != channelId) {
        runCatching { manager.deleteNotificationChannel(previousAlertChannelId) }
    }
    prefs.edit().putString("timer_alert_channel_id", channelId).apply()
}

class TimerService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var countDownTimer: CountDownTimer? = null
    private lateinit var manager: NotificationManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pendingNotificationRetry: Runnable? = null
    private val retryHandler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }
    // Reused across countdown ticks — buildNotification used to decode/rotate the same photo on
    // every update (once a second in the last minute), which is both heap churn and main-thread
    // I/O for the whole life of an active pin. Cached until the path changes or the service dies.
    private var cachedNotificationBitmap: Bitmap? = null
    private var cachedNotificationPhotoPath: String? = null

    override fun onCreate() {
        super.onCreate()
        manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureNotificationChannels()
    }

    /** Ensures channels exist when FGS starts from background (MainActivity may never have run). */
    private fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val countdownChannel = android.app.NotificationChannel(
            CHANNEL_ID,
            "Timer Countdown",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { setSound(null, null) }
        manager.createNotificationChannel(countdownChannel)
        val silentChannel = android.app.NotificationChannel(
            CHANNEL_ID_SILENT,
            "Active Tracking (Silent)",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(silentChannel)

        // Ten-minute early-warning posts here. MainActivity.createNotificationChannel() also
        // creates/prunes this when the UI is open, but Boot / Watchdog / NotificationGuard can
        // resume the FGS without ever touching MainActivity — without this, fireTenMinuteAlert
        // would target a channel that does not exist yet.
        val prefs = getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        ensureTimerAlertChannel(manager, prefs)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        val isPinned = prefs.getBoolean("is_pinned", false)

        if (!isPinned || intent?.action == ACTION_STOP) {
            stopPinnedSession(prefs)
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_REPOST, ACTION_RESUME, ACTION_UPDATE_DETAILS -> {
                restorePinnedNotification(prefs)
                return START_STICKY
            }
        }

        val photoPath = prefs.getString("photo_path", "") ?: ""
        var timeMs = intent?.getLongExtra("TIME_MS", 0L) ?: 0L
        var isExpired = false

        if (intent?.action == ACTION_SILENCE_ALARM) {
            prefs.edit().putBoolean("is_alarm_ringing", false).apply()
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            postPinnedNotification(photoPath, "Vault timer expired!")
            return START_STICKY
        }

        if (intent?.action == ACTION_CANCEL_TIMER) {
            prefs.edit().putBoolean("is_alarm_ringing", false).apply()
            countDownTimer?.cancel()
            countDownTimer = null
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            prefs.edit().remove("timer_end_time").apply()
            timeMs = 0L
        } else {
            val endTime = prefs.getLong("timer_end_time", 0L)
            if (timeMs > 0) {
                prefs.edit().putLong("timer_end_time", System.currentTimeMillis() + timeMs).apply()
            } else if (intent == null || timeMs == 0L) {
                if (endTime > System.currentTimeMillis()) {
                    timeMs = endTime - System.currentTimeMillis()
                } else if (endTime > 0) {
                    isExpired = true
                }
            }
        }

        NotificationGuard.schedule(this)
        val notificationStatusText = resolveStatusText(prefs, timeMs, isExpired)
        postPinnedNotification(photoPath, notificationStatusText)
        startOrResumeCountdown(prefs, photoPath, timeMs, isExpired)

        return START_STICKY
    }

    /** Cancels any existing countdown and starts a fresh one for [timeMs] remaining — the single
     * place that owns "the alarm fires at zero, the 10-minute warning fires once." Called both
     * from the normal start path and from [restorePinnedNotification], which used to only repost
     * a one-time static notification without this: [NotificationGuard]'s every-2-minutes watchdog
     * alarm (and onTaskRemoved's own restart) both resume the service via ACTION_RESUME, which is
     * exactly what runs after the OS has killed this service's process outright (swiping the app
     * from Recents kills it on plenty of real devices despite the foreground service). Without
     * restarting the countdown here too, that resume left a frozen, no-longer-ticking notification
     * behind and — critically — meant onFinish() below could never fire again, so the expiry alarm
     * itself would silently never sound even though timer_end_time said it should have. */
    private fun startOrResumeCountdown(
        prefs: android.content.SharedPreferences,
        photoPath: String,
        timeMs: Long,
        isExpired: Boolean
    ) {
        countDownTimer?.cancel()
        countDownTimer = null

        if (timeMs <= 0) {
            if (isExpired && !prefs.getBoolean("is_alarm_ringing", false)) {
                // Process died past timer_end_time before onFinish could run — same audible
                // expiry path as a live countdown completing, not a silent "expired" label.
                soundExpiredAlarm(prefs, photoPath)
            } else if (isExpired) {
                postPinnedNotification(photoPath, "Vault timer expired!")
            }
            return
        }

        // Already at/under 10 minutes when this countdown starts (resume after kill, or a
        // sub-10 start) — don't fire the early warning as if we just crossed the threshold.
        // Exact 10:00 still fires on the first tick via the <= check below.
        var hasFiredTenMinAlert = timeMs > 0 && timeMs < 10 * 60_000L
        countDownTimer = object : CountDownTimer(timeMs, 1000) {
            private var lastMinTick = -1L

            override fun onTick(millisUntilFinished: Long) {
                val minsLeft = millisUntilFinished / 60000
                val secsLeft = millisUntilFinished / 1000
                val text = if (millisUntilFinished < 60000) {
                    "Time remaining: ${secsLeft}s"
                } else {
                    "Time remaining: $minsLeft minutes"
                }

                val shouldUpdate = if (millisUntilFinished < 60000) {
                    // Final minute: every 5s is enough for the countdown label and avoids
                    // rebuilding a BigPicture notification 60 times on low-RAM phones.
                    secsLeft % 5L == 0L || millisUntilFinished < 1500
                } else {
                    minsLeft != lastMinTick
                }
                if (shouldUpdate) {
                    lastMinTick = minsLeft
                    // Notification only — do NOT refresh Glance widgets on every tick.
                    // In-flight provideGlance() calls that still saw is_pinned=true were
                    // racing Found/clear and painting the tracking UI back over the idle UI.
                    manager.notify(NOTIFICATION_ID, buildNotification(photoPath, text))
                }

                if (!hasFiredTenMinAlert && millisUntilFinished <= 10 * 60_000L) {
                    hasFiredTenMinAlert = true
                    fireTenMinuteAlert()
                }
            }

            override fun onFinish() {
                soundExpiredAlarm(prefs, photoPath)
            }
        }.start()
    }

    private fun soundExpiredAlarm(prefs: android.content.SharedPreferences, photoPath: String) {
        val text = "Vault timer expired!"
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = startLoopingAlarmPlayer(this, prefs)
            // Only claim the alarm is ringing when audio actually started — otherwise the
            // UI/resume path shows "Silence Alarm" / re-ring logic for a silent failure.
            prefs.edit().putBoolean("is_alarm_ringing", mediaPlayer?.isPlaying == true).apply()
        } catch (e: Exception) {
            e.printStackTrace()
            prefs.edit().putBoolean("is_alarm_ringing", false).apply()
        }
        postPinnedNotification(photoPath, text)
    }

    private fun restorePinnedNotification(prefs: android.content.SharedPreferences) {
        if (!prefs.getBoolean("is_pinned", false)) {
            stopPinnedSession(prefs)
            return
        }
        NotificationGuard.schedule(this)
        val photoPath = prefs.getString("photo_path", "") ?: ""
        val endTime = prefs.getLong("timer_end_time", 0L)
        val isExpired = endTime in 1 until System.currentTimeMillis()
        val timeMs = if (endTime > System.currentTimeMillis()) endTime - System.currentTimeMillis() else 0L
        val alarmShouldBeRinging = prefs.getBoolean("is_alarm_ringing", false)
        val statusText = when {
            alarmShouldBeRinging -> "Vault timer expired!"
            timeMs > 0 -> resolveStatusText(prefs, timeMs, false)
            isExpired -> "Vault timer expired!"
            else -> SAVED_BEACON_TEXT
        }
        postPinnedNotification(photoPath, statusText)

        // This is what actually resumes a *live* countdown after the service's process was
        // killed outright — see startOrResumeCountdown's doc for why that matters. Without it,
        // ACTION_RESUME here (from onTaskRemoved, or from NotificationGuard's own watchdog alarm
        // firing every 2 minutes regardless of whether a restart was ever needed) just posted a
        // notification that would never update again and could never reach onFinish() to sound
        // the alarm.
        if (alarmShouldBeRinging) {
            // The process restarted mid-ring: is_alarm_ringing is still true from before the
            // kill, but this fresh instance's own mediaPlayer is null, so the sound has actually
            // gone silent with no live object left to silence — replay it rather than leave the
            // pref claiming an alarm that isn't actually audible anymore.
            if (mediaPlayer?.isPlaying != true) {
                replayExpiredAlarmSound(prefs)
            }
            postPinnedNotification(photoPath, "Vault timer expired!")
        } else {
            startOrResumeCountdown(prefs, photoPath, timeMs, isExpired)
        }
    }

    private fun replayExpiredAlarmSound(prefs: android.content.SharedPreferences) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = startLoopingAlarmPlayer(this, prefs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resolveStatusText(
        prefs: android.content.SharedPreferences,
        timeMs: Long,
        isExpired: Boolean
    ): String {
        return when {
            timeMs > 0 -> "Calculating time..."
            isExpired -> "Vault timer expired!"
            else -> SAVED_BEACON_TEXT
        }
    }

    private fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun postPinnedNotification(photoPath: String, text: String, isRetry: Boolean = false) {
        if (!canPostNotifications()) {
            // Without POST_NOTIFICATIONS, startForeground fails and the guard/watchdog would
            // restart this service in a loop. Keep the vault pin prefs; skip FGS until granted.
            NotificationGuard.cancel(this)
            stopSelf()
            return
        }
        val notification = buildNotification(photoPath, text)
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
            if (!isRetry) {
                // Right after boot the system can still be settling (Play services, location
                // providers, etc. not fully up yet) and briefly refuse a new foreground service;
                // one short retry covers that instead of silently giving up until the user
                // manually reopens the app. Reference kept + cancelled on every teardown path so
                // a "Found"/stop landing inside this 2s window can't have the retry fire on an
                // already-dead service instance and resurrect the notification.
                cancelPendingNotificationRetry()
                val retry = Runnable { postPinnedNotification(photoPath, text, isRetry = true) }
                pendingNotificationRetry = retry
                retryHandler.postDelayed(retry, 2000)
            } else {
                stopSelf()
            }
        }
    }

    private fun cancelPendingNotificationRetry() {
        pendingNotificationRetry?.let { retryHandler.removeCallbacks(it) }
        pendingNotificationRetry = null
    }

    private fun stopPinnedSession(prefs: android.content.SharedPreferences) {
        cancelPendingNotificationRetry()
        prefs.edit().putBoolean("is_alarm_ringing", false).apply()
        countDownTimer?.cancel()
        countDownTimer = null
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        // Quiet-save / ACTION_STOP used to leave the 10-minute early-warning notification up
        // after clearing the countdown — Found already cancels both IDs via ActiveTrackingHelper.
        manager.cancel(TEN_MINUTE_ALERT_ID)
        clearCachedNotificationBitmap()
        NotificationGuard.cancel(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun fireTenMinuteAlert() {
        if (!canPostNotifications()) return
        val prefs = getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("timer_early_warning", true)) return
        val channelId = timerAlertChannelId(prefs)
        val alert = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Vault Timer Alert")
            .setContentText("Your timer expires in 10 minutes!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        manager.notify(TEN_MINUTE_ALERT_ID, alert)
    }

    private fun clearCachedNotificationBitmap() {
        cachedNotificationBitmap?.recycle()
        cachedNotificationBitmap = null
        cachedNotificationPhotoPath = null
    }

    private fun notificationBitmapFor(photoPath: String): Bitmap? {
        if (photoPath.isEmpty()) {
            if (cachedNotificationPhotoPath != null) clearCachedNotificationBitmap()
            return null
        }
        if (photoPath == cachedNotificationPhotoPath) return cachedNotificationBitmap
        clearCachedNotificationBitmap()
        return try {
            val file = File(photoPath)
            if (!file.exists()) return null
            val exif = ExifInterface(photoPath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }
            // Downsampled — BigPictureStyle never displays this above a few hundred dp, and the
            // notification rebuilds on every countdown tick in the final minute.
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(photoPath, boundsOpts)
            var sample = 1
            val maxDim = 512
            while (boundsOpts.outWidth / sample > maxDim || boundsOpts.outHeight / sample > maxDim) {
                sample *= 2
            }
            val original = BitmapFactory.decodeFile(photoPath, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return null
            val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
            if (rotated !== original) original.recycle()
            cachedNotificationBitmap = rotated
            cachedNotificationPhotoPath = photoPath
            rotated
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildNotification(photoPath: String, text: String): Notification {
        lastNotificationPhotoPath = photoPath
        lastNotificationText = text
        val bitmap = notificationBitmapFor(photoPath)

        val clearIntent = Intent(this, ClearNotificationReceiver::class.java)
        val clearPendingIntent = PendingIntent.getBroadcast(
            this,
            20,
            clearIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            21,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prefs = getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        val lat = prefs.getCoord("lat").toFloat()
        val lng = prefs.getCoord("lng").toFloat()
        // With App Lock on, this action must not hand the tracked location straight to Maps from
        // a locked device — that's a real bypass of what App Lock is for. Route through
        // MainActivity's own gate instead (same as every other entry point); it already knows how
        // to resume this exact navigate-to-Maps intent once unlocked.
        val mapIntent = if (prefs.getBoolean(APP_LOCK_ENABLED_PREF, false)) {
            PremiumWidgetIntents.openMapsNavigation(this, lat.toDouble(), lng.toDouble())
        } else {
            // No setPackage() — ActiveTrackingHelper.launchNavigation() (the widget/relay-activity
            // path for the same action) already dropped this same restriction. Locking the intent
            // to Google Maps specifically means the button silently does nothing when tapped on any
            // device without it installed (Huawei phones since 2019 ship with no Google Play
            // Services at all, and some budget/regional Android phones omit it too) — even if
            // another installed app also handles this URI scheme. There's no way to add error
            // handling here the way launchNavigation() does with its try/catch, since this fires
            // later from a system notification tap, not a direct startActivity() call this code
            // controls — so leaving it unrestricted is the only lever available to make it resolve
            // successfully more often.
            val mapUri = android.net.Uri.parse("google.navigation:q=$lat,$lng&mode=w")
            Intent(Intent.ACTION_VIEW, mapUri)
        }
        val mapPendingIntent = PendingIntent.getActivity(
            this,
            22,
            mapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Separate from "Navigate to" above — that one uses google.navigation:, which always
        // launches straight into live turn-by-turn guidance the instant it's tapped, no preview or
        // choice of app first. This mirrors the app's own "Show Map" buttons instead (geo:, same
        // as PinnedStateView's Show Map / PremiumWidgetIntents.showMapIntent): just opens the pin
        // in whatever maps app the user has, letting them look before choosing to navigate.
        val showMapLabel = prefsSafeAddress(
            prefs.getString("current_address", "")?.ifBlank { "Saved Spot" } ?: "Saved Spot"
        )
        // Same App Lock bypass "Navigate to" above already guards against — this must not hand
        // the tracked location straight to an external maps app from a locked device either.
        val showMapIntentTarget = if (prefs.getBoolean(APP_LOCK_ENABLED_PREF, false)) {
            PremiumWidgetIntents.openMapsShow(this, lat.toDouble(), lng.toDouble())
        } else {
            val showMapUri = android.net.Uri.parse("geo:0,0?q=$lat,$lng(${android.net.Uri.encode(showMapLabel)})")
            Intent(Intent.ACTION_VIEW, showMapUri)
        }
        val showMapPendingIntent = PendingIntent.getActivity(
            this,
            26,
            showMapIntentTarget,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val locationDetails = (prefs.getString("location_details", "") ?: "")
            .take(PINNED_NOTIFICATION_NOTES_MAX_CHARS)

        val repostIntent = Intent(this, TimerService::class.java).apply { action = ACTION_REPOST }
        val repostPendingIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                this,
                23,
                repostIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getService(
                this,
                23,
                repostIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val silentPost = prefs.getBoolean(PINNED_NOTIFICATION_SILENT_PREF, false)
        if (silentPost) {
            prefs.edit().remove(PINNED_NOTIFICATION_SILENT_PREF).apply()
        }
        val channelId = if (silentPost) CHANNEL_ID_SILENT else CHANNEL_ID

        val pinnedVehicle = loadPinnedVehicle(prefs)
        val displayText = if (pinnedVehicle != null) {
            "${pinnedVehicle.name} — $text"
        } else {
            text
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(PINNED_BEACON_TITLE)
            .setContentText(displayText)
            .setOngoing(true)
            .setAutoCancel(false)
            .setDeleteIntent(repostPendingIntent)
            .addAction(android.R.drawable.ic_menu_delete, FOUND_BEACON_ACTION, clearPendingIntent)
            .addAction(android.R.drawable.ic_menu_directions, "Navigate to", mapPendingIntent)
            .addAction(android.R.drawable.ic_menu_mapmode, "Show Map", showMapPendingIntent)

        if (text == "Vault timer expired!" && mediaPlayer != null && mediaPlayer?.isPlaying == true) {
            val silenceIntent = Intent(this, TimerService::class.java).apply { action = ACTION_SILENCE_ALARM }
            val silencePendingIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    this,
                    25,
                    silenceIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            } else {
                PendingIntent.getService(
                    this,
                    25,
                    silenceIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            builder.addAction(android.R.drawable.ic_media_pause, "Silence Alarm", silencePendingIntent)
        }

        val lockScreenVisible = prefs.getBoolean("show_on_lock_screen", true)
        builder.setContentIntent(openPendingIntent)
            .setVisibility(if (lockScreenVisible) NotificationCompat.VISIBILITY_PUBLIC else NotificationCompat.VISIBILITY_SECRET)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (silentPost) {
            builder.setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
        }

        pinnedVehicle?.let { vehicle ->
            builder.setColor(vehicle.colorArgb)
        }

        if (locationDetails.isNotEmpty()) {
            builder.setSubText(locationDetails)
        }

        val summaryText = buildString {
            if (pinnedVehicle != null) append("${pinnedVehicle.name}\n")
            if (locationDetails.isNotEmpty()) append("$locationDetails\n")
            append(text)
        }

        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?)
                    .setSummaryText(summaryText)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
        }

        val notification = builder.build()
        notification.flags = notification.flags or
            Notification.FLAG_NO_CLEAR or
            Notification.FLAG_ONGOING_EVENT or
            Notification.FLAG_FOREGROUND_SERVICE
        return notification
    }

    private var cachedPinnedVehicleId: Int = -1
    private var cachedPinnedVehicle: Vehicle? = null
    private var vehicleFetchInFlight = false
    private var lastNotificationPhotoPath: String = ""
    private var lastNotificationText: String = ""

    /** Notifications rebuild on every countdown tick (once a second) — without caching, that's a
     * blocking Room query on the main thread every second for the life of an active timer. The
     * pinned vehicle can't change mid-session, so it only needs to be (re)fetched when the id
     * itself changes. On a cache miss this never blocks the caller (which can be running right
     * before a time-boxed startForeground() call) — it kicks the fetch off async and returns null
     * for this one build, then re-notifies with the same id/text once the vehicle lands. */
    private fun loadPinnedVehicle(prefs: android.content.SharedPreferences): Vehicle? {
        val id = prefs.getInt(PINNED_VEHICLE_ID_PREF, -1)
        if (id <= 0) {
            cachedPinnedVehicleId = -1
            cachedPinnedVehicle = null
            return null
        }
        if (id == cachedPinnedVehicleId) return cachedPinnedVehicle
        if (!vehicleFetchInFlight) {
            vehicleFetchInFlight = true
            serviceScope.launch {
                val vehicle = runCatching {
                    AppDatabase.getDatabase(applicationContext).vehicleDao().getById(id)
                }.getOrNull()
                cachedPinnedVehicleId = id
                cachedPinnedVehicle = vehicle
                vehicleFetchInFlight = false
                manager.notify(NOTIFICATION_ID, buildNotification(lastNotificationPhotoPath, lastNotificationText))
            }
        }
        return null
    }

    // Guaranteed last-chance cleanup regardless of *how* the service is stopped. The explicit
    // ACTION_STOP/ACTION_CANCEL_TIMER/!isPinned paths in onStartCommand already release
    // countDownTimer/mediaPlayer themselves, but nothing enforced that every stop went through
    // one of those — and this service can be holding a *looping* alarm MediaPlayer. The comment
    // on startOrResumeCountdown above already notes this exact device class kills foreground
    // services more aggressively than the docs promise ("swiping the app from Recents kills it
    // on plenty of real devices"); a service stop that skips onStartCommand's own paths (a
    // system-initiated stopService() from background execution limits, or any future code path
    // that calls stopSelf()/stopService() directly) would otherwise leave the alarm playing
    // indefinitely with no way to silence it short of killing the whole app process.
    override fun onDestroy() {
        cancelPendingNotificationRetry()
        serviceScope.cancel()
        countDownTimer?.cancel()
        countDownTimer = null
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        clearCachedNotificationBitmap()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        val prefs = getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_pinned", false)) {
            val serviceIntent = Intent(applicationContext, TimerService::class.java).apply {
                action = ACTION_RESUME
            }
            ContextCompat.startForegroundService(applicationContext, serviceIntent)
        }
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        const val ACTION_STOP = "STOP"
        const val ACTION_REPOST = "REPOST"
        const val ACTION_RESUME = "RESUME"
        const val ACTION_UPDATE_DETAILS = "UPDATE_DETAILS"
        const val ACTION_SILENCE_ALARM = "SILENCE_ALARM"
        const val ACTION_CANCEL_TIMER = "CANCEL_TIMER"

        private const val NOTIFICATION_ID = 1
        private const val TEN_MINUTE_ALERT_ID = 2
        // Renamed from TIMER_COUNTDOWN: a channel's importance is locked in the first time it's
        // created on a device and never updates on reinstall/upgrade, even if this code changes
        // later — so bumping IMPORTANCE_LOW to IMPORTANCE_DEFAULT for lock-screen visibility
        // silently did nothing for anyone who already had the old channel. A new ID forces a
        // fresh channel with today's settings for everyone, old installs included.
        private const val CHANNEL_ID = "TIMER_COUNTDOWN_V2"
        private const val CHANNEL_ID_SILENT = "TIMER_COUNTDOWN_SILENT"
        private const val PINNED_BEACON_TITLE = "Pin Saved"
        private const val SAVED_BEACON_TEXT = "This pin is saved."
        private const val FOUND_BEACON_ACTION = "Found"
    }
}
