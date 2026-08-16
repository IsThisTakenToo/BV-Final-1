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
    SIGNAL_FORGE("signal_forge", "Signal Forge", "Molten vault energy spirals inward and forges your mark into being"),
    VAULT_DOOR("vault_door", "Vault Door", "A polished bank vault dial spins and the steel door swings open"),
    RADAR("radar", "Radar Scan", "A high-fidelity radar sweep locks onto your mark"),
    TRIANGULATE("triangulate", "Triangulate", "Satellite pings converge and lock onto your vault mark"),
    BEACON_WAKE("beacon_wake", "Beacon Wake", "Your icon ignites as a beacon and pulses to life"),
    VAULT_BLOOM("vault_bloom", "Vault Bloom", "A flower blooms and your icon buzzes out like a bee into the camera"),
    HALLOWEEN("halloween", "All Hallows' Gate", "The harvest moon rises, séance rings ignite, a witch-hat jack-o'-lantern portal opens, and your mark erupts from the inferno"),
    CHRISTMAS("christmas", "Christmas Eve Gate", "The North Star ignites, a giant ornament portal opens, and your mark arrives in a blizzard of light");

    companion object {
        fun fromId(id: String?): SplashStyle {
            val migrated = when (id) {
                "grid", "pin_drop" -> TRIANGULATE.id
                "icon_surge" -> VAULT_BLOOM.id
                "screen_crack" -> SIGNAL_FORGE.id
                else -> id
            }
            return entries.firstOrNull { it.id == migrated } ?: DEFAULT
        }
    }
}

fun SplashStyle.effectDurationMillis(): Long = when (this) {
    SplashStyle.NONE -> 0L
    SplashStyle.DEFAULT -> 0L
    SplashStyle.SIGNAL_FORGE -> 1200L
    SplashStyle.VAULT_DOOR -> 1100L
    SplashStyle.RADAR -> 950L
    SplashStyle.TRIANGULATE -> 1000L
    SplashStyle.BEACON_WAKE -> 900L
    SplashStyle.VAULT_BLOOM -> 1250L
    SplashStyle.HALLOWEEN -> 1380L
    SplashStyle.CHRISTMAS -> 1380L
}

fun SplashStyle.totalDisplayMillis(): Long = when (this) {
    SplashStyle.NONE -> 0L
    else -> 800L + effectDurationMillis()
}

fun splashSignalForgeIconScale(progress: Float): Float {
    val forge = ((progress - 0.54f) / 0.38f).coerceIn(0f, 1f)
    val eased = forge * forge * (3f - 2f * forge)
    return 0.35f + eased * 0.65f + sin(forge * PI.toFloat()) * 0.05f
}

fun splashSignalForgeIconAlpha(progress: Float): Float =
    ((progress - 0.50f) / 0.35f).coerceIn(0f, 1f)

fun splashTriangulateIconScale(progress: Float): Float {
    val t = ((progress - 0.48f) / 0.38f).coerceIn(0f, 1f)
    val eased = FastOutSlowInEasing.transform(t)
    return 0.55f + eased * 0.45f
}

fun splashTriangulateIconAlpha(progress: Float): Float {
    val t = ((progress - 0.44f) / 0.4f).coerceIn(0f, 1f)
    return FastOutSlowInEasing.transform(t)
}

fun splashVaultBloomIconScale(progress: Float): Float {
    val launch = ((progress - 0.40f) / 0.60f).coerceIn(0f, 1f)
    val eased = launch * launch * launch
    val pop = sin(launch * PI.toFloat()) * 0.14f
    return 0.07f + eased * 0.93f + pop
}

fun splashVaultBloomIconAlpha(progress: Float): Float =
    ((progress - 0.38f) / 0.22f).coerceIn(0f, 1f)

fun splashVaultBloomIconRotation(progress: Float): Float {
    val launch = ((progress - 0.40f) / 0.60f).coerceIn(0f, 1f)
    return sin(launch * PI.toFloat() * 5.5f) * 13f * (1f - launch * 0.4f)
}

fun splashVaultBloomWingFlap(progress: Float): Float =
    sin(progress * PI.toFloat() * 11f) * 24f

