@file:OptIn(ExperimentalMaterial3WindowSizeClassApi::class)

package com.spotvault.app

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.window.layout.FoldingFeature

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

/** Current hinge state of the window this Activity is hosted in, from Jetpack WindowManager's
 * WindowInfoTracker (see MainActivity's setContent) — null on every device without a fold (the
 * overwhelming majority of phones/tablets) or whenever a foldable simply has no hinge feature to
 * report right now. [WindowSizeClass] above only ever answers "how much space is there," which is
 * exactly the wrong question for a foldable specifically — it says nothing about *where* a
 * physical seam sits inside that space, which is the one thing genuinely unique to fold-aware
 * layout versus "any large screen." */
val LocalFoldingFeature = androidx.compose.runtime.staticCompositionLocalOf<androidx.window.layout.FoldingFeature?> { null }

/** True only for a hinge that's actually separating the window into two areas right now (a book/
 * tabletop posture) — [FoldingFeature.isSeparating] is false for a device that HAS a hinge but is
 * currently fully flat/open, which behaves like an ordinary large screen and needs no special
 * handling here. Layouts that would otherwise place something important (a button, a text field)
 * spanning straight across the hinge should branch on this rather than ignore it. */
@Composable
fun hasOccludingFold(): Boolean = LocalFoldingFeature.current?.isSeparating == true

/** True for the "half-open laptop" posture specifically — a horizontal hinge with the device held
 * like a tiny laptop or propped up on a table (top half acting as a display, bottom half a base).
 * Content that assumes full-height availability (e.g. a bottom sheet, a full-screen dialog) can
 * use this to avoid stacking primary content into the bottom half, which is often physically flat
 * on a table and awkward to view at typical holding angles. */
