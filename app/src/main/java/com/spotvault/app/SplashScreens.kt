package com.spotvault.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Launch intro styles — NONE is first so it renders leftmost in [SplashStylePicker].
 * NONE and DEFAULT are free (see [PremiumFreeTier.freeSplashStyleIds]). */
enum class SplashStyle(val id: String, val label: String, val description: String) {
    NONE("none", "None", "Skip the launch intro — straight into the app"),
    DEFAULT("default", "Default", "Clean mark fade-in — the classic look"),
    SIGNAL_FORGE("signal_forge", "Signal Forge", "Embers ignite, plasma helixes spiral inward, a vault lock ring forges, and your mark is hammered into being"),
    VAULT_DOOR("vault_door", "Vault Door", "Security bolts retract, the dial completes its sequence, steel doors swing wide, and your mark emerges from the treasury glow"),
    RADAR("radar", "Radar Scan", "Tactical HUD boots, range rings pulse outward, the sweep acquires contacts, and your mark locks at center"),
    TRIANGULATE("triangulate", "Triangulate", "Orbital towers ignite, triangulation beams converge, the fix triangle locks, and your mark resolves at center"),
    SIGNAL_STORM("signal_storm", "Signal Storm", "The ion field charges, lightning forks inward, the storm eye collapses, and your mark erupts from the core"),
    VAULT_BLOOM("vault_bloom", "Vault Bloom", "Morning mist clears, a vault flower blooms in layers, pollen erupts, and your mark buzzes forward on golden wings"),
    HALLOWEEN("halloween", "All Hallows' Gate", "The harvest moon rises, séance rings ignite, a witch-hat jack-o'-lantern portal opens, and your mark erupts from the inferno"),
    CHRISTMAS("christmas", "Christmas Eve Gate", "The North Star ignites, a giant ornament portal opens, and your mark arrives in a blizzard of light");

    companion object {
        fun fromId(id: String?): SplashStyle {
            val migrated = when (id) {
                "grid", "pin_drop" -> TRIANGULATE.id
                "icon_surge" -> VAULT_BLOOM.id
                "screen_crack" -> SIGNAL_FORGE.id
                "beacon_wake" -> SIGNAL_STORM.id
                else -> id
            }
            return entries.firstOrNull { it.id == migrated } ?: DEFAULT
        }
    }
}

fun SplashStyle.effectDurationMillis(): Long {
    // Reduce Animations previously had zero effect here — every other animated system in the
    // app (buttons, background patterns, the compass dial, and critically the Found celebration
    // screen right next to this setting in Settings) shortens or freezes under this flag, but
    // the launch splash's full 1.4-2.4s particle intro played in full on literally every cold
    // launch regardless. Same flat, short cap FoundSplashScreens' celebration already uses under
    // this setting — still visible, not the full effect. totalDisplayMillis() below calls this
    // function too, so the outer "how long the splash stays up" delay shrinks along with it from
    // this one place, no separate check needed there. Low-RAM devices get the same short window
    // so Canvas-heavy styles don't stay on screen thrashing under memory pressure.
    if ((ThemeState.reduceAnimations || ThemeState.lowRamDevice) &&
        this != SplashStyle.NONE && this != SplashStyle.DEFAULT
    ) {
        return 900L
    }
    return when (this) {
        SplashStyle.NONE -> 0L
        SplashStyle.DEFAULT -> 0L
        SplashStyle.SIGNAL_FORGE -> 1500L
        SplashStyle.VAULT_DOOR -> 1520L
        SplashStyle.RADAR -> 1480L
        SplashStyle.TRIANGULATE -> 1500L
        SplashStyle.SIGNAL_STORM -> 1520L
        SplashStyle.VAULT_BLOOM -> 1550L
        SplashStyle.HALLOWEEN -> 1380L
        SplashStyle.CHRISTMAS -> 1380L
    }
}

/** Scales dense Canvas loop counts under Reduce Animations / low-RAM — same style, fewer draws. */
fun splashLoopCount(full: Int): Int {
    val scale = when {
        ThemeState.reduceAnimations -> 0.35f
        ThemeState.lowRamDevice -> 0.5f
        else -> 1f
    }
    if (scale >= 0.99f) return full
    return (full * scale).toInt().coerceAtLeast(if (full <= 3) full else 3)
}

fun SplashStyle.totalDisplayMillis(): Long = when (this) {
    SplashStyle.NONE -> 0L
    else -> 800L + effectDurationMillis()
}

fun splashSignalForgeIconScale(progress: Float): Float {
    val forge = ((progress - 0.58f) / 0.38f).coerceIn(0f, 1f)
    val eased = forge * forge * (3f - 2f * forge)
    val hammerPop = sin(forge * PI.toFloat()) * 0.12f
    return 0.08f + eased * 0.92f + hammerPop
}

fun splashSignalForgeIconAlpha(progress: Float): Float =
    ((progress - 0.54f) / 0.30f).coerceIn(0f, 1f)

fun splashSignalForgeIconRotation(progress: Float): Float {
    val forge = ((progress - 0.58f) / 0.38f).coerceIn(0f, 1f)
    return sin(forge * PI.toFloat() * 3.5f) * 9f * (1f - forge * 0.45f)
}

fun splashRadarIconScale(progress: Float): Float {
    val lock = ((progress - 0.60f) / 0.36f).coerceIn(0f, 1f)
    val eased = lock * lock * (3f - 2f * lock)
    val ping = sin(lock * PI.toFloat()) * 0.10f
    return 0.10f + eased * 0.90f + ping
}

fun splashRadarIconAlpha(progress: Float): Float =
    ((progress - 0.56f) / 0.30f).coerceIn(0f, 1f)

fun splashRadarIconRotation(progress: Float): Float {
    val lock = ((progress - 0.60f) / 0.36f).coerceIn(0f, 1f)
    return sin(lock * PI.toFloat() * 4f) * 6f * (1f - lock * 0.5f)
}

fun splashTriangulateIconScale(progress: Float): Float {
    val lock = ((progress - 0.60f) / 0.36f).coerceIn(0f, 1f)
    val eased = lock * lock * (3f - 2f * lock)
    val resolve = sin(lock * PI.toFloat()) * 0.11f
    return 0.10f + eased * 0.90f + resolve
}

fun splashTriangulateIconAlpha(progress: Float): Float =
    ((progress - 0.56f) / 0.30f).coerceIn(0f, 1f)

fun splashTriangulateIconRotation(progress: Float): Float {
    val lock = ((progress - 0.60f) / 0.36f).coerceIn(0f, 1f)
    return sin(lock * PI.toFloat() * 3.8f) * 7f * (1f - lock * 0.45f)
}

fun splashVaultDoorIconScale(progress: Float): Float {
    val reveal = ((progress - 0.58f) / 0.36f).coerceIn(0f, 1f)
    val eased = reveal * reveal * (3f - 2f * reveal)
    val gleam = sin(reveal * PI.toFloat()) * 0.10f
    return 0.08f + eased * 0.92f + gleam
}

fun splashVaultDoorIconAlpha(progress: Float): Float =
    ((progress - 0.54f) / 0.30f).coerceIn(0f, 1f)

fun splashVaultDoorIconRotation(progress: Float): Float {
    val reveal = ((progress - 0.58f) / 0.36f).coerceIn(0f, 1f)
    return sin(reveal * PI.toFloat() * 3.2f) * 7f * (1f - reveal * 0.5f)
}

fun splashVaultBloomIconScale(progress: Float): Float {
    val buzz = ((progress - 0.56f) / 0.38f).coerceIn(0f, 1f)
    val eased = buzz * buzz * (3f - 2f * buzz)
    val zip = sin(buzz * PI.toFloat()) * 0.16f
    return 0.06f + eased * 0.94f + zip
}

fun splashVaultBloomIconAlpha(progress: Float): Float =
    ((progress - 0.52f) / 0.30f).coerceIn(0f, 1f)

fun splashVaultBloomIconRotation(progress: Float): Float {
    val buzz = ((progress - 0.56f) / 0.38f).coerceIn(0f, 1f)
    return sin(buzz * PI.toFloat() * 6f) * 14f * (1f - buzz * 0.35f)
}

fun splashVaultBloomWingFlap(progress: Float): Float =
    sin(progress * PI.toFloat() * 13f) * 28f

fun splashSignalStormIconScale(progress: Float): Float {
    val erupt = ((progress - 0.58f) / 0.38f).coerceIn(0f, 1f)
    val eased = erupt * erupt * (3f - 2f * erupt)
    val surge = sin(erupt * PI.toFloat()) * 0.13f
    return 0.06f + eased * 0.94f + surge
}

fun splashSignalStormIconAlpha(progress: Float): Float =
    ((progress - 0.54f) / 0.30f).coerceIn(0f, 1f)

fun splashSignalStormIconRotation(progress: Float): Float {
    val erupt = ((progress - 0.58f) / 0.38f).coerceIn(0f, 1f)
    return sin(erupt * PI.toFloat() * 4.2f) * 10f * (1f - erupt * 0.45f)
}

fun splashHalloweenIconScale(progress: Float): Float {
    val emerge = ((progress - 0.44f) / 0.48f).coerceIn(0f, 1f)
    val eased = emerge * emerge * (3f - 2f * emerge)
    val pop = sin(emerge * PI.toFloat()) * 0.1f
    return 0.04f + eased * 0.96f + pop
}

fun splashHalloweenIconAlpha(progress: Float): Float =
    ((progress - 0.40f) / 0.28f).coerceIn(0f, 1f)

fun splashHalloweenIconRotation(progress: Float): Float {
    val emerge = ((progress - 0.44f) / 0.48f).coerceIn(0f, 1f)
    return sin(emerge * PI.toFloat() * 3.2f) * 11f * (1f - emerge * 0.5f)
}

fun splashChristmasIconScale(progress: Float): Float {
    val emerge = ((progress - 0.44f) / 0.48f).coerceIn(0f, 1f)
    val eased = emerge * emerge * (3f - 2f * emerge)
    val pop = sin(emerge * PI.toFloat()) * 0.1f
    return 0.04f + eased * 0.96f + pop
}

fun splashChristmasIconAlpha(progress: Float): Float =
    ((progress - 0.40f) / 0.28f).coerceIn(0f, 1f)

fun splashChristmasIconRotation(progress: Float): Float {
    val emerge = ((progress - 0.44f) / 0.48f).coerceIn(0f, 1f)
    return sin(emerge * PI.toFloat() * 2.8f) * 8f * (1f - emerge * 0.5f)
}

