package com.spotvault.app

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

/** Celebration overlay when you mark a spot Found — in-app, widgets, and Quick Settings tile.
 * Classic and None are free; everything else is premium (see
 * [PremiumFreeTier.freeFoundSplashStyleIds]). NONE is declared first so it renders as the
 * leftmost option in [FoundSplashStylePicker] (which walks [entries] in declaration order)
 * without disturbing CLASSIC as the actual saved default — that's still driven by [fromId]'s
 * fallback and by what's written to prefs, not by position in this list. */
enum class FoundSplashStyle(val id: String, val label: String, val description: String) {
    NONE("none", "None", "Skip the celebration screen — marking Found just closes silently"),
    CLASSIC("classic", "Classic", "The polished card with a teal check — clean and satisfying"),
    FIREWORKS("fireworks", "Fireworks", "Nightfall descends, rockets streak upward, margin bursts bloom in color, willow embers cascade, and stardust lingers"),
    CONFETTI("confetti", "Confetti", "A golden halo frames the card — twin cannons salvo, ring bursts pop around the edges, and confetti fountains arc through the celebration dome"),
    VICTORY_RIPPLE("victory_ripple", "Victory Ripple", "Sacred geometry, shock rings, and pillar beams of light"),
    STAR_BURST("star_burst", "Star Burst", "Counter-rotating nova rays, comets, and orbiting stars"),
    CHAMPAGNE_POP("champagne_pop", "Champagne Pop", "A golden halo glows as the cork pops from a mini bottle below — fizz mist and bubbles spiral up around the card in a tight celebratory toast"),
    HALLOWEEN("halloween", "All Hallows' Found", "A moonlit séance — cobweb arches, spirit rings, and jack-o'-lantern triumph"),
    CHRISTMAS("christmas", "Yuletide Found", "An aurora vigil — North Star pillar, snowflake rings, and gift-box triumph");

    companion object {
        fun fromId(id: String?): FoundSplashStyle =
            entries.firstOrNull { it.id == id } ?: CLASSIC
    }
}

fun loadFoundSplashStyleFromPrefs(prefs: SharedPreferences): FoundSplashStyle {
    val saved = FoundSplashStyle.fromId(prefs.getString("found_splash_style", FoundSplashStyle.CLASSIC.id))
    return if (saved.id !in PremiumFreeTier.freeFoundSplashStyleIds && !isPremiumUnlocked(prefs)) {
        FoundSplashStyle.CLASSIC
    } else {
        saved
    }
}

const val DEFAULT_FOUND_SPLASH_TITLE = "Found it!"
const val DEFAULT_FOUND_SPLASH_SUBTITLE = "Tracking cleared — nice work getting back."

fun loadFoundSplashTitleFromPrefs(prefs: SharedPreferences): String =
    prefs.getString("found_splash_title", DEFAULT_FOUND_SPLASH_TITLE)?.ifBlank { DEFAULT_FOUND_SPLASH_TITLE }
        ?: DEFAULT_FOUND_SPLASH_TITLE

fun loadFoundSplashSubtitleFromPrefs(prefs: SharedPreferences): String =
    prefs.getString("found_splash_subtitle", DEFAULT_FOUND_SPLASH_SUBTITLE)?.ifBlank { DEFAULT_FOUND_SPLASH_SUBTITLE }
        ?: DEFAULT_FOUND_SPLASH_SUBTITLE

fun FoundSplashStyle.effectDurationMillis(): Long = when (this) {
    FoundSplashStyle.NONE -> 0L
    FoundSplashStyle.CLASSIC -> 1980L
    FoundSplashStyle.FIREWORKS -> 3000L
    FoundSplashStyle.CONFETTI -> 2900L
    FoundSplashStyle.VICTORY_RIPPLE -> 2550L
    FoundSplashStyle.STAR_BURST -> 2750L
    FoundSplashStyle.CHAMPAGNE_POP -> 2900L
    FoundSplashStyle.HALLOWEEN -> 3000L
    FoundSplashStyle.CHRISTMAS -> 3000L
}

private fun FoundSplashStyle.previewProgress(): Float = when (this) {
    FoundSplashStyle.NONE -> 0.65f
    FoundSplashStyle.CLASSIC -> 0.65f
    FoundSplashStyle.FIREWORKS -> 0.52f
    FoundSplashStyle.CONFETTI -> 0.52f
    FoundSplashStyle.VICTORY_RIPPLE -> 0.44f
    FoundSplashStyle.STAR_BURST -> 0.50f
    FoundSplashStyle.CHAMPAGNE_POP -> 0.54f
    FoundSplashStyle.HALLOWEEN -> 0.52f
    FoundSplashStyle.CHRISTMAS -> 0.52f
}

private fun smoothStep(t: Float): Float {
    val c = t.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

private fun drawFivePointStar(
    center: Offset,
    outerR: Float,
    innerR: Float,
    rotationDeg: Float = -90f
) = Path().apply {
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

private fun foundCardScale(style: FoundSplashStyle, progress: Float): Float {
    val reveal = when (style) {
        FoundSplashStyle.NONE -> ((progress - 0.05f) / 0.35f).coerceIn(0f, 1f)
        FoundSplashStyle.CLASSIC -> ((progress - 0.05f) / 0.35f).coerceIn(0f, 1f)
        FoundSplashStyle.FIREWORKS -> ((progress - 0.36f) / 0.34f).coerceIn(0f, 1f)
        FoundSplashStyle.CONFETTI -> ((progress - 0.36f) / 0.34f).coerceIn(0f, 1f)
        FoundSplashStyle.VICTORY_RIPPLE -> ((progress - 0.26f) / 0.36f).coerceIn(0f, 1f)
        FoundSplashStyle.STAR_BURST -> ((progress - 0.20f) / 0.34f).coerceIn(0f, 1f)
        FoundSplashStyle.CHAMPAGNE_POP -> ((progress - 0.36f) / 0.34f).coerceIn(0f, 1f)
        FoundSplashStyle.HALLOWEEN -> ((progress - 0.36f) / 0.34f).coerceIn(0f, 1f)
        FoundSplashStyle.CHRISTMAS -> ((progress - 0.36f) / 0.34f).coerceIn(0f, 1f)
    }
    val eased = reveal * reveal * (3f - 2f * reveal)
    val pop = sin(reveal * PI.toFloat()) * 0.06f
    return 0.82f + eased * 0.18f + pop
}

private fun foundCardAlpha(style: FoundSplashStyle, progress: Float): Float {
    val start = when (style) {
        FoundSplashStyle.NONE -> 0.04f
        FoundSplashStyle.CLASSIC -> 0.04f
        FoundSplashStyle.FIREWORKS -> 0.34f
        FoundSplashStyle.CONFETTI -> 0.34f
        FoundSplashStyle.VICTORY_RIPPLE -> 0.24f
        FoundSplashStyle.STAR_BURST -> 0.18f
        FoundSplashStyle.CHAMPAGNE_POP -> 0.34f
        FoundSplashStyle.HALLOWEEN -> 0.34f
        FoundSplashStyle.CHRISTMAS -> 0.34f
    }
    return ((progress - start) / 0.28f).coerceIn(0f, 1f)
}

@Composable
fun FoundSplashStyleEffect(
    style: FoundSplashStyle,
    progress: () -> Float,
    modifier: Modifier = Modifier
) {
    when (style) {
        FoundSplashStyle.NONE -> Unit
        FoundSplashStyle.CLASSIC -> Unit
        FoundSplashStyle.FIREWORKS -> FireworksFoundEffect(progress, modifier)
        FoundSplashStyle.CONFETTI -> ConfettiFoundEffect(progress, modifier)
        FoundSplashStyle.VICTORY_RIPPLE -> VictoryRippleFoundEffect(progress, modifier)
        FoundSplashStyle.STAR_BURST -> StarBurstFoundEffect(progress, modifier)
        FoundSplashStyle.CHAMPAGNE_POP -> ChampagnePopFoundEffect(progress, modifier)
        FoundSplashStyle.HALLOWEEN -> HalloweenFoundEffect(progress, modifier)
        FoundSplashStyle.CHRISTMAS -> ChristmasFoundEffect(progress, modifier)
    }
}

@Composable
fun FoundSplashCard(
    progress: () -> Float,
    style: FoundSplashStyle,
    compact: Boolean = false,
    title: String = "Found it!",
    subtitle: String = "Tracking cleared — nice work getting back.",
    modifier: Modifier = Modifier
) {
    val iconSize = if (compact) 28.dp else 72.dp
    val checkSize = if (compact) 16.dp else 42.dp
    val hPad = if (compact) 10.dp else 28.dp
    val vPad = if (compact) 8.dp else 30.dp
    val titleSize = if (compact) 11.sp else MaterialTheme.typography.headlineMedium.fontSize
    val subtitleSize = if (compact) 8.sp else MaterialTheme.typography.bodyMedium.fontSize

    // Classic/None have no animation running behind them, so the solid card is still the right
    // call there. Every other style exists specifically to show off a full-screen animation —
    // a big opaque panel sitting in the middle of it defeats the point. Those get just the icon
    // and text floating free with a drop shadow standing in for the card's contrast, so the
    // portal/burst/etc. underneath actually shows.
    val hasBackdrop = style == FoundSplashStyle.NONE || style == FoundSplashStyle.CLASSIC
    val textShadow = if (hasBackdrop) null else Shadow(color = Color.Black.copy(alpha = 0.85f), blurRadius = 18f)
    val textStyle = LocalTextStyle.current.copy(shadow = textShadow)

    // scale/alpha used to be plain composable-body vals fed into .scale()/.alpha() (plain-Float
    // modifiers) — every frame of the reveal tween recomposed this whole card (icon circle, both
    // Text lines) rather than just redrawing it. graphicsLayer{} reads progress() (and derives
    // scale/alpha from it) only at draw time instead — same animation, same visuals.
    Column(
        modifier = modifier
            .graphicsLayer {
                val p = progress()
                val s = foundCardScale(style, p)
                scaleX = s
                scaleY = s
                alpha = foundCardAlpha(style, p)
            }
            .then(
                if (hasBackdrop) {
                    Modifier
                        .background(SpotVaultColors.Surface.copy(alpha = 0.55f), RoundedCornerShape(if (compact) 12.dp else 28.dp))
                        .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.45f), RoundedCornerShape(if (compact) 12.dp else 28.dp))
                } else {
                    Modifier
                }
            )
            .padding(horizontal = hPad, vertical = vPad),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .background(SpotVaultColors.Teal.copy(alpha = if (hasBackdrop) 0.18f else 0.32f), CircleShape)
                .border(2.dp, SpotVaultColors.Teal.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SpotVaultColors.Teal,
                modifier = Modifier.size(checkSize)
            )
        }
        if (!compact) Spacer(modifier = Modifier.height(16.dp))
        Text(
            title,
            style = textStyle,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            color = SpotVaultColors.OnSurface,
            maxLines = if (compact) 2 else 2,
            textAlign = TextAlign.Center
        )
        if (!compact) Spacer(modifier = Modifier.height(6.dp))
        Text(
            subtitle,
            style = textStyle,
            fontSize = subtitleSize,
            color = if (hasBackdrop) SpotVaultColors.Muted else SpotVaultColors.OnSurface.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            maxLines = if (compact) 2 else 3,
            lineHeight = if (compact) 10.sp else MaterialTheme.typography.bodyMedium.lineHeight
        )
    }
}

