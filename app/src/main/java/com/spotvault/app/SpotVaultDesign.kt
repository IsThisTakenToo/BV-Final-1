package com.spotvault.app
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** SpotVault premium palette — purple + teal vault aesthetic */
object SpotVaultColors {
    val Void: Color @androidx.compose.runtime.Composable get() = if (ThemeState.isAmoled) Color.Black else LocalSpotVaultColors.current.Void
    val Deep: Color @androidx.compose.runtime.Composable get() = if (ThemeState.isAmoled) Color.Black else LocalSpotVaultColors.current.Deep
    val Surface: Color @androidx.compose.runtime.Composable get() = if (ThemeState.isAmoled) Color(0xFF080808) else LocalSpotVaultColors.current.Surface
    fun updateAmoled(isAmoled: Boolean) {
        ThemeState.isAmoled = isAmoled
    }
    
    val Elevated: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.Elevated
    val Glass: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.Glass
    val Primary: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.Primary
    val PrimaryBright: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.PrimaryBright
    val PrimaryDeep: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.PrimaryDeep
    val Teal: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.Teal
    val TealSoft: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.TealSoft
    val TealDeep: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.TealDeep
    val Ink: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.Ink
    /** Labels and icons on filled glass action buttons — white reads best on the polished shell. */
    val ButtonLabel: Color = Color.White
    val OnSurface: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.OnSurface
    val Muted: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.Muted
    val Outline: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.Outline
    val Danger: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.Danger
    /** Text/icon color for content sitting on a Primary-colored background — auto-contrasts by
     * luminance, unless the custom theme has an explicit override set (Settings > Appearance >
     * custom theme > Primary Button Text), for when auto-contrast doesn't match what someone
     * actually wants after picking an unusual color from the wheel. */
    val OnPrimary: Color @androidx.compose.runtime.Composable get() {
        val override = ThemeState.customOnPrimaryArgb
        if (ThemeState.currentTheme == "custom" && override != null) return Color(override)
        return if (LocalSpotVaultColors.current.Primary.luminance() > 0.4f) LocalSpotVaultColors.current.Ink else androidx.compose.ui.graphics.Color.White
    }
    /** Guaranteed-readable text/icon color for content sitting on a Teal-colored background —
     * unlike the per-theme fixed [Ink], this checks Teal's actual brightness so a dark custom
     * accent color never pairs with dark text, unless overridden (see [OnPrimary]). */
    val OnTeal: Color @androidx.compose.runtime.Composable get() {
        val override = ThemeState.customOnAccentArgb
        if (ThemeState.currentTheme == "custom" && override != null) return Color(override)
        return if (LocalSpotVaultColors.current.Teal.luminance() > 0.4f) LocalSpotVaultColors.current.Ink else androidx.compose.ui.graphics.Color.White
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SpotVaultColors.Elevated.copy(alpha = 0.95f),
                        SpotVaultColors.Surface.copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        SpotVaultColors.PrimaryBright.copy(alpha = 0.45f),
                        SpotVaultColors.Teal.copy(alpha = 0.2f),
                        SpotVaultColors.Outline.copy(alpha = 0.25f)
                    )
                ),
                shape = shape
            ),
        content = content
    )
}

@Composable
fun SpotVaultButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = spotVaultButtonShape(),
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = SpotVaultColors.Primary,
        contentColor = SpotVaultColors.ButtonLabel
    ),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val hapticContext = LocalContext.current
    val hapticPrefs = remember { hapticContext.getSharedPreferences("SpotVaultPrefs", android.content.Context.MODE_PRIVATE) }
    val wrappedOnClick: () -> Unit = { performAppHaptic(hapticContext, hapticPrefs); AppSounds.playClickSound(hapticContext, hapticPrefs); onClick() }
    if (ThemeState.buttonStyle == "wild") {
        WildStyledButton(
            onClick = wrappedOnClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            fillStyle = WildButtonFillStyle.Solid,
            containerColor = colors.containerColor,
            contentColor = colors.contentColor,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content
        )
    } else {
        GlassStyledButton(
            onClick = wrappedOnClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            fillStyle = GlassButtonFillStyle.Solid,
            containerColor = colors.containerColor,
            contentColor = colors.contentColor,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content
        )
    }
}

@Composable
fun SpotVaultOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = spotVaultButtonShape(),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.Teal),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val hapticContext = LocalContext.current
    val hapticPrefs = remember { hapticContext.getSharedPreferences("SpotVaultPrefs", android.content.Context.MODE_PRIVATE) }
    val wrappedOnClick: () -> Unit = { performAppHaptic(hapticContext, hapticPrefs); AppSounds.playClickSound(hapticContext, hapticPrefs); onClick() }
    if (ThemeState.buttonStyle == "wild") {
        WildStyledButton(
            onClick = wrappedOnClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            fillStyle = WildButtonFillStyle.Outlined,
            containerColor = SpotVaultColors.Surface.copy(alpha = 0.72f),
            contentColor = colors.contentColor,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content
        )
    } else {
        GlassStyledButton(
            onClick = wrappedOnClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            fillStyle = GlassButtonFillStyle.Outlined,
            containerColor = colors.contentColor,
            contentColor = colors.contentColor,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content
        )
    }
}

private enum class GlassButtonFillStyle { Solid, Outlined }

private enum class WildButtonFillStyle { Solid, Outlined }

private fun colorsSimilar(a: Color, b: Color, threshold: Float = 0.14f): Boolean {
    return kotlin.math.abs(a.red - b.red) +
        kotlin.math.abs(a.green - b.green) +
        kotlin.math.abs(a.blue - b.blue) < threshold
}

private fun isTealButtonColor(containerColor: Color, themeColors: SpotVaultColorScheme): Boolean {
    return colorsSimilar(containerColor, themeColors.Teal) ||
        colorsSimilar(containerColor, themeColors.TealSoft, 0.16f) ||
        colorsSimilar(containerColor, themeColors.TealDeep, 0.18f)
}

@Composable
private fun solidButtonLabelColor(containerColor: Color): Color {
    val themeColors = LocalSpotVaultColors.current
    return if (isTealButtonColor(containerColor, themeColors)) {
        SpotVaultColors.OnTeal
    } else if (colorsSimilar(containerColor, themeColors.Primary, 0.16f)) {
        // Every non-Teal solid button used to fall straight through to a hardcoded white here,
        // which assumed Primary is always some dark, saturated color — true for every theme
        // except Yin Yang, whose Primary is a near-white gray. That made Snap, Quick Pin, and
        // Found render white text on a near-white button there. OnPrimary already does the real
        // luminance check (plus honors a custom theme's explicit override), so routing the actual
        // Primary case through it instead of the flat fallback fixes this without touching every
        // call site that passes containerColor = SpotVaultColors.Primary.
        SpotVaultColors.OnPrimary
    } else {
        SpotVaultColors.ButtonLabel
    }
}

/** Same-hue palette for a raised physical button — saturated face, beveled edges. */
private data class PhysicalButtonPalette(
    val faceTop: Color,
    val face: Color,
    val faceBottom: Color,
    val wall: Color,
    val bevelLight: Color,
    val bevelDark: Color,
    val castShadow: Color
)

private fun physicalPaletteFor(color: Color): PhysicalButtonPalette = PhysicalButtonPalette(
    faceTop = lerp(color, Color.White, 0.16f),
    face = color,
    faceBottom = lerp(color, Color.Black, 0.20f),
    wall = lerp(color, Color.Black, 0.42f),
    bevelLight = lerp(color, Color.White, 0.52f),
    bevelDark = lerp(color, Color.Black, 0.48f),
    castShadow = lerp(color, Color.Black, 0.62f)
)

private fun DrawScope.drawPhysicalButtonShell(
    outline: Outline,
    size: Size,
    themeColors: SpotVaultColorScheme,
    fillStyle: GlassButtonFillStyle,
    containerColor: Color,
    accentColor: Color = containerColor,
    pressed: Boolean = false,
    largeSurface: Boolean = false
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    val palette = when (fillStyle) {
        GlassButtonFillStyle.Solid -> physicalPaletteFor(containerColor)
        GlassButtonFillStyle.Outlined -> physicalPaletteFor(accentColor)
    }
    val wallDrop = if (pressed) 1.1.dp.toPx() else if (largeSurface) 3.2.dp.toPx() else 2.6.dp.toPx()
    val castDrop = if (pressed) 1.6.dp.toPx() else if (largeSurface) 5.dp.toPx() else 4.dp.toPx()
    val bevelStroke = if (largeSurface) 2.4.dp.toPx() else 1.8.dp.toPx()
    val pressedDarken = if (pressed) 0.10f else 0f

    fun Color.pressAdjusted(): Color = if (pressedDarken > 0f) lerp(this, Color.Black, pressedDarken) else this

    when (fillStyle) {
        GlassButtonFillStyle.Solid -> {
            translate(top = castDrop) {
                drawSpotVaultOutline(
                    outline = outline,
                    brush = SolidColor(Color.Black.copy(alpha = if (pressed) 0.22f else 0.36f))
                )
            }
            translate(top = castDrop * 0.55f) {
                drawSpotVaultOutline(
                    outline = outline,
                    brush = SolidColor(palette.castShadow.copy(alpha = if (pressed) 0.20f else 0.32f))
                )
            }
            translate(top = wallDrop) {
                drawSpotVaultOutline(
                    outline = outline,
                    brush = SolidColor(palette.wall.pressAdjusted())
                )
            }
            drawSpotVaultOutline(
                outline = outline,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.faceTop.pressAdjusted(),
                        palette.face.pressAdjusted(),
                        palette.face.pressAdjusted(),
                        palette.faceBottom.pressAdjusted()
                    ),
                    startY = 0f,
                    endY = h
                )
            )
            drawSpotVaultOutline(
                outline = outline,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.bevelLight.copy(alpha = 0.55f),
                        palette.bevelLight.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.14f
                )
            )
            drawSpotVaultOutline(
                outline = outline,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        palette.bevelDark.copy(alpha = 0.18f),
                        palette.bevelDark.copy(alpha = 0.42f)
                    ),
                    startY = h * 0.78f,
                    endY = h
                )
            )
            drawSpotVaultOutline(
                outline = outline,
                brush = Brush.linearGradient(
                    colors = listOf(
                        palette.bevelLight.copy(alpha = 0.95f),
                        palette.face.copy(alpha = 0.55f),
                        palette.bevelDark.copy(alpha = 0.90f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                style = Stroke(width = bevelStroke)
            )
            drawSpotVaultOutline(
                outline = outline,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.34f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.22f)
                    ),
                    start = Offset(w * 0.5f, 0f),
                    end = Offset(w * 0.5f, h)
                ),
                style = Stroke(width = bevelStroke * 0.45f)
            )
        }
        GlassButtonFillStyle.Outlined -> {
            drawSpotVaultOutline(
                outline = outline,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lerp(themeColors.Elevated, Color.Black, 0.18f),
                        themeColors.Surface,
                        lerp(themeColors.Surface, Color.Black, 0.22f)
                    )
                )
            )
            drawSpotVaultOutline(
                outline = outline,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.bevelLight.copy(alpha = 0.14f),
                        Color.Transparent,
                        palette.bevelDark.copy(alpha = 0.16f)
                    ),
                    startY = 0f,
                    endY = h
                )
            )
            drawSpotVaultOutline(
                outline = outline,
                brush = Brush.linearGradient(
                    colors = listOf(
                        palette.bevelLight.copy(alpha = 0.75f),
                        palette.face.copy(alpha = 0.85f),
                        palette.bevelDark.copy(alpha = 0.80f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                style = Stroke(width = bevelStroke)
            )
        }
    }
}

@Composable
private fun GlassStyledButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape,
    fillStyle: GlassButtonFillStyle,
    containerColor: Color,
    contentColor: Color,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource,
    largeSurface: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val themeColors = LocalSpotVaultColors.current
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = if (pressed) 0.968f else 1f
    val labelColor = when (fillStyle) {
        GlassButtonFillStyle.Solid -> solidButtonLabelColor(containerColor)
        else -> contentColor
    }
    val labelStyle = MaterialTheme.typography.labelLarge.copy(
        color = labelColor,
        fontWeight = FontWeight.Bold,
        shadow = if (labelColor.luminance() > 0.72f) {
            androidx.compose.ui.graphics.Shadow(
                color = Color.Black.copy(alpha = 0.82f),
                offset = Offset(0f, 1.5f),
                blurRadius = 3f
            )
        } else {
            null
        }
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.5f
            }
            .drawBehind {
                val outline = shape.createOutline(size, LayoutDirection.Ltr, this)
                drawPhysicalButtonShell(
                    outline = outline,
                    size = size,
                    themeColors = themeColors,
                    fillStyle = fillStyle,
                    containerColor = containerColor,
                    accentColor = contentColor,
                    pressed = pressed,
                    largeSurface = largeSurface
                )
            }
            .clip(shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = themeColors.Teal),
                role = Role.Button,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides labelColor) {
            ProvideTextStyle(labelStyle) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}

// Holds State<Float> rather than plain Float — every consumer draws these via .drawBehind{},
// which is already a proper deferred draw-phase callback, but passing in *pre-read* floats
// defeated that: reading .value here via `by` (as this used to) meant the composable that calls
// rememberWildPortalMotion() (WildStyledButton, WildButtonPreview, GradientCtaCard) still
// recomposed on every single animation frame before the value ever reached .drawBehind{}. Reading
// .value only inside each .drawBehind{} block below actually gets the deferred-draw benefit.
private class WildPortalMotion(
    val rimRotation: State<Float>,
    val orbitClock: State<Float>,
    val dustPulse: State<Float>,
    val wobble: State<Float>
)

@Composable
private fun rememberWildPortalMotion(): WildPortalMotion {
    if (ThemeState.reduceAnimations) {
        val zero = remember { mutableFloatStateOf(0f) }
        return remember { WildPortalMotion(zero, zero, zero, zero) }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "WildPortal")
    val rimRotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WildRimRotation"
    )
    val dustPulse = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WildDustPulse"
    )
    val wobble = infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WildWobble"
    )
    val orbitClock = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WildOrbitClock"
    )
    return WildPortalMotion(
        rimRotation = rimRotation,
        orbitClock = orbitClock,
        dustPulse = dustPulse,
        wobble = wobble
    )
}