private fun splashSmoothStep(t: Float): Float {
    val c = t.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

@Composable
fun SplashStyleEffect(style: SplashStyle, progress: Float, modifier: Modifier = Modifier) {
    when (style) {
        SplashStyle.NONE, SplashStyle.DEFAULT -> Unit
        SplashStyle.SIGNAL_FORGE -> SignalForgeSplashEffect(progress, modifier)
        SplashStyle.VAULT_DOOR -> VaultDoorSplashEffect(progress, modifier)
        SplashStyle.RADAR -> RadarSplashEffect(progress, modifier)
        SplashStyle.TRIANGULATE -> TriangulateSplashEffect(progress, modifier)
        SplashStyle.SIGNAL_STORM -> SignalStormSplashEffect(progress, modifier)
        SplashStyle.VAULT_BLOOM -> VaultBloomSplashEffect(progress, modifier)
        SplashStyle.HALLOWEEN -> HalloweenSplashEffect(progress, modifier)
        SplashStyle.CHRISTMAS -> ChristmasSplashEffect(progress, modifier)
    }
}

@Composable
fun VaultBloomWings(progress: Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    val launch = ((progress - 0.50f) / 0.46f).coerceIn(0f, 1f)
    val wingAlpha = (launch * 1.35f).coerceIn(0f, 1f)
    if (wingAlpha <= 0.02f) return
    val flap = splashVaultBloomWingFlap(progress)
    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val wingW = size.width * 0.40f
        val wingH = size.height * 0.24f
        listOf(-1f, 1f).forEach { side ->
            rotate(flap * side * 0.85f, pivot = Offset(cx + side * wingW * 0.12f, cy)) {
                listOf(0.0f, 0.18f).forEach { layer ->
                    val layerAlpha = wingAlpha * (1f - layer * 0.35f)
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFF8E1).copy(alpha = 0.82f * layerAlpha),
                                Color(0xFFFFD54F).copy(alpha = 0.55f * layerAlpha),
                                colors.TealSoft.copy(alpha = 0.28f * layerAlpha),
                                Color.Transparent
                            ),
                            center = Offset(cx + side * wingW * (0.52f + layer), cy - wingH * 0.08f),
                            radius = wingW * (0.50f - layer * 0.08f)
                        ),
                        topLeft = Offset(cx + side * wingW * (0.06f + layer * 0.04f), cy - wingH * 0.58f),
                        size = Size(wingW * (0.68f - layer * 0.08f), wingH * (0.92f - layer * 0.12f))
                    )
                    drawOval(
                        color = Color(0xFF5D4037).copy(alpha = 0.22f * layerAlpha),
                        topLeft = Offset(cx + side * wingW * (0.10f + layer * 0.04f), cy - wingH * 0.52f),
                        size = Size(wingW * (0.62f - layer * 0.08f), wingH * (0.82f - layer * 0.12f)),
                        style = Stroke(width = 1.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SignalForgeSplashEffect(progress: Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.46f
            val w = size.width
            val h = size.height
            val floorPhase = splashSmoothStep((progress / 0.22f).coerceIn(0f, 1f))
            val emberPhase = splashSmoothStep(((progress - 0.04f) / 0.28f).coerceIn(0f, 1f))
            val helixPhase = splashSmoothStep(((progress - 0.10f) / 0.46f).coerceIn(0f, 1f))
            val cogPhase = splashSmoothStep(((progress - 0.18f) / 0.38f).coerceIn(0f, 1f))
            val lockPhase = splashSmoothStep(((progress - 0.32f) / 0.34f).coerceIn(0f, 1f))
            val cruciblePhase = splashSmoothStep(((progress - 0.46f) / 0.30f).coerceIn(0f, 1f))
            val strikePhase = splashSmoothStep(((progress - 0.62f) / 0.38f).coerceIn(0f, 1f))
            val heatFlicker = 0.78f + 0.22f * sin(progress * PI.toFloat() * 7.5f)

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A080C).copy(alpha = 0.72f * floorPhase),
                        Color(0xFF1A0E08).copy(alpha = 0.55f * floorPhase),
                        Color.Transparent
                    ),
                    startY = h * 0.55f,
                    endY = h
                )
            )

            if (emberPhase > 0f) {
                drawSplashForgeEmberField(
                    center = center + Offset(0f, maxR * 0.62f),
                    spread = maxR * 1.15f,
                    phase = emberPhase,
                    flicker = heatFlicker,
                    primary = colors.PrimaryBright,
                    accent = colors.Teal
                )
            }

            if (floorPhase > 0.15f) {
                val shimmer = ((floorPhase - 0.15f) / 0.85f).coerceIn(0f, 1f)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            colors.PrimaryBright.copy(alpha = 0.06f * shimmer * heatFlicker),
                            colors.Teal.copy(alpha = 0.10f * shimmer),
                            Color.Transparent
                        ),
                        startY = center.y + maxR * 0.15f,
                        endY = center.y + maxR * 1.05f
                    )
                )
            }

            if (cogPhase > 0f) {
                drawSplashForgeCogRing(
                    center = center,
                    radius = maxR * (0.78f + cogPhase * 0.08f),
                    phase = cogPhase,
                    spin = progress * 95f,
                    primary = colors.PrimaryBright,
                    accent = colors.Teal
                )
            }

            if (helixPhase > 0f) {
                val helixFade = (1f - strikePhase * 0.55f).coerceIn(0.2f, 1f)
                listOf(colors.PrimaryBright to 1f, colors.Teal to -1f).forEach { (tint, direction) ->
                    drawSplashForgeHelix(
                        center = center,
                        maxRadius = maxR,
                        phase = helixPhase,
                        progress = progress,
                        direction = direction,
                        tint = tint,
                        alpha = helixFade,
                        heatFlicker = heatFlicker
                    )
                }
            }

            if (lockPhase > 0f) {
                drawSplashForgeLockRing(
                    center = center,
                    radius = maxR * 0.42f,
                    phase = lockPhase,
                    progress = progress,
                    primary = colors.PrimaryBright,
                    accent = colors.Teal
                )
            }

            if (cruciblePhase > 0f) {
                drawSplashForgeCrucible(
                    center = center,
                    radius = maxR * (0.34f + cruciblePhase * 0.06f),
                    phase = cruciblePhase,
                    strikePhase = strikePhase,
                    primary = colors.PrimaryBright,
                    accent = colors.Teal,
                    flicker = heatFlicker
                )
            }

            if (strikePhase > 0f) {
                for (wave in 0 until 5) {
                    val delay = wave * 0.11f
                    val local = ((strikePhase - delay) / 0.72f).coerceIn(0f, 1f)
                    if (local <= 0f) continue
                    drawSplashForgeShockwave(
                        center = center,
                        maxRadius = maxR * (0.42f + wave * 0.12f),
                        local = local,
                        tint = if (wave % 2 == 0) colors.PrimaryBright else colors.Teal,
                        flicker = heatFlicker
                    )
                }

                if (strikePhase > 0.08f) {
                    val sparkT = ((strikePhase - 0.08f) / 0.92f).coerceIn(0f, 1f)
                    drawSplashForgeSparkShower(
                        center = center,
                        maxRadius = maxR,
                        phase = sparkT,
                        progress = progress,
                        primary = colors.PrimaryBright,
                        accent = colors.Teal
                    )
                }

                val flash = ((strikePhase - 0.02f) / 0.18f).coerceIn(0f, 1f)
                if (flash > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (1f - flash) * 0.55f),
                                colors.PrimaryBright.copy(alpha = (1f - flash) * 0.35f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxR * 0.55f
                        ),
                        radius = maxR * 0.55f,
                        center = center
                    )
                }
            }

            if (helixPhase > 0.2f && strikePhase < 0.85f) {
                for (p in 0 until splashLoopCount(14)) {
                    val local = ((helixPhase - 0.2f - p * 0.03f) / 0.65f).coerceIn(0f, 1f)
                    if (local <= 0f) continue
                    val angle = (p * 25.7f + progress * 140f) * PI.toFloat() / 180f
                    val dist = maxR * (0.92f - local * 0.58f)
                    val sparkPos = center + Offset(cos(angle), sin(angle)) * dist
                    drawCircle(
                        color = colors.PrimaryBright.copy(alpha = (1f - local) * 0.75f * heatFlicker),
                        radius = 2.2f + local * 1.4f,
                        center = sparkPos
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawSplashForgeEmberField(
    center: Offset,
    spread: Float,
    phase: Float,
    flicker: Float,
    primary: Color,
    accent: Color
) {
    for (i in 0 until splashLoopCount(28)) {
        val seed = i * 17.3f
        val rise = ((phase * 1.15f - (i % 7) * 0.04f)).coerceIn(0f, 1f)
        if (rise <= 0f) continue
        val x = center.x + sin(seed) * spread * (0.35f + (i % 5) * 0.11f)
        val y = center.y - rise * spread * (0.18f + (i % 4) * 0.06f)
        val emberR = spread * (0.012f + (i % 3) * 0.004f)
        val tint = if (i % 3 == 0) accent else primary
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = rise * 0.85f * flicker),
                    tint.copy(alpha = rise * 0.7f),
                    Color.Transparent
                ),
                center = Offset(x, y),
                radius = emberR * 2.4f
            ),
            radius = emberR * 2.4f,
            center = Offset(x, y)
        )
    }
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                primary.copy(alpha = 0.42f * phase * flicker),
                accent.copy(alpha = 0.22f * phase),
                Color.Transparent
            ),
            center = center,
            radius = spread * 0.55f
        ),
        topLeft = Offset(center.x - spread * 0.65f, center.y - spread * 0.18f),
        size = Size(spread * 1.3f, spread * 0.36f)
    )
}

private fun DrawScope.drawSplashForgeHelix(
    center: Offset,
    maxRadius: Float,
    phase: Float,
    progress: Float,
    direction: Float,
    tint: Color,
    alpha: Float,
    heatFlicker: Float
) {
    val helixPath = Path()
    var started = false
    val steps = 96
    for (step in 0..steps) {
        val t = (step / steps.toFloat()) * phase
        if (t <= 0f) continue
        val angle = (t * 720f * direction + progress * 52f * direction) * PI.toFloat() / 180f
        val radius = maxRadius * (1.08f - t * 0.78f)
        val wobble = sin(t * PI.toFloat() * 6f + progress * 12f) * maxRadius * 0.018f
        val point = center + Offset(cos(angle), sin(angle)) * (radius + wobble)
        if (!started) {
            helixPath.moveTo(point.x, point.y)
            started = true
        } else {
            helixPath.lineTo(point.x, point.y)
        }
    }
    if (!started) return

    drawPath(
        helixPath,
        brush = Brush.linearGradient(
            colors = listOf(
                tint.copy(alpha = 0.08f * alpha),
                tint.copy(alpha = 0.55f * alpha * heatFlicker),
                Color.White.copy(alpha = 0.72f * alpha),
                tint.copy(alpha = 0.35f * alpha)
            ),
            start = center + Offset(-maxRadius, -maxRadius),
            end = center + Offset(maxRadius, maxRadius)
        ),
        style = Stroke(width = 4.2f, cap = StrokeCap.Round)
    )
    drawPath(
        helixPath,
        color = Color.White.copy(alpha = 0.22f * alpha * heatFlicker),
        style = Stroke(width = 1.4f, cap = StrokeCap.Round)
    )

    for (spark in 0 until splashLoopCount(10)) {
        val t = ((phase * 1.05f - spark * 0.07f)).coerceIn(0f, 1f)
        if (t <= 0.05f) continue
        val angle = (t * 720f * direction + progress * 52f * direction) * PI.toFloat() / 180f
        val radius = maxRadius * (1.08f - t * 0.78f)
        val sparkPos = center + Offset(cos(angle), sin(angle)) * radius
        drawCircle(
            color = Color.White.copy(alpha = alpha * (1f - t) * 0.8f),
            radius = 2.6f,
            center = sparkPos
        )
    }
}

private fun DrawScope.drawSplashForgeCogRing(
    center: Offset,
    radius: Float,
    phase: Float,
    spin: Float,
    primary: Color,
    accent: Color
) {
    rotate(spin, pivot = center) {
        val teeth = 16
        for (i in 0 until teeth) {
            val toothAngle = i * (360f / teeth)
            rotate(toothAngle, pivot = center) {
                val toothLen = radius * 0.07f * phase
                drawLine(
                    color = primary.copy(alpha = 0.55f * phase),
                    start = center + Offset(0f, -radius),
                    end = center + Offset(0f, -radius - toothLen),
                    strokeWidth = 3.2f,
                    cap = StrokeCap.Round
                )
            }
        }
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    primary.copy(alpha = 0.75f * phase),
                    accent.copy(alpha = 0.55f * phase),
                    primary.copy(alpha = 0.75f * phase)
                ),
                center = center
            ),
            radius = radius,
            center = center,
            style = Stroke(width = 2.8f)
        )
        drawCircle(
            color = accent.copy(alpha = 0.35f * phase),
            radius = radius * 0.92f,
            center = center,
            style = Stroke(width = 1.6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 14f)))
        )
    }
}

private fun DrawScope.drawSplashForgeLockRing(
    center: Offset,
    radius: Float,
    phase: Float,
    progress: Float,
    primary: Color,
    accent: Color
) {
    for (i in 0 until 6) {
        val segDelay = i * 0.14f
        val segProgress = splashSmoothStep(((phase - segDelay) / (1f - segDelay)).coerceIn(0f, 1f))
        if (segProgress <= 0f) continue
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    primary.copy(alpha = 0.95f * segProgress),
                    accent.copy(alpha = 0.78f * segProgress),
                    primary.copy(alpha = 0.95f * segProgress)
                ),
                center = center
            ),
            startAngle = i * 60f - 90f,
            sweepAngle = 56f * segProgress,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = 4.2f, cap = StrokeCap.Round)
        )
        if (segProgress > 0.72f) {
            val weldAngle = (i * 60f - 90f + 56f * segProgress) * PI.toFloat() / 180f
            val weldPos = center + Offset(cos(weldAngle), sin(weldAngle)) * radius
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        primary.copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    center = weldPos,
                    radius = 10f
                ),
                radius = 10f,
                center = weldPos
            )
        }
    }
    if (phase > 0.55f) {
        val pulse = ((phase - 0.55f) / 0.45f).coerceIn(0f, 1f)
        drawCircle(
            color = accent.copy(alpha = pulse * 0.35f),
            radius = radius * (1f + sin(progress * PI.toFloat() * 8f) * 0.03f),
            center = center,
            style = Stroke(width = 2f)
        )
    }
}

private fun DrawScope.drawSplashForgeCrucible(
    center: Offset,
    radius: Float,
    phase: Float,
    strikePhase: Float,
    primary: Color,
    accent: Color,
    flicker: Float
) {
    val bowlDepth = radius * 0.38f
    drawArc(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF3A3A42).copy(alpha = 0.85f * phase),
                Color(0xFF1E1E24).copy(alpha = 0.95f * phase)
            ),
            start = center + Offset(-radius, -radius),
            end = center + Offset(radius, radius)
        ),
        startAngle = 200f,
        sweepAngle = 140f * phase,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius * 0.55f),
        size = Size(radius * 2f, radius * 1.35f),
        style = Stroke(width = 5f, cap = StrokeCap.Round)
    )

    val moltenAlpha = (phase * (1f - strikePhase * 0.35f)).coerceIn(0f, 1f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.45f * moltenAlpha * flicker),
                primary.copy(alpha = 0.55f * moltenAlpha),
                accent.copy(alpha = 0.28f * moltenAlpha),
                Color.Transparent
            ),
            center = center + Offset(0f, bowlDepth * 0.15f),
            radius = radius * 0.72f
        ),
        radius = radius * 0.72f,
        center = center + Offset(0f, bowlDepth * 0.15f)
    )

    if (strikePhase > 0.05f) {
        val compress = splashSmoothStep(((strikePhase - 0.05f) / 0.55f).coerceIn(0f, 1f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = (1f - compress) * 0.65f),
                    accent.copy(alpha = (1f - compress) * 0.35f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * (0.85f - compress * 0.55f)
            ),
            radius = radius * (0.85f - compress * 0.55f),
            center = center
        )
    }
}

private fun DrawScope.drawSplashForgeShockwave(
    center: Offset,
    maxRadius: Float,
    local: Float,
    tint: Color,
    flicker: Float
) {
    val eased = splashSmoothStep(local)
    val ringR = maxRadius * eased
    val ringAlpha = (1f - eased) * 0.82f * flicker
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                tint.copy(alpha = ringAlpha),
                Color.White.copy(alpha = ringAlpha * 0.85f),
                tint.copy(alpha = ringAlpha)
            ),
            center = center
        ),
        radius = ringR,
        center = center,
        style = Stroke(width = (5.5f - local * 2.8f).coerceAtLeast(1.8f))
    )
}