@Composable
fun isTabletopPosture(): Boolean {
    val feature = LocalFoldingFeature.current ?: return false
    return feature.isSeparating &&
        feature.orientation == androidx.window.layout.FoldingFeature.Orientation.HORIZONTAL &&
        feature.state == androidx.window.layout.FoldingFeature.State.HALF_OPENED
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
 * Requiring height to also be non-compact is what actually tells the two apart.
 *
 * Not private — this is also the single source of truth for "should navigation switch from the
 * bottom bar to a side rail" (SpotVaultMainScaffold). Deliberately reusing the exact same check
 * that already decides content-width capping rather than introducing a second, differently-tuned
 * width threshold: those two decisions disagreeing right around the Medium/Expanded boundary
 * would look janky (e.g. content still capped to a narrow centered column while navigation had
 * already switched to a side rail sized for a much wider layout, or vice versa).
 *
 * NOT the right check for "should a screen split into two side-by-side panes," despite an earlier
 * version of this doc comment claiming it would be — see [isWideEnoughForTwoPane] for why that
 * turned out to need a materially different, higher threshold instead of reusing this one. */
@Composable
fun isGenuineTablet(): Boolean =
    !LocalWindowSizeClass.current.isCompactWidth() && !isCompactHeight()

/** True only once the window is wide enough that an actual two-pane split (a fixed-width list
 * pane plus a detail pane taking the remainder) leaves *both* panes a reasonable width — not just
 * "wide enough to cap a single centered column," which is all [isGenuineTablet] actually checks.
 * [isGenuineTablet] goes true starting at Medium width (600dp), and a two-pane layout carved out
 * of a 600dp window would squeeze its list pane down toward ~250dp — well under what a search bar
 * plus filter chips plus spot cards need to render correctly (this app has already hit exactly
 * that failure mode once: see [isGenuineTablet]'s own doc on the "search bar collapsing into a
 * near-square oval" bug). Requiring Expanded width (840dp+) instead means even a fixed ~380dp list
 * pane still leaves the detail pane a genuinely comfortable width, not just "technically nonzero."
 * A window between 600dp and 840dp (real Medium-width tablets, e.g. a 7-8" tablet in portrait)
 * still gets [isGenuineTablet]'s existing single-pane, width-capped treatment instead — that's
 * already proven correct at that width; forcing a premature split into it is the actual bug this
 * function exists to avoid, not a screen size this app is otherwise unable to handle. */
@Composable
fun isWideEnoughForTwoPane(): Boolean =
    LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Expanded && !isCompactHeight()

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
 * [wide] skips the cap for a caller building its own two-pane split inside.
 */
@Composable
fun AdaptiveTabletContainer(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopCenter,
    // False for every existing caller — a single centered, capped column is the right shape for
    // all of them. The one caller that opts in (TimerSelectionDialog's Snap/Pin save screen, on
    // a wide enough window) puts a genuine two-pane split — photo/pin preview beside the form —
    // inside this container instead, which needs the full window width to be worth building at
    // all; capping it to 600dp first would squeeze that split right back down to the cramped
    // layout it exists to replace. Same reasoning, same parameter name, as VaultOverlayDialog's
    // own wide escape hatch.
    wide: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    if (!isGenuineTablet()) {
        Box(modifier = modifier, content = content)
    } else {
        // Earlier revision shifted TopCenter to TopStart here whenever a vertical separating
        // hinge was detected, on the theory that a centered card would straddle the physical
        // seam. Reverted after actually looking at it on an unfolded foldable emulator: this
        // display is one continuous surface with no real gap at the crease, so a centered card
        // spanning it reads completely normally — exactly like the Vault's own two-pane layout,
        // which never did any hinge-avoidance at all and spans the same crease with no issue.
        // What TopStart actually produced was worse: a narrow capped card pinned to one edge
        // with roughly half the unfolded width sitting empty, which reads as broken rather than
        // intentional. Simple, unconditional centering is the correct default here.
        //
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
            // fillMaxSize(), not fillMaxWidth() alone — a caller whose own content relies on a
            // real bounded height (weight(1f) regions, a fixed bottom bar pinned to the true
            // screen edge — see TimerSelectionDialog) got no height at all from this Box on the
            // tablet branch, so its own fillMaxSize()/weight(1f) chain had nothing to measure
            // against and fell back to wrapping its natural content height instead — the whole
            // screen shrink-wrapped to the top, leaving the rest of a tall window as dead space
            // no matter what that caller's own internal layout did. Content that doesn't care
            // about height (the common case this container was built for) renders identically
            // either way, since a Box's un-arranged children already position themselves within
            // whatever bounds they're given.
            Box(
                modifier = Modifier
                    .let { if (wide) it else it.adaptiveMaxContentWidth() }
                    .fillMaxSize(),
                content = content
            )
        }
    }
}

/** The list pane's target width and the gap left before the detail pane, before accounting for
 * where a foldable's hinge actually is — see [TwoPaneRow]'s own doc. Every two-pane screen in
 * this app used exactly these two numbers with no hinge awareness at all before this existed. */
private val DefaultTwoPaneListWidth = 380.dp
private val DefaultTwoPaneGutter = 32.dp
/** Preferred floor for the list pane's width, applied only when there's enough room before the
 * hinge to honor it without crossing into the hinge/gutter zone — see [TwoPaneRow]'s own doc for
 * why this can never win over hinge clearance. Exists so a hinge with generous room ahead of it
 * still gives the list pane something reasonable to work with (search bar, filter chips, cards)
 * rather than an arbitrarily thin sliver. */
private val MinTwoPaneListWidth = 280.dp
/** Ceiling on how far [TwoPaneRow] will widen the list pane to hug a hinge that sits further out
 * than [DefaultTwoPaneListWidth] — a spot list or settings category list doesn't get more useful
 * past a certain width, just more empty padding inside its own cards/rows, so there's no reason
 * to hand it the entire "before the hinge" region on an unusually wide unfolded window. */
private val MaxTwoPaneListWidth = 520.dp
/** Clearance kept between the gutter's own edges and the hinge's reported bounds — the hinge
 * needs room to spare inside the gutter, not just to land exactly on one edge of it. */
private val HingeSafetyMargin = 16.dp

