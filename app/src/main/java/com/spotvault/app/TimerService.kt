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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.File

class TimerService : Service() {
    private var mediaPlayer: MediaPlayer? = null

    private var countDownTimer: CountDownTimer? = null
    private lateinit var manager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        val isPinned = prefs.getBoolean("is_pinned", false)
        if (!isPinned || intent?.action == "STOP") {
            prefs.edit().putBoolean("is_alarm_ringing", false).apply()
            countDownTimer?.cancel()
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val photoPath = prefs.getString("photo_path", "") ?: ""
        var timeMs = intent?.getLongExtra("TIME_MS", 0L) ?: 0L
        
        var isExpired = false
        if (intent?.action == "SILENCE_ALARM") {
            prefs.edit().putBoolean("is_alarm_ringing", false).apply()
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this, SpotVaultWidget::class.java))
            for (id in appWidgetIds) {
                SpotVaultWidget.updateAppWidget(this, appWidgetManager, id)
            }
            val compactWidgetIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this, CompactSpotVaultWidget::class.java))
            for (id in compactWidgetIds) {
                CompactSpotVaultWidget.updateAppWidget(this, appWidgetManager, id)
            }
            
            val photoPath = prefs.getString("photo_path", "") ?: ""
            val notification = buildNotification(photoPath, "Vault timer expired!")
            manager.notify(1, notification)
            return START_STICKY
        }
        
        if (intent?.action == "CANCEL_TIMER") {
            prefs.edit().putBoolean("is_alarm_ringing", false).apply()
            countDownTimer?.cancel()
            countDownTimer = null
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            prefs.edit().remove("timer_end_time").apply()
            timeMs = 0L
            
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this, SpotVaultWidget::class.java))
            for (id in appWidgetIds) {
                SpotVaultWidget.updateAppWidget(this, appWidgetManager, id)
            }
            val compactWidgetIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this, CompactSpotVaultWidget::class.java))
            for (id in compactWidgetIds) {
                CompactSpotVaultWidget.updateAppWidget(this, appWidgetManager, id)
            }
        } else {
            // Save or resume timer
            val endTime = prefs.getLong("timer_end_time", 0L)
            
            if (timeMs > 0) {
                val newEndTime = System.currentTimeMillis() + timeMs
                prefs.edit().putLong("timer_end_time", newEndTime).apply()
            } else if (intent == null || timeMs == 0L) {
                if (endTime > System.currentTimeMillis()) {
                    timeMs = endTime - System.currentTimeMillis()
                } else if (endTime > 0) {
                    isExpired = true
                }
            }
        }

        val notificationStatusText = when {
            timeMs > 0 -> "Calculating time..."
            isExpired -> "Vault timer expired!"
            else -> "This spot is saved."
        }
        val notification = buildNotification(photoPath, notificationStatusText)
        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
        
        try {
            startForeground(1, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (timeMs > 0) {
            countDownTimer?.cancel()
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
                        manager.notify(1, buildNotification(photoPath, text))
                        
                        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this@TimerService)
                        val appWidgetIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this@TimerService, SpotVaultWidget::class.java))
                        for (id in appWidgetIds) {
                            SpotVaultWidget.updateAppWidget(this@TimerService, appWidgetManager, id)
                        }
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
                    
                    try {
                        startForeground(1, buildNotification(photoPath, text))
                    } catch (e: Exception) {
                        manager.notify(1, buildNotification(photoPath, text))
                    }
                    
                    val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this@TimerService)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this@TimerService, SpotVaultWidget::class.java))
                    for (id in appWidgetIds) {
                        SpotVaultWidget.updateAppWidget(this@TimerService, appWidgetManager, id)
                    }
                    val compactWidgetIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this@TimerService, CompactSpotVaultWidget::class.java))
                    for (id in compactWidgetIds) {
                        CompactSpotVaultWidget.updateAppWidget(this@TimerService, appWidgetManager, id)
                    }
                    
                }
            }.start()
        } else {
            val statusText = if (isExpired) "Vault timer expired!" else "This spot is saved."
            try {
                startForeground(1, buildNotification(photoPath, statusText))
            } catch (e: Exception) {
                manager.notify(1, buildNotification(photoPath, statusText))
            }
        }

        return START_STICKY
    }


    private fun fireTenMinuteAlert() {
        val prefs = getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
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
        manager.notify(2, alert)
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
                    val original = BitmapFactory.decodeFile(photoPath)
                    bitmap = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val clearIntent = Intent(this, ClearNotificationReceiver::class.java)
        val clearPendingIntent = PendingIntent.getBroadcast(this, 0, clearIntent, PendingIntent.FLAG_IMMUTABLE)

        val openIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)

        val prefs = getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("lat", 0f)
        val lng = prefs.getFloat("lng", 0f)
        val mapUri = android.net.Uri.parse("google.navigation:q=$lat,$lng&mode=w")
        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        val mapPendingIntent = PendingIntent.getActivity(this, 0, mapIntent, PendingIntent.FLAG_IMMUTABLE)

        val locationDetails = prefs.getString("location_details", "") ?: ""
        val category = prefs.getString("category", "Other") ?: "Other"
        
        val repostIntent = Intent(this, TimerService::class.java).apply { action = "REPOST" }
        val repostPendingIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(this, 1, repostIntent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(this, 1, repostIntent, PendingIntent.FLAG_IMMUTABLE)
        }
        
        val exportIntent = Intent(this, ExportReceiver::class.java).apply {
            putExtra("PHOTO_PATH", photoPath)
        }
        val exportPendingIntent = PendingIntent.getBroadcast(this, 10, exportIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val channelIdBuilder = "TIMER_COUNTDOWN"
        val builder = NotificationCompat.Builder(this, channelIdBuilder)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Spot Pinned")
            .setContentText(text)
            .setOngoing(true)
            .setAutoCancel(false)
            .setDeleteIntent(repostPendingIntent)
            .addAction(android.R.drawable.ic_menu_delete, "Found Spot", clearPendingIntent)
            .addAction(android.R.drawable.ic_menu_directions, "Navigate to", mapPendingIntent)
            
        if (text == "Vault timer expired!" && mediaPlayer != null && mediaPlayer?.isPlaying == true) {
            val silenceIntent = Intent(this, TimerService::class.java).apply { action = "SILENCE_ALARM" }
            val silencePendingIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(this, 2, silenceIntent, PendingIntent.FLAG_IMMUTABLE)
            } else {
                PendingIntent.getService(this, 2, silenceIntent, PendingIntent.FLAG_IMMUTABLE)
            }
            builder.addAction(android.R.drawable.ic_media_pause, "Silence Alarm", silencePendingIntent)
        } else if (photoPath.isNotEmpty()) {
            builder.addAction(android.R.drawable.ic_menu_save, "Save", exportPendingIntent)
        }
        
        builder.setContentIntent(openPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (locationDetails.isNotEmpty()) {
            builder.setSubText(locationDetails)
        }
        
        val summaryText = buildString {
            if (locationDetails.isNotEmpty()) append("$locationDetails\n")
            append(text)
        }

        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
            builder.setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(bitmap)
                .bigLargeIcon(null as Bitmap?)
                .setSummaryText(summaryText))
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
        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
        return notification
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
