package com.spotvault.app
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
    val OnSurface: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.OnSurface
    val Muted: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.Muted
    val Outline: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.Outline
    val Danger: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.Danger
    val OnPrimary: Color @androidx.compose.runtime.Composable get() = if (LocalSpotVaultColors.current.Primary.luminance() > 0.4f) LocalSpotVaultColors.current.Ink else androidx.compose.ui.graphics.Color.White
}

@Composable
fun SpotVaultAmbientBackground(modifier: Modifier = Modifier) {
    val themeColors = LocalSpotVaultColors.current

    val transition = rememberInfiniteTransition(label = "ambient")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height
                
                val radius1 = w * 0.55f
                val brush1 = Brush.radialGradient(
                    colors = listOf(themeColors.Primary.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset.Zero,
                    radius = radius1
                )
                
                val radius2 = w * 0.45f
                val brush2 = Brush.radialGradient(
                    colors = listOf(themeColors.Teal.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset.Zero,
                    radius = radius2
                )
                
                val radius3 = w * 0.35f
                val brush3 = Brush.radialGradient(
                    colors = listOf(themeColors.PrimaryDeep.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = radius3
                )
                
                onDrawBehind {
                    drawRect(themeColors.Void)
                    
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
fun VaultDoorButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val themeColors = LocalSpotVaultColors.current

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val spin = rememberInfiniteTransition(label = "vaultSpin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )
    val pulse by spin.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(if (pressed) 0.92f else 1f)
            .clip(CircleShape)
            .semantics { contentDescription = "Open Vault" }
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = themeColors.Teal),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.toPx() / 2f
            val c = Offset(r, r)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        themeColors.Teal.copy(alpha = 0.35f * pulse),
                        Color.Transparent
                    ),
                    center = c,
                    radius = r * 1.15f
                ),
                radius = r * 1.05f,
                center = c
            )
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(themeColors.PrimaryBright, themeColors.Primary, themeColors.PrimaryDeep)
                ),
                radius = r * 0.96f,
                center = c
            )
            drawCircle(color = themeColors.Deep, radius = r * 0.82f, center = c)
            rotate(angle, c) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            themeColors.Primary,
                            themeColors.Teal,
                            themeColors.PrimaryBright,
                            themeColors.Primary
                        ),
                        center = c
                    ),
                    radius = r * 0.72f,
                    center = c,
                    style = Stroke(width = r * 0.08f)
                )
                // Rotating fine-tick combination dial
                for (i in 0 until 60) {
                    val a = Math.toRadians(i * 6.0)
                    val isMajor = i % 5 == 0
                    val tickLength = if (isMajor) r * 0.08f else r * 0.04f
                    val startR = r * 0.68f
                    val endR = startR - tickLength
                    drawLine(
                        color = if (isMajor) themeColors.Teal else themeColors.PrimaryBright,
                        start = Offset(
                            c.x + cos(a).toFloat() * startR,
                            c.y + sin(a).toFloat() * startR
                        ),
                        end = Offset(
                            c.x + cos(a).toFloat() * endR,
                            c.y + sin(a).toFloat() * endR
                        ),
                        strokeWidth = r * 0.015f,
                        cap = StrokeCap.Round
                    )
                }
            }
            // Fixed, chunky 4-spoke handle
            for (i in 0 until 4) {
                val a = Math.toRadians(i * 90.0)
                drawLine(
                    color = themeColors.PrimaryBright.copy(alpha = 0.95f),
                    start = Offset(
                        c.x + cos(a).toFloat() * r * 0.22f,
                        c.y + sin(a).toFloat() * r * 0.22f
                    ),
                    end = Offset(
                        c.x + cos(a).toFloat() * r * 0.85f,
                        c.y + sin(a).toFloat() * r * 0.85f
                    ),
                    strokeWidth = r * 0.12f,
                    cap = StrokeCap.Round
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(themeColors.PrimaryBright, themeColors.PrimaryDeep),
                    center = Offset(c.x - r * 0.12f, c.y - r * 0.12f),
                    radius = r * 0.28f
                ),
                radius = r * 0.22f,
                center = c
            )
            drawCircle(color = themeColors.Void, radius = r * 0.12f, center = c)
            drawCircle(color = themeColors.Teal, radius = r * 0.07f, center = c)
        }
    }
}