private fun DrawScope.drawWildNeonShell(
    outline: Outline,
    size: Size,
    themeColors: SpotVaultColorScheme,
    fillStyle: WildButtonFillStyle,
    containerColor: Color,
    rimRotation: Float = 0f,
    orbitClock: Float = 0f,
    dustPulse: Float = 0f,
    wobble: Float = 0f,
    reducedMotion: Boolean = false
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val minEdge = minOf(w, h)
    val referenceSize = 160f
    val scaleFactor = (minEdge / referenceSize.dp.toPx()).coerceIn(0.35f, 1f)

    val plateLeft = w * 0.16f
    val plateTop = h * 0.23f
    val plateW = w * 0.68f
    val plateH = h * 0.54f
    val plateCenter = Offset(plateLeft + plateW / 2f, plateTop + plateH / 2f)
    val plateRadiusX = plateW / 2f
    val plateRadiusY = plateH / 2f

    val wobbleX = wobble * minEdge * 0.05f
    val wobbleY = wobble * minEdge * 0.03f
    val dashPhase = rimRotation * (1.6f * scaleFactor).dp.toPx()
    val rimAngle = Math.toRadians(rimRotation.toDouble())
    val rimCos = cos(rimAngle).toFloat()
    val rimSin = sin(rimAngle).toFloat()
    val breatheScale = 0.985f + wobble * 0.015f

    fun dissolveAlpha(baseAlpha: Float, phaseOffset: Float): Float {
        val wave = (sin(dustPulse * 2f * PI.toFloat() + phaseOffset) + 1f) / 2f
        return (baseAlpha * (0.4f + 0.6f * wave)).coerceIn(0f, 1f)
    }

    // 1) Void body
    drawSpotVaultOutline(
        outline = outline,
        brush = Brush.radialGradient(
            colors = listOf(themeColors.Deep, themeColors.Void, Color.Black),
            center = Offset(cx, cy),
            radius = maxOf(w, h) * 0.75f
        )
    )

    // Soft radial vignette where labels sit — darkens glyphs only, never a box
    val vignetteRadius = minOf(plateW, plateH) * 0.58f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.52f),
                Color.Black.copy(alpha = 0.24f),
                Color.Black.copy(alpha = 0.06f),
                Color.Transparent
            ),
            center = plateCenter,
            radius = vignetteRadius
        ),
        radius = vignetteRadius,
        center = plateCenter
    )

    // 2) Animated decorative shell — breathes, orbits, dissolves
    if (!reducedMotion) {
    withTransform({
        scale(breatheScale, breatheScale, pivot = Offset(cx, cy))
    }) {
        if (fillStyle == WildButtonFillStyle.Solid) {
            val glowA = Offset(w * 0.08f + wobbleX, h * 0.12f - wobbleY)
            val glowB = Offset(w * 0.92f - wobbleX, h * 0.88f + wobbleY)
            val glowC = Offset(w * 0.85f + wobbleX * 0.6f, h * 0.15f - wobbleY * 0.6f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(themeColors.PrimaryBright.copy(alpha = 0.75f), Color.Transparent),
                    center = glowA,
                    radius = w * 0.38f
                ),
                radius = w * 0.38f,
                center = glowA
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(themeColors.Teal.copy(alpha = 0.7f), Color.Transparent),
                    center = glowB,
                    radius = w * 0.36f
                ),
                radius = w * 0.36f,
                center = glowB
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(containerColor.copy(alpha = 0.45f), Color.Transparent),
                    center = glowC,
                    radius = w * 0.25f
                ),
                radius = w * 0.25f,
                center = glowC
            )
        }

        data class Shard(
            val baseX: Float,
            val baseY: Float,
            val rw: Float,
            val rh: Float,
            val rot: Float,
            val color: Color,
            val baseAlpha: Float,
            val orbitSpeed: Float,
            val orbitRadius: Float,
            val phaseOffset: Float
        )
        val shards = listOf(
            Shard(0.06f, 0.1f, 0.07f, 0.045f, -28f, themeColors.TealSoft, 0.9f, 0.55f, 0.045f, 0.4f),
            Shard(0.14f, 0.04f, 0.05f, 0.03f, 18f, themeColors.PrimaryBright, 0.7f, -0.42f, 0.035f, 1.2f),
            Shard(0.9f, 0.08f, 0.065f, 0.04f, 32f, themeColors.PrimaryBright, 0.85f, 0.38f, 0.05f, 2.1f),
            Shard(0.96f, 0.22f, 0.04f, 0.055f, -12f, themeColors.Teal, 0.65f, -0.6f, 0.04f, 2.8f),
            Shard(0.93f, 0.78f, 0.07f, 0.04f, 24f, themeColors.TealSoft, 0.88f, 0.48f, 0.048f, 3.5f),
            Shard(0.86f, 0.92f, 0.05f, 0.032f, -20f, themeColors.PrimaryBright, 0.7f, -0.35f, 0.038f, 4.2f),
            Shard(0.08f, 0.85f, 0.06f, 0.04f, 16f, themeColors.PrimaryBright, 0.8f, 0.62f, 0.042f, 5.0f),
            Shard(0.04f, 0.62f, 0.035f, 0.05f, -35f, themeColors.Teal, 0.6f, -0.5f, 0.032f, 5.7f),
            Shard(0.5f, 0.03f, 0.045f, 0.028f, 8f, themeColors.TealSoft, 0.55f, 0.72f, 0.03f, 6.4f),
            Shard(0.48f, 0.95f, 0.05f, 0.03f, -8f, themeColors.PrimaryBright, 0.55f, -0.68f, 0.036f, 7.1f)
        )
        shards.forEach { s ->
            val orbitRad = Math.toRadians((orbitClock * s.orbitSpeed + s.phaseOffset * 57.3f).toDouble())
            val orbitPx = cos(orbitRad).toFloat() * s.orbitRadius * w
            val orbitPy = sin(orbitRad).toFloat() * s.orbitRadius * h
            val alpha = dissolveAlpha(s.baseAlpha, s.phaseOffset)
            withTransform({
                translate(w * s.baseX + orbitPx, h * s.baseY + orbitPy)
                rotate(s.rot, pivot = Offset.Zero)
            }) {
                drawRoundRect(
                    color = s.color.copy(alpha = alpha),
                    topLeft = Offset(-w * s.rw / 2f, -h * s.rh / 2f),
                    size = Size(w * s.rw, h * s.rh),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        }

        val dust = listOf(
            0.02f to 0.3f, 0.03f to 0.48f, 0.025f to 0.7f,
            0.97f to 0.32f, 0.98f to 0.5f, 0.96f to 0.68f,
            0.22f to 0.02f, 0.4f to 0.015f, 0.62f to 0.02f, 0.78f to 0.025f,
            0.2f to 0.97f, 0.38f to 0.985f, 0.6f to 0.97f, 0.76f to 0.98f
        )
        dust.forEachIndexed { i, (px, py) ->
            val r = ((1.2f + (i % 3) * 0.7f) * scaleFactor).dp.toPx()
            val c = if (i % 2 == 0) themeColors.TealSoft else themeColors.PrimaryBright
            val baseAlpha = 0.35f + (i % 4) * 0.12f
            val phaseOffset = i * 0.85f
            val alpha = dissolveAlpha(baseAlpha, phaseOffset)
            drawCircle(c.copy(alpha = alpha), r, Offset(w * px, h * py))
        }

        data class Ember(
            val angleDeg: Float,
            val driftSpeed: Float,
            val phaseOffset: Float,
            val color: Color
        )
        val embers = listOf(
            Ember(18f, 0.14f, 0.6f, themeColors.PrimaryBright),
            Ember(72f, 0.11f, 1.8f, themeColors.TealSoft),
            Ember(148f, 0.16f, 2.9f, themeColors.Teal),
            Ember(214f, 0.12f, 4.1f, themeColors.PrimaryBright),
            Ember(286f, 0.15f, 5.3f, themeColors.TealSoft),
            Ember(338f, 0.13f, 6.5f, themeColors.PrimaryBright)
        )
        val outerDist = minEdge * 0.46f
        embers.forEach { ember ->
            val cycle = (((orbitClock * ember.driftSpeed + ember.phaseOffset * 40f) % 360f) + 360f) % 360f / 360f
            val angleRad = Math.toRadians(ember.angleDeg.toDouble())
            val cosA = cos(angleRad).toFloat()
            val sinA = sin(angleRad).toFloat()
            val edgeDist = (plateRadiusX * plateRadiusY) /
                kotlin.math.sqrt((plateRadiusY * cosA).let { it * it } + (plateRadiusX * sinA).let { it * it })
            val dist = edgeDist + cycle * (outerDist - edgeDist)
            val px = plateCenter.x + cosA * dist
            val py = plateCenter.y + sinA * dist
            val coreR = (1.8f + cycle * 1.2f) * scaleFactor.dp.toPx()
            val glowR = coreR * 2.8f
            val alpha = dissolveAlpha(0.92f, ember.phaseOffset)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(ember.color.copy(alpha = alpha), ember.color.copy(alpha = alpha * 0.35f), Color.Transparent),
                    center = Offset(px, py),
                    radius = glowR
                ),
                radius = glowR,
                center = Offset(px, py)
            )
            drawCircle(ember.color.copy(alpha = alpha), coreR, Offset(px, py))
        }

        val rimStart = Offset(cx + rimCos * w * 0.5f, cy + rimSin * h * 0.5f)
        val rimEnd = Offset(cx - rimCos * w * 0.5f, cy - rimSin * h * 0.5f)
        drawSpotVaultOutline(
            outline = outline,
            brush = Brush.linearGradient(
                listOf(
                    themeColors.TealSoft,
                    themeColors.PrimaryBright,
                    themeColors.Teal,
                    themeColors.PrimaryBright
                ),
                start = rimStart,
                end = rimEnd
            ),
            style = Stroke(
                width = (3.2f * scaleFactor).dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(
                        (14f * scaleFactor).dp.toPx(),
                        (8f * scaleFactor).dp.toPx(),
                        (6f * scaleFactor).dp.toPx(),
                        (10f * scaleFactor).dp.toPx()
                    ),
                    dashPhase
                ),
                cap = StrokeCap.Round
            )
        )
        drawSpotVaultOutline(
            outline = outline,
            brush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.55f), Color.Transparent, themeColors.Teal.copy(alpha = 0.4f)),
                start = rimEnd,
                end = rimStart
            ),
            style = Stroke(
                width = (1.3f * scaleFactor).dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(
                        (8f * scaleFactor).dp.toPx(),
                        (12f * scaleFactor).dp.toPx()
                    ),
                    (6f * scaleFactor).dp.toPx() + dashPhase * 0.65f
                )
            )
        )

        val crackStroke = (1.6f * scaleFactor).dp.toPx()
        listOf(
            listOf(Offset(w * 0.1f, h * 0.08f), Offset(w * 0.18f, h * 0.16f), Offset(w * 0.14f, h * 0.2f)),
            listOf(Offset(w * 0.9f, h * 0.9f), Offset(w * 0.82f, h * 0.84f), Offset(w * 0.86f, h * 0.74f)),
            listOf(Offset(w * 0.92f, h * 0.12f), Offset(w * 0.84f, h * 0.18f))
        ).forEachIndexed { idx, pts ->
            for (i in 0 until pts.lastIndex) {
                drawLine(
                    color = (if (idx == 1) themeColors.TealSoft else themeColors.PrimaryBright)
                        .copy(alpha = dissolveAlpha(0.75f, idx * 1.7f)),
                    start = pts[i],
                    end = pts[i + 1],
                    strokeWidth = crackStroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
    } else {
        if (fillStyle == WildButtonFillStyle.Solid) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(themeColors.PrimaryBright.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(w * 0.25f, h * 0.3f),
                    radius = w * 0.35f
                ),
                radius = w * 0.35f,
                center = Offset(w * 0.25f, h * 0.3f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(themeColors.Teal.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(w * 0.75f, h * 0.7f),
                    radius = w * 0.32f
                ),
                radius = w * 0.32f,
                center = Offset(w * 0.75f, h * 0.7f)
            )
        }
        drawSpotVaultOutline(
            outline = outline,
            brush = Brush.linearGradient(
                listOf(themeColors.TealSoft, themeColors.PrimaryBright, themeColors.Teal),
                start = Offset(w, h * 0.5f),
                end = Offset(0f, h * 0.5f)
            ),
            style = Stroke(width = (2.5f * scaleFactor).dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun wildPortalFloatingTextStyle(base: androidx.compose.ui.text.TextStyle): androidx.compose.ui.text.TextStyle {
    return base.copy(
        fontWeight = FontWeight.Black,
        color = Color.White,
        shadow = androidx.compose.ui.graphics.Shadow(
            color = Color.Black.copy(alpha = 0.95f),
            offset = Offset(0f, 3f),
            blurRadius = 18f
        )
    )
}

@Composable
private fun WildPortalFloatingLabel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val themeColors = LocalSpotVaultColors.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .offset(x = 1.dp, y = 2.dp)
                .graphicsLayer { alpha = 0.88f }
        ) {
            CompositionLocalProvider(LocalContentColor provides themeColors.Void) {
                content()
            }
        }
        content()
    }
}

private fun DrawScope.drawSpotVaultOutline(
    outline: Outline,
    brush: Brush,
    style: androidx.compose.ui.graphics.drawscope.DrawStyle = Fill
) {
    when (outline) {
        is Outline.Rectangle -> drawRect(brush = brush, topLeft = outline.rect.topLeft, size = outline.rect.size, style = style)
        is Outline.Rounded -> {
            val roundRect = outline.roundRect
            drawRoundRect(
                brush = brush,
                topLeft = Offset(roundRect.left, roundRect.top),
                size = Size(roundRect.width, roundRect.height),
                cornerRadius = roundRect.topLeftCornerRadius,
                style = style
            )
        }
        is Outline.Generic -> drawPath(outline.path, brush = brush, style = style)
    }
}

@Composable
private fun WildStyledButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape,
    fillStyle: WildButtonFillStyle,
    containerColor: Color,
    contentColor: Color,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource,
    content: @Composable RowScope.() -> Unit
) {
    val themeColors = LocalSpotVaultColors.current
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = if (pressed) 0.975f else 1f
    val motion = rememberWildPortalMotion()
    val labelStyle = wildPortalFloatingTextStyle(
        MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp)
    )

    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.55f
            }
            .clip(shape)
            .drawBehind {
                val outline = shape.createOutline(size, LayoutDirection.Ltr, this)
                drawWildNeonShell(
                    outline = outline,
                    size = size,
                    themeColors = themeColors,
                    fillStyle = fillStyle,
                    containerColor = containerColor,
                    rimRotation = motion.rimRotation.value,
                    orbitClock = motion.orbitClock.value,
                    dustPulse = motion.dustPulse.value,
                    wobble = motion.wobble.value,
                    reducedMotion = ThemeState.reduceAnimations
                )
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = themeColors.Teal),
                role = Role.Button,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides Color.White,
            androidx.compose.material3.LocalTextStyle provides labelStyle
        ) {
            ProvideTextStyle(labelStyle) {
                WildPortalFloatingLabel {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        content = content
                    )
                }
            }
        }
    }
}

@Composable
private fun LeafButtonPreview(modifier: Modifier = Modifier) {
    val themeColors = LocalSpotVaultColors.current
    val shape = remember { leafButtonPreviewShape() }
    Box(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(36.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        themeColors.PrimaryBright.copy(alpha = 0.95f),
                        themeColors.Primary,
                        themeColors.Teal.copy(alpha = 0.85f)
                    )
                )
            )
            .border(1.dp, themeColors.Teal.copy(alpha = 0.45f), shape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = size.width * 0.5f
            drawLine(
                color = themeColors.TealSoft.copy(alpha = 0.55f),
                start = Offset(cx, size.height * 0.18f),
                end = Offset(cx, size.height * 0.78f),
                strokeWidth = 1.2f
            )
        }
        Text(
            "LEAF",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun HexButtonPreview(modifier: Modifier = Modifier) {
    val themeColors = LocalSpotVaultColors.current
    val shape = remember { hexButtonPreviewShape() }
    Box(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(36.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        themeColors.PrimaryBright.copy(alpha = 0.92f),
                        themeColors.Primary,
                        themeColors.PrimaryDeep
                    )
                )
            )
            .border(1.5.dp, themeColors.Teal.copy(alpha = 0.5f), shape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            drawLine(
                color = themeColors.TealSoft.copy(alpha = 0.45f),
                start = Offset(cx, cy - size.height * 0.22f),
                end = Offset(cx, cy + size.height * 0.22f),
                strokeWidth = 1f
            )
            drawLine(
                color = themeColors.TealSoft.copy(alpha = 0.35f),
                start = Offset(cx - size.width * 0.18f, cy),
                end = Offset(cx + size.width * 0.18f, cy),
                strokeWidth = 1f
            )
        }
        Text(
            "HEX",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 1.1.sp
        )
    }
}

