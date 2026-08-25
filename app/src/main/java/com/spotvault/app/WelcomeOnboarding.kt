package com.spotvault.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class WelcomeSlide(val imageRes: Int, val title: String, val body: String)

sealed interface DriveOnboardingState {
    data object Idle : DriveOnboardingState
    data object Working : DriveOnboardingState
    data class Connected(val email: String, val restoredCount: Int?) : DriveOnboardingState
    /** A backup already exists in this Google account and this device also already has local
     * spots of its own — see [DriveSyncManager.ConnectOutcome.ConflictFound]. The email/token are
     * carried here so the composable can hand them straight back to
     * [DriveSyncManager.resolveConflict] once the user picks a side. */
    data class NeedsConflictResolution(val email: String, val accessToken: String) : DriveOnboardingState
    data class Failed(val message: String) : DriveOnboardingState
}

val WelcomeSlides = listOf(
    WelcomeSlide(
        R.drawable.onboard_snap_pin,
        "Snap & Pin",
        "Snap a photo of where you parked, or drop a Pin with just your GPS — either way, DropPin Vault saves the exact spot to your Vault, ready to navigate back to anytime."
    ),
    WelcomeSlide(
        R.drawable.onboard_quick_actions,
        "Quick Pins & Widgets",
        "Save your spot instantly. Use Quick Track for a live parking notification, or add the home-screen widget to drop a pin without even opening the app."
    ),
    WelcomeSlide(
        R.drawable.onboard_compass,
        "Find Your Way Back",
        "Navigate straight back to anything saved in your Vault with compass guidance or your own maps app — free, always."
    )
)

private enum class OnboardingStep { WELCOME, DRIVE }