/** Full-screen Found celebration — used in-app and from widget relay. */
@Composable
fun FoundSplashOverlay(
    style: FoundSplashStyle,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Found it!",
    subtitle: String = "Tracking cleared — nice work getting back.",
    reducedMotion: Boolean = ThemeState.reduceAnimations
) {
    var visible by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    // Guards against both the timed path and the tap-to-dismiss path calling onFinished —
    // a tap can land in the same frame the timed sequence was about to finish on its own.
    fun finishOnce() {
        if (!finished) {
            finished = true
            onFinished()
        }
    }

    LaunchedEffect(style, reducedMotion) {
        // NONE means skip the celebration entirely — no scrim, no delay, straight to onFinished
        // — rather than playing the full overlay lifecycle with an empty effect underneath it.
        if (style == FoundSplashStyle.NONE) {
            finishOnce()
            return@LaunchedEffect
        }
        visible = true
        val totalMs = if (reducedMotion) 900L else style.effectDurationMillis()
        val animateMs = (totalMs * 0.82f).toInt().coerceAtLeast(400)
        val fadeMs = (totalMs - animateMs).coerceAtLeast(120L)

        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = animateMs,
                easing = FastOutSlowInEasing
            )
        )
        if (dismissed) return@LaunchedEffect
        delay((totalMs * 0.08f).toLong())
        if (dismissed) return@LaunchedEffect
        visible = false
        delay(fadeMs)
        finishOnce()
    }

    // A tap short-circuits straight to the same fade-out/onFinished tail the timed path uses,
    // instead of waiting out however many seconds are left on the celebration.
    LaunchedEffect(dismissed) {
        if (dismissed) {
            visible = false
            delay(if (reducedMotion) 120L else 280L)
            finishOnce()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(if (reducedMotion) 120 else 220)),
        exit = fadeOut(tween(if (reducedMotion) 120 else 280)),
        modifier = modifier
            .fillMaxSize()
            .zIndex(40f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpotVaultColors.Deep.copy(alpha = 0.72f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { dismissed = true }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!reducedMotion) {
                // Lambdas, not progress.value directly — reading .value right here would
                // recompose this whole overlay (background, both the effect Canvas dispatch and
                // the card) on every frame of the reveal animation. Passing a lambda instead lets
                // FoundSplashStyleEffect/FoundSplashCard each defer the actual read to their own
                // draw phase (see their own comments), so only real redraws happen per frame.
                FoundSplashStyleEffect(
                    style = style,
                    progress = { progress.value },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FoundSplashCard(
                    progress = { if (reducedMotion) 0.75f else progress.value },
                    style = style,
                    title = title,
                    subtitle = subtitle,
                    modifier = Modifier.padding(horizontal = 28.dp)
                )
                // Tapping anywhere on the scrim already dismisses this (see the clickable above)
                // — the celebration itself never had any visible sign that was possible, so it
                // just read as "wait it out," which is exactly what this makes obvious instead.
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(400, delayMillis = 300)),
                    exit = fadeOut(tween(120))
                ) {
                    Text(
                        "Tap anywhere to dismiss",
                        color = SpotVaultColors.Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FireworksFoundEffect(progress: () -> Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    val palette = listOf(
        Color(0xFFFFD54F),
        colors.Teal,
        Color(0xFFFF6B9D),
        colors.PrimaryBright,
        Color(0xFF81D4FA),
        Color(0xFFFFAB40),
        Color.White,
        Color(0xFFCE93D8)
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        val progress = progress()
        val w = size.width
        val h = size.height
        val minDim = size.minDimension
        val center = Offset(w / 2f, h / 2f)
        val nightPhase = smoothStep((progress / 0.16f).coerceIn(0f, 1f))
        val rocketPhase = smoothStep(((progress - 0.08f) / 0.28f).coerceIn(0f, 1f))
        val burstPhase = smoothStep(((progress - 0.24f) / 0.42f).coerceIn(0f, 1f))
        val willowPhase = smoothStep(((progress - 0.38f) / 0.38f).coerceIn(0f, 1f))
        val stardustPhase = smoothStep(((progress - 0.58f) / 0.38f).coerceIn(0f, 1f))
        val skyFlicker = 0.82f + 0.18f * sin(progress * PI.toFloat() * 5.5f)

        // Act I — nightfall sky
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF060812).copy(alpha = 0.75f * nightPhase),
                    Color(0xFF0A0E1A).copy(alpha = 0.55f * nightPhase),
                    Color(0xFF141028).copy(alpha = 0.28f * nightPhase),
                    Color.Transparent
                ),
                startY = 0f,
                endY = h * 0.82f
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF1A1028).copy(alpha = 0.12f * nightPhase),
                    Color(0xFF0A0610).copy(alpha = 0.20f * nightPhase)
                ),
                startY = h * 0.72f,
                endY = h
            )
        )
        for (s in 0 until splashLoopCount(28)) {
            val sx = (s * 73.7f + 11f) % w
            val sy = (s * 41.3f + 7f) % (h * 0.42f)
            val twinkle = sin(progress * PI.toFloat() * (3.5f + s * 0.15f) + s * 1.7f) * 0.5f + 0.5f
            drawCircle(
                color = Color.White.copy(alpha = nightPhase * (0.12f + twinkle * 0.38f)),
                radius = 1f + (s % 3) * 0.6f,
                center = Offset(sx, sy)
            )
        }
        if (nightPhase > 0.3f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        colors.Teal.copy(alpha = nightPhase * 0.06f * skyFlicker),
                        colors.PrimaryBright.copy(alpha = nightPhase * 0.05f * skyFlicker),
                        Color.Transparent
                    ),
                    startX = 0f,
                    endX = w
                ),
                topLeft = Offset(0f, h * 0.04f),
                size = Size(w, minDim * 0.08f)
            )
        }

        // Act II — ascending rockets (launch from bottom margins toward burst sites)
        val rocketFlights = listOf(
            Triple(0.14f, 0.14f, 0.04f),
            Triple(0.86f, 0.12f, 0.08f),
            Triple(0.22f, 0.20f, 0.12f),
            Triple(0.78f, 0.18f, 0.16f),
            Triple(0.10f, 0.40f, 0.20f),
            Triple(0.90f, 0.38f, 0.24f),
            Triple(0.50f, 0.10f, 0.10f),
            Triple(0.18f, 0.62f, 0.28f),
            Triple(0.82f, 0.58f, 0.32f)
        )
        if (rocketPhase > 0f) {
            rocketFlights.forEachIndexed { index, (targetX, targetY, launchAt) ->
                val local = smoothStep(((rocketPhase - launchAt) / 0.22f).coerceIn(0f, 1f))
                if (local <= 0f || local >= 0.98f) return@forEachIndexed
                drawFoundFireworkRocket(
                    start = Offset(w * targetX, h * 0.96f),
                    end = Offset(w * targetX, h * targetY),
                    progress = local,
                    minDim = minDim,
                    tint = palette[index % palette.size],
                    flicker = skyFlicker
                )
            }
        }

        // Act III — margin sky bursts (kept off center copy)
        val burstSites = listOf(
            Offset(w * 0.14f, h * 0.14f) to 0.04f,
            Offset(w * 0.86f, h * 0.12f) to 0.10f,
            Offset(w * 0.22f, h * 0.20f) to 0.16f,
            Offset(w * 0.78f, h * 0.18f) to 0.22f,
            Offset(w * 0.10f, h * 0.40f) to 0.28f,
            Offset(w * 0.90f, h * 0.38f) to 0.34f,
            Offset(w * 0.50f, h * 0.10f) to 0.14f,
            Offset(w * 0.18f, h * 0.62f) to 0.40f,
            Offset(w * 0.82f, h * 0.58f) to 0.46f
        )
        burstSites.forEachIndexed { index, (site, delay) ->
            val local = smoothStep(((burstPhase - delay) / 0.38f).coerceIn(0f, 1f))
            if (local <= 0f) return@forEachIndexed
            drawFoundFireworkBurst(
                center = site,
                phase = local,
                minDim = minDim,
                tint = palette[index % palette.size],
                index = index,
                style = index % 3,
                flicker = skyFlicker
            )
        }

        // Act IV — willow embers & crackle pops
        if (willowPhase > 0f) {
            burstSites.forEachIndexed { index, (site, delay) ->
                val local = smoothStep(((willowPhase - delay * 0.6f) / 0.72f).coerceIn(0f, 1f))
                if (local <= 0f) return@forEachIndexed
                drawFoundFireworkWillow(
                    origin = site,
                    phase = local,
                    minDim = minDim,
                    tint = palette[(index + 2) % palette.size],
                    index = index
                )
                if (index % 2 == 0 && local > 0.35f) {
                    val crackle = smoothStep(((local - 0.35f) / 0.55f).coerceIn(0f, 1f))
                    drawFoundFireworkCrackle(
                        center = site + Offset(minDim * 0.04f * (index % 3 - 1), minDim * 0.03f),
                        phase = crackle,
                        minDim = minDim * 0.55f,
                        tint = palette[(index + 4) % palette.size],
                        seed = index
                    )
                }
            }
        }

        // Act V — stardust afterglow
        if (stardustPhase > 0f) {
            for (g in 0 until splashLoopCount(32)) {
                val gx = (g * 97f + stardustPhase * 45f) % w
                val gy = h * (0.08f + (g * 0.033f) % 0.62f) + stardustPhase * 14f
                val ga = stardustPhase * (1f - g * 0.012f).coerceAtLeast(0.25f) * (0.28f + (g % 5) * 0.10f)
                drawCircle(
                    color = Color.White.copy(alpha = ga * skyFlicker),
                    radius = 1.2f + (g % 3) * 0.7f,
                    center = Offset(gx, gy)
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.PrimaryBright.copy(alpha = stardustPhase * 0.06f * skyFlicker),
                        colors.Teal.copy(alpha = stardustPhase * 0.04f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = minDim * 0.40f
                ),
                radius = minDim * 0.40f,
                center = center
            )
            for (wisp in 0 until 6) {
                val wx = w * (0.15f + wisp * 0.14f)
                val wy = h * (0.72f + sin(progress * PI.toFloat() * 2f + wisp) * 0.02f)
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3A3A48).copy(alpha = stardustPhase * 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(wx, wy),
                        radius = minDim * 0.06f
                    ),
                    topLeft = Offset(wx - minDim * 0.08f, wy - minDim * 0.025f),
                    size = Size(minDim * 0.16f, minDim * 0.05f)
                )
            }
        }
    }
}

private fun DrawScope.drawFoundFireworkRocket(
    start: Offset,
    end: Offset,
    progress: Float,
    minDim: Float,
    tint: Color,
    flicker: Float
) {
    val pos = start + (end - start) * progress
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.90f * flicker),
                tint.copy(alpha = 0.55f),
                Color.Transparent
            ),
            start = pos,
            end = pos + Offset(0f, minDim * 0.07f)
        ),
        start = pos,
        end = pos + Offset(0f, minDim * 0.07f),
        strokeWidth = 2.8f,
        cap = StrokeCap.Round
    )
    for (trail in 0 until 4) {
        val t = progress - trail * 0.06f
        if (t <= 0f) continue
        val trailPos = start + (end - start) * t
        drawCircle(
            color = tint.copy(alpha = (1f - trail * 0.2f) * 0.45f),
            radius = 2f - trail * 0.3f,
            center = trailPos
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, tint.copy(alpha = 0.85f), Color.Transparent),
            center = pos,
            radius = 5f
        ),
        radius = 5f,
        center = pos
    )
}

private fun DrawScope.drawFoundFireworkBurst(
    center: Offset,
    phase: Float,
    minDim: Float,
    tint: Color,
    index: Int,
    style: Int,
    flicker: Float
) {
    val fade = (1f - phase * 0.88f).coerceIn(0f, 1f)
    val radius = minDim * (0.22f + (index % 3) * 0.04f) * smoothStep(phase)

    val flash = ((phase - 0.02f) / 0.12f).coerceIn(0f, 1f)
    if (flash > 0f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = (1f - flash) * 0.60f),
                    tint.copy(alpha = (1f - flash) * 0.35f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 0.55f
            ),
            radius = radius * 0.55f,
            center = center
        )
    }

    for (ring in 0 until 3) {
        val ringDelay = ring * 0.12f
        val ringLocal = smoothStep(((phase - ringDelay) / 0.72f).coerceIn(0f, 1f))
        if (ringLocal <= 0f) continue
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    tint.copy(alpha = fade * 0.65f * flicker),
                    Color.White.copy(alpha = fade * 0.55f),
                    tint.copy(alpha = fade * 0.45f),
                    tint.copy(alpha = fade * 0.65f * flicker)
                ),
                center = center
            ),
            radius = radius * (0.85f + ring * 0.12f) * ringLocal,
            center = center,
            style = Stroke(width = (3.5f - ring * 0.8f).coerceAtLeast(1.2f))
        )
    }

    val streakCount = when (style) {
        0 -> 40
        1 -> 24
        else -> 32
    }
    for (ray in 0 until streakCount) {
        val angle = (ray * (360f / streakCount) + index * 13f + phase * 25f) * PI.toFloat() / 180f
        val len = radius * when (style) {
            0 -> 0.70f + (ray % 3) * 0.10f
            1 -> 0.55f + (ray % 2) * 0.18f
            else -> 0.62f + sin(ray * 1.7f) * 0.08f
        }
        val tail = center + Offset(cos(angle), sin(angle)) * len
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = fade * 0.90f),
                    tint.copy(alpha = fade * 0.60f),
                    Color.Transparent
                ),
                start = center,
                end = tail
            ),
            start = center,
            end = tail,
            strokeWidth = if (ray % 3 == 0) 3f else 1.6f,
            cap = StrokeCap.Round
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = fade * 0.95f * flicker),
                tint.copy(alpha = fade * 0.75f),
                Color.Transparent
            ),
            center = center,
            radius = radius * 0.28f
        ),
        radius = radius * 0.28f,
        center = center
    )
}

private fun DrawScope.drawFoundFireworkWillow(
    origin: Offset,
    phase: Float,
    minDim: Float,
    tint: Color,
    index: Int
) {
    for (strand in 0 until splashLoopCount(10)) {
        val baseAngle = (strand * (360f / 10f) + index * 20f) * PI.toFloat() / 180f
        val path = Path()
        var started = false
        for (step in 0..14) {
            val t = step / 14f * phase
            val dist = minDim * 0.18f * t
            val droop = t * t * minDim * 0.14f
            val point = origin + Offset(cos(baseAngle) * dist, sin(baseAngle) * dist * 0.35f + droop)
            if (!started) {
                path.moveTo(point.x, point.y)
                started = true
            } else {
                path.lineTo(point.x, point.y)
            }
        }
        if (started) {
            drawPath(
                path,
                brush = Brush.linearGradient(
                    colors = listOf(
                        tint.copy(alpha = (1f - phase * 0.5f) * 0.65f),
                        tint.copy(alpha = (1f - phase) * 0.25f)
                    ),
                    start = origin,
                    end = origin + Offset(0f, minDim * 0.18f)
                ),
                style = Stroke(width = 1.8f, cap = StrokeCap.Round)
            )
        }
        val tipT = phase
        val tipDist = minDim * 0.18f * tipT
        val tipDroop = tipT * tipT * minDim * 0.14f
        val tip = origin + Offset(cos(baseAngle) * tipDist, sin(baseAngle) * tipDist * 0.35f + tipDroop)
        drawCircle(
            color = Color.White.copy(alpha = (1f - phase * 0.6f) * 0.70f),
            radius = 2.2f,
            center = tip
        )
    }
}