private fun DrawScope.drawSplashForgeSparkShower(
    center: Offset,
    maxRadius: Float,
    phase: Float,
    progress: Float,
    primary: Color,
    accent: Color
) {
    for (i in 0 until splashLoopCount(24)) {
        val delay = (i % 6) * 0.04f
        val local = ((phase - delay) / 0.88f).coerceIn(0f, 1f)
        if (local <= 0f) continue
        val angle = (i * 15f + progress * 180f + (i % 3) * 22f) * PI.toFloat() / 180f
        val dist = maxRadius * (0.14f + local * 0.72f)
        val sparkPos = center + Offset(cos(angle), sin(angle)) * dist
        val tint = if (i % 2 == 0) primary else accent
        drawCircle(
            color = tint.copy(alpha = (1f - local) * 0.9f),
            radius = (3.5f - local * 1.8f).coerceAtLeast(1.2f),
            center = sparkPos
        )
        if (local < 0.55f) {
            val trailAngle = angle + PI.toFloat()
            drawLine(
                color = Color.White.copy(alpha = (1f - local) * 0.45f),
                start = sparkPos,
                end = sparkPos + Offset(cos(trailAngle), sin(trailAngle)) * maxRadius * 0.06f * local,
                strokeWidth = 1.6f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun VaultDoorSplashEffect(progress: Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.46f
            val doorR = maxR * 0.62f
            val hinge = center + Offset(-doorR * 1.02f, 0f)
            val chamberPhase = splashSmoothStep((progress / 0.18f).coerceIn(0f, 1f))
            val framePhase = splashSmoothStep(((progress - 0.06f) / 0.28f).coerceIn(0f, 1f))
            val dialPhase = splashSmoothStep(((progress - 0.12f) / 0.38f).coerceIn(0f, 1f))
            val boltPhase = splashSmoothStep(((progress - 0.36f) / 0.24f).coerceIn(0f, 1f))
            val swingPhase = splashSmoothStep(((progress - 0.48f) / 0.38f).coerceIn(0f, 1f))
            val revealPhase = splashSmoothStep(((progress - 0.58f) / 0.38f).coerceIn(0f, 1f))
            val dialRotation = dialPhase * 1080f
            val doorSwing = -swingPhase * 72f
            val steelFlicker = 0.84f + 0.16f * sin(progress * PI.toFloat() * 7f)

            drawSplashVaultChamber(
                center = center,
                maxRadius = maxR,
                phase = chamberPhase,
                accent = colors.Teal
            )

            if (framePhase > 0f) {
                drawSplashVaultDoorFrame(
                    center = center,
                    doorRadius = doorR,
                    phase = framePhase,
                    flicker = steelFlicker
                )
            }

            if (swingPhase > 0.04f || revealPhase > 0f) {
                drawSplashVaultInteriorGlow(
                    center = center,
                    doorRadius = doorR,
                    swingPhase = swingPhase,
                    revealPhase = revealPhase,
                    primary = colors.PrimaryBright,
                    accent = colors.Teal,
                    flicker = steelFlicker
                )
            }

            if (boltPhase > 0f) {
                drawSplashVaultBolts(
                    center = center,
                    doorRadius = doorR,
                    phase = boltPhase,
                    primary = colors.PrimaryBright,
                    accent = colors.Teal
                )
            }

            withTransform({
                rotate(doorSwing, pivot = hinge)
                val clickPulse = if (boltPhase > 0.85f && swingPhase < 0.15f) {
                    1f + sin(progress * PI.toFloat() * 18f) * 0.025f
                } else {
                    1f
                }
                scale(clickPulse, clickPulse, pivot = center)
            }) {
                drawSplashVaultDoorPanel(
                    center = center,
                    doorRadius = doorR,
                    phase = framePhase,
                    flicker = steelFlicker
                )
                if (dialPhase > 0f) {
                    drawSplashVaultDial(
                        center = center,
                        doorRadius = doorR,
                        rotation = dialRotation,
                        phase = dialPhase,
                        boltPhase = boltPhase,
                        primary = colors.PrimaryBright,
                        accent = colors.Teal,
                        flicker = steelFlicker
                    )
                }
            }

            if (revealPhase > 0f) {
                for (wave in 0 until 4) {
                    val delay = wave * 0.12f
                    val local = splashSmoothStep(((revealPhase - delay) / 0.72f).coerceIn(0f, 1f))
                    if (local <= 0f) continue
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                colors.PrimaryBright.copy(alpha = (1f - local) * 0.70f * steelFlicker),
                                Color.White.copy(alpha = (1f - local) * 0.55f),
                                colors.Teal.copy(alpha = (1f - local) * 0.70f * steelFlicker)
                            ),
                            center = center
                        ),
                        radius = doorR * (0.28f + local * (0.55f + wave * 0.06f)),
                        center = center,
                        style = Stroke(width = (4.5f - wave * 0.6f).coerceAtLeast(1.5f))
                    )
                }

                val flash = ((revealPhase - 0.02f) / 0.14f).coerceIn(0f, 1f)
                if (flash > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (1f - flash) * 0.40f),
                                colors.PrimaryBright.copy(alpha = (1f - flash) * 0.28f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = doorR * 0.55f
                        ),
                        radius = doorR * 0.55f,
                        center = center
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawSplashVaultChamber(center: Offset, maxRadius: Float, phase: Float, accent: Color) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF101014).copy(alpha = 0.75f * phase),
                Color(0xFF08080A).copy(alpha = 0.55f * phase),
                Color.Transparent
            ),
            center = center,
            radius = maxRadius * 1.25f
        ),
        topLeft = Offset(center.x - maxRadius * 1.25f, center.y - maxRadius * 1.25f),
        size = Size(maxRadius * 2.5f, maxRadius * 2.5f)
    )
    drawLine(
        color = accent.copy(alpha = 0.18f * phase),
        start = center + Offset(-maxRadius * 0.95f, maxRadius * 0.72f),
        end = center + Offset(maxRadius * 0.95f, maxRadius * 0.72f),
        strokeWidth = 2.4f,
        cap = StrokeCap.Round
    )
    for (tile in 0 until 5) {
        val x = center.x + (tile - 2) * maxRadius * 0.22f
        drawLine(
            color = Color(0xFF3A3A42).copy(alpha = 0.35f * phase),
            start = Offset(x, center.y + maxRadius * 0.68f),
            end = Offset(x, center.y + maxRadius * 0.95f),
            strokeWidth = 1.2f
        )
    }
}

private fun DrawScope.drawSplashVaultDoorFrame(
    center: Offset,
    doorRadius: Float,
    phase: Float,
    flicker: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF4A4A52).copy(alpha = 0.95f * phase),
                Color(0xFF222228).copy(alpha = 0.98f * phase)
            ),
            center = center - Offset(doorRadius * 0.08f, doorRadius * 0.10f),
            radius = doorRadius * 1.14f
        ),
        radius = doorRadius * 1.14f,
        center = center
    )
    drawCircle(
        color = Color(0xFF9A9AA2).copy(alpha = 0.65f * phase * flicker),
        radius = doorRadius * 1.14f,
        center = center,
        style = Stroke(width = 4.2f)
    )
    drawCircle(
        color = Color(0xFF5A5A62).copy(alpha = 0.45f * phase),
        radius = doorRadius * 1.06f,
        center = center,
        style = Stroke(width = 2f)
    )
    for (rivet in 0 until splashLoopCount(12)) {
        rotate(rivet * 30f, pivot = center) {
            val rivetPos = center + Offset(0f, -doorRadius * 1.08f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFBCBCC4).copy(alpha = phase), Color(0xFF5A5A62).copy(alpha = phase)),
                    center = rivetPos,
                    radius = 4f
                ),
                radius = 3.2f,
                center = rivetPos
            )
        }
    }
}

private fun DrawScope.drawSplashVaultDoorPanel(
    center: Offset,
    doorRadius: Float,
    phase: Float,
    flicker: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF787880).copy(alpha = 0.98f * phase),
                Color(0xFF45454C).copy(alpha = 0.98f * phase),
                Color(0xFF28282E).copy(alpha = 0.98f * phase)
            ),
            center = center - Offset(doorRadius * 0.18f, doorRadius * 0.22f),
            radius = doorRadius * 1.05f
        ),
        radius = doorRadius,
        center = center
    )
    drawArc(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f * phase * flicker),
                Color.Transparent
            ),
            start = center + Offset(-doorRadius, -doorRadius),
            end = center + Offset(doorRadius * 0.3f, doorRadius * 0.3f)
        ),
        startAngle = 200f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(center.x - doorRadius, center.y - doorRadius),
        size = Size(doorRadius * 2f, doorRadius * 2f),
        style = Stroke(width = doorRadius * 0.35f)
    )
}

private fun DrawScope.drawSplashVaultDial(
    center: Offset,
    doorRadius: Float,
    rotation: Float,
    phase: Float,
    boltPhase: Float,
    primary: Color,
    accent: Color,
    flicker: Float
) {
    rotate(rotation, pivot = center) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF505058), Color(0xFF1E1E24)),
                center = center,
                radius = doorRadius * 0.40f
            ),
            radius = doorRadius * 0.40f,
            center = center
        )
        drawCircle(
            color = Color(0xFF888890).copy(alpha = 0.75f * phase),
            radius = doorRadius * 0.40f,
            center = center,
            style = Stroke(width = 2.8f)
        )
        for (tick in 0 until splashLoopCount(24)) {
            // Capping the threshold at phase * 0.85f (rather than just phase) left the last 3 of
            // 24 ticks permanently unlit even once the dial finished its sequence at phase = 1.
            val lit = tick / 24f <= phase
            rotate(tick * 15f, pivot = center) {
                drawLine(
                    color = if (lit) accent.copy(alpha = 0.95f * flicker) else Color(0xFF666670).copy(alpha = 0.55f * phase),
                    start = center + Offset(0f, -doorRadius * 0.36f),
                    end = center + Offset(0f, -doorRadius * (if (tick % 6 == 0) 0.28f else 0.31f)),
                    strokeWidth = if (tick % 6 == 0) 2.8f else 1.6f,
                    cap = StrokeCap.Round
                )
            }
        }
        for (i in 0 until 6) {
            rotate(i * 60f, pivot = center) {
                drawLine(
                    color = primary.copy(alpha = 0.92f * phase * flicker),
                    start = center + Offset(0f, -doorRadius * 0.34f),
                    end = center + Offset(0f, -doorRadius * 0.20f),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.85f), accent.copy(alpha = 0.65f)),
                center = center,
                radius = doorRadius * 0.08f
            ),
            radius = doorRadius * 0.08f,
            center = center
        )
    }
    if (boltPhase > 0.88f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = (boltPhase - 0.88f) / 0.12f * 0.45f),
                    Color.Transparent
                ),
                center = center,
                radius = doorRadius * 0.22f
            ),
            radius = doorRadius * 0.22f,
            center = center
        )
    }
}

private fun DrawScope.drawSplashVaultBolts(
    center: Offset,
    doorRadius: Float,
    phase: Float,
    primary: Color,
    accent: Color
) {
    for (bolt in 0 until splashLoopCount(8)) {
        val delay = bolt * 0.08f
        val local = splashSmoothStep(((phase - delay) / 0.78f).coerceIn(0f, 1f))
        if (local <= 0f) continue
        rotate(bolt * 45f, pivot = center) {
            val retract = local * doorRadius * 0.10f
            val boltStart = center + Offset(0f, -doorRadius * 1.02f)
            val boltEnd = center + Offset(0f, -doorRadius * 0.88f + retract)
            drawLine(
                color = accent.copy(alpha = 0.85f * (1f - local * 0.4f)),
                start = boltStart,
                end = boltEnd,
                strokeWidth = 4.5f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = primary.copy(alpha = 0.90f * (1f - local * 0.3f)),
                radius = 3.5f,
                center = boltEnd
            )
        }
    }
}

private fun DrawScope.drawSplashVaultInteriorGlow(
    center: Offset,
    doorRadius: Float,
    swingPhase: Float,
    revealPhase: Float,
    primary: Color,
    accent: Color,
    flicker: Float
) {
    val glow = (swingPhase * 0.65f + revealPhase * 0.55f).coerceIn(0f, 1f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f * glow * flicker),
                primary.copy(alpha = 0.48f * glow),
                accent.copy(alpha = 0.28f * glow),
                Color.Transparent
            ),
            center = center,
            radius = doorRadius * 1.15f
        ),
        radius = doorRadius * 1.15f,
        center = center
    )
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                primary.copy(alpha = 0.12f * glow),
                Color.Transparent
            ),
            start = center + Offset(-doorRadius * 0.5f, 0f),
            end = center + Offset(doorRadius * 1.2f, 0f)
        ),
        topLeft = Offset(center.x - doorRadius * 0.3f, center.y - doorRadius * 0.8f),
        size = Size(doorRadius * 1.5f * swingPhase.coerceAtLeast(0.15f), doorRadius * 1.6f)
    )
}

