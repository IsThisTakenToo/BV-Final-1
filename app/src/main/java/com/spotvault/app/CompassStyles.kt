package com.spotvault.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/** Every selectable compass face — id is the persisted pref value. [activeMonths] (1-12 each)
 * marks a seasonal face — see [getSeasonallySortedItems]. Null for the year-round standard
 * faces. */
enum class CompassStyle(val id: String, val label: String, val activeMonths: List<Int>? = null) {
    CLASSIC("classic", "Classic Dial"),
    ARROW("arrow", "Nav Arrow"),
    DOT("dot", "Directional Dot"),
    SCI_FI("scifi", "Sci-Fi HUD"),
    PAPER_AIRPLANE("paper_airplane", "Paper Airplane"),
    BEACON_PULSE("beacon_pulse", "Beacon Pulse"),
    TRAIL_PUP("trail_pup", "Trail Pup"),
    LASER_DOT("laser_dot", "Laser Dot"),
    SKELETON_HAND("skeleton_hand", "Skeleton Hand", activeMonths = listOf(10)),
    REINDEER("reindeer", "Rudolph", activeMonths = listOf(11, 12));

    companion object {
        fun fromId(id: String?): CompassStyle {
            val migrated = when (id) {
                "star_chart", "wind_vane", "guide_star" -> LASER_DOT.id
                else -> id
            }
            return entries.firstOrNull { it.id == migrated } ?: CLASSIC
        }
    }
}

internal data class CompassDialPalette(
    val primary: Color,
    val primaryDeep: Color,
    val primaryBright: Color,
    val teal: Color,
    val deep: Color,
    val outline: Color,
    val muted: Color,
    val elevated: Color
)

@Composable
internal fun currentCompassDialPalette(): CompassDialPalette = CompassDialPalette(
    primary = SpotVaultColors.Primary,
    primaryDeep = SpotVaultColors.PrimaryDeep,
    primaryBright = SpotVaultColors.PrimaryBright,
    teal = SpotVaultColors.Teal,
    deep = SpotVaultColors.Deep,
    outline = SpotVaultColors.Outline,
    muted = SpotVaultColors.Muted,
    elevated = SpotVaultColors.Elevated
)

/** Renders the selected [CompassStyle]. [dialRotationDegrees] carries device heading (cardinal
 * ring/ticks), [needleRotationDegrees] carries bearing to the target (pointer element).
 * [compactPreview] tightens padding for settings thumbnails so N/E/S/W labels scale correctly. */
@Composable
fun VaultCompassDial(
    styleId: String,
    // Lambdas rather than plain Float — the real caller (CompassNavigationScreen) drives these
    // from a spring Animatable that updates on every animation frame while the heading changes.
    // Passing dialRotation.value/needleRotation.value directly used to mean reading .value in
    // that screen's own composable body, which recomposed the *entire* compass screen (distance
    // text, bearing label, top bar, everything) on every single animation frame, not just this
    // dial. Invoking these lambdas only inside the Canvas draw block below defers the read to
    // the draw phase instead, same as the other animation fixes this app has had — same physics,
    // same visuals, just recomposing far less.
    dialRotationDegrees: () -> Float,
    needleRotationDegrees: () -> Float,
    modifier: Modifier = Modifier,
    compactPreview: Boolean = false
) {
    val palette = currentCompassDialPalette()
    val style = CompassStyle.fromId(styleId)

    // Only Beacon Pulse and Sci-Fi actually use a continuous animation — pulsePhase/sweepPhase
    // used to be subscribed unconditionally for every style, which meant this whole composable
    // (Canvas included) recomposed at full animation frame rate forever, even for the six styles
    // that never read either value. That cost was multiplied in the Settings picker, which
    // renders all eight styles' dials at once as thumbnails — all eight canvases were
    // perpetually redrawing because of two animations only two of them actually use. Only
    // creating the animated state when the selected style needs it means the other styles now
    // only redraw when the bearing/heading itself changes, same visuals either way.
    // compactPreview means this is a Settings-picker thumbnail, not the live compass screen —
    // freeze on a static frame there instead of subscribing to the infinite transition, same
    // fix as the other Appearance-screen preview swatches (background patterns, vault icon).
    // State<Float>, not a `by`-delegated plain Float — reading .value only inside the Canvas
    // draw block below (same fix, same reasoning as dialRotationDegrees/needleRotationDegrees
    // above) instead of at this composable's own body scope. `by` here used to mean this whole
    // composable — palette/style resolution included, not just the Canvas — recomposed on every
    // single animation frame for as long as Beacon Pulse or Sci-Fi was selected, the exact bug
    // the lambda-based dial/needle rotation already exists to avoid.
    val pulsePhaseState = if (style == CompassStyle.BEACON_PULSE && !compactPreview) {
        val infinite = rememberInfiniteTransition(label = "compassPulse")
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
            label = "compassPulsePhase"
        )
    } else {
        null
    }
    val sweepPhaseState = if (style == CompassStyle.SCI_FI && !compactPreview) {
        val infinite = rememberInfiniteTransition(label = "compassSweep")
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
            label = "compassSweepPhase"
        )
    } else {
        null
    }

    Canvas(modifier = modifier.padding(if (compactPreview) 8.dp else 18.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        val dialDeg = dialRotationDegrees()
        val needleDeg = needleRotationDegrees()
        val pulsePhase = pulsePhaseState?.value ?: 0f
        val sweepPhase = sweepPhaseState?.value ?: 0f

        when (style) {
            CompassStyle.CLASSIC -> drawClassicCompass(center, radius, palette, dialDeg, needleDeg)
            CompassStyle.ARROW -> drawArrowCompass(center, radius, palette, dialDeg, needleDeg)
            CompassStyle.DOT -> drawDotCompass(center, radius, palette, dialDeg, needleDeg)
            CompassStyle.SCI_FI -> drawSciFiCompass(center, radius, palette, dialDeg, needleDeg, sweepPhase)
            CompassStyle.PAPER_AIRPLANE -> drawPaperAirplaneCompass(center, radius, palette, dialDeg, needleDeg)
            CompassStyle.BEACON_PULSE -> drawBeaconPulseCompass(center, radius, palette, dialDeg, needleDeg, pulsePhase)
            CompassStyle.TRAIL_PUP -> drawTrailPupCompass(center, radius, palette, dialDeg, needleDeg)
            CompassStyle.LASER_DOT -> drawLaserDotCompass(center, radius, palette, dialDeg, needleDeg)
            CompassStyle.SKELETON_HAND -> drawSkeletonHandCompass(center, radius, palette, dialDeg, needleDeg)
            CompassStyle.REINDEER -> drawReindeerCompass(center, radius, palette, dialDeg, needleDeg)
        }
    }
}

// ---------------------------------------------------------------------------------
// Classic — the original jeweled dial: bezel, fine ticks, N/E/S/W labels, diamond needle.
// ---------------------------------------------------------------------------------

