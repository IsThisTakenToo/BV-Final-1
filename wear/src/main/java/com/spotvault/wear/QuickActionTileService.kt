package com.spotvault.wear

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.ColorFilter
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.Layout
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.Wearable
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/** The watch-side Tile: free for everyone (no premium gate). Shows Quick Pin / Quick Share / Quick
 * Track when no session is active, or Compass / Found / Navigate once [TrackingStateStore] says
 * one is — synced from the phone's own ActiveTrackingHelper.isActive() state (see TrackingWearSync
 * on the phone side). Found and the two Quick buttons use loadAction() to re-invoke onTileRequest()
 * in-process rather than launching anything, keeping this entirely free of Android's
 * background-activity-launch restrictions on the watch side too — the same reasoning that keeps
 * WearActionListenerService on the phone from ever calling startActivity() directly; Navigate and
 * Quick Share both need a real Activity on the *phone* (Maps, the share sheet), so those two post a
 * notification the user taps instead, exactly the same way the phone's own home-screen widget
 * already reaches QuickActionRelayActivity for the identical actions. Compass is different: it
 * launches CompassActivity right here on the watch, which a tile tap can do directly since the tap
 * itself is a foreground user gesture on this device — see CompassActivity's own doc.
 *
 * Colors and button shape come from [ThemeStateStore] (synced from the phone's own active color
 * theme / button style — see ThemeWearSync.kt on the phone side) rather than a fixed palette, so
 * this matches whatever the user has the app itself set to. */
class QuickActionTileService : TileService() {

    companion object {
        private const val CLICK_QUICK_PIN = "quick_pin"
        private const val CLICK_QUICK_TRACK = "quick_track"
        private const val CLICK_QUICK_SHARE = "quick_share"
        private const val CLICK_NAVIGATE = "navigate"
        private const val CLICK_FOUND = "found"
        private const val CLICK_COMPASS = "compass"
        private const val RESOURCES_VERSION = "4"

        // Guards against an accidental double-tap on a small watch touch target firing two
        // MessageClient sends — the phone treats each as an independent, unacknowledged action with
        // no dedup by default (Smart Deduplication is opt-in for pins), so an un-debounced double-tap
        // here would create two duplicate Vault entries with no indication anything doubled up. Keyed
        // per-path (not global) so a deliberate Quick Pin-then-Quick Track in quick succession still
        // goes through. Survives across TileService instance recreation within the same process —
        // which covers the realistic double-tap window — but resets on process death, which is fine
        // since that's well past any accidental-double-tap timescale anyway.
        private const val DEBOUNCE_MS = 1_500L
        private val lastSentAtMs = mutableMapOf<String, Long>()

        // Comfortably larger than half of any button's actual width/height at this tile's sizes —
        // ProtoLayout clamps a corner radius that exceeds half the shorter side, so this reliably
        // reads as a full pill/stadium shape rather than a fixed literal that would only happen to
        // work for one particular row's button dimensions.
        private const val CAPSULE_RADIUS_DP = 48f
        private const val DEFAULT_RADIUS_DP = 16f
    }

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        when (requestParams.currentState?.lastClickableId) {
            CLICK_QUICK_PIN -> sendQuickAction(QuickActionPaths.QUICK_PIN)
            CLICK_QUICK_TRACK -> {
                // Optimistic local flip, corrected by the real DataClient push moments later if the
                // save actually failed — mirrors how the phone's own QuickTrackTileService already
                // flips its Quick Settings Tile to Active the instant it's tapped, before hearing
                // back from anything, rather than leaving the tile showing stale Quick Pin/Track
                // buttons for the second or two the round trip to the phone takes.
                TrackingStateStore.setTracking(this, true)
                sendQuickAction(QuickActionPaths.QUICK_TRACK)
            }
            CLICK_QUICK_SHARE -> sendQuickAction(QuickActionPaths.QUICK_SHARE)
            CLICK_NAVIGATE -> sendQuickAction(QuickActionPaths.NAVIGATE)
            CLICK_FOUND -> {
                TrackingStateStore.setTracking(this, false)
                sendQuickAction(QuickActionPaths.FOUND)
            }
        }