@Composable
private fun RadarSplashEffect(progress: Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    val contacts = listOf(
        RadarContact(28f, 0.62f),
        RadarContact(74f, 0.44f),
        RadarContact(118f, 0.78f),
        RadarContact(168f, 0.52f),
        RadarContact(214f, 0.68f),
        RadarContact(258f, 0.38f),
        RadarContact(302f, 0.58f),
        RadarContact(342f, 0.72f)
    )
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.46f
            val bootPhase = splashSmoothStep((progress / 0.18f).coerceIn(0f, 1f))
            val hudPhase = splashSmoothStep(((progress - 0.05f) / 0.26f).coerceIn(0f, 1f))
            val ringsPhase = splashSmoothStep(((progress - 0.10f) / 0.34f).coerceIn(0f, 1f))
            val sweepPhase = splashSmoothStep(((progress - 0.14f) / 0.54f).coerceIn(0f, 1f))
            val acquirePhase = splashSmoothStep(((progress - 0.34f) / 0.42f).coerceIn(0f, 1f))
            val lockPhase = splashSmoothStep(((progress - 0.58f) / 0.38f).coerceIn(0f, 1f))
            val sweepAngle = -90f + progress * 480f
            val crtFlicker = 0.80f + 0.20f * sin(progress * PI.toFloat() * 9f)

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF061018).copy(alpha = 0.55f * bootPhase),
                        Color(0xFF0A1418).copy(alpha = 0.35f * bootPhase),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxR * 1.35f
                ),
                topLeft = Offset(center.x - maxR * 1.35f, center.y - maxR * 1.35f),
                size = Size(maxR * 2.7f, maxR * 2.7f)
            )

            if (hudPhase > 0f) {
                drawSplashRadarHudFrame(
                    center = center,
                    extent = maxR * 1.02f,
                    phase = hudPhase,
                    tint = colors.Teal
                )
            }

            if (ringsPhase > 0f) {
                drawSplashRadarRangeRings(
                    center = center,
                    maxRadius = maxR,
                    phase = ringsPhase,
                    flicker = crtFlicker,
                    primary = colors.PrimaryBright,
                    accent = colors.Teal
                )
            }

            if (hudPhase > 0.25f) {
                val crossAlpha = ((hudPhase - 0.25f) / 0.75f).coerceIn(0f, 1f)
                drawSplashRadarCrosshairs(
                    center = center,
                    extent = maxR * 0.92f,
                    alpha = crossAlpha * 0.55f * crtFlicker,
                    tint = colors.Teal
                )
            }

            if (sweepPhase > 0f) {
                drawSplashRadarSweep(
                    center = center,
                    maxRadius = maxR,
                    sweepAngle = sweepAngle,
                    phase = sweepPhase,
                    lockPhase = lockPhase,
                    primary = colors.PrimaryBright,
                    accent = colors.Teal,
                    flicker = crtFlicker
                )
            }

            if (acquirePhase > 0f) {
                contacts.forEachIndexed { index, contact ->
                    val hitProgress = drawSplashRadarContact(
                        center = center,
                        maxRadius = maxR,
                        contact = contact,
                        sweepAngle = sweepAngle,
                        progress = progress,
                        index = index,
                        primary = colors.PrimaryBright,
                        accent = colors.Teal,
                        flicker = crtFlicker
                    )
                    if (hitProgress > 0.75f && lockPhase < 0.35f) {
                        val ripple = ((hitProgress - 0.75f) / 0.25f).coerceIn(0f, 1f)
                        val contactRad = contact.angleDeg * PI.toFloat() / 180f
                        val contactPos = center + Offset(cos(contactRad), sin(contactRad)) * maxR * contact.distFrac
                        drawCircle(
                            color = colors.Teal.copy(alpha = (1f - ripple) * 0.45f),
                            radius = maxR * 0.06f + ripple * maxR * 0.22f,
                            center = contactPos,
                            style = Stroke(width = 1.8f)
                        )
                    }
                }
            }

            if (lockPhase > 0f) {
                drawSplashRadarLockReticle(
                    center = center,
                    maxRadius = maxR,
                    phase = lockPhase,
                    progress = progress,
                    primary = colors.PrimaryBright,
                    accent = colors.Teal,
                    flicker = crtFlicker
                )
            }

            if (bootPhase > 0.2f) {
                val scanY = center.y - maxR + ((progress * 2.8f) % 1f) * maxR * 2f
                drawLine(
                    color = colors.PrimaryBright.copy(alpha = 0.08f * bootPhase * crtFlicker),
                    start = Offset(center.x - maxR, scanY),
                    end = Offset(center.x + maxR, scanY),
                    strokeWidth = 1.2f
                )
            }

            if (sweepPhase > 0.15f && lockPhase < 0.9f) {
                rotate(progress * 18f, pivot = center) {
                    for (tick in 0 until splashLoopCount(36)) {
                        val tickAngle = tick * 10f
                        rotate(tickAngle, pivot = center) {
                            val tickAlpha = sweepPhase * 0.35f * crtFlicker
                            drawLine(
                                color = colors.Teal.copy(alpha = tickAlpha),
                                start = center + Offset(0f, -maxR * 0.96f),
                                end = center + Offset(0f, -maxR * (if (tick % 3 == 0) 0.88f else 0.92f)),
                                strokeWidth = if (tick % 3 == 0) 1.8f else 1f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class RadarContact(val angleDeg: Float, val distFrac: Float)

private fun DrawScope.drawSplashRadarHudFrame(center: Offset, extent: Float, phase: Float, tint: Color) {
    val corner = extent * 0.22f
    val arm = extent * 0.34f
    val alpha = phase * 0.85f
    listOf(
        Offset(-1f, -1f), Offset(1f, -1f), Offset(-1f, 1f), Offset(1f, 1f)
    ).forEach { sign ->
        val origin = center + Offset(sign.x * extent * 0.78f, sign.y * extent * 0.78f)
        drawLine(
            color = tint.copy(alpha = alpha),
            start = origin,
            end = origin + Offset(sign.x * corner, 0f),
            strokeWidth = 2.4f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint.copy(alpha = alpha),
            start = origin,
            end = origin + Offset(0f, sign.y * corner),
            strokeWidth = 2.4f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint.copy(alpha = alpha * 0.45f),
            start = center + Offset(sign.x * arm * 0.35f, sign.y * extent * 0.55f),
            end = center + Offset(sign.x * arm, sign.y * extent * 0.55f),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSplashRadarRangeRings(
    center: Offset,
    maxRadius: Float,
    phase: Float,
    flicker: Float,
    primary: Color,
    accent: Color
) {
    for (i in 1..6) {
        val delay = (i - 1) * 0.10f
        val local = splashSmoothStep(((phase - delay) / 0.78f).coerceIn(0f, 1f))
        if (local <= 0f) continue
        val ringR = maxRadius * (i / 6f) * local
        val ringAlpha = (0.34f - i * 0.03f) * local * flicker
        val tint = if (i % 2 == 0) primary else accent
        drawCircle(
            color = tint.copy(alpha = ringAlpha.coerceAtLeast(0.05f)),
            radius = ringR,
            center = center,
            style = Stroke(width = if (i == 6) 2f else 1.3f)
        )
        if (local > 0.55f) {
            val pulse = sin((local + i * 0.2f) * PI.toFloat() * 3f) * 0.5f + 0.5f
            drawCircle(
                color = tint.copy(alpha = pulse * 0.12f * local),
                radius = ringR * 1.04f,
                center = center,
                style = Stroke(width = 1f)
            )
        }
    }
}

private fun DrawScope.drawSplashRadarCrosshairs(center: Offset, extent: Float, alpha: Float, tint: Color) {
    drawLine(
        color = tint.copy(alpha = alpha),
        start = center + Offset(-extent, 0f),
        end = center + Offset(extent, 0f),
        strokeWidth = 1.2f
    )
    drawLine(
        color = tint.copy(alpha = alpha),
        start = center + Offset(0f, -extent),
        end = center + Offset(0f, extent),
        strokeWidth = 1.2f
    )
    drawCircle(
        color = tint.copy(alpha = alpha * 0.7f),
        radius = extent * 0.12f,
        center = center,
        style = Stroke(width = 1.2f)
    )
}

private fun DrawScope.drawSplashRadarSweep(
    center: Offset,
    maxRadius: Float,
    sweepAngle: Float,
    phase: Float,
    lockPhase: Float,
    primary: Color,
    accent: Color,
    flicker: Float
) {
    val sweepAlpha = (phase * (1f - lockPhase * 0.55f)).coerceIn(0f, 1f)
    for (trail in 0..7) {
        val trailAlpha = (0.42f - trail * 0.045f) * sweepAlpha * flicker
        if (trailAlpha <= 0f) continue
        rotate(sweepAngle - trail * 11f, pivot = center) {
            drawArc(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = trailAlpha),
                        accent.copy(alpha = trailAlpha * 0.55f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                ),
                startAngle = -38f,
                sweepAngle = 38f,
                useCenter = true,
                topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                size = Size(maxRadius * 2f, maxRadius * 2f)
            )
        }
    }

    rotate(sweepAngle, pivot = center) {
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f * sweepAlpha),
                    primary.copy(alpha = 0.85f * sweepAlpha),
                    Color.Transparent
                ),
                start = center,
                end = center + Offset(maxRadius * 0.98f, 0f)
            ),
            start = center,
            end = center + Offset(maxRadius * 0.98f, 0f),
            strokeWidth = 3.2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = primary.copy(alpha = 0.35f * sweepAlpha),
            start = center,
            end = center + Offset(maxRadius * 0.98f, 0f),
            strokeWidth = 7f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSplashRadarContact(
    center: Offset,
    maxRadius: Float,
    contact: RadarContact,
    sweepAngle: Float,
    progress: Float,
    index: Int,
    primary: Color,
    accent: Color,
    flicker: Float
): Float {
    // The sweep does 480 degrees total (1.33 full rotations) so every contact is guaranteed to
    // get crossed at least once, but that also means naively wrapping the angle into 0-360 for
    // the hit test made already-acquired contacts un-hit for a few frames right as the sweep
    // wrapped past 360 degrees (they'd all vanish and then pop back via a forced-visible
    // fallback once the effect got close to finishing). Comparing against the raw, never-wrapped
    // sweep angle instead means "hit" only ever turns true once, and can never flicker back off.
    val unwrappedSweep = sweepAngle + 90f
    val hit = unwrappedSweep >= contact.angleDeg
    if (!hit) return 0f

    val hitProgress = splashSmoothStep(((unwrappedSweep - contact.angleDeg) / 48f).coerceIn(0f, 1f))
    if (hitProgress <= 0f) return 0f

    val rad = contact.angleDeg * PI.toFloat() / 180f
    val point = center + Offset(cos(rad), sin(rad)) * maxRadius * contact.distFrac
    val blink = 0.65f + 0.35f * sin(progress * PI.toFloat() * (6f + index))

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.9f * hitProgress * blink),
                primary.copy(alpha = 0.85f * hitProgress),
                Color.Transparent
            ),
            center = point,
            radius = 12f
        ),
        radius = 12f,
        center = point
    )
    drawCircle(
        color = accent.copy(alpha = 0.95f * hitProgress * flicker),
        radius = 4.5f,
        center = point
    )

    for (ring in 0 until 2) {
        val ringDelay = ring * 0.18f
        val ringLocal = ((hitProgress - ringDelay) / 0.75f).coerceIn(0f, 1f)
        if (ringLocal <= 0f) continue
        drawCircle(
            color = primary.copy(alpha = (1f - ringLocal) * 0.55f),
            radius = maxRadius * 0.05f + ringLocal * maxRadius * 0.16f,
            center = point,
            style = Stroke(width = 1.6f)
        )
    }

    return hitProgress
}

private fun DrawScope.drawSplashRadarLockReticle(
    center: Offset,
    maxRadius: Float,
    phase: Float,
    progress: Float,
    primary: Color,
    accent: Color,
    flicker: Float
) {
    val bracketSize = maxRadius * 0.22f * (1f - phase * 0.42f)
    val bracketInset = maxRadius * 0.28f * (1f - phase * 0.35f)
    listOf(
        Offset(-1f, -1f), Offset(1f, -1f), Offset(-1f, 1f), Offset(1f, 1f)
    ).forEach { sign ->
        val corner = center + Offset(sign.x * bracketInset, sign.y * bracketInset)
        drawLine(
            color = primary.copy(alpha = 0.95f * phase * flicker),
            start = corner,
            end = corner + Offset(sign.x * bracketSize, 0f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = primary.copy(alpha = 0.95f * phase * flicker),
            start = corner,
            end = corner + Offset(0f, sign.y * bracketSize),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }

    for (ring in 0 until 4) {
        val delay = ring * 0.12f
        val local = splashSmoothStep(((phase - delay) / 0.72f).coerceIn(0f, 1f))
        if (local <= 0f) continue
        val lockR = maxRadius * (0.16f + local * (0.10f + ring * 0.05f))
        val tint = if (ring % 2 == 0) primary else accent
        drawCircle(
            color = tint.copy(alpha = (1f - local * 0.35f) * 0.75f * flicker),
            radius = lockR,
            center = center,
            style = Stroke(width = (3.2f - ring * 0.5f).coerceAtLeast(1.4f))
        )
    }

    if (phase > 0.35f) {
        val pulse = sin(progress * PI.toFloat() * 10f) * 0.5f + 0.5f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = pulse * 0.25f * phase),
                    primary.copy(alpha = pulse * 0.18f * phase),
                    Color.Transparent
                ),
                center = center,
                radius = maxRadius * 0.28f
            ),
            radius = maxRadius * 0.28f,
            center = center
        )
    }
}

@Composable
private fun TriangulateSplashEffect(progress: Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.46f
            val bootPhase = splashSmoothStep((progress / 0.18f).coerceIn(0f, 1f))
            val gridPhase = splashSmoothStep(((progress - 0.05f) / 0.28f).coerceIn(0f, 1f))
            val towerPhase = splashSmoothStep(((progress - 0.10f) / 0.34f).coerceIn(0f, 1f))
            val beamPhase = splashSmoothStep(((progress - 0.22f) / 0.40f).coerceIn(0f, 1f))
            val trianglePhase = splashSmoothStep(((progress - 0.40f) / 0.34f).coerceIn(0f, 1f))
            val lockPhase = splashSmoothStep(((progress - 0.58f) / 0.38f).coerceIn(0f, 1f))
            val signalFlicker = 0.82f + 0.18f * sin(progress * PI.toFloat() * 8f)
            val towers = listOf(
                center + Offset(-maxR * 0.82f, -maxR * 0.58f),
                center + Offset(maxR * 0.88f, -maxR * 0.42f),
                center + Offset(0f, maxR * 0.92f)
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF080C14).copy(alpha = 0.62f * bootPhase),
                        Color(0xFF0A1018).copy(alpha = 0.38f * bootPhase),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxR * 1.32f
                ),
                topLeft = Offset(center.x - maxR * 1.32f, center.y - maxR * 1.32f),
                size = Size(maxR * 2.64f, maxR * 2.64f)
            )

            if (gridPhase > 0f) {
                drawSplashTriangulateGrid(
                    center = center,
                    extent = maxR * 1.05f,
                    phase = gridPhase,
                    tint = colors.Teal
                )
                drawSplashTriangulateStarField(
                    center = center,
                    spread = maxR * 1.2f,
                    phase = gridPhase,
                    progress = progress,
                    tint = colors.PrimaryBright
                )
            }

            if (trianglePhase > 0f) {
                drawSplashTriangulateTriangleMesh(
                    towers = towers,
                    center = center,
                    phase = trianglePhase,
                    progress = progress,
                    primary = colors.PrimaryBright,
                    accent = colors.Teal,
                    flicker = signalFlicker
                )
            }

            if (beamPhase > 0f) {
                towers.forEachIndexed { index, tower ->
                    drawSplashTriangulateBeam(
                        tower = tower,
                        center = center,
                        phase = beamPhase,
                        index = index,
                        progress = progress,
                        lockPhase = lockPhase,
                        primary = colors.PrimaryBright,
                        accent = colors.Teal,
                        flicker = signalFlicker
                    )
                }
            }

            if (towerPhase > 0f) {
                towers.forEachIndexed { index, tower ->
                    drawSplashTriangulateTower(
                        tower = tower,
                        phase = towerPhase,
                        index = index,
                        progress = progress,
                        maxRadius = maxR,
                        primary = colors.PrimaryBright,
                        accent = colors.Teal,
                        flicker = signalFlicker
                    )
                }
            }

            if (lockPhase > 0f) {
                drawSplashTriangulateLockReticle(
                    center = center,
                    maxRadius = maxR,
                    phase = lockPhase,
                    progress = progress,
                    primary = colors.PrimaryBright,
                    accent = colors.Teal,
                    flicker = signalFlicker
                )
            }

            if (trianglePhase > 0.45f && lockPhase < 0.85f) {
                for (node in 0 until splashLoopCount(9)) {
                    val edgeT = ((trianglePhase - 0.45f) * 1.4f + node * 0.09f) % 1f
                    val edgeIndex = node % 3
                    val start = towers[edgeIndex]
                    val end = towers[(edgeIndex + 1) % 3]
                    val pos = start + (end - start) * edgeT
                    drawCircle(
                        color = colors.PrimaryBright.copy(alpha = (1f - edgeT) * 0.55f * signalFlicker),
                        radius = 2.4f,
                        center = pos
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawSplashTriangulateGrid(center: Offset, extent: Float, phase: Float, tint: Color) {
    val step = extent * 0.18f
    val alpha = phase * 0.22f
    var y = center.y - extent
    while (y <= center.y + extent) {
        drawLine(
            color = tint.copy(alpha = alpha),
            start = Offset(center.x - extent, y),
            end = Offset(center.x + extent, y),
            strokeWidth = 0.9f
        )
        y += step
    }
    var x = center.x - extent
    while (x <= center.x + extent) {
        drawLine(
            color = tint.copy(alpha = alpha),
            start = Offset(x, center.y - extent),
            end = Offset(x, center.y + extent),
            strokeWidth = 0.9f
        )
        x += step
    }
    drawCircle(
        color = tint.copy(alpha = alpha * 1.4f),
        radius = extent * 0.92f,
        center = center,
        style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f)))
    )
}

private fun DrawScope.drawSplashTriangulateStarField(
    center: Offset,
    spread: Float,
    phase: Float,
    progress: Float,
    tint: Color
) {
    for (i in 0 until splashLoopCount(18)) {
        val seed = i * 23.7f
        val x = center.x + cos(seed) * spread * (0.25f + (i % 5) * 0.14f)
        val y = center.y + sin(seed * 1.3f) * spread * (0.22f + (i % 4) * 0.12f)
        val twinkle = 0.45f + 0.55f * sin(progress * PI.toFloat() * (4f + i * 0.3f))
        drawCircle(
            color = tint.copy(alpha = phase * 0.35f * twinkle),
            radius = 1.2f + (i % 3) * 0.6f,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawSplashTriangulateTower(
    tower: Offset,
    phase: Float,
    index: Int,
    progress: Float,
    maxRadius: Float,
    primary: Color,
    accent: Color,
    flicker: Float
) {
    val delay = index * 0.14f
    val local = splashSmoothStep(((phase - delay) / 0.78f).coerceIn(0f, 1f))
    if (local <= 0f) return

    drawLine(
        color = accent.copy(alpha = 0.55f * local),
        start = tower + Offset(0f, maxRadius * 0.04f),
        end = tower + Offset(0f, maxRadius * 0.12f),
        strokeWidth = 2.4f,
        cap = StrokeCap.Round
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f * local),
                primary.copy(alpha = 0.75f * local),
                Color.Transparent
            ),
            center = tower,
            radius = 14f
        ),
        radius = 14f,
        center = tower
    )
    drawCircle(
        color = accent.copy(alpha = 0.95f * local * flicker),
        radius = 5f,
        center = tower
    )

    for (ring in 0 until 3) {
        val ringDelay = ring * 0.16f
        val ringLocal = ((local - ringDelay) / 0.75f).coerceIn(0f, 1f)
        if (ringLocal <= 0f) continue
        drawCircle(
            color = primary.copy(alpha = (1f - ringLocal) * 0.55f),
            radius = maxRadius * 0.06f + ringLocal * maxRadius * (0.22f + ring * 0.06f),
            center = tower,
            style = Stroke(width = (2.2f - ring * 0.4f).coerceAtLeast(1.2f))
        )
    }
}

