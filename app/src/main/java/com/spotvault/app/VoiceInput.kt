package com.spotvault.app

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Launches the device's built-in speech-recognition UI ([RecognizerIntent.ACTION_RECOGNIZE_SPEECH])
 * and reports the transcribed text back through [onResult]. Deliberately not using
 * `SpeechRecognizer` directly — that requires the RECORD_AUDIO permission and a persistent
 * listening session we'd own; handing off to the system recognizer activity means the mic
 * permission (and all its UI) belongs to that app, not us, and costs nothing to integrate.
 *
 * Silently no-ops with a toast rather than crashing on devices/emulators with no recognizer
 * installed (some tablets, AOSP-only builds) — `resolveActivity` is the standard way to check
 * before launching an implicit intent that might have nothing to handle it.
 */
@Composable
fun rememberVoiceInputLauncher(prompt: String = "Speak now…", onResult: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val gatePending = remember { AtomicBoolean(false) }
    DisposableEffect(Unit) {
        onDispose {
            // Host torn down before the recognizer result (nav/recompose) — without this,
            // AppLockGate.begin() would leave App Lock suppressed for the rest of the process.
            if (gatePending.getAndSet(false)) AppLockGate.end()
        }
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // This launcher is registered by whichever composable hosts the mic button — if App Lock
        // re-locked while the system recognizer had the foreground, AppLockScreen swapping in
        // would tear that composable down, unregistering the launcher before the recognizer's
        // result could be delivered, silently dropping the dictated text.
        if (gatePending.getAndSet(false)) AppLockGate.end()
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrEmpty()) onResult(spoken)
        }
    }
    return {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            AppLockGate.begin()
            gatePending.set(true)
            launcher.launch(intent)
        } else {
            Toast.makeText(context, "Voice input isn't available on this device", Toast.LENGTH_SHORT).show()
        }
    }
}

/** Mic [IconButton] for a text field's trailingIcon slot — tap to dictate, [onResult] receives
 * the transcribed text (caller decides replace vs. append). */
@Composable
fun VoiceMicButton(
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier.size(32.dp),
    prompt: String = "Speak now…",
    tint: Color = SpotVaultColors.Teal,
    contentDescription: String = "Voice input"
) {
    val launchVoiceInput = rememberVoiceInputLauncher(prompt, onResult)
    IconButton(onClick = launchVoiceInput, modifier = modifier) {
        Icon(Icons.Default.Mic, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
    }
}