fun splashBeaconIconScale(progress: Float): Float {
    val ignitePhase = (progress / 0.35f).coerceIn(0f, 1f)
    val pulsePhase = ((progress - 0.2f) / 0.8f).coerceIn(0f, 1f)
    val pulse = if (pulsePhase > 0f) {
        sin(pulsePhase * PI.toFloat() * 3f) * 0.06f + 1f
    } else {
        ignitePhase * 0.85f + 0.15f
    }
    return pulse.coerceIn(0.85f, 1.12f)
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
        SplashStyle.BEACON_WAKE -> BeaconWakeSplashEffect(progress, modifier)
        SplashStyle.VAULT_BLOOM -> VaultBloomSplashEffect(progress, modifier)
        SplashStyle.HALLOWEEN -> HalloweenSplashEffect(progress, modifier)
        SplashStyle.CHRISTMAS -> ChristmasSplashEffect(progress, modifier)
    }
}

@Composable
fun VaultBloomWings(progress: Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    val launch = ((progress - 0.38f) / 0.62f).coerceIn(0f, 1f)
    val wingAlpha = (launch * 1.4f).coerceIn(0f, 1f)
    if (wingAlpha <= 0.02f) return
    val flap = splashVaultBloomWingFlap(progress)
    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val wingW = size.width * 0.38f
        val wingH = size.height * 0.22f
        listOf(-1f, 1f).forEach { side ->
            rotate(flap * side, pivot = Offset(cx + side * wingW * 0.15f, cy)) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.75f * wingAlpha),
                            colors.TealSoft.copy(alpha = 0.45f * wingAlpha),
                            Color.Transparent
                        ),
                        center = Offset(cx + side * wingW * 0.55f, cy - wingH * 0.1f),
                        radius = wingW * 0.55f
                    ),
                    topLeft = Offset(cx + side * wingW * 0.08f, cy - wingH * 0.55f),
                    size = Size(wingW * 0.72f, wingH)
                )
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
            val ignitePhase = (progress / 0.22f).coerceIn(0f, 1f)
            val spiralPhase = ((progress - 0.12f) / 0.48f).coerceIn(0f, 1f)
            val spiralEased = spiralPhase * spiralPhase * (3f - 2f * spiralPhase)
            val ringPhase = ((progress - 0.42f) / 0.38f).coerceIn(0f, 1f)
            val ringEased = ringPhase * ringPhase * (3f - 2f * ringPhase)
            val forgePhase = ((progress - 0.58f) / 0.42f).coerceIn(0f, 1f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.PrimaryBright.copy(alpha = 0.35f * ignitePhase),
                        colors.Teal.copy(alpha = 0.18f * ignitePhase),
                        Color.Transparent
                    ),
                    center = center + Offset(0f, maxR * 0.55f),
                    radius = maxR * 1.1f
                ),
                radius = maxR * 1.1f,
                center = center + Offset(0f, maxR * 0.55f)
            )

            if (spiralPhase > 0f) {
                val helixAlpha = (1f - forgePhase * 0.65f).coerceIn(0f, 1f)
                listOf(colors.PrimaryBright to 1f, colors.Teal to -1f).forEach { (tint, direction) ->
                    val helixPath = Path()
                    var started = false
                    for (step in 0..72) {
                        val t = (step / 72f) * spiralEased
                        if (t <= 0f) continue
                        val angle = (t * 540f * direction + progress * 40f * direction) * PI.toFloat() / 180f
                        val radius = maxR * (1.05f - t * 0.72f)
                        val point = center + Offset(cos(angle), sin(angle)) * radius
                        if (!started) { helixPath.moveTo(point.x, point.y); started = true }
                        else helixPath.lineTo(point.x, point.y)
                    }
                    if (started) {
                        drawPath(
                            helixPath,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    tint.copy(alpha = 0.15f * helixAlpha),
                                    tint.copy(alpha = 0.85f * helixAlpha),
                                    Color.White.copy(alpha = 0.55f * helixAlpha)
                                ),
                                start = center + Offset(-maxR, -maxR),
                                end = center + Offset(maxR, maxR)
                            ),
                            style = Stroke(width = 3.2f, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            if (ringPhase > 0f) {
                val ringR = maxR * 0.38f
                for (i in 0 until 6) {
                    val segDelay = i * 0.12f
                    val segProgress = ((ringEased - segDelay) / (1f - segDelay)).coerceIn(0f, 1f)
                    if (segProgress <= 0f) continue
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                colors.PrimaryBright.copy(alpha = 0.95f * segProgress),
                                colors.Teal.copy(alpha = 0.75f * segProgress),
                                colors.PrimaryBright.copy(alpha = 0.95f * segProgress)
                            ),
                            center = center
                        ),
                        startAngle = i * 60f - 90f,
                        sweepAngle = 58f * segProgress,
                        useCenter = false,
                        topLeft = Offset(center.x - ringR, center.y - ringR),
                        size = Size(ringR * 2f, ringR * 2f),
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )
                }
            }

            if (forgePhase > 0.35f) {
                val burst = ((forgePhase - 0.35f) / 0.65f).coerceIn(0f, 1f)
                for (i in 0 until 12) {
                    val angle = (i * 30f + progress * 45f) * PI.toFloat() / 180f
                    val dist = maxR * (0.18f + burst * 0.55f)
                    drawCircle(
                        color = colors.PrimaryBright.copy(alpha = (1f - burst) * 0.9f),
                        radius = 3f,
                        center = center + Offset(cos(angle), sin(angle)) * dist
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultDoorSplashEffect(progress: Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val doorRadius = size.minDimension * 0.28f
            val hinge = center + Offset(-doorRadius * 1.05f, 0f)
            val spinEased = 1f - (1f - (progress / 0.42f).coerceIn(0f, 1f)).let { it * it * it }
            val dialRotation = spinEased * 1080f
            val clickPhase = ((progress - 0.38f) / 0.1f).coerceIn(0f, 1f)
            val openEased = (((progress - 0.48f) / 0.52f).coerceIn(0f, 1f)).let { it * it * (3f - 2f * it) }
            val doorSwing = -openEased * 68f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF5A5A62), Color(0xFF2A2A30), Color(0xFF121216)),
                    center = center - Offset(doorRadius * 0.15f, doorRadius * 0.15f),
                    radius = doorRadius * 1.18f
                ),
                radius = doorRadius * 1.12f,
                center = center
            )
            drawCircle(
                color = Color(0xFF888890).copy(alpha = 0.55f),
                radius = doorRadius * 1.12f,
                center = center,
                style = Stroke(width = 3.5f)
            )

            if (openEased > 0.05f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.PrimaryBright.copy(alpha = openEased * 0.85f),
                            colors.Teal.copy(alpha = openEased * 0.45f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = doorRadius * 1.4f
                    ),
                    radius = doorRadius * 1.4f,
                    center = center
                )
            }

            withTransform({
                rotate(doorSwing, pivot = hinge)
                val doorPulse = 1f + sin(clickPhase * PI.toFloat()) * 0.12f
                scale(doorPulse, doorPulse, pivot = center)
            }) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF707078), Color(0xFF45454C), Color(0xFF28282E)),
                        center = center - Offset(doorRadius * 0.2f, doorRadius * 0.25f),
                        radius = doorRadius * 1.1f
                    ),
                    radius = doorRadius,
                    center = center
                )
                rotate(dialRotation, pivot = center) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF3A3A42), Color(0xFF1E1E24)),
                            center = center,
                            radius = doorRadius * 0.38f
                        ),
                        radius = doorRadius * 0.38f,
                        center = center
                    )
                    for (i in 0 until 6) {
                        rotate(i * 60f, pivot = center) {
                            drawLine(
                                color = colors.Teal.copy(alpha = 0.9f),
                                start = center + Offset(0f, -doorRadius * 0.34f),
                                end = center + Offset(0f, -doorRadius * 0.22f),
                                strokeWidth = 3.5f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarSplashEffect(progress: Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    val contacts = listOf(35f to 0.58f, 128f to 0.72f, 215f to 0.48f, 290f to 0.65f)
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension * 0.44f
            val fadeIn = (progress * 2.5f).coerceIn(0f, 1f)
            val sweepAngle = progress * 540f - 90f

            for (i in 1..5) {
                val ringR = maxRadius * i / 5f
                drawCircle(
                    color = colors.Teal.copy(alpha = ((0.28f - i * 0.04f) * fadeIn).coerceAtLeast(0.04f)),
                    radius = ringR,
                    center = center,
                    style = Stroke(width = if (i == 5) 1.8f else 1.2f)
                )
            }

            for (trail in 0..3) {
                rotate(sweepAngle - trail * 14f, pivot = center) {
                    drawArc(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.PrimaryBright.copy(alpha = (0.35f - trail * 0.08f) * fadeIn),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxRadius
                        ),
                        startAngle = -32f,
                        sweepAngle = 32f,
                        useCenter = true,
                        topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                        size = Size(maxRadius * 2f, maxRadius * 2f)
                    )
                }
            }

            rotate(sweepAngle, pivot = center) {
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(colors.PrimaryBright.copy(alpha = 0.95f), Color.Transparent),
                        start = center,
                        end = center + Offset(maxRadius * 0.98f, 0f)
                    ),
                    start = center,
                    end = center + Offset(maxRadius * 0.98f, 0f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }

            contacts.forEach { (angleDeg, distFrac) ->
                val normalized = ((sweepAngle + 90f) % 360f + 360f) % 360f
                if (normalized >= angleDeg || progress > 0.82f) {
                    val pingPhase = if (progress > 0.82f) 1f else ((normalized - angleDeg) / 40f).coerceIn(0f, 1f)
                    val rad = angleDeg * PI.toFloat() / 180f
                    val point = center + Offset(cos(rad), sin(rad)) * maxRadius * distFrac
                    drawCircle(color = colors.Teal.copy(alpha = 0.95f * pingPhase), radius = 4.5f, center = point)
                }
            }
        }
    }
}

