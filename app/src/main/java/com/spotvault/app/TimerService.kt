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
import android.graphics.Canvas
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
import kotlinx.coroutines.runBlocking

class TimerService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var countDownTimer: CountDownTimer? = null
    private lateinit var manager: NotificationManager

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
                postPinnedNotification(photoPath, "Vault timer expired!")
            }
            return
        }

        var hasFiredTenMinAlert = false
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

                val shouldUpdate = millisUntilFinished < 60000 || minsLeft != lastMinTick
                if (shouldUpdate) {
                    lastMinTick = minsLeft
                    // Notification only — do NOT refresh Glance widgets on every tick.
                    // In-flight provideGlance() calls that still saw is_pinned=true were
                    // racing Found/clear and painting the tracking UI back over the idle UI.
                    manager.notify(NOTIFICATION_ID, buildNotification(photoPath, text))
                }

                if (minsLeft == 10L && !hasFiredTenMinAlert) {
                    hasFiredTenMinAlert = true
                    fireTenMinuteAlert()
                }
            }

            override fun onFinish() {
                val text = "Vault timer expired!"
                val soundUriStr = prefs.getString("alarm_sound_uri", null)
                val uri = if (!soundUriStr.isNullOrEmpty()) {
                    android.net.Uri.parse(soundUriStr)
                } else {
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                }

                try {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer.create(this@TimerService, uri)
                    mediaPlayer?.isLooping = true
                    mediaPlayer?.start()
                    prefs.edit().putBoolean("is_alarm_ringing", true).apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                postPinnedNotification(photoPath, text)
            }
        }.start()
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
        val soundUriStr = prefs.getString("alarm_sound_uri", null)
        val uri = if (!soundUriStr.isNullOrEmpty()) {
            android.net.Uri.parse(soundUriStr)
        } else {
            android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
        }
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, uri)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
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
        val notification = buildNotification(photoPath, text)
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
            if (!isRetry) {
                // Right after boot the system can still be settling (Play services, location
                // providers, etc. not fully up yet) and briefly refuse a new foreground service;
                // one short retry covers that instead of silently giving up until the user
                // manually reopens the app.
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    postPinnedNotification(photoPath, text, isRetry = true)
                }, 2000)
            } else {
                stopSelf()
            }
        }
    }

    private fun stopPinnedSession(prefs: android.content.SharedPreferences) {
        prefs.edit().putBoolean("is_alarm_ringing", false).apply()
        countDownTimer?.cancel()
        countDownTimer = null
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        NotificationGuard.cancel(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun fireTenMinuteAlert() {
        if (!canPostNotifications()) return
        val prefs = getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("timer_early_warning", true)) return
        val soundUriStr = prefs.getString("alarm_sound_uri", null)
        val baseId = "TIMER_ALERT"
        val channelId = if (soundUriStr != null) "${baseId}_${soundUriStr.hashCode()}" else baseId
        val alert = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Vault Timer Alert")
            .setContentText("Your timer expires in 10 minutes!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        manager.notify(TEN_MINUTE_ALERT_ID, alert)
    }

    private fun buildNotification(photoPath: String, text: String): Notification {
        var bitmap: Bitmap? = null
        if (photoPath.isNotEmpty()) {
            try {
                val file = File(photoPath)
                if (file.exists()) {
                    val exif = ExifInterface(photoPath)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    // Downsampled — this notification rebuilds on every countdown tick (once a
                    // second in the final minute) for the whole life of an active tracking
                    // session, so a full, uncompressed camera-resolution decode here churned tens
                    // of MB of heap every tick. BigPictureStyle never displays this above a few
                    // hundred dp anyway.
                    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(photoPath, boundsOpts)
                    var sample = 1
                    val maxDim = 1024
                    while (boundsOpts.outWidth / sample > maxDim || boundsOpts.outHeight / sample > maxDim) {
                        sample *= 2
                    }
                    val original = BitmapFactory.decodeFile(photoPath, BitmapFactory.Options().apply { inSampleSize = sample })
                    if (original != null) {
                        val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
                        if (rotated !== original) original.recycle()
                        bitmap = rotated
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

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
        val lat = prefs.getFloat("lat", 0f)
        val lng = prefs.getFloat("lng", 0f)
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

        val locationDetails = prefs.getString("location_details", "") ?: ""

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
            val drawable = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation)
            if (drawable != null) {
                val fallbackBitmap = Bitmap.createBitmap(
                    if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 400,
                    if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 400,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(fallbackBitmap)
                canvas.drawColor(android.graphics.Color.LTGRAY)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                builder.setLargeIcon(fallbackBitmap)
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
            } else {
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
            }
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

    /** Notifications rebuild on every countdown tick (once a second) — without caching, that's a
     * blocking Room query on the main thread every second for the life of an active timer. The
     * pinned vehicle can't change mid-session, so it only needs to be (re)fetched when the id
     * itself changes. */
    private fun loadPinnedVehicle(prefs: android.content.SharedPreferences): Vehicle? {
        val id = prefs.getInt(PINNED_VEHICLE_ID_PREF, -1)
        if (id <= 0) {
            cachedPinnedVehicleId = -1
            cachedPinnedVehicle = null
            return null
        }
        if (id == cachedPinnedVehicleId) return cachedPinnedVehicle
        val vehicle = runBlocking {
            AppDatabase.getDatabase(applicationContext).vehicleDao().getById(id)
        }
        cachedPinnedVehicleId = id
        cachedPinnedVehicle = vehicle
        return vehicle
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
        countDownTimer?.cancel()
        countDownTimer = null
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
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
