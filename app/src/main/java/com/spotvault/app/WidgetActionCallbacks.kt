package com.spotvault.app

import android.content.Context
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
        val wantsToEnable = !isAutoParkEnabled(prefs)
        // Turning off never needs a permission check — only turning on does. An ActionCallback
        // isn't an Activity, so it has no way to show the actual system permission dialog itself;
        // this used to just flip the pref regardless, which showed the widget as "on" even on a
        // device that had never granted location (or Bluetooth, on API 31+) — a toggle that lied
        // about actually working. Automatic Parking settings already has the real permission-
        // request flow; hand off to it instead of claiming success here.
        if (wantsToEnable && !hasAutoParkPermissions(context)) {
            WidgetFeedback.toast(context, "Bluetooth Auto needs permission — opening Automatic Parking…")
            context.startActivity(PremiumWidgetIntents.openAutoParkSettings(context))
            return
        }
        val enabled = wantsToEnable
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
        // Same reasoning as ToggleBluetoothActionCallback above — used to silently no-op (toggle
        // stayed off, only a toast said why) when Activity Recognition wasn't granted; that toast
        // is easy to miss entirely from a home-screen tap. Opening Automatic Parking settings
        // actually gets the user to a real permission prompt instead of leaving them to go dig up
        // Settings themselves.
        if (wantsToEnable && !hasActivityRecognitionPermission(context)) {
            WidgetFeedback.toast(context, "Motion needs permission — opening Automatic Parking…")
            context.startActivity(PremiumWidgetIntents.openAutoParkSettings(context))
            return
        }
        val enabling = wantsToEnable
        prefs.edit().putBoolean(MOTION_AUTOPARK_ENABLED_PREF, enabling).commit()
        if (enabling) {
            // Match home / Automatic Parking settings: arm every linked vehicle MAC (plus legacy
            // single-MAC prefs), not only loadAutoParkCarMac — multi-vehicle installs otherwise
            // stayed unarmed until the next ACL connect.
            val db = AppDatabase.getDatabase(context)
            val linkedMacs = db.vehicleDao().getAllList()
                .asSequence()
                .filter { !it.isArchived && !it.bluetoothMac.isNullOrBlank() }
                .mapNotNull { it.bluetoothMac }
                .toList()
            linkedMacs.forEach { armMotionWatchIfAlreadyConnected(context, it) }
            loadAutoParkCarMac(prefs)?.let { armMotionWatchIfAlreadyConnected(context, it) }
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
        if (enabling) {
            WidgetFeedback.toast(context, "Motion on")
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
        val intent = QuickActionRelayActivity.intentForAction(
            context,
            QuickActionRelayActivity.ACTION_FOUND
        )
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
