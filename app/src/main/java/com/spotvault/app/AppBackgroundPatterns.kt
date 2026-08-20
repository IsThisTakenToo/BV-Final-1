package com.spotvault.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Every selectable app-wide background — the layer that sits behind all screen content.
 * id is the persisted pref value. */
enum class BackgroundPattern(val id: String, val label: String) {
    GRADIENT_MESH("gradient_mesh", "Gradient Mesh"),
    AURORA_BANDS("aurora_bands", "Aurora Bands"),
    HEX_TESS("hex_tess", "Hex Tessellation"),
    LIGHT_LEAKS("light_leaks", "Light Leaks"),
    WAVE_FLOW("wave_flow", "Wave Flow"),
    NEBULA_MIST("nebula_mist", "Nebula Mist"),
    SCAN_SWEEP("scan_sweep", "Scan Sweep"),
    PIN_FIELD("pin_field", "Pin Field"),
    PLAIN_DARK("plain_dark", "Plain Dark");

    companion object {
        private val legacyIds = setOf(
            "grid_lines", "dot_matrix", "diagonal_hatch", "topographic",
            "constellation", "radial_rings", "blueprint"
        )

        fun fromId(id: String?): BackgroundPattern {
            if (id != null && id in legacyIds) return GRADIENT_MESH
            return entries.firstOrNull { it.id == id } ?: GRADIENT_MESH
        }
    }
}

/** Maps saved prefs (including retired pattern ids) to a current pattern id. */
fun normalizeBackgroundPatternId(raw: String?): String = BackgroundPattern.fromId(raw).id

/** The background behind every screen in the app — dispatches to whichever pattern is
 * selected in Settings > Appearance. Signature is unchanged from before patterns existed,
 * so every existing call site keeps working with no edits. */
@Composable
fun SpotVaultAmbientBackground(modifier: Modifier = Modifier) {
    when (BackgroundPattern.fromId(ThemeState.backgroundPatternId)) {
        BackgroundPattern.GRADIENT_MESH -> GradientMeshBackground(modifier)
        BackgroundPattern.AURORA_BANDS -> AuroraBandsBackground(modifier)
        BackgroundPattern.HEX_TESS -> HexTessellationBackground(modifier)
        BackgroundPattern.LIGHT_LEAKS -> LightLeaksBackground(modifier)
        BackgroundPattern.WAVE_FLOW -> WaveFlowBackground(modifier)
        BackgroundPattern.NEBULA_MIST -> NebulaMistBackground(modifier)
        BackgroundPattern.SCAN_SWEEP -> ScanSweepBackground(modifier)
        BackgroundPattern.PIN_FIELD -> PinFieldBackground(modifier)
        BackgroundPattern.PLAIN_DARK -> PlainDarkBackground(modifier)
    }
}

/** Subtle theme wash so patterns read on Void across home, vault, and settings. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPatternBase(
    baseFill: Color,
    themeColors: SpotVaultColorScheme,
    previewBoost: Boolean
) {
    val wash = if (previewBoost) 1.35f else 1f
    drawRect(baseFill)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                themeColors.PrimaryDeep.copy(alpha = 0.14f * wash),
                Color.Transparent,
                themeColors.Teal.copy(alpha = 0.10f * wash)
            )
        )
    )
}

private fun patternAlpha(base: Float, previewBoost: Boolean, boost: Float = 2.4f): Float =
    (base * if (previewBoost) boost else 1.75f).coerceAtMost(0.95f)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHexOutline(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float
) {
    val path = Path()
    for (i in 0 until 6) {
        val angle = Math.toRadians(60.0 * i - 30.0)
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMapPinGlyph(
    center: Offset,
    size: Float,
    color: Color
) {
    val headR = size * 0.34f
    val headCenter = center + Offset(0f, -size * 0.12f)
    drawCircle(color = color, radius = headR, center = headCenter)
    val tip = center + Offset(0f, size * 0.42f)
    val pin = Path().apply {
        moveTo(headCenter.x - headR * 0.72f, headCenter.y + headR * 0.15f)
        lineTo(tip.x, tip.y)
        lineTo(headCenter.x + headR * 0.72f, headCenter.y + headR * 0.15f)
        close()
    }
    drawPath(pin, color = color)
    drawCircle(color = Color.White.copy(alpha = color.alpha * 0.45f), radius = headR * 0.28f, center = headCenter + Offset(-headR * 0.22f, -headR * 0.18f))
}

/** [previewBoost] raises fill brightness and pattern alpha for use in a small Settings picker
 * cell — these patterns are tuned to be a subtle backdrop behind a full screen of real content,
 * which reads as solid black once shrunk into a ~100dp preview at their normal, very low alpha. */
