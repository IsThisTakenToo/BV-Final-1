package com.spotvault.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Single source of truth for the real system bar insets (status bar / navigation bar height
 * in pixels), measured once from the main Activity's own window — the one window we know for
 * certain receives correct WindowInsets, since the home screen, tracking screen, and Vault all
 * already render correctly against it.
 *
 * Full-screen content hosted in its own platform Dialog window (Settings, the photo viewer, the
 * Notepad editor, PremiumDialog-based save flows, etc.) can NOT reliably read
 * WindowInsets.navigationBars/statusBars from inside their own window — on some devices/OEM
 * skins that composition local reports 0 there even when edge-to-edge is set up correctly,
 * which is exactly why bottom-aligned buttons kept ending up hidden behind the real nav bar.
 * Reading from this shared object instead sidesteps that per-window inconsistency entirely.
 */
object SystemBarInsets {
    var statusBarPx by mutableIntStateOf(0)
        internal set
    var navigationBarPx by mutableIntStateOf(0)
        internal set

    // Horizontal insets — a display cutout (camera notch) that sits on the *top* edge in
    // portrait moves to a *side* edge once the device is rotated to landscape, and a
    // gesture-nav bar's swipe-exclusion zone likewise shifts from bottom to the sides in
    // landscape. Neither was ever captured before this — statusBarPx/navigationBarPx only ever
    // read .top/.bottom — so anything padding purely by those two in landscape left the left/right
    // edges completely unprotected.
    var leftPx by mutableIntStateOf(0)
        internal set
    var rightPx by mutableIntStateOf(0)
        internal set
}
