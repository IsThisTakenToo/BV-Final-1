package com.spotvault.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private val WheelHueColors = listOf(
    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
)

/** A hue/saturation wheel (tap or drag anywhere on the disc) plus a brightness slider —
 * the full HSV range, not just a preset swatch list. [onColorChange] fires live as the user
 * drags, so a preview elsewhere on screen can update in real time. */
@Composable
fun ColorWheelPicker(
    label: String,
    selectedColor: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    wheelSize: Dp = 140.dp,
    compact: Boolean = false,
    blackShortcut: Boolean = false,
    showLabel: Boolean = true
) {
    val resolvedWheelSize = if (compact) minOf(wheelSize, 104.dp) else wheelSize
    val labelSize = if (compact) 12.sp else 13.sp
    val sliderHeight = if (compact) 24.dp else 32.dp

    val hsv = remember {
        val out = FloatArray(3)
        android.graphics.Color.colorToHSV(selectedColor.toArgb(), out)
        out
    }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    var lastAppliedColor by remember { mutableStateOf(selectedColor) }

    // Resyncs hue/saturation/value whenever selectedColor changes for a reason other than this
    // picker's own last onColorChange — e.g. re-tapping "Custom" to reset in-progress edits back
    // to the saved color while this same picker instance stays mounted (showCustomPicker doesn't
    // flip, so the wheel is never torn down/recreated to pick up a fresh initial value on its
    // own). Deliberately compared against lastAppliedColor rather than keying remember() on
    // selectedColor directly — that would also round-trip HSV -> RGB -> HSV on every self-driven
    // drag update, and hue is undefined at saturation 0, so a drag passing through the wheel's
    // center could have its hue snap unpredictably mid-gesture.
    if (selectedColor != lastAppliedColor) {
        val out = FloatArray(3)
        android.graphics.Color.colorToHSV(selectedColor.toArgb(), out)
        hue = out[0]
        saturation = out[1]
        value = out[2]
        lastAppliedColor = selectedColor
    }

    fun currentColor(): Color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))

    fun emitColor(color: Color) {
        lastAppliedColor = color
        onColorChange(color)
    }

    fun applyBlack() {
        value = 0f
        emitColor(Color.Black)
    }

    fun updateFromOffset(offset: Offset, radiusPx: Float, center: Offset) {
        val dx = offset.x - center.x
        val dy = offset.y - center.y
        val dist = hypot(dx, dy).coerceAtMost(radiusPx)
        if (blackShortcut && dist < radiusPx * 0.13f) {
            applyBlack()
            return
        }
        var angle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        if (angle < 0f) angle += 360f
        hue = angle
        saturation = (dist / radiusPx).coerceIn(0f, 1f)
        if (value <= 0f) value = 1f
        emitColor(currentColor())
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showLabel) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    color = SpotVaultColors.Muted,
                    fontSize = labelSize,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (blackShortcut) {
                    Box(
                        modifier = Modifier
                            .size(if (compact) 20.dp else 24.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(
                                1.dp,
                                SpotVaultColors.Outline.copy(alpha = 0.55f),
                                CircleShape
                            )
                            .semantics {
                                contentDescription = "Set to black"
                                role = Role.Button
                            }
                            .clickable { applyBlack() },
                        contentAlignment = Alignment.Center
                    ) {}
                }
            }
        }
        Box(
            modifier = Modifier.padding(top = if (compact) 4.dp else 8.dp, bottom = if (compact) 2.dp else 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(resolvedWheelSize)
                    // A screen-reader label alone can't make this operable — TalkBack has no
                    // equivalent of this custom drag gesture, so announcing it at least tells a
                    // TalkBack user what the element is and where the black-shortcut swatch above
                    // covers the one color this control can't otherwise reach non-visually.
                    .semantics {
                        contentDescription = "Color wheel. Drag to choose a hue and saturation; " +
                            "use the brightness slider below and the black swatch for black."
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val radiusPx = size.width / 2f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            updateFromOffset(down.position, radiusPx, center)
                            down.consume()
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { change ->
                                    if (change.pressed) {
                                        updateFromOffset(change.position, radiusPx, center)
                                        change.consume()
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            ) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(brush = Brush.sweepGradient(WheelHueColors, center = center), radius = radius, center = center)
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color.White, Color.Transparent), center = center, radius = radius),
                    radius = radius,
                    center = center
                )
                if (blackShortcut) {
                    drawCircle(
                        color = Color.Black,
                        radius = radius * 0.11f,
                        center = center
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                            center = center,
                            radius = radius * 0.22f
                        ),
                        radius = radius * 0.22f,
                        center = center
                    )
                }
                val angleRad = Math.toRadians(hue.toDouble())
                val dotR = radius * saturation
                val dotPos = Offset(center.x + (dotR * cos(angleRad)).toFloat(), center.y + (dotR * sin(angleRad)).toFloat())
                drawCircle(color = Color.Black.copy(alpha = 0.35f), radius = 7.dp.toPx(), center = dotPos, style = Stroke(width = 2.dp.toPx()))
                drawCircle(color = Color.White, radius = 7.dp.toPx(), center = dotPos, style = Stroke(width = 1.dp.toPx()))
            }
        }
        val brightColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation.coerceAtLeast(0.01f), 1f)))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sliderHeight),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 3.dp else 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.horizontalGradient(listOf(Color.Black, brightColor)))
            )
            Slider(
                value = value,
                onValueChange = { value = it; emitColor(currentColor()) },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = SpotVaultColors.Teal,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}