@Composable
private fun WaveButtonPreview(modifier: Modifier = Modifier) {
    val themeColors = LocalSpotVaultColors.current
    val shape = remember { waveButtonPreviewShape() }
    Box(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(36.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        themeColors.Teal.copy(alpha = 0.88f),
                        themeColors.TealDeep.copy(alpha = 0.95f)
                    )
                )
            )
            .border(1.dp, themeColors.TealSoft.copy(alpha = 0.55f), shape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val waveY = size.height * 0.5f
            val path = Path().apply {
                moveTo(size.width * 0.08f, waveY)
                cubicTo(size.width * 0.3f, waveY - 4f, size.width * 0.7f, waveY + 4f, size.width * 0.92f, waveY)
            }
            drawPath(
                path,
                color = themeColors.OnSurface.copy(alpha = 0.35f),
                style = Stroke(width = 1.2f)
            )
        }
        Text(
            "TIDE",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = SpotVaultColors.OnTeal,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun TacticalButtonPreview(modifier: Modifier = Modifier) {
    val themeColors = LocalSpotVaultColors.current
    val shape = remember { tacticalButtonPreviewShape() }
    Box(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(36.dp)
            .clip(shape)
            .background(themeColors.Elevated)
            .border(1.5.dp, themeColors.Teal.copy(alpha = 0.55f), shape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = size.width * 0.5f
            drawCircle(
                color = themeColors.Muted.copy(alpha = 0.65f),
                radius = size.minDimension * 0.055f,
                center = Offset(cx, size.height * 0.08f)
            )
            drawLine(
                color = themeColors.Teal.copy(alpha = 0.35f),
                start = Offset(size.width * 0.12f, size.height * 0.72f),
                end = Offset(size.width * 0.88f, size.height * 0.72f),
                strokeWidth = 1f
            )
        }
        Text(
            "TAG",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = themeColors.OnSurface,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun WildButtonPreview(modifier: Modifier = Modifier) {
    val themeColors = LocalSpotVaultColors.current
    val shape = remember { wildButtonPreviewShape() }
    // Static, not rememberWildPortalMotion() — this is the only call site (a settings picker
    // swatch alongside Leaf/Tactical/Hex/Wave, none of which animate either), and
    // rememberWildPortalMotion drives four separate rememberInfiniteTransitions. Those already run
    // continuously on every real Wild-styled button on screen; adding a fifth, sixth, seventh and
    // eighth here just to animate a 36dp settings swatch was extra perpetual redraw work for
    // something the reduceAnimations setting already treats as fine to freeze (same all-zero state
    // it falls back to) — reused verbatim here since it's already a shipped, vetted look.
    val zeroMotion = remember { mutableFloatStateOf(0f) }
    val motion = remember { WildPortalMotion(zeroMotion, zeroMotion, zeroMotion, zeroMotion) }
    // Same shell as real buttons so the picker matches what you get, just frozen mid-animation.
    Box(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(36.dp)
            .clip(shape)
            .drawBehind {
                val outline = shape.createOutline(size, LayoutDirection.Ltr, this)
                drawWildNeonShell(
                    outline = outline,
                    size = size,
                    themeColors = themeColors,
                    fillStyle = WildButtonFillStyle.Solid,
                    containerColor = themeColors.Primary,
                    rimRotation = motion.rimRotation.value,
                    orbitClock = motion.orbitClock.value,
                    dustPulse = motion.dustPulse.value,
                    wobble = motion.wobble.value,
                    reducedMotion = ThemeState.reduceAnimations
                )
            },
        contentAlignment = Alignment.Center
    ) {
        WildPortalFloatingLabel {
            Text(
                "WILD",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp,
                style = wildPortalFloatingTextStyle(TextStyle())
            )
        }
    }
}

@Composable
fun GradientCtaCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 168.dp,
    tealDominant: Boolean = false,
    titleColor: Color? = null,
    dense: Boolean = false,
    showSubtitle: Boolean = true,
    titleFontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val buttonStyle = ThemeState.buttonStyle
    val isWild = buttonStyle == "wild"
    val themeColors = LocalSpotVaultColors.current
    val shape = remember(buttonStyle) {
        spotVaultButtonShape(SpotVaultButtonShapeSize.Large)
    }
    val containerColor = if (tealDominant) SpotVaultColors.Teal else SpotVaultColors.Primary
    val accentColor = if (tealDominant) SpotVaultColors.Ink else SpotVaultColors.OnSurface

    // A minimum rather than a fixed height: on short screens the label must be
    // allowed to push the card taller instead of being clipped by the shape.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .scale(if (pressed) 0.985f else 1f)
            .then(
                if (isWild) {
                    // Was created unconditionally at the top of this composable regardless of
                    // isWild, spinning up 4 simultaneous infinite animations (one running for
                    // ~16.7 minutes) for every GradientCtaCard on screen — including the Snap/Pin
                    // home-screen buttons — even for the majority of users who never selected
                    // "wild" as their button style and never read any of these values. Moved here,
                    // inside the branch that's the only place it's ever used, so it's only created
                    // when actually needed — same pattern WildStyledButton already uses correctly
                    // (it's a whole separate composable only mounted when isWild is true).
                    val wildMotion = rememberWildPortalMotion()
                    Modifier
                        .clip(shape)
                        .drawBehind {
                            val outline = shape.createOutline(size, LayoutDirection.Ltr, this)
                            drawWildNeonShell(
                                outline = outline,
                                size = size,
                                themeColors = themeColors,
                                fillStyle = WildButtonFillStyle.Solid,
                                containerColor = containerColor,
                                rimRotation = wildMotion.rimRotation.value,
                                orbitClock = wildMotion.orbitClock.value,
                                dustPulse = wildMotion.dustPulse.value,
                                wobble = wildMotion.wobble.value,
                                reducedMotion = ThemeState.reduceAnimations
                            )
                        }
                } else {
                    Modifier
                        .drawBehind {
                            val outline = shape.createOutline(size, LayoutDirection.Ltr, this)
                            drawPhysicalButtonShell(
                                outline = outline,
                                size = size,
                                themeColors = themeColors,
                                fillStyle = GlassButtonFillStyle.Solid,
                                containerColor = containerColor,
                                accentColor = accentColor,
                                pressed = pressed,
                                largeSurface = true
                            )
                        }
                        .clip(shape)
                }
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = SpotVaultColors.Teal),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isWild) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(
                    horizontal = if (dense) 18.dp else 28.dp,
                    vertical = if (dense) 10.dp else 16.dp
                )
            ) {
                icon()
                Spacer(modifier = Modifier.height(if (dense) 6.dp else 10.dp))
                WildPortalFloatingLabel {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = if (dense) 18.sp else 22.sp,
                        style = wildPortalFloatingTextStyle(TextStyle())
                    )
                }
                if (showSubtitle) {
                    Text(
                        text = subtitle,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = if (dense) 12.sp else 14.sp,
                        modifier = Modifier.padding(top = if (dense) 2.dp else 4.dp),
                        style = wildPortalFloatingTextStyle(TextStyle()).copy(
                            fontWeight = FontWeight.SemiBold,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.9f),
                                offset = Offset(0f, 2f),
                                blurRadius = 12f
                            )
                        )
                    )
                }
            }
        } else {
            val ctaLabelColor = solidButtonLabelColor(containerColor)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(
                    horizontal = if (dense) 16.dp else 20.dp,
                    vertical = if (dense) 8.dp else 14.dp
                )
            ) {
                icon()
                Spacer(modifier = Modifier.height(if (dense) 6.dp else 12.dp))
                Text(
                    text = title,
                    style = (if (dense) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge).copy(
                        shadow = if (ctaLabelColor.luminance() > 0.72f) {
                            androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.82f),
                                offset = Offset(0f, 2f),
                                blurRadius = 4f
                            )
                        } else {
                            null
                        }
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = titleFontSize,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = titleColor ?: ctaLabelColor
                )
                if (showSubtitle) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        // Full opacity, not the faded alpha this used to carry — against a solid
                        // gradient card (as opposed to a neutral surface) that fade read as washed
                        // out next to the home screen's Quick Pin/Quick Track row right below it,
                        // which uses the button's plain, undimmed content color for its label.
                        color = titleColor ?: ctaLabelColor,
                        modifier = Modifier.padding(top = if (dense) 2.dp else 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * [includeNavBarInset] adds the real system nav-bar height on top of its own small gap — for
 * screens (like Settings) that draw this hint in a layer *not* already pushed above the nav bar
 * by a Scaffold/bottom-bar. Pass false where the hint already lives inside content that's been
 * inset once already (e.g. the Vault, which sits inside the app's persistent bottom nav bar's own
 * Scaffold) — otherwise the nav-bar height gets subtracted twice and the hint floats with a gap
 * above where it should sit.
 */
@Composable
fun PremiumScrollMoreHint(modifier: Modifier = Modifier, includeNavBarInset: Boolean = true) {
    val colors = LocalSpotVaultColors.current
    val navBarBottom = if (includeNavBarInset) {
        with(androidx.compose.ui.platform.LocalDensity.current) { SystemBarInsets.navigationBarPx.toDp() } + 2.dp
    } else {
        0.dp
    }
    val reducedMotion = ThemeState.reduceAnimations
    val infiniteTransition = rememberInfiniteTransition(label = "scrollHint")
    val animatedBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    val animatedPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val animatedArrowScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowScale"
    )

    // bounce/pulseAlpha/arrowScale used to be read into plain vals right here in the composable
    // body (then fed into .background()/.border() Brushes and .copy(alpha=) tints built at body
    // scope) — every one of those reads recomposed this whole hint (both Icons, the pill
    // background, the border) on every frame of three simultaneous infinite animations, for as
    // long as the hint was visible (i.e. the whole time there's more content below the fold on
    // any screen using it — Settings tabs and the Vault list). Every read below is now inline
    // inside a graphicsLayer{}/drawWithCache{} lambda instead, deferring it to the draw phase —
    // same values, same visuals, no per-frame recomposition. The pill's background+border moved
    // from .background()/.border() (which needed the animated alpha at body scope to build their
    // Brushes) to a manual drawRoundRect pass; the outer .clip() still shapes the pill exactly as
    // before, and each Icon keeps its own independent alpha exactly like its original
    // tint.copy(alpha=) did, via a per-Icon graphicsLayer{alpha=} instead.
    Column(
        modifier = modifier
            .padding(bottom = navBarBottom + 6.dp)
            .graphicsLayer { translationY = if (reducedMotion) 0f else animatedBounce },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .graphicsLayer {
                    val s = if (reducedMotion) 1f else animatedArrowScale
                    scaleX = s
                    scaleY = s
                }
                .clip(RoundedCornerShape(999.dp))
                .drawWithCache {
                    val cornerRadius = CornerRadius(size.height / 2f)
                    val strokeWidthPx = 1.dp.toPx()
                    onDrawBehind {
                        val pulseAlpha = if (reducedMotion) 0.85f else animatedPulseAlpha
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    colors.Teal.copy(alpha = pulseAlpha * 0.35f),
                                    colors.Primary.copy(alpha = pulseAlpha * 0.2f)
                                )
                            ),
                            cornerRadius = cornerRadius
                        )
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    colors.Teal.copy(alpha = pulseAlpha),
                                    colors.PrimaryBright.copy(alpha = pulseAlpha * 0.7f)
                                )
                            ),
                            cornerRadius = cornerRadius,
                            style = Stroke(width = strokeWidthPx)
                        )
                    }
                }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.Teal,
                modifier = Modifier
                    .size(26.dp)
                    .offset(y = 4.dp)
                    .graphicsLayer { alpha = if (reducedMotion) 0.85f else animatedPulseAlpha }
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Scroll for more",
                tint = colors.PrimaryBright,
                modifier = Modifier
                    .size(26.dp)
                    .offset(y = (-10).dp)
                    .graphicsLayer { alpha = if (reducedMotion) 0.85f else animatedPulseAlpha }
            )
        }
    }
}

@Composable
fun AnimatedScrollDownHint(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scrollDownHint")
    val offsetY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    // Named animatedAlpha, not alpha — inside the graphicsLayer{} block below, a bare `alpha`
    // reference would resolve to GraphicsLayerScope's OWN alpha property (the lambda receiver),
    // not this one, silently turning `alpha = alpha` into a no-op self-assignment rather than
    // applying the animated value at all.
    val animatedAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        // tint.copy(alpha = animatedAlpha) used to be built at composable-body scope, recomposing
        // this Icon every animation frame for as long as the hint was visible — moved into
        // graphicsLayer{} (alongside the translationY bounce, already correctly deferred here)
        // so both reads happen at draw time instead. Full-opacity Teal + a layer-alpha multiply is
        // visually identical to the original tint.copy(alpha=) for a flat-color icon glyph.
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = "Scroll for more",
            tint = SpotVaultColors.Teal,
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    translationY = offsetY
                    alpha = animatedAlpha
                }
        )
    }
}

@Composable
fun PremiumVerticalScrollbar(
    scrollState: ScrollState,
    viewportHeightPx: Int,
    emphasizeHint: Boolean = false,
    modifier: Modifier = Modifier
) {
    val maxScroll = scrollState.maxValue.coerceAtLeast(1)
    val scrollFraction = (scrollState.value.toFloat() / maxScroll).coerceIn(0f, 1f)
    val contentHeight = (viewportHeightPx + scrollState.maxValue).coerceAtLeast(1)
    val thumbHeightFraction = (viewportHeightPx.toFloat() / contentHeight).coerceIn(0.16f, 1f)
    PremiumVerticalScrollbarCanvas(
        scrollFraction = scrollFraction,
        thumbHeightFraction = thumbHeightFraction,
        emphasizeHint = emphasizeHint,
        modifier = modifier
    )
}

@Composable
fun PremiumVerticalScrollbar(
    listState: androidx.compose.foundation.lazy.LazyListState,
    emphasizeHint: Boolean = false,
    modifier: Modifier = Modifier
) {
    val layoutInfo = listState.layoutInfo
    val viewportHeightPx = layoutInfo.viewportSize.height.coerceAtLeast(1)
    val visibleItems = layoutInfo.visibleItemsInfo
    val totalItems = layoutInfo.totalItemsCount.coerceAtLeast(1)
    val avgItemSize = visibleItems.map { it.size }.average().toFloat().coerceAtLeast(1f)
    val estimatedContentHeight = avgItemSize * totalItems
    val scrolled = listState.firstVisibleItemIndex * avgItemSize + listState.firstVisibleItemScrollOffset
    val maxScroll = (estimatedContentHeight - viewportHeightPx).coerceAtLeast(1f)
    val scrollFraction = when {
        !listState.canScrollForward && !listState.canScrollBackward -> 0f
        !listState.canScrollForward -> 1f
        !listState.canScrollBackward -> 0f
        else -> (scrolled / maxScroll).coerceIn(0f, 1f)
    }
    val contentHeight = estimatedContentHeight.coerceAtLeast(viewportHeightPx.toFloat()).toInt()
    val thumbHeightFraction = (viewportHeightPx.toFloat() / contentHeight).coerceIn(0.16f, 1f)
    PremiumVerticalScrollbarCanvas(
        scrollFraction = scrollFraction,
        thumbHeightFraction = thumbHeightFraction,
        emphasizeHint = emphasizeHint,
        modifier = modifier
    )
}

/** Same glowing-track scrollbar as the List-mode overload, sized off a
 * [androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState] instead — the Vault's
 * Grid mode (2 fixed columns) had no scroll indicator of its own at all before this. Row count
 * (item count / column count), not raw item count, is what approximates the actual scrollable
 * height for a multi-column grid. */
@Composable
fun PremiumVerticalScrollbar(
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    columns: Int = 2,
    emphasizeHint: Boolean = false,
    modifier: Modifier = Modifier
) {
    val layoutInfo = gridState.layoutInfo
    val viewportHeightPx = layoutInfo.viewportSize.height.coerceAtLeast(1)
    val visibleItems = layoutInfo.visibleItemsInfo
    val totalItems = layoutInfo.totalItemsCount.coerceAtLeast(1)
    val avgItemSize = visibleItems.map { it.size.height }.average().toFloat().coerceAtLeast(1f)
    val estimatedRows = ((totalItems + columns - 1) / columns).coerceAtLeast(1)
    val estimatedContentHeight = avgItemSize * estimatedRows
    val scrolledRows = gridState.firstVisibleItemIndex / columns
    val scrolled = scrolledRows * avgItemSize + gridState.firstVisibleItemScrollOffset
    val maxScroll = (estimatedContentHeight - viewportHeightPx).coerceAtLeast(1f)
    val scrollFraction = when {
        !gridState.canScrollForward && !gridState.canScrollBackward -> 0f
        !gridState.canScrollForward -> 1f
        !gridState.canScrollBackward -> 0f
        else -> (scrolled / maxScroll).coerceIn(0f, 1f)
    }
    val contentHeight = estimatedContentHeight.coerceAtLeast(viewportHeightPx.toFloat()).toInt()
    val thumbHeightFraction = (viewportHeightPx.toFloat() / contentHeight).coerceIn(0.16f, 1f)
    PremiumVerticalScrollbarCanvas(
        scrollFraction = scrollFraction,
        thumbHeightFraction = thumbHeightFraction,
        emphasizeHint = emphasizeHint,
        modifier = modifier
    )
}

@Composable
private fun PremiumVerticalScrollbarCanvas(
    scrollFraction: Float,
    thumbHeightFraction: Float,
    emphasizeHint: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalSpotVaultColors.current
    val pulseTransition = rememberInfiniteTransition(label = "scrollbarPulse")
    // Kept as a State object — see PremiumHorizontalScrollbar's matching comment above. This
    // scrollbar stays mounted for as long as any scrollable Settings/grid screen is open, not
    // just briefly, so deferring the read into Canvas's draw phase is worth doing here too.
    val glowAlphaState = pulseTransition.animateFloat(
        initialValue = if (emphasizeHint) 0.35f else 0.12f,
        targetValue = if (emphasizeHint) 0.85f else 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (emphasizeHint) 900 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Canvas(modifier = modifier) {
        val trackWidth = size.width
        val trackHeight = size.height
        val corner = CornerRadius(trackWidth / 2f, trackWidth / 2f)

        if (emphasizeHint) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.Teal.copy(alpha = glowAlphaState.value * 0.35f),
                        colors.Primary.copy(alpha = glowAlphaState.value * 0.2f),
                        Color.Transparent
                    )
                ),
                size = Size(trackWidth * 2.8f, trackHeight),
                topLeft = Offset(-trackWidth * 0.9f, 0f),
                cornerRadius = CornerRadius(trackWidth * 1.4f, trackWidth * 1.4f)
            )
        }

        drawRoundRect(
            color = colors.Outline.copy(alpha = if (emphasizeHint) 0.42f else 0.28f),
            size = Size(trackWidth, trackHeight),
            cornerRadius = corner
        )

        val thumbHeight = trackHeight * thumbHeightFraction
        val thumbTravel = (trackHeight - thumbHeight).coerceAtLeast(0f)
        val thumbTop = thumbTravel * scrollFraction

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(colors.Teal, colors.PrimaryBright, colors.Primary)
            ),
            topLeft = Offset(0f, thumbTop),
            size = Size(trackWidth, thumbHeight),
            cornerRadius = corner
        )

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = if (emphasizeHint) 0.5f else 0.35f), Color.Transparent)
            ),
            topLeft = Offset(0f, thumbTop),
            size = Size(trackWidth, thumbHeight * 0.45f),
            cornerRadius = corner
        )
    }
}

@Composable
fun SpotVaultBrandHeader(
    modifier: Modifier = Modifier,
    displayName: String = DEFAULT_VAULT_DISPLAY_NAME,
    belowTitle: (@Composable () -> Unit)? = null
) {
    val colors = LocalSpotVaultColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // Compose BOM 2024.09 has no TextAutoSize — measure/step font size so the
        // vault name always fits on one line without ellipsis truncation.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val textMeasurer = rememberTextMeasurer()
            val titleSizes = listOf(32.sp, 28.sp, 24.sp, 20.sp)
            val titleBrush = Brush.horizontalGradient(
                colors = listOf(colors.PrimaryBright, colors.Teal, colors.Primary)
            )
            val fittedSize = titleSizes.firstOrNull { candidate ->
                val measured = textMeasurer.measure(
                    text = displayName,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = candidate,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    maxLines = 1,
                    softWrap = false
                )
                measured.size.width <= constraints.maxWidth
            } ?: 20.sp
            Text(
                text = displayName,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = fittedSize,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    brush = titleBrush
                )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Brush.horizontalGradient(listOf(colors.Primary, colors.Teal)))
        )
        if (belowTitle != null) {
            Spacer(modifier = Modifier.height(12.dp))
            belowTitle()
        }
    }
}

