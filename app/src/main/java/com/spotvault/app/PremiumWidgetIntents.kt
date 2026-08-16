package com.spotvault.app

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Deep-link and relay intents used by the premium home-screen widget. */
object PremiumWidgetIntents {

    const val EXTRA_SPOT_ID = "spot_id"
    const val EXTRA_WIDGET_ACTION = "widget_action"
    const val EXTRA_COMPASS_LAT = "compass_lat"
    const val EXTRA_COMPASS_LNG = "compass_lng"

    const val ACTION_SNAP = "snap"
    const val ACTION_PIN_ONLY = "pin_only"
    const val ACTION_SHARE_PHOTO = "share_photo"
    const val ACTION_PREMIUM = "premium"
    const val ACTION_COMPASS = "compass"
    const val ACTION_VAULT = "vault"
    const val ACTION_NAVIGATE_MAPS = "navigate_maps"

    fun openSpot(context: Context, spotId: Int): Intent =
        Intent(context, MainActivity::class.java).apply {
            // Unique action + data so each history row gets its own PendingIntent
            // (extras alone are ignored by PendingIntent.filterEquals).
            this.action = "com.spotvault.app.widget.OPEN_SPOT_$spotId"
            data = Uri.parse("spotvault://widget/spot/$spotId")
            putExtra(EXTRA_SPOT_ID, spotId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

    fun widgetAction(context: Context, action: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            this.action = "com.spotvault.app.widget.ACTION_$action"
            data = Uri.parse("spotvault://widget/action/$action")
            putExtra(EXTRA_WIDGET_ACTION, action)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

    fun openCompass(context: Context, lat: Double, lng: Double): Intent =
        Intent(context, MainActivity::class.java).apply {
            this.action = "com.spotvault.app.widget.ACTION_$ACTION_COMPASS"
            data = Uri.parse("spotvault://widget/action/$ACTION_COMPASS")
            putExtra(EXTRA_WIDGET_ACTION, ACTION_COMPASS)
            putExtra(EXTRA_COMPASS_LAT, lat)
            putExtra(EXTRA_COMPASS_LNG, lng)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

    /** Used instead of a direct-to-Maps PendingIntent when App Lock is on — see TimerService's
     * "Navigate to" notification action. Routes through MainActivity's own App Lock gate first
     * (the same gate every other entry point already goes through) rather than opening Maps with
     * the tracked location straight from a locked device. */
    fun openMapsNavigation(context: Context, lat: Double, lng: Double): Intent =
        Intent(context, MainActivity::class.java).apply {
            this.action = "com.spotvault.app.widget.ACTION_$ACTION_NAVIGATE_MAPS"
            data = Uri.parse("spotvault://widget/action/$ACTION_NAVIGATE_MAPS")
            putExtra(EXTRA_WIDGET_ACTION, ACTION_NAVIGATE_MAPS)
            putExtra(EXTRA_COMPASS_LAT, lat)
            putExtra(EXTRA_COMPASS_LNG, lng)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

    fun openPremiumSettings(context: Context): Intent =
        widgetAction(context, ACTION_PREMIUM)

    fun openVault(context: Context): Intent =
        widgetAction(context, ACTION_VAULT)
}
