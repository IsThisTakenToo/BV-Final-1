@file:OptIn(ExperimentalMaterial3WindowSizeClassApi::class)

package com.spotvault.app

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration

/** Maximum content width on medium/expanded screens (tablets, 600dp+). */
val TabletMaxContentWidth = 600.dp

/**
 * Bottom-nav scale for short-height phones. Exactly [1f] at 700dp+ screen height;
 * shrinks down to 0.75f on the shortest supported devices.
 */
fun shortScreenNavScale(screenHeightDp: Int): Float =
    (screenHeightDp / 700f).coerceIn(0.75f, 1f)

@Composable
fun rememberShortScreenNavScale(): Float =
    shortScreenNavScale(LocalConfiguration.current.screenHeightDp)

/** Shared orientation check — the app has no orientation lock (android:resizeableActivity="true"
 * in the manifest, no android:screenOrientation), so any screen can end up in landscape on a
 * phone, not just on tablets. A screen that only ever stacks content in a single vertical Column
 * squishes badly there (much less height, plenty of unused width), which is exactly what this
 * flags so a screen can branch to a side-by-side layout instead. Deliberately keyed off the raw
 * orientation, not width class — a narrow-but-landscape phone still has this problem, and
 * [LocalWindowSizeClass] alone wouldn't catch it. */
@Composable
fun isLandscapeOrientation(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

val LocalWindowSizeClass = staticCompositionLocalOf {
    WindowSizeClass.calculateFromSize(DpSize(360.dp, 640.dp))
}

fun WindowSizeClass.isCompactWidth(): Boolean =
    widthSizeClass == WindowWidthSizeClass.Compact

/** True whenever the window itself is short (<480dp), regardless of orientation — a compact
 * split-screen/multi-window slice, a small device, or a phone with the IME up all land here even
 * while nominally still "portrait". Computed from the real Activity window via
 * [calculateWindowSizeClass][androidx.compose.material3.windowsizeclass.calculateWindowSizeClass]
 * (see [LocalWindowSizeClass]), not just screenHeightDp, so it reacts to actual available space. */
@Composable
fun isCompactHeight(): Boolean =
    LocalWindowSizeClass.current.heightSizeClass == WindowHeightSizeClass.Compact

/** True whenever a screen risks the "header/filters/form controls push the main content off
 * screen with no way to scroll to it" trap — either the device is rotated to landscape (plenty
 * of width, not much height) or the window is short height-wise regardless of orientation. Any
 * screen with a header, filter row, or form above its main content should treat this as "wrap
 * the header in scroll" / "use the side-by-side layout" rather than reacting to orientation
 * alone — a short *portrait* window has exactly the same trapping risk landscape does.
 *
 * The landscape half of this is deliberately gated on ![isGenuineTablet] — a phone rotated to
 * landscape trades height for width and genuinely needs the compact/side-by-side treatment, but
 * a real tablet in landscape has plenty of *both* (by definition, since [isGenuineTablet] already
 * requires non-compact height). Without this exclusion, every one of this function's call sites
 * — the Vault list's header/feed split, the Snap/Pin save screen, the active-tracking photo
 * sizing, the Calendar day-results sheet, and others — squeezed itself into the same
 * "not much height" layout on a tablet held sideways as it does on a phone, even though the
 * tablet was never actually short on room. isCompactHeight() alone still catches a tablet in an
 * unusually short multi-window slice, since that's a real "not much height" case regardless of
 * device class. */
@Composable
fun needsCompactHeightLayout(): Boolean =
    (isLandscapeOrientation() && !isGenuineTablet()) || isCompactHeight()

/** A genuine tablet (or a phone in an unusually large multi-window slice) is wide AND tall.
 * A phone simply rotated to landscape is wide but short — Medium (or even Expanded) width class,
 * Compact height class. Capping/centering content the same way for both used to squeeze the
 * rotated-phone case into a narrow strip with big empty gutters on either side, on top of
 * starving anything laid out inside it (like the Vault's landscape two-column split) for real
 * width — the search bar collapsing into a near-square "oval" was that starvation made visible.
 * Requiring height to also be non-compact is what actually tells the two apart. */
@Composable
private fun isGenuineTablet(): Boolean =
    !LocalWindowSizeClass.current.isCompactWidth() && !isCompactHeight()

/**
 * Caps width at [TabletMaxContentWidth] on genuine tablets only — see [isGenuineTablet].
 * On phones (including one rotated to landscape) this is a no-op so layout stays pixel-identical.
 */
@Composable
fun Modifier.adaptiveMaxContentWidth(): Modifier {
    return if (isGenuineTablet()) {
        widthIn(max = TabletMaxContentWidth)
    } else {
        this
    }
}

/**
 * Centers content and caps its width on genuine tablets. On phones — including one rotated to
 * landscape, see [isGenuineTablet] — behaves as a plain [Box] using the full available width.
 */
@Composable
fun AdaptiveTabletContainer(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable BoxScope.() -> Unit
) {
    if (!isGenuineTablet()) {
        Box(modifier = modifier, content = content)
    } else {
        // Forcing fillMaxSize() here regardless of what the caller passed used to override a
        // caller that deliberately passed only fillMaxWidth() — the bottom nav bar being the one
        // real case (SpotVaultNavigation.kt), since a Scaffold's bottomBar slot has no bounded
        // height of its own. On any window ≥600dp wide (every tablet, and any phone simply
        // rotated to landscape — there's no orientation lock), that turned the nav bar into a
        // box filling the *entire* screen height, squeezing the actual page content behind it
        // down to nothing. Every other call site already passes fillMaxSize() itself, so this
        // was always redundant for them and only actively wrong for the one that didn't.
        Box(
            modifier = modifier,
            contentAlignment = contentAlignment
        ) {
            Box(
                modifier = Modifier
                    .adaptiveMaxContentWidth()
                    .fillMaxWidth(),
                content = content
            )
        }
    }
}