@Composable
fun AnimatedVaultIcon(
    modifier: Modifier = Modifier,
    style: String = ThemeState.vaultIconStyle,
    iconSize: Dp = 76.dp,
    // False for VaultIconStylePicker, which renders every style's icon at once as a settings
    // swatch — true (the default) is the real, single, persistent bottom-nav Vault icon, which is
    // meant to keep animating for as long as the app is open. See VaultIconMotionLayer's comment
    // for why an unconditional loop here is worse than it looks: each style option would otherwise
    // start its own independent LaunchedEffect/withFrameNanos loop the instant the picker renders,
    // same bug class as the animated background-pattern picker (see AppBackgroundPatterns.kt).
    animated: Boolean = true
) {
    when (style) {
        "hex" -> AnimatedVaultIconHex(modifier, iconSize, animated)
        "bank" -> AnimatedVaultIconBank(modifier, iconSize, animated)
        "wacky" -> AnimatedVaultIconWacky(modifier, iconSize, animated)
        "camo" -> AnimatedVaultIconCamo(modifier, iconSize, animated)
        "leaf" -> AnimatedVaultIconLeaf(modifier, iconSize, animated)
        "sonar" -> AnimatedVaultIconSonar(modifier, iconSize, animated)
        "maglock", "gear" -> AnimatedVaultIconMagLock(modifier, iconSize, animated)
        "halloween" -> AnimatedVaultIconHalloween(modifier, iconSize, animated)
        "christmas" -> AnimatedVaultIconChristmas(modifier, iconSize, animated)
        else -> AnimatedVaultIconClassic(modifier, iconSize, animated)
    }
}

@Composable
private fun VaultIconMotionLayer(
    modifier: Modifier = Modifier,
    iconSize: Dp = 76.dp,
    animated: Boolean = true,
    drawDoor: DrawScope.(center: Offset, radius: Float, outerRotation: Float, innerRotation: Float, pulsePhase: Float, colors: SpotVaultColorScheme) -> Unit
) {
    val themeColors = LocalSpotVaultColors.current
    val reducedMotion = ThemeState.reduceAnimations

    var pulsePhase by remember { mutableFloatStateOf(0f) }
    var rotationDeg by remember { mutableFloatStateOf(0f) }
    if (!reducedMotion && animated) {
        LaunchedEffect(Unit) {
            var startTime = 0L
            while (isActive) {
                withFrameNanos { time ->
                    if (startTime == 0L) startTime = time
                    val elapsedSec = (time - startTime) / 1_000_000_000f
                    // Continuous phase — never restarts, so sin-based glow/scale stays seamless.
                    pulsePhase = elapsedSec * (PI.toFloat() / 2f)
                    rotationDeg = elapsedSec * 45f
                }
            }
        }
    }

    // This is the persistent bottom-nav Vault icon — mounted for essentially the entire time
    // the app is open, on every screen with the bottom bar. Reading pulsePhase/rotationDeg
    // directly in the composable body (as scale/alphaGlow used to) made every single animation
    // frame trigger a full recomposition of this whole subtree — measure, layout, and
    // modifier-chain re-evaluation, not just a redraw — for the entire time the app was in the
    // foreground. graphicsLayer/drawWithCache below read the same state but *inside* their
    // lambdas, which Compose defers straight to the draw phase instead: same animation, same
    // values, but skipping composition/layout on every frame. The inner door drawing already
    // used drawWithCache; its rotation/pulse reads just move fully inside onDrawBehind too, so
    // drawWithCache's own cache doesn't get invalidated by every tick either.
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = if (reducedMotion) 1f else 1f + 0.05f * sin(pulsePhase)
                    scaleX = s
                    scaleY = s
                }
                .drawWithCache {
                    // Matches what Modifier.background(brush, shape = CircleShape) drew before —
                    // an oval spanning the full box (CircleShape's own outline for a non-square
                    // box is an oval bounded by the box, not a true circle) — so this is a pure
                    // "defer the read" change with the same shape either way.
                    val glowCenter = Offset(size.width / 2f, size.height / 2f)
                    val glowRadius = size.maxDimension / 2f
                    onDrawBehind {
                        val alphaGlow = if (reducedMotion) 0.6f else 0.5f + 0.2f * sin(pulsePhase)
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(themeColors.PrimaryBright.copy(alpha = alphaGlow), Color.Transparent),
                                center = glowCenter,
                                radius = glowRadius
                            ),
                            topLeft = Offset.Zero,
                            size = size
                        )
                    }
                }
        )

        Box(
            modifier = Modifier
                .size(iconSize)
                .drawWithCache {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2
                    onDrawBehind {
                        val outerRotation = rotationDeg
                        val innerRotation = rotationDeg * 1.5f
                        drawDoor(center, radius, outerRotation, innerRotation, pulsePhase, themeColors)
                    }
                }
        )
    }
}

private fun DrawScope.drawRegularPolygon(
    center: Offset,
    radius: Float,
    sides: Int,
    rotationDeg: Float,
    brush: Brush,
    style: androidx.compose.ui.graphics.drawscope.DrawStyle = Fill
) {
    val path = Path()
    for (i in 0 until sides) {
        val angle = Math.toRadians(rotationDeg + (360.0 / sides * i) - 90.0)
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, brush = brush, style = style)
}

@Composable
fun AnimatedVaultIconClassic(modifier: Modifier = Modifier, iconSize: Dp = 76.dp, animated: Boolean = true) {
    VaultIconMotionLayer(modifier = modifier, iconSize = iconSize, animated = animated) { center, radius, outerRotation, innerRotation, pulsePhase, themeColors ->
        val rimStroke = 6.dp.toPx()
        val hubStroke = 2.dp.toPx()
        val spokeStroke = 4.dp.toPx()
        val boltHeadRadius = 4.dp.toPx()
        val outerBoltRadius = 2.dp.toPx()

        val doorBrush = Brush.radialGradient(
            colors = listOf(themeColors.Surface, themeColors.Deep, themeColors.PrimaryDeep),
            center = center,
            radius = radius
        )
        val rimBrush = Brush.sweepGradient(
            colors = listOf(
                themeColors.PrimaryDeep,
                themeColors.PrimaryBright.copy(alpha = 0.8f),
                themeColors.PrimaryDeep,
                themeColors.Teal.copy(alpha = 0.8f),
                themeColors.PrimaryDeep
            ),
            center = center
        )
        val hubBrush = Brush.radialGradient(
            colors = listOf(themeColors.PrimaryBright, themeColors.Teal),
            center = center,
            radius = radius * 0.35f
        )

        drawCircle(brush = doorBrush, radius = radius, center = center)

        withTransform({ rotate(outerRotation, center) }) {
            drawCircle(
                brush = rimBrush,
                radius = radius,
                center = center,
                style = Stroke(width = rimStroke)
            )
        }

        drawCircle(color = themeColors.Surface, radius = radius * 0.85f, center = center)

        withTransform({ rotate(innerRotation, center) }) {
            drawCircle(brush = hubBrush, radius = radius * 0.35f, center = center)
            drawCircle(
                color = themeColors.Deep,
                radius = radius * 0.15f,
                center = center,
                style = Stroke(width = hubStroke)
            )

            val numBolts = 5
            for (i in 0 until numBolts) {
                val angleRad = Math.toRadians((i * (360.0 / numBolts)))
                val innerX = center.x + (radius * 0.35f) * cos(angleRad).toFloat()
                val innerY = center.y + (radius * 0.35f) * sin(angleRad).toFloat()
                val outerX = center.x + (radius * 0.7f) * cos(angleRad).toFloat()
                val outerY = center.y + (radius * 0.7f) * sin(angleRad).toFloat()

                drawLine(
                    color = themeColors.PrimaryBright,
                    start = Offset(innerX, innerY),
                    end = Offset(outerX, outerY),
                    strokeWidth = spokeStroke,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = themeColors.Teal,
                    radius = boltHeadRadius,
                    center = Offset(outerX, outerY)
                )
            }
        }

        val numOuterBolts = 8
        for (i in 0 until numOuterBolts) {
            val angleRad = Math.toRadians((i * (360.0 / numOuterBolts) + (360.0 / numOuterBolts / 2.0)))
            drawCircle(
                color = themeColors.Outline.copy(alpha = 0.5f),
                radius = outerBoltRadius,
                center = Offset(
                    center.x + (radius * 0.9f) * cos(angleRad).toFloat(),
                    center.y + (radius * 0.9f) * sin(angleRad).toFloat()
                )
            )
        }
    }
}

@Composable
fun AnimatedVaultIconHex(modifier: Modifier = Modifier, iconSize: Dp = 76.dp, animated: Boolean = true) {
    VaultIconMotionLayer(modifier = modifier, iconSize = iconSize, animated = animated) { center, radius, outerRotation, innerRotation, pulsePhase, themeColors ->
        val stroke = 5.dp.toPx()
        val doorBrush = Brush.radialGradient(
            colors = listOf(themeColors.Elevated, themeColors.Deep, themeColors.PrimaryDeep),
            center = center,
            radius = radius
        )
        val rimBrush = Brush.sweepGradient(
            colors = listOf(
                themeColors.Teal,
                themeColors.PrimaryBright,
                themeColors.PrimaryDeep,
                themeColors.TealSoft,
                themeColors.Teal
            ),
            center = center
        )

        drawRegularPolygon(center, radius, 6, 0f, doorBrush)

        withTransform({ rotate(outerRotation, center) }) {
            drawRegularPolygon(
                center, radius * 0.96f, 6, 0f,
                brush = rimBrush,
                style = Stroke(width = stroke)
            )
            for (i in 0 until 6) {
                val angle = Math.toRadians(outerRotation + (60.0 * i))
                drawCircle(
                    color = themeColors.TealSoft,
                    radius = 3.dp.toPx(),
                    center = Offset(
                        center.x + radius * 0.88f * cos(angle).toFloat(),
                        center.y + radius * 0.88f * sin(angle).toFloat()
                    )
                )
            }
        }

        drawRegularPolygon(center, radius * 0.72f, 6, 15f, Brush.linearGradient(
            listOf(themeColors.Surface, themeColors.Deep)
        ))

        withTransform({ rotate(-innerRotation, center) }) {
            for (i in 0 until 6) {
                val startAngle = 60f * i
                drawArc(
                    color = themeColors.PrimaryBright.copy(alpha = 0.85f),
                    startAngle = startAngle + 4f,
                    sweepAngle = 24f,
                    useCenter = true,
                    topLeft = Offset(center.x - radius * 0.55f, center.y - radius * 0.55f),
                    size = Size(radius * 1.1f, radius * 1.1f)
                )
            }
            drawRegularPolygon(
                center, radius * 0.22f, 6, 0f,
                brush = Brush.radialGradient(listOf(themeColors.TealSoft, themeColors.TealDeep)),
            )
            drawCircle(color = themeColors.Ink, radius = radius * 0.07f, center = center)
        }
    }
}

@Composable
fun AnimatedVaultIconBank(modifier: Modifier = Modifier, iconSize: Dp = 76.dp, animated: Boolean = true) {
    VaultIconMotionLayer(modifier = modifier, iconSize = iconSize, animated = animated) { center, radius, outerRotation, innerRotation, pulsePhase, themeColors ->
        val rimStroke = 5.dp.toPx()
        val doorBrush = Brush.radialGradient(
            colors = listOf(themeColors.Glass, themeColors.Surface, themeColors.PrimaryDeep),
            center = center,
            radius = radius
        )

        drawCircle(brush = doorBrush, radius = radius, center = center)

        withTransform({ rotate(outerRotation, center) }) {
            for (i in 0 until 12) {
                val angle = Math.toRadians(outerRotation + (30.0 * i))
                drawLine(
                    color = themeColors.PrimaryBright.copy(alpha = 0.55f),
                    start = Offset(
                        center.x + radius * 0.78f * cos(angle).toFloat(),
                        center.y + radius * 0.78f * sin(angle).toFloat()
                    ),
                    end = Offset(
                        center.x + radius * 0.94f * cos(angle).toFloat(),
                        center.y + radius * 0.94f * sin(angle).toFloat()
                    ),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(themeColors.PrimaryDeep, themeColors.Teal, themeColors.PrimaryBright, themeColors.PrimaryDeep),
                    center = center
                ),
                radius = radius,
                center = center,
                style = Stroke(width = rimStroke)
            )
        }

        drawCircle(color = themeColors.Deep.copy(alpha = 0.9f), radius = radius * 0.68f, center = center)
        drawCircle(
            color = themeColors.Outline.copy(alpha = 0.45f),
            radius = radius * 0.68f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        withTransform({ rotate(innerRotation, center) }) {
            drawCircle(
                brush = Brush.radialGradient(listOf(themeColors.PrimaryBright, themeColors.Primary)),
                radius = radius * 0.42f,
                center = center
            )
            for (i in 0 until 4) {
                val angle = Math.toRadians((90.0 * i) + 45.0)
                drawLine(
                    color = themeColors.TealSoft,
                    start = center,
                    end = Offset(
                        center.x + radius * 0.38f * cos(angle).toFloat(),
                        center.y + radius * 0.38f * sin(angle).toFloat()
                    ),
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            drawCircle(color = themeColors.Ink, radius = radius * 0.11f, center = center)
            drawCircle(
                color = themeColors.Teal,
                radius = radius * 0.11f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        for (i in 0 until 4) {
            val angle = Math.toRadians((90.0 * i) + 45.0)
            drawCircle(
                color = themeColors.Outline.copy(alpha = 0.65f),
                radius = 3.dp.toPx(),
                center = Offset(
                    center.x + radius * 0.84f * cos(angle).toFloat(),
                    center.y + radius * 0.84f * sin(angle).toFloat()
                )
            )
        }
    }
}

@Composable
fun AnimatedVaultIconWacky(modifier: Modifier = Modifier, iconSize: Dp = 76.dp, animated: Boolean = true) {
    WackyVaultIconMotionLayer(modifier = modifier, iconSize = iconSize, animated = animated) { center, radius, rotationDeg, innerRotationDeg, wobble, themeColors ->
        val wobbleX = wobble * radius * 0.05f
        val wobbleY = wobble * radius * 0.03f
        val shiftedCenter = Offset(center.x + wobbleX, center.y - wobbleY)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(themeColors.PrimaryBright.copy(alpha = 0.35f), Color.Transparent),
                center = shiftedCenter,
                radius = radius * 1.08f
            ),
            radius = radius * 1.08f,
            center = shiftedCenter
        )

        withTransform({ rotate(-rotationDeg * 1.35f, shiftedCenter) }) {
            drawRegularPolygon(
                center = shiftedCenter,
                radius = radius * 0.96f,
                sides = 5,
                rotationDeg = 0f,
                brush = Brush.sweepGradient(
                    listOf(themeColors.Teal, themeColors.PrimaryBright, themeColors.PrimaryDeep, themeColors.Teal),
                    center = shiftedCenter
                ),
                style = Stroke(width = 6.dp.toPx())
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(themeColors.Surface, themeColors.Deep, themeColors.PrimaryDeep),
                center = shiftedCenter,
                radius = radius * 0.82f
            ),
            radius = radius * 0.82f,
            center = shiftedCenter
        )

        withTransform({ rotate(innerRotationDeg * 1.8f, shiftedCenter) }) {
            drawRegularPolygon(
                center = shiftedCenter,
                radius = radius * 0.58f,
                sides = 3,
                rotationDeg = 0f,
                brush = Brush.linearGradient(
                    listOf(themeColors.PrimaryBright, themeColors.Teal, themeColors.Primary)
                )
            )
            for (i in 0 until 3) {
                val angle = Math.toRadians((120.0 * i) + 30.0)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White, themeColors.TealSoft, themeColors.Teal),
                        center = Offset(
                            shiftedCenter.x + radius * 0.2f * cos(angle).toFloat(),
                            shiftedCenter.y + radius * 0.2f * sin(angle).toFloat()
                        ),
                        radius = radius * 0.12f
                    ),
                    radius = radius * 0.12f,
                    center = Offset(
                        shiftedCenter.x + radius * 0.34f * cos(angle).toFloat(),
                        shiftedCenter.y + radius * 0.34f * sin(angle).toFloat()
                    )
                )
            }
        }

        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White, themeColors.PrimaryBright, themeColors.PrimaryDeep)),
            radius = radius * 0.14f,
            center = shiftedCenter
        )
        drawCircle(color = themeColors.Ink, radius = radius * 0.05f, center = shiftedCenter)

        val eyeOffset = radius * 0.24f
        drawCircle(color = themeColors.TealSoft.copy(alpha = 0.9f), radius = radius * 0.055f, center = Offset(shiftedCenter.x - eyeOffset, shiftedCenter.y - eyeOffset * 0.35f))
        drawCircle(color = themeColors.TealSoft.copy(alpha = 0.9f), radius = radius * 0.055f, center = Offset(shiftedCenter.x + eyeOffset, shiftedCenter.y - eyeOffset * 0.35f))
    }
}