private fun DrawScope.drawClassicCompass(
    center: Offset,
    radius: Float,
    palette: CompassDialPalette,
    dialRotationDegrees: Float,
    needleRotationDegrees: Float
) {
    drawCompassBezel(center, radius, palette)
    withTransform({ rotate(dialRotationDegrees, center) }) {
        drawCardinalTicks(center, radius, palette)
        drawCardinalLabels(center, radius, palette)
    }
    withTransform({ rotate(needleRotationDegrees, center) }) {
        drawNavigationNeedle(center, radius, palette)
    }
    drawCompassHub(center, radius, palette)
}

internal fun DrawScope.drawCompassBezel(center: Offset, radius: Float, palette: CompassDialPalette) {
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(palette.primaryDeep, palette.primaryBright, palette.teal, palette.primaryDeep),
            center = center
        ),
        radius = radius,
        center = center,
        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
    )
    drawCircle(color = palette.deep.copy(alpha = 0.88f), radius = radius * 0.92f, center = center)
    drawCircle(
        color = palette.outline.copy(alpha = 0.35f),
        radius = radius * 0.92f,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )
}

internal fun DrawScope.drawCardinalTicks(center: Offset, radius: Float, palette: CompassDialPalette) {
    // Low-RAM / Reduce Animations: half the minor ticks — same cardinals, less Canvas work.
    val step = if (ThemeState.lowRamDevice || ThemeState.reduceAnimations) 2 else 1
    for (i in 0 until 72 step step) {
        val angleRad = Math.toRadians(i * 5.0)
        val isMajor = i % 6 == 0
        val isCardinal = i % 18 == 0
        val innerR = when {
            isCardinal -> radius * 0.72f
            isMajor -> radius * 0.78f
            else -> radius * 0.84f
        }
        val stroke = when {
            isCardinal -> 3.dp.toPx()
            isMajor -> 2.dp.toPx()
            else -> 1.dp.toPx()
        }
        val color = when {
            isCardinal -> palette.teal
            isMajor -> palette.primaryBright.copy(alpha = 0.75f)
            else -> palette.muted.copy(alpha = 0.45f)
        }
        drawLine(
            color = color,
            start = Offset(center.x + innerR * cos(angleRad).toFloat(), center.y + innerR * sin(angleRad).toFloat()),
            end = Offset(center.x + radius * 0.88f * cos(angleRad).toFloat(), center.y + radius * 0.88f * sin(angleRad).toFloat()),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

// Reused across every call rather than allocated fresh per label — this runs every frame for as
// long as the live compass screen (CompassNavigationScreen) is open and the heading is changing,
// which for the app's core "find my car" flow can be the whole time a user is actually walking
// with it open. Mutating one Paint's properties per label instead of `Paint().apply {}`-ing four
// new ones every draw call removes real, measurable per-frame GC churn on the app's most
// safety/UX-critical screen. Safe to share: Compose draw calls for a given frame run sequentially
// on one thread, never concurrently.
private val cardinalLabelPaint = android.graphics.Paint().apply {
    textAlign = android.graphics.Paint.Align.CENTER
    isAntiAlias = true
}

internal fun DrawScope.drawCardinalLabels(center: Offset, radius: Float, palette: CompassDialPalette) {
    val northSize = (radius * 0.24f).coerceIn(7.sp.toPx(), 18.sp.toPx())
    val otherSize = (radius * 0.19f).coerceIn(6.sp.toPx(), 14.sp.toPx())
    val labelR = radius * if (radius < 32.dp.toPx()) 0.50f else 0.58f
    val labels = listOf(
        Triple("N", -90f, palette.primaryBright),
        Triple("E", 0f, palette.muted),
        Triple("S", 90f, palette.muted),
        Triple("W", 180f, palette.muted)
    )
    labels.forEach { (label, angleDeg, tint) ->
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val pos = Offset(center.x + labelR * cos(angleRad).toFloat(), center.y + labelR * sin(angleRad).toFloat())
        drawContext.canvas.nativeCanvas.apply {
            cardinalLabelPaint.color = tint.toArgb()
            cardinalLabelPaint.textSize = if (label == "N") northSize else otherSize
            cardinalLabelPaint.isFakeBoldText = label == "N"
            drawText(label, pos.x, pos.y + cardinalLabelPaint.textSize * 0.35f, cardinalLabelPaint)
        }
    }
}

internal fun DrawScope.drawNavigationNeedle(center: Offset, radius: Float, palette: CompassDialPalette) {
    val needleLen = radius * 0.62f
    val baseW = radius * 0.09f

    val tip = Offset(center.x, center.y - needleLen)
    val tail = Offset(center.x, center.y + needleLen * 0.28f)
    val left = Offset(center.x - baseW, center.y + baseW * 0.35f)
    val right = Offset(center.x + baseW, center.y + baseW * 0.35f)

    drawLine(
        brush = Brush.linearGradient(colors = listOf(palette.teal, palette.primaryBright), start = tail, end = tip),
        start = tail, end = tip, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round
    )
    drawLine(color = palette.muted.copy(alpha = 0.55f), start = center, end = tail, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)

    Path().apply {
        moveTo(tip.x, tip.y); lineTo(left.x, left.y); lineTo(right.x, right.y); close()
    }.also { path ->
        drawPath(path = path, brush = Brush.linearGradient(colors = listOf(palette.primaryBright, palette.teal), start = tip, end = center))
    }
}

internal fun DrawScope.drawCompassHub(center: Offset, radius: Float, palette: CompassDialPalette) {
    drawCircle(
        brush = Brush.radialGradient(colors = listOf(palette.elevated, palette.deep), center = center, radius = radius * 0.14f),
        radius = radius * 0.14f,
        center = center
    )
    drawCircle(color = palette.teal.copy(alpha = 0.85f), radius = radius * 0.05f, center = center)
}

// ---------------------------------------------------------------------------------
// Arrow — a bold single nav chevron, clean ring, only major/cardinal ticks.
// ---------------------------------------------------------------------------------

private fun DrawScope.drawArrowCompass(
    center: Offset,
    radius: Float,
    palette: CompassDialPalette,
    dialRotationDegrees: Float,
    needleRotationDegrees: Float
) {
    drawCircle(color = palette.deep.copy(alpha = 0.9f), radius = radius * 0.94f, center = center)
    drawCircle(
        color = palette.outline.copy(alpha = 0.5f),
        radius = radius * 0.94f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )

    withTransform({ rotate(dialRotationDegrees, center) }) {
        for (i in 0 until 12) {
            val angleRad = Math.toRadians(i * 30.0)
            val isCardinal = i % 3 == 0
            val innerR = radius * 0.80f
            val outerR = if (isCardinal) radius * 0.92f else radius * 0.87f
            drawLine(
                color = if (isCardinal) palette.teal else palette.muted.copy(alpha = 0.5f),
                start = Offset(center.x + innerR * cos(angleRad).toFloat(), center.y + innerR * sin(angleRad).toFloat()),
                end = Offset(center.x + outerR * cos(angleRad).toFloat(), center.y + outerR * sin(angleRad).toFloat()),
                strokeWidth = if (isCardinal) 3.dp.toPx() else 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawCardinalLabels(center, radius, palette)
    }

    withTransform({ rotate(needleRotationDegrees, center) }) {
        val len = radius * 0.72f
        val tip = Offset(center.x, center.y - len)
        val shoulderY = center.y - len * 0.42f
        val wingSpan = radius * 0.34f
        val shaftHalf = radius * 0.07f
        val tailY = center.y + len * 0.5f

        val head = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(center.x - wingSpan, shoulderY + wingSpan * 0.55f)
            lineTo(center.x - shaftHalf, shoulderY)
            lineTo(center.x + shaftHalf, shoulderY)
            lineTo(center.x + wingSpan, shoulderY + wingSpan * 0.55f)
            close()
        }
        drawPath(
            path = head,
            brush = Brush.linearGradient(colors = listOf(palette.primaryBright, palette.teal), start = tip, end = Offset(center.x, shoulderY))
        )
        drawRoundRect(
            brush = Brush.verticalGradient(colors = listOf(palette.teal, palette.primaryDeep)),
            topLeft = Offset(center.x - shaftHalf * 0.7f, shoulderY),
            size = androidx.compose.ui.geometry.Size(shaftHalf * 1.4f, tailY - shoulderY),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(shaftHalf * 0.7f)
        )
    }

    drawCircle(
        brush = Brush.radialGradient(colors = listOf(palette.elevated, palette.deep), center = center, radius = radius * 0.12f),
        radius = radius * 0.12f,
        center = center
    )
}

// ---------------------------------------------------------------------------------
// Dot — minimalist ring with a glowing orbiting dot and a thin directional spoke.
// ---------------------------------------------------------------------------------

private fun DrawScope.drawDotCompass(
    center: Offset,
    radius: Float,
    palette: CompassDialPalette,
    dialRotationDegrees: Float,
    needleRotationDegrees: Float
) {
    drawCircle(
        color = palette.outline.copy(alpha = 0.4f),
        radius = radius * 0.86f,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )

    withTransform({ rotate(dialRotationDegrees, center) }) {
        val ringStep = if (ThemeState.lowRamDevice || ThemeState.reduceAnimations) 2 else 1
        for (i in 0 until 36 step ringStep) {
            val angleRad = Math.toRadians(i * 10.0)
            val isCardinal = i % 9 == 0
            val innerR = radius * 0.80f
            drawLine(
                color = if (isCardinal) palette.teal.copy(alpha = 0.85f) else palette.muted.copy(alpha = 0.3f),
                start = Offset(center.x + innerR * cos(angleRad).toFloat(), center.y + innerR * sin(angleRad).toFloat()),
                end = Offset(center.x + radius * 0.86f * cos(angleRad).toFloat(), center.y + radius * 0.86f * sin(angleRad).toFloat()),
                strokeWidth = if (isCardinal) 2.5.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawCardinalLabels(center, radius, palette)
    }

    withTransform({ rotate(needleRotationDegrees, center) }) {
        val dotR = radius * 0.62f
        val dotPos = Offset(center.x, center.y - dotR)

        drawLine(
            brush = Brush.linearGradient(colors = listOf(Color.Transparent, palette.teal), start = center, end = dotPos),
            start = center,
            end = dotPos,
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(color = palette.teal.copy(alpha = 0.25f), radius = radius * 0.13f, center = dotPos)
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(Color.White, palette.teal, palette.primaryBright), center = dotPos, radius = radius * 0.075f),
            radius = radius * 0.075f,
            center = dotPos
        )
    }

    drawCircle(color = palette.muted.copy(alpha = 0.6f), radius = radius * 0.035f, center = center)
}

// ---------------------------------------------------------------------------------
// Sci-Fi — theme-matched HUD: jeweled bezel, corner brackets, radar sweep, reticle needle.
// ---------------------------------------------------------------------------------

private fun DrawScope.drawSciFiCompass(
    center: Offset,
    radius: Float,
    palette: CompassDialPalette,
    dialRotationDegrees: Float,
    needleRotationDegrees: Float,
    sweepPhase: Float
) {
    drawCompassBezel(center, radius, palette)
    drawCircle(color = palette.deep.copy(alpha = 0.9f), radius = radius * 0.86f, center = center)

    rotate(sweepPhase, center) {
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.Transparent,
                    palette.teal.copy(alpha = 0.45f),
                    palette.primaryBright.copy(alpha = 0.18f),
                    Color.Transparent
                ),
                center = center
            ),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = true,
            topLeft = Offset(center.x - radius * 0.84f, center.y - radius * 0.84f),
            size = androidx.compose.ui.geometry.Size(radius * 1.68f, radius * 1.68f)
        )
    }

    drawHudCornerBrackets(center, radius, palette)

    drawCircle(
        color = palette.teal.copy(alpha = 0.4f),
        radius = radius * 0.52f,
        center = center,
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
        )
    )

    for (seg in 0 until 8) {
        drawArc(
            color = palette.primaryBright.copy(alpha = 0.55f),
            startAngle = seg * 45f + 8f,
            sweepAngle = 28f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 0.78f, center.y - radius * 0.78f),
            size = androidx.compose.ui.geometry.Size(radius * 1.56f, radius * 1.56f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Butt)
        )
    }

    withTransform({ rotate(dialRotationDegrees, center) }) {
        for (i in 0 until 12) {
            val angleRad = Math.toRadians(i * 30.0)
            val isCardinal = i % 3 == 0
            val innerR = radius * 0.68f
            drawLine(
                color = if (isCardinal) palette.teal else palette.muted.copy(alpha = 0.45f),
                start = Offset(center.x + innerR * cos(angleRad).toFloat(), center.y + innerR * sin(angleRad).toFloat()),
                end = Offset(center.x + radius * 0.78f * cos(angleRad).toFloat(), center.y + radius * 0.78f * sin(angleRad).toFloat()),
                strokeWidth = if (isCardinal) 2.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawCardinalLabels(center, radius, palette)
    }

    withTransform({ rotate(needleRotationDegrees, center) }) {
        drawSciFiReticleNeedle(center, radius, palette)
    }

    drawCompassHub(center, radius, palette)
}

private fun DrawScope.drawHudCornerBrackets(center: Offset, radius: Float, palette: CompassDialPalette) {
    val arm = radius * 0.13f
    val stroke = 2.dp.toPx()
    val color = palette.primaryBright.copy(alpha = 0.9f)
    val offset = radius * 0.72f

    val corners = listOf(
        Offset(center.x - offset, center.y - offset),
        Offset(center.x + offset, center.y - offset),
        Offset(center.x + offset, center.y + offset),
        Offset(center.x - offset, center.y + offset)
    )
    corners.forEachIndexed { index, corner ->
        when (index) {
            0 -> {
                drawLine(color, corner, Offset(corner.x + arm, corner.y), stroke, StrokeCap.Square)
                drawLine(color, corner, Offset(corner.x, corner.y + arm), stroke, StrokeCap.Square)
            }
            1 -> {
                drawLine(color, corner, Offset(corner.x - arm, corner.y), stroke, StrokeCap.Square)
                drawLine(color, corner, Offset(corner.x, corner.y + arm), stroke, StrokeCap.Square)
            }
            2 -> {
                drawLine(color, corner, Offset(corner.x - arm, corner.y), stroke, StrokeCap.Square)
                drawLine(color, corner, Offset(corner.x, corner.y - arm), stroke, StrokeCap.Square)
            }
            else -> {
                drawLine(color, corner, Offset(corner.x + arm, corner.y), stroke, StrokeCap.Square)
                drawLine(color, corner, Offset(corner.x, corner.y - arm), stroke, StrokeCap.Square)
            }
        }
    }
}

private fun DrawScope.drawSciFiReticleNeedle(center: Offset, radius: Float, palette: CompassDialPalette) {
    val crossArm = radius * 0.22f
    drawLine(
        color = palette.outline.copy(alpha = 0.45f),
        start = Offset(center.x - crossArm, center.y),
        end = Offset(center.x + crossArm, center.y),
        strokeWidth = 1.dp.toPx()
    )
    drawLine(
        color = palette.outline.copy(alpha = 0.45f),
        start = Offset(center.x, center.y - crossArm),
        end = Offset(center.x, center.y + crossArm),
        strokeWidth = 1.dp.toPx()
    )

    val tipY = center.y - radius * 0.62f
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(palette.teal, palette.primaryBright),
            start = center,
            end = Offset(center.x, tipY)
        ),
        start = Offset(center.x, center.y - radius * 0.12f),
        end = Offset(center.x, tipY),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )

    val chevW = radius * 0.09f
    drawLine(
        color = palette.primaryBright,
        start = Offset(center.x - chevW, tipY + chevW * 0.9f),
        end = Offset(center.x, tipY),
        strokeWidth = 2.5.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = palette.primaryBright,
        start = Offset(center.x + chevW, tipY + chevW * 0.9f),
        end = Offset(center.x, tipY),
        strokeWidth = 2.5.dp.toPx(),
        cap = StrokeCap.Round
    )

    val reticleCenter = Offset(center.x, tipY - radius * 0.04f)
    drawCircle(
        color = palette.teal.copy(alpha = 0.85f),
        radius = radius * 0.042f,
        center = reticleCenter,
        style = Stroke(width = 1.5.dp.toPx())
    )
    drawCircle(color = palette.primaryBright.copy(alpha = 0.9f), radius = radius * 0.018f, center = reticleCenter)
}

