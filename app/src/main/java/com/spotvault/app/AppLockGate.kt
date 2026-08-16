package com.spotvault.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Suppresses App Lock's "re-lock whenever the app loses the foreground" check for activity
 * transitions the app itself deliberately started — SAF file/folder pickers (backup export,
 * backup import, choosing an auto-backup folder, GPX import) and the Google Drive sign-in/
 * consent flow.
 *
 * Without this, [MainActivity.onStop] can't tell "the user backgrounded the app" apart from "a
 * system picker the app itself launched briefly took the foreground" — both look identical from
 * onStop's point of view. Re-locking in the second case used to swap the whole screen out for
 * [AppLockScreen] the moment the picker returned, which tore down (and re-created from scratch)
 * the very [androidx.activity.compose.ManagedActivityResultLauncher] that was about to receive
 * the picked file's Uri — silently dropping it. That's the "backup/import never completes after
 * App Lock makes me sign back in" bug this exists to fix.
 */
object AppLockGate {
    var suppressed by mutableStateOf(false)
        private set

    // A counter rather than a plain flag: two trusted launches can legitimately overlap in
    // flight (e.g. a slow picker still resolving while another one starts), and a plain boolean
    // would let the first one's cleanup turn suppression back off while the second is still
    // pending, re-exposing the exact race this is meant to close.
    private var depth = 0

    /** Call immediately before launching a trusted external activity (SAF picker, OAuth consent
     * intent, etc.) — must run while the app still has the foreground. */
    fun begin() {
        depth++
        suppressed = true
    }

    /** Call once the trusted activity's result has been delivered (success, failure, or
     * cancellation all count — this only needs to know the trusted transition is over). */
    fun end() {
        depth = (depth - 1).coerceAtLeast(0)
        if (depth == 0) suppressed = false
    }
}