@Composable
fun AnimatedVaultIconCamo(modifier: Modifier = Modifier, iconSize: Dp = 76.dp, animated: Boolean = true) {
    VaultIconMotionLayer(modifier = modifier, iconSize = iconSize, animated = animated) { center, radius, outerRotation, innerRotation, pulsePhase, themeColors ->
        val rimStroke = 5.dp.toPx()

        // Mottled drab door — a few overlapping soft blobs stand in for a camo pattern
        // without needing a bitmap asset, tinted entirely from the active theme colors.
        val doorBrush = Brush.radialGradient(
            colors = listOf(themeColors.Elevated, themeColors.Surface, themeColors.Deep),
            center = center,
            radius = radius
        )
        drawCircle(brush = doorBrush, radius = radius, center = center)
        drawCircle(
            color = themeColors.PrimaryDeep.copy(alpha = 0.55f),
            radius = radius * 0.62f,
            center = Offset(center.x - radius * 0.32f, center.y - radius * 0.22f)
        )
        drawCircle(
            color = themeColors.Primary.copy(alpha = 0.4f),
            radius = radius * 0.5f,
            center = Offset(center.x + radius * 0.3f, center.y + radius * 0.28f)
        )
        drawCircle(
            color = themeColors.Deep.copy(alpha = 0.5f),
            radius = radius * 0.4f,
            center = Offset(center.x + radius * 0.18f, center.y - radius * 0.35f)
        )

        // Rotating compass bezel with tick marks, like a rugged field compass rim.
        withTransform({ rotate(outerRotation, center) }) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        themeColors.PrimaryDeep,
                        themeColors.PrimaryBright,
                        themeColors.Teal,
                        themeColors.PrimaryDeep
                    ),
                    center = center
                ),
                radius = radius,
                center = center,
                style = Stroke(width = rimStroke)
            )
            for (i in 0 until 16) {
                val angle = Math.toRadians(i * 22.5)
                val isCardinal = i % 4 == 0
                val innerR = if (isCardinal) radius * 0.82f else radius * 0.88f
                drawLine(
                    color = if (isCardinal) themeColors.Teal else themeColors.PrimaryBright.copy(alpha = 0.65f),
                    start = Offset(
                        center.x + innerR * cos(angle).toFloat(),
                        center.y + innerR * sin(angle).toFloat()
                    ),
                    end = Offset(
                        center.x + radius * 0.95f * cos(angle).toFloat(),
                        center.y + radius * 0.95f * sin(angle).toFloat()
                    ),
                    strokeWidth = if (isCardinal) 3.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        drawCircle(color = themeColors.Deep.copy(alpha = 0.92f), radius = radius * 0.68f, center = center)
        drawCircle(
            color = themeColors.Outline.copy(alpha = 0.6f),
            radius = radius * 0.68f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Scope reticle crosshair — a nod to hunting optics — rotates gently with the inner ring.
        withTransform({ rotate(innerRotation * 0.4f, center) }) {
            val armLen = radius * 0.46f
            val gap = radius * 0.14f
            drawLine(
                color = themeColors.Teal.copy(alpha = 0.9f),
                start = Offset(center.x, center.y - armLen),
                end = Offset(center.x, center.y - gap),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = themeColors.Teal.copy(alpha = 0.9f),
                start = Offset(center.x, center.y + gap),
                end = Offset(center.x, center.y + armLen),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = themeColors.Teal.copy(alpha = 0.9f),
                start = Offset(center.x - armLen, center.y),
                end = Offset(center.x - gap, center.y),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = themeColors.Teal.copy(alpha = 0.9f),
                start = Offset(center.x + gap, center.y),
                end = Offset(center.x + armLen, center.y),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(themeColors.PrimaryBright, themeColors.Primary)),
                radius = radius * 0.13f,
                center = center
            )
            drawCircle(color = themeColors.Ink, radius = radius * 0.045f, center = center)
        }

        for (i in 0 until 4) {
            val angle = Math.toRadians((90.0 * i) + 45.0)
            drawCircle(
                color = themeColors.Teal.copy(alpha = 0.55f),
                radius = 2.5.dp.toPx(),
                center = Offset(
                    center.x + radius * 0.9f * cos(angle).toFloat(),
                    center.y + radius * 0.9f * sin(angle).toFloat()
                )
            )
        }
    }
}

@Composable
fun AnimatedVaultIconLeaf(modifier: Modifier = Modifier, iconSize: Dp = 76.dp, animated: Boolean = true) {
    val density = LocalDensity.current
    val radiusPx = with(density) { (iconSize / 2f).toPx() }
    val leafPath = remember(radiusPx) {
        Path().apply {
            val lw = radiusPx * 0.18f
            val lh = radiusPx * 0.34f
            moveTo(0f, lh * 0.42f)
            cubicTo(lw * 0.75f, lh * 0.48f, lw, -lh * 0.12f, 0f, -lh * 0.5f)
            cubicTo(-lw, -lh * 0.12f, -lw * 0.75f, lh * 0.48f, 0f, lh * 0.42f)
            close()
        }
    }
    val leafPathSmall = remember(radiusPx) {
        Path().apply {
            val lw = radiusPx * 0.13f
            val lh = radiusPx * 0.24f
            moveTo(0f, lh * 0.38f)
            cubicTo(lw * 0.72f, lh * 0.44f, lw * 0.92f, -lh * 0.08f, 0f, -lh * 0.46f)
            cubicTo(-lw * 0.92f, -lh * 0.08f, -lw * 0.72f, lh * 0.44f, 0f, lh * 0.38f)
            close()
        }
    }
    val sprigLeafPath = remember(radiusPx) {
        Path().apply {
            val lw = radiusPx * 0.24f
            val lh = radiusPx * 0.18f
            moveTo(0f, lh * 0.35f)
            cubicTo(lw * 0.85f, lh * 0.2f, lw, -lh * 0.35f, lw * 0.15f, -lh * 0.55f)
            cubicTo(-lw * 0.25f, -lh * 0.15f, -lw * 0.35f, lh * 0.25f, 0f, lh * 0.35f)
            close()
        }
    }
    VaultIconMotionLayer(modifier = modifier, iconSize = iconSize, animated = animated) { center, radius, outerRotation, innerRotation, pulsePhase, themeColors ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    themeColors.Elevated,
                    themeColors.Deep.copy(alpha = 0.95f),
                    themeColors.Ink.copy(alpha = 0.92f)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        for (ring in 1..4) {
            drawCircle(
                color = themeColors.Teal.copy(alpha = 0.08f + ring * 0.04f),
                radius = radius * (0.24f + ring * 0.16f),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        withTransform({ rotate(outerRotation * 0.55f, center) }) {
            val leafCount = 12
            for (i in 0 until leafCount) {
                val angle = Math.toRadians(i * (360.0 / leafCount))
                val leafCenter = Offset(
                    center.x + radius * 0.84f * cos(angle).toFloat(),
                    center.y + radius * 0.84f * sin(angle).toFloat()
                )
                val path = if (i % 2 == 0) leafPath else leafPathSmall
                val foliage = Brush.linearGradient(
                    listOf(
                        themeColors.TealSoft.copy(alpha = 0.95f),
                        themeColors.Teal,
                        themeColors.Primary.copy(alpha = 0.88f)
                    )
                )
                withTransform({
                    translate(leafCenter.x, leafCenter.y)
                    rotate(Math.toDegrees(angle).toFloat() + 90f, pivot = Offset.Zero)
                }) {
                    drawPath(path = path, brush = foliage)
                    drawLine(
                        color = themeColors.PrimaryDeep.copy(alpha = 0.55f),
                        start = Offset(0f, -radius * 0.14f),
                        end = Offset(0f, radius * 0.1f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            drawCircle(
                color = themeColors.Teal.copy(alpha = 0.22f),
                radius = radius * 0.93f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = themeColors.PrimaryBright.copy(alpha = 0.14f),
                radius = radius * 0.88f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                listOf(themeColors.Surface.copy(alpha = 0.9f), themeColors.Deep.copy(alpha = 0.96f)),
                center = center,
                radius = radius * 0.58f
            ),
            radius = radius * 0.58f,
            center = center
        )
        drawCircle(
            color = themeColors.Outline.copy(alpha = 0.45f),
            radius = radius * 0.58f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        withTransform({ rotate(innerRotation * 0.28f, center) }) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(themeColors.TealSoft.copy(alpha = 0.85f), themeColors.Teal.copy(alpha = 0.55f))
                ),
                radius = radius * 0.3f,
                center = center
            )
            drawLine(
                color = themeColors.PrimaryDeep.copy(alpha = 0.85f),
                start = Offset(center.x, center.y + radius * 0.22f),
                end = Offset(center.x, center.y - radius * 0.04f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            withTransform({
                translate(center.x, center.y)
                rotate(-28f, pivot = Offset.Zero)
            }) {
                drawPath(
                    path = sprigLeafPath,
                    brush = Brush.linearGradient(listOf(themeColors.TealSoft, themeColors.Primary))
                )
            }
            withTransform({
                translate(center.x, center.y)
                rotate(28f, pivot = Offset.Zero)
                scale(-1f, 1f, pivot = Offset.Zero)
            }) {
                drawPath(
                    path = sprigLeafPath,
                    brush = Brush.linearGradient(listOf(themeColors.TealSoft, themeColors.PrimaryBright))
                )
            }
            drawCircle(color = themeColors.Ink.copy(alpha = 0.75f), radius = radius * 0.05f, center = center)
        }
    }
}

private fun DrawScope.drawSonarRipples(
    center: Offset,
    radius: Float,
    pulsePhase: Float,
    themeColors: SpotVaultColorScheme
) {
    val ringCount = 4
    val cycle = pulsePhase / (2f * PI.toFloat())
    for (i in 0 until ringCount) {
        val offset = i.toFloat() / ringCount
        val progress = (cycle + offset) % 1f
        val ringR = radius * (0.24f + 0.7f * progress)
        val alpha = (1f - progress).coerceIn(0f, 1f) * 0.52f
        drawCircle(
            color = themeColors.Teal.copy(alpha = alpha),
            radius = ringR,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun AnimatedVaultIconSonar(modifier: Modifier = Modifier, iconSize: Dp = 76.dp, animated: Boolean = true) {
    VaultIconMotionLayer(modifier = modifier, iconSize = iconSize, animated = animated) { center, radius, outerRotation, innerRotation, pulsePhase, themeColors ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(themeColors.Deep, themeColors.Ink, themeColors.Void),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        drawSonarRipples(center, radius, pulsePhase, themeColors)

        withTransform({ rotate(outerRotation - 90f, center) }) {
            drawArc(
                color = themeColors.TealSoft.copy(alpha = 0.32f),
                startAngle = -24f,
                sweepAngle = 48f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f)
            )
            drawArc(
                color = themeColors.Teal.copy(alpha = 0.55f),
                startAngle = -4f,
                sweepAngle = 8f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f)
            )
        }

        drawCircle(
            color = themeColors.Surface.copy(alpha = 0.92f),
            radius = radius * 0.62f,
            center = center
        )
        drawCircle(
            color = themeColors.Outline.copy(alpha = 0.5f),
            radius = radius * 0.62f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        withTransform({ rotate(innerRotation * 0.35f, center) }) {
            for (i in 0 until 8) {
                val angle = Math.toRadians(i * 45.0)
                drawLine(
                    color = themeColors.PrimaryBright.copy(alpha = 0.45f),
                    start = Offset(
                        center.x + radius * 0.28f * cos(angle).toFloat(),
                        center.y + radius * 0.28f * sin(angle).toFloat()
                    ),
                    end = Offset(
                        center.x + radius * 0.52f * cos(angle).toFloat(),
                        center.y + radius * 0.52f * sin(angle).toFloat()
                    ),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        val corePulse = 0.85f + 0.15f * sin(pulsePhase * 1.4f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    themeColors.TealSoft.copy(alpha = corePulse),
                    themeColors.Teal,
                    themeColors.PrimaryDeep
                ),
                center = center,
                radius = radius * 0.22f
            ),
            radius = radius * 0.22f,
            center = center
        )
        drawCircle(color = themeColors.Ink.copy(alpha = 0.8f), radius = radius * 0.06f, center = center)
    }
}

@Composable
fun AnimatedVaultIconMagLock(modifier: Modifier = Modifier, iconSize: Dp = 76.dp, animated: Boolean = true) {
    VaultIconMotionLayer(modifier = modifier, iconSize = iconSize, animated = animated) { center, radius, outerRotation, innerRotation, pulsePhase, themeColors ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(themeColors.Elevated, themeColors.Surface, themeColors.Deep),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // Slow energy sweep — continuous, no loop reset
        withTransform({ rotate(outerRotation * 0.6f - 90f, center) }) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        themeColors.Teal.copy(alpha = 0.08f),
                        themeColors.TealSoft.copy(alpha = 0.42f),
                        themeColors.PrimaryBright.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f)
            )
        }

        // Vault door ring
        drawCircle(
            color = themeColors.Outline.copy(alpha = 0.55f),
            radius = radius * 0.88f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Six magnetic bolts — each slides in/out on a continuous sine phase (no stutter)
        for (i in 0 until 6) {
            val angle = Math.toRadians((60.0 * i) - 90.0)
            val boltPhase = sin(pulsePhase + i * 1.05f) * 0.5f + 0.5f
            val housingR = radius * 0.72f
            val housing = Offset(
                center.x + housingR * cos(angle).toFloat(),
                center.y + housingR * sin(angle).toFloat()
            )
            val slide = radius * 0.06f * (1f - boltPhase)
            val boltCenter = Offset(
                housing.x - slide * cos(angle).toFloat(),
                housing.y - slide * sin(angle).toFloat()
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(themeColors.PrimaryBright, themeColors.Primary, themeColors.PrimaryDeep),
                    start = boltCenter + Offset(-4f, -6f),
                    end = boltCenter + Offset(4f, 6f)
                ),
                topLeft = Offset(boltCenter.x - radius * 0.055f, boltCenter.y - radius * 0.035f),
                size = Size(radius * 0.11f, radius * 0.07f),
                cornerRadius = CornerRadius(radius * 0.025f)
            )
            if (boltPhase > 0.82f) {
                drawCircle(
                    color = themeColors.TealSoft.copy(alpha = (boltPhase - 0.82f) * 2.8f),
                    radius = radius * 0.05f,
                    center = housing
                )
            }
        }

        // Iris core — breathes open and closed smoothly
        val irisOpen = 0.78f + 0.22f * sin(pulsePhase * 0.85f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    themeColors.TealSoft.copy(alpha = 0.95f),
                    themeColors.Teal,
                    themeColors.PrimaryDeep
                ),
                center = center,
                radius = radius * 0.28f * irisOpen
            ),
            radius = radius * 0.28f * irisOpen,
            center = center
        )
        drawCircle(
            color = themeColors.Outline.copy(alpha = 0.45f),
            radius = radius * 0.28f * irisOpen,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        withTransform({ rotate(innerRotation * 0.25f, center) }) {
            for (i in 0 until 8) {
                val spoke = Math.toRadians(i * 45.0)
                drawLine(
                    color = themeColors.PrimaryBright.copy(alpha = 0.35f),
                    start = center,
                    end = Offset(
                        center.x + radius * 0.22f * irisOpen * cos(spoke).toFloat(),
                        center.y + radius * 0.22f * irisOpen * sin(spoke).toFloat()
                    ),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.9f), themeColors.Teal, themeColors.Ink),
                center = center - Offset(1.5f, 1.5f),
                radius = radius * 0.08f
            ),
            radius = radius * 0.08f,
            center = center
        )
    }
}

// ---------------------------------------------------------------------------------
// Halloween — harvest-moon graveyard vault: candy-corn ring, witch-hat jack-o'-lantern dial,
// spider bolts, counter-spinning ghost ring, candle-flame lock, tombstones, and pumpkin lights.
// VaultIconMotionLayer only — pulse/rotation read inside draw, no extra recomposition.
// ---------------------------------------------------------------------------------

private fun DrawScope.drawHalloweenGhost(
    center: Offset,
    size: Float,
    alpha: Float
) {
    val w = size * 0.55f
    val h = size * 0.7f
    val top = center.y - h * 0.45f
    drawRoundRect(
        color = Color.White.copy(alpha = alpha),
        topLeft = Offset(center.x - w * 0.5f, top),
        size = Size(w, h * 0.62f),
        cornerRadius = CornerRadius(w * 0.5f)
    )
    // Wavy sheet tail
    val tailY = top + h * 0.58f
    val wave = Path().apply {
        moveTo(center.x - w * 0.5f, tailY)
        cubicTo(
            center.x - w * 0.25f, tailY + h * 0.18f,
            center.x - w * 0.08f, tailY - h * 0.04f,
            center.x, tailY + h * 0.12f
        )
        cubicTo(
            center.x + w * 0.08f, tailY - h * 0.04f,
            center.x + w * 0.25f, tailY + h * 0.18f,
            center.x + w * 0.5f, tailY
        )
        lineTo(center.x + w * 0.5f, tailY + h * 0.02f)
        lineTo(center.x - w * 0.5f, tailY + h * 0.02f)
        close()
    }
    drawPath(wave, color = Color.White.copy(alpha = alpha))
    // Eyes
    listOf(-1f, 1f).forEach { side ->
        drawOval(
            color = Color(0xFF1A0A04).copy(alpha = alpha * 0.9f),
            topLeft = Offset(center.x + side * w * 0.16f - w * 0.07f, top + h * 0.22f),
            size = Size(w * 0.14f, h * 0.16f)
        )
    }
}

private fun DrawScope.drawHalloweenWitchHat(
    center: Offset,
    radius: Float
) {
    val brimY = center.y - radius * 0.80f
    // Wide brim
    drawOval(
        color = Color(0xFF1A0A04),
        topLeft = Offset(center.x - radius * 0.28f, brimY - radius * 0.03f),
        size = Size(radius * 0.56f, radius * 0.1f)
    )
    // Cone — apex and curl were previously at 1.08x/1.12x radius, past the icon's own drawing
    // bounds (the box's top edge sits at exactly 1.0x radius above center), so Compose's default
    // clip-to-layout-bounds was silently cutting the hat's point off. Pulled both in under 1.0x.
    val hat = Path().apply {
        moveTo(center.x - radius * 0.18f, brimY)
        lineTo(center.x + radius * 0.04f, center.y - radius * 0.96f)
        lineTo(center.x + radius * 0.2f, brimY)
        close()
    }
    drawPath(hat, color = Color(0xFF1A0A04))
    // Purple band
    drawLine(
        color = Color(0xFF9B30FF),
        start = Offset(center.x - radius * 0.17f, brimY - radius * 0.01f),
        end = Offset(center.x + radius * 0.17f, brimY - radius * 0.01f),
        strokeWidth = radius * 0.045f,
        cap = StrokeCap.Round
    )
    // Buckle
    drawRoundRect(
        color = Color(0xFFFFB300),
        topLeft = Offset(center.x - radius * 0.035f, brimY - radius * 0.035f),
        size = Size(radius * 0.07f, radius * 0.05f),
        cornerRadius = CornerRadius(radius * 0.01f)
    )
    // Crooked tip curl
    drawArc(
        color = Color(0xFF1A0A04),
        startAngle = 280f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = Offset(center.x + radius * 0.01f, center.y - radius * 0.97f),
        size = Size(radius * 0.14f, radius * 0.1f),
        style = Stroke(width = radius * 0.045f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawHalloweenPumpkinDial(
    center: Offset,
    radius: Float,
    rotationDeg: Float,
    flicker: Float
) {
    withTransform({ rotate(rotationDeg, center) }) {
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent),
                center = center + Offset(0f, radius * 0.1f),
                radius = radius * 0.7f
            ),
            topLeft = Offset(center.x - radius * 0.68f, center.y - radius * 0.48f),
            size = Size(radius * 1.36f, radius * 1.2f)
        )
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFB74D),
                    Color(0xFFFF7A1A),
                    Color(0xFFBF4F00),
                    Color(0xFF6B2800)
                ),
                center = center - Offset(radius * 0.14f, radius * 0.1f),
                radius = radius * 0.82f
            ),
            topLeft = Offset(center.x - radius * 0.78f, center.y - radius * 0.68f),
            size = Size(radius * 1.56f, radius * 1.36f)
        )
        for (i in -3..3) {
            val x = center.x + i * radius * 0.11f
            drawLine(
                color = Color(0xFFD45A00).copy(alpha = 0.5f),
                start = Offset(x, center.y - radius * 0.62f),
                end = Offset(x, center.y + radius * 0.62f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawArc(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.06f), Color.Transparent),
                start = Offset(center.x - radius * 0.35f, center.y - radius * 0.45f),
                end = Offset(center.x + radius * 0.15f, center.y - radius * 0.05f)
            ),
            startAngle = 210f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 0.76f, center.y - radius * 0.66f),
            size = Size(radius * 1.52f, radius * 1.32f),
            style = Stroke(width = radius * 0.06f, cap = StrokeCap.Round)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF176).copy(alpha = flicker * 0.85f),
                    Color(0xFFFF9100).copy(alpha = flicker * 0.55f),
                    Color(0xFFFF5722).copy(alpha = flicker * 0.22f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 0.58f
            ),
            radius = radius * 0.58f,
            center = center
        )
        fun glowFill(pathCenter: Offset, haloRadius: Float) = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFDE7).copy(alpha = flicker),
                Color(0xFFFFAB40).copy(alpha = flicker * 0.95f),
                Color(0xFFE65100).copy(alpha = flicker * 0.7f)
            ),
            center = pathCenter,
            radius = haloRadius
        )
        listOf(-1f, 1f).forEach { side ->
            val ex = center.x + side * radius * 0.24f
            val ey = center.y - radius * 0.14f
            val eyeW = radius * 0.18f
            val eye = Path().apply {
                moveTo(ex, ey - eyeW * 0.6f)
                lineTo(ex - side * eyeW * 0.62f, ey + eyeW * 0.48f)
                lineTo(ex + side * eyeW * 0.62f, ey + eyeW * 0.48f)
                close()
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF176).copy(alpha = flicker * 0.65f), Color.Transparent),
                    center = Offset(ex, ey),
                    radius = eyeW * 1.8f
                ),
                radius = eyeW * 1.8f,
                center = Offset(ex, ey)
            )
            drawPath(eye, brush = glowFill(Offset(ex, ey), eyeW * 0.95f))
        }
        val noseY = center.y + radius * 0.04f
        drawPath(
            path = Path().apply {
                moveTo(center.x, noseY - radius * 0.055f)
                lineTo(center.x - radius * 0.065f, noseY + radius * 0.055f)
                lineTo(center.x + radius * 0.065f, noseY + radius * 0.055f)
                close()
            },
            color = Color(0xFF1A0A04)
        )
        val mouthY = center.y + radius * 0.2f
        val mouth = Path().apply {
            moveTo(center.x - radius * 0.34f, mouthY)
            lineTo(center.x - radius * 0.26f, mouthY + radius * 0.09f)
            lineTo(center.x - radius * 0.18f, mouthY)
            lineTo(center.x - radius * 0.10f, mouthY + radius * 0.09f)
            lineTo(center.x - radius * 0.02f, mouthY)
            lineTo(center.x + radius * 0.06f, mouthY + radius * 0.09f)
            lineTo(center.x + radius * 0.14f, mouthY)
            lineTo(center.x + radius * 0.22f, mouthY + radius * 0.09f)
            lineTo(center.x + radius * 0.30f, mouthY)
            lineTo(center.x + radius * 0.34f, mouthY - radius * 0.04f)
            lineTo(center.x - radius * 0.34f, mouthY - radius * 0.04f)
            close()
        }
        val mouthCenter = Offset(center.x, mouthY + radius * 0.02f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF176).copy(alpha = flicker * 0.6f), Color.Transparent),
                center = mouthCenter,
                radius = radius * 0.42f
            ),
            radius = radius * 0.42f,
            center = mouthCenter
        )
        drawPath(mouth, brush = glowFill(mouthCenter, radius * 0.34f))
        // Witch hat crowns the jack-o'-lantern
        drawHalloweenWitchHat(center, radius)
    }
}

