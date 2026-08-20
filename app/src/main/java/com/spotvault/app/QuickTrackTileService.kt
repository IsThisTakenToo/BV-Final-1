package com.spotvault.app

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile — mirrors the same active-tracking on/off toggle the app already tracks in
 * SharedPreferences ("is_pinned", read the same way [TimerService] and the home-screen widgets
 * do), rather than a generic one-shot action. That's what actually earns the Active/Inactive
 * state distinction Quick Settings tiles are built around: tap to start tracking a spot, tap
 * again to mark it Found — a real toggle, not just a button.
 *
 * (Formerly a class named QuickPinTileService with this exact Track/Found behavior — this file
 * is the rename, not new logic. The naming mismatch was also why the Settings UI's
 * requestAddTileService() call could pass the label "Quick Pin" for what was actually the Track
 * tile: the class name and its real behavior had drifted apart. See [QuickPinTileService] for
 * the genuine one-shot Pin tile that now exists alongside this one.)
 *
 * Both taps route through [QuickActionRelayActivity], the same translucent, no-full-UI flow the
 * home-screen widget already uses for Pin/Track/Found — this reuses that logic instead of
 * duplicating the save/track pipeline a second time.
 */
class QuickTrackTileService : TileService() {

    // onTileAdded() fires exactly once, right when the user adds this tile — but on some OEM
    // panels (Samsung's included, confirmed on a real Note 20) onStartListening() doesn't reliably
    // follow immediately on that first add, and subtitle has no static XML default to fall back
    // on (it's Tile.subtitle, settable only at runtime), so the tile sat with no "Tap to start
    // tracking"/"Tap to mark Found" text underneath it until the panel was closed and reopened —
    // the next onStartListening() is what actually fixed it, not the tap itself. Calling the same
    // update here too means the first-ever display is already correct.
    override fun onTileAdded() {
        super.onTileAdded()
        updateTileState()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val prefs = getSharedPreferences("SpotVaultPrefs", MODE_PRIVATE)
        val isTracking = prefs.getBoolean("is_pinned", false)
        val action = if (isTracking) QuickActionRelayActivity.ACTION_FOUND else QuickActionRelayActivity.ACTION_TRACK

        val intent = QuickActionRelayActivity.intentForAction(this, action)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                QuickActionRelayActivity.pendingIntentRequestCode(action),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }

        // Optimistic flip so the tile doesn't sit stale until the relay activity finishes its
        // own work (GPS fix, DB write) — onStartListening()/requestTileUpdate() below correct it
        // if the action actually failed (e.g. no GPS fix).
        qsTile?.let { tile ->
            tile.state = if (isTracking) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            tile.updateTile()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val prefs = getSharedPreferences("SpotVaultPrefs", MODE_PRIVATE)
        val isTracking = prefs.getBoolean("is_pinned", false)
        tile.state = if (isTracking) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isTracking) "Spot Tracked" else getString(R.string.quick_track_tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isTracking) "Tap to mark Found" else "Tap to start tracking"
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_quick_track)
        tile.updateTile()
    }

    companion object {
        /** Call after anything changes "is_pinned" (Track/Found/Pin, auto-park, timer expiry,
         * etc.) so a pinned tile updates immediately instead of only on its next onStartListening
         * — the whole reason for declaring ACTIVE_TILE in the manifest. Safe to call even if the
         * tile was never added; TileService just no-ops. */
        fun requestTileUpdate(context: Context) {
            TileService.requestListeningState(
                context.applicationContext,
                ComponentName(context.applicationContext, QuickTrackTileService::class.java)
            )
        }
    }
}