private fun DrawScope.drawFoundFireworkCrackle(
    center: Offset,
    phase: Float,
    minDim: Float,
    tint: Color,
    seed: Int
) {
    for (spark in 0 until splashLoopCount(12)) {
        val angle = (spark * 30f + seed * 17f) * PI.toFloat() / 180f
        val dist = minDim * 0.12f * phase
        val pos = center + Offset(cos(angle), sin(angle)) * dist
        drawCircle(
            color = tint.copy(alpha = (1f - phase) * 0.85f),
            radius = 2f + (spark % 2),
            center = pos
        )
    }
}

@Composable
private fun ConfettiFoundEffect(progress: () -> Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    val confettiPalette = listOf(
        colors.Teal,
        colors.PrimaryBright,
        Color(0xFFFFD54F),
        Color(0xFFFF6B9D),
        Color(0xFF81C784),
        Color(0xFFCE93D8),
        Color.White,
        Color(0xFFFFAB40),
        Color(0xFF4DD0E1)
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        val progress = progress()
        val w = size.width
        val minDim = size.minDimension
        val haloCenter = Offset(w / 2f, size.height / 2f - minDim * 0.04f)
        val haloR = minDim * 0.34f
        val haloScale = haloR / minDim
        val spotPhase = smoothStep((progress / 0.14f).coerceIn(0f, 1f))
        val cannonPhase = smoothStep(((progress - 0.06f) / 0.22f).coerceIn(0f, 1f))
        val burstPhase = smoothStep(((progress - 0.22f) / 0.32f).coerceIn(0f, 1f))
        val fountainPhase = smoothStep(((progress - 0.18f) / 0.58f).coerceIn(0f, 1f))
        val ringPhase = smoothStep(((progress - 0.38f) / 0.48f).coerceIn(0f, 1f))
        val partyFlicker = 0.78f + 0.22f * sin(progress * PI.toFloat() * 7f)

        // Act I — localized golden halo spotlight on the card
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF8E1).copy(alpha = 0.22f * spotPhase),
                    Color(0xFFFFD54F).copy(alpha = 0.12f * spotPhase),
                    colors.PrimaryBright.copy(alpha = 0.05f * spotPhase),
                    Color.Transparent
                ),
                center = haloCenter,
                radius = haloR * 1.08f
            ),
            radius = haloR * 1.08f,
            center = haloCenter
        )
        for (light in 0 until 3) {
            val sweep = sin(progress * PI.toFloat() * (2.2f + light * 0.4f) + light * 1.8f) * 0.5f + 0.5f
            val angle = (-110f + light * 110f) * PI.toFloat() / 180f
            val lx = haloCenter.x + cos(angle) * haloR * 0.92f
            val ly = haloCenter.y + sin(angle) * haloR * 0.55f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        confettiPalette[light + 2].copy(alpha = sweep * 0.14f * spotPhase),
                        Color.Transparent
                    ),
                    center = Offset(lx, ly),
                    radius = haloR * 0.22f
                ),
                radius = haloR * 0.22f,
                center = Offset(lx, ly)
            )
        }

        // Act II — twin frame cannons at the halo base (not screen corners)
        if (cannonPhase > 0f) {
            val cannonY = haloCenter.y + haloR * 0.72f
            listOf(-1f, 1f).forEach { side ->
                val origin = Offset(haloCenter.x + side * haloR * 0.58f, cannonY)
                drawFoundConfettiCannon(
                    origin = origin,
                    side = side,
                    phase = cannonPhase,
                    minDim = minDim * 0.55f,
                    accent = colors.PrimaryBright,
                    flicker = partyFlicker
                )
                val volley = smoothStep(((cannonPhase - 0.10f) / 0.75f).coerceIn(0f, 1f))
                if (volley <= 0f) return@forEach
                for (piece in 0 until splashLoopCount(10)) {
                    val seed = piece + if (side < 0) 0 else 30
                    val angle = (-118f + piece * 11f * side) * PI.toFloat() / 180f
                    val dist = haloR * volley * (0.55f + (piece % 3) * 0.12f)
                    val pos = origin + Offset(cos(angle) * dist, sin(angle) * dist)
                    if ((pos - haloCenter).getDistance() > haloR * 1.05f) continue
                    drawFoundConfettiPiece(
                        pos = pos,
                        rotation = seed * 37f + volley * 520f,
                        shape = piece % 5,
                        tint = confettiPalette[(piece + 1) % confettiPalette.size],
                        alpha = (1f - volley * 0.40f).coerceIn(0.35f, 1f) * partyFlicker,
                        scale = 0.85f + (piece % 3) * 0.12f
                    )
                }
            }
        }

        // Act III — ring bursts around the halo perimeter (top + sides)
        if (burstPhase > 0f) {
            val burstAngles = listOf(-90f, -148f, -32f, -118f, -62f)
            burstAngles.forEachIndexed { index, deg ->
                val delay = index * 0.06f
                val local = smoothStep(((burstPhase - delay) / 0.68f).coerceIn(0f, 1f))
                if (local <= 0f) return@forEachIndexed
                val rad = deg * PI.toFloat() / 180f
                val site = haloCenter + Offset(cos(rad) * haloR * 0.88f, sin(rad) * haloR * 0.72f)
                drawFoundConfettiBurst(
                    center = site,
                    phase = local,
                    minDim = minDim * haloScale * (0.72f + (index % 2) * 0.08f),
                    palette = confettiPalette,
                    index = index,
                    flicker = partyFlicker
                )
            }
            // Short celebratory streamers linking burst points
            listOf(-148f to -32f, -118f to -62f).forEachIndexed { idx, (fromDeg, toDeg) ->
                val local = smoothStep(((burstPhase - 0.08f - idx * 0.05f) / 0.62f).coerceIn(0f, 1f))
                if (local <= 0f) return@forEachIndexed
                val fromRad = fromDeg * PI.toFloat() / 180f
                val toRad = toDeg * PI.toFloat() / 180f
                drawFoundConfettiStreamerArc(
                    start = haloCenter + Offset(cos(fromRad) * haloR * 0.75f, sin(fromRad) * haloR * 0.60f),
                    apex = haloCenter + Offset(0f, -haloR * 0.95f),
                    end = haloCenter + Offset(cos(toRad) * haloR * 0.75f, sin(toRad) * haloR * 0.60f),
                    phase = local,
                    tint = confettiPalette[idx + 4],
                    side = if (idx == 0) -1f else 1f
                )
            }
        }

        // Act IV — confetti fountain arcs within the celebration dome
        if (fountainPhase > 0f) {
            val launchPoints = listOf(
                Offset(haloCenter.x - haloR * 0.45f, haloCenter.y + haloR * 0.65f),
                Offset(haloCenter.x + haloR * 0.45f, haloCenter.y + haloR * 0.65f),
                Offset(haloCenter.x, haloCenter.y + haloR * 0.78f)
            )
            for (layer in 0 until 2) {
                for (i in 0 until splashLoopCount(18)) {
                    val seed = i + layer * 40
                    val launch = launchPoints[seed % launchPoints.size]
                    val delay = (i % 7) * 0.028f + layer * 0.06f
                    val local = ((fountainPhase - delay) / 0.82f).coerceIn(0f, 1f)
                    if (local <= 0f) continue
                    val aim = (-155f + (seed % 11) * 14f) * PI.toFloat() / 180f
                    val speed = haloR * (0.72f + (seed % 4) * 0.10f)
                    val t = local
                    val pos = launch + Offset(
                        cos(aim) * speed * t,
                        sin(aim) * speed * t + t * t * haloR * 0.55f
                    )
                    if ((pos - haloCenter).getDistance() > haloR * 1.02f) continue
                    drawFoundConfettiPiece(
                        pos = pos,
                        rotation = seed * 3.1f + local * (480f + layer * 90f),
                        shape = (seed + layer) % 5,
                        tint = confettiPalette[(seed + layer) % confettiPalette.size],
                        alpha = (1f - local * 0.42f).coerceIn(0.25f, 1f) * partyFlicker,
                        scale = 0.75f + layer * 0.12f + (seed % 3) * 0.08f
                    )
                }
            }
        }

        // Act V — spinning shimmer ring + halo glitter
        if (ringPhase > 0f) {
            rotate(progress * 42f, pivot = haloCenter) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFFFD54F).copy(alpha = ringPhase * 0.55f * partyFlicker),
                            Color.White.copy(alpha = ringPhase * 0.70f),
                            colors.Teal.copy(alpha = ringPhase * 0.45f),
                            Color(0xFFFF6B9D).copy(alpha = ringPhase * 0.50f),
                            Color(0xFFFFD54F).copy(alpha = ringPhase * 0.55f * partyFlicker)
                        ),
                        center = haloCenter
                    ),
                    radius = haloR * (0.82f + sin(progress * PI.toFloat() * 2f) * 0.04f),
                    center = haloCenter,
                    style = Stroke(width = 3.5f)
                )
            }
            for (g in 0 until splashLoopCount(16)) {
                val angle = (g * 22.5f + progress * 65f) * PI.toFloat() / 180f
                val dist = haloR * (0.35f + (g % 4) * 0.14f)
                val gx = haloCenter.x + cos(angle) * dist
                val gy = haloCenter.y + sin(angle) * dist * 0.85f
                val twinkle = sin(progress * PI.toFloat() * (6f + g * 0.25f) + g) * 0.5f + 0.5f
                drawCircle(
                    color = Color.White.copy(alpha = ringPhase * twinkle * 0.55f),
                    radius = 1.4f + (g % 3) * 0.6f,
                    center = Offset(gx, gy)
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD54F).copy(alpha = ringPhase * 0.12f * partyFlicker),
                        Color.Transparent
                    ),
                    center = haloCenter,
                    radius = haloR * 0.55f
                ),
                radius = haloR * 0.55f,
                center = haloCenter
            )
        }
    }
}

private fun DrawScope.drawFoundConfettiCannon(
    origin: Offset,
    side: Float,
    phase: Float,
    minDim: Float,
    accent: Color,
    flicker: Float
) {
    val barrelLen = minDim * 0.10f * phase
    val angle = (-55f * side) * PI.toFloat() / 180f
    val muzzle = origin + Offset(cos(angle) * barrelLen, sin(angle) * barrelLen)
    drawLine(
        color = Color(0xFF3A3A42).copy(alpha = 0.85f * phase),
        start = origin,
        end = muzzle,
        strokeWidth = minDim * 0.035f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = Color(0xFF5A5A62).copy(alpha = 0.75f * phase),
        radius = minDim * 0.028f,
        center = origin
    )
    if (phase > 0.15f) {
        val flash = ((phase - 0.15f) / 0.25f).coerceIn(0f, 1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = (1f - flash) * 0.70f * flicker),
                    accent.copy(alpha = (1f - flash) * 0.45f),
                    Color.Transparent
                ),
                center = muzzle,
                radius = minDim * 0.08f
            ),
            radius = minDim * 0.08f,
            center = muzzle
        )
    }
    if (phase > 0.08f) {
        val smoke = smoothStep(((phase - 0.08f) / 0.55f).coerceIn(0f, 1f))
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = smoke * 0.18f),
                    Color.Transparent
                ),
                center = origin + Offset(0f, -minDim * 0.02f),
                radius = minDim * 0.06f
            ),
            topLeft = Offset(origin.x - minDim * 0.08f, origin.y - minDim * 0.05f),
            size = Size(minDim * 0.16f, minDim * 0.05f)
        )
    }
}

private fun DrawScope.drawFoundConfettiStreamerArc(
    start: Offset,
    apex: Offset,
    end: Offset,
    phase: Float,
    tint: Color,
    side: Float
) {
    val path = Path()
    var started = false
    for (step in 0..28) {
        val t = step / 28f * phase
        val u = 1f - t
        val point = start * (u * u) + apex * (2f * u * t) + end * (t * t)
        if (!started) {
            path.moveTo(point.x, point.y)
            started = true
        } else {
            path.lineTo(point.x, point.y)
        }
    }
    if (!started) return
    drawPath(
        path,
        color = tint.copy(alpha = phase * 0.22f),
        style = Stroke(width = 8f, cap = StrokeCap.Round)
    )
    drawPath(
        path,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = phase * 0.75f),
                tint.copy(alpha = phase * 0.85f),
                tint.copy(alpha = phase * 0.45f)
            ),
            start = start,
            end = end
        ),
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )
    if (phase > 0.55f) {
        val tip = start * 0.2025f + apex * 0.495f + end * 0.3025f
        drawCircle(
            color = Color.White.copy(alpha = (phase - 0.55f) / 0.45f * 0.65f),
            radius = 4f,
            center = tip
        )
    }
}

private fun DrawScope.drawFoundConfettiBurst(
    center: Offset,
    phase: Float,
    minDim: Float,
    palette: List<Color>,
    index: Int,
    flicker: Float
) {
    val radius = minDim * 0.28f * smoothStep(phase)
    val fade = (1f - phase * 0.75f).coerceIn(0f, 1f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = fade * 0.55f * flicker),
                palette[index % palette.size].copy(alpha = fade * 0.35f),
                Color.Transparent
            ),
            center = center,
            radius = radius * 0.45f
        ),
        radius = radius * 0.45f,
        center = center
    )
    drawCircle(
        color = palette[(index + 2) % palette.size].copy(alpha = fade * 0.55f),
        radius = radius,
        center = center,
        style = Stroke(width = (3.5f - phase * 2f).coerceAtLeast(1.2f))
    )
    for (p in 0 until splashLoopCount(22)) {
        val angle = (p * 16.36f + index * 22f + phase * 80f) * PI.toFloat() / 180f
        val dist = radius * (0.35f + (p % 4) * 0.14f)
        val pos = center + Offset(cos(angle), sin(angle)) * dist
        drawFoundConfettiPiece(
            pos = pos,
            rotation = p * 44f + phase * 260f,
            shape = (p + index) % 5,
            tint = palette[(p + index) % palette.size],
            alpha = fade * flicker,
            scale = 0.85f + (p % 3) * 0.12f
        )
    }
}

