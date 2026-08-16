package com.spotvault.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState

class ToggleBluetoothActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        val enabled = !isAutoParkEnabled(prefs)
        prefs.edit().putBoolean(AUTO_PARK_ENABLED_PREF, enabled).commit()
        WidgetThemeHelper.bumpWidgetRevision(prefs)

        updateAppWidgetState(context, glanceId) { glancePrefs ->
            glancePrefs[PremiumGlanceWidget.KEY_AUTO_PARK_ENABLED] = enabled
            glancePrefs[PremiumGlanceWidget.KEY_REVISION] = System.currentTimeMillis()
        }
        PremiumGlanceWidget().update(context, glanceId)
        WidgetFeedback.tick(context)
        if (enabled) {
            WidgetFeedback.toast(context, "BT Auto on")
        }
        WidgetThemeHelper.refreshAllWidgets(context)
    }
}

class ToggleMotionActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        val wantsToEnable = !isMotionAutoParkEnabled(prefs)
        val enabling = wantsToEnable && hasActivityRecognitionPermission(context)
        prefs.edit().putBoolean(MOTION_AUTOPARK_ENABLED_PREF, enabling).commit()
        if (enabling) {
            val mac = loadAutoParkCarMac(prefs)
            if (mac != null) startMotionWatch(context, prefs, mac)
        } else {
            stopMotionWatch(context, prefs)
        }
        WidgetThemeHelper.bumpWidgetRevision(prefs)

        updateAppWidgetState(context, glanceId) { glancePrefs ->
            glancePrefs[PremiumGlanceWidget.KEY_MOTION_ENABLED] = enabling
            glancePrefs[PremiumGlanceWidget.KEY_REVISION] = System.currentTimeMillis()
        }
        PremiumGlanceWidget().update(context, glanceId)
        WidgetFeedback.tick(context)
        when {
            enabling -> WidgetFeedback.toast(context, "Motion on")
            wantsToEnable && !enabling -> WidgetFeedback.toast(context, "Motion needs permission")
        }
        WidgetThemeHelper.refreshAllWidgets(context)
    }
}

class FoundActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, QuickActionRelayActivity::class.java).apply {
            putExtra(QuickActionRelayActivity.EXTRA_ACTION, QuickActionRelayActivity.ACTION_FOUND)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

/** Light haptic + toast feedback for widget toggles — does not change update/state plumbing. */
object WidgetFeedback {
    fun toast(context: Context, message: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun tick(context: Context) {
        vibrate(context, longArrayOf(0, 28), intArrayOf(0, 40))
    }

    private fun vibrate(context: Context, timings: LongArray, amplitudes: IntArray) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        }
    }
}
