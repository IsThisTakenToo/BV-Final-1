package com.spotvault.app

import android.app.PendingIntent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile — a one-shot silent pin drop at the current location, the same ACTION_PIN
 * relay the home-screen widget's Quick Pin button already uses (see [performPin] in
 * [QuickActionRelayActivity]). Unlike [QuickTrackTileService], this isn't a toggle — there's no
 * on/off state to reflect, just an action to fire — so it stays STATE_INACTIVE always and isn't
 * declared ACTIVE_TILE in the manifest.
 */
class QuickPinTileService : TileService() {

    // onTileAdded() fires exactly once, right when the user adds this tile — but on some OEM
    // panels (Samsung's included, confirmed on a real Note 20) onStartListening() doesn't reliably
    // follow immediately on that first add, and subtitle has no static XML default to fall back
    // on (it's Tile.subtitle, settable only at runtime), so the tile sat with no "Tap to save a
    // pin" text underneath it until the panel was closed and reopened — same fix as
    // QuickTrackTileService's identical case.
    override fun onTileAdded() {
        super.onTileAdded()
        updateTileState()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = getString(R.string.quick_pin_tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = "Tap to save a pin"
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_quick_pin)
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = QuickActionRelayActivity.intentForAction(
            this,
            QuickActionRelayActivity.ACTION_PIN
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                QuickActionRelayActivity.pendingIntentRequestCode(QuickActionRelayActivity.ACTION_PIN),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