private fun DrawScope.drawFoundConfettiPiece(
    pos: Offset,
    rotation: Float,
    shape: Int,
    tint: Color,
    alpha: Float,
    scale: Float
) {
    rotate(rotation, pivot = pos) {
        when (shape % 5) {
            0 -> drawRect(
                color = tint.copy(alpha = alpha),
                topLeft = Offset(pos.x - 4f * scale, pos.y - 7f * scale),
                size = Size(8f * scale, 14f * scale)
            )
            1 -> drawCircle(
                color = tint.copy(alpha = alpha),
                radius = 5f * scale,
                center = pos
            )
            2 -> drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(tint.copy(alpha = alpha), tint.copy(alpha = alpha * 0.25f))
                ),
                start = pos,
                end = pos + Offset(0f, 26f * scale),
                strokeWidth = 3f * scale,
                cap = StrokeCap.Round
            )
            3 -> drawPath(
                drawFivePointStar(pos, (6f + scale * 2f), 2.8f * scale),
                color = tint.copy(alpha = alpha)
            )
            else -> drawPath(
                Path().apply {
                    moveTo(pos.x, pos.y - 8f * scale)
                    lineTo(pos.x + 7f * scale, pos.y + 6f * scale)
                    lineTo(pos.x - 7f * scale, pos.y + 6f * scale)
                    close()
                },
                color = tint.copy(alpha = alpha)
            )
        }
    }
}

@Composable
private fun VictoryRippleFoundEffect(progress: () -> Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    Canvas(modifier = modifier.fillMaxSize()) {
        val progress = progress()
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = size.minDimension * 0.58f

        // Ambient victory glow
        val ambient = sin(progress * PI.toFloat() * 2.5f) * 0.15f + 0.2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors.PrimaryBright.copy(alpha = ambient),
                    colors.Teal.copy(alpha = ambient * 0.45f),
                    Color.Transparent
                ),
                center = center,
                radius = maxR * 0.5f
            ),
            radius = maxR * 0.5f,
            center = center
        )

        // Expanding shock rings with gradient strokes
        for (ring in 0 until splashLoopCount(9)) {
            val delay = ring * 0.065f
            val local = ((progress - delay) / 0.72f).coerceIn(0f, 1f)
            if (local <= 0f) continue
            val radius = maxR * smoothStep(local)
            val alpha = (1f - local).coerceIn(0f, 1f) * 0.75f
            val gold = Color(0xFFFFD54F)
            val tint = if (ring % 2 == 0) colors.Teal else gold
            val strokeW = (5f - ring * 0.35f).coerceAtLeast(1.5f)

            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        tint.copy(alpha = alpha),
                        Color.White.copy(alpha = alpha * 0.85f),
                        tint.copy(alpha = alpha * 0.6f),
                        tint.copy(alpha = alpha)
                    ),
                    center = center
                ),
                radius = radius,
                center = center,
                style = Stroke(width = strokeW)
            )

            // Spark particles riding the wave front
            for (dot in 0 until splashLoopCount(8)) {
                val a = (dot * 45f + ring * 12f + progress * 30f) * PI.toFloat() / 180f
                val dotPos = center + Offset(cos(a), sin(a)) * radius
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.9f),
                    radius = 2.5f,
                    center = dotPos
                )
            }
        }

        // Sacred geometry hexagon pulse
        val hexPhase = smoothStep((progress / 0.85f).coerceIn(0f, 1f))
        val hexR = maxR * 0.22f * (0.6f + hexPhase * 0.4f)
        val hexPath = Path().apply {
            for (v in 0 until 6) {
                val a = (v * 60f - 90f) * PI.toFloat() / 180f
                val pt = center + Offset(cos(a) * hexR, sin(a) * hexR)
                if (v == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
            }
            close()
        }
        drawPath(
            path = hexPath,
            color = colors.Teal.copy(alpha = 0.35f * (1f - hexPhase * 0.4f)),
            style = Stroke(width = 2.5f)
        )
        drawPath(
            path = hexPath,
            brush = Brush.linearGradient(
                listOf(
                    Color(0xFFFFD54F).copy(alpha = 0.25f),
                    colors.PrimaryBright.copy(alpha = 0.15f)
                )
            )
        )

        // Cardinal light pillars
        if (progress > 0.15f) {
            val pillar = ((progress - 0.15f) / 0.55f).coerceIn(0f, 1f)
            val pillarAlpha = sin(pillar * PI.toFloat()) * 0.45f
            listOf(0f, 90f, 180f, 270f).forEach { deg ->
                rotate(deg, pivot = center) {
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = pillarAlpha),
                                colors.Teal.copy(alpha = pillarAlpha * 0.6f),
                                Color.Transparent
                            ),
                            startY = center.y - maxR * 0.55f,
                            endY = center.y + maxR * 0.55f
                        ),
                        start = Offset(center.x, center.y - maxR * 0.55f * pillar),
                        end = Offset(center.x, center.y + maxR * 0.55f * pillar),
                        strokeWidth = 14f - deg / 30f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Golden core pulse
        val corePulse = sin(progress * PI.toFloat() * 3f) * 0.2f + 0.35f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = corePulse),
                    Color(0xFFFFD54F).copy(alpha = corePulse * 0.7f),
                    Color.Transparent
                ),
                center = center,
                radius = maxR * 0.14f
            ),
            radius = maxR * 0.14f,
            center = center
        )
    }
}

@Composable
private fun StarBurstFoundEffect(progress: () -> Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    Canvas(modifier = modifier.fillMaxSize()) {
        val progress = progress()
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = size.minDimension * 0.52f
        val burst = smoothStep((progress / 0.75f).coerceIn(0f, 1f))
        val fade = (1f - (progress - 0.45f) / 0.55f).coerceIn(0.25f, 1f)

        // Lens flare cross
        val flareAlpha = sin(progress * PI.toFloat() * 2f) * 0.25f + 0.3f
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = flareAlpha * fade),
                    Color.Transparent
                ),
                startX = center.x - maxR,
                endX = center.x + maxR
            ),
            start = Offset(center.x - maxR * burst, center.y),
            end = Offset(center.x + maxR * burst, center.y),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = flareAlpha * fade * 0.85f),
                    Color.Transparent
                ),
                startY = center.y - maxR,
                endY = center.y + maxR
            ),
            start = Offset(center.x, center.y - maxR * burst),
            end = Offset(center.x, center.y + maxR * burst),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        // Three counter-rotating ray layers
        listOf(
            Triple(48, 1f, 22f),
            Triple(32, -0.65f, 15f),
            Triple(24, 0.4f, 28f)
        ).forEach { (rayCount, spinMult, baseWidth) ->
            val spin = progress * 40f * spinMult
            rotate(spin, pivot = center) {
                for (ray in 0 until rayCount) {
                    val angle = ray * (360f / rayCount)
                    rotate(angle, pivot = center) {
                        val len = maxR * burst * (0.55f + (ray % 4) * 0.12f)
                        val isMajor = ray % 4 == 0
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.95f * fade),
                                    colors.Teal.copy(alpha = 0.55f * fade),
                                    colors.PrimaryBright.copy(alpha = 0.25f * fade),
                                    Color.Transparent
                                ),
                                start = center,
                                end = center + Offset(0f, -len)
                            ),
                            start = center,
                            end = center + Offset(0f, -len),
                            strokeWidth = if (isMajor) baseWidth else baseWidth * 0.45f,
                            cap = StrokeCap.Round
                        )
                        // Comet head on major rays
                        if (isMajor && burst > 0.2f) {
                            val head = center + Offset(0f, -len)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = fade),
                                        colors.Teal.copy(alpha = fade * 0.5f),
                                        Color.Transparent
                                    ),
                                    center = head,
                                    radius = 8f
                                ),
                                radius = 8f,
                                center = head
                            )
                        }
                    }
                }
            }
        }

        // Orbiting stars
        for (star in 0 until splashLoopCount(16)) {
            val orbitA = (star * 22.5f + progress * 55f) * PI.toFloat() / 180f
            val orbitR = maxR * (0.28f + (star % 4) * 0.1f) * burst
            val starCenter = center + Offset(cos(orbitA), sin(orbitA)) * orbitR
            val starR = 5f + (star % 3) * 1.5f
            drawPath(
                path = drawFivePointStar(starCenter, starR, starR * 0.45f, rotationDeg = star * 24f + progress * 80f),
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = fade * 0.95f),
                        Color(0xFFFFD54F).copy(alpha = fade * 0.85f),
                        Color(0xFFFFAB40).copy(alpha = fade * 0.5f)
                    ),
                    center = starCenter,
                    radius = starR
                )
            )
        }

        // Supernova core
        val nova = sin(progress * PI.toFloat() * 4f) * 0.15f + 0.45f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = nova * fade),
                    colors.PrimaryBright.copy(alpha = nova * 0.6f * fade),
                    colors.Teal.copy(alpha = nova * 0.3f * fade),
                    Color.Transparent
                ),
                center = center,
                radius = maxR * 0.18f * (1f + burst * 0.35f)
            ),
            radius = maxR * 0.18f * (1f + burst * 0.35f),
            center = center
        )
    }
}