/** True whenever a foldable's hinge is currently reported running left-to-right instead of the
 * usual book-style top-to-bottom — the device rotated 90° from the hold [TwoPaneRow]'s own doc
 * describes, so the physical crease now cuts across the window horizontally instead of vertically.
 * [TwoPaneRow] already reads this internally to switch from its usual left/right split to a
 * top/bottom one; exposed here too for the handful of callers that impose their own fixed
 * height on a [TwoPaneRow] sized for the left/right case (two panes sharing one row's height) and
 * need to relax it for a stacked pair instead. */
@Composable
fun isHorizontalHinge(): Boolean =
    LocalFoldingFeature.current?.orientation == FoldingFeature.Orientation.HORIZONTAL

/** Shared by [TwoPaneRow]'s left/right and top/bottom cases — same hinge-hugging math either way,
 * just fed the row's own X position and the hinge's left/right bounds for one axis, or the
 * column's own Y position and the hinge's top/bottom bounds for the other. */
private fun hingeSplit(
    positionStart: Float,
    hingeStartPx: Int,
    hingeEndPx: Int,
    density: androidx.compose.ui.unit.Density
): Pair<Dp, Dp> {
    val hingeStartDp = with(density) { (hingeStartPx - positionStart).toDp() }
    val hingeEndDp = with(density) { (hingeEndPx - positionStart).toDp() }
    // Hugs wherever the hinge actually sits rather than treating preferredSize as a ceiling —
    // capped so it can't hand the first pane the whole window for no reason on an unusually
    // placed hinge. Deliberately NOT floored at MinTwoPaneListWidth here: the caller's own
    // position (from onGloballyPositioned) already reflects everything ahead of it, including a
    // nav rail some callers sit behind — when that eats enough space that less than
    // MinTwoPaneListWidth remains before the hinge, forcing the floor would push the first pane's
    // own edge past the hinge and into the gutter, exactly the crossing this whole mechanism
    // exists to prevent. Clearing the hinge always wins over hitting the preferred minimum; a
    // pane that merely looks cramped beats one that visually crosses the physical crease.
    val availableBeforeHinge = (hingeStartDp - HingeSafetyMargin).coerceAtLeast(0.dp)
    val firstPaneSize = if (availableBeforeHinge < MinTwoPaneListWidth) {
        availableBeforeHinge
    } else {
        availableBeforeHinge.coerceAtMost(MaxTwoPaneListWidth)
    }
    val gutterEnd = maxOf(firstPaneSize + DefaultTwoPaneGutter, hingeEndDp + HingeSafetyMargin)
    return firstPaneSize to (gutterEnd - firstPaneSize).coerceAtLeast(DefaultTwoPaneGutter)
}

