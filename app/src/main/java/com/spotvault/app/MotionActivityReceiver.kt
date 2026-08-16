package com.spotvault.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

/**
 * Fires when Play Services detects a watched "left the vehicle" transition after
 * [startMotionWatch] armed it (only ever armed while parked next to a known car, see
 * [MotionWatchStartWorker]). This never saves a spot directly — it only queues
 * [MotionBookmarkWorker] to cache a fresh location. The actual save is decided later, when the
 * car's Bluetooth actually disconnects (see [CarBluetoothReceiver] and
 * [consumeValidMotionBookmark]), so a stray or premature transition can never create a wrong pin.
 */
class MotionActivityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || !ActivityTransitionResult.hasResult(intent)) return
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        if (!isAutoParkEnabled(prefs) || !isMotionAutoParkEnabled(prefs)) return

        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val leftVehicle = result.transitionEvents.any { event ->
            (event.activityType == DetectedActivity.IN_VEHICLE && event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) ||
                ((event.activityType == DetectedActivity.WALKING || event.activityType == DetectedActivity.ON_FOOT) &&
                    event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER)
        }
        if (!leftVehicle) return
        if (currentMotionWatchMac(prefs) == null) return

        enqueueMotionBookmarkFetch(appContext)
    }
}