private fun DrawScope.drawHalloweenSpiderBolts(
    center: Offset,
    radius: Float,
    pulsePhase: Float,
    rotationDeg: Float,
    flicker: Float
) {
    withTransform({ rotate(rotationDeg, center) }) {
        for (i in 0 until 6) {
            val angle = Math.toRadians((60.0 * i) - 90.0)
            val boltPhase = sin(pulsePhase + i * 1.12f) * 0.5f + 0.5f
            val housingR = radius * 0.7f
            val housing = Offset(
                center.x + housingR * cos(angle).toFloat(),
                center.y + housingR * sin(angle).toFloat()
            )
            val slide = radius * 0.075f * (1f - boltPhase)
            val spiderCenter = Offset(
                housing.x - slide * cos(angle).toFloat(),
                housing.y - slide * sin(angle).toFloat()
            )
            for (leg in 0 until 4) {
                val legAngle = angle + Math.toRadians((leg - 1.5) * 38.0)
                val legLen = radius * 0.09f
                drawLine(
                    color = Color(0xFF1A0A04).copy(alpha = 0.9f),
                    start = spiderCenter,
                    end = Offset(
                        spiderCenter.x + legLen * cos(legAngle).toFloat(),
                        spiderCenter.y + legLen * sin(legAngle).toFloat()
                    ),
                    strokeWidth = 1.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2A1204),
                        Color(0xFF1A0A04),
                        Color(0xFF0A0610)
                    ),
                    center = spiderCenter - Offset(radius * 0.012f, radius * 0.012f),
                    radius = radius * 0.055f
                ),
                radius = radius * 0.055f,
                center = spiderCenter
            )
            val eyeOff = radius * 0.018f
            listOf(-1f, 1f).forEach { side ->
                drawCircle(
                    color = Color(0xFFFF9100).copy(alpha = flicker * (0.75f + boltPhase * 0.25f)),
                    radius = radius * 0.013f,
                    center = spiderCenter + Offset(side * eyeOff, -eyeOff * 0.4f)
                )
            }
            if (boltPhase > 0.82f) {
                drawCircle(
                    color = Color(0xFFFF9100).copy(alpha = (boltPhase - 0.82f) * 3f),
                    radius = radius * 0.05f,
                    center = housing
                )
            }
        }
    }
}

private fun DrawScope.drawHalloweenGhostRing(
    center: Offset,
    radius: Float,
    rotationDeg: Float,
    pulsePhase: Float
) {
    withTransform({ rotate(rotationDeg, center) }) {
        for (i in 0 until 4) {
            val angle = Math.toRadians(i * 90.0 - 90.0)
            val bob = sin(pulsePhase * 1.2f + i * 1.4f) * radius * 0.03f
            val pos = Offset(
                center.x + radius * cos(angle).toFloat(),
                center.y + radius * sin(angle).toFloat() + bob
            )
            val alpha = 0.55f + 0.25f * sin(pulsePhase + i)
            drawHalloweenGhost(pos, radius * 0.22f, alpha)
        }
        drawCircle(
            color = Color.White.copy(alpha = 0.15f),
            radius = radius * 0.95f,
            center = center,
            style = Stroke(width = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
        )
    }
}

private fun DrawScope.drawHalloweenCandleLock(
    center: Offset,
    radius: Float,
    flicker: Float,
    pulsePhase: Float
) {
    // Short candle stub — sits between nose and mouth, doesn't cover the carving
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFFFF8E1), Color(0xFFD7CCC8), Color(0xFF8D6E63))),
        topLeft = Offset(center.x - radius * 0.07f, center.y - radius * 0.04f),
        size = Size(radius * 0.14f, radius * 0.16f),
        cornerRadius = CornerRadius(radius * 0.02f)
    )
    val flameH = radius * (0.14f + flicker * 0.04f)
    val flame = Path().apply {
        moveTo(center.x, center.y - radius * 0.06f - flameH)
        cubicTo(
            center.x + radius * 0.05f, center.y - radius * 0.06f - flameH * 0.55f,
            center.x + radius * 0.04f, center.y - radius * 0.04f,
            center.x, center.y - radius * 0.04f
        )
        cubicTo(
            center.x - radius * 0.04f, center.y - radius * 0.04f,
            center.x - radius * 0.05f, center.y - radius * 0.06f - flameH * 0.55f,
            center.x, center.y - radius * 0.06f - flameH
        )
        close()
    }
    drawPath(
        path = flame,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFF176).copy(alpha = flicker),
                Color(0xFFFF9100).copy(alpha = flicker * 0.9f),
                Color(0xFFFF5722).copy(alpha = flicker * 0.6f)
            ),
            startY = center.y - radius * 0.06f - flameH,
            endY = center.y - radius * 0.04f
        )
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFF9100).copy(alpha = flicker * 0.4f), Color.Transparent),
            center = center - Offset(0f, radius * 0.08f),
            radius = radius * 0.2f
        ),
        radius = radius * 0.2f,
        center = center - Offset(0f, radius * 0.06f)
    )
}