/**
 * Drop-in replacement for `Row { Box(width) { list }; Box(weight(1f)) { detail } }`, the shape
 * every two-pane screen in this app (Vault, Favorites Hub, Location Browser, Calendar Day
 * Results, the Snap/Pin save screen, Settings) used — except the gap between panes actually
 * clears a foldable's physical hinge instead of guessing a fixed padding value and hoping it
 * happens to land in the right place.
 *
 * Why a flat padding guess isn't good enough: a book-style unfolded foldable's hinge sits, in
 * practice, uncomfortably close to a fixed ~380dp list-pane width — not a coincidence, since
 * both numbers are describing roughly the same thing (a comfortable list width happens to land
 * near where a symmetric foldable puts its seam). Padding that fixed boundary by a flat amount
 * narrows the gap between "the hinge" and "where the padding starts," but doesn't guarantee the
 * hinge — whose real on-screen position varies by device and window width — actually ends up
 * inside it. This reads the hinge's own reported bounds instead (from Jetpack WindowManager, via
 * [LocalFoldingFeature]) and sizes the list pane to the space actually available before the hinge
 * (capped at [MaxTwoPaneListWidth], and floored at [MinTwoPaneListWidth] only when that floor
 * still leaves the hinge clear — never the other way around, since a pane that merely looks
 * cramped beats one that visually crosses the physical crease) rather than treating
 * [preferredListWidth] as a ceiling it can shrink from but never grow past — an earlier version
 * did exactly that, and on a hinge sitting past the preferred width it meant the list pane still
 * capped out at the same fixed number while the detail pane silently absorbed every bit of the
 * space the list pane could have used instead, an asymmetry with no real reason behind it once
 * the hinge is the thing actually deciding where the seam between panes belongs. Hugging the
 * hinge this way also means an unfolded foldable — whose seam usually sits close to the window's
 * own center — naturally lands close to two evenly matched panes, not a narrow fixed sidebar
 * beside an oversized remainder. The gutter itself still widens past [DefaultTwoPaneGutter]
 * whenever needed so the hinge sits inside it with [HingeSafetyMargin] to spare on both sides.
 * With no hinge reported at all (every tablet, and any foldable not currently separating) this
 * behaves exactly as before hinge awareness existed: a fixed [DefaultTwoPaneListWidth] list pane
 * and a fixed [DefaultTwoPaneGutter] gap.
 *
 * Needs this Row's own on-screen position, not just the hinge's — [FoldingFeature.bounds] is
 * reported in the Activity window's coordinate space, and every one of this app's two-pane
 * screens sits behind at least one layer of status-bar/inset padding before reaching this Row,
 * so the hinge's raw bounds can't be compared to local coordinates directly. [onGloballyPositioned]
 * resolves that generically regardless of how much padding/nesting sits above this Row on any
 * given screen, rather than needing each caller to track its own insets by hand.
 *
 * Renders as a Column instead of a Row — [listPane] on top, [detailPane] below, split at the
 * same hinge-hugging math applied to the vertical axis instead — whenever [isHorizontalHinge] is
 * true: a book-style foldable rotated 90° from its usual hold has its hinge running left-to-right,
 * and a left/right split at that point isn't just unaligned with the crease, it's split on the
 * wrong axis entirely. Every caller gets this for free without its own orientation branch; the
 * handful whose own modifier imposes a fixed height sized for the left/right case (both panes
 * sharing one row's height) check [isHorizontalHinge] themselves to relax it for a stacked pair.
 */
@Composable
fun TwoPaneRow(
    modifier: Modifier = Modifier,
    preferredListWidth: Dp = DefaultTwoPaneListWidth,
    listPane: @Composable BoxScope.() -> Unit,
    detailPane: @Composable BoxScope.() -> Unit
) {
    val foldingFeature = LocalFoldingFeature.current
    val density = LocalDensity.current

    // Deliberately NOT gated on hinge.isSeparating — confirmed via an on-screen debug overlay
    // that on a foldable sitting fully flat/unfolded (the normal, expected posture for actually
    // using an app, as opposed to propped open at an angle like a book or tent), isSeparating is
    // false even though the hinge's bounds are still reported and still accurate. isSeparating
    // describes whether Android treats the display as two functionally distinct logical areas,
    // which is genuinely false when flat — but that's not what this composable cares about.
    // There's still a real, visible crease on a flat unfolded foldable, and that's the thing
    // being avoided here, so bounds and orientation alone are the right gate; requiring
    // isSeparating too meant this whole mechanism silently never activated in the single most
    // common way people actually hold a foldable.
    if (foldingFeature?.orientation == FoldingFeature.Orientation.HORIZONTAL) {
        var columnPositionInWindow by remember { mutableStateOf<Offset?>(null) }
        val (topHeight, gutter) = remember(columnPositionInWindow, foldingFeature, preferredListWidth, density) {
            val position = columnPositionInWindow
            if (position == null) {
                preferredListWidth to DefaultTwoPaneGutter
            } else {
                hingeSplit(position.y, foldingFeature.bounds.top, foldingFeature.bounds.bottom, density)
            }
        }
        Column(modifier = modifier.onGloballyPositioned { columnPositionInWindow = it.positionInWindow() }) {
            Box(modifier = Modifier.height(topHeight).fillMaxWidth()) { listPane() }
            Spacer(modifier = Modifier.height(gutter).fillMaxWidth())
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) { detailPane() }
        }
        return
    }

    var rowPositionInWindow by remember { mutableStateOf<Offset?>(null) }

    val (listWidth, gutter) = remember(rowPositionInWindow, foldingFeature, preferredListWidth, density) {
        val position = rowPositionInWindow
        val hinge = foldingFeature
        if (position == null || hinge == null ||
            hinge.orientation != FoldingFeature.Orientation.VERTICAL
        ) {
            preferredListWidth to DefaultTwoPaneGutter
        } else {
            hingeSplit(position.x, hinge.bounds.left, hinge.bounds.right, density)
        }
    }

    Row(modifier = modifier.onGloballyPositioned { rowPositionInWindow = it.positionInWindow() }) {
        Box(modifier = Modifier.width(listWidth).fillMaxHeight()) { listPane() }
        Spacer(modifier = Modifier.width(gutter).fillMaxHeight())
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) { detailPane() }
    }
}