        val theme = TileTheme(
            primary = ThemeStateStore.primary(this),
            accent = ThemeStateStore.accent(this),
            surface = ThemeStateStore.surface(this),
            onPrimary = ThemeStateStore.onPrimary(this),
            onAccent = ThemeStateStore.onAccent(this),
            onSurface = ThemeStateStore.onSurface(this),
            cornerRadiusDp = if (ThemeStateStore.buttonStyle(this) == "capsule") CAPSULE_RADIUS_DP else DEFAULT_RADIUS_DP
        )
        val tracking = TrackingStateStore.isTracking(this)
        val layout = Layout.Builder()
            .setRoot(if (tracking) trackingRoot(theme) else idleRoot(theme))
            .build()

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                Timeline.Builder()
                    .addTimelineEntry(TimelineEntry.Builder().setLayout(layout).build())
                    .build()
            )
            .build()
        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> {
        val resources = ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .addIdToImageMapping("ic_pin", drawableResource(R.drawable.ic_pin))
            .addIdToImageMapping("ic_track", drawableResource(R.drawable.ic_track))
            .addIdToImageMapping("ic_share", drawableResource(R.drawable.ic_share))
            .addIdToImageMapping("ic_navigate", drawableResource(R.drawable.ic_navigate))
            .addIdToImageMapping("ic_found", drawableResource(R.drawable.ic_found))
            .addIdToImageMapping("ic_compass", drawableResource(R.drawable.ic_compass))
            .build()
        return Futures.immediateFuture(resources)
    }

    private fun drawableResource(resId: Int) =
        ResourceBuilders.ImageResource.Builder()
            .setAndroidResourceByResId(
                ResourceBuilders.AndroidImageResourceByResId.Builder()
                    .setResourceId(resId)
                    .build()
            )
            .build()

    /** Fire-and-forget — the tile doesn't wait for delivery confirmation before redrawing. A
     * failed send (phone unreachable) is the correct, non-blocking outcome for a one-shot action;
     * queuing it for later delivery would risk it firing hours after the user actually tapped. */
    private fun sendQuickAction(path: String) {
        val now = System.currentTimeMillis()
        val lastSent = lastSentAtMs[path] ?: 0L
        if (now - lastSent < DEBOUNCE_MS) return
        // Set eagerly (not inside the success callback below) so a rapid double-tap is still
        // blocked during the async gap before connectedNodes resolves — but rolled back on a
        // genuine failure/no-connection below, so that doesn't also eat a legitimate retry for
        // the rest of the debounce window.
        lastSentAtMs[path] = now
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    lastSentAtMs.remove(path)
                    return@addOnSuccessListener
                }
                nodes.forEach { node ->
                    Wearable.getMessageClient(this).sendMessage(node.id, path, ByteArray(0))
                }
            }
            .addOnFailureListener {
                lastSentAtMs.remove(path)
            }
    }

    /** Snapshot of one onTileRequest()'s worth of synced colors/shape, so every actionTile() call
     * in that pass reads the same values without each one re-hitting SharedPreferences. */
    private data class TileTheme(
        val primary: Int,
        val accent: Int,
        val surface: Int,
        val onPrimary: Int,
        val onAccent: Int,
        val onSurface: Int,
        val cornerRadiusDp: Float
    )

    private fun idleRoot(theme: TileTheme): LayoutElementBuilders.LayoutElement =
        Row.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .addContent(
                actionTile(
                    label = "Pin",
                    iconId = "ic_pin",
                    background = theme.primary,
                    textColor = theme.onPrimary,
                    clickableId = CLICK_QUICK_PIN,
                    cornerRadiusDp = theme.cornerRadiusDp,
                    iconSizeDp = 22f,
                    paddingDp = 6f,
                    textSizeSp = 12f
                )
            )
            .addContent(Spacer.Builder().setWidth(dp(6f)).build())
            .addContent(
                actionTile(
                    label = "Share",
                    iconId = "ic_share",
                    background = theme.surface,
                    textColor = theme.onSurface,
                    clickableId = CLICK_QUICK_SHARE,
                    cornerRadiusDp = theme.cornerRadiusDp,
                    iconSizeDp = 22f,
                    paddingDp = 6f,
                    textSizeSp = 12f
                )
            )
            .addContent(Spacer.Builder().setWidth(dp(6f)).build())
            .addContent(
                actionTile(
                    label = "Track",
                    iconId = "ic_track",
                    background = theme.accent,
                    textColor = theme.onAccent,
                    clickableId = CLICK_QUICK_TRACK,
                    cornerRadiusDp = theme.cornerRadiusDp,
                    iconSizeDp = 22f,
                    paddingDp = 6f,
                    textSizeSp = 12f
                )
            )
            .build()

    private fun trackingRoot(theme: TileTheme): LayoutElementBuilders.LayoutElement =
        Row.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .addContent(
                actionTile(
                    label = "Compass",
                    iconId = "ic_compass",
                    background = theme.surface,
                    textColor = theme.onSurface,
                    clickableId = CLICK_COMPASS,
                    // Launches a real Activity instead of loadAction() — a Tile can't render a
                    // smoothly-rotating live arrow (no per-frame animation, no live sensor
                    // binding), so this needs CompassActivity's own Compose screen. Unlike
                    // WearActionListenerService on the phone, this tap IS itself a foreground user
                    // gesture on the watch, so there's no background-activity-launch restriction
                    // stopping a direct Activity launch here (see CompassActivity's own doc).
                    onClickAction = ActionBuilders.LaunchAction.Builder()
                        .setAndroidActivity(
                            ActionBuilders.AndroidActivity.Builder()
                                .setPackageName(packageName)
                                .setClassName(CompassActivity::class.java.name)
                                .build()
                        )
                        .build(),
                    cornerRadiusDp = theme.cornerRadiusDp,
                    iconSizeDp = 22f,
                    paddingDp = 6f,
                    textSizeSp = 12f
                )
            )
            .addContent(Spacer.Builder().setWidth(dp(6f)).build())
            .addContent(
                actionTile(
                    label = "Found!",
                    iconId = "ic_found",
                    background = theme.primary,
                    textColor = theme.onPrimary,
                    clickableId = CLICK_FOUND,
                    cornerRadiusDp = theme.cornerRadiusDp,
                    iconSizeDp = 22f,
                    paddingDp = 6f,
                    textSizeSp = 12f
                )
            )
            .addContent(Spacer.Builder().setWidth(dp(6f)).build())
            .addContent(
                actionTile(
                    label = "Navigate",
                    iconId = "ic_navigate",
                    background = theme.accent,
                    textColor = theme.onAccent,
                    clickableId = CLICK_NAVIGATE,
                    cornerRadiusDp = theme.cornerRadiusDp,
                    iconSizeDp = 22f,
                    paddingDp = 6f,
                    textSizeSp = 12f
                )
            )
            .build()

    private fun actionTile(
        label: String,
        iconId: String,
        background: Int,
        textColor: Int,
        clickableId: String,
        cornerRadiusDp: Float,
        onClickAction: ActionBuilders.Action = ActionBuilders.LoadAction.Builder().build(),
        iconSizeDp: Float = 28f,
        paddingDp: Float = 8f,
        textSizeSp: Float = 13f
    ): LayoutElementBuilders.LayoutElement =
        Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(
                Modifiers.Builder()
                    .setBackground(
                        Background.Builder()
                            .setColor(argb(background))
                            .setCorner(Corner.Builder().setRadius(dp(cornerRadiusDp)).build())
                            .build()
                    )
                    .setClickable(
                        Clickable.Builder()
                            .setId(clickableId)
                            .setOnClick(onClickAction)
                            .build()
                    )
                    .setPadding(Padding.Builder().setAll(dp(paddingDp)).build())
                    .build()
            )
            .addContent(
                Column.Builder()
                    .setWidth(wrap())
                    .setHeight(wrap())
                    .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                    .addContent(
                        Image.Builder()
                            .setResourceId(iconId)
                            .setWidth(dp(iconSizeDp))
                            .setHeight(dp(iconSizeDp))
                            // Icons are single-color white vector drawables — tinting them to match
                            // each button's own text color (rather than staying fixed white) keeps
                            // them legible against every theme, including ones with a very light
                            // accent (Yin Yang's near-white accent would otherwise wash out a plain
                            // white icon the same way it would plain white text).
                            .setColorFilter(
                                ColorFilter.Builder().setTint(argb(textColor)).build()
                            )
                            .build()
                    )
                    .addContent(Spacer.Builder().setHeight(dp(6f)).build())
                    .addContent(
                        Text.Builder()
                            .setText(label)
                            .setMaxLines(1)
                            .setFontStyle(
                                LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(sp(textSizeSp))
                                    .setWeight(
                                        LayoutElementBuilders.FontWeightProp.Builder()
                                            .setValue(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                            .build()
                                    )
                                    .setColor(argb(textColor))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
}