// ---------------------------------------------------------------------------------
// Paper Airplane — theme-matched bezel + folded dart needle with a soft flight trail.
// ---------------------------------------------------------------------------------

private fun DrawScope.drawPaperAirplaneCompass(
    center: Offset,
    radius: Float,
    palette: CompassDialPalette,
    dialRotationDegrees: Float,
    needleRotationDegrees: Float
) {
    drawCompassBezel(center, radius, palette)

    withTransform({ rotate(dialRotationDegrees, center) }) {
        val ringStep = if (ThemeState.lowRamDevice || ThemeState.reduceAnimations) 2 else 1
        for (i in 0 until 36 step ringStep) {
            val angleRad = Math.toRadians(i * 10.0)
            val isCardinal = i % 9 == 0
            val innerR = radius * 0.78f
            drawLine(
                color = if (isCardinal) palette.teal.copy(alpha = 0.9f) else palette.muted.copy(alpha = 0.35f),
                start = Offset(center.x + innerR * cos(angleRad).toFloat(), center.y + innerR * sin(angleRad).toFloat()),
                end = Offset(center.x + radius * 0.86f * cos(angleRad).toFloat(), center.y + radius * 0.86f * sin(angleRad).toFloat()),
                strokeWidth = if (isCardinal) 2.5.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawCardinalLabels(center, radius, palette)
    }

    withTransform({ rotate(needleRotationDegrees, center) }) {
        drawThemePaperAirplane(center, radius, palette)
    }

    drawCompassHub(center, radius, palette)
}

private fun DrawScope.drawThemePaperAirplane(center: Offset, radius: Float, palette: CompassDialPalette) {
    // Top-down origami dart — unmistakable paper-plane silhouette pointing toward bearing.
    val nose = Offset(center.x, center.y - radius * 0.60f)
    val leftTip = Offset(center.x - radius * 0.44f, center.y + radius * 0.04f)
    val rightTip = Offset(center.x + radius * 0.44f, center.y + radius * 0.04f)
    val leftRear = Offset(center.x - radius * 0.10f, center.y + radius * 0.14f)
    val rightRear = Offset(center.x + radius * 0.10f, center.y + radius * 0.14f)
    val tail = Offset(center.x, center.y + radius * 0.22f)

    val body = Path().apply {
        moveTo(nose.x, nose.y)
        lineTo(leftTip.x, leftTip.y)
        lineTo(leftRear.x, leftRear.y)
        lineTo(tail.x, tail.y)
        lineTo(rightRear.x, rightRear.y)
        lineTo(rightTip.x, rightTip.y)
        close()
    }
    drawPath(
        path = body,
        brush = Brush.linearGradient(
            colors = listOf(palette.primaryBright, palette.teal, palette.primary),
            start = nose,
            end = tail
        )
    )

    // Left wing panel (slightly darker)
    val leftWing = Path().apply {
        moveTo(nose.x, nose.y)
        lineTo(leftTip.x, leftTip.y)
        lineTo(leftRear.x, leftRear.y)
        close()
    }
    drawPath(
        path = leftWing,
        brush = Brush.linearGradient(
            colors = listOf(palette.primaryBright.copy(alpha = 0.95f), palette.primaryDeep.copy(alpha = 0.88f)),
            start = nose,
            end = leftTip
        )
    )

    // Fold creases
    drawLine(
        color = palette.deep.copy(alpha = 0.55f),
        start = nose,
        end = leftTip,
        strokeWidth = 1.3.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = palette.deep.copy(alpha = 0.55f),
        start = nose,
        end = rightTip,
        strokeWidth = 1.3.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = palette.elevated.copy(alpha = 0.85f),
        start = nose,
        end = tail,
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Nose highlight
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.95f), palette.primaryBright),
            center = nose,
            radius = radius * 0.05f
        ),
        radius = radius * 0.035f,
        center = nose
    )
}

