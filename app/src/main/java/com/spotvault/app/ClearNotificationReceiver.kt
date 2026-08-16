package com.spotvault.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                WidgetThemeHelper.refreshAllWidgetsAwait(appContext)
            } finally {
                ActiveTrackingHelper.stopTrackingServiceAndNotifications(appContext)
                pendingResult.finish()
            }
        }
    }
}
