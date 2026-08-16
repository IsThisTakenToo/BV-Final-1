package com.spotvault.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Every selectable haptic feel — id is the persisted pref value. Each maps to a native
 * OEM-tuned effect (Android's [VibrationEffect.createPredefined], API 29+) rather than a raw
 * amplitude/duration guess, so the feel matches whatever the device's own haptic motor and
 * system haptic-strength setting are actually tuned for — the same effects system keyboards and
 * Material buttons use under the hood. Devices below API 29 fall back to a hand-tuned waveform
 * that approximates the same feel. */
enum class HapticPattern(val id: String, val label: String, val description: String) {
    LIGHT_TICK("light_tick", "Light Tick", "Faint, rapid click — like spinning a dial"),
    CRISP_CLICK("crisp_click", "Crisp Click", "Standard, satisfying tap"),
    HEAVY_SNAP("heavy_snap", "Heavy Snap", "Deep, authoritative thud for major actions"),
    DOUBLE_PULSE("double_pulse", "Double Pulse", "Quick double-tap for success and saves"),
    ERROR_BUZZ("error_buzz", "Error Buzz", "Harsh buzz for failures and deletes");

    companion object {
        /** sharp_tap/soft_tick/pulse_thud/double_click/rising_buzz/heavy_knock are this enum's
         * previous entries — mapped to their closest new equivalent so an existing user's pick
         * carries forward instead of silently reverting to the default. */
        fun fromId(id: String?): HapticPattern = when (id) {
            "sharp_tap" -> CRISP_CLICK
            "soft_tick" -> LIGHT_TICK
            "pulse_thud" -> HEAVY_SNAP
            "double_click" -> DOUBLE_PULSE
            "rising_buzz", "heavy_knock" -> HEAVY_SNAP
            else -> entries.firstOrNull { it.id == id } ?: CRISP_CLICK
        }
    }
}

private fun systemVibrator(context: Context): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}

private fun HapticPattern.legacyDurationMs(): Long = when (this) {
    HapticPattern.LIGHT_TICK -> 8L
    HapticPattern.CRISP_CLICK -> 14L
    HapticPattern.HEAVY_SNAP -> 30L
    HapticPattern.DOUBLE_PULSE -> 10L
    HapticPattern.ERROR_BUZZ -> 45L
}

// Deliberately NOT using VibrationEffect.createPredefined() here. The theory was that native
// EFFECT_CLICK/EFFECT_HEAVY_CLICK/EFFECT_DOUBLE_CLICK would give each pattern an OEM-tuned,
// "premium" feel — in practice, on real hardware, several of those predefined effects turned out
// to be hard to tell apart from each other, since plenty of vibrator motors don't actually
// differentiate them by much. Hand-tuned waveforms give full, consistent control over duration/
// amplitude/gap on every device instead of trusting each phone's own interpretation — the
// numbers below are chosen to be as far apart as possible on every axis at once (short+faint vs.
// short+firm vs. long+max vs. two firm hits with an unmistakable gap vs. three harsh pulses).
private fun HapticPattern.toVibrationEffect(): VibrationEffect = when (this) {
    // 6ms/40 (the original value here) turned out to be below the threshold plenty of vibrator
    // motors actually respond to — some ERM motors need a minimum drive duration/amplitude just
    // to start spinning up at all, so "make it as faint as possible" silently produced nothing
    // rather than a subtle tick. 10ms/90 is still clearly the lightest of the five, just no
    // longer under that floor.
    HapticPattern.LIGHT_TICK -> VibrationEffect.createOneShot(10, 90)
    HapticPattern.CRISP_CLICK -> VibrationEffect.createOneShot(15, 160)
    HapticPattern.HEAVY_SNAP -> VibrationEffect.createOneShot(45, 255)
    HapticPattern.DOUBLE_PULSE -> VibrationEffect.createWaveform(
        longArrayOf(0, 15, 100, 15), intArrayOf(0, 200, 0, 200), -1
    )
    HapticPattern.ERROR_BUZZ -> VibrationEffect.createWaveform(
        longArrayOf(0, 40, 40, 40, 40, 60), intArrayOf(0, 255, 0, 255, 0, 220), -1
    )
}

/** Fires the user's chosen haptic pattern for a UI interaction — respects the Haptic Feedback
 * on/off toggle (off by default), and degrades to a plain buzz on pre-Oreo devices where
 * amplitude/waveform control isn't available. Always safe to call: no-ops if haptics are off
 * or unsupported. */
fun performAppHaptic(context: Context, prefs: SharedPreferences) {
    if (!prefs.getBoolean("haptic_feedback", false)) return
    val vibrator = systemVibrator(context) ?: return
    if (!vibrator.hasVibrator()) return
    val patternId = premiumGatedId(prefs, prefs.getString("haptic_pattern", HapticPattern.CRISP_CLICK.id) ?: HapticPattern.CRISP_CLICK.id, PremiumFreeTier.freeHapticPatternId)
    val pattern = HapticPattern.fromId(patternId)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        runCatching { vibrator.vibrate(pattern.toVibrationEffect()) }
    } else {
        @Suppress("DEPRECATION")
        runCatching { vibrator.vibrate(pattern.legacyDurationMs()) }
    }
}

/** Bypasses the on/off toggle — used only by the Settings picker so a pattern can be felt while
 * deciding whether to turn haptics on at all, even before the toggle is enabled. */
private fun previewHapticPattern(context: Context, pattern: HapticPattern) {
    val vibrator = systemVibrator(context) ?: return
    if (!vibrator.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        runCatching { vibrator.vibrate(pattern.toVibrationEffect()) }
    } else {
        @Suppress("DEPRECATION")
        runCatching { vibrator.vibrate(pattern.legacyDurationMs()) }
    }
}

/** Settings picker: horizontally scrollable cards, one per pattern — tapping one both selects
 * it and demonstrates the feel (bypassing the on/off toggle, see [previewHapticPattern]). */
@Composable
fun HapticPatternPicker(
    selectedPatternId: String,
    onPatternSelected: (HapticPattern) -> Unit,
    modifier: Modifier = Modifier,
    isLocked: (String) -> Boolean = { false },
    onLockedClick: () -> Unit = {}
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HapticPattern.entries.forEach { pattern ->
            val selected = pattern.id == selectedPatternId
            val locked = isLocked(pattern.id)
            Box(modifier = Modifier.width(140.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (locked) 0.45f else 1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) SpotVaultColors.Teal.copy(alpha = 0.12f) else SpotVaultColors.Elevated.copy(alpha = 0.5f))
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) SpotVaultColors.Teal else SpotVaultColors.Outline.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            if (locked) {
                                onLockedClick()
                            } else {
                                onPatternSelected(pattern)
                                previewHapticPattern(context, pattern)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            pattern.label,
                            color = if (selected) SpotVaultColors.Teal else SpotVaultColors.OnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = SpotVaultColors.Teal,
                                modifier = Modifier.padding(start = 4.dp).size(14.dp)
                            )
                        }
                    }
                    Text(pattern.description, color = SpotVaultColors.Muted, fontSize = 12.sp, maxLines = 2)
                }
                if (locked) {
                    PremiumLockBadge(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
                }
            }
        }
    }
}