private fun DrawScope.drawHalloweenMiniPumpkinLight(
    center: Offset,
    size: Float,
    flicker: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFB74D), Color(0xFFFF7A1A), Color(0xFFBF4F00)),
            center = center - Offset(size * 0.1f, size * 0.08f),
            radius = size
        ),
        radius = size,
        center = center
    )
    listOf(-1f, 1f).forEach { side ->
        drawCircle(
            color = Color(0xFFFFF176).copy(alpha = flicker * 0.85f),
            radius = size * 0.14f,
            center = center + Offset(side * size * 0.28f, -size * 0.08f)
        )
    }
    drawArc(
        color = Color(0xFF1A0A04).copy(alpha = 0.7f),
        startAngle = 10f,
        sweepAngle = 160f,
        useCenter = false,
        topLeft = Offset(center.x - size * 0.55f, center.y - size * 0.05f),
        size = Size(size * 1.1f, size * 0.5f),
        style = Stroke(width = size * 0.18f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawHalloweenBlackCat(
    center: Offset,
    radius: Float,
    pulsePhase: Float
) {
    val catCenter = center + Offset(radius * 0.62f, radius * 0.55f)
    val s = radius * 0.14f
    // Body
    drawOval(
        color = Color(0xFF1A0A04),
        topLeft = Offset(catCenter.x - s * 0.9f, catCenter.y - s * 0.35f),
        size = Size(s * 1.8f, s * 0.9f)
    )
    // Head
    drawCircle(color = Color(0xFF1A0A04), radius = s * 0.55f, center = catCenter + Offset(s * 0.55f, -s * 0.15f))
    // Ears
    listOf(-1f, 1f).forEach { side ->
        val ear = Path().apply {
            val ex = catCenter.x + s * 0.55f + side * s * 0.28f
            val ey = catCenter.y - s * 0.55f
            moveTo(ex, ey)
            lineTo(ex - side * s * 0.22f, ey - s * 0.38f)
            lineTo(ex + side * s * 0.12f, ey - s * 0.08f)
            close()
        }
        drawPath(ear, color = Color(0xFF1A0A04))
    }
    // Eyes — orange slits
    val eyeGlow = 0.7f + 0.3f * sin(pulsePhase * 2f)
    listOf(-1f, 1f).forEach { side ->
        drawOval(
            color = Color(0xFFFF9100).copy(alpha = eyeGlow),
            topLeft = Offset(
                catCenter.x + s * 0.55f + side * s * 0.14f - s * 0.05f,
                catCenter.y - s * 0.22f
            ),
            size = Size(s * 0.1f, s * 0.14f)
        )
    }
    // Tail
    drawArc(
        color = Color(0xFF1A0A04),
        startAngle = 200f,
        sweepAngle = 120f,
        useCenter = false,
        topLeft = Offset(catCenter.x - s * 1.5f, catCenter.y - s * 0.5f),
        size = Size(s * 1.4f, s * 1.2f),
        style = Stroke(width = s * 0.22f, cap = StrokeCap.Round)
    )
}

@Composable
fun AnimatedVaultIconHalloween(modifier: Modifier = Modifier, iconSize: Dp = 76.dp, animated: Boolean = true) {
    VaultIconMotionLayer(modifier = modifier, iconSize = iconSize, animated = animated) { center, radius, outerRotation, innerRotation, pulsePhase, themeColors ->
        val flicker = (0.72f + 0.28f * sin(pulsePhase * 0.9f)).coerceIn(0.62f, 1f)

        // Moonlit Halloween night
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1A1028), Color(0xFF0A0610), Color(0xFF020104)),
                center = center,
                radius = radius * 1.02f
            ),
            radius = radius * 1.02f,
            center = center
        )

        // Harvest moon — upper-left glow bleeding into the scene
        val moonCenter = center + Offset(-radius * 0.38f, -radius * 0.42f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF176).copy(alpha = 0.55f),
                    Color(0xFFFFB74D).copy(alpha = 0.35f),
                    Color(0xFFFF9100).copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = moonCenter,
                radius = radius * 0.55f
            ),
            radius = radius * 0.55f,
            center = moonCenter
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF8E1), Color(0xFFFFE082), Color(0xFFFFB74D)),
                center = moonCenter - Offset(radius * 0.04f, radius * 0.04f),
                radius = radius * 0.14f
            ),
            radius = radius * 0.14f,
            center = moonCenter
        )

        // Drifting autumn leaves / embers
        for (s in 0 until 7) {
            val sx = center.x + radius * (0.6f * cos(pulsePhase * 0.13f + s * 1.8f))
            val sy = center.y + radius * (0.6f * sin(pulsePhase * 0.1f + s * 2.2f))
            val sa = 0.18f + 0.38f * sin(pulsePhase * 1.9f + s)
            drawCircle(color = Color(0xFFFF7A1A).copy(alpha = sa), radius = 1.2f + (s % 2), center = Offset(sx, sy))
        }

        // Moonbeam fog sweeps — orange and purple, classic Halloween sky
        withTransform({ rotate(outerRotation * 0.55f - 90f, center) }) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFF9100).copy(alpha = 0.22f),
                        Color(0xFF9B30FF).copy(alpha = 0.32f),
                        Color(0xFFFF5722).copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f)
            )
        }
        withTransform({ rotate(-outerRotation * 0.4f + 60f, center) }) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFFB74D).copy(alpha = 0.18f),
                        Color(0xFF6A1FB8).copy(alpha = 0.24f),
                        Color.Transparent
                    ),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f)
            )
        }

        // Orange sonar ripples — pumpkin-energy pulse
        listOf(
            (pulsePhase / (2f * PI.toFloat())) % 1f,
            ((pulsePhase / (2f * PI.toFloat())) + 0.5f) % 1f
        ).forEach { phase ->
            val ringR = radius * (0.2f + phase * 0.76f)
            drawCircle(
                color = Color(0xFFFF9100).copy(alpha = (1f - phase) * 0.32f),
                radius = ringR,
                center = center,
                style = Stroke(width = 1.6.dp.toPx())
            )
        }

        // Candy-corn vault ring — white / orange / yellow segments
        val candyColors = listOf(Color.White, Color(0xFFFF9100), Color(0xFFFFEB3B))
        withTransform({ rotate(outerRotation, center) }) {
            for (seg in 0 until 12) {
                drawArc(
                    color = candyColors[seg % 3],
                    startAngle = seg * 30f,
                    sweepAngle = 30f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius * 0.96f, center.y - radius * 0.96f),
                    size = Size(radius * 1.92f, radius * 1.92f),
                    style = Stroke(width = radius * 0.058f, cap = StrokeCap.Butt)
                )
            }
        }

        drawCircle(
            color = Color(0xFF1A0A04).copy(alpha = 0.45f),
            radius = radius * 0.9f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Cobwebs with a spider perched on one
        for (corner in 0 until 4) {
            val baseAngle = Math.toRadians(45.0 + corner * 90.0)
            val webOrigin = Offset(
                center.x + radius * 0.82f * cos(baseAngle).toFloat(),
                center.y + radius * 0.82f * sin(baseAngle).toFloat()
            )
            for (strand in 0 until 4) {
                val spread = Math.toRadians((strand - 1.5) * 16.0)
                val a = baseAngle + spread
                drawLine(
                    color = Color.White.copy(alpha = 0.14f + strand * 0.03f),
                    start = webOrigin,
                    end = Offset(
                        webOrigin.x + radius * 0.24f * cos(a).toFloat(),
                        webOrigin.y + radius * 0.24f * sin(a).toFloat()
                    ),
                    strokeWidth = 0.9.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            if (corner == 1) {
                val spiderPos = webOrigin + Offset(radius * 0.08f, radius * 0.06f)
                drawCircle(color = Color(0xFF1A0A04), radius = radius * 0.028f, center = spiderPos)
                for (leg in 0 until 4) {
                    val la = baseAngle + Math.toRadians((leg - 1.5) * 40.0)
                    drawLine(
                        color = Color(0xFF1A0A04),
                        start = spiderPos,
                        end = Offset(
                            spiderPos.x + radius * 0.06f * cos(la).toFloat(),
                            spiderPos.y + radius * 0.06f * sin(la).toFloat()
                        ),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Jack-o'-lantern with witch hat IS the vault dial
        drawHalloweenPumpkinDial(center, radius, outerRotation, flicker)

        // Spider bolts seal the graveyard vault
        drawHalloweenSpiderBolts(center, radius, pulsePhase, outerRotation, flicker)

        // Counter-spinning ghost ring orbits inside the pumpkin
        drawHalloweenGhostRing(center, radius * 0.52f, -innerRotation * 0.85f, pulsePhase)

        // Candle-flame vault lock — small, sits in the nose zone
        drawHalloweenCandleLock(center, radius * 0.22f, flicker, pulsePhase)

        // Mini jack-o'-lantern light strand on the outer rim. r + light radius (0.055) was 0.97 +
        // 0.055 = 1.025x radius — past the icon's own square drawing bounds near the four
        // cardinal angles, clipping a sliver off each light as it rotated through them. Pulled in
        // to keep the full light circle inside at every angle.
        for (i in 0 until 10) {
            val angle = Math.toRadians(i * 36.0 + outerRotation * 0.25)
            val r = radius * 0.93f
            val pos = Offset(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat())
            if (i > 0) {
                val prevAngle = Math.toRadians((i - 1) * 36.0 + outerRotation * 0.25)
                val prevPos = Offset(
                    center.x + r * cos(prevAngle).toFloat(),
                    center.y + r * sin(prevAngle).toFloat()
                )
                drawLine(
                    color = Color(0xFF1A0A04).copy(alpha = 0.65f),
                    start = prevPos,
                    end = pos,
                    strokeWidth = 1.3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            drawHalloweenMiniPumpkinLight(pos, radius * 0.055f, flicker)
        }

        // Bats silhouetted against the moon
        for (b in 0 until 3) {
            val batAngle = pulsePhase * 0.32f + b * 2.2f
            val batDist = radius * (0.52f + b * 0.1f)
            val batPos = Offset(
                center.x + batDist * cos(batAngle),
                center.y + batDist * sin(batAngle) - radius * 0.08f
            )
            val wingFlap = sin(pulsePhase * 4.2f + b * 2f) * radius * 0.045f
            drawLine(
                color = Color(0xFF1A0A04).copy(alpha = 0.9f),
                start = batPos,
                end = batPos + Offset(-radius * 0.08f, -wingFlap - radius * 0.02f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF1A0A04).copy(alpha = 0.9f),
                start = batPos,
                end = batPos + Offset(radius * 0.08f, -wingFlap - radius * 0.02f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(color = Color(0xFFFF9100).copy(alpha = 0.85f), radius = 1.3f, center = batPos)
        }

        // Lone black cat watching from the graveyard edge
        drawHalloweenBlackCat(center, radius, pulsePhase)

        // Grave fog along the bottom
        for (i in 0 until 4) {
            val mistX = center.x + radius * (-0.5f + i * 0.33f)
            val mistY = center.y + radius * 0.78f + sin(pulsePhase * 0.55f + i) * radius * 0.015f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF9B30FF).copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(mistX, mistY),
                    radius = radius * 0.12f
                ),
                topLeft = Offset(mistX - radius * 0.12f, mistY - radius * 0.045f),
                size = Size(radius * 0.24f, radius * 0.09f)
            )
        }

        // One drifting sheet ghost in the background
        val ghostDrift = sin(pulsePhase * 0.8f) * radius * 0.04f
        drawHalloweenGhost(
            center = center + Offset(-radius * 0.55f + ghostDrift, -radius * 0.35f),
            size = radius * 0.2f,
            alpha = 0.45f + 0.15f * sin(pulsePhase * 1.1f)
        )

        drawCircle(
            color = themeColors.Teal.copy(alpha = 0.28f),
            radius = radius * 0.99f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

// ---------------------------------------------------------------------------------
// Christmas — glossy ornament vault dial: aurora chamber, candy-cane ring, holly bolts that
// seal like Mag Lock, counter-spinning snowflake iris, and a blazing North Star hub.
// VaultIconMotionLayer only — pulse/rotation read inside draw, no extra recomposition.
// ---------------------------------------------------------------------------------

private fun DrawScope.drawChristmasNorthStar(
    center: Offset,
    radius: Float,
    pulsePhase: Float
) {
    val glow = 0.68f + 0.32f * sin(pulsePhase * 1.35f)
    // Gift ribbon behind the lock — vertical red, horizontal gold
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFB71C1C), Color(0xFFE0233D), Color(0xFFB71C1C)),
            start = Offset(center.x, center.y - radius * 0.22f),
            end = Offset(center.x, center.y + radius * 0.22f)
        ),
        start = Offset(center.x, center.y - radius * 0.22f),
        end = Offset(center.x, center.y + radius * 0.22f),
        strokeWidth = radius * 0.09f,
        cap = StrokeCap.Round
    )
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFE0A017), Color(0xFFFFE082), Color(0xFFE0A017)),
            start = Offset(center.x - radius * 0.22f, center.y),
            end = Offset(center.x + radius * 0.22f, center.y)
        ),
        start = Offset(center.x - radius * 0.22f, center.y),
        end = Offset(center.x + radius * 0.22f, center.y),
        strokeWidth = radius * 0.085f,
        cap = StrokeCap.Round
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = glow),
                Color(0xFFFFF0B0).copy(alpha = glow * 0.9f),
                Color(0xFFFFC93C).copy(alpha = glow * 0.5f),
                Color.Transparent
            ),
            center = center,
            radius = radius * 0.28f
        ),
        radius = radius * 0.28f,
        center = center
    )
    val starR = radius * 0.16f
    val starPath = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) starR else starR * 0.4f
        val angle = Math.toRadians(-90.0 + i * 36.0)
        val x = center.x + r * cos(angle).toFloat()
        val y = center.y + r * sin(angle).toFloat()
        if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
    }
    starPath.close()
    drawPath(
        path = starPath,
        brush = Brush.linearGradient(
            colors = listOf(Color.White, Color(0xFFFFE082), Color(0xFFFFB300)),
            start = Offset(center.x, center.y - starR),
            end = Offset(center.x, center.y + starR)
        )
    )
    for (ray in 0 until 8) {
        val sparkle = sin(pulsePhase * 3.1f + ray * 1.25f) * 0.5f + 0.5f
        if (sparkle > 0.38f) {
            val rayLen = radius * (0.24f + sparkle * 0.16f)
            val a = Math.toRadians(ray * 45.0)
            drawLine(
                color = Color.White.copy(alpha = (sparkle - 0.28f) * 1.25f),
                start = Offset(center.x - rayLen * cos(a).toFloat(), center.y - rayLen * sin(a).toFloat()),
                end = Offset(center.x + rayLen * cos(a).toFloat(), center.y + rayLen * sin(a).toFloat()),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
    drawCircle(color = Color(0xFFE0233D).copy(alpha = 0.9f), radius = radius * 0.032f, center = center)
}

private fun DrawScope.drawChristmasSnowflakeDial(
    center: Offset,
    radius: Float,
    rotationDeg: Float,
    pulsePhase: Float
) {
    withTransform({ rotate(rotationDeg, center) }) {
        val armLen = radius * 0.64f
        val branch = radius * 0.15f
        for (i in 0 until 6) {
            rotate(i * 60f, center) {
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.98f),
                            Color(0xFFE1F5FE).copy(alpha = 0.9f),
                            Color(0xFF4FC3F7).copy(alpha = 0.6f)
                        ),
                        start = center,
                        end = Offset(center.x, center.y - armLen)
                    ),
                    start = center,
                    end = Offset(center.x, center.y - armLen),
                    strokeWidth = 3.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                val mid = Offset(center.x, center.y - armLen * 0.55f)
                drawLine(
                    color = Color.White.copy(alpha = 0.95f),
                    start = mid + Offset(-branch, 0f),
                    end = mid + Offset(branch, 0f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFE1F5FE).copy(alpha = 0.9f),
                    start = Offset(center.x, center.y - armLen * 0.82f),
                    end = Offset(center.x + branch * 0.68f, center.y - armLen * 0.82f),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFE1F5FE).copy(alpha = 0.9f),
                    start = Offset(center.x, center.y - armLen * 0.82f),
                    end = Offset(center.x - branch * 0.68f, center.y - armLen * 0.82f),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        val hubPulse = 0.86f + 0.14f * sin(pulsePhase * 1.15f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = hubPulse),
                    Color(0xFF81D4FA).copy(alpha = 0.8f),
                    Color(0xFF0277BD).copy(alpha = 0.92f)
                ),
                center = center,
                radius = radius * 0.17f
            ),
            radius = radius * 0.17f,
            center = center
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = radius * 0.17f,
            center = center,
            style = Stroke(width = 1.4.dp.toPx())
        )
    }
}

private fun DrawScope.drawChristmasOrnamentBody(
    center: Offset,
    radius: Float,
    rotationDeg: Float,
    pulsePhase: Float
) {
    withTransform({ rotate(rotationDeg, center) }) {
        // Deep shadow under the ornament
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent),
                center = center + Offset(0f, radius * 0.08f),
                radius = radius * 0.72f
            ),
            topLeft = Offset(center.x - radius * 0.72f, center.y - radius * 0.58f),
            size = Size(radius * 1.44f, radius * 1.36f)
        )
        // Glossy crimson ornament sphere
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF6B7F),
                    Color(0xFFE0233D),
                    Color(0xFFA10E24),
                    Color(0xFF5A0612)
                ),
                center = center - Offset(radius * 0.18f, radius * 0.22f),
                radius = radius * 0.82f
            ),
            radius = radius * 0.78f,
            center = center
        )
        // Gold filigree latitude bands
        for (band in 0 until 3) {
            val yOff = radius * (-0.28f + band * 0.28f)
            drawArc(
                color = Color(0xFFFFD54F).copy(alpha = 0.35f + band * 0.08f),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.74f, center.y + yOff - radius * 0.08f),
                size = Size(radius * 1.48f, radius * 0.16f),
                style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        // Specular highlight sweep
        drawArc(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.72f), Color.White.copy(alpha = 0.08f), Color.Transparent),
                start = Offset(center.x - radius * 0.4f, center.y - radius * 0.5f),
                end = Offset(center.x + radius * 0.2f, center.y - radius * 0.1f)
            ),
            startAngle = 225f,
            sweepAngle = 55f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 0.78f, center.y - radius * 0.78f),
            size = Size(radius * 1.56f, radius * 1.56f),
            style = Stroke(width = radius * 0.07f, cap = StrokeCap.Round)
        )
        // Ornament cap — gold bands at the pole
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFB300), Color(0xFFE0A017))),
            topLeft = Offset(center.x - radius * 0.16f, center.y - radius * 0.92f),
            size = Size(radius * 0.32f, radius * 0.12f),
            cornerRadius = CornerRadius(radius * 0.03f)
        )
        drawRoundRect(
            color = Color(0xFFB8860B).copy(alpha = 0.55f),
            topLeft = Offset(center.x - radius * 0.18f, center.y - radius * 0.82f),
            size = Size(radius * 0.36f, radius * 0.025f),
            cornerRadius = CornerRadius(radius * 0.01f)
        )
        // Hanging loop
        drawCircle(
            color = Color(0xFFFFB300),
            radius = radius * 0.055f,
            center = Offset(center.x, center.y - radius * 0.98f),
            style = Stroke(width = 2.2.dp.toPx())
        )
        // Warm inner glow through the ornament glass
        val innerGlow = 0.55f + 0.45f * sin(pulsePhase * 0.95f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF8E1).copy(alpha = innerGlow * 0.45f),
                    Color(0xFFFF8A80).copy(alpha = innerGlow * 0.18f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 0.55f
            ),
            radius = radius * 0.55f,
            center = center
        )
    }
}

private fun DrawScope.drawChristmasHollyBolts(
    center: Offset,
    radius: Float,
    pulsePhase: Float,
    rotationDeg: Float
) {
    withTransform({ rotate(rotationDeg, center) }) {
        for (i in 0 until 6) {
            val angle = Math.toRadians((60.0 * i) - 90.0)
            val boltPhase = sin(pulsePhase + i * 1.08f) * 0.5f + 0.5f
            val housingR = radius * 0.68f
            val housing = Offset(
                center.x + housingR * cos(angle).toFloat(),
                center.y + housingR * sin(angle).toFloat()
            )
            val slide = radius * 0.07f * (1f - boltPhase)
            val berryCenter = Offset(
                housing.x - slide * cos(angle).toFloat(),
                housing.y - slide * sin(angle).toFloat()
            )
            // Holly leaf pair
            val leafLen = radius * 0.11f
            val perpX = -sin(angle).toFloat()
            val perpY = cos(angle).toFloat()
            drawLine(
                color = Color(0xFF1FA34D).copy(alpha = 0.85f),
                start = berryCenter + Offset(-perpX * leafLen, -perpY * leafLen),
                end = berryCenter + Offset(perpX * leafLen * 0.3f, perpY * leafLen * 0.3f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF0E7A32).copy(alpha = 0.85f),
                start = berryCenter + Offset(perpX * leafLen, perpY * leafLen),
                end = berryCenter + Offset(-perpX * leafLen * 0.3f, -perpY * leafLen * 0.3f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Ornament berry bolt
            val berryColors = listOf(Color(0xFFE0233D), Color(0xFFFFD54F), Color(0xFF1FA34D))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        berryColors[i % 3],
                        berryColors[i % 3].copy(alpha = 0.75f)
                    ),
                    center = berryCenter - Offset(radius * 0.015f, radius * 0.015f),
                    radius = radius * 0.065f
                ),
                radius = radius * 0.065f,
                center = berryCenter
            )
            if (boltPhase > 0.8f) {
                drawCircle(
                    color = Color(0xFFFFE082).copy(alpha = (boltPhase - 0.8f) * 3.2f),
                    radius = radius * 0.055f,
                    center = housing
                )
            }
        }
    }
}

