package com.spotvault.app

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * Full-screen content hosted in its own platform [androidx.compose.ui.window.Dialog] window
 * (e.g. the photo viewer, the Notepad editor, Settings) does not automatically inherit the
 * host Activity's edge-to-edge setup. Without this, WindowInsets.navigationBars/statusBars can
 * silently report 0 inside that window, so bottom-aligned buttons/content end up drawn
 * underneath the real system navigation bar on devices where it's visible on-screen — even
 * though everything looks correct in the main Activity content (home screen, Vault sheet).
 *
 * Call this once near the top of any full-screen Dialog's content to make sure its window is
 * genuinely edge-to-edge and that WindowInsets reported inside it are accurate, so that
 * `.navigationBarsPadding()` / `.statusBarsPadding()` modifiers applied further down actually
 * do something meaningful.
 */
@Composable
fun EnsureDialogEdgeToEdge() {
    val view = LocalView.current
    val context = LocalContext.current
    SideEffect {
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        if (dialogWindow != null) {
            // App Lock's screenshot/screen-recording block and blanked Recents thumbnail
            // (MainActivity.applySecureFlag) only ever touched MainActivity's own Window —
            // FLAG_SECURE is per-Window, not inherited by child windows, and every full-screen
            // Dialog that calls this function (photo viewer, Notepad, Settings, the Snap/Pin save
            // screen, and others) opens its own separate Window. Without this, a locked vault's
            // actual photo/note content was still screenshottable, recordable, and visible in the
            // Recents thumbnail the instant one of these Dialogs was on top — completely
            // defeating the one thing App Lock's own doc comment says it exists to prevent.
            val appLockEnabled = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
                .getBoolean(APP_LOCK_ENABLED_PREF, false)
            if (appLockEnabled) {
                dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            // Compose's Dialog can otherwise size its window to the "safe" area (excluding
            // system bars) even after decorFitsSystemWindows is disabled — leaving less real
            // height than navigationBarsPadding()/imePadding() further down assume is available,
            // which pushes bottom-anchored content (e.g. action button bars) off the bottom edge.
            dialogWindow.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
            dialogWindow.statusBarColor = android.graphics.Color.TRANSPARENT
            dialogWindow.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dialogWindow.attributes = dialogWindow.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }
    }
}