@Composable
private fun GradientMeshBackground(modifier: Modifier = Modifier, previewBoost: Boolean = false) {
    val themeColors = LocalSpotVaultColors.current
    val reducedMotion = ThemeState.reduceAnimations
    // themeColors.Void directly, not SpotVaultColors.Void — this ambient background is drawn
    // behind every screen for the entire time the app is open, and reading straight off the
    // CompositionLocal instead of the AMOLED-aware getter meant the app's flagship "true black"
    // toggle never actually applied to it: every screen kept painting the per-theme dark color
    // (e.g. a dark purple, not black) right over wherever a screen-level Void read (which does
    // go through the AMOLED-aware getter) had already painted real black underneath. Checking
    // ThemeState.isAmoled here directly — it's a real mutableStateOf, so this still redraws
    // correctly the instant the toggle changes — is the fix, same logic SpotVaultColors.Void
    // itself uses. previewBoost (Settings' pattern picker thumbnails) intentionally stays
    // Elevated regardless, same as before.
    val baseFill = if (previewBoost) themeColors.Elevated else if (ThemeState.isAmoled) Color.Black else themeColors.Void
    val boost = if (previewBoost) 2.6f else 1.75f

    // Kept as a State object (not `by`-destructured here) — this is the app's default
    // background, drawn behind every screen for the entire time the app is open. Reading the
    // animated value at composable-body scope (as it used to) made every single animation frame
    // recompose this whole function AND rebuild all three gradient brushes (they lived in
    // drawWithCache's outer scope, which invalidates whenever anything it reads changes) —
    // continuous, avoidable work for the app's most-mounted piece of UI. Reading .value only
    // inside onDrawBehind below defers it straight to the draw phase: same animation, but no
    // composition/layout and no brush reconstruction on every tick.
    //
    // previewBoost (Settings' pattern picker) renders all eight patterns' thumbnails at once —
    // an unconditional infinite transition here means five perpetually-redrawing canvases the
    // instant that screen opens, the same bug already fixed for the compass style picker (see
    // VaultCompassDial's pulsePhase/sweepPhase comment). Skipping rememberInfiniteTransition
    // entirely in that case (not just ignoring its value, the way reducedMotion below does) is
    // what actually stops the per-frame Choreographer tick and draw-phase Path/Brush churn for a
    // ~100dp thumbnail nobody watches long enough to see drift anyway.
    val animatedDriftState: State<Float> = if (previewBoost) {
        remember { mutableStateOf(0.5f) }
    } else {
        val transition = rememberInfiniteTransition(label = "ambient")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "drift"
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height

                val radius1 = w * 0.55f
                val brush1 = Brush.radialGradient(
                    colors = listOf(themeColors.Primary.copy(alpha = (0.28f * boost).coerceAtMost(0.95f)), Color.Transparent),
                    center = Offset.Zero,
                    radius = radius1
                )

                val radius2 = w * 0.45f
                val brush2 = Brush.radialGradient(
                    colors = listOf(themeColors.Teal.copy(alpha = (0.16f * boost).coerceAtMost(0.95f)), Color.Transparent),
                    center = Offset.Zero,
                    radius = radius2
                )

                val radius3 = w * 0.35f
                val brush3 = Brush.radialGradient(
                    colors = listOf(themeColors.PrimaryDeep.copy(alpha = (0.35f * boost).coerceAtMost(0.95f)), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = radius3
                )

                onDrawBehind {
                    val drift = if (reducedMotion) 0.5f else animatedDriftState.value
                    drawRect(baseFill)

                    withTransform({
                        translate(left = w * (0.18f + drift * 0.08f), top = h * 0.12f)
                    }) {
                        drawCircle(brush = brush1, radius = radius1, center = Offset.Zero)
                    }

                    withTransform({
                        translate(left = w * (0.85f - drift * 0.06f), top = h * 0.78f)
                    }) {
                        drawCircle(brush = brush2, radius = radius2, center = Offset.Zero)
                    }

                    drawCircle(brush = brush3, radius = radius3, center = Offset(w * 0.5f, h * 0.45f))
                }
            }
    )
}

