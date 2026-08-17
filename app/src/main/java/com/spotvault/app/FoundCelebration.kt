package com.spotvault.app

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast

/**
 * Shared "Found it!" feedback for widget + in-app — haptic, sound, and toast.
 * Keep the clear/tracking logic separate; this is celebration only.
 */
object FoundCelebration {

    fun play(context: Context, withToast: Boolean = true) {
        val app = context.applicationContext
        celebrateHaptic(app)
        playSuccessTone(app)
        if (!withToast) return
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(app, "Found it!", Toast.LENGTH_LONG).show()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                Toast.makeText(app, "Tracking cleared — nice work", Toast.LENGTH_SHORT).show()
            }, 900)
        }
    }

    private fun celebrateHaptic(context: Context) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (!vibrator.hasVibrator()) return
            // Rising celebration: short tap → pause → stronger double hit.
            val timings = longArrayOf(0, 40, 70, 55, 45, 90)
            val amplitudes = intArrayOf(0, 70, 0, 140, 0, 255)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        }
    }

    private fun playSuccessTone(context: Context) {
        runCatching {
            // Same "alarm_sound_uri" pref used for the Timer Alert channel and the expired-timer
            // alarm — was hardcoded to the system's default notification sound here regardless of
            // what the user actually picked in Settings, so "Found" sounded different from every
            // other alert the app makes.
            val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
            val soundUriStr = prefs.getString("alarm_sound_uri", null)
            val uri = if (!soundUriStr.isNullOrEmpty()) {
                android.net.Uri.parse(soundUriStr)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } ?: return
            val ringtone = RingtoneManager.getRingtone(context, uri) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                ringtone.isLooping = false
            }
            ringtone.play()
        }
    }
}
