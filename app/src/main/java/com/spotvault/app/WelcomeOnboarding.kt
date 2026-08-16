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
        "Quick Pin saves your spot instantly, no dialog — perfect on the move. Quick Track does the same but keeps a live notification until you tap Found. Add the home-screen widget for one-tap access without even opening the app."
    ),
    WelcomeSlide(
        R.drawable.onboard_compass,
        "Find Your Way Back",
        "Navigate straight back to anything saved in your Vault with compass guidance or your own maps app — free, always."
    )
)

private enum class OnboardingStep { WELCOME, PERMISSIONS, DRIVE }

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

    val density = LocalDensity.current
    val statusPad = with(density) { SystemBarInsets.statusBarPx.toDp() }
    val navPad = with(density) { SystemBarInsets.navigationBarPx.toDp() } + 16.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SpotVaultColors.Void, SpotVaultColors.Deep, SpotVaultColors.Surface)))
    ) {
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
            OnboardingStep.PERMISSIONS -> {
                PermissionsPrimerStep(
                    statusPad = statusPad,
                    navPad = navPad,
                    onContinue = { step = OnboardingStep.DRIVE }
                )
            }
            OnboardingStep.WELCOME -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .padding(top = statusPad + 16.dp, bottom = navPad)
                ) {
                    TextButton(onClick = { step = OnboardingStep.PERMISSIONS }, modifier = Modifier.align(Alignment.End)) {
                        Text("Skip", color = SpotVaultColors.Muted)
                    }
                    HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                        val slide = WelcomeSlides[page]
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = slide.imageRes),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth(0.78f)
                                    .heightIn(max = 220.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(slide.title, color = SpotVaultColors.OnSurface, fontWeight = FontWeight.Black, fontSize = 26.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                slide.body,
                                color = SpotVaultColors.Muted,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center
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
                    SpotVaultButton(
                        onClick = {
                            if (pagerState.currentPage < WelcomeSlides.lastIndex) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            } else {
                                step = OnboardingStep.PERMISSIONS
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text(
                            if (pagerState.currentPage < WelcomeSlides.lastIndex) "Next" else "Get Started",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/** Primes the user for the system permission dialogs MainActivity fires right after onboarding
 * finishes (see the has_requested_initial_permissions LaunchedEffect) — a plain "Continue" here
 * would leave those dialogs feeling like they came out of nowhere. Purely informational, so it
 * has no Skip of its own: there's nothing to opt out of, just one button forward. */
@Composable
private fun PermissionsPrimerStep(
    statusPad: androidx.compose.ui.unit.Dp,
    navPad: androidx.compose.ui.unit.Dp,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = statusPad + 16.dp, bottom = navPad),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(SpotVaultColors.Teal.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    "Just A Heads Up",
                    color = SpotVaultColors.OnSurface,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "DropPin Vault needs a few permissions to work its magic. We need Location access to drop your pins and track your parking, and Camera access if you want to snap photos of your spots. We only use these to get you back to your car.",
                    color = SpotVaultColors.Muted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        SpotVaultButton(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Got It", fontWeight = FontWeight.Bold)
        }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = statusPad + 16.dp, bottom = navPad),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
                val icon = when (driveState) {
                    is DriveOnboardingState.Connected -> Icons.Default.CloudDone
                    is DriveOnboardingState.Failed -> Icons.Default.CloudOff
                    is DriveOnboardingState.NeedsConflictResolution -> Icons.Default.CloudSync
                    else -> Icons.Default.CloudUpload
                }
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(SpotVaultColors.Teal.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (driveState is DriveOnboardingState.Working) {
                        CircularProgressIndicator(color = SpotVaultColors.Teal)
                    } else {
                        Icon(icon, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(48.dp))
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text("Never Lose Your Vault", color = SpotVaultColors.OnSurface, fontWeight = FontWeight.Black, fontSize = 26.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                val body = when (driveState) {
                    is DriveOnboardingState.Idle -> "If you lose your phone, you lose your spots. Connect Google Drive for automatic, private, and free background backups. Only DropPin Vault can see this hidden folder."
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    body,
                    color = SpotVaultColors.Muted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

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