@Composable
private fun ChampagnePopFoundEffect(progress: () -> Float, modifier: Modifier = Modifier) {
    val colors = LocalSpotVaultColors.current
    val gold = Color(0xFFFFD54F)
    val amber = Color(0xFFFFAB40)
    Canvas(modifier = modifier.fillMaxSize()) {
        val progress = progress()
        val w = size.width
        val minDim = size.minDimension
        val haloCenter = Offset(w / 2f, size.height / 2f - minDim * 0.04f)
        val haloR = minDim * 0.34f
        val bottleBase = Offset(haloCenter.x, haloCenter.y + haloR * 0.82f)
        val bottleNeck = bottleBase - Offset(0f, minDim * 0.14f)
        val glowPhase = smoothStep((progress / 0.14f).coerceIn(0f, 1f))
        val bottlePhase = smoothStep(((progress - 0.05f) / 0.20f).coerceIn(0f, 1f))
        val popPhase = smoothStep(((progress - 0.18f) / 0.20f).coerceIn(0f, 1f))
        val fizzPhase = smoothStep(((progress - 0.30f) / 0.34f).coerceIn(0f, 1f))
        val bubblePhase = smoothStep(((progress - 0.22f) / 0.68f).coerceIn(0f, 1f))
        val toastPhase = smoothStep(((progress - 0.50f) / 0.42f).coerceIn(0f, 1f))
        val fizzFlicker = 0.80f + 0.20f * sin(progress * PI.toFloat() * 6.5f)

        // Act I — localized golden halo around the card
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.14f * glowPhase * fizzFlicker),
                    gold.copy(alpha = 0.16f * glowPhase),
                    amber.copy(alpha = 0.08f * glowPhase),
                    Color.Transparent
                ),
                center = haloCenter,
                radius = haloR * 1.05f
            ),
            radius = haloR * 1.05f,
            center = haloCenter
        )
        for (dust in 0 until splashLoopCount(8)) {
            val angle = (dust * 45f + progress * 35f) * PI.toFloat() / 180f
            val dist = haloR * (0.28f + (dust % 3) * 0.18f)
            val dx = haloCenter.x + cos(angle) * dist
            val dy = haloCenter.y + sin(angle) * dist * 0.75f
            val twinkle = sin(progress * PI.toFloat() * 3.5f + dust) * 0.5f + 0.5f
            drawCircle(
                color = Color.White.copy(alpha = glowPhase * twinkle * 0.38f),
                radius = 1.2f + (dust % 2) * 0.5f,
                center = Offset(dx, dy)
            )
        }

        // Act II — mini bottle tucked below the card within the halo
        if (bottlePhase > 0f) {
            drawFoundChampagneBottle(
                base = bottleBase,
                minDim = minDim * 0.62f,
                phase = bottlePhase,
                gold = gold,
                accent = colors.Teal
            )
        }

        // Act III — cork pop with burst confined to the halo
        if (popPhase > 0f) {
            drawFoundChampagnePopBurst(
                origin = bottleNeck,
                phase = popPhase,
                minDim = minDim * 0.55f,
                gold = gold,
                accent = colors.PrimaryBright,
                flicker = fizzFlicker
            )
            val corkT = smoothStep(((popPhase - 0.06f) / 0.82f).coerceIn(0f, 1f))
            if (corkT > 0f) {
                val corkX = bottleNeck.x - corkT * haloR * 0.22f
                val corkY = bottleNeck.y - corkT * haloR * 0.95f - sin(corkT * PI.toFloat()) * haloR * 0.12f
                val corkPos = Offset(corkX, corkY)
                if ((corkPos - haloCenter).getDistance() <= haloR * 1.05f) {
                    drawFoundChampagneCork(
                        pos = corkPos,
                        rotation = corkT * 480f,
                        scale = 1f - corkT * 0.12f,
                        alpha = (1f - corkT * 0.40f).coerceIn(0.35f, 1f)
                    )
                }
            }
        }

        // Act IV — fizz mist dome from the bottle neck
        if (fizzPhase > 0f) {
            listOf(-1f, 1f).forEach { side ->
                drawFoundChampagneFizzSpray(
                    origin = bottleNeck + Offset(side * minDim * 0.025f, 0f),
                    side = side,
                    phase = fizzPhase,
                    minDim = minDim * 0.48f,
                    h = haloR * 2f,
                    gold = gold,
                    accent = colors.Teal,
                    flicker = fizzFlicker
                )
            }
            for (ring in 0 until 3) {
                val delay = ring * 0.09f
                val local = smoothStep(((fizzPhase - delay) / 0.68f).coerceIn(0f, 1f))
                if (local <= 0f) continue
                drawCircle(
                    color = gold.copy(alpha = (1f - local) * 0.50f * fizzFlicker),
                    radius = haloR * (0.08f + local * (0.22f + ring * 0.05f)),
                    center = bottleNeck,
                    style = Stroke(width = (3f - ring * 0.5f).coerceAtLeast(1.2f))
                )
            }
            // Mist dome cap above the bottle
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = fizzPhase * 0.22f * fizzFlicker),
                        gold.copy(alpha = fizzPhase * 0.12f),
                        Color.Transparent
                    ),
                    center = bottleNeck - Offset(0f, haloR * 0.18f),
                    radius = haloR * 0.28f
                ),
                topLeft = Offset(bottleNeck.x - haloR * 0.32f, bottleNeck.y - haloR * 0.48f),
                size = Size(haloR * 0.64f, haloR * 0.36f)
            )
        }

        // Act V — bubble helix rising within the halo column
        if (bubblePhase > 0f) {
            for (b in 0 until splashLoopCount(26)) {
                val seed = b
                val delay = (b % 8) * 0.022f
                val local = ((bubblePhase - delay) / 0.85f).coerceIn(0f, 1f)
                if (local <= 0f) continue
                val rise = local * local
                val helixAngle = (seed * 24f + progress * 90f) * PI.toFloat() / 180f
                val helixR = haloR * 0.22f * (0.6f + sin(seed.toFloat()) * 0.2f)
                val x = haloCenter.x + cos(helixAngle) * helixR
                val y = bottleBase.y - rise * haloR * 1.35f
                if ((Offset(x, y) - haloCenter).getDistance() > haloR * 1.02f) continue
                val bubbleR = (3f + (b % 5) * 1.4f)
                val alpha = (1f - local * 0.32f).coerceIn(0.25f, 0.92f) * fizzFlicker
                drawFoundChampagneBubble(
                    center = Offset(x, y),
                    radius = bubbleR,
                    alpha = alpha,
                    gold = gold
                )
                if (local in 0.55f..0.72f && b % 4 == 0) {
                    val spark = sin(((local - 0.55f) / 0.17f) * PI.toFloat())
                    for (s in 0 until 4) {
                        val sa = (s * 90f + seed) * PI.toFloat() / 180f
                        drawCircle(
                            color = Color.White.copy(alpha = spark * 0.70f),
                            radius = 1.3f,
                            center = Offset(x, y) + Offset(cos(sa), sin(sa)) * bubbleR * 1.6f
                        )
                    }
                }
            }
        }

        // Toast — flutes at halo sides + localized sparkle
        if (toastPhase > 0f) {
            listOf(-1f, 1f).forEach { side ->
                drawFoundChampagneFlute(
                    base = Offset(haloCenter.x + side * haloR * 0.72f, haloCenter.y + haloR * 0.78f),
                    minDim = minDim * 0.48f,
                    phase = toastPhase,
                    gold = gold
                )
            }
            for (f in 0 until splashLoopCount(12)) {
                val angle = (f * 30f + toastPhase * 40f) * PI.toFloat() / 180f
                val dist = haloR * (0.40f + (f % 3) * 0.12f)
                val fx = haloCenter.x + cos(angle) * dist
                val fy = haloCenter.y + sin(angle) * dist * 0.70f - haloR * 0.15f
                val fa = toastPhase * (1f - f * 0.04f).coerceAtLeast(0.35f) * 0.50f
                drawCircle(
                    color = Color.White.copy(alpha = fa * fizzFlicker),
                    radius = 1.2f + (f % 2),
                    center = Offset(fx, fy)
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        gold.copy(alpha = toastPhase * 0.14f * fizzFlicker),
                        Color.Transparent
                    ),
                    center = haloCenter,
                    radius = haloR * 0.48f
                ),
                radius = haloR * 0.48f,
                center = haloCenter
            )
        }
    }
}

private fun DrawScope.drawFoundChampagneBottle(
    base: Offset,
    minDim: Float,
    phase: Float,
    gold: Color,
    accent: Color
) {
    val bodyW = minDim * 0.11f * phase
    val bodyH = minDim * 0.22f * phase
    val neckW = minDim * 0.035f * phase
    val neckH = minDim * 0.07f * phase
    val bodyTop = base.y - bodyH
    val neckBottom = bodyTop
    val neckTop = neckBottom - neckH

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2E7D32).copy(alpha = 0.95f * phase),
                Color(0xFF1B5E20).copy(alpha = 0.98f * phase)
            ),
            startY = bodyTop,
            endY = base.y
        ),
        topLeft = Offset(base.x - bodyW / 2f, bodyTop),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(bodyW * 0.18f)
    )
    drawRoundRect(
        color = Color(0xFF1B5E20).copy(alpha = 0.95f * phase),
        topLeft = Offset(base.x - neckW / 2f, neckTop),
        size = Size(neckW, neckH),
        cornerRadius = CornerRadius(neckW * 0.2f)
    )
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                gold.copy(alpha = 0.75f * phase),
                Color.White.copy(alpha = 0.55f * phase),
                gold.copy(alpha = 0.75f * phase)
            ),
            startX = base.x - bodyW * 0.35f,
            endX = base.x + bodyW * 0.35f
        ),
        topLeft = Offset(base.x - bodyW * 0.35f, bodyTop + bodyH * 0.38f),
        size = Size(bodyW * 0.70f, bodyH * 0.14f)
    )
    drawCircle(
        color = accent.copy(alpha = 0.35f * phase),
        radius = minDim * 0.012f,
        center = Offset(base.x - bodyW * 0.25f, bodyTop + bodyH * 0.22f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.25f * phase),
        radius = minDim * 0.010f,
        center = Offset(base.x + bodyW * 0.20f, bodyTop + bodyH * 0.55f)
    )
}

private fun DrawScope.drawFoundChampagneCork(
    pos: Offset,
    rotation: Float,
    scale: Float,
    alpha: Float
) {
    rotate(rotation, pivot = pos) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFBCAAA4).copy(alpha = alpha),
                    Color(0xFF8D6E63).copy(alpha = alpha)
                )
            ),
            topLeft = Offset(pos.x - 5f * scale, pos.y - 10f * scale),
            size = Size(10f * scale, 18f * scale),
            cornerRadius = CornerRadius(3f * scale)
        )
    }
}

private fun DrawScope.drawFoundChampagnePopBurst(
    origin: Offset,
    phase: Float,
    minDim: Float,
    gold: Color,
    accent: Color,
    flicker: Float
) {
    val flash = ((phase - 0.02f) / 0.14f).coerceIn(0f, 1f)
    if (flash > 0f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = (1f - flash) * 0.65f),
                    gold.copy(alpha = (1f - flash) * 0.40f),
                    Color.Transparent
                ),
                center = origin,
                radius = minDim * 0.14f
            ),
            radius = minDim * 0.14f,
            center = origin
        )
    }
    for (chunk in 0 until splashLoopCount(18)) {
        val local = smoothStep(((phase - 0.04f - chunk * 0.015f) / 0.55f).coerceIn(0f, 1f))
        if (local <= 0f) continue
        val angle = (chunk * 20f + phase * 35f) * PI.toFloat() / 180f
        val dist = minDim * 0.10f * local
        val pos = origin + Offset(cos(angle) * dist, sin(angle) * dist - dist * 1.3f)
        drawCircle(
            color = Color(0xFF8D6E63).copy(alpha = (1f - local) * 0.85f),
            radius = 2.5f + (chunk % 3),
            center = pos
        )
    }
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = phase * 0.25f * flicker),
                accent.copy(alpha = phase * 0.12f),
                Color.Transparent
            ),
            center = origin - Offset(0f, minDim * 0.02f),
            radius = minDim * 0.08f
        ),
        topLeft = Offset(origin.x - minDim * 0.10f, origin.y - minDim * 0.06f),
        size = Size(minDim * 0.20f, minDim * 0.08f)
    )
}

private fun DrawScope.drawFoundChampagneFizzSpray(
    origin: Offset,
    side: Float,
    phase: Float,
    minDim: Float,
    h: Float,
    gold: Color,
    accent: Color,
    flicker: Float
) {
    for (jet in 0 until splashLoopCount(12)) {
        val delay = jet * 0.05f
        val local = smoothStep(((phase - delay) / 0.75f).coerceIn(0f, 1f))
        if (local <= 0f) continue
        val angle = (-75f + jet * 11f * side) * PI.toFloat() / 180f
        val dist = minDim * local * (0.35f + (jet % 3) * 0.10f)
        val pos = origin + Offset(cos(angle) * dist, sin(angle) * dist)
        drawFoundChampagneBubble(
            center = pos,
            radius = 3f + (jet % 4) * 1.2f,
            alpha = (1f - local * 0.4f) * flicker,
            gold = gold
        )
    }
    val sprayPath = Path()
    var started = false
    for (step in 0..16) {
        val t = step / 16f * phase
        val x = origin.x + side * minDim * t * 0.32f
        val y = origin.y - minDim * t * 0.55f + sin(t * PI.toFloat() * 2f) * minDim * 0.03f
        val point = Offset(x, y)
        if (!started) {
            sprayPath.moveTo(point.x, point.y)
            started = true
        } else {
            sprayPath.lineTo(point.x, point.y)
        }
    }
    if (started) {
        drawPath(
            sprayPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = phase * 0.55f * flicker),
                    gold.copy(alpha = phase * 0.35f),
                    accent.copy(alpha = phase * 0.15f)
                ),
                start = origin,
                end = origin + Offset(side * minDim * 0.28f, -minDim * 0.45f)
            ),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawFoundChampagneBubble(
    center: Offset,
    radius: Float,
    alpha: Float,
    gold: Color
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha * 0.95f),
                gold.copy(alpha = alpha * 0.55f),
                gold.copy(alpha = alpha * 0.12f),
                Color.Transparent
            ),
            center = center - Offset(radius * 0.25f, radius * 0.25f),
            radius = radius
        ),
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color.White.copy(alpha = alpha * 0.80f),
        radius = radius * 0.22f,
        center = center - Offset(radius * 0.35f, radius * 0.35f)
    )
}

private fun DrawScope.drawFoundChampagneFlute(
    base: Offset,
    minDim: Float,
    phase: Float,
    gold: Color
) {
    val stemH = minDim * 0.08f * phase
    val bowlH = minDim * 0.07f * phase
    val bowlW = minDim * 0.045f * phase
    drawLine(
        color = Color.White.copy(alpha = 0.55f * phase),
        start = base,
        end = base - Offset(0f, stemH),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.White.copy(alpha = 0.65f * phase),
        start = base - Offset(0f, stemH),
        end = base - Offset(0f, stemH + bowlH),
        strokeWidth = bowlW,
        cap = StrokeCap.Round
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                gold.copy(alpha = 0.45f * phase),
                Color.Transparent
            ),
            center = base - Offset(0f, stemH + bowlH * 0.5f),
            radius = bowlW * 0.8f
        ),
        radius = bowlW * 0.8f,
        center = base - Offset(0f, stemH + bowlH * 0.5f)
    )
}

private fun DrawScope.drawFoundHalloweenCobweb(origin: Offset, span: Float, alpha: Float, mirror: Boolean) {
    val baseAngle = if (mirror) 135f else 45f
    for (spoke in 0 until 7) {
        val angle = (baseAngle + spoke * 14f) * PI.toFloat() / 180f
        drawLine(
            color = Color.White.copy(alpha = alpha * (0.55f + spoke * 0.06f)),
            start = origin,
            end = origin + Offset(cos(angle), sin(angle)) * span,
            strokeWidth = 0.9f,
            cap = StrokeCap.Round
        )
    }
    for (ring in 1..4) {
        val t = ring / 4f
        val arcSpan = span * t
        val start = if (mirror) 90f else 0f
        drawArc(
            color = Color.White.copy(alpha = alpha * 0.35f),
            startAngle = start,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(origin.x - arcSpan, origin.y - arcSpan),
            size = Size(arcSpan * 2f, arcSpan * 2f),
            style = Stroke(width = 0.7f)
        )
    }
}

