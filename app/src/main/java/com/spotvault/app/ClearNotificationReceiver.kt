package com.spotvault.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClearNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Prefs → await widget refresh while TimerService is still foreground → then stop
        // service. goAsync() keeps receiver priority for the full refresh window.
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        ActiveTrackingHelper.clearTrackingPrefs(appContext)
        val prefs = appContext.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        WidgetThemeHelper.bumpWidgetRevision(prefs)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                try {
                    WidgetThemeHelper.refreshAllWidgetsAwait(appContext)
                } catch (e: Exception) {
                    // This scope has no exception handler of its own, so an uncaught exception
                    // here would propagate past goAsync() and crash the whole process — a bad
                    // widget refresh shouldn't take the app down. Stop-service still runs below.
                    Log.e("ClearNotificationReceiver", "Widget refresh failed", e)
                }
            } finally {
                ActiveTrackingHelper.stopTrackingServiceAndNotifications(appContext)
                pendingResult.finish()
            }
        }
    }
}