// ---------------------------------------------------------------------------------
// Trail Pup — a friendly dog silhouette points the way; paw-print ticks on the dial ring.
// ---------------------------------------------------------------------------------

private fun DrawScope.drawTrailPupCompass(
    center: Offset,
    radius: Float,
    palette: CompassDialPalette,
    dialRotationDegrees: Float,
    needleRotationDegrees: Float
) {
    drawCompassBezel(center, radius, palette)
    drawCircle(color = palette.deep.copy(alpha = 0.88f), radius = radius * 0.9f, center = center)

    withTransform({ rotate(dialRotationDegrees, center) }) {
        val ringStep = if (ThemeState.lowRamDevice || ThemeState.reduceAnimations) 2 else 1
        for (i in 0 until 36 step ringStep) {
            val angleRad = Math.toRadians(i * 10.0)
            val isCardinal = i % 9 == 0
            if (isCardinal) {
                val pawCenter = Offset(
                    center.x + radius * 0.82f * cos(angleRad).toFloat(),
                    center.y + radius * 0.82f * sin(angleRad).toFloat()
                )
                drawPawPrint(
                    center = pawCenter,
                    size = radius * 0.09f,
                    rotationDeg = Math.toDegrees(angleRad).toFloat() + 90f,
                    color = palette.teal.copy(alpha = 0.88f)
                )
            } else if (i % 3 == 0) {
                val innerR = radius * 0.8f
                drawLine(
                    color = palette.muted.copy(alpha = 0.35f),
                    start = Offset(center.x + innerR * cos(angleRad).toFloat(), center.y + innerR * sin(angleRad).toFloat()),
                    end = Offset(center.x + radius * 0.86f * cos(angleRad).toFloat(), center.y + radius * 0.86f * sin(angleRad).toFloat()),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        drawCardinalLabels(center, radius, palette)
    }

    withTransform({ rotate(needleRotationDegrees, center) }) {
        drawThemeDogPointer(center, radius, palette)
    }

    drawCircle(
        brush = Brush.radialGradient(colors = listOf(palette.elevated, palette.deep), center = center, radius = radius * 0.11f),
        radius = radius * 0.11f,
        center = center
    )
}

private fun DrawScope.drawPawPrint(center: Offset, size: Float, rotationDeg: Float, color: Color) {
    withTransform({
        translate(center.x, center.y)
        rotate(rotationDeg, pivot = Offset.Zero)
    }) {
        val padR = size * 0.28f
        val toeR = size * 0.16f
        val toeSpread = size * 0.34f
        val toeY = -size * 0.22f
        drawCircle(color = color, radius = padR, center = Offset(0f, size * 0.18f))
        for (i in -1..1) {
            drawCircle(
                color = color,
                radius = toeR,
                center = Offset(i * toeSpread, toeY)
            )
        }
    }
}

private fun DrawScope.drawThemeDogPointer(center: Offset, radius: Float, palette: CompassDialPalette) {
    // Small pup at the needle tip — leads the way without dominating the dial. Top-down and
    // left/right symmetric on purpose (like the paper-airplane needle) since this spins through
    // every angle as the bearing changes — a side-profile "walking" dog would tumble oddly
    // upside-down. Reads as a bird-dog "on point": nose out front, tail held straight out
    // behind. Deliberately kept to one solid-fill body path plus a handful of small shapes (no
    // per-frame gradient construction, no leg/torso path pile-up) since this redraws on every
    // heading update while the compass is open.
    // Anchor pulled in from the edge (was 0.68) and scale roughly 2.3x bigger (was 0.0032) — at
    // the old size the pup read as an unrecognizable smudge; this puts its silhouette on par
    // with the Paper Airplane/Arrow needles, which span most of the dial's radius.
    val tip = Offset(center.x, center.y - radius * 0.46f)
    val scale = radius * 0.0075f

    // Tiny dotted trail behind the pup
    drawLine(
        color = palette.muted.copy(alpha = 0.35f),
        start = Offset(center.x, center.y - radius * 0.08f),
        end = tip + Offset(0f, radius * 0.06f),
        strokeWidth = 1.2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())),
        cap = StrokeCap.Round
    )

    withTransform({
        translate(tip.x, tip.y)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        // One smooth outline: long narrow snout -> head -> lean neck -> deep chest -> tucked
        // waist -> hip -> long straight tail, mirrored across the centerline. That deep-chest,
        // tucked-waist proportion (rather than a uniformly round body) is what actually reads as
        // a lean pointer breed instead of a generic blob — a single gradient fill gives it some
        // roundness/depth. Still just one Brush built once per draw (not rebuilt in a loop), so
        // it costs nothing extra redrawing on every heading update.
        val body = Path().apply {
            moveTo(0f, -40f) // nose tip — long, narrow snout
            quadraticTo(5.5f, -37f, 6.5f, -31f)
            quadraticTo(7.5f, -26f, 10f, -23f) // head widens at the ears
            quadraticTo(9f, -18f, 6f, -13f) // neck leans in
            quadraticTo(13.5f, -6f, 14f, 3f) // chest — the deepest point
            quadraticTo(13f, 10f, 9f, 15f)
            quadraticTo(6f, 19f, 5f, 22f) // waist tucks in sharply — the "lean pointer" tell
            lineTo(2f, 44f) // tail, long and thin
            lineTo(-2f, 44f)
            lineTo(-5f, 22f)
            quadraticTo(-6f, 19f, -9f, 15f)
            quadraticTo(-13f, 10f, -14f, 3f)
            quadraticTo(-13.5f, -6f, -6f, -13f)
            quadraticTo(-9f, -18f, -10f, -23f)
            quadraticTo(-7.5f, -26f, -6.5f, -31f)
            quadraticTo(-5.5f, -37f, 0f, -40f)
            close()
        }
        drawPath(
            path = body,
            brush = Brush.linearGradient(
                colors = listOf(palette.primaryBright, palette.primary, palette.primaryDeep),
                start = Offset(-14f, -40f),
                end = Offset(14f, 44f)
            )
        )

        // Ears, swept back and alert — narrower and set further back than before, matching a
        // pointer's short, high-set ear rather than a floppy hound ear.
        val earShape = Path().apply {
            moveTo(-6f, -21f)
            quadraticTo(-14f, -18f, -12f, -8f)
            quadraticTo(-9f, -11f, -5f, -15f)
            close()
        }
        drawPath(earShape, color = palette.primaryDeep.copy(alpha = 0.92f))
        withTransform({ scale(-1f, 1f, pivot = Offset.Zero) }) {
            drawPath(earShape, color = palette.primaryDeep.copy(alpha = 0.92f))
        }

        // Snout highlight + nose tip
        drawPath(
            path = Path().apply {
                moveTo(0f, -40f)
                lineTo(-3f, -30f)
                lineTo(3f, -30f)
                close()
            },
            color = palette.teal.copy(alpha = 0.85f)
        )
        drawCircle(color = palette.deep, radius = 2.2f, center = Offset(0f, -38f))

        // Eyes, set on the head just behind the snout
        drawCircle(color = palette.deep, radius = 1.8f, center = Offset(-4f, -24f))
        drawCircle(color = palette.deep, radius = 1.8f, center = Offset(4f, -24f))
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 0.7f, center = Offset(-3.5f, -24.6f))
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 0.7f, center = Offset(4.5f, -24.6f))

        // Tail held straight out and slightly tapered — the "point" stance's other telltale,
        // never a curled wag.
        drawLine(
            color = palette.primaryDeep.copy(alpha = 0.9f),
            start = Offset(0f, 22f),
            end = Offset(0f, 47f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }
}

// ---------------------------------------------------------------------------------
// Laser Dot — a sniper-sight red dot at the bearing, fed by a dashed beam from a small
// emitter housing at the hub. One unmistakable bearing axis, deliberately silly.
// ---------------------------------------------------------------------------------

private fun DrawScope.drawLaserDotCompass(
    center: Offset,
    radius: Float,
    palette: CompassDialPalette,
    dialRotationDegrees: Float,
    needleRotationDegrees: Float
) {
    drawCircle(color = palette.deep.copy(alpha = 0.94f), radius = radius * 0.94f, center = center)
    drawCircle(color = palette.outline.copy(alpha = 0.4f), radius = radius * 0.94f, center = center, style = Stroke(width = 1.5.dp.toPx()))

    withTransform({ rotate(dialRotationDegrees, center) }) {
        val ringStep = if (ThemeState.lowRamDevice || ThemeState.reduceAnimations) 2 else 1
        for (i in 0 until 36 step ringStep) {
            val angleRad = Math.toRadians(i * 10.0)
            val isCardinal = i % 9 == 0
            val innerR = radius * 0.80f
            drawLine(
                color = if (isCardinal) palette.teal.copy(alpha = 0.85f) else palette.muted.copy(alpha = 0.3f),
                start = Offset(center.x + innerR * cos(angleRad).toFloat(), center.y + innerR * sin(angleRad).toFloat()),
                end = Offset(center.x + radius * 0.86f * cos(angleRad).toFloat(), center.y + radius * 0.86f * sin(angleRad).toFloat()),
                strokeWidth = if (isCardinal) 2.5.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawCardinalLabels(center, radius, palette)
    }

    withTransform({ rotate(needleRotationDegrees, center) }) {
        drawLaserDotPointer(center, radius, palette)
    }
}

private fun DrawScope.drawLaserDotPointer(center: Offset, radius: Float, palette: CompassDialPalette) {
    val tip = Offset(center.x, center.y - radius * 0.68f)
    val emitterR = radius * 0.13f

    // Emitter housing — small dark cylinder at the hub the beam appears to come from. A single
    // flat-shaded rounded rect, no per-frame gradient build.
    drawRoundRect(
        color = palette.elevated.copy(alpha = 0.95f),
        topLeft = Offset(center.x - emitterR * 0.55f, center.y - emitterR * 0.9f),
        size = androidx.compose.ui.geometry.Size(emitterR * 1.1f, emitterR * 1.8f),
        cornerRadius = CornerRadius(emitterR * 0.35f)
    )
    drawCircle(color = palette.teal.copy(alpha = 0.9f), radius = emitterR * 0.28f, center = Offset(center.x, center.y - emitterR * 0.9f))

    // Dashed beam from emitter to dot — reads as a laser sight, not a solid pointer shaft.
    drawLine(
        color = Color.Red.copy(alpha = 0.85f),
        start = Offset(center.x, center.y - emitterR * 0.9f),
        end = tip,
        strokeWidth = 1.6.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
        cap = StrokeCap.Round
    )

    // Soft red glow halo behind the dot
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Red.copy(alpha = 0.45f), Color.Transparent),
            center = tip,
            radius = radius * 0.16f
        ),
        radius = radius * 0.16f,
        center = tip
    )

    // The dot itself — hot white-red core, classic laser-sight look.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, Color.Red, Color(0xFFB00000)),
            center = tip,
            radius = radius * 0.06f
        ),
        radius = radius * 0.055f,
        center = tip
    )
    drawCircle(color = Color.White.copy(alpha = 0.9f), radius = radius * 0.016f, center = tip - Offset(radius * 0.012f, radius * 0.012f))
}