private fun DrawScope.drawFoundHalloweenGhost(center: Offset, size: Float, alpha: Float) {
    val w = size * 0.52f
    val h = size * 0.68f
    val top = center.y - h * 0.42f
    drawRoundRect(
        color = Color.White.copy(alpha = alpha),
        topLeft = Offset(center.x - w * 0.5f, top),
        size = Size(w, h * 0.58f),
        cornerRadius = CornerRadius(w * 0.5f)
    )
    val tail = Path().apply {
        moveTo(center.x - w * 0.5f, top + h * 0.54f)
        cubicTo(
            center.x - w * 0.22f, top + h * 0.72f,
            center.x - w * 0.06f, top + h * 0.48f,
            center.x, top + h * 0.62f
        )
        cubicTo(
            center.x + w * 0.06f, top + h * 0.48f,
            center.x + w * 0.22f, top + h * 0.72f,
            center.x + w * 0.5f, top + h * 0.54f
        )
        close()
    }
    drawPath(tail, color = Color.White.copy(alpha = alpha))
    listOf(-1f, 1f).forEach { side ->
        drawOval(
            color = Color(0xFF1A0A04).copy(alpha = alpha * 0.88f),
            topLeft = Offset(center.x + side * w * 0.17f - w * 0.08f, top + h * 0.18f),
            size = Size(w * 0.16f, h * 0.18f)
        )
    }
}

private fun DrawScope.drawFoundHalloweenJackGlow(
    center: Offset,
    radius: Float,
    flicker: Float,
    alpha: Float
) {
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF176).copy(alpha = alpha * flicker * 0.55f),
                Color(0xFFFF9100).copy(alpha = alpha * flicker * 0.35f),
                Color.Transparent
            ),
            center = center,
            radius = radius * 1.35f
        ),
        topLeft = Offset(center.x - radius * 1.35f, center.y - radius * 1.1f),
        size = Size(radius * 2.7f, radius * 2.2f)
    )
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFB74D),
                Color(0xFFFF7A1A),
                Color(0xFFBF4F00),
                Color(0xFF6B2800)
            ),
            center = center - Offset(radius * 0.12f, radius * 0.1f),
            radius = radius
        ),
        topLeft = Offset(center.x - radius, center.y - radius * 0.86f),
        size = Size(radius * 2f, radius * 1.72f)
    )
    for (i in -2..2) {
        val x = center.x + i * radius * 0.16f
        drawLine(
            color = Color(0xFFD45A00).copy(alpha = alpha * 0.45f),
            start = Offset(x, center.y - radius * 0.72f),
            end = Offset(x, center.y + radius * 0.72f),
            strokeWidth = 1.6f,
            cap = StrokeCap.Round
        )
    }
    fun carveGlow(pathCenter: Offset, haloR: Float) = Brush.radialGradient(
        colors = listOf(
            Color(0xFFFFFDE7).copy(alpha = alpha * flicker),
            Color(0xFFFFAB40).copy(alpha = alpha * flicker * 0.9f),
            Color(0xFFE65100).copy(alpha = alpha * flicker * 0.65f)
        ),
        center = pathCenter,
        radius = haloR
    )
    listOf(-1f, 1f).forEach { side ->
        val ex = center.x + side * radius * 0.26f
        val ey = center.y - radius * 0.16f
        val ew = radius * 0.19f
        val eye = Path().apply {
            moveTo(ex, ey - ew * 0.58f)
            lineTo(ex - side * ew * 0.58f, ey + ew * 0.46f)
            lineTo(ex + side * ew * 0.58f, ey + ew * 0.46f)
            close()
        }
        drawPath(eye, brush = carveGlow(Offset(ex, ey), ew * 0.95f))
    }
    val mouthY = center.y + radius * 0.18f
    drawArc(
        brush = carveGlow(Offset(center.x, mouthY), radius * 0.34f),
        startAngle = 12f,
        sweepAngle = 156f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.58f, mouthY - radius * 0.08f),
        size = Size(radius * 1.16f, radius * 0.52f),
        style = Stroke(width = radius * 0.13f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawFoundHalloweenBat(pos: Offset, wingFlap: Float, scale: Float, alpha: Float) {
    val s = scale
    drawLine(
        color = Color(0xFF1A0A04).copy(alpha = alpha),
        start = pos,
        end = pos + Offset(-s * 0.9f, -wingFlap),
        strokeWidth = 2.4f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF1A0A04).copy(alpha = alpha),
        start = pos,
        end = pos + Offset(s * 0.9f, -wingFlap),
        strokeWidth = 2.4f,
        cap = StrokeCap.Round
    )
    drawCircle(color = Color(0xFFFF9100).copy(alpha = alpha * 0.85f), radius = s * 0.12f, center = pos)
}

@Composable
private fun HalloweenFoundEffect(progress: () -> Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val progress = progress()
        val w = size.width
        val h = size.height
        val minDim = size.minDimension
        val center = Offset(w / 2f, h / 2f)

        // Act I — moonlit sky & vignette
        val nightPhase = smoothStep((progress / 0.18f).coerceIn(0f, 1f))
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF120A1E).copy(alpha = 0.82f * nightPhase),
                    Color(0xFF0A0610).copy(alpha = 0.68f * nightPhase),
                    Color(0xFF100818).copy(alpha = 0.42f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = h * 0.88f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.35f * nightPhase)
                ),
                center = center,
                radius = minDim * 0.72f
            ),
            radius = minDim * 0.72f,
            center = center
        )

        // Harvest moon — hero light source, upper left
        val moonPhase = smoothStep(((progress - 0.02f) / 0.20f).coerceIn(0f, 1f))
        val moonCenter = Offset(w * 0.20f, h * 0.13f)
        val moonBreath = 0.9f + 0.1f * sin(progress * PI.toFloat() * 2.2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF176).copy(alpha = 0.42f * moonPhase),
                    Color(0xFFFF9100).copy(alpha = 0.24f * moonPhase),
                    Color.Transparent
                ),
                center = moonCenter,
                radius = minDim * 0.26f * moonBreath
            ),
            radius = minDim * 0.26f * moonBreath,
            center = moonCenter
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF8E1), Color(0xFFFFE082), Color(0xFFFFB74D)),
                center = moonCenter - Offset(minDim * 0.018f, minDim * 0.018f),
                radius = minDim * 0.068f
            ),
            radius = minDim * 0.068f * moonPhase,
            center = moonCenter
        )
        // Soft moonbeam toward center
        if (moonPhase > 0.2f) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFE082).copy(alpha = moonPhase * 0.08f),
                        Color(0xFFFF9100).copy(alpha = moonPhase * 0.04f),
                        Color.Transparent
                    ),
                    start = moonCenter,
                    end = center
                ),
                topLeft = Offset(moonCenter.x, moonCenter.y),
                size = Size(w * 0.55f, h * 0.45f)
            )
        }

        // Sparse stars — tasteful, not noisy
        for (s in 0 until splashLoopCount(16)) {
            val sx = (s * 91.7f + 23f) % w
            val sy = (s * 53.3f + 11f) % (h * 0.38f)
            val twinkle = sin(progress * PI.toFloat() * 2.8f + s * 1.3f) * 0.5f + 0.5f
            drawCircle(
                color = Color.White.copy(alpha = moonPhase * (0.14f + twinkle * 0.28f)),
                radius = 1f + (s % 2) * 0.5f,
                center = Offset(sx, sy)
            )
        }

        // Act II — haunted frame: cobwebs, tombstones, garland
        val framePhase = smoothStep(((progress - 0.10f) / 0.28f).coerceIn(0f, 1f))
        if (framePhase > 0f) {
            drawFoundHalloweenCobweb(Offset(w * 0.05f, h * 0.07f), minDim * 0.20f, framePhase * 0.55f, mirror = false)
            drawFoundHalloweenCobweb(Offset(w * 0.95f, h * 0.07f), minDim * 0.20f, framePhase * 0.55f, mirror = true)
            drawFoundHalloweenCobweb(Offset(w * 0.05f, h * 0.93f), minDim * 0.16f, framePhase * 0.4f, mirror = false)
            drawFoundHalloweenCobweb(Offset(w * 0.95f, h * 0.93f), minDim * 0.16f, framePhase * 0.4f, mirror = true)
        }

        // Grave fog — low, slow, purple-grey
        val fogPhase = smoothStep(((progress - 0.06f) / 0.38f).coerceIn(0f, 1f))
        for (f in 0 until 4) {
            val fx = w * (0.1f + f * 0.24f)
            val fy = h * 0.9f - fogPhase * h * 0.18f + sin(progress * PI.toFloat() * 0.9f + f * 1.2f) * 5f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF6A1FB8).copy(alpha = 0.16f * fogPhase),
                        Color.Transparent
                    ),
                    center = Offset(fx, fy),
                    radius = minDim * 0.14f
                ),
                topLeft = Offset(fx - minDim * 0.14f, fy - minDim * 0.045f),
                size = Size(minDim * 0.28f, minDim * 0.09f)
            )
        }

        // Tombstone gate posts — bottom edge, clear of the Found copy
        if (framePhase > 0.25f) {
            val stoneAlpha = ((framePhase - 0.25f) / 0.75f).coerceIn(0f, 1f) * 0.85f
            listOf(0.16f, 0.34f, 0.66f, 0.84f).forEachIndexed { i, xFrac ->
                val tx = w * xFrac
                val ty = h * 0.91f
                val tw = minDim * 0.13f
                val th = minDim * (0.11f + (i % 2) * 0.03f)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF6E6E6E), Color(0xFF3A3A3A), Color(0xFF222222))
                    ),
                    topLeft = Offset(tx - tw * 0.5f, ty - th),
                    size = Size(tw, th),
                    cornerRadius = CornerRadius(tw * 0.12f, tw * 0.04f)
                )
                drawLine(
                    color = Color(0xFF1A0A04).copy(alpha = stoneAlpha * 0.35f),
                    start = Offset(tx - tw * 0.2f, ty - th * 0.55f),
                    end = Offset(tx + tw * 0.2f, ty - th * 0.55f),
                    strokeWidth = 1.2f
                )
            }
        }

        // Pumpkin-light garland — high across the top, well above the Found copy
        if (framePhase > 0.15f) {
            val garlandLit = ((framePhase - 0.15f) / 0.85f).coerceIn(0f, 1f)
            for (i in 0 until splashLoopCount(11)) {
                val t = i / 10f
                val angle = (200f + t * 140f) * PI.toFloat() / 180f
                val gr = minDim * 0.62f
                val pos = Offset(
                    center.x + cos(angle) * gr,
                    h * 0.16f + sin(angle) * gr * 0.22f
                )
                val twinkle = 0.55f + 0.45f * sin(progress * PI.toFloat() * 3.5f + i * 0.9f)
                val lit = ((garlandLit * 11f - i) / 2f).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF9100).copy(alpha = lit * twinkle * 0.85f),
                            Color.Transparent
                        ),
                        center = pos,
                        radius = minDim * 0.028f
                    ),
                    radius = minDim * 0.028f,
                    center = pos
                )
                drawCircle(
                    color = Color(0xFFFFF176).copy(alpha = lit * twinkle * 0.9f),
                    radius = minDim * 0.009f,
                    center = pos
                )
            }
        }

        // Act III — séance spirit rings frame the copy; they start outside the text, never under it
        val ringPhase = smoothStep(((progress - 0.14f) / 0.62f).coerceIn(0f, 1f))
        if (ringPhase > 0f) {
            val flicker = (0.78f + 0.22f * sin(progress * PI.toFloat() * 0.95f)).coerceIn(0.72f, 1f)
            for (ring in 0 until splashLoopCount(8)) {
                val delay = ring * 0.07f
                val local = ((ringPhase - delay) / 0.78f).coerceIn(0f, 1f)
                if (local <= 0f) continue
                val radius = minDim * (0.44f + 0.28f * smoothStep(local))
                val alpha = (1f - local).coerceIn(0f, 1f) * 0.72f * flicker
                val orange = Color(0xFFFF9100)
                val violet = Color(0xFF9B30FF)
                val tint = if (ring % 2 == 0) orange else violet
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            tint.copy(alpha = alpha),
                            Color.White.copy(alpha = alpha * 0.75f),
                            tint.copy(alpha = alpha * 0.55f),
                            tint.copy(alpha = alpha)
                        ),
                        center = center
                    ),
                    radius = radius,
                    center = center,
                    style = Stroke(width = (4.5f - ring * 0.35f).coerceAtLeast(1.5f))
                )
                for (dot in 0 until 6) {
                    val a = (dot * 60f + ring * 14f + progress * 22f) * PI.toFloat() / 180f
                    drawCircle(
                        color = Color(0xFFFFF176).copy(alpha = alpha * 0.85f),
                        radius = 2.2f,
                        center = center + Offset(cos(a), sin(a)) * radius
                    )
                }
            }
        }

        // Hero jack-o'-lanterns — left/right margins, never overlapping the Found copy
        val jackPhase = smoothStep(((progress - 0.18f) / 0.35f).coerceIn(0f, 1f))
        val jackFade = (1f - smoothStep(((progress - 0.62f) / 0.28f).coerceIn(0f, 1f)) * 0.35f)
        if (jackPhase > 0f) {
            val flicker = (0.76f + 0.24f * sin(progress * PI.toFloat() * 0.88f)).coerceIn(0.68f, 1f)
            listOf(w * 0.13f, w * 0.87f).forEach { x ->
                drawFoundHalloweenJackGlow(
                    center = Offset(x, center.y - minDim * 0.06f),
                    radius = minDim * 0.11f,
                    flicker = flicker,
                    alpha = jackPhase * jackFade * 0.9f
                )
            }
            listOf(w * 0.22f, w * 0.78f).forEach { x ->
                drawFoundHalloweenJackGlow(
                    center = Offset(x, h * 0.78f),
                    radius = minDim * 0.075f,
                    flicker = flicker,
                    alpha = jackPhase * jackFade * 0.7f
                )
            }
        }

        // Act IV — V-formation bat sweep across the moon (already above the copy)
        val batPhase = smoothStep(((progress - 0.24f) / 0.32f).coerceIn(0f, 1f))
        if (batPhase > 0f && batPhase < 1f) {
            for (b in 0 until 5) {
                val offset = (b - 2) * 0.14f
                val bx = w * (0.08f + batPhase * 0.84f)
                val by = h * (0.14f + offset * offset * 0.7f) + sin(batPhase * PI.toFloat() * 3f + b) * minDim * 0.02f
                val wing = sin(batPhase * PI.toFloat() * 12f + b * 1.5f) * minDim * 0.028f
                val alpha = (1f - kotlin.math.abs(batPhase - 0.5f) * 2f).coerceIn(0.15f, 0.95f)
                drawFoundHalloweenBat(Offset(bx, by), wing, minDim * 0.045f, alpha)
            }
        }

        // Rising spirits — flanks and a low pass under the copy, never through the title
        for (g in 0 until 3) {
            val delay = 0.26f + g * 0.08f
            val local = smoothStep(((progress - delay) / 0.50f).coerceIn(0f, 1f))
            if (local <= 0f) continue
            val gx = when (g) {
                0 -> w * 0.14f
                1 -> w * 0.86f
                else -> center.x
            }
            val gy = if (g < 2) {
                h * 0.90f - local * h * 0.55f + sin(local * PI.toFloat() * 2.5f + g) * 10f
            } else {
                h * 0.94f - local * h * 0.16f + sin(local * PI.toFloat() * 2.2f) * 8f
            }
            val alpha = (1f - local * 0.55f).coerceIn(0.2f, 0.7f)
            drawFoundHalloweenGhost(Offset(gx, gy), minDim * 0.11f, alpha)
        }

        // Act V — autumn leaf drift (refined, fewer particles)
        if (progress > 0.30f) {
            val leafPhase = ((progress - 0.30f) / 0.70f).coerceIn(0f, 1f)
            for (p in 0 until splashLoopCount(22)) {
                val seed = p * 31.3f
                val delay = (p % 10) * 0.018f
                val local = ((leafPhase - delay) / 0.85f).coerceIn(0f, 1f)
                if (local <= 0f) continue
                val px = (seed * 19f + local * 22f) % w
                val py = h * 0.05f + local * h * 0.92f
                val sway = sin(local * PI.toFloat() * 4f + seed) * minDim * 0.035f
                val alpha = (1f - local * 0.45f).coerceIn(0.2f, 0.8f)
                rotate(local * 180f + seed, pivot = Offset(px + sway, py)) {
                    val leaf = Path().apply {
                        moveTo(px + sway, py)
                        lineTo(px + sway + 9f, py + 16f)
                        lineTo(px + sway - 9f, py + 16f)
                        close()
                    }
                    val leafColor = when (p % 3) {
                        0 -> Color(0xFFFF7A1A)
                        1 -> Color(0xFFD84315)
                        else -> Color(0xFFBF360C)
                    }
                    drawPath(leaf, color = leafColor.copy(alpha = alpha * 0.88f))
                }
            }
        }

        // Single cinematic lightning flash
        if (progress in 0.40f..0.50f) {
            val flash = sin(((progress - 0.40f) / 0.10f) * PI.toFloat())
            if (flash > 0.25f) {
                drawRect(color = Color(0xFFE1BEE7).copy(alpha = flash * 0.09f))
                // Bolt zigzag near moon
                val bolt = Path().apply {
                    moveTo(moonCenter.x + minDim * 0.06f, moonCenter.y + minDim * 0.05f)
                    lineTo(moonCenter.x + minDim * 0.12f, moonCenter.y + minDim * 0.14f)
                    lineTo(moonCenter.x + minDim * 0.08f, moonCenter.y + minDim * 0.14f)
                    lineTo(moonCenter.x + minDim * 0.16f, moonCenter.y + minDim * 0.26f)
                }
                drawPath(
                    bolt,
                    color = Color.White.copy(alpha = flash * 0.65f),
                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                )
            }
        }

        // Warm ember linger — celebration tail
        if (progress > 0.50f) {
            val drift = ((progress - 0.50f) / 0.50f).coerceIn(0f, 1f)
            for (e in 0 until splashLoopCount(18)) {
                val ex = (e * 107f + drift * 28f) % w
                val ey = h * (0.22f + (e * 0.038f) % 0.55f) - drift * 10f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFF176).copy(alpha = (1f - drift * 0.55f) * 0.5f),
                            Color(0xFFFF9100).copy(alpha = (1f - drift * 0.55f) * 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(ex, ey),
                        radius = 4f
                    ),
                    radius = 4f,
                    center = Offset(ex, ey)
                )
            }
        }
    }
}