private fun DrawScope.drawSplashTriangulateBeam(
    tower: Offset,
    center: Offset,
    phase: Float,
    index: Int,
    progress: Float,
    lockPhase: Float,
    primary: Color,
    accent: Color,
    flicker: Float
) {
    val delay = index * 0.10f
    val local = splashSmoothStep(((phase - delay) / 0.82f).coerceIn(0f, 1f))
    if (local <= 0f) return

    val beamAlpha = (local * (1f - lockPhase * 0.35f)).coerceIn(0f, 1f)
    val lineEnd = tower + (center - tower) * local
    drawLine(
        color = accent.copy(alpha = 0.22f * beamAlpha),
        start = tower,
        end = lineEnd,
        strokeWidth = 6f,
        cap = StrokeCap.Round
    )
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                primary.copy(alpha = 0.95f * beamAlpha * flicker),
                accent.copy(alpha = 0.65f * beamAlpha),
                Color.Transparent
            ),
            start = tower,
            end = lineEnd
        ),
        start = tower,
        end = lineEnd,
        strokeWidth = 2.6f,
        cap = StrokeCap.Round
    )

    for (pulse in 0 until 3) {
        val pulseT = ((local * 1.1f - pulse * 0.22f + progress * 0.08f) % 1f).coerceIn(0f, 1f)
        val pulsePos = tower + (center - tower) * pulseT
        drawCircle(
            color = Color.White.copy(alpha = (1f - pulseT) * 0.75f * beamAlpha),
            radius = 3.2f,
            center = pulsePos
        )
    }
}

private fun DrawScope.drawSplashTriangulateTriangleMesh(
    towers: List<Offset>,
    center: Offset,
    phase: Float,
    progress: Float,
    primary: Color,
    accent: Color,
    flicker: Float
) {
    for (edge in 0 until 3) {
        val segDelay = edge * 0.18f
        val segLocal = splashSmoothStep(((phase - segDelay) / 0.72f).coerceIn(0f, 1f))
        if (segLocal <= 0f) continue
        val start = towers[edge]
        val end = towers[(edge + 1) % 3]
        val segEnd = start + (end - start) * segLocal
        drawLine(
            color = accent.copy(alpha = 0.18f * segLocal),
            start = start,
            end = segEnd,
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    primary.copy(alpha = 0.85f * segLocal * flicker),
                    accent.copy(alpha = 0.75f * segLocal),
                    primary.copy(alpha = 0.85f * segLocal * flicker)
                ),
                start = start,
                end = segEnd
            ),
            start = start,
            end = segEnd,
            strokeWidth = 2.4f,
            cap = StrokeCap.Round
        )
    }

    if (phase > 0.55f) {
        val fill = splashSmoothStep(((phase - 0.55f) / 0.45f).coerceIn(0f, 1f))
        val trianglePath = Path().apply {
            moveTo(towers[0].x, towers[0].y)
            lineTo(towers[1].x, towers[1].y)
            lineTo(towers[2].x, towers[2].y)
            close()
        }
        drawPath(
            trianglePath,
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = 0.16f * fill),
                    accent.copy(alpha = 0.10f * fill),
                    Color.Transparent
                ),
                center = center,
                radius = (towers[0] - center).getDistance()
            )
        )
    }
}

private fun DrawScope.drawSplashTriangulateLockReticle(
    center: Offset,
    maxRadius: Float,
    phase: Float,
    progress: Float,
    primary: Color,
    accent: Color,
    flicker: Float
) {
    val bracket = maxRadius * 0.20f * (1f - phase * 0.38f)
    val inset = maxRadius * 0.24f * (1f - phase * 0.32f)
    listOf(
        Offset(-1f, -1f), Offset(1f, -1f), Offset(-1f, 1f), Offset(1f, 1f)
    ).forEach { sign ->
        val corner = center + Offset(sign.x * inset, sign.y * inset)
        drawLine(
            color = primary.copy(alpha = 0.95f * phase * flicker),
            start = corner,
            end = corner + Offset(sign.x * bracket, 0f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = primary.copy(alpha = 0.95f * phase * flicker),
            start = corner,
            end = corner + Offset(0f, sign.y * bracket),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }

    for (ring in 0 until 4) {
        val delay = ring * 0.11f
        val local = splashSmoothStep(((phase - delay) / 0.74f).coerceIn(0f, 1f))
        if (local <= 0f) continue
        val lockR = maxRadius * (0.14f + local * (0.08f + ring * 0.04f))
        val tint = if (ring % 2 == 0) primary else accent
        drawCircle(
            color = tint.copy(alpha = (1f - local * 0.4f) * 0.78f * flicker),
            radius = lockR,
            center = center,
            style = Stroke(width = (3f - ring * 0.45f).coerceAtLeast(1.3f))
        )
    }

    if (phase > 0.30f) {
        val pulse = sin(progress * PI.toFloat() * 9f) * 0.5f + 0.5f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = pulse * 0.28f * phase),
                    primary.copy(alpha = pulse * 0.20f * phase),
                    Color.Transparent
                ),
                center = center,
                radius = maxRadius * 0.26f
            ),
            radius = maxRadius * 0.26f,
            center = center
        )
        drawLine(
            color = accent.copy(alpha = 0.65f * phase),
            start = center + Offset(-maxRadius * 0.08f, 0f),
            end = center + Offset(maxRadius * 0.08f, 0f),
            strokeWidth = 1.6f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accent.copy(alpha = 0.65f * phase),
            start = center + Offset(0f, -maxRadius * 0.08f),
            end = center + Offset(0f, maxRadius * 0.08f),
            strokeWidth = 1.6f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun SignalStormSplashEffect(progress: Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    val lightningOrigins = listOf(
        Offset(-0.92f, -0.68f), Offset(0.88f, -0.72f), Offset(-0.78f, 0.82f),
        Offset(0.94f, 0.64f), Offset(-0.18f, -0.96f), Offset(0.22f, 0.94f)
    )
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.46f
            val chargePhase = splashSmoothStep((progress / 0.20f).coerceIn(0f, 1f))
            val ionPhase = splashSmoothStep(((progress - 0.08f) / 0.34f).coerceIn(0f, 1f))
            val boltPhase = splashSmoothStep(((progress - 0.18f) / 0.42f).coerceIn(0f, 1f))
            val eyePhase = splashSmoothStep(((progress - 0.38f) / 0.36f).coerceIn(0f, 1f))
            val eruptPhase = splashSmoothStep(((progress - 0.58f) / 0.38f).coerceIn(0f, 1f))
            val stormFlicker = 0.76f + 0.24f * sin(progress * PI.toFloat() * 11f)

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF120818).copy(alpha = 0.70f * chargePhase),
                        Color(0xFF0A0610).copy(alpha = 0.50f * chargePhase),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxR * 1.38f
                ),
                topLeft = Offset(center.x - maxR * 1.38f, center.y - maxR * 1.38f),
                size = Size(maxR * 2.76f, maxR * 2.76f)
            )

            if (chargePhase > 0f) {
                drawSplashStormStaticField(
                    center = center,
                    extent = maxR * 1.05f,
                    phase = chargePhase,
                    progress = progress,
                    tint = colors.Teal
                )
            }

            if (ionPhase > 0f) {
                for (band in 0 until 4) {
                    drawSplashStormIonBand(
                        center = center,
                        maxRadius = maxR,
                        band = band,
                        phase = ionPhase,
                        progress = progress,
                        eruptPhase = eruptPhase,
                        primary = colors.PrimaryBright,
                        accent = colors.Teal,
                        flicker = stormFlicker
                    )
                }
                for (particle in 0 until splashLoopCount(22)) {
                    val seed = particle * 19.4f
                    val orbit = ionPhase * (1.05f + (particle % 4) * 0.08f)
                    val angle = (seed + progress * (140f + particle * 8f)) * PI.toFloat() / 180f
                    val dist = maxR * (0.28f + (particle % 5) * 0.11f) * orbit
                    val pos = center + Offset(cos(angle), sin(angle)) * dist
                    drawCircle(
                        color = colors.PrimaryBright.copy(alpha = (1f - orbit * 0.35f) * 0.55f * stormFlicker),
                        radius = 1.8f + (particle % 3),
                        center = pos
                    )
                }
            }

            if (boltPhase > 0f) {
                lightningOrigins.forEachIndexed { index, originFrac ->
                    val delay = index * 0.09f
                    val local = splashSmoothStep(((boltPhase - delay) / 0.78f).coerceIn(0f, 1f))
                    if (local <= 0f) return@forEachIndexed
                    val start = center + Offset(originFrac.x * maxR, originFrac.y * maxR)
                    val end = center + Offset(
                        sin(index * 2.7f + progress * 6f) * maxR * 0.06f,
                        cos(index * 1.9f + progress * 5f) * maxR * 0.06f
                    )
                    drawSplashStormLightning(
                        start = start,
                        end = end,
                        phase = local,
                        seed = index * 31 + 7,
                        primary = colors.PrimaryBright,
                        accent = colors.Teal,
                        flicker = stormFlicker
                    )
                }
            }

            if (eyePhase > 0f) {
                drawSplashStormEye(
                    center = center,
                    maxRadius = maxR,
                    phase = eyePhase,
                    progress = progress,
                    eruptPhase = eruptPhase,
                    primary = colors.PrimaryBright,
                    accent = colors.Teal,
                    flicker = stormFlicker
                )
            }

            if (eruptPhase > 0f) {
                for (wave in 0 until 5) {
                    val delay = wave * 0.10f
                    val local = splashSmoothStep(((eruptPhase - delay) / 0.72f).coerceIn(0f, 1f))
                    if (local <= 0f) continue
                    val ringR = maxR * (0.20f + local * (0.55f + wave * 0.08f))
                    val tint = if (wave % 2 == 0) colors.PrimaryBright else colors.Teal
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                tint.copy(alpha = (1f - local) * 0.80f * stormFlicker),
                                Color.White.copy(alpha = (1f - local) * 0.65f),
                                tint.copy(alpha = (1f - local) * 0.80f * stormFlicker)
                            ),
                            center = center
                        ),
                        radius = ringR,
                        center = center,
                        style = Stroke(width = (5.5f - wave * 0.7f).coerceAtLeast(1.6f))
                    )
                }

                val flash = ((eruptPhase - 0.02f) / 0.16f).coerceIn(0f, 1f)
                if (flash > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (1f - flash) * 0.62f),
                                colors.PrimaryBright.copy(alpha = (1f - flash) * 0.40f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxR * 0.48f
                        ),
                        radius = maxR * 0.48f,
                        center = center
                    )
                }

                for (spark in 0 until splashLoopCount(20)) {
                    val delay = (spark % 5) * 0.04f
                    val local = ((eruptPhase - delay) / 0.85f).coerceIn(0f, 1f)
                    if (local <= 0f) continue
                    val angle = (spark * 18f + progress * 200f) * PI.toFloat() / 180f
                    val dist = maxR * (0.12f + local * 0.78f)
                    val sparkPos = center + Offset(cos(angle), sin(angle)) * dist
                    val tint = if (spark % 2 == 0) colors.PrimaryBright else colors.Teal
                    drawCircle(
                        color = tint.copy(alpha = (1f - local) * 0.92f),
                        radius = (3.8f - local * 2f).coerceAtLeast(1.2f),
                        center = sparkPos
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawSplashStormStaticField(
    center: Offset,
    extent: Float,
    phase: Float,
    progress: Float,
    tint: Color
) {
    for (line in 0 until splashLoopCount(8)) {
        val y = center.y - extent + ((progress * 2.2f + line * 0.12f) % 1f) * extent * 2f
        drawLine(
            color = tint.copy(alpha = 0.07f * phase),
            start = Offset(center.x - extent, y),
            end = Offset(center.x + extent, y),
            strokeWidth = 1f
        )
    }
    listOf(
        Offset(-0.95f, -0.55f), Offset(0.92f, -0.48f), Offset(-0.88f, 0.62f)
    ).forEachIndexed { i, frac ->
        val flick = splashSmoothStep(((phase - i * 0.12f) / 0.70f).coerceIn(0f, 1f))
        if (flick <= 0f) return@forEachIndexed
        val pos = center + Offset(frac.x * extent, frac.y * extent)
        drawLine(
            color = Color.White.copy(alpha = flick * 0.35f),
            start = pos + Offset(-extent * 0.08f, 0f),
            end = pos + Offset(extent * 0.08f, 0f),
            strokeWidth = 1.4f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSplashStormIonBand(
    center: Offset,
    maxRadius: Float,
    band: Int,
    phase: Float,
    progress: Float,
    eruptPhase: Float,
    primary: Color,
    accent: Color,
    flicker: Float
) {
    val delay = band * 0.12f
    val local = splashSmoothStep(((phase - delay) / 0.78f).coerceIn(0f, 1f))
    if (local <= 0f) return
    val fade = (local * (1f - eruptPhase * 0.45f)).coerceIn(0f, 1f)
    val spin = progress * (95f + band * 35f) * if (band % 2 == 0) 1f else -1f
    rotate(spin, pivot = center) {
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    primary.copy(alpha = 0.08f * fade),
                    accent.copy(alpha = 0.55f * fade * flicker),
                    Color.White.copy(alpha = 0.35f * fade),
                    primary.copy(alpha = 0.08f * fade)
                ),
                center = center
            ),
            startAngle = band * 42f,
            sweepAngle = 118f,
            useCenter = false,
            topLeft = Offset(center.x - maxRadius * (0.72f + band * 0.08f), center.y - maxRadius * (0.72f + band * 0.08f)),
            size = Size(maxRadius * (1.44f + band * 0.16f), maxRadius * (1.44f + band * 0.16f)),
            style = Stroke(width = (4.2f - band * 0.5f).coerceAtLeast(1.8f), cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawSplashStormLightning(
    start: Offset,
    end: Offset,
    phase: Float,
    seed: Int,
    primary: Color,
    accent: Color,
    flicker: Float
) {
    val path = Path()
    path.moveTo(start.x, start.y)
    val segments = 9
    for (i in 1 until segments) {
        val t = i / segments.toFloat() * phase
        val base = start + (end - start) * t
        val dx = end.x - start.x
        val dy = end.y - start.y
        val len = kotlin.math.hypot(dx, dy).coerceAtLeast(1f)
        val jitter = sin(seed + i * 4.7f) * len * 0.09f * (1f - t * 0.35f)
        val perpX = -dy / len * jitter
        val perpY = dx / len * jitter
        path.lineTo(base.x + perpX, base.y + perpY)
    }
    path.lineTo(end.x, end.y)

    drawPath(
        path,
        color = accent.copy(alpha = 0.35f * phase),
        style = Stroke(width = 7f, cap = StrokeCap.Round)
    )
    drawPath(
        path,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f * phase * flicker),
                primary.copy(alpha = 0.85f * phase),
                accent.copy(alpha = 0.55f * phase)
            ),
            start = start,
            end = end
        ),
        style = Stroke(width = 2.4f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawSplashStormEye(
    center: Offset,
    maxRadius: Float,
    phase: Float,
    progress: Float,
    eruptPhase: Float,
    primary: Color,
    accent: Color,
    flicker: Float
) {
    for (spiral in 0 until 3) {
        val delay = spiral * 0.14f
        val local = splashSmoothStep(((phase - delay) / 0.72f).coerceIn(0f, 1f))
        if (local <= 0f) continue
        val spin = progress * (160f + spiral * 40f) * if (spiral % 2 == 0) 1f else -1f
        rotate(spin, pivot = center) {
            val armPath = Path()
            var started = false
            for (step in 0..48) {
                val t = (step / 48f) * local
                val angle = (t * 540f + spiral * 40f) * PI.toFloat() / 180f
                val radius = maxRadius * (0.82f - t * 0.58f)
                val point = center + Offset(cos(angle), sin(angle)) * radius
                if (!started) {
                    armPath.moveTo(point.x, point.y)
                    started = true
                } else {
                    armPath.lineTo(point.x, point.y)
                }
            }
            if (started) {
                drawPath(
                    armPath,
                    color = accent.copy(alpha = (1f - eruptPhase * 0.5f) * 0.45f * local * flicker),
                    style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                )
            }
        }
    }

    for (ring in 0 until 6) {
        val delay = ring * 0.08f
        val local = splashSmoothStep(((phase - delay) / 0.75f).coerceIn(0f, 1f))
        if (local <= 0f) continue
        val collapse = 1f - eruptPhase * 0.55f
        val ringR = maxRadius * (0.68f - ring * 0.09f) * local * collapse
        val tint = if (ring % 2 == 0) primary else accent
        drawCircle(
            color = tint.copy(alpha = (1f - local * 0.35f) * 0.65f * flicker),
            radius = ringR.coerceAtLeast(4f),
            center = center,
            style = Stroke(width = (3.6f - ring * 0.4f).coerceAtLeast(1.4f))
        )
    }

    if (phase > 0.45f) {
        val core = splashSmoothStep(((phase - 0.45f) / 0.55f).coerceIn(0f, 1f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = core * 0.55f * flicker),
                    primary.copy(alpha = core * 0.42f),
                    accent.copy(alpha = core * 0.22f),
                    Color.Transparent
                ),
                center = center,
                radius = maxRadius * 0.22f
            ),
            radius = maxRadius * 0.22f,
            center = center
        )
    }
}

@Composable
private fun VaultBloomSplashEffect(progress: Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    val petalPink = Color(0xFFFF6B9D)
    val petalRose = Color(0xFFE84393)
    val petalMagenta = Color(0xFFD63384)
    val stamenGold = Color(0xFFFFD54F)
    val leafGreen = Color(0xFF43A047)
    val leafDark = Color(0xFF2E7D32)
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.46f
            val h = size.height
            val mistPhase = splashSmoothStep((progress / 0.18f).coerceIn(0f, 1f))
            val lightPhase = splashSmoothStep(((progress - 0.04f) / 0.26f).coerceIn(0f, 1f))
            val stemPhase = splashSmoothStep(((progress - 0.08f) / 0.30f).coerceIn(0f, 1f))
            val bloomPhase = splashSmoothStep(((progress - 0.22f) / 0.38f).coerceIn(0f, 1f))
            val corePhase = splashSmoothStep(((progress - 0.38f) / 0.28f).coerceIn(0f, 1f))
            val pollenPhase = splashSmoothStep(((progress - 0.48f) / 0.32f).coerceIn(0f, 1f))
            val buzzPhase = splashSmoothStep(((progress - 0.56f) / 0.38f).coerceIn(0f, 1f))
            val petalShimmer = 0.88f + 0.12f * sin(progress * PI.toFloat() * 6f)

            drawSplashBloomSkyWash(
                center = center,
                height = h,
                maxRadius = maxR,
                mistPhase = mistPhase,
                lightPhase = lightPhase,
                accent = colors.Teal
            )

            if (mistPhase > 0f) {
                drawSplashBloomMist(
                    center = center,
                    spread = maxR * 1.2f,
                    phase = mistPhase,
                    progress = progress
                )
            }

            if (stemPhase > 0f) {
                drawSplashBloomStem(
                    center = center,
                    maxRadius = maxR,
                    phase = stemPhase,
                    leafGreen = leafGreen,
                    leafDark = leafDark
                )
            }

            if (bloomPhase > 0f) {
                for (i in 0 until splashLoopCount(8)) {
                    val delay = i * 0.07f
                    val local = splashSmoothStep(((bloomPhase - delay) / 0.78f).coerceIn(0f, 1f))
                    if (local <= 0f) continue
                    drawSplashBloomPetal(
                        center = center,
                        maxRadius = maxR,
                        index = i,
                        count = 8,
                        open = local,
                        layer = 0,
                        tint = if (i % 2 == 0) petalPink else petalRose,
                        shimmer = petalShimmer
                    )
                }
                for (i in 0 until 5) {
                    val delay = 0.22f + i * 0.08f
                    val local = splashSmoothStep(((bloomPhase - delay) / 0.72f).coerceIn(0f, 1f))
                    if (local <= 0f) continue
                    drawSplashBloomPetal(
                        center = center,
                        maxRadius = maxR,
                        index = i,
                        count = 5,
                        open = local,
                        layer = 1,
                        tint = petalMagenta,
                        shimmer = petalShimmer
                    )
                }
            }

            if (corePhase > 0f) {
                drawSplashBloomStamen(
                    center = center,
                    maxRadius = maxR,
                    phase = corePhase,
                    stamenGold = stamenGold,
                    accent = colors.PrimaryBright,
                    flicker = petalShimmer
                )
            }

            if (pollenPhase > 0f) {
                drawSplashBloomPollenBurst(
                    center = center,
                    maxRadius = maxR,
                    phase = pollenPhase,
                    progress = progress,
                    stamenGold = stamenGold,
                    accent = colors.Teal
                )
            }

            if (buzzPhase > 0f) {
                drawSplashBloomBuzzTrail(
                    center = center,
                    maxRadius = maxR,
                    phase = buzzPhase,
                    progress = progress,
                    stamenGold = stamenGold,
                    accent = colors.PrimaryBright
                )
            }
        }
    }
}

private fun DrawScope.drawSplashBloomSkyWash(
    center: Offset,
    height: Float,
    maxRadius: Float,
    mistPhase: Float,
    lightPhase: Float,
    accent: Color
) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                accent.copy(alpha = 0.10f * lightPhase),
                Color(0xFFFFF8E1).copy(alpha = 0.06f * lightPhase),
                Color.Transparent
            ),
            startY = 0f,
            endY = center.y + maxRadius * 0.2f
        )
    )
    for (ray in 0 until 5) {
        val rayAlpha = lightPhase * (0.06f + ray * 0.015f)
        val xOff = (ray - 2) * maxRadius * 0.22f
        drawLine(
            color = Color(0xFFFFF8E1).copy(alpha = rayAlpha),
            start = Offset(center.x + xOff, center.y - maxRadius * 0.95f),
            end = Offset(center.x + xOff * 0.3f, center.y + maxRadius * 0.15f),
            strokeWidth = maxRadius * 0.08f,
            cap = StrokeCap.Round
        )
    }
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF1B5E20).copy(alpha = 0.08f * mistPhase),
                Color(0xFF2E7D32).copy(alpha = 0.14f * mistPhase)
            ),
            startY = center.y + maxRadius * 0.35f,
            endY = height
        )
    )
}

