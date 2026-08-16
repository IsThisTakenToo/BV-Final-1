package com.spotvault.app

import android.app.PendingIntent
import android.content.Intent
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

    override fun onStartListening() {
        super.onStartListening()
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
        val intent = Intent(this, QuickActionRelayActivity::class.java).apply {
            putExtra(QuickActionRelayActivity.EXTRA_ACTION, QuickActionRelayActivity.ACTION_PIN)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
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