@Composable
fun AnimatedVaultIconChristmas(modifier: Modifier = Modifier, iconSize: Dp = 76.dp, animated: Boolean = true) {
    VaultIconMotionLayer(modifier = modifier, iconSize = iconSize, animated = animated) { center, radius, outerRotation, innerRotation, pulsePhase, themeColors ->
        // Midnight aurora chamber
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1A2840), Color(0xFF0A1220), Color(0xFF020508)),
                center = center,
                radius = radius * 1.02f
            ),
            radius = radius * 1.02f,
            center = center
        )

        // Distant stars
        for (s in 0 until 9) {
            val sx = center.x + radius * (0.62f * cos(pulsePhase * 0.13f + s * 1.7f))
            val sy = center.y + radius * (0.62f * sin(pulsePhase * 0.11f + s * 2.4f))
            val sa = 0.2f + 0.45f * sin(pulsePhase * 1.8f + s * 0.9f)
            drawCircle(color = Color.White.copy(alpha = sa), radius = 1f + (s % 3) * 0.6f, center = Offset(sx, sy))
        }

        // Aurora borealis — triple-layer sweep, cranked up vs the tame snow-globe pass
        withTransform({ rotate(outerRotation * 0.62f - 90f, center) }) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF1FA34D).copy(alpha = 0.22f),
                        Color(0xFF4DD0E1).copy(alpha = 0.52f),
                        Color(0xFFE0233D).copy(alpha = 0.38f),
                        Color(0xFFFFD54F).copy(alpha = 0.32f),
                        Color(0xFF1FA34D).copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f)
            )
        }
        withTransform({ rotate(-outerRotation * 0.44f + 35f, center) }) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF7B1FA2).copy(alpha = 0.18f),
                        Color(0xFF26A69A).copy(alpha = 0.42f),
                        Color.Transparent
                    ),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f)
            )
        }
        withTransform({ rotate(outerRotation * 0.28f + 120f, center) }) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFF6B7F).copy(alpha = 0.15f),
                        Color(0xFF6FDB8F).copy(alpha = 0.28f),
                        Color.Transparent
                    ),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f)
            )
        }

        // Magic sonar ripples
        listOf(
            (pulsePhase / (2f * PI.toFloat())) % 1f,
            ((pulsePhase / (2f * PI.toFloat())) + 0.5f) % 1f
        ).forEach { phase ->
            val ringR = radius * (0.18f + phase * 0.78f)
            drawCircle(
                color = Color(0xFF4DD0E1).copy(alpha = (1f - phase) * 0.38f),
                radius = ringR,
                center = center,
                style = Stroke(width = 1.8.dp.toPx())
            )
        }

        // Candy-cane vault ring — spins with the ornament dial
        withTransform({ rotate(outerRotation, center) }) {
            for (seg in 0 until 8) {
                val start = seg * 45f
                drawArc(
                    color = if (seg % 2 == 0) Color(0xFFE0233D) else Color.White,
                    startAngle = start,
                    sweepAngle = 45f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius * 0.96f, center.y - radius * 0.96f),
                    size = Size(radius * 1.92f, radius * 1.92f),
                    style = Stroke(width = radius * 0.065f, cap = StrokeCap.Butt)
                )
            }
        }

        // Vault door ring
        drawCircle(
            color = Color(0xFFFFD54F).copy(alpha = 0.35f),
            radius = radius * 0.9f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // The ornament IS the vault dial — glossy, spinning, unmistakably Christmas
        drawChristmasOrnamentBody(center, radius, outerRotation, pulsePhase)

        // Holly berry bolts seal the vault like Mag Lock
        drawChristmasHollyBolts(center, radius, pulsePhase, outerRotation)

        // Counter-spinning snowflake iris etched into the ornament
        drawChristmasSnowflakeDial(center, radius * 0.52f, -innerRotation * 0.85f, pulsePhase)

        // North Star + gift ribbon vault lock
        drawChristmasNorthStar(center, radius * 0.38f, pulsePhase)

        // Christmas-light strand on the outer rim
        val bulbColors = listOf(
            Color(0xFFFF4D4D),
            Color(0xFF3DDC6E),
            Color(0xFFFFD24D),
            Color(0xFF64B5F6)
        )
        // Same fix as the Halloween light strand: r + the glow halo's own radius (0.08) was
        // 1.05x radius, clipping the halo near the four cardinal angles as it rotated through.
        for (i in 0 until 12) {
            val angle = Math.toRadians(i * 30.0 + outerRotation * 0.3)
            val r = radius * 0.88f
            val pos = Offset(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat())
            val twinkle = 0.4f + 0.6f * sin(pulsePhase * 2.6f + i * 0.9f)
            if (i > 0) {
                val prevAngle = Math.toRadians((i - 1) * 30.0 + outerRotation * 0.3)
                val prevPos = Offset(
                    center.x + r * cos(prevAngle).toFloat(),
                    center.y + r * sin(prevAngle).toFloat()
                )
                drawLine(
                    color = Color(0xFF2E4A32).copy(alpha = 0.55f),
                    start = prevPos,
                    end = pos,
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        bulbColors[i % 4].copy(alpha = 0.4f + 0.6f * twinkle),
                        Color.Transparent
                    ),
                    center = pos,
                    radius = radius * 0.08f
                ),
                radius = radius * 0.08f,
                center = pos
            )
            drawCircle(
                color = bulbColors[i % 4].copy(alpha = 0.55f + 0.45f * twinkle),
                radius = radius * 0.04f,
                center = pos
            )
            drawCircle(
                color = Color.White.copy(alpha = twinkle * 0.8f),
                radius = radius * 0.015f,
                center = pos - Offset(radius * 0.013f, radius * 0.013f)
            )
        }

        // Falling snow — eight flakes drifting upward inside the chamber
        for (i in 0 until 8) {
            val t = (pulsePhase * 0.22f + i * 0.31f) % 1f
            val angle = i * 0.92f + pulsePhase * 0.4f
            val dist = radius * (0.2f + t * 0.72f)
            val pos = Offset(
                center.x + dist * cos(angle),
                center.y + dist * sin(angle) - radius * 0.04f
            )
            drawCircle(
                color = Color.White.copy(alpha = (1f - t) * 0.6f + 0.2f),
                radius = radius * (0.012f + (i % 3) * 0.005f),
                center = pos
            )
        }

        // Icicle teeth along the bottom rim
        for (i in 0 until 5) {
            val ix = center.x + radius * (-0.55f + i * 0.275f)
            val icicleLen = radius * (0.08f + (i % 2) * 0.04f)
            val sway = sin(pulsePhase * 0.7f + i) * radius * 0.012f
            val icicleTop = center.y + radius * 0.72f
            val icicleBottom = icicleTop + icicleLen
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.7f), Color(0xFFB3E5FC).copy(alpha = 0.15f)),
                    startY = icicleTop,
                    endY = icicleBottom
                ),
                start = Offset(ix, icicleTop),
                end = Offset(ix + sway, icicleBottom),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Theme tie-in — teal vault seal
        drawCircle(
            color = themeColors.Teal.copy(alpha = 0.28f),
            radius = radius * 0.99f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun WackyVaultIconMotionLayer(
    modifier: Modifier = Modifier,
    iconSize: Dp = 76.dp,
    animated: Boolean = true,
    drawDoor: DrawScope.(center: Offset, radius: Float, rotationDeg: Float, innerRotationDeg: Float, wobble: Float, colors: SpotVaultColorScheme) -> Unit
) {
    val themeColors = LocalSpotVaultColors.current
    val reducedMotion = ThemeState.reduceAnimations

    var pulsePhase by remember { mutableFloatStateOf(0f) }
    var rotationDeg by remember { mutableFloatStateOf(0f) }
    if (!reducedMotion && animated) {
        LaunchedEffect(Unit) {
            var startTime = 0L
            while (isActive) {
                withFrameNanos { time ->
                    if (startTime == 0L) startTime = time
                    val elapsedSec = (time - startTime) / 1_000_000_000f
                    pulsePhase = elapsedSec * (PI.toFloat() / 2f)
                    rotationDeg = elapsedSec * 45f
                }
            }
        }
    }

    // Same fix as VaultIconMotionLayer above (see its comment) — this one was missed when that
    // fix went in. scale/alphaGlow/wobble were all read directly in the composable body, so every
    // single animation frame recomposed this whole subtree (measure, layout, modifier-chain
    // re-evaluation) rather than just redrawing it — for as long as a "Chaos Portal"-styled vault
    // icon was on screen, which for the persistent bottom-nav icon means the entire time the app
    // is open. graphicsLayer/drawWithCache below read the same pulsePhase/wobble state but
    // *inside* their lambdas, deferring straight to the draw phase instead — same animation, same
    // values, no per-frame recomposition.
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = if (reducedMotion) 1f else 1f + 0.05f * sin(pulsePhase)
                    scaleX = s
                    scaleY = s
                }
                .drawWithCache {
                    // Matches what Modifier.background(brush, shape = CircleShape) drew before —
                    // an oval spanning the full box, same as VaultIconMotionLayer's equivalent fix.
                    val glowCenter = Offset(size.width / 2f, size.height / 2f)
                    val glowRadius = size.maxDimension / 2f
                    onDrawBehind {
                        val alphaGlow = if (reducedMotion) 0.6f else 0.5f + 0.2f * sin(pulsePhase)
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(themeColors.PrimaryBright.copy(alpha = alphaGlow), Color.Transparent),
                                center = glowCenter,
                                radius = glowRadius
                            ),
                            topLeft = Offset.Zero,
                            size = size
                        )
                    }
                }
        )

        Box(
            modifier = Modifier
                .size(iconSize)
                .drawWithCache {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2
                    onDrawBehind {
                        val wobble = if (reducedMotion) 0f else sin(pulsePhase)
                        drawDoor(center, radius, rotationDeg, rotationDeg * 1.5f, wobble, themeColors)
                    }
                }
        )
    }
}

@Composable
fun VaultIconStylePicker(
    selectedStyle: String,
    onStyleSelected: (String) -> Unit,
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val seasonallySortedVaultIconStyles = getSeasonallySortedItems(VaultIconStyles) { it.activeMonths }
        seasonallySortedVaultIconStyles.forEach { (id, title, subtitle) ->
            val isSelected = selectedStyle == id
            val locked = isLocked(id)
            Box(modifier = Modifier.width(108.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (locked) 0.45f else 1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) SpotVaultColors.PrimaryDeep.copy(alpha = 0.35f)
                        else SpotVaultColors.Surface.copy(alpha = 0.85f)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) SpotVaultColors.Teal else SpotVaultColors.Outline.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { if (locked) onLockedClick() else onStyleSelected(id) }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedVaultIcon(modifier = Modifier.size(72.dp), style = id, animated = false)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SpotVaultColors.OnSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = SpotVaultColors.Muted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (locked) {
                PremiumLockBadge(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
            }
        }
    }
    TrailingScrollHint(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
fun ButtonStylePicker(
    selectedStyle: String,
    onStyleSelected: (String) -> Unit,
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ButtonStyleOptions.forEach { (id, title, subtitle) ->
            val isSelected = selectedStyle == id
            val locked = isLocked(id)
            Box(modifier = Modifier.width(108.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (locked) 0.45f else 1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) SpotVaultColors.PrimaryDeep.copy(alpha = 0.35f)
                        else SpotVaultColors.Surface.copy(alpha = 0.85f)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) SpotVaultColors.Teal else SpotVaultColors.Outline.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { if (locked) onLockedClick() else onStyleSelected(id) }
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val previewShape = when (id) {
                    "capsule" -> RoundedCornerShape(percent = 50)
                    "chamfer" -> CutCornerShape(10.dp)
                    "wild" -> wildButtonPreviewShape()
                    "leaf" -> leafButtonPreviewShape()
                    "tactical" -> tacticalButtonPreviewShape()
                    "hex" -> hexButtonPreviewShape()
                    "wave" -> waveButtonPreviewShape()
                    else -> RoundedCornerShape(10.dp)
                }
                when (id) {
                    "wild" -> WildButtonPreview()
                    "leaf" -> LeafButtonPreview()
                    "tactical" -> TacticalButtonPreview()
                    "hex" -> HexButtonPreview()
                    "wave" -> WaveButtonPreview()
                    else -> Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(34.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(SpotVaultColors.Primary, SpotVaultColors.PrimaryBright)
                                ),
                                previewShape
                            )
                            .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.35f), previewShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "BUTTON",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = SpotVaultColors.OnPrimary,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SpotVaultColors.OnSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = SpotVaultColors.Muted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (locked) {
                PremiumLockBadge(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
            }
        }
    }
    TrailingScrollHint(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
fun SpotVaultBottomBar(
    onVaultClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAppearanceClick: () -> Unit = {},
    activeDestination: BottomNavDestination? = null,
    showSettings: Boolean = true,
    modifier: Modifier = Modifier
) {
    val vaultSoundContext = LocalContext.current
    val vaultSoundPrefs = remember { vaultSoundContext.getSharedPreferences("SpotVaultPrefs", android.content.Context.MODE_PRIVATE) }
    val navScale = rememberShortScreenNavScale()
    val barHeight = (96f * navScale).dp
    val pillHeight = (60f * navScale).dp
    val vaultButtonSize = (96f * navScale).dp
    val sideButtonSize = (48f * navScale).dp
    val vaultIconSize = (76f * navScale).dp
    val pillCornerRadius = (30f * navScale).dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pillHeight)
                .background(SpotVaultColors.Surface.copy(alpha = 0.5f), RoundedCornerShape(pillCornerRadius))
                .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.2f), RoundedCornerShape(pillCornerRadius))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSettings) {
                BottomNavSideButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Settings",
                    selected = activeDestination == BottomNavDestination.SETTINGS,
                    onClick = onSettingsClick,
                    modifier = Modifier.padding(start = 16.dp),
                    buttonSize = sideButtonSize
                )
            } else {
                Spacer(modifier = Modifier.padding(start = 16.dp).size(sideButtonSize))
            }

            Spacer(modifier = Modifier.weight(1f))

            if (showSettings) {
                BottomNavSideButton(
                    icon = Icons.Default.Palette,
                    contentDescription = "Appearance & Themes",
                    selected = false,
                    onClick = onAppearanceClick,
                    modifier = Modifier.padding(end = 16.dp),
                    buttonSize = sideButtonSize
                )
            } else {
                Spacer(modifier = Modifier.padding(end = 16.dp).size(sideButtonSize))
            }
        }

        // Center Vault Button, visually popping out
        Box(
            modifier = Modifier
                .size(vaultButtonSize)
                .then(
                    if (activeDestination == BottomNavDestination.VAULT) {
                        Modifier.border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                listOf(SpotVaultColors.PrimaryBright, SpotVaultColors.Teal)
                            ),
                            shape = CircleShape
                        )
                    } else {
                        Modifier
                    }
                )
                .semantics {
                    contentDescription = "Open Vault"
                    role = Role.Button
                }
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        performAppHaptic(vaultSoundContext, vaultSoundPrefs)
                        AppSounds.playVaultSound(vaultSoundContext, vaultSoundPrefs)
                        onVaultClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVaultIcon(
                modifier = Modifier.fillMaxSize(),
                iconSize = vaultIconSize
            )
        }
    }
}

@Composable
private fun BottomNavSideButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp
) {
    val shape = spotVaultButtonShape(SpotVaultButtonShapeSize.Small)
    val themeColors = LocalSpotVaultColors.current
    val tint = if (selected) Color.White else SpotVaultColors.Teal.copy(alpha = 0.55f)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val containerColor = if (selected) SpotVaultColors.PrimaryDeep else SpotVaultColors.PrimaryDeep.copy(alpha = 0.35f)
    val accentColor = themeColors.Teal

    Box(
        modifier = modifier
            .size(buttonSize)
            .graphicsLayer {
                scaleX = if (pressed) 0.96f else 1f
                scaleY = if (pressed) 0.96f else 1f
            }
            .drawBehind {
                val outline = shape.createOutline(size, LayoutDirection.Ltr, this)
                drawPhysicalButtonShell(
                    outline = outline,
                    size = size,
                    themeColors = themeColors,
                    fillStyle = if (selected) GlassButtonFillStyle.Solid else GlassButtonFillStyle.Outlined,
                    containerColor = containerColor,
                    accentColor = accentColor,
                    pressed = pressed,
                    largeSurface = false
                )
            }
            .clip(shape)
            .semantics {
                this.selected = selected
                this.role = Role.Tab
            }
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = SpotVaultColors.Teal),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
internal fun spotVaultSwitchColors() = androidx.compose.material3.SwitchDefaults.colors(
    checkedThumbColor = SpotVaultColors.OnPrimary,
    checkedTrackColor = SpotVaultColors.Teal,
    checkedBorderColor = SpotVaultColors.Teal,
    uncheckedThumbColor = SpotVaultColors.OnSurface.copy(alpha = 0.92f),
    uncheckedTrackColor = SpotVaultColors.Muted.copy(alpha = 0.35f),
    uncheckedBorderColor = SpotVaultColors.Muted.copy(alpha = 0.75f)
)

@Composable
internal fun spotVaultCheckboxColors() = androidx.compose.material3.CheckboxDefaults.colors(
    checkedColor = SpotVaultColors.Teal,
    uncheckedColor = SpotVaultColors.Muted.copy(alpha = 0.85f),
    checkmarkColor = SpotVaultColors.Ink,
    disabledUncheckedColor = SpotVaultColors.Muted.copy(alpha = 0.5f)
)