// ---------------------------------------------------------------------------------
// Skeleton Hand — a bony hand, index finger extended, pointing the way. Curled fist plus one
// long jointed finger reads clearly at any rotation without needing a side profile.
// ---------------------------------------------------------------------------------

private fun DrawScope.drawSkeletonHandCompass(
    center: Offset,
    radius: Float,
    palette: CompassDialPalette,
    dialRotationDegrees: Float,
    needleRotationDegrees: Float
) {
    drawCompassBezel(center, radius, palette)
    drawCircle(color = Color(0xFF1A1028).copy(alpha = 0.92f), radius = radius * 0.9f, center = center)

    withTransform({ rotate(dialRotationDegrees, center) }) {
        // Faint cobweb spokes — static, cheap lines only
        val webColor = Color(0xFF9B30FF).copy(alpha = 0.18f)
        for (i in 0 until 6) {
            val a = Math.toRadians(i * 60.0)
            drawLine(
                color = webColor,
                start = center,
                end = Offset(
                    center.x + radius * 0.82f * cos(a).toFloat(),
                    center.y + radius * 0.82f * sin(a).toFloat()
                ),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        for (ring in 1..3) {
            drawCircle(
                color = webColor.copy(alpha = 0.12f),
                radius = radius * (0.28f + ring * 0.18f),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        drawCardinalTicks(center, radius, palette)
        drawCardinalLabels(center, radius, palette)
    }

    withTransform({ rotate(needleRotationDegrees, center) }) {
        drawSkeletonHandPointer(center, radius, palette)
    }

    drawCompassHub(center, radius, palette)
}

private fun DrawScope.drawSkeletonHandPointer(center: Offset, radius: Float, palette: CompassDialPalette) {
    val bone = Color(0xFFF4EDE0)
    val boneShadow = Color(0xFFC8BBA0)
    val joint = Color(0xFF8A7A62)
    val eerie = Color(0xFF5CFF8A)

    val tip = Offset(center.x, center.y - radius * 0.50f)
    val scale = radius * 0.0065f

    withTransform({
        translate(tip.x, tip.y)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        // Fingertip beacon — unmistakable "this way"
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(eerie.copy(alpha = 0.55f), Color.Transparent),
                center = Offset.Zero,
                radius = 16f
            ),
            radius = 16f,
            center = Offset.Zero
        )

        // Forearm rising from the hub
        drawRoundRect(
            brush = Brush.linearGradient(listOf(boneShadow, bone), start = Offset(-5f, 42f), end = Offset(5f, 58f)),
            topLeft = Offset(-6f, 42f),
            size = Size(12f, 18f),
            cornerRadius = CornerRadius(4f)
        )

        // Palm — blocky Halloween hand
        val palm = Path().apply {
            moveTo(-11f, 42f)
            lineTo(-12f, 18f)
            quadraticTo(-10f, 8f, -4f, 6f)
            lineTo(6f, 6f)
            quadraticTo(12f, 8f, 13f, 18f)
            lineTo(12f, 42f)
            close()
        }
        drawPath(palm, brush = Brush.linearGradient(listOf(bone, boneShadow), start = Offset(0f, 6f), end = Offset(0f, 42f)))

        // Curled fingers (middle, ring, pinky) — knuckle bumps along palm
        listOf(-7f, 0f, 7f).forEach { x ->
            drawRoundRect(
                color = boneShadow.copy(alpha = 0.85f),
                topLeft = Offset(x - 3.5f, 14f),
                size = Size(7f, 16f),
                cornerRadius = CornerRadius(3.5f)
            )
            drawCircle(color = joint, radius = 2.2f, center = Offset(x, 14f))
        }

        // Thumb — hooked across palm
        val thumb = Path().apply {
            moveTo(10f, 28f)
            quadraticTo(18f, 22f, 17f, 12f)
            quadraticTo(16f, 6f, 11f, 8f)
            quadraticTo(8f, 14f, 9f, 22f)
            close()
        }
        drawPath(thumb, color = boneShadow)

        // Index finger — three phalanges, pointing straight ahead (bearing tip at origin)
        val segments = listOf(
            Triple(Offset(-4f, 6f), Offset(-4.5f, -14f), 8f),
            Triple(Offset(-4.5f, -14f), Offset(-5f, -28f), 6.5f),
            Triple(Offset(-5f, -28f), Offset(-5f, -38f), 5f)
        )
        segments.forEach { (from, to, w) ->
            drawLine(color = bone, start = from, end = to, strokeWidth = w, cap = StrokeCap.Round)
            drawCircle(color = joint, radius = w * 0.38f, center = from)
        }
        drawCircle(color = bone, radius = 3f, center = Offset(-5f, -38f))
        drawCircle(color = eerie.copy(alpha = 0.85f), radius = 2.2f, center = Offset(-5f, -39f))

        // Crisp bone highlight along index finger
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(-2.5f, 4f),
            end = Offset(-3.5f, -36f),
            strokeWidth = 1.4f,
            cap = StrokeCap.Round
        )
    }
}

// ---------------------------------------------------------------------------------
// Reindeer — front-on head, antlers spread wide near the base, glowing red Rudolph nose
// right at the tip. Kept front-facing (not a sleigh silhouette) so it reads correctly no
// matter which way the needle has spun.
// ---------------------------------------------------------------------------------

private fun DrawScope.drawReindeerCompass(
    center: Offset,
    radius: Float,
    palette: CompassDialPalette,
    dialRotationDegrees: Float,
    needleRotationDegrees: Float
) {
    drawCompassBezel(center, radius, palette)
    drawCircle(color = Color(0xFF0E1A28).copy(alpha = 0.90f), radius = radius * 0.9f, center = center)

    withTransform({ rotate(dialRotationDegrees, center) }) {
        // Snow-dusted ticks
        val ringStep = if (ThemeState.lowRamDevice || ThemeState.reduceAnimations) 2 else 1
        for (i in 0 until 36 step ringStep) {
            val angleRad = Math.toRadians(i * 10.0)
            val isCardinal = i % 9 == 0
            val innerR = radius * 0.78f
            val outerR = radius * 0.86f
            val tickColor = if (isCardinal) Color(0xFFE8F4FF).copy(alpha = 0.9f) else palette.muted.copy(alpha = 0.35f)
            drawLine(
                color = tickColor,
                start = Offset(center.x + innerR * cos(angleRad).toFloat(), center.y + innerR * sin(angleRad).toFloat()),
                end = Offset(center.x + outerR * cos(angleRad).toFloat(), center.y + outerR * sin(angleRad).toFloat()),
                strokeWidth = if (isCardinal) 2.5.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round
            )
            if (isCardinal) {
                val cap = Offset(center.x + outerR * cos(angleRad).toFloat(), center.y + outerR * sin(angleRad).toFloat())
                drawCircle(color = Color.White.copy(alpha = 0.75f), radius = 2.2f, center = cap)
            }
        }
        drawCardinalLabels(center, radius, palette)
    }

    withTransform({ rotate(needleRotationDegrees, center) }) {
        drawReindeerPointer(center, radius, palette)
    }

    drawCompassHub(center, radius, palette)
}

private fun DrawScope.drawReindeerPointer(center: Offset, radius: Float, palette: CompassDialPalette) {
    val furLight = Color(0xFFC47A42)
    val furMid = Color(0xFF9A5528)
    val furDeep = Color(0xFF6B3818)
    val muzzle = Color(0xFFF2DCC8)
    val antler = Color(0xFFE8D5B5)
    val antlerShadow = Color(0xFFBFA888)

    val tip = Offset(center.x, center.y - radius * 0.48f)
    val scale = radius * 0.0062f

    withTransform({
        translate(tip.x, tip.y)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        // Antlers — wide, unmistakable silhouette
        val antlerLeft = Path().apply {
            moveTo(-3f, 4f)
            cubicTo(-10f, -2f, -14f, -12f, -18f, -24f)
            lineTo(-22f, -30f)
            moveTo(-14f, -12f)
            lineTo(-8f, -22f)
            moveTo(-12f, -4f)
            lineTo(-20f, -6f)
        }
        drawPath(antlerLeft, color = antlerShadow, style = Stroke(width = 3.6f, cap = StrokeCap.Round))
        withTransform({ scale(-1f, 1f, pivot = Offset.Zero) }) {
            drawPath(antlerLeft, color = antlerShadow, style = Stroke(width = 3.6f, cap = StrokeCap.Round))
        }
        listOf(Offset(-22f, -30f), Offset(-8f, -22f), Offset(-20f, -6f)).forEach { p ->
            drawCircle(color = antler, radius = 2f, center = p)
            withTransform({ scale(-1f, 1f, pivot = Offset.Zero) }) {
                drawCircle(color = antler, radius = 2f, center = p)
            }
        }

        // Ears
        drawOval(color = furDeep, topLeft = Offset(-18f, 0f), size = Size(9f, 6f))
        drawOval(color = furDeep, topLeft = Offset(9f, 0f), size = Size(9f, 6f))
        drawOval(color = Color(0xFF8B4518), topLeft = Offset(-15f, 1f), size = Size(5f, 4f))
        drawOval(color = Color(0xFF8B4518), topLeft = Offset(10f, 1f), size = Size(5f, 4f))

        // Head
        val head = Path().apply {
            moveTo(0f, -42f)
            cubicTo(8f, -38f, 14f, -28f, 14f, -14f)
            cubicTo(14f, 0f, 8f, 10f, 0f, 12f)
            cubicTo(-8f, 10f, -14f, 0f, -14f, -14f)
            cubicTo(-14f, -28f, -8f, -38f, 0f, -42f)
            close()
        }
        drawPath(
            path = head,
            brush = Brush.radialGradient(
                colors = listOf(furLight, furMid, furDeep),
                center = Offset(0f, -16f),
                radius = 30f
            )
        )

        // White muzzle patch
        val muzzlePatch = Path().apply {
            moveTo(0f, -42f)
            cubicTo(6f, -36f, 7f, -28f, 5f, -22f)
            cubicTo(-5f, -22f, -7f, -28f, 0f, -42f)
            close()
        }
        drawPath(muzzlePatch, color = muzzle.copy(alpha = 0.95f))

        // Eyes — big, friendly, obvious forward gaze
        drawCircle(color = Color(0xFF2A1408), radius = 3.2f, center = Offset(-7f, -16f))
        drawCircle(color = Color(0xFF2A1408), radius = 3.2f, center = Offset(7f, -16f))
        drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 1.2f, center = Offset(-6.2f, -16.8f))
        drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 1.2f, center = Offset(7.8f, -16.8f))

        // Rosy cheeks
        drawCircle(color = Color(0xFFFF8A80).copy(alpha = 0.35f), radius = 4f, center = Offset(-10f, -10f))
        drawCircle(color = Color(0xFFFF8A80).copy(alpha = 0.35f), radius = 4f, center = Offset(10f, -10f))

        // Festive scarf
        drawRoundRect(
            color = Color(0xFFE0233D),
            topLeft = Offset(-14f, 8f),
            size = Size(28f, 7f),
            cornerRadius = CornerRadius(3f)
        )
        drawRoundRect(
            color = Color(0xFF1FA34D),
            topLeft = Offset(-14f, 15f),
            size = Size(28f, 4f),
            cornerRadius = CornerRadius(2f)
        )

        // Rudolph nose — blazing red beacon at the tip
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF8A75).copy(alpha = 0.7f), Color.Transparent),
                center = Offset(0f, -43f),
                radius = 14f
            ),
            radius = 14f,
            center = Offset(0f, -43f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFE0D8), Color(0xFFFF3D2E), Color(0xFFB01010)),
                center = Offset(-1f, -44f),
                radius = 7f
            ),
            radius = 6f,
            center = Offset(0f, -43f)
        )
        drawCircle(color = Color.White.copy(alpha = 0.75f), radius = 1.6f, center = Offset(-2f, -45f))
    }
}