@Composable
private fun AuroraBandsBackground(modifier: Modifier = Modifier, previewBoost: Boolean = false) {
    val themeColors = LocalSpotVaultColors.current
    val reducedMotion = ThemeState.reduceAnimations
    // themeColors.Void directly, not SpotVaultColors.Void — this ambient background is drawn
    // behind every screen for the entire time the app is open, and reading straight off the
    // CompositionLocal instead of the AMOLED-aware getter meant the app's flagship "true black"
    // toggle never actually applied to it: every screen kept painting the per-theme dark color
    // (e.g. a dark purple, not black) right over wherever a screen-level Void read (which does
    // go through the AMOLED-aware getter) had already painted real black underneath. Checking
    // ThemeState.isAmoled here directly — it's a real mutableStateOf, so this still redraws
    // correctly the instant the toggle changes — is the fix, same logic SpotVaultColors.Void
    // itself uses. previewBoost (Settings' pattern picker thumbnails) intentionally stays
    // Elevated regardless, same as before.
    val baseFill = if (previewBoost) themeColors.Elevated else if (ThemeState.isAmoled) Color.Black else themeColors.Void

    // Kept as a State object — see GradientMeshBackground's comment above for why, including why
    // previewBoost skips rememberInfiniteTransition entirely instead of just ignoring its value.
    val driftState: State<Float> = if (previewBoost) {
        remember { mutableStateOf(0.5f) }
    } else {
        val transition = rememberInfiniteTransition(label = "aurora")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse),
            label = "auroraDrift"
        )
    }

    Box(
        modifier = modifier.fillMaxSize().drawWithCache {
            val w = size.width
            val h = size.height
            onDrawBehind {
                val phase = if (reducedMotion) 0.5f else driftState.value
                drawPatternBase(baseFill, themeColors, previewBoost)

                val bandCount = 5
                for (i in 0 until bandCount) {
                    val bandY = h * (0.08f + i * 0.19f) + sin(phase * PI.toFloat() * 2f + i * 0.9f) * h * 0.025f
                    val bandH = h * 0.11f
                    val tint = if (i % 2 == 0) themeColors.Primary else themeColors.Teal
                    val path = Path().apply {
                        moveTo(-w * 0.05f, bandY)
                        cubicTo(w * 0.25f, bandY - bandH * 0.45f, w * 0.75f, bandY + bandH * 0.35f, w * 1.05f, bandY - bandH * 0.1f)
                        lineTo(w * 1.05f, bandY + bandH)
                        cubicTo(w * 0.75f, bandY + bandH * 1.35f, w * 0.25f, bandY + bandH * 0.65f, -w * 0.05f, bandY + bandH * 0.95f)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                tint.copy(alpha = patternAlpha(0.16f, previewBoost)),
                                tint.copy(alpha = patternAlpha(0.22f, previewBoost)),
                                Color.Transparent
                            ),
                            start = Offset(0f, bandY),
                            end = Offset(w, bandY + bandH)
                        )
                    )
                }

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, baseFill.copy(alpha = 0.55f)),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.85f
                    )
                )
            }
        }
    )
}