@Composable
fun AlertSoundButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(44.dp)
            .scale(if (pressed) 0.92f else 1f)
            .clip(CircleShape)
            .background(SpotVaultColors.Elevated.copy(alpha = 0.9f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(SpotVaultColors.Teal.copy(alpha = 0.55f), SpotVaultColors.Primary.copy(alpha = 0.35f))
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = SpotVaultColors.Teal),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Alert Sound",
            tint = SpotVaultColors.TealSoft,
            modifier = Modifier.size(22.dp)
        )
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
fun GradientCtaCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 168.dp,
    tealDominant: Boolean = false,
    titleColor: Color? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(28.dp)
    val fill = if (tealDominant) {
        Brush.linearGradient(
            colors = listOf(Color(0xFF00F0FF), SpotVaultColors.Teal, Color(0xFF2A1850))
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFF2A1850), SpotVaultColors.Primary, Color(0xFF00B8CC))
        )
    }
    val borderBrush = if (tealDominant) {
        Brush.linearGradient(listOf(SpotVaultColors.TealSoft, SpotVaultColors.PrimaryBright))
    } else {
        Brush.linearGradient(listOf(SpotVaultColors.PrimaryBright, SpotVaultColors.Teal.copy(alpha = 0.7f)))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(if (pressed) 0.985f else 1f)
            .clip(shape)
            .background(fill)
            .border(1.5.dp, borderBrush, shape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = SpotVaultColors.Teal),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        val glowColor = if (tealDominant) SpotVaultColors.Primary.copy(alpha = 0.35f) else SpotVaultColors.Teal.copy(alpha = 0.35f)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(140.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent)
                    )
                )
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = titleColor ?: (if (tealDominant) SpotVaultColors.Ink else SpotVaultColors.OnSurface)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (tealDominant) SpotVaultColors.Ink.copy(alpha = 0.75f)
                else SpotVaultColors.Muted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun SpotVaultBrandHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_spotvault_mark),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "BeaconVault",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = SpotVaultColors.OnSurface,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "SAVE · SHARE · NAVIGATE · RECALL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.0.sp,
                    color = SpotVaultColors.Teal.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun AnimatedVaultIcon(modifier: Modifier = Modifier) {
    val themeColors = LocalSpotVaultColors.current

    // Sine-mapped pulse: seamless at loop boundary (no easing pause every 2s)
    val infiniteTransition = rememberInfiniteTransition(label = "VaultGlow")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulsePhase"
    )
    val scale = 1f + 0.05f * sin(pulsePhase)
    val alphaGlow = 0.5f + 0.2f * sin(pulsePhase)

    // Monotonic rotation: always adds 360° per cycle — never snaps back to 0
    val rotationAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            rotationAnim.animateTo(
                targetValue = rotationAnim.value + 360f,
                animationSpec = tween(durationMillis = 8000, easing = LinearEasing)
            )
        }
    }

    Box(modifier = modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(themeColors.PrimaryBright.copy(alpha = alphaGlow), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(76.dp)
                .drawWithCache {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2
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

                    onDrawBehind {
                        val outerRotation = rotationAnim.value % 360f
                        val innerRotation = (rotationAnim.value * 1.5f) % 360f

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
        )
    }
}

@Composable
fun SpotVaultBottomBar(
    onVaultClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(SpotVaultColors.Surface.copy(alpha = 0.5f), RoundedCornerShape(30.dp))
                .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.2f), RoundedCornerShape(30.dp))
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Alerts Button (Left)
            androidx.compose.material3.IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.padding(start = 16.dp).size(48.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = SpotVaultColors.Muted)
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
        
        // Center Vault Button, visually popping out
        Box(
            modifier = Modifier
                .size(96.dp)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onVaultClick
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVaultIcon()
        }
    }
}