// ---------------------------------------------------------------------------------
// Beacon Pulse — original design: a lighthouse-style directional beam over an outward-
// pulsing radar beacon, matching the app's own "beacon" branding.
// ---------------------------------------------------------------------------------

private fun DrawScope.drawBeaconPulseCompass(
    center: Offset,
    radius: Float,
    palette: CompassDialPalette,
    dialRotationDegrees: Float,
    needleRotationDegrees: Float,
    pulsePhase: Float
) {
    drawCircle(color = palette.deep.copy(alpha = 0.92f), radius = radius * 0.94f, center = center)
    drawCircle(color = palette.outline.copy(alpha = 0.3f), radius = radius * 0.94f, center = center, style = Stroke(width = 1.dp.toPx()))

    // Two continuously-expanding pulse rings, offset half a cycle apart.
    listOf(pulsePhase, (pulsePhase + 0.5f) % 1f).forEach { phase ->
        val ringR = radius * (0.18f + phase * 0.68f)
        val alpha = (1f - phase) * 0.5f
        drawCircle(color = palette.teal.copy(alpha = alpha), radius = ringR, center = center, style = Stroke(width = 2.dp.toPx()))
    }

    withTransform({ rotate(dialRotationDegrees, center) }) {
        val ringStep = if (ThemeState.lowRamDevice || ThemeState.reduceAnimations) 2 else 1
        for (i in 0 until 36 step ringStep) {
            val angleRad = Math.toRadians(i * 10.0)
            val isCardinal = i % 9 == 0
            val innerR = radius * 0.82f
            drawLine(
                color = if (isCardinal) palette.teal.copy(alpha = 0.85f) else palette.muted.copy(alpha = 0.3f),
                start = Offset(center.x + innerR * cos(angleRad).toFloat(), center.y + innerR * sin(angleRad).toFloat()),
                end = Offset(center.x + radius * 0.88f * cos(angleRad).toFloat(), center.y + radius * 0.88f * sin(angleRad).toFloat()),
                strokeWidth = if (isCardinal) 2.5.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawCardinalLabels(center, radius, palette)
    }

    // Lighthouse-style directional beam wedge pointing at the bearing.
    withTransform({ rotate(needleRotationDegrees, center) }) {
        val beamLen = radius * 0.66f
        drawArc(
            brush = Brush.radialGradient(
                colors = listOf(palette.primaryBright.copy(alpha = 0.55f), Color.Transparent),
                center = center,
                radius = beamLen
            ),
            startAngle = -105f,
            sweepAngle = 30f,
            useCenter = true,
            topLeft = Offset(center.x - beamLen, center.y - beamLen),
            size = androidx.compose.ui.geometry.Size(beamLen * 2f, beamLen * 2f)
        )
        drawLine(
            brush = Brush.linearGradient(colors = listOf(palette.teal, Color.Transparent), start = center, end = Offset(center.x, center.y - beamLen)),
            start = center,
            end = Offset(center.x, center.y - beamLen),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    drawCircle(
        brush = Brush.radialGradient(colors = listOf(Color.White, palette.teal, palette.primaryDeep), center = center, radius = radius * 0.13f),
        radius = radius * 0.13f,
        center = center
    )
}

/** Settings picker — a live-rendered preview of each style, using the current theme's colors,
 * so what you pick is exactly what you'll see on the compass screen. */
@Composable
fun CompassStylePicker(
    selectedStyleId: String,
    onStyleSelected: (CompassStyle) -> Unit,
    modifier: Modifier = Modifier,
    isLocked: (String) -> Boolean = { false },
    onLockedClick: () -> Unit = {}
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val seasonallySortedStyles = remember {
        getSeasonallySortedItems(CompassStyle.entries) { it.activeMonths }
    }
    // Normalized through fromId(), not a raw id == id comparison — same fix
    // SplashStylePicker/FoundSplashStylePicker already have. CompassStyle.fromId() migrates
    // retired ids ("wind_vane", "guide_star") to LASER_DOT, but a raw comparison here never
    // matched that migrated id against anything in CompassStyle.entries, so anyone whose
    // stored preference was still one of those retired ids (a pre-rename install, or a
    // restored backup) saw no card highlighted at all here — even though the live compass
    // screen itself renders Laser Dot correctly, since it calls fromId() internally.
    val normalizedSelectedId = CompassStyle.fromId(selectedStyleId).id
    Box(modifier = modifier.fillMaxWidth()) {
        androidx.compose.foundation.lazy.LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(seasonallySortedStyles.size, key = { seasonallySortedStyles[it].id }) { index ->
                val style = seasonallySortedStyles[index]
                val selected = style.id == normalizedSelectedId
                val locked = isLocked(style.id)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(76.dp)) {
                        Column(
                            modifier = Modifier
                                .size(76.dp)
                                .alpha(if (locked) 0.45f else 1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(SpotVaultColors.Elevated.copy(alpha = 0.55f))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) SpotVaultColors.Teal else SpotVaultColors.Outline.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                // The label Text below sits outside this clickable Column as a separate
                                // sibling, so (unlike the color-theme swatches, which merge their label
                                // in) TalkBack had nothing to announce here but an unlabeled button.
                                .semantics {
                                    contentDescription = if (locked) "${style.label}, Premium" else style.label
                                    this.selected = selected
                                    role = Role.RadioButton
                                }
                                .clickable { if (locked) onLockedClick() else onStyleSelected(style) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            VaultCompassDial(
                                styleId = style.id,
                                dialRotationDegrees = { 0f },
                                needleRotationDegrees = { 55f },
                                modifier = Modifier.size(76.dp),
                                compactPreview = true
                            )
                        }
                        if (locked) {
                            PremiumLockBadge(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
                        }
                    }
                    Text(
                        text = style.label,
                        fontSize = 10.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) SpotVaultColors.Teal else SpotVaultColors.Muted,
                        maxLines = 2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }
        TrailingScrollHint(listState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}