@Composable
private fun HexTessellationBackground(modifier: Modifier = Modifier, previewBoost: Boolean = false) {
    val themeColors = LocalSpotVaultColors.current
    // themeColors.Void directly, not SpotVaultColors.Void — this ambient background is drawn
    // behind every screen for the entire time the app is open, and reading straight off the
    // CompositionLocal instead of the AMOLED-aware getter meant the app's flagship "true black"
    // toggle never actually applied to it: every screen kept painting the per-theme dark color
    // (e.g. a dark purple, not black) right over wherever a screen-level Void read (which does
    // go through the AMOLED-aware getter) had already painted real black underneath. Checking
    // ThemeState.isAmoled here directly — it's a real mutableStateOf, so this still redraws
    // correctly the instant the toggle changes — is the fix, same logic SpotVaultColors.Void
    // itself uses. previewBoost (Settings' pattern picker thumbnails) intentionally stays
    // Elevated regardless, same as before.
    val baseFill = if (previewBoost) themeColors.Elevated else if (ThemeState.isAmoled) Color.Black else themeColors.Void
    Box(
        modifier = modifier.fillMaxSize().drawWithCache {
            val ramScale = if (ThemeState.lowRamDevice) 1.7f else 1f
            val spacing = (if (previewBoost) 22.dp else 34.dp).toPx() * ramScale
            val hexR = spacing * 0.52f
            val hexH = hexR * 1.732f
            val center = Offset(size.width * 0.5f, size.height * 0.46f)
            onDrawBehind {
                drawPatternBase(baseFill, themeColors, previewBoost)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            themeColors.Teal.copy(alpha = patternAlpha(0.14f, previewBoost)),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.55f
                    ),
                    radius = size.minDimension * 0.55f,
                    center = center
                )

                val cols = (size.width / (hexR * 3f)).toInt() + 3
                val rows = (size.height / hexH).toInt() + 3
                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        val offsetX = if (row % 2 == 0) 0f else hexR * 1.5f
                        val cx = col * hexR * 3f + offsetX - hexR
                        val cy = row * hexH * 0.86f - hexH * 0.3f
                        val dist = (Offset(cx, cy) - center).getDistance()
                        val falloff = (1f - (dist / (size.maxDimension * 0.72f)).coerceIn(0f, 1f))
                        val accent = (row + col) % 2 == 0
                        val tint = if (accent) themeColors.Teal else themeColors.PrimaryBright
                        val alpha = patternAlpha(0.1f + falloff * 0.12f, previewBoost)
                        drawHexOutline(Offset(cx, cy), hexR * 0.92f, tint.copy(alpha = alpha), strokeWidth = if (accent) 1.4f else 1f)
                        if (falloff > 0.55f && accent) {
                            drawCircle(
                                color = themeColors.PrimaryBright.copy(alpha = alpha * 0.35f),
                                radius = 2.2f,
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun LightLeaksBackground(modifier: Modifier = Modifier, previewBoost: Boolean = false) {
    val themeColors = LocalSpotVaultColors.current
    // themeColors.Void directly, not SpotVaultColors.Void — this ambient background is drawn
    // behind every screen for the entire time the app is open, and reading straight off the
    // CompositionLocal instead of the AMOLED-aware getter meant the app's flagship "true black"
    // toggle never actually applied to it: every screen kept painting the per-theme dark color
    // (e.g. a dark purple, not black) right over wherever a screen-level Void read (which does
    // go through the AMOLED-aware getter) had already painted real black underneath. Checking
    // ThemeState.isAmoled here directly — it's a real mutableStateOf, so this still redraws
    // correctly the instant the toggle changes — is the fix, same logic SpotVaultColors.Void
    // itself uses. previewBoost (Settings' pattern picker thumbnails) intentionally stays
    // Elevated regardless, same as before.
    val baseFill = if (previewBoost) themeColors.Elevated else if (ThemeState.isAmoled) Color.Black else themeColors.Void
    Box(
        modifier = modifier.fillMaxSize().drawWithCache {
            val w = size.width
            val h = size.height
            onDrawBehind {
                drawPatternBase(baseFill, themeColors, previewBoost)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            themeColors.PrimaryBright.copy(alpha = patternAlpha(0.28f, previewBoost)),
                            themeColors.Primary.copy(alpha = patternAlpha(0.12f, previewBoost)),
                            Color.Transparent
                        ),
                        center = Offset(-w * 0.08f, -h * 0.06f),
                        radius = w * 0.72f
                    ),
                    radius = w * 0.72f,
                    center = Offset(-w * 0.08f, -h * 0.06f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            themeColors.Teal.copy(alpha = patternAlpha(0.24f, previewBoost)),
                            themeColors.PrimaryDeep.copy(alpha = patternAlpha(0.1f, previewBoost)),
                            Color.Transparent
                        ),
                        center = Offset(w * 1.06f, h * 1.02f),
                        radius = w * 0.68f
                    ),
                    radius = w * 0.68f,
                    center = Offset(w * 1.06f, h * 1.02f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            themeColors.Teal.copy(alpha = patternAlpha(0.14f, previewBoost)),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.92f, h * 0.08f),
                        radius = w * 0.38f
                    ),
                    radius = w * 0.38f,
                    center = Offset(w * 0.92f, h * 0.08f)
                )

                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            themeColors.PrimaryBright.copy(alpha = patternAlpha(0.06f, previewBoost)),
                            Color.Transparent,
                            themeColors.Teal.copy(alpha = patternAlpha(0.05f, previewBoost))
                        ),
                        start = Offset.Zero,
                        end = Offset(w, h)
                    )
                )
            }
        }
    )
}