@Composable
private fun TriangulateSplashEffect(progress: Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.44f
            val pingPhase = (progress / 0.55f).coerceIn(0f, 1f)
            val lineEased = (((progress - 0.18f) / 0.52f).coerceIn(0f, 1f)).let { it * it * (3f - 2f * it) }
            val lockPhase = ((progress - 0.62f) / 0.38f).coerceIn(0f, 1f)
            val towers = listOf(
                center + Offset(-maxR * 0.82f, -maxR * 0.58f),
                center + Offset(maxR * 0.88f, -maxR * 0.42f),
                center + Offset(0f, maxR * 0.92f)
            )

            towers.forEachIndexed { index, tower ->
                val towerPing = ((pingPhase - index * 0.12f) / (1f - index * 0.12f)).coerceIn(0f, 1f)
                if (towerPing <= 0f) return@forEachIndexed
                drawCircle(color = colors.PrimaryBright.copy(alpha = 0.9f), radius = 4f, center = tower)
                val pingR = maxR * 0.08f + towerPing * maxR * 0.75f
                drawCircle(
                    color = colors.Teal.copy(alpha = (1f - towerPing) * 0.55f),
                    radius = pingR,
                    center = tower,
                    style = Stroke(width = 2f)
                )
                if (lineEased > 0f) {
                    val lineEnd = tower + (center - tower) * lineEased
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(colors.Teal.copy(alpha = 0.85f * lineEased), Color.Transparent),
                            start = tower,
                            end = lineEnd
                        ),
                        start = tower,
                        end = lineEnd,
                        strokeWidth = 2.2f,
                        cap = StrokeCap.Round
                    )
                }
            }

            if (lockPhase > 0f) {
                val lockR = maxR * (0.18f + lockPhase * 0.08f)
                drawCircle(
                    color = colors.PrimaryBright.copy(alpha = 0.75f),
                    radius = lockR,
                    center = center,
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}

@Composable
private fun BeaconWakeSplashEffect(progress: Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.42f
            val ignitePhase = (progress / 0.35f).coerceIn(0f, 1f)
            val pulsePhase = ((progress - 0.2f) / 0.8f).coerceIn(0f, 1f)
            val needleEased = (((progress - 0.55f) / 0.45f).coerceIn(0f, 1f)).let { it * it * (3f - 2f * it) }

            if (pulsePhase > 0f) {
                for (wave in 0..3) {
                    val waveP = ((pulsePhase - wave * 0.15f) / (1f - wave * 0.15f)).coerceIn(0f, 1f)
                    if (waveP > 0f) {
                        drawCircle(
                            color = colors.Teal.copy(alpha = (1f - waveP) * 0.42f),
                            radius = maxR * 0.12f + waveP * maxR * 0.95f,
                            center = center,
                            style = Stroke(width = 2.5f - wave * 0.4f)
                        )
                    }
                }
            }

            if (ignitePhase > 0.2f) {
                val rayAlpha = (ignitePhase - 0.2f) / 0.8f
                for (i in 0 until 8) {
                    rotate(i * 45f + progress * 18f, pivot = center) {
                        drawLine(
                            color = colors.PrimaryBright.copy(alpha = rayAlpha * 0.5f),
                            start = center,
                            end = center + Offset(maxR * 0.85f, 0f),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            rotate(-120f + needleEased * 120f, pivot = center) {
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(colors.Teal, colors.PrimaryBright),
                        start = center,
                        end = center + Offset(0f, -maxR * 0.55f * needleEased)
                    ),
                    start = center,
                    end = center + Offset(0f, -maxR * 0.55f * needleEased),
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun VaultBloomSplashEffect(progress: Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    val petalPink = Color(0xFFFF6B9D)
    val petalRose = Color(0xFFE84393)
    val stamenGold = Color(0xFFFFD54F)
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.40f
            val bloomEased = (((progress - 0.08f) / 0.52f).coerceIn(0f, 1f)).let { it * it * (3f - 2f * it) }
            val buzzPhase = ((progress - 0.42f) / 0.58f).coerceIn(0f, 1f)

            drawLine(
                color = Color(0xFF43A047),
                start = Offset(center.x, center.y + maxR * 0.08f),
                end = Offset(center.x, center.y + maxR * 0.95f),
                strokeWidth = 4.5f,
                cap = StrokeCap.Round
            )

            for (i in 0 until 7) {
                val baseAngle = i * (360f / 7f) - 90f
                val petalAngle = baseAngle + (1f - bloomEased) * 38f * (if (i % 2 == 0) 1f else -1f)
                val petalLen = maxR * (0.28f + bloomEased * 0.22f)
                rotate(petalAngle, pivot = center) {
                    val petalPath = Path().apply {
                        moveTo(center.x, center.y)
                        cubicTo(
                            center.x + maxR * 0.1f, center.y - petalLen * 0.35f,
                            center.x + maxR * 0.14f, center.y - petalLen * 0.75f,
                            center.x, center.y - petalLen
                        )
                        cubicTo(
                            center.x - maxR * 0.14f, center.y - petalLen * 0.75f,
                            center.x - maxR * 0.1f, center.y - petalLen * 0.35f,
                            center.x, center.y
                        )
                        close()
                    }
                    drawPath(petalPath, color = petalPink.copy(alpha = 0.95f))
                }
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(stamenGold, Color(0xFFFF8F00)),
                    center = center,
                    radius = maxR * 0.11f
                ),
                radius = maxR * 0.11f,
                center = center
            )

            if (buzzPhase > 0.02f) {
                val burst = sin(buzzPhase * PI.toFloat()) * (1f - buzzPhase * 0.5f)
                for (p in 0 until 10) {
                    val a = (p * 36f + progress * 80f) * PI.toFloat() / 180f
                    val dist = maxR * (0.15f + buzzPhase * 0.55f)
                    drawCircle(
                        color = stamenGold.copy(alpha = burst * 0.85f),
                        radius = 3f,
                        center = center + Offset(cos(a), sin(a)) * dist
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawSplashHalloweenCobweb(origin: Offset, span: Float, alpha: Float, mirror: Boolean) {
    val baseAngle = if (mirror) 135f else 45f
    for (spoke in 0 until 8) {
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
                    for (seg in 0 until 14) {
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
                for (ring in 0 until 8) {
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
                for (b in 0 until 10) {
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
    for (ray in 0 until 8) {
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
                    for (seg in 0 until 14) {
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
                for (s in 0 until 18) {
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
    SplashStyle.SIGNAL_FORGE -> 0.62f
    SplashStyle.VAULT_DOOR -> 0.52f
    SplashStyle.RADAR -> 0.58f
    SplashStyle.TRIANGULATE -> 0.68f
    SplashStyle.BEACON_WAKE -> 0.48f
    SplashStyle.VAULT_BLOOM -> 0.62f
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
    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SplashStyle.entries.forEach { style ->
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
                                    SplashStyle.SIGNAL_FORGE -> Modifier.size(previewMarkSize).scale(splashSignalForgeIconScale(previewProgress)).alpha(0.2f + splashSignalForgeIconAlpha(previewProgress) * 0.8f)
                                    SplashStyle.TRIANGULATE -> Modifier.size(previewMarkSize).scale(splashTriangulateIconScale(previewProgress)).alpha(0.25f + splashTriangulateIconAlpha(previewProgress) * 0.75f)
                                    SplashStyle.BEACON_WAKE -> Modifier.size(previewMarkSize).scale(splashBeaconIconScale(previewProgress))
                                    SplashStyle.VAULT_BLOOM -> Modifier.size(previewMarkSize).scale(splashVaultBloomIconScale(previewProgress)).rotate(splashVaultBloomIconRotation(previewProgress)).alpha(0.25f + splashVaultBloomIconAlpha(previewProgress) * 0.75f)
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
        TrailingScrollHint(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

