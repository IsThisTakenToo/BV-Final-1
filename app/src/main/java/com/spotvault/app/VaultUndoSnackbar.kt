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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * App-wide "action + Undo" snackbar. No composable in the tree has to thread an onArchive
 * callback down to trigger it — a spot card can call [VaultUndoSnackbar.show] directly, and
 * whichever screen has [VaultUndoSnackbarHost] mounted (once, near the app root) displays it.
 * There is no other Snackbar host anywhere in the app.
 */
object VaultUndoSnackbar {
    private var hostState: SnackbarHostState? = null
    private val scope = MainScope()

    internal fun attach(state: SnackbarHostState) {
        hostState = state
    }

    // onUndo is captured directly in this coroutine's closure rather than stashed in a shared
    // mutable field — archiving two spots in quick succession queues two snackbars (SnackbarHost
    // shows one at a time), and a shared "pending" var would have the second call's undo silently
    // overwrite the first's, so tapping Undo on the first snackbar would undo the wrong spot.
    fun show(message: String, onUndo: suspend () -> Unit) {
        hostState?.let { state ->
            scope.launch {
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
}

/** [bottomInset] should be the real space already reserved below the content by the app's own
 * bottom nav bar (plus the system nav bar it already includes) — without it, this renders flush
 * to the raw bottom of the screen and ends up hidden behind both bars on an edge-to-edge layout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultUndoSnackbarHost(bottomInset: Dp = 0.dp) {
    val state = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { VaultUndoSnackbar.attach(state) }
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