private fun DrawScope.drawFoundChristmasFrostCorner(origin: Offset, span: Float, alpha: Float, mirror: Boolean) {
    val dir = if (mirror) -1f else 1f
    for (icicle in 0 until 4) {
        val ix = origin.x + dir * icicle * span * 0.22f
        val len = span * (0.18f + (icicle % 2) * 0.08f)
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = alpha * 0.75f),
                    Color(0xFFB3E5FC).copy(alpha = alpha * 0.2f)
                ),
                startY = origin.y,
                endY = origin.y + len
            ),
            start = Offset(ix, origin.y),
            end = Offset(ix + dir * span * 0.02f, origin.y + len),
            strokeWidth = 1.8f,
            cap = StrokeCap.Round
        )
    }
    drawLine(
        color = Color.White.copy(alpha = alpha * 0.35f),
        start = origin,
        end = origin + Offset(dir * span, 0f),
        strokeWidth = 1.2f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawFoundChristmasSnowflakeArm(
    center: Offset,
    armLen: Float,
    alpha: Float,
    rotationDeg: Float
) {
    rotate(rotationDeg, pivot = center) {
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = alpha),
                    Color(0xFF4DD0E1).copy(alpha = alpha * 0.75f),
                    Color(0xFF0277BD).copy(alpha = alpha * 0.45f)
                ),
                start = center,
                end = center + Offset(0f, -armLen)
            ),
            start = center,
            end = center + Offset(0f, -armLen),
            strokeWidth = 2.8f,
            cap = StrokeCap.Round
        )
        val mid = center + Offset(0f, -armLen * 0.55f)
        val branch = armLen * 0.22f
        drawLine(
            color = Color.White.copy(alpha = alpha * 0.9f),
            start = mid + Offset(-branch, 0f),
            end = mid + Offset(branch, 0f),
            strokeWidth = 1.8f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawFoundChristmasOrnamentGlow(
    center: Offset,
    radius: Float,
    tint: Color,
    alpha: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha * 0.45f),
                tint.copy(alpha = alpha * 0.35f),
                Color.Transparent
            ),
            center = center,
            radius = radius * 1.6f
        ),
        radius = radius * 1.6f,
        center = center
    )
    drawLine(
        color = Color(0xFF8D6E63).copy(alpha = alpha * 0.55f),
        start = center + Offset(0f, -radius * 1.5f),
        end = center + Offset(0f, -radius * 0.95f),
        strokeWidth = 1.4f,
        cap = StrokeCap.Round
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha * 0.65f),
                tint,
                tint.copy(alpha = alpha * 0.75f)
            ),
            center = center - Offset(radius * 0.15f, radius * 0.12f),
            radius = radius
        ),
        radius = radius,
        center = center
    )
    drawArc(
        color = Color.White.copy(alpha = alpha * 0.35f),
        startAngle = 215f,
        sweepAngle = 50f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.9f, center.y - radius * 0.9f),
        size = Size(radius * 1.8f, radius * 1.8f),
        style = Stroke(width = radius * 0.08f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawFoundChristmasGiftBox(
    center: Offset,
    size: Float,
    unwrap: Float,
    alpha: Float
) {
    val boxH = size * 0.72f
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFE0233D), Color(0xFFA10E24)),
            start = Offset(center.x - size, center.y),
            end = Offset(center.x + size, center.y)
        ),
        topLeft = Offset(center.x - size, center.y - boxH * 0.5f),
        size = Size(size * 2f, boxH),
        cornerRadius = CornerRadius(size * 0.1f)
    )
    drawLine(
        color = Color(0xFFFFD54F).copy(alpha = alpha),
        start = Offset(center.x, center.y - boxH * 0.5f),
        end = Offset(center.x, center.y + boxH * 0.5f),
        strokeWidth = size * 0.14f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFFFD54F).copy(alpha = alpha),
        start = Offset(center.x - size, center.y),
        end = Offset(center.x + size, center.y),
        strokeWidth = size * 0.14f,
        cap = StrokeCap.Round
    )
    if (unwrap > 0.08f) {
        val lidLift = unwrap * size * 1.4f
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFF6B7F), Color(0xFFE0233D)),
                start = Offset(center.x - size, center.y - boxH * 0.5f - lidLift),
                end = Offset(center.x + size, center.y - lidLift)
            ),
            topLeft = Offset(center.x - size, center.y - boxH * 0.58f - lidLift),
            size = Size(size * 2f, boxH * 0.28f),
            cornerRadius = CornerRadius(size * 0.08f)
        )
        for (r in 0 until splashLoopCount(8)) {
            val angle = (r * 45f + unwrap * 70f) * PI.toFloat() / 180f
            val dist = size * unwrap * 1.6f
            val pos = center + Offset(cos(angle), sin(angle)) * dist - Offset(0f, lidLift * 0.4f)
            rotate(r * 45f + unwrap * 260f, pivot = pos) {
                drawRect(
                    color = (if (r % 2 == 0) Color(0xFFE0233D) else Color(0xFFFFD54F)).copy(alpha = alpha * (1f - unwrap * 0.5f)),
                    topLeft = Offset(pos.x - 2.5f, pos.y - 9f),
                    size = Size(5f, 18f)
                )
            }
        }
    }
}

private fun DrawScope.drawFoundChristmasNorthStar(
    center: Offset,
    outerR: Float,
    rayPhase: Float,
    alpha: Float
) {
    for (ray in 0 until splashLoopCount(8)) {
        val rayLen = outerR * (0.55f + rayPhase * 0.65f)
        val angle = (ray * 45f - 90f) * PI.toFloat() / 180f
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = alpha * 0.9f),
                    Color(0xFFFFD54F).copy(alpha = alpha * 0.45f),
                    Color.Transparent
                ),
                start = center,
                end = center + Offset(cos(angle), sin(angle)) * rayLen
            ),
            start = center,
            end = center + Offset(cos(angle), sin(angle)) * rayLen,
            strokeWidth = if (ray % 2 == 0) 2.6f else 1.6f,
            cap = StrokeCap.Round
        )
    }
    drawPath(
        path = drawFivePointStar(center, outerR * 0.42f, outerR * 0.18f),
        brush = Brush.linearGradient(
            colors = listOf(Color.White, Color(0xFFFFE082), Color(0xFFFFB300)),
            start = Offset(center.x, center.y - outerR * 0.5f),
            end = Offset(center.x, center.y + outerR * 0.5f)
        )
    )
}