@Composable
private fun WaveFlowBackground(modifier: Modifier = Modifier, previewBoost: Boolean = false) {
    val themeColors = LocalSpotVaultColors.current
    val reducedMotion = ThemeState.reduceAnimations
    // themeColors.Void directly, not SpotVaultColors.Void — this ambient background is drawn
    // behind every screen for the entire time the app is open, and reading straight off the
    // CompositionLocal instead of the AMOLED-aware getter meant the app's flagship "true black"
    // toggle never actually applied to it: every screen kept painting the per-theme dark color
    // (e.g. a dark purple, not black) right over wherever a screen-level Void read (which does
    // go through the AMOLED-aware getter) had already painted real black underneath. Checking
    // ThemeState.isAmoled here directly — it's a real mutableStateOf, so this still redraws
    // correctly the instant the toggle changes — is the fix, same logic SpotVaultColors.Void
    // itself uses. previewBoost (Settings' pattern picker thumbnails) intentionally stays
    // Elevated regardless, same as before.
    val baseFill = if (previewBoost) themeColors.Elevated else if (ThemeState.isAmoled) Color.Black else themeColors.Void

    // Kept as a State object — see GradientMeshBackground's comment above for why, including why
    // previewBoost skips rememberInfiniteTransition entirely instead of just ignoring its value.
    val shiftState: State<Float> = if (previewBoost) {
        remember { mutableStateOf(0f) }
    } else {
        val transition = rememberInfiniteTransition(label = "waveFlow")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (PI * 2).toFloat(),
            animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart),
            label = "waveShift"
        )
    }

    Box(
        modifier = modifier.fillMaxSize().drawWithCache {
            val w = size.width
            val h = size.height
            onDrawBehind {
                val wavePhase = if (reducedMotion) 0f else shiftState.value
                drawPatternBase(baseFill, themeColors, previewBoost)

                val waves = listOf(
                    themeColors.Teal to 0.18f,
                    themeColors.PrimaryBright to 0.28f,
                    themeColors.Primary to 0.38f,
                    themeColors.Teal to 0.48f,
                    themeColors.PrimaryBright to 0.58f
                )
                waves.forEachIndexed { index, (tint, yFrac) ->
                    val baseY = h * yFrac
                    val amplitude = h * 0.028f
                    val path = Path()
                    val steps = 48
                    for (step in 0..steps) {
                        val x = w * step / steps
                        val y = baseY + sin(wavePhase + x / w * PI.toFloat() * 4f + index * 0.7f) * amplitude
                        if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = tint.copy(alpha = patternAlpha(if (index % 2 == 0) 0.14f else 0.1f, previewBoost)),
                        style = Stroke(width = if (index % 2 == 0) 2f else 1.3f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    )
}

@Composable
private fun NebulaMistBackground(modifier: Modifier = Modifier, previewBoost: Boolean = false) {
    val themeColors = LocalSpotVaultColors.current
    val reducedMotion = ThemeState.reduceAnimations
    // themeColors.Void directly, not SpotVaultColors.Void — this ambient background is drawn
    // behind every screen for the entire time the app is open, and reading straight off the
    // CompositionLocal instead of the AMOLED-aware getter meant the app's flagship "true black"
    // toggle never actually applied to it: every screen kept painting the per-theme dark color
    // (e.g. a dark purple, not black) right over wherever a screen-level Void read (which does
    // go through the AMOLED-aware getter) had already painted real black underneath. Checking
    // ThemeState.isAmoled here directly — it's a real mutableStateOf, so this still redraws
    // correctly the instant the toggle changes — is the fix, same logic SpotVaultColors.Void
    // itself uses. previewBoost (Settings' pattern picker thumbnails) intentionally stays
    // Elevated regardless, same as before.
    val baseFill = if (previewBoost) themeColors.Elevated else if (ThemeState.isAmoled) Color.Black else themeColors.Void

    // Kept as a State object — see GradientMeshBackground's comment above for why, including why
    // previewBoost skips rememberInfiniteTransition entirely instead of just ignoring its value.
    // Value read only inside onDrawBehind below, so the `clouds` list (previously rebuilt in
    // drawWithCache's outer scope on every tick because it captured drift) is only rebuilt when
    // size changes.
    val breatheState: State<Float> = if (previewBoost) {
        remember { mutableStateOf(0.5f) }
    } else {
        val transition = rememberInfiniteTransition(label = "nebula")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse),
            label = "nebulaBreathe"
        )
    }

    Box(
        modifier = modifier.fillMaxSize().drawWithCache {
            val w = size.width
            val h = size.height
            onDrawBehind {
                val drift = if (reducedMotion) 0.5f else breatheState.value
                val clouds = listOf(
                    Triple(Offset(w * 0.22f, h * 0.2f), themeColors.Primary, w * 0.42f),
                    Triple(Offset(w * (0.78f + drift * 0.04f), h * 0.32f), themeColors.Teal, w * 0.36f),
                    Triple(Offset(w * 0.38f, h * (0.72f - drift * 0.03f)), themeColors.PrimaryBright, w * 0.38f),
                    Triple(Offset(w * 0.68f, h * 0.78f), themeColors.PrimaryDeep, w * 0.44f),
                    Triple(Offset(w * 0.5f, h * 0.48f), themeColors.Teal, w * 0.28f)
                )
                drawPatternBase(baseFill, themeColors, previewBoost)

                clouds.forEach { (center, tint, cloudRadius) ->
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tint.copy(alpha = patternAlpha(0.22f, previewBoost)),
                                tint.copy(alpha = patternAlpha(0.1f, previewBoost)),
                                Color.Transparent
                            ),
                            center = center,
                            radius = cloudRadius
                        ),
                        radius = cloudRadius,
                        center = center
                    )
                }

                // Fine star dust
                val random = Random(31)
                repeat(28) {
                    val sx = random.nextFloat() * w
                    val sy = random.nextFloat() * h
                    drawCircle(
                        color = themeColors.Muted.copy(alpha = patternAlpha(0.18f, previewBoost)),
                        radius = 0.8f + random.nextFloat() * 1.2f,
                        center = Offset(sx, sy)
                    )
                }
            }
        }
    )
}