private fun DrawScope.drawSplashBloomMist(center: Offset, spread: Float, phase: Float, progress: Float) {
    for (puff in 0 until 7) {
        val drift = sin(progress * PI.toFloat() * 2f + puff) * spread * 0.04f
        val x = center.x + (puff - 3) * spread * 0.28f + drift
        val y = center.y + spread * 0.42f - phase * spread * 0.08f
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.18f * phase),
                    Color.White.copy(alpha = 0.06f * phase),
                    Color.Transparent
                ),
                center = Offset(x, y),
                radius = spread * 0.22f
            ),
            topLeft = Offset(x - spread * 0.28f, y - spread * 0.08f),
            size = Size(spread * 0.56f, spread * 0.16f)
        )
    }
}

private fun DrawScope.drawSplashBloomStem(
    center: Offset,
    maxRadius: Float,
    phase: Float,
    leafGreen: Color,
    leafDark: Color
) {
    val stemTop = center + Offset(0f, maxRadius * 0.08f)
    val stemBottom = center + Offset(0f, maxRadius * 0.98f)
    val stemEnd = stemTop + (stemBottom - stemTop) * phase
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(leafGreen, leafDark),
            start = stemTop,
            end = stemBottom
        ),
        start = stemTop,
        end = stemEnd,
        strokeWidth = 5.5f,
        cap = StrokeCap.Round
    )
    if (phase > 0.35f) {
        val leafOpen = splashSmoothStep(((phase - 0.35f) / 0.55f).coerceIn(0f, 1f))
        listOf(-1f, 1f).forEach { side ->
            val leafCenter = stemTop + Offset(side * maxRadius * 0.18f * leafOpen, maxRadius * 0.22f * phase)
            drawOval(
                color = leafGreen.copy(alpha = 0.92f * leafOpen),
                topLeft = Offset(leafCenter.x - maxRadius * 0.14f, leafCenter.y - maxRadius * 0.05f),
                size = Size(maxRadius * 0.28f * leafOpen, maxRadius * 0.10f)
            )
        }
    }
}

private fun DrawScope.drawSplashBloomPetal(
    center: Offset,
    maxRadius: Float,
    index: Int,
    count: Int,
    open: Float,
    layer: Int,
    tint: Color,
    shimmer: Float
) {
    val baseAngle = index * (360f / count) - 90f
    val curl = (1f - open) * 42f * (if (index % 2 == 0) 1f else -1f)
    val petalAngle = baseAngle + curl
    val scale = if (layer == 0) 1f else 0.62f
    val petalLen = maxRadius * (0.26f + open * 0.24f) * scale
    rotate(petalAngle, pivot = center) {
        val petalPath = Path().apply {
            moveTo(center.x, center.y)
            cubicTo(
                center.x + maxRadius * 0.11f * scale, center.y - petalLen * 0.38f,
                center.x + maxRadius * 0.16f * scale, center.y - petalLen * 0.78f,
                center.x, center.y - petalLen
            )
            cubicTo(
                center.x - maxRadius * 0.16f * scale, center.y - petalLen * 0.78f,
                center.x - maxRadius * 0.11f * scale, center.y - petalLen * 0.38f,
                center.x, center.y
            )
            close()
        }
        drawPath(
            petalPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    tint.copy(alpha = 0.98f * open * shimmer),
                    Color.White.copy(alpha = 0.35f * open),
                    tint.copy(alpha = 0.85f * open)
                ),
                start = center,
                end = center + Offset(0f, -petalLen)
            )
        )
        drawPath(
            petalPath,
            color = Color.White.copy(alpha = 0.12f * open),
            style = Stroke(width = 1f)
        )
    }
}

private fun DrawScope.drawSplashBloomStamen(
    center: Offset,
    maxRadius: Float,
    phase: Float,
    stamenGold: Color,
    accent: Color,
    flicker: Float
) {
    for (i in 0 until splashLoopCount(8)) {
        val filament = splashSmoothStep(((phase - i * 0.05f) / 0.75f).coerceIn(0f, 1f))
        if (filament <= 0f) continue
        rotate(i * 45f, pivot = center) {
            drawLine(
                color = stamenGold.copy(alpha = 0.85f * filament),
                start = center,
                end = center + Offset(0f, -maxRadius * 0.14f * filament),
                strokeWidth = 1.6f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = accent.copy(alpha = 0.90f * filament * flicker),
                radius = 2.4f,
                center = center + Offset(0f, -maxRadius * 0.14f * filament)
            )
        }
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.75f * phase * flicker),
                stamenGold.copy(alpha = 0.95f * phase),
                Color(0xFFFF8F00).copy(alpha = 0.85f * phase)
            ),
            center = center,
            radius = maxRadius * 0.13f
        ),
        radius = maxRadius * 0.13f * phase,
        center = center
    )
}

private fun DrawScope.drawSplashBloomPollenBurst(
    center: Offset,
    maxRadius: Float,
    phase: Float,
    progress: Float,
    stamenGold: Color,
    accent: Color
) {
    for (ring in 0 until 3) {
        val delay = ring * 0.14f
        val local = splashSmoothStep(((phase - delay) / 0.72f).coerceIn(0f, 1f))
        if (local <= 0f) continue
        drawCircle(
            color = stamenGold.copy(alpha = (1f - local) * 0.45f),
            radius = maxRadius * (0.10f + local * (0.28f + ring * 0.10f)),
            center = center,
            style = Stroke(width = (2.4f - ring * 0.5f).coerceAtLeast(1f))
        )
    }
    for (mote in 0 until splashLoopCount(24)) {
        val delay = (mote % 6) * 0.04f
        val local = ((phase - delay) / 0.85f).coerceIn(0f, 1f)
        if (local <= 0f) continue
        val angle = (mote * 15f + progress * 120f) * PI.toFloat() / 180f
        val dist = maxRadius * (0.12f + local * 0.72f)
        val pos = center + Offset(cos(angle), sin(angle)) * dist
        val tint = if (mote % 3 == 0) accent else stamenGold
        drawCircle(
            color = tint.copy(alpha = (1f - local) * 0.85f),
            radius = (2.8f - local * 1.2f).coerceAtLeast(1f),
            center = pos
        )
    }
}