@Composable
private fun ChristmasFoundEffect(progress: () -> Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val progress = progress()
        val w = size.width
        val h = size.height
        val minDim = size.minDimension
        val center = Offset(w / 2f, h / 2f)

        // Act I — midnight winter sky & soft vignette
        val nightPhase = smoothStep((progress / 0.18f).coerceIn(0f, 1f))
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0A1628).copy(alpha = 0.78f * nightPhase),
                    Color(0xFF061018).copy(alpha = 0.62f * nightPhase),
                    Color(0xFF0A1810).copy(alpha = 0.38f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = h * 0.88f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0xFF061018).copy(alpha = 0.4f * nightPhase)),
                center = center,
                radius = minDim * 0.72f
            ),
            radius = minDim * 0.72f,
            center = center
        )

        // Sparse star field
        for (s in 0 until splashLoopCount(16)) {
            val sx = (s * 88.3f + 19f) % w
            val sy = (s * 49.1f + 7f) % (h * 0.4f)
            val twinkle = sin(progress * PI.toFloat() * 2.6f + s * 1.2f) * 0.5f + 0.5f
            drawCircle(
                color = Color.White.copy(alpha = nightPhase * (0.16f + twinkle * 0.32f)),
                radius = 1f + (s % 2) * 0.55f,
                center = Offset(sx, sy)
            )
        }

        // Aurora borealis — top of the sky, above the Found copy
        val auroraPhase = smoothStep(((progress - 0.06f) / 0.50f).coerceIn(0f, 1f))
        if (auroraPhase > 0f) {
            val auroraCenter = Offset(center.x, h * 0.16f)
            rotate(progress * 24f - 90f, pivot = auroraCenter) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF1FA34D).copy(alpha = 0.18f * auroraPhase),
                            Color(0xFF4DD0E1).copy(alpha = 0.32f * auroraPhase),
                            Color(0xFFE0233D).copy(alpha = 0.16f * auroraPhase),
                            Color(0xFFFFD54F).copy(alpha = 0.10f * auroraPhase),
                            Color.Transparent
                        ),
                        center = auroraCenter
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = true,
                    topLeft = Offset(auroraCenter.x - minDim * 0.52f, auroraCenter.y - minDim * 0.52f),
                    size = Size(minDim * 1.04f, minDim * 1.04f)
                )
            }
            rotate(-progress * 18f + 45f, pivot = auroraCenter) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF7B1FA2).copy(alpha = 0.12f * auroraPhase),
                            Color(0xFF26A69A).copy(alpha = 0.22f * auroraPhase),
                            Color.Transparent
                        ),
                        center = auroraCenter
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = true,
                    topLeft = Offset(auroraCenter.x - minDim * 0.44f, auroraCenter.y - minDim * 0.44f),
                    size = Size(minDim * 0.88f, minDim * 0.88f)
                )
            }
        }

        // Act II — winter frame: frost corners, candy-cane ring, light garland
        val framePhase = smoothStep(((progress - 0.10f) / 0.28f).coerceIn(0f, 1f))
        if (framePhase > 0f) {
            drawFoundChristmasFrostCorner(Offset(w * 0.04f, h * 0.06f), minDim * 0.18f, framePhase * 0.6f, mirror = false)
            drawFoundChristmasFrostCorner(Offset(w * 0.96f, h * 0.06f), minDim * 0.18f, framePhase * 0.6f, mirror = true)
            drawFoundChristmasFrostCorner(Offset(w * 0.04f, h * 0.94f), minDim * 0.14f, framePhase * 0.45f, mirror = false)
            drawFoundChristmasFrostCorner(Offset(w * 0.96f, h * 0.94f), minDim * 0.14f, framePhase * 0.45f, mirror = true)

            rotate(progress * 45f, pivot = center) {
                val candy = listOf(Color(0xFFE0233D), Color.White)
                val candyR = minDim * 0.58f
                for (seg in 0 until splashLoopCount(12)) {
                    drawArc(
                        color = candy[seg % candy.size].copy(alpha = framePhase * 0.88f),
                        startAngle = seg * 30f,
                        sweepAngle = 30f,
                        useCenter = false,
                        topLeft = Offset(center.x - candyR, center.y - candyR),
                        size = Size(candyR * 2f, candyR * 2f),
                        style = Stroke(width = minDim * 0.028f, cap = StrokeCap.Butt)
                    )
                }
            }
        }

        // Light garland — high across the top, well above the Found copy
        if (framePhase > 0.12f) {
            val garlandLit = ((framePhase - 0.12f) / 0.88f).coerceIn(0f, 1f)
            val lightColors = listOf(
                Color(0xFFFF4D4D), Color(0xFF3DDC6E), Color(0xFFFFD24D), Color(0xFF64B5F6)
            )
            for (i in 0 until splashLoopCount(11)) {
                val t = i / 10f
                val angle = (200f + t * 140f) * PI.toFloat() / 180f
                val gr = minDim * 0.62f
                val pos = Offset(
                    center.x + cos(angle) * gr,
                    h * 0.15f + sin(angle) * gr * 0.20f
                )
                val lit = ((garlandLit * 11f - i) / 2.2f).coerceIn(0f, 1f)
                val twinkle = 0.58f + 0.42f * sin(progress * PI.toFloat() * 3.2f + i * 0.85f)
                if (i > 0) {
                    val prevT = (i - 1) / 10f
                    val prevAngle = (200f + prevT * 140f) * PI.toFloat() / 180f
                    val prevPos = Offset(
                        center.x + cos(prevAngle) * gr,
                        h * 0.15f + sin(prevAngle) * gr * 0.20f
                    )
                    drawLine(
                        color = Color(0xFF2E4A32).copy(alpha = lit * 0.45f),
                        start = prevPos,
                        end = pos,
                        strokeWidth = 1.1f,
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            lightColors[i % 4].copy(alpha = lit * twinkle * 0.85f),
                            Color.Transparent
                        ),
                        center = pos,
                        radius = minDim * 0.026f
                    ),
                    radius = minDim * 0.026f,
                    center = pos
                )
                drawCircle(
                    color = Color.White.copy(alpha = lit * twinkle * 0.75f),
                    radius = minDim * 0.008f,
                    center = pos - Offset(minDim * 0.006f, minDim * 0.006f)
                )
            }
        }

        // Act III — snowflake spirit rings frame the copy; they start outside the text
        val ringPhase = smoothStep(((progress - 0.14f) / 0.62f).coerceIn(0f, 1f))
        if (ringPhase > 0f) {
            val shimmer = 0.82f + 0.18f * sin(progress * PI.toFloat() * 1.1f)
            for (ring in 0 until splashLoopCount(8)) {
                val delay = ring * 0.07f
                val local = ((ringPhase - delay) / 0.78f).coerceIn(0f, 1f)
                if (local <= 0f) continue
                val radius = minDim * (0.44f + 0.28f * smoothStep(local))
                val alpha = (1f - local).coerceIn(0f, 1f) * 0.7f * shimmer
                val ice = Color(0xFF4DD0E1)
                val gold = Color(0xFFFFD54F)
                val tint = if (ring % 2 == 0) ice else gold
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            tint.copy(alpha = alpha),
                            Color.White.copy(alpha = alpha * 0.8f),
                            tint.copy(alpha = alpha * 0.5f),
                            tint.copy(alpha = alpha)
                        ),
                        center = center
                    ),
                    radius = radius,
                    center = center,
                    style = Stroke(width = (4.2f - ring * 0.32f).coerceAtLeast(1.4f))
                )
                for (dot in 0 until 6) {
                    val a = (dot * 60f + ring * 12f + progress * 18f) * PI.toFloat() / 180f
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.88f),
                        radius = 2f,
                        center = center + Offset(cos(a), sin(a)) * radius
                    )
                }
            }
        }

        // North Star — pillar stops above the Found copy so it never punches through the title
        val starPhase = smoothStep(((progress - 0.16f) / 0.38f).coerceIn(0f, 1f))
        val starCenter = Offset(center.x, h * 0.09f)
        if (starPhase > 0f) {
            val pillarStrength = smoothStep(((progress - 0.24f) / 0.32f).coerceIn(0f, 1f))
            val pillarEnd = center.y - minDim * 0.24f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = starPhase * pillarStrength * 0.32f),
                        Color(0xFFFFD54F).copy(alpha = starPhase * pillarStrength * 0.2f),
                        Color(0xFF4DD0E1).copy(alpha = starPhase * pillarStrength * 0.08f),
                        Color.Transparent
                    ),
                    startY = starCenter.y,
                    endY = pillarEnd
                ),
                topLeft = Offset(center.x - minDim * 0.055f, starCenter.y),
                size = Size(minDim * 0.11f, (pillarEnd - starCenter.y).coerceAtLeast(0f))
            )
            drawFoundChristmasNorthStar(starCenter, minDim * 0.075f, starPhase, starPhase * 0.95f)
        }

        // Flanking ornaments — left/right margins, stay visible after the copy lands
        val ornamentPhase = smoothStep(((progress - 0.20f) / 0.32f).coerceIn(0f, 1f))
        val ornamentFade = 1f - smoothStep(((progress - 0.62f) / 0.28f).coerceIn(0f, 1f)) * 0.3f
        if (ornamentPhase > 0f) {
            val tints = listOf(Color(0xFFE0233D), Color(0xFF1FA34D), Color(0xFFFFD54F), Color(0xFF1565C0))
            listOf(
                Offset(w * 0.13f, center.y - minDim * 0.08f),
                Offset(w * 0.87f, center.y - minDim * 0.08f),
                Offset(w * 0.20f, h * 0.78f),
                Offset(w * 0.80f, h * 0.78f)
            ).forEachIndexed { i, pos ->
                drawFoundChristmasOrnamentGlow(
                    center = pos,
                    radius = minDim * if (i < 2) 0.08f else 0.06f,
                    tint = tints[i],
                    alpha = ornamentPhase * ornamentFade * if (i < 2) 0.9f else 0.75f
                )
            }
        }

        // Gift boxes — bottom corners, never under the Found copy
        val giftPhase = smoothStep(((progress - 0.26f) / 0.34f).coerceIn(0f, 1f))
        val giftFade = (1f - smoothStep(((progress - 0.62f) / 0.28f).coerceIn(0f, 1f)) * 0.45f)
        if (giftPhase > 0f && giftFade > 0.05f) {
            listOf(w * 0.22f, w * 0.78f).forEach { gx ->
                val giftCenter = Offset(gx, h * 0.86f)
                drawFoundChristmasGiftBox(giftCenter, minDim * 0.09f, giftPhase, giftPhase * giftFade)
            }
        }

        // Snowflake sigils — far flanks, outside the copy
        val sigilPhase = smoothStep(((progress - 0.30f) / 0.40f).coerceIn(0f, 1f))
        if (sigilPhase > 0f) {
            val sigilAlpha = sigilPhase * (1f - smoothStep(((progress - 0.62f) / 0.28f).coerceIn(0f, 1f)) * 0.25f)
            listOf(w * 0.12f to -progress * 40f, w * 0.88f to progress * 40f).forEach { (x, rot) ->
                val sigilCenter = Offset(x, center.y + minDim * 0.18f)
                for (arm in 0 until 6) {
                    drawFoundChristmasSnowflakeArm(sigilCenter, minDim * 0.055f, sigilAlpha * 0.8f, rot + arm * 60f)
                }
            }
        }

        // Gentle snowfall — two refined layers
        if (progress > 0.18f) {
            val snowPhase = ((progress - 0.18f) / 0.82f).coerceIn(0f, 1f)
            for (layer in 0 until 2) {
                val depth = 0.45f + layer * 0.35f
                val count = 18 + layer * 10
                for (s in 0 until count) {
                    val seed = s + layer * 60
                    val delay = (seed % 12) * 0.012f
                    val local = ((snowPhase - delay) / 0.88f).coerceIn(0f, 1f)
                    if (local <= 0f) continue
                    val sx = (seed * 21.1f + local * (16f + layer * 8f)) % w
                    val sy = h * 0.04f + local * h * 0.94f
                    val sway = sin(local * PI.toFloat() * (3.5f + layer) + seed) * minDim * 0.028f
                    val alpha = depth * (1f - local * 0.3f) * 0.85f
                    if (layer == 1 && s % 6 == 0) {
                        val fc = Offset(sx + sway, sy)
                        for (arm in 0 until 6) {
                            drawFoundChristmasSnowflakeArm(fc, minDim * 0.014f, alpha * 0.65f, arm * 60f + progress * 20f)
                        }
                    } else {
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = (1.2f + (s % 3)) * depth,
                            center = Offset(sx + sway, sy)
                        )
                    }
                }
            }
        }

        // Wreath honour ring — frames the copy from the outside, never grows through it
        if (progress in 0.20f..0.92f) {
            val wreathLocal = smoothStep(((progress - 0.20f) / 0.38f).coerceIn(0f, 1f))
            val wreathR = minDim * (0.46f + 0.12f * wreathLocal)
            val wreathAlpha = (0.55f - wreathLocal * 0.12f)
            drawCircle(
                color = Color(0xFF1FA34D).copy(alpha = wreathAlpha),
                radius = wreathR,
                center = center,
                style = Stroke(width = minDim * 0.024f)
            )
            for (berry in 0 until splashLoopCount(10)) {
                val ba = (berry * 36f + progress * 22f) * PI.toFloat() / 180f
                drawCircle(
                    color = Color(0xFFE0233D).copy(alpha = wreathAlpha * 1.35f),
                    radius = minDim * 0.012f,
                    center = center + Offset(cos(ba), sin(ba)) * wreathR
                )
            }
        }

        // Golden fairy dust — celebration tail
        if (progress > 0.48f) {
            val drift = ((progress - 0.48f) / 0.52f).coerceIn(0f, 1f)
            for (g in 0 until splashLoopCount(16)) {
                val gx = (g * 97f + drift * 32f) % w
                val gy = h * (0.2f + (g * 0.036f) % 0.52f) - drift * 12f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (1f - drift * 0.5f) * 0.55f),
                            Color(0xFFFFD54F).copy(alpha = (1f - drift * 0.5f) * 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(gx, gy),
                        radius = 3.5f
                    ),
                    radius = 3.5f,
                    center = Offset(gx, gy)
                )
            }
        }
    }
}

@Composable
fun FoundSplashStylePicker(
    selectedStyleId: String,
    onStyleSelected: (FoundSplashStyle) -> Unit,
    modifier: Modifier = Modifier,
    isLocked: (String) -> Boolean = { false },
    onLockedClick: () -> Unit = {},
    previewTitle: String = DEFAULT_FOUND_SPLASH_TITLE,
    previewSubtitle: String = DEFAULT_FOUND_SPLASH_SUBTITLE
) {
    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FoundSplashStyle.entries.forEach { style ->
                val normalizedSelection = FoundSplashStyle.fromId(selectedStyleId).id
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
                            if (style == FoundSplashStyle.NONE) {
                                // A real FoundSplashCard preview here would imply this option
                                // still shows *something* — the whole point of None is that
                                // nothing shows, so the preview needs to look like "nothing" too.
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = null,
                                    tint = SpotVaultColors.Muted.copy(alpha = 0.6f),
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                val previewProgress = style.previewProgress()
                                FoundSplashStyleEffect(
                                    style = style,
                                    progress = { previewProgress },
                                    modifier = Modifier.fillMaxSize()
                                )
                                FoundSplashCard(
                                    progress = { previewProgress },
                                    style = style,
                                    compact = true,
                                    title = previewTitle,
                                    subtitle = previewSubtitle,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .fillMaxWidth(0.88f)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selected) SpotVaultColors.Teal.copy(alpha = 0.14f)
                                    else SpotVaultColors.Elevated.copy(alpha = 0.6f)
                                )
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    style.label,
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
                    if (locked) {
                        PremiumLockBadge(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
                    }
                }
            }
        }
        TrailingScrollHint(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}