/**
 * Like `Box(contentAlignment = Alignment.Center)`, except when a horizontal-orientation hinge
 * currently intersects this box's own measured bounds — a clamshell/flip phone unfolded (Galaxy Z
 * Flip, Razr, that shape), not a book-style fold wide enough for [isWideEnoughForTwoPane]'s own
 * two-pane treatment, still has a real physical crease running left-to-right across the middle of
 * an otherwise completely ordinary single-column phone layout. Plain centering does the one thing
 * guaranteed to be wrong there: it aims the content's own vertical middle exactly at the hinge.
 * This favors whichever side of the hinge — above or below — has more room instead, so a status
 * card, an icon-and-headline block, or a primary button lands clear of the crease rather than
 * straddling it.
 *
 * Falls back to plain centering whenever there's no hinge, the hinge isn't horizontal (a
 * book-style fold's own vertical hinge is [TwoPaneRow]'s concern, not this composable's — a
 * single-column phone layout was never going to be split into two panes over a fold that narrow
 * to begin with), or the hinge's reported bounds don't actually fall inside this box's own
 * on-screen position at all (this same composable is used by screens that also render at
 * tablet/genuine-two-pane widths sometimes, where a width-capped centered column can easily sit
 * entirely to one side of a vertical hinge with nothing to avoid).
 *
 * Doesn't try to know whether [content] actually fits above or below — favoring the roomier side
 * is a real improvement over centering (which always aims for the worst spot) without needing a
 * second measurement pass to find out exactly how tall [content] itself is.
 */
@Composable
fun HingeAvoidingCenterBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var boxPositionInWindow by remember { mutableStateOf<Offset?>(null) }
    var boxHeightPx by remember { mutableStateOf(0) }
    val hinge = LocalFoldingFeature.current
    val density = LocalDensity.current

    val alignment = remember(boxPositionInWindow, boxHeightPx, hinge, density) {
        val position = boxPositionInWindow
        if (position == null || hinge == null || hinge.orientation != FoldingFeature.Orientation.HORIZONTAL) {
            Alignment.Center
        } else {
            val hingeTopDp = with(density) { (hinge.bounds.top - position.y).toDp() }
            val hingeBottomDp = with(density) { (hinge.bounds.bottom - position.y).toDp() }
            val boxHeightDp = with(density) { boxHeightPx.toDp() }
            if (hingeBottomDp <= 0.dp || hingeTopDp >= boxHeightDp) {
                // Hinge falls entirely outside this box's own bounds — nothing to avoid.
                Alignment.Center
            } else {
                val spaceAbove = hingeTopDp.coerceAtLeast(0.dp)
                val spaceBelow = (boxHeightDp - hingeBottomDp).coerceAtLeast(0.dp)
                if (spaceAbove >= spaceBelow) Alignment.TopCenter else Alignment.BottomCenter
            }
        }
    }

    Box(
        modifier = modifier.onGloballyPositioned {
            boxPositionInWindow = it.positionInWindow()
            boxHeightPx = it.size.height
        },
        contentAlignment = alignment,
        content = content
    )
}