private fun DrawScope.drawSplashBloomBuzzTrail(
    center: Offset,
    maxRadius: Float,
    phase: Float,
    progress: Float,
    stamenGold: Color,
    accent: Color
) {
    val spiralPath = Path()
    var started = false
    for (step in 0..36) {
        val t = (step / 36f) * phase
        val angle = (t * 720f + progress * 60f) * PI.toFloat() / 180f
        val radius = maxRadius * (0.08f + t * 0.42f)
        val point = center + Offset(cos(angle), sin(angle)) * radius
        if (!started) {
            spiralPath.moveTo(point.x, point.y)
            started = true
        } else {
            spiralPath.lineTo(point.x, point.y)
        }
    }
    if (started) {
        drawPath(
            spiralPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    stamenGold.copy(alpha = 0.15f * phase),
                    accent.copy(alpha = 0.55f * phase),
                    Color.White.copy(alpha = 0.45f * phase)
                ),
                start = center,
                end = center + Offset(maxRadius * 0.3f, -maxRadius * 0.3f)
            ),
            style = Stroke(width = 3.2f, cap = StrokeCap.Round)
        )
    }
    for (streak in 0 until splashLoopCount(8)) {
        val local = ((phase - streak * 0.06f) / 0.80f).coerceIn(0f, 1f)
        if (local <= 0f) continue
        val angle = (streak * 45f + progress * 140f) * PI.toFloat() / 180f
        val dist = maxRadius * (0.18f + local * 0.55f)
        val pos = center + Offset(cos(angle), sin(angle)) * dist
        drawLine(
            color = Color.White.copy(alpha = (1f - local) * 0.50f),
            start = pos,
            end = pos + Offset(cos(angle), sin(angle)) * maxRadius * 0.07f,
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSplashHalloweenCobweb(origin: Offset, span: Float, alpha: Float, mirror: Boolean) {
    val baseAngle = if (mirror) 135f else 45f
    for (spoke in 0 until splashLoopCount(8)) {
        val angle = (baseAngle + spoke * 13f) * PI.toFloat() / 180f
        drawLine(
            color = Color.White.copy(alpha = alpha * (0.5f + spoke * 0.06f)),
            start = origin,
            end = origin + Offset(cos(angle), sin(angle)) * span,
            strokeWidth = 1.1f,
            cap = StrokeCap.Round
        )
    }
    for (ring in 1..5) {
        val arcSpan = span * (ring / 5f)
        drawArc(
            color = Color.White.copy(alpha = alpha * 0.38f),
            startAngle = if (mirror) 90f else 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(origin.x - arcSpan, origin.y - arcSpan),
            size = Size(arcSpan * 2f, arcSpan * 2f),
            style = Stroke(width = 0.85f)
        )
    }
}

private fun DrawScope.drawSplashHalloweenBat(pos: Offset, wingFlap: Float, scale: Float, alpha: Float) {
    drawLine(color = Color(0xFF1A0A04).copy(alpha = alpha), start = pos, end = pos + Offset(-scale * 0.95f, -wingFlap), strokeWidth = 2.6f, cap = StrokeCap.Round)
    drawLine(color = Color(0xFF1A0A04).copy(alpha = alpha), start = pos, end = pos + Offset(scale * 0.95f, -wingFlap), strokeWidth = 2.6f, cap = StrokeCap.Round)
}

private fun DrawScope.drawSplashHalloweenGhost(center: Offset, size: Float, alpha: Float) {
    val w = size * 0.52f
    val h = size * 0.68f
    val top = center.y - h * 0.42f
    drawRoundRect(color = Color.White.copy(alpha = alpha * 0.92f), topLeft = Offset(center.x - w * 0.5f, top), size = Size(w, h * 0.58f), cornerRadius = CornerRadius(w * 0.5f))
}

private fun DrawScope.drawSplashHalloweenTombstone(topLeft: Offset, width: Float, height: Float, alpha: Float) {
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF6E6E6E).copy(alpha = alpha), Color(0xFF222222).copy(alpha = alpha))),
        topLeft = topLeft,
        size = Size(width, height),
        cornerRadius = CornerRadius(width * 0.12f)
    )
}

/** Witch-hat jack-o'-lantern — mouth glow ONLY in smile cavity; no center inferno blob. */
private fun DrawScope.drawSplashHalloweenJackPortal(
    center: Offset,
    radius: Float,
    portalPhase: Float,
    innerPhase: Float,
    flicker: Float
) {
    val alpha = portalPhase
    val hatTop = center.y - radius * 1.08f
    val hatBrimY = center.y - radius * 0.82f
    val hatPath = Path().apply {
        moveTo(center.x, hatTop - radius * 0.35f)
        lineTo(center.x - radius * 0.38f, hatBrimY)
        lineTo(center.x + radius * 0.38f, hatBrimY)
        close()
    }
    drawPath(hatPath, brush = Brush.linearGradient(listOf(Color(0xFF1A0A28), Color(0xFF2D1B4E)), start = Offset(center.x, hatTop), end = Offset(center.x, hatBrimY)))
    drawOval(color = Color(0xFF1A0A28).copy(alpha = alpha * 0.95f), topLeft = Offset(center.x - radius * 0.52f, hatBrimY - radius * 0.06f), size = Size(radius * 1.04f, radius * 0.12f))
    drawOval(
        brush = Brush.radialGradient(listOf(Color(0xFFFFB74D), Color(0xFFFF7A1A), Color(0xFFBF4F00)), center = center - Offset(radius * 0.14f, radius * 0.11f), radius = radius * 1.05f),
        topLeft = Offset(center.x - radius, center.y - radius * 0.86f),
        size = Size(radius * 2f, radius * 1.78f)
    )
    listOf(-1f, 1f).forEach { side ->
        val ex = center.x + side * radius * 0.27f
        val ey = center.y - radius * 0.17f
        val ew = radius * 0.21f
        val eye = Path().apply {
            moveTo(ex, ey - ew * 0.58f)
            lineTo(ex - side * ew * 0.58f, ey + ew * 0.48f)
            lineTo(ex + side * ew * 0.58f, ey + ew * 0.48f)
            close()
        }
        drawPath(eye, color = Color(0xFFFFF176).copy(alpha = alpha * flicker))
    }
    val noseY = center.y + radius * 0.02f
    val noseW = radius * 0.09f
    drawPath(
        Path().apply {
            moveTo(center.x, noseY - noseW)
            lineTo(center.x - noseW, noseY + noseW * 0.8f)
            lineTo(center.x + noseW, noseY + noseW * 0.8f)
            close()
        },
        color = Color(0xFFFF9100).copy(alpha = alpha * flicker * 0.9f)
    )
    val mouthY = center.y + radius * 0.2f
    val mouthOpen = (portalPhase * 0.55f + innerPhase * 0.45f) * radius * 0.42f
    drawArc(
        color = Color(0xFF1A0A04).copy(alpha = alpha * 0.85f),
        startAngle = 10f,
        sweepAngle = 160f,
        useCenter = false,
        topLeft = Offset(center.x - mouthOpen, mouthY - mouthOpen * 0.38f),
        size = Size(mouthOpen * 2f, mouthOpen * 0.88f),
        style = Stroke(width = radius * 0.14f, cap = StrokeCap.Round)
    )
    if (innerPhase > 0.05f) {
        drawArc(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF176).copy(alpha = innerPhase * flicker * 0.9f),
                    Color(0xFFFF9100).copy(alpha = innerPhase * flicker * 0.55f),
                    Color.Transparent
                ),
                center = Offset(center.x, mouthY + mouthOpen * 0.12f),
                radius = mouthOpen * 0.85f
            ),
            startAngle = 10f,
            sweepAngle = 160f,
            useCenter = true,
            topLeft = Offset(center.x - mouthOpen, mouthY - mouthOpen * 0.2f),
            size = Size(mouthOpen * 2f, mouthOpen * 0.75f)
        )
    }
}

@Composable
private fun HalloweenSplashEffect(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.46f
            val w = size.width
            val h = size.height
            val skyPhase = splashSmoothStep((progress / 0.20f).coerceIn(0f, 1f))
            val framePhase = splashSmoothStep(((progress - 0.08f) / 0.30f).coerceIn(0f, 1f))
            val fogPhase = splashSmoothStep(((progress - 0.06f) / 0.36f).coerceIn(0f, 1f))
            val ringPhase = splashSmoothStep(((progress - 0.14f) / 0.50f).coerceIn(0f, 1f))
            val portalPhase = splashSmoothStep(((progress - 0.12f) / 0.38f).coerceIn(0f, 1f))
            val innerPhase = splashSmoothStep(((progress - 0.30f) / 0.42f).coerceIn(0f, 1f))
            val burstPhase = splashSmoothStep(((progress - 0.44f) / 0.40f).coerceIn(0f, 1f))
            val flicker = 0.74f + 0.26f * sin(progress * PI.toFloat() * 5.2f)

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF120A1E).copy(alpha = 0.85f * skyPhase), Color(0xFF0A0610).copy(alpha = 0.68f), Color.Transparent),
                    startY = 0f,
                    endY = h * 0.92f
                )
            )
            val moonCenter = Offset(w * 0.20f, h * 0.11f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFFF8E1).copy(alpha = skyPhase), Color(0xFFFFB74D).copy(alpha = skyPhase)),
                    center = moonCenter,
                    radius = maxR * 0.12f
                ),
                radius = maxR * 0.12f,
                center = moonCenter
            )

            if (framePhase > 0f) {
                val webAlpha = framePhase * 0.65f
                drawSplashHalloweenCobweb(Offset(w * 0.05f, h * 0.07f), maxR * 0.42f, webAlpha, false)
                drawSplashHalloweenCobweb(Offset(w * 0.95f, h * 0.07f), maxR * 0.42f, webAlpha, true)
                drawSplashHalloweenCobweb(Offset(w * 0.05f, h * 0.93f), maxR * 0.32f, webAlpha * 0.75f, false)
                drawSplashHalloweenCobweb(Offset(w * 0.95f, h * 0.93f), maxR * 0.32f, webAlpha * 0.75f, true)
            }

            for (f in 0 until 6) {
                val fx = w * (0.04f + f * 0.18f)
                val fy = h * 0.92f - fogPhase * h * 0.32f
                drawOval(
                    brush = Brush.radialGradient(listOf(Color(0xFF9B30FF).copy(alpha = 0.28f * fogPhase), Color.Transparent), center = Offset(fx, fy), radius = maxR * 0.38f),
                    topLeft = Offset(fx - maxR * 0.38f, fy - maxR * 0.12f),
                    size = Size(maxR * 0.76f, maxR * 0.24f)
                )
            }

            if (framePhase > 0.2f) {
                val stoneAlpha = ((framePhase - 0.2f) / 0.7f).coerceIn(0f, 1f)
                listOf(-0.88f, -0.44f, 0f, 0.44f, 0.88f).forEachIndexed { i, xFrac ->
                    val tw = maxR * (0.18f + (i % 2) * 0.04f)
                    val th = maxR * (0.15f + (i % 3) * 0.04f)
                    drawSplashHalloweenTombstone(Offset(center.x + maxR * xFrac - tw * 0.5f, h * 0.84f - th), tw, th, stoneAlpha)
                }
            }

            if (portalPhase > 0f) {
                rotate(progress * 115f, pivot = center) {
                    val candy = listOf(Color.White, Color(0xFFFF9100), Color(0xFFFFEB3B))
                    for (seg in 0 until splashLoopCount(14)) {
                        drawArc(
                            color = candy[seg % 3].copy(alpha = portalPhase * 0.92f),
                            startAngle = seg * (360f / 14f),
                            sweepAngle = 360f / 14f,
                            useCenter = false,
                            topLeft = Offset(center.x - maxR * 1.08f, center.y - maxR * 1.08f),
                            size = Size(maxR * 2.16f, maxR * 2.16f),
                            style = Stroke(width = maxR * 0.072f)
                        )
                    }
                }
            }

            if (ringPhase > 0f) {
                for (ring in 0 until splashLoopCount(8)) {
                    val delay = ring * 0.065f
                    val local = ((ringPhase - delay) / 0.78f).coerceIn(0f, 1f)
                    if (local <= 0f) continue
                    val ringR = maxR * 0.58f * splashSmoothStep(local)
                    val ringAlpha = (1f - local) * 0.75f * flicker
                    val tint = if (ring % 2 == 0) Color(0xFFFF9100) else Color(0xFF9B30FF)
                    drawCircle(
                        brush = Brush.sweepGradient(listOf(tint.copy(alpha = ringAlpha), Color.White.copy(alpha = ringAlpha * 0.8f), tint.copy(alpha = ringAlpha)), center = center),
                        radius = ringR,
                        center = center,
                        style = Stroke(width = (4.8f - ring * 0.38f).coerceAtLeast(1.6f))
                    )
                }
            }

            if (portalPhase > 0.05f) {
                drawSplashHalloweenJackPortal(center, maxR * (0.70f + portalPhase * 0.1f), portalPhase, innerPhase, flicker)
            }

            if (portalPhase > 0.10f) {
                for (b in 0 until splashLoopCount(10)) {
                    val local = ((portalPhase - 0.10f - b * 0.035f) / 0.62f).coerceIn(0f, 1f)
                    if (local <= 0f) continue
                    val angle = (b * 36f + progress * 220f) * PI.toFloat() / 180f
                    val batPos = center + Offset(cos(angle), sin(angle)) * maxR * (1.08f - local * 0.78f)
                    drawSplashHalloweenBat(batPos, sin(local * PI.toFloat() * 18f + b) * maxR * 0.05f, maxR * 0.05f, (1f - local * 0.5f).coerceIn(0.3f, 1f))
                }
            }

            for (g in 0 until 3) {
                val local = splashSmoothStep(((progress - 0.22f - g * 0.07f) / 0.48f).coerceIn(0f, 1f))
                if (local <= 0f) continue
                drawSplashHalloweenGhost(Offset(center.x + maxR * (-0.44f + g * 0.44f), h * 0.9f - local * h * 0.52f), maxR * 0.28f, (1f - local * 0.5f).coerceIn(0.25f, 0.75f))
            }

            if (burstPhase > 0f) {
                for (ring in 0 until 3) {
                    val ringLocal = ((burstPhase - ring * 0.14f) / 0.72f).coerceIn(0f, 1f)
                    if (ringLocal <= 0f) continue
                    drawCircle(
                        color = (if (ring == 0) Color(0xFFFF9100) else Color(0xFF9B30FF)).copy(alpha = (1f - ringLocal) * 0.65f),
                        radius = maxR * 0.52f * ringLocal,
                        center = center,
                        style = Stroke(width = 4f - ringLocal * 2.5f)
                    )
                }
            }

            if (progress in 0.34f..0.54f) {
                val flash = sin(((progress - 0.34f) / 0.20f) * PI.toFloat())
                if (flash > 0.25f) drawRect(color = Color(0xFFE1BEE7).copy(alpha = flash * 0.14f))
            }
        }
    }
}

private fun splashFivePointStarPath(center: Offset, outerR: Float, innerR: Float, rotationDeg: Float = -90f) = Path().apply {
    for (p in 0 until 5) {
        val outerA = ((p * 72f + rotationDeg) * PI / 180.0).toFloat()
        val innerA = (((p * 72f + 36f) + rotationDeg) * PI / 180.0).toFloat()
        val outer = center + Offset(cos(outerA) * outerR, sin(outerA) * outerR)
        val inner = center + Offset(cos(innerA) * innerR, sin(innerA) * innerR)
        if (p == 0) moveTo(outer.x, outer.y) else lineTo(outer.x, outer.y)
        lineTo(inner.x, inner.y)
    }
    close()
}

private fun DrawScope.drawSplashChristmasFrostCorner(origin: Offset, span: Float, alpha: Float, mirror: Boolean) {
    val dir = if (mirror) -1f else 1f
    for (icicle in 0 until 5) {
        val ix = origin.x + dir * icicle * span * 0.19f
        val len = span * (0.16f + (icicle % 3) * 0.07f)
        drawLine(color = Color.White.copy(alpha = alpha * 0.8f), start = Offset(ix, origin.y), end = Offset(ix + dir * span * 0.025f, origin.y + len), strokeWidth = 2f, cap = StrokeCap.Round)
    }
}

/** Compact snowflake glyph — drawn at a point, not radiating from screen center. */
private fun DrawScope.drawSplashChristmasSnowflakeGlyph(center: Offset, size: Float, alpha: Float, rotationDeg: Float) {
    rotate(rotationDeg, pivot = center) {
        for (arm in 0 until 6) {
            rotate(arm * 60f, pivot = center) {
                drawLine(color = Color.White.copy(alpha = alpha), start = center, end = center + Offset(0f, -size), strokeWidth = 2.2f, cap = StrokeCap.Round)
                val mid = center + Offset(0f, -size * 0.55f)
                drawLine(color = Color(0xFF4DD0E1).copy(alpha = alpha * 0.85f), start = mid + Offset(-size * 0.22f, 0f), end = mid + Offset(size * 0.22f, 0f), strokeWidth = 1.6f, cap = StrokeCap.Round)
            }
        }
        drawCircle(color = Color.White.copy(alpha = alpha), radius = size * 0.12f, center = center)
    }
}

