package com.spotvault.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * App-wide "action + Undo" snackbar. No composable in the tree has to thread an onArchive
 * callback down to trigger it — a spot card can call [VaultUndoSnackbar.show] directly, and
 * whichever [VaultUndoSnackbarHost] is currently topmost displays it.
 */
object VaultUndoSnackbar {
    // A stack, not a single reference. Vault sub-dialogs (Favorites Hub, Location Browser,
    // Archived Spots, etc. — anything built on VaultOverlayDialog) each render in their own
    // raised Dialog window, which sits visually on top of the main Activity window the root
    // VaultUndoSnackbarHost lives in. An Undo action taken while one of those is open used to
    // show correctly but stay completely hidden behind the dialog's own opaque surface, since it
    // was rendering into a window one layer further back. VaultOverlayDialog now mounts its own
    // VaultUndoSnackbarHost, which pushes itself on top of this stack while composed and pops
    // back off on dispose — show() always targets whichever host is actually on top, i.e.
    // whichever window the user can currently see.
    private val hostStates = mutableListOf<SnackbarHostState>()
    private val scope = MainScope()
    private var activeJob: Job? = null

    internal fun push(state: SnackbarHostState) {
        hostStates.add(state)
    }

    internal fun pop(state: SnackbarHostState) {
        hostStates.remove(state)
    }

    // onUndo is captured directly in this coroutine's closure rather than stashed in a shared
    // mutable field — a shared "pending" var would let a second call's undo silently overwrite
    // the first's, so tapping Undo could end up undoing the wrong spot.
    fun show(message: String, onUndo: suspend () -> Unit) {
        val state = hostStates.lastOrNull() ?: return
        // Replaces whatever's currently showing/queued instead of letting it queue behind —
        // SnackbarHostState shows one at a time, each for its own full duration, so deleting
        // several spots in a row used to leave an Undo banner cycling through every one of them
        // for the better part of a minute, well after the user had moved on elsewhere in the
        // app. This does cost the ability to undo anything but the single most recent action —
        // the same tradeoff most apps with this exact pattern (Gmail's archive/undo, etc.) make.
        activeJob?.cancel()
        state.currentSnackbarData?.dismiss()
        activeJob = scope.launch {
            // Explicit duration matters here: showSnackbar()'s own default flips to
            // SnackbarDuration.Indefinite the moment actionLabel is non-null, which is why
            // this used to sit on screen forever instead of timing itself out.
            val result = state.showSnackbar(
                message = message,
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                onUndo()
            }
        }
    }
}

/** [bottomInset] should be the real space already reserved below the content by the app's own
 * bottom nav bar (plus the system nav bar it already includes) — without it, this renders flush
 * to the raw bottom of the screen and ends up hidden behind both bars on an edge-to-edge layout.
 * Mounting this more than once is expected and safe: each instance pushes itself onto
 * [VaultUndoSnackbar]'s host stack for as long as it stays composed (see the object's own doc
 * comment for why a raised Dialog needs its own instance) and pops back off on dispose. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultUndoSnackbarHost(bottomInset: Dp = 0.dp) {
    val state = remember { SnackbarHostState() }
    DisposableEffect(state) {
        VaultUndoSnackbar.push(state)
        onDispose { VaultUndoSnackbar.pop(state) }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = state,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomInset)
        ) { data ->
            Snackbar(
                containerColor = SpotVaultColors.Surface,
                contentColor = SpotVaultColors.OnSurface,
                action = {
                    data.visuals.actionLabel?.let { label ->
                        TextButton(onClick = { data.performAction() }) {
                            Text(label, color = SpotVaultColors.Teal)
                        }
                    }
                }
            ) {
                Text(data.visuals.message)
            }
        }
    }
}