@Composable
fun WelcomeOnboardingScreen(
    driveState: DriveOnboardingState,
    onConnectDrive: () -> Unit,
    onResolveConflict: (DriveSyncManager.ConflictChoice) -> Unit,
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { WelcomeSlides.size })
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(OnboardingStep.WELCOME) }
    // This is the very first screen a new user sees, and on an unfolded foldable it's also the
    // very first thing that tells them whether this app was actually built with their device in
    // mind — see isWideEnoughForTwoPane's own doc for why this is a materially higher bar than
    // "is this a tablet."
    val isWide = isWideEnoughForTwoPane()

    val density = LocalDensity.current
    val statusPad = with(density) { SystemBarInsets.statusBarPx.toDp() }
    val navPad = with(density) { SystemBarInsets.navigationBarPx.toDp() } + 16.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SpotVaultColors.Void, SpotVaultColors.Deep, SpotVaultColors.Surface)))
    ) {
        // wide = isWide: the slide image and the Drive-connect icon both get their own dedicated
        // showcase pane on a window this size — AdaptiveTabletContainer's usual 600dp cap would
        // squeeze that split right back into the narrow phone-style column it exists to replace,
        // the same reasoning the home screen's own AdaptiveTabletContainer call already uses.
        AdaptiveTabletContainer(modifier = Modifier.fillMaxSize(), wide = isWide) {
        when (step) {
            OnboardingStep.DRIVE -> {
                DriveConnectStep(
                    statusPad = statusPad,
                    navPad = navPad,
                    driveState = driveState,
                    onConnectDrive = onConnectDrive,
                    onResolveConflict = onResolveConflict,
                    onFinish = onFinish
                )
            }
            OnboardingStep.WELCOME -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .padding(top = statusPad + 16.dp, bottom = navPad)
                ) {
                    TextButton(onClick = { step = OnboardingStep.DRIVE }, modifier = Modifier.align(Alignment.End)) {
                        Text("Skip", color = SpotVaultColors.Muted)
                    }
                    // Shared between the wide detail pane below and the narrow layout further
                    // down — both need the exact same label/action, just placed differently.
                    val nextLabel = if (pagerState.currentPage < WelcomeSlides.lastIndex) "Next" else "Get Started"
                    val onNextClick: () -> Unit = {
                        if (pagerState.currentPage < WelcomeSlides.lastIndex) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            step = OnboardingStep.DRIVE
                        }
                    }
                    val dotsRow: @Composable () -> Unit = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalArrangement = if (isWide) Arrangement.Start else Arrangement.Center
                        ) {
                            repeat(WelcomeSlides.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                                        .background(
                                            if (pagerState.currentPage == index) SpotVaultColors.Teal else SpotVaultColors.Muted.copy(alpha = 0.4f),
                                            androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                            }
                        }
                    }
                    HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                        val slide = WelcomeSlides[page]
                        if (isWide) {
                            // A showcase pane beside the story instead of a small image stacked
                            // above a mostly-empty stretched column — the same "two peers either
                            // side of the hinge" shape the home screen's Snap/Pin split already
                            // established, applied to the first screen a new user on this size
                            // device ever sees. TwoPaneRow, not a plain Row, so the gap between
                            // them still clears the physical hinge.
                            TwoPaneRow(
                                modifier = Modifier.fillMaxSize(),
                                listPane = {
                                    Image(
                                        painter = painterResource(id = slide.imageRes),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                    )
                                },
                                detailPane = {
                                    // Dots and Next/Get Started both live in this pane now, not as
                                    // a separate full-width row below the whole pager — a fixed
                                    // action button spanning the entire wide row put its own label
                                    // right on top of the physical hinge, the same crossing this
                                    // whole mechanism otherwise exists to avoid. Confined to this
                                    // pane instead, matching exactly how the Drive-connect step
                                    // right after this one already keeps its own button.
                                    Column(
                                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(slide.title, color = SpotVaultColors.OnSurface, fontWeight = FontWeight.Black, fontSize = 34.sp)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            slide.body,
                                            color = SpotVaultColors.Muted,
                                            fontSize = 17.sp,
                                            lineHeight = 25.sp
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        dotsRow()
                                        SpotVaultButton(
                                            onClick = onNextClick,
                                            modifier = Modifier.fillMaxWidth().height(56.dp)
                                        ) {
                                            Text(nextLabel, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            )
                        } else {
                            // A phone rotated to landscape (this app has no orientation lock) is
                            // exactly needsCompactHeightLayout()'s case: plenty of width, not much
                            // height. Center + a scrollable Column taller than its own viewport is
                            // its own trap distinct from the overflow this was originally guarding
                            // against — Compose still lays the content out centered within its full
                            // (oversized) height first, so the scroll starts partway down into it
                            // instead of at the top, and the very first thing a new user sees is a
                            // cropped mid-image with the title/body scrolled off in both directions.
                            // Top avoids that entirely by starting the scroll where the content
                            // actually starts; a shorter image cap in the same case means the
                            // common landscape-phone size needs little or no scrolling to begin with.
                            val compactHeight = needsCompactHeightLayout()
                            Column(
                                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = if (compactHeight) Arrangement.Top else Arrangement.Center
                            ) {
                                Image(
                                    painter = painterResource(id = slide.imageRes),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth(0.78f)
                                        .heightIn(max = if (compactHeight) 90.dp else 220.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                                Spacer(modifier = Modifier.height(if (compactHeight) 10.dp else 32.dp))
                                Text(
                                    slide.title,
                                    color = SpotVaultColors.OnSurface,
                                    fontWeight = FontWeight.Black,
                                    fontSize = if (compactHeight) 20.sp else 26.sp
                                )
                                Spacer(modifier = Modifier.height(if (compactHeight) 6.dp else 12.dp))
                                Text(
                                    slide.body,
                                    color = SpotVaultColors.Muted,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    // Landscape phones have room to spare width-wise, not
                                    // height-wise — capping lines here (with the scroll fallback
                                    // still there for anyone who wants the rest) guarantees this
                                    // fits the same short viewport the image/title were just
                                    // shrunk for, instead of gambling on wherever this particular
                                    // slide's copy happens to wrap at whatever width is available.
                                    maxLines = if (compactHeight) 1 else Int.MAX_VALUE,
                                    overflow = if (compactHeight) androidx.compose.ui.text.style.TextOverflow.Ellipsis else androidx.compose.ui.text.style.TextOverflow.Clip,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                    // Wide already rendered its own dots + button inside the pager's detail pane
                    // above (see the comment there) — this copy is the narrow/phone layout only.
                    if (!isWide) {
                        dotsRow()
                        SpotVaultButton(
                            onClick = onNextClick,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text(nextLabel, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        } // close AdaptiveTabletContainer content
    }
}

/** The frictionless-default backup step: connecting here needs nothing but a Google sign-in —
 * no folder to pick, unlike the manual SAF-based backup still available for power users under
 * Settings → Data. Skippable at every stage since this is meant to help people who'd never dig
 * through Settings on their own, not to gate the app behind an account. */
@Composable
private fun DriveConnectStep(
    statusPad: androidx.compose.ui.unit.Dp,
    navPad: androidx.compose.ui.unit.Dp,
    driveState: DriveOnboardingState,
    onConnectDrive: () -> Unit,
    onResolveConflict: (DriveSyncManager.ConflictChoice) -> Unit,
    onFinish: () -> Unit
) {
    val isWide = isWideEnoughForTwoPane()
    val icon = when (driveState) {
        is DriveOnboardingState.Connected -> Icons.Default.CloudDone
        is DriveOnboardingState.Failed -> Icons.Default.CloudOff
        is DriveOnboardingState.NeedsConflictResolution -> Icons.Default.CloudSync
        else -> Icons.Default.CloudUpload
    }
    val body = when (driveState) {
        is DriveOnboardingState.Idle -> "If you lose your phone, you lose your spots. Connect Google Drive for automatic, private background backups."
        is DriveOnboardingState.Working -> "Just a moment…"
        is DriveOnboardingState.Connected -> {
            val restored = driveState.restoredCount
            if (restored != null) "Connected as ${driveState.email}. Found a previous backup and restored $restored spot${if (restored == 1) "" else "s"}."
            else "Connected as ${driveState.email}. No previous backup found — you're all set, backups start now."
        }
        is DriveOnboardingState.NeedsConflictResolution ->
            "Connected as ${driveState.email}. This account already has a backup, and this device already has spots saved too — pick which one to keep."
        is DriveOnboardingState.Failed -> driveState.message
    }
    // iconSize/boxSize are parameters, not fixed — the wide branch below gives this its own much
    // larger dedicated pane, and a 96dp circle scaled for a narrow phone column would look tiny
    // and lost rather than intentionally sized for the space.
    val iconContent: @Composable (boxSize: androidx.compose.ui.unit.Dp, iconSize: androidx.compose.ui.unit.Dp) -> Unit = { boxSize, iconSize ->
        Box(
            modifier = Modifier
                .size(boxSize)
                .background(SpotVaultColors.Teal.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (driveState is DriveOnboardingState.Working) {
                CircularProgressIndicator(color = SpotVaultColors.Teal)
            } else {
                Icon(icon, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(iconSize))
            }
        }
    }
    val buttonsContent: @Composable ColumnScope.() -> Unit = {
        when (driveState) {
            is DriveOnboardingState.Connected -> {
                SpotVaultButton(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Continue", fontWeight = FontWeight.Bold)
                }
            }
            is DriveOnboardingState.Working, is DriveOnboardingState.NeedsConflictResolution -> Unit
            else -> {
                SpotVaultButton(onClick = onConnectDrive, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(if (driveState is DriveOnboardingState.Failed) "Try Again" else "Connect Google Drive", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onFinish, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Skip for now", color = SpotVaultColors.Muted)
                }
            }
        }
        // This step is the last screen before onFinish() hands off into the app on every path
        // (Connected/Idle/Failed all render one of the buttons above, wide or narrow) — one place
        // to disclose this once, instead of the persistent Vault-tab disclaimer this replaced.
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "By using DropPin Vault, you agree to our Terms of Service & Privacy Policy",
            color = SpotVaultColors.Muted.copy(alpha = 0.7f),
            fontSize = 11.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )
    }

    if (isWide) {
        // Same "showcase pane beside the message" shape as the Welcome slides right before this
        // step — a big reassuring icon on one side, the actual status/action on the other, rather
        // than the same small icon-over-centered-text column phones use just floating in the
        // middle of a much wider window.
        TwoPaneRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = statusPad + 16.dp, bottom = navPad),
            listPane = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    iconContent(180.dp, 84.dp)
                }
            },
            detailPane = {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Never Lose Your Vault", color = SpotVaultColors.OnSurface, fontWeight = FontWeight.Black, fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(body, color = SpotVaultColors.Muted, fontSize = 16.sp, lineHeight = 23.sp)
                    Spacer(modifier = Modifier.height(28.dp))
                    buttonsContent()
                }
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = statusPad + 16.dp, bottom = navPad),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Same landscape-phone case as the Welcome slides right before this step (see that
            // branch's own doc) — plenty of width, not much height. The Box below doesn't clip its
            // own child, so on a short window the un-shrunk icon+title+body stack simply rendered
            // past this Box's bounds and visually overlapped buttonsContent() drawn right after it,
            // rather than scrolling cleanly out of the way — scroll only helps content that's
            // still confined to its own bounds to begin with.
            val compactHeight = needsCompactHeightLayout()
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    // A Drive-connect failure surfaces the raw exception/OAuth error message here
                    // (arbitrary length, not something this screen controls), and the Idle
                    // description is already fairly long on its own — without scroll, content taller
                    // than this weight(1f) Box isn't clipped, it overflows into the button below it,
                    // making the actual retry/skip action hard to tap right when someone needs it most.
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    iconContent(if (compactHeight) 64.dp else 96.dp, if (compactHeight) 32.dp else 48.dp)
                    Spacer(modifier = Modifier.height(if (compactHeight) 12.dp else 32.dp))
                    Text(
                        "Never Lose Your Vault",
                        color = SpotVaultColors.OnSurface,
                        fontWeight = FontWeight.Black,
                        fontSize = if (compactHeight) 20.sp else 26.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(if (compactHeight) 6.dp else 16.dp))
                    Text(
                        body,
                        color = SpotVaultColors.Muted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        // Same reasoning as the Welcome slides' own body text: guarantee this fits
                        // the shrunk viewport rather than gambling on where it happens to wrap —
                        // the failure/conflict messages capped here are still fully reachable by
                        // scrolling, this only bounds the common Idle/Connected case.
                        maxLines = if (compactHeight) 2 else Int.MAX_VALUE,
                        overflow = if (compactHeight) androidx.compose.ui.text.style.TextOverflow.Ellipsis else androidx.compose.ui.text.style.TextOverflow.Clip,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            buttonsContent()
        }
    }

    if (driveState is DriveOnboardingState.NeedsConflictResolution) {
        DriveConflictDialog(onResolveConflict = onResolveConflict)
    }
}

/** Neither side of this choice can be made for the user — see
 * [DriveSyncManager.ConnectOutcome.ConflictFound] for why. No dismiss-without-choosing: leaving
 * this device connected in limbo (neither backed up nor restored) is worse than forcing a pick,
 * and both options here are safe in the sense that nothing is lost that wasn't already only on
 * one side of the choice. */
@Composable
private fun DriveConflictDialog(onResolveConflict: (DriveSyncManager.ConflictChoice) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = SpotVaultColors.Surface,
        titleContentColor = SpotVaultColors.OnSurface,
        textContentColor = SpotVaultColors.Muted,
        title = { Text("Which backup do you want to keep?") },
        text = {
            Text("This Google account already has a DropPin Vault backup, and this device already has spots saved locally too. Restoring will replace what's on this device with the Drive backup. Overwriting will replace the Drive backup with what's on this device.")
        },
        confirmButton = {
            SpotVaultButton(onClick = { onResolveConflict(DriveSyncManager.ConflictChoice.RESTORE_FROM_DRIVE) }) {
                Text("Restore from Drive", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { onResolveConflict(DriveSyncManager.ConflictChoice.OVERWRITE_DRIVE_BACKUP) }) {
                Text("Overwrite Backup", color = SpotVaultColors.Danger)
            }
        }
    )
}