/** Glossy red ornament — NO inner orange/gold portal circle filling center. */
private fun DrawScope.drawSplashChristmasOrnamentPortal(center: Offset, radius: Float, portalPhase: Float, flicker: Float) {
    val capTop = center.y - radius * 1.02f
    drawRoundRect(
        brush = Brush.linearGradient(listOf(Color(0xFFFFF176), Color(0xFFFFB300)), start = Offset(center.x - radius * 0.22f, capTop), end = Offset(center.x + radius * 0.22f, capTop + radius * 0.18f)),
        topLeft = Offset(center.x - radius * 0.22f, capTop),
        size = Size(radius * 0.44f, radius * 0.18f),
        cornerRadius = CornerRadius(radius * 0.06f)
    )
    drawLine(color = Color(0xFF8D6E63).copy(alpha = portalPhase * 0.65f), start = Offset(center.x, capTop + radius * 0.18f), end = Offset(center.x, center.y - radius * 0.88f), strokeWidth = 2.2f, cap = StrokeCap.Round)
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFFFF6B7F), Color(0xFFE0233D), Color(0xFF5A0610)), center = center - Offset(radius * 0.18f, radius * 0.14f), radius = radius * 1.05f),
        radius = radius,
        center = center
    )
    drawArc(color = Color.White.copy(alpha = portalPhase * flicker * 0.55f), startAngle = 210f, sweepAngle = 55f, useCenter = false, topLeft = Offset(center.x - radius * 0.85f, center.y - radius * 0.85f), size = Size(radius * 1.7f, radius * 1.7f), style = Stroke(width = radius * 0.1f, cap = StrokeCap.Round))
}

private fun DrawScope.drawSplashChristmasNorthStar(center: Offset, outerR: Float, rayPhase: Float, alpha: Float) {
    for (ray in 0 until splashLoopCount(8)) {
        val rayLen = outerR * (0.6f + rayPhase * 0.85f)
        val angle = (ray * 45f - 90f) * PI.toFloat() / 180f
        drawLine(
            brush = Brush.linearGradient(listOf(Color.White.copy(alpha = alpha * 0.95f), Color(0xFFFFD54F).copy(alpha = alpha * 0.5f), Color.Transparent), start = center, end = center + Offset(cos(angle), sin(angle)) * rayLen),
            start = center,
            end = center + Offset(cos(angle), sin(angle)) * rayLen,
            strokeWidth = if (ray % 2 == 0) 3f else 1.8f,
            cap = StrokeCap.Round
        )
    }
    drawPath(splashFivePointStarPath(center, outerR * 0.48f, outerR * 0.2f), brush = Brush.linearGradient(listOf(Color.White, Color(0xFFFFE082)), start = Offset(center.x, center.y - outerR), end = Offset(center.x, center.y + outerR)))
}

private fun DrawScope.drawSplashChristmasMiniOrnament(center: Offset, radius: Float, tint: Color, alpha: Float, flicker: Float) {
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.White.copy(alpha = alpha * 0.5f), tint.copy(alpha = alpha * flicker)), center = center - Offset(radius * 0.12f, radius * 0.1f), radius = radius),
        radius = radius,
        center = center
    )
}

@Composable
private fun ChristmasSplashEffect(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.46f
            val w = size.width
            val h = size.height
            val skyPhase = splashSmoothStep((progress / 0.20f).coerceIn(0f, 1f))
            val auroraPhase = splashSmoothStep(((progress - 0.06f) / 0.50f).coerceIn(0f, 1f))
            val portalPhase = splashSmoothStep(((progress - 0.12f) / 0.38f).coerceIn(0f, 1f))
            val pillarPhase = splashSmoothStep(((progress - 0.22f) / 0.40f).coerceIn(0f, 1f))
            val ringPhase = splashSmoothStep(((progress - 0.16f) / 0.44f).coerceIn(0f, 1f))
            val burstPhase = splashSmoothStep(((progress - 0.44f) / 0.40f).coerceIn(0f, 1f))
            val flicker = 0.78f + 0.22f * sin(progress * PI.toFloat() * 4.5f)

            drawRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF0A1428).copy(alpha = 0.82f * skyPhase), Color(0xFF061018).copy(alpha = 0.65f), Color.Transparent), startY = 0f, endY = h * 0.9f)
            )

            if (skyPhase > 0.2f) {
                drawSplashChristmasFrostCorner(Offset(w * 0.05f, h * 0.07f), maxR * 0.38f, skyPhase * 0.85f, false)
                drawSplashChristmasFrostCorner(Offset(w * 0.95f, h * 0.07f), maxR * 0.38f, skyPhase * 0.85f, true)
            }

            if (auroraPhase > 0f) {
                rotate(progress * 38f - 90f, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Color.Transparent, Color(0xFF1FA34D).copy(alpha = 0.32f * auroraPhase), Color(0xFF4DD0E1).copy(alpha = 0.42f * auroraPhase), Color.Transparent), center = center),
                        startAngle = 0f, sweepAngle = 360f, useCenter = true,
                        topLeft = Offset(center.x - maxR * 1.2f, center.y - maxR * 1.2f), size = Size(maxR * 2.4f, maxR * 2.4f)
                    )
                }
            }

            val starCenter = Offset(center.x, h * 0.09f)
            if (pillarPhase > 0.02f) {
                drawSplashChristmasNorthStar(starCenter, maxR * 0.14f, pillarPhase, pillarPhase)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = pillarPhase * 0.45f), Color(0xFFFFD54F).copy(alpha = pillarPhase * 0.32f), Color(0xFF4DD0E1).copy(alpha = pillarPhase * 0.18f), Color.Transparent),
                        startY = starCenter.y,
                        endY = center.y + maxR * 0.2f
                    ),
                    topLeft = Offset(center.x - maxR * 0.1f, starCenter.y),
                    size = Size(maxR * 0.2f, center.y - starCenter.y + maxR * 0.2f)
                )
            }

            if (portalPhase > 0f) {
                rotate(progress * 110f, pivot = center) {
                    for (seg in 0 until splashLoopCount(14)) {
                        drawArc(
                            color = (if (seg % 2 == 0) Color(0xFFE0233D) else Color.White).copy(alpha = portalPhase * 0.92f),
                            startAngle = seg * (360f / 14f), sweepAngle = 360f / 14f, useCenter = false,
                            topLeft = Offset(center.x - maxR * 1.08f, center.y - maxR * 1.08f), size = Size(maxR * 2.16f, maxR * 2.16f),
                            style = Stroke(width = maxR * 0.07f)
                        )
                    }
                }
            }

            if (portalPhase > 0.08f) {
                val globeR = maxR * 0.92f
                drawCircle(color = Color.White.copy(alpha = portalPhase * 0.28f), radius = globeR, center = center, style = Stroke(width = 2.5f))
            }

            if (ringPhase > 0.15f) {
                val wreathR = maxR * 0.68f * splashSmoothStep(((ringPhase - 0.15f) / 0.7f).coerceIn(0f, 1f))
                drawCircle(color = Color(0xFF1FA34D).copy(alpha = 0.55f), radius = wreathR, center = center, style = Stroke(width = maxR * 0.038f))
            }

            if (portalPhase > 0.05f) {
                drawSplashChristmasOrnamentPortal(center, maxR * (0.68f + portalPhase * 0.1f), portalPhase, flicker)
            }

            if (portalPhase > 0.15f) {
                val glyphRingR = maxR * (0.78f + portalPhase * 0.08f)
                for (i in 0 until 6) {
                    val angle = (i * 60f + progress * 35f - 90f) * PI.toFloat() / 180f
                    val pos = center + Offset(cos(angle), sin(angle)) * glyphRingR
                    drawSplashChristmasSnowflakeGlyph(pos, maxR * 0.055f, portalPhase * 0.9f, i * 60f + progress * 80f)
                }
            }

            if (portalPhase > 0.2f) {
                val flankAlpha = ((portalPhase - 0.2f) / 0.6f).coerceIn(0f, 1f)
                drawSplashChristmasMiniOrnament(center + Offset(-maxR * 1.05f, maxR * 0.08f), maxR * 0.11f, Color(0xFF1FA34D), flankAlpha, flicker)
                drawSplashChristmasMiniOrnament(center + Offset(maxR * 1.05f, maxR * 0.08f), maxR * 0.11f, Color(0xFF1565C0), flankAlpha, flicker)
            }

            for (layer in 0 until 3) {
                for (s in 0 until splashLoopCount(18)) {
                    val seed = s + layer * 80
                    val local = ((progress - (seed % 16) * 0.008f) / (0.9f + layer * 0.03f)).coerceIn(0f, 1f)
                    if (local <= 0f) continue
                    val sx = (seed * 19.3f + local * (20f + layer * 10f)) % w
                    val sy = -maxR * 0.1f + local * h * 1.05f
                    drawCircle(color = Color.White.copy(alpha = (0.3f + layer * 0.22f) * (1f - local * 0.15f)), radius = 1.1f + layer, center = Offset(sx, sy))
                }
            }

            if (burstPhase > 0f) {
                for (ring in 0 until 3) {
                    val ringLocal = ((burstPhase - ring * 0.14f) / 0.72f).coerceIn(0f, 1f)
                    if (ringLocal <= 0f) continue
                    val ringColor = when (ring) { 0 -> Color(0xFFFFD54F); 1 -> Color(0xFFE0233D); else -> Color(0xFF4DD0E1) }
                    drawCircle(color = ringColor.copy(alpha = (1f - ringLocal) * 0.6f), radius = maxR * 0.5f * ringLocal, center = center, style = Stroke(width = 4f - ringLocal * 2.5f))
                }
            }
        }
    }
}

private fun SplashStyle.previewProgress(): Float = when (this) {
    SplashStyle.NONE -> 0f
    SplashStyle.DEFAULT -> 0f
    SplashStyle.SIGNAL_FORGE -> 0.60f
    SplashStyle.VAULT_DOOR -> 0.60f
    SplashStyle.RADAR -> 0.62f
    SplashStyle.TRIANGULATE -> 0.62f
    SplashStyle.SIGNAL_STORM -> 0.60f
    SplashStyle.VAULT_BLOOM -> 0.60f
    SplashStyle.HALLOWEEN -> 0.58f
    SplashStyle.CHRISTMAS -> 0.60f
}

@Composable
fun SplashStylePicker(
    selectedStyleId: String,
    onStyleSelected: (SplashStyle) -> Unit,
    appIcon: AppIcon,
    modifier: Modifier = Modifier,
    isLocked: (String) -> Boolean = { false },
    onLockedClick: () -> Unit = {}
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    Box(modifier = modifier.fillMaxWidth()) {
        androidx.compose.foundation.lazy.LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(SplashStyle.entries.size, key = { SplashStyle.entries[it].id }) { index ->
                val style = SplashStyle.entries[index]
                val normalizedSelection = SplashStyle.fromId(selectedStyleId).id
                val selected = style.id == normalizedSelection
                val locked = isLocked(style.id)
                Box(modifier = Modifier.width(120.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (locked) 0.45f else 1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) SpotVaultColors.Teal else SpotVaultColors.Outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { if (locked) onLockedClick() else onStyleSelected(style) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp))
                                .background(SpotVaultColors.Void),
                            contentAlignment = Alignment.Center
                        ) {
                            if (style == SplashStyle.NONE) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = SpotVaultColors.Muted.copy(alpha = 0.6f), modifier = Modifier.size(28.dp))
                            } else {
                                val previewProgress = style.previewProgress()
                                if (style != SplashStyle.DEFAULT) {
                                    SplashStyleEffect(style = style, progress = previewProgress, modifier = Modifier.fillMaxSize())
                                }
                                val previewMarkSize = 44.dp
                                val iconModifier = when (style) {
                                    SplashStyle.VAULT_DOOR -> Modifier.size(previewMarkSize).scale(splashVaultDoorIconScale(previewProgress)).rotate(splashVaultDoorIconRotation(previewProgress)).alpha(0.2f + splashVaultDoorIconAlpha(previewProgress) * 0.8f)
                                    SplashStyle.SIGNAL_FORGE -> Modifier.size(previewMarkSize).scale(splashSignalForgeIconScale(previewProgress)).rotate(splashSignalForgeIconRotation(previewProgress)).alpha(0.2f + splashSignalForgeIconAlpha(previewProgress) * 0.8f)
                                    SplashStyle.RADAR -> Modifier.size(previewMarkSize).scale(splashRadarIconScale(previewProgress)).rotate(splashRadarIconRotation(previewProgress)).alpha(0.2f + splashRadarIconAlpha(previewProgress) * 0.8f)
                                    SplashStyle.TRIANGULATE -> Modifier.size(previewMarkSize).scale(splashTriangulateIconScale(previewProgress)).rotate(splashTriangulateIconRotation(previewProgress)).alpha(0.2f + splashTriangulateIconAlpha(previewProgress) * 0.8f)
                                    SplashStyle.SIGNAL_STORM -> Modifier.size(previewMarkSize).scale(splashSignalStormIconScale(previewProgress)).rotate(splashSignalStormIconRotation(previewProgress)).alpha(0.2f + splashSignalStormIconAlpha(previewProgress) * 0.8f)
                                    SplashStyle.VAULT_BLOOM -> Modifier.size(previewMarkSize).scale(splashVaultBloomIconScale(previewProgress)).rotate(splashVaultBloomIconRotation(previewProgress)).alpha(0.2f + splashVaultBloomIconAlpha(previewProgress) * 0.8f)
                                    SplashStyle.HALLOWEEN -> Modifier.size(previewMarkSize).scale(splashHalloweenIconScale(previewProgress)).rotate(splashHalloweenIconRotation(previewProgress)).alpha(0.2f + splashHalloweenIconAlpha(previewProgress) * 0.8f)
                                    SplashStyle.CHRISTMAS -> Modifier.size(previewMarkSize).scale(splashChristmasIconScale(previewProgress)).rotate(splashChristmasIconRotation(previewProgress)).alpha(0.2f + splashChristmasIconAlpha(previewProgress) * 0.8f)
                                    else -> Modifier.size(previewMarkSize)
                                }
                                Box(contentAlignment = Alignment.Center) {
                                    if (style == SplashStyle.VAULT_BLOOM && splashVaultBloomIconAlpha(previewProgress) > 0.01f) {
                                        VaultBloomWings(progress = previewProgress, modifier = Modifier.fillMaxSize())
                                    }
                                    AppIconMark(appIcon = appIcon, modifier = iconModifier)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().background(if (selected) SpotVaultColors.Teal.copy(alpha = 0.14f) else SpotVaultColors.Elevated.copy(alpha = 0.6f)).padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(style.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selected) SpotVaultColors.Teal else SpotVaultColors.OnSurface, maxLines = 1)
                                if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.padding(start = 4.dp).size(14.dp))
                            }
                        }
                    }
                    if (locked) PremiumLockBadge(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
                }
            }
        }
        TrailingScrollHint(listState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

