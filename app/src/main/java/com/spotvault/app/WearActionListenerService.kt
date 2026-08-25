package com.spotvault.app

import android.content.Context
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking

/** Receives Quick Pin / Quick Track / Quick Share / Navigate / Found taps from a paired watch (see
 * QuickActionTileService on the wear side) and performs them exactly the way the phone's own
 * in-app buttons and Quick Settings Tiles already do. Free feature, no premium gate.
 *
 * Quick Pin and Found are pure data operations (a DB insert, a prefs write, stopping an already-
 * running service) with no UI of their own and no foreground-service-start needs, so they're
 * called directly, the same way QuickActionRelayActivity.performPin() and
 * ActiveTrackingHelper.clearActiveTracking() already do. Quick Track is different: it needs to
 * *start* TimerService as a new foreground service, which this Service has no exemption to do (see
 * WearQuickTrackWorker's own doc) — it's handed off to an expedited WorkManager job instead. Quick
 * Share and Navigate both genuinely need a real Activity on the phone (the share sheet, Maps) —
 * those route through QuickActionRelayActivity.intentForAction(), the exact same factory the
 * phone's own home-screen widget already uses for these two actions, via a notification the user
 * taps. That's not just for consistency: a MessageEvent arriving from a watch has no foreground
 * user gesture on the phone side (this service can be woken from a fully-stopped process solely to
 * deliver it), so Android's background-activity-launch restrictions (this app targets SDK 36, so
 * they fully apply) would silently block starting that Activity directly from here — tapping the
 * notification is what supplies the user gesture instead.
 *
 * onMessageReceived() blocks on the parts it still does inline via runBlocking rather than firing a
 * coroutine and returning immediately — there's no goAsync()-equivalent for WearableListenerService,
 * and nothing else guarantees this process survives past the callback returning (the realistic
 * trigger is a fully-stopped process woken solely to deliver this message). Returning early risked
 * an action and its confirmation notification silently never happening if the system reclaimed the
 * process first. */
class WearActionListenerService : WearableListenerService() {

    companion object {
        // Must match QuickActionPaths.QUICK_PIN / QUICK_TRACK / QUICK_SHARE / NAVIGATE / FOUND in
        // wear/src/main/java/com/spotvault/wear/QuickActionPaths.kt exactly — duplicated rather
        // than shared because :app has no dependency on :wear. A mismatch fails silently (this
        // service's own manifest intent-filter path prefix just never matches), so keep both
        // sides in sync by hand.
        private const val PATH_QUICK_PIN = "/droppinvault/quick_pin"
        private const val PATH_QUICK_TRACK = "/droppinvault/quick_track"
        private const val PATH_QUICK_SHARE = "/droppinvault/quick_share"
        private const val PATH_NAVIGATE = "/droppinvault/navigate"
        private const val PATH_FOUND = "/droppinvault/found"
    }

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            PATH_QUICK_PIN -> handleQuickPin()
            PATH_QUICK_TRACK -> handleQuickTrack()
            PATH_QUICK_SHARE -> handleRelayAction(
                action = QuickActionRelayActivity.ACTION_SHARE,
                title = "Tap to share your location",
                body = "Pick where to send your spot."
            )
            PATH_NAVIGATE -> handleRelayAction(
                action = QuickActionRelayActivity.ACTION_NAVIGATE,
                title = "Tap to start navigation",
                body = "Opens turn-by-turn on your phone, which mirrors to your watch."
            )
            PATH_FOUND -> handleFound()
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun handleQuickPin() = runBlocking {
        val context = applicationContext
        // Same pre-check QuickActionRelayActivity.performPin() already does before calling this
        // same function — missing it here meant a save with no location permission ever granted
        // fell straight through to quietSaveTacticalPin's own documented fallback (save anyway at
        // a 0,0 placeholder rather than losing the pin), silently filling the Vault with
        // "Unresolved Location" entries on every watch tap instead of ever telling the user why. A
        // background Service can't itself show the permission dialog — only a foreground Activity
        // can — so this can only ask the user to go grant it, not grant it here.
        if (!hasLocationPermission()) {
            WearActionNotifications.post(
                context,
                notificationId = WearActionNotifications.ID_LOCATION_PERMISSION,
                title = "Location access needed",
                body = "Open DropPin Vault and tap any Quick action once to grant location access, then try again from your watch.",
                actionNeeded = true
            )
            return@runBlocking
        }
        val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        val dao = AppDatabase.getDatabase(context).locationDao()
        when (quietSaveTacticalPin(context, dao, prefs, GENERIC_QUICK_PIN)) {
            is QuietSaveResult.Saved -> WearActionNotifications.post(
                context,
                notificationId = WearActionNotifications.ID_QUICK_PIN,
                title = "Pin saved from your watch",
                body = "Your spot is in the Vault."
            )
            QuietSaveResult.Failed -> WearActionNotifications.post(
                context,
                notificationId = WearActionNotifications.ID_QUICK_PIN,
                title = "Save failed",
                body = "Couldn't save the spot your watch sent. Please try again."
            )
            QuietSaveResult.NeedsNotificationPermission -> Unit
        }
    }

    private fun handleQuickTrack() {
        val context = applicationContext
        if (!hasLocationPermission()) {
            WearActionNotifications.post(
                context,
                notificationId = WearActionNotifications.ID_LOCATION_PERMISSION,
                title = "Location access needed",
                body = "Open DropPin Vault and tap any Quick action once to grant location access, then try again from your watch.",
                actionNeeded = true
            )
            return
        }
        // See WearQuickTrackWorker's own doc — quickActiveTrackPin() needs to start TimerService
        // as a new foreground service, which this Service has no exemption to do from a
        // fully-stopped-process wakeup. Handed off to an expedited WorkManager job instead.
        enqueueWearQuickTrack(context)
    }

    private fun handleFound() {
        val context = applicationContext
        // One line — ActiveTrackingHelper.clearActiveTracking() is already the same quiet, no-UI
        // "stop tracking" path AutoParkWorker and the in-app Cancel Tracking button use. Skips
        // QuickActionRelayActivity's FoundCelebration.play() deliberately: that's a full-screen
        // animation meant for when the user is actually looking at the phone, which isn't the
        // point of tapping Found from the watch instead.
        ActiveTrackingHelper.clearActiveTracking(context)
        WearActionNotifications.post(
            context,
            notificationId = WearActionNotifications.ID_FOUND,
            title = "Marked as found",
            body = "Tracking stopped and your spot is saved."
        )
    }

    /** Navigate and Quick Share both need a real Activity on the phone, which this service can't
     * launch directly (see the class doc) — so this posts a notification whose tap launches
     * QuickActionRelayActivity with the given action, the identical entry point
     * EverydayQuickActionsWidget/PremiumGlanceWidget already use for their own Navigate/Quick
     * Share buttons. Reuses that Activity's own existing performNavigate()/performShare() logic
     * completely unchanged — including their own coordinate/App-Lock handling — rather than
     * duplicating any of it here. */
    private fun handleRelayAction(action: String, title: String, body: String) {
        val context = applicationContext
        val notificationId = if (action == QuickActionRelayActivity.ACTION_SHARE) {
            WearActionNotifications.ID_QUICK_SHARE
        } else {
            WearActionNotifications.ID_NAVIGATE
        }
        WearActionNotifications.post(
            context,
            notificationId = notificationId,
            title = title,
            body = body,
            contentIntent = QuickActionRelayActivity.intentForAction(context, action),
            requestCode = QuickActionRelayActivity.pendingIntentRequestCode(action),
            actionNeeded = true
        )
    }
}