@Composable
private fun ScanSweepBackground(modifier: Modifier = Modifier, previewBoost: Boolean = false) {
    val themeColors = LocalSpotVaultColors.current
    val reducedMotion = ThemeState.reduceAnimations
    // themeColors.Void directly, not SpotVaultColors.Void — this ambient background is drawn
    // behind every screen for the entire time the app is open, and reading straight off the
    // CompositionLocal instead of the AMOLED-aware getter meant the app's flagship "true black"
    // toggle never actually applied to it: every screen kept painting the per-theme dark color
    // (e.g. a dark purple, not black) right over wherever a screen-level Void read (which does
    // go through the AMOLED-aware getter) had already painted real black underneath. Checking
    // ThemeState.isAmoled here directly — it's a real mutableStateOf, so this still redraws
    // correctly the instant the toggle changes — is the fix, same logic SpotVaultColors.Void
    // itself uses. previewBoost (Settings' pattern picker thumbnails) intentionally stays
    // Elevated regardless, same as before.
    val baseFill = if (previewBoost) themeColors.Elevated else if (ThemeState.isAmoled) Color.Black else themeColors.Void

    // Kept as a State object — see GradientMeshBackground's comment above for why, including why
    // previewBoost skips rememberInfiniteTransition entirely instead of just ignoring its value.
    // 0.45f matches the reducedMotion fallback below.
    val sweepState: State<Float> = if (previewBoost) {
        remember { mutableStateOf(0.45f) }
    } else {
        val transition = rememberInfiniteTransition(label = "scan")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
            label = "scanPos"
        )
    }

    Box(
        modifier = modifier.fillMaxSize().drawWithCache {
            val w = size.width
            val h = size.height
            val ramScale = if (ThemeState.lowRamDevice) 1.7f else 1f
            val spacing = (if (previewBoost) 24.dp else 40.dp).toPx() * ramScale
            onDrawBehind {
                drawPatternBase(baseFill, themeColors, previewBoost)

                // Subtle horizontal scan lines
                var y = 0f
                while (y <= h) {
                    drawLine(
                        color = themeColors.Teal.copy(alpha = patternAlpha(0.06f, previewBoost)),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 0.8f
                    )
                    y += spacing * 0.5f
                }

                // Moving sweep band
                val scanY = if (reducedMotion) 0.45f else sweepState.value
                val bandCenter = h * scanY
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            themeColors.Teal.copy(alpha = patternAlpha(0.08f, previewBoost)),
                            themeColors.PrimaryBright.copy(alpha = patternAlpha(0.18f, previewBoost)),
                            themeColors.Teal.copy(alpha = patternAlpha(0.08f, previewBoost)),
                            Color.Transparent
                        ),
                        startY = bandCenter - h * 0.12f,
                        endY = bandCenter + h * 0.12f
                    ),
                    topLeft = Offset(0f, bandCenter - h * 0.12f),
                    size = Size(w, h * 0.24f)
                )
                drawLine(
                    color = themeColors.Teal.copy(alpha = patternAlpha(0.35f, previewBoost)),
                    start = Offset(0f, bandCenter),
                    end = Offset(w, bandCenter),
                    strokeWidth = 1.6f
                )

                // HUD corner brackets
                val arm = w * 0.08f
                val inset = w * 0.06f
                val bracketColor = themeColors.PrimaryBright.copy(alpha = patternAlpha(0.2f, previewBoost))
                listOf(
                    Offset(inset, inset),
                    Offset(w - inset, inset),
                    Offset(w - inset, h - inset),
                    Offset(inset, h - inset)
                ).forEachIndexed { i, c ->
                    when (i) {
                        0 -> { drawLine(bracketColor, c, c + Offset(arm, 0f), 1.5f); drawLine(bracketColor, c, c + Offset(0f, arm), 1.5f) }
                        1 -> { drawLine(bracketColor, c, c + Offset(-arm, 0f), 1.5f); drawLine(bracketColor, c, c + Offset(0f, arm), 1.5f) }
                        2 -> { drawLine(bracketColor, c, c + Offset(-arm, 0f), 1.5f); drawLine(bracketColor, c, c + Offset(0f, -arm), 1.5f) }
                        else -> { drawLine(bracketColor, c, c + Offset(arm, 0f), 1.5f); drawLine(bracketColor, c, c + Offset(0f, -arm), 1.5f) }
                    }
                }
            }
        }
    )
}

@Composable
private fun PinFieldBackground(modifier: Modifier = Modifier, previewBoost: Boolean = false) {
    val themeColors = LocalSpotVaultColors.current
    // themeColors.Void directly, not SpotVaultColors.Void — this ambient background is drawn
    // behind every screen for the entire time the app is open, and reading straight off the
    // CompositionLocal instead of the AMOLED-aware getter meant the app's flagship "true black"
    // toggle never actually applied to it: every screen kept painting the per-theme dark color
    // (e.g. a dark purple, not black) right over wherever a screen-level Void read (which does
    // go through the AMOLED-aware getter) had already painted real black underneath. Checking
    // ThemeState.isAmoled here directly — it's a real mutableStateOf, so this still redraws
    // correctly the instant the toggle changes — is the fix, same logic SpotVaultColors.Void
    // itself uses. previewBoost (Settings' pattern picker thumbnails) intentionally stays
    // Elevated regardless, same as before.
    val baseFill = if (previewBoost) themeColors.Elevated else if (ThemeState.isAmoled) Color.Black else themeColors.Void
    Box(
        modifier = modifier.fillMaxSize().drawWithCache {
            val ramScale = if (ThemeState.lowRamDevice) 1.7f else 1f
            val spacing = (if (previewBoost) 52.dp else 72.dp).toPx() * ramScale
            val center = Offset(size.width * 0.5f, size.height * 0.45f)
            onDrawBehind {
                // Random(42) created here, inside onDrawBehind, not hoisted up next to `spacing`
                // above — onDrawBehind can legitimately re-run many times without drawWithCache
                // itself re-running (e.g. every frame of the splash screen's own progress
                // animation, which shares this draw phase with no layer boundary between them).
                // A single Random instance shared across those re-invocations kept advancing its
                // sequence on every extra call, so each pin's jitter offset silently drifted frame
                // to frame — read as the whole pin field stuttering/vibrating for the splash's
                // duration. Recreating it fresh here means every draw call reproduces the exact
                // same seeded sequence, so the pins render in the same place every time.
                val random = Random(42)
                drawPatternBase(baseFill, themeColors, previewBoost)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            themeColors.Primary.copy(alpha = patternAlpha(0.12f, previewBoost)),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.5f
                    ),
                    radius = size.minDimension * 0.5f,
                    center = center
                )

                var row = 0
                var y = spacing * 0.4f
                while (y <= size.height + spacing) {
                    var col = 0
                    var x = spacing * 0.35f + if (row % 2 == 0) 0f else spacing * 0.5f
                    while (x <= size.width + spacing) {
                        val jitterX = (random.nextFloat() - 0.5f) * spacing * 0.22f
                        val jitterY = (random.nextFloat() - 0.5f) * spacing * 0.22f
                        val pos = Offset(x + jitterX, y + jitterY)
                        val dist = (pos - center).getDistance()
                        val falloff = (1f - (dist / (size.maxDimension * 0.75f)).coerceIn(0f, 1f))
                        val pinSize = spacing * (0.22f + falloff * 0.1f)
                        val accent = (row + col) % 3 == 0
                        val tint = if (accent) themeColors.Teal else themeColors.PrimaryBright
                        drawMapPinGlyph(
                            center = pos,
                            size = pinSize,
                            color = tint.copy(alpha = patternAlpha(0.12f + falloff * 0.14f, previewBoost))
                        )
                        x += spacing
                        col++
                    }
                    y += spacing
                    row++
                }

                // Faint connection arcs between nearby pins (map network feel)
                val nodes = List(8) { i ->
                    Offset(
                        size.width * (0.15f + (i % 4) * 0.22f),
                        size.height * (0.18f + (i / 4) * 0.38f)
                    )
                }
                for (i in nodes.indices) {
                    for (j in i + 1 until nodes.size) {
                        drawLine(
                            color = themeColors.Teal.copy(alpha = patternAlpha(0.08f, previewBoost)),
                            start = nodes[i],
                            end = nodes[j],
                            strokeWidth = 0.9f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    )
}

/** No motif, no motion — just the same base fill and subtle theme wash every other pattern
 * draws underneath its own decoration, for anyone who'd rather the background just get out of
 * the way. Cheapest option in the list: no drawWithCache work beyond the shared wash, and
 * nothing to gate behind reducedMotion since there's nothing animated to begin with. */
@Composable
private fun PlainDarkBackground(modifier: Modifier = Modifier, previewBoost: Boolean = false) {
    val themeColors = LocalSpotVaultColors.current
    // themeColors.Void directly, not SpotVaultColors.Void — this ambient background is drawn
    // behind every screen for the entire time the app is open, and reading straight off the
    // CompositionLocal instead of the AMOLED-aware getter meant the app's flagship "true black"
    // toggle never actually applied to it: every screen kept painting the per-theme dark color
    // (e.g. a dark purple, not black) right over wherever a screen-level Void read (which does
    // go through the AMOLED-aware getter) had already painted real black underneath. Checking
    // ThemeState.isAmoled here directly — it's a real mutableStateOf, so this still redraws
    // correctly the instant the toggle changes — is the fix, same logic SpotVaultColors.Void
    // itself uses. previewBoost (Settings' pattern picker thumbnails) intentionally stays
    // Elevated regardless, same as before.
    val baseFill = if (previewBoost) themeColors.Elevated else if (ThemeState.isAmoled) Color.Black else themeColors.Void
    Box(
        modifier = modifier.fillMaxSize().drawWithCache {
            onDrawBehind {
                drawPatternBase(baseFill, themeColors, previewBoost)
            }
        }
    )
}

/** Settings picker — each cell is a small live-rendered preview of the actual pattern, using
 * the current theme's colors, so what you pick is (contrast-boosted, see [BackgroundPatternPreview])
 * recognizably close to what you'll see everywhere. Horizontally scrollable so eight options don't
 * eat a whole screen of vertical space in Appearance settings. */
@Composable
fun BackgroundPatternPicker(
    selectedPatternId: String,
    onPatternSelected: (BackgroundPattern) -> Unit,
    modifier: Modifier = Modifier,
    isLocked: (String) -> Boolean = { false },
    onLockedClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BackgroundPattern.entries.forEach { pattern ->
                val locked = isLocked(pattern.id)
                BackgroundPatternCell(
                    pattern = pattern,
                    selected = pattern.id == selectedPatternId,
                    locked = locked,
                    onClick = { if (locked) onLockedClick() else onPatternSelected(pattern) },
                    modifier = Modifier.width(128.dp)
                )
            }
        }
        TrailingScrollHint(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun BackgroundPatternCell(
    pattern: BackgroundPattern,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .alpha(if (locked) 0.45f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) SpotVaultColors.Teal else SpotVaultColors.Outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
                .clip(RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp))
        ) {
            BackgroundPatternPreview(pattern = pattern, modifier = Modifier.fillMaxSize())
            if (locked) {
                PremiumLockBadge(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) SpotVaultColors.Teal.copy(alpha = 0.14f) else SpotVaultColors.Elevated.copy(alpha = 0.6f))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    pattern.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) SpotVaultColors.Teal else SpotVaultColors.OnSurface,
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
        }
    }
}

@Composable
private fun BackgroundPatternPreview(pattern: BackgroundPattern, modifier: Modifier = Modifier) {
    when (pattern) {
        BackgroundPattern.GRADIENT_MESH -> GradientMeshBackground(modifier, previewBoost = true)
        BackgroundPattern.AURORA_BANDS -> AuroraBandsBackground(modifier, previewBoost = true)
        BackgroundPattern.HEX_TESS -> HexTessellationBackground(modifier, previewBoost = true)
        BackgroundPattern.LIGHT_LEAKS -> LightLeaksBackground(modifier, previewBoost = true)
        BackgroundPattern.WAVE_FLOW -> WaveFlowBackground(modifier, previewBoost = true)
        BackgroundPattern.NEBULA_MIST -> NebulaMistBackground(modifier, previewBoost = true)
        BackgroundPattern.SCAN_SWEEP -> ScanSweepBackground(modifier, previewBoost = true)
        BackgroundPattern.PIN_FIELD -> PinFieldBackground(modifier, previewBoost = true)
        BackgroundPattern.PLAIN_DARK -> PlainDarkBackground(modifier, previewBoost = true)
    }
}
