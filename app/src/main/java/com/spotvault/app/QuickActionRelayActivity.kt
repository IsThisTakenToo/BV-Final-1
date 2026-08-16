package com.spotvault.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RelayOutcome(val success: Boolean, val message: String, val subtitle: String = "")

/**
 * Translucent overlay the widget launches — not the main app UI. Runs quick actions, shows a
 * polished confirmation card over the launcher, then exits without leaving DropPin Vault open.
 */
class QuickActionRelayActivity : ComponentActivity() {

    private lateinit var relayPrefs: android.content.SharedPreferences
    private var outcomeState = mutableStateOf<RelayOutcome?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) performTrack() else finishWith(RelayOutcome(false, "Notifications blocked", "Enable alerts in Settings to track from the widget."))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        suppressTransitionAnimation()

        relayPrefs = getSharedPreferences("SpotVaultPrefs", MODE_PRIVATE)
        ThemeState.currentTheme = loadColorThemeFromPrefs(relayPrefs)
        ThemeState.buttonStyle = loadButtonStyleFromPrefs(relayPrefs)
        ThemeState.customPrimaryArgb = relayPrefs.getInt("custom_primary_color", 0xFF6B2FFF.toInt())
        ThemeState.customAccentArgb = relayPrefs.getInt("custom_accent_color", 0xFF00F0FF.toInt())
        // This Activity can cold-start directly from a widget tap or QS tile with no prior
        // MainActivity launch in this process, so ThemeState still holds its plain default here
        // unless explicitly synced — without this line every other theme property above already
        // got that treatment, but the font choice didn't, so this screen's buttons silently
        // reverted to the default typeface regardless of what was picked in Settings.
        ThemeState.fontFamilyId = relayPrefs.getString("app_font_family", AppFontOptions.DEFAULT_ID) ?: AppFontOptions.DEFAULT_ID
        SpotVaultColors.updateAmoled(relayPrefs.getBoolean("amoled_black", false))
        ThemeState.reduceAnimations = relayPrefs.getBoolean("reduce_animations", false)

        val action = intent.getStringExtra(EXTRA_ACTION) ?: ACTION_SHARE

        // This Activity uses the default (standard) launch mode, so a fast double-tap on a
        // widget button — or a QS tile tapped again before the panel visually collapses — can
        // spawn two overlapping instances, each independently firing its own GPS fetch and DB
        // insert for what the user meant as a single tap. actionInFlight blocks the second
        // instance outright rather than letting it race the first.
        if (actionInFlight) {
            finish()
            return
        }
        actionInFlight = true

        setContent {
            SpotVaultTheme {
                val foundStyle = remember { loadFoundSplashStyleFromPrefs(relayPrefs) }
                QuickActionRelayScreen(
                    action = action,
                    outcome = outcomeState.value,
                    foundSplashStyle = foundStyle,
                    onFoundSplashFinished = { finish() },
                    onDismissRequest = { finish() }
                )
            }
        }

        when (action) {
            ACTION_PIN -> performPin()
            ACTION_TRACK -> performTrack()
            ACTION_SHARE -> performShare()
            ACTION_FOUND -> performFound()
            ACTION_NAVIGATE -> performNavigate()
            else -> finishWith(RelayOutcome(false, "Unknown action"))
        }
    }

    override fun finish() {
        super.finish()
        suppressTransitionAnimation()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleared here rather than in finish() so this can't get stuck true if the Activity is
        // ever torn down some other way (the guard exists to block re-entrant taps, not to
        // survive as a permanent lockout).
        actionInFlight = false
    }

    private fun suppressTransitionAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    /** True (and redirects to MainActivity, which shows AppLockScreen) if App Lock is on. Only
     * gates the three actions here that reveal a location — live GPS position (Share/Show Map)
     * or the actively-tracked spot (Navigate) — the same content App Lock exists to protect.
     * Pin/Track/Found aren't gated: they only create a new save or clear the active one, never
     * display anything already in the Vault, matching how a phone's own lock screen still allows
     * a quick camera launch without a full unlock. Without this, anyone with the device could
     * reach a saved location via the QS tile or a widget button without ever unlocking. */
    private fun denyIfAppLocked(): Boolean {
        if (!relayPrefs.getBoolean(APP_LOCK_ENABLED_PREF, false)) return false
        startActivity(
            android.content.Intent(this, MainActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
        finish()
        return true
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun performPin() {
        if (!hasLocationPermission()) {
            finishWith(RelayOutcome(false, "Location needed", "Open DropPin Vault once and grant location access."))
            return
        }
        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(this@QuickActionRelayActivity).locationDao()
            when (quietSaveTacticalPin(this@QuickActionRelayActivity, dao, relayPrefs, GENERIC_QUICK_PIN)) {
                is QuietSaveResult.Saved -> finishWith(RelayOutcome(true, "Pin saved", "Your spot is in the Vault."))
                QuietSaveResult.Failed -> finishWith(RelayOutcome(false, "Save failed", "Please try again in a moment."))
                QuietSaveResult.NeedsNotificationPermission -> finishWith(RelayOutcome(true, "Pin saved", "Your spot is in the Vault."))
            }
        }
    }

    private fun performTrack() {
        if (!hasLocationPermission()) {
            finishWith(RelayOutcome(false, "Location needed", "Open DropPin Vault once and grant location access."))
            return
        }
        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(this@QuickActionRelayActivity).locationDao()
            val option = quickTrackOption()
            when (quickActiveTrackPin(this@QuickActionRelayActivity, dao, relayPrefs, option)) {
                is QuietSaveResult.Saved -> {
                    finishWith(
                        outcome = RelayOutcome(
                            success = true,
                            message = "Tracking active",
                            subtitle = "Widget updated — use Found or Navigate when you're ready."
                        ),
                        refreshWidgetsAfter = true
                    )
                }
                QuietSaveResult.NeedsNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        finishWith(RelayOutcome(false, "Notifications blocked", "Enable alerts to keep tracking visible."))
                    }
                }
                QuietSaveResult.Failed -> finishWith(RelayOutcome(false, "Couldn't start tracking", "Please try again in a moment."))
            }
        }
    }

    private fun performShare() {
        if (denyIfAppLocked()) return
        if (!hasLocationPermission()) {
            finishWith(RelayOutcome(false, "Location needed", "Open DropPin Vault once and grant location access."))
            return
        }
        lifecycleScope.launch {
            val located = withContext(Dispatchers.IO) {
                val coords = resolveCurrentLocation(this@QuickActionRelayActivity, relayPrefs) ?: return@withContext null
                val (lat, lng) = coords
                val address = if (relayPrefs.getBoolean("auto_fetch_address", true)) {
                    reverseGeocodeAddress(this@QuickActionRelayActivity, lat, lng).full
                } else ""
                Triple(lat, lng, address)
            }
            if (located == null) {
                finishWith(RelayOutcome(false, "No GPS fix", "Move somewhere with signal and try again."))
            } else {
                val (lat, lng, address) = located
                outcomeState.value = RelayOutcome(true, "Opening share…", "Pick where to send your location.")
                delay(350)
                shareLocation(
                    this@QuickActionRelayActivity,
                    lat, lng, address,
                    notes = "",
                    imagePath = "",
                    includePhoto = false
                )
                finish()
            }
        }
    }

    private fun performFound() {
        // Prefs first, then await widget repaint WHILE TimerService is still foreground
        // (elevated process priority), then tear the service down. Stopping the service before
        // the refresh was why Track updated the widget but Found often did not.
        ActiveTrackingHelper.clearTrackingPrefs(this)
        WidgetThemeHelper.bumpWidgetRevision(relayPrefs)
        FoundCelebration.play(this, withToast = false)
        lifecycleScope.launch {
            WidgetThemeHelper.refreshAllWidgetsAwait(applicationContext)
            ActiveTrackingHelper.stopTrackingServiceAndNotifications(this@QuickActionRelayActivity)
            outcomeState.value = RelayOutcome(
                true,
                loadFoundSplashTitleFromPrefs(relayPrefs),
                loadFoundSplashSubtitleFromPrefs(relayPrefs)
            )
        }
    }

    private fun performNavigate() {
        if (denyIfAppLocked()) return
        lifecycleScope.launch {
            outcomeState.value = RelayOutcome(true, "Opening navigation…", "Launching your maps app.")
            delay(260)
            val launched = ActiveTrackingHelper.launchNavigation(this@QuickActionRelayActivity)
            if (launched) {
                finish()
            } else {
                finishWith(RelayOutcome(false, "Can't navigate", "No coordinates saved for this track."))
            }
        }
    }

    private fun finishWith(outcome: RelayOutcome, refreshWidgetsAfter: Boolean = false) {
        outcomeState.value = outcome
        lifecycleScope.launch {
            if (refreshWidgetsAfter) {
                // Await the refresh here, while this Activity — and therefore the process — is
                // still guaranteed alive. Firing it from a detached scope after finish() risked
                // the process losing priority (or being killed with no foreground component
                // left) before the background work completed, which is what made Found/Track
                // silently never repaint the widget.
                WidgetThemeHelper.refreshAllWidgetsAwait(applicationContext)
            }
            delay(if (outcome.success) 1400 else 2000)
            finish()
        }
    }

    companion object {
        const val EXTRA_ACTION = "relay_action"
        const val ACTION_PIN = "pin"
        const val ACTION_TRACK = "track"
        const val ACTION_SHARE = "share"
        const val ACTION_FOUND = "found"
        const val ACTION_NAVIGATE = "navigate"

        // Only ever read/written from onCreate/onDestroy on the main thread, so a plain flag is
        // enough — this is a same-process re-entrancy guard, not a real concurrency primitive.
        @Volatile
        private var actionInFlight = false
    }
}

@Composable
private fun QuickActionRelayScreen(
    action: String,
    outcome: RelayOutcome?,
    foundSplashStyle: FoundSplashStyle = FoundSplashStyle.CLASSIC,
    onFoundSplashFinished: () -> Unit = {},
    onDismissRequest: () -> Unit = {}
) {
    if (action == QuickActionRelayActivity.ACTION_FOUND && outcome?.success == true) {
        FoundSplashOverlay(
            style = foundSplashStyle,
            title = outcome.message,
            subtitle = outcome.subtitle,
            onFinished = onFoundSplashFinished
        )
        return
    }

    val haptic = LocalHapticFeedback.current
    var lastHapticFor by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(outcome?.success) {
        if (outcome != null && lastHapticFor != outcome.success) {
            lastHapticFor = outcome.success
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val (icon, workingLabel, workingSubtitle) = when (action) {
        QuickActionRelayActivity.ACTION_PIN -> Triple(Icons.Default.PinDrop, "Saving pin…", "Getting your location")
        QuickActionRelayActivity.ACTION_TRACK -> Triple(Icons.Default.Route, "Starting track…", "Saving spot & turning on alerts")
        QuickActionRelayActivity.ACTION_FOUND -> Triple(Icons.Default.CheckCircle, "Clearing track…", "Wrapping up active tracking")
        QuickActionRelayActivity.ACTION_NAVIGATE -> Triple(Icons.Default.Navigation, "Opening maps…", "Launching turn-by-turn navigation")
        else -> Triple(Icons.Default.Share, "Getting location…", "Preparing your share link")
    }

    val scale by animateFloatAsState(
        targetValue = if (outcome != null) 1f else 0.94f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "cardScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(220),
        label = "cardAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.68f))
            // Only dismissible once outcome is known — the actual save/track work runs in a
            // lifecycleScope coroutine tied to this Activity, so letting a tap finish() (and tear
            // the Activity down) while that's still in flight would cancel it mid-write instead
            // of just skipping the confirmation card that lingers afterward. Before this, there
            // was no way to skip that lingering display at all — hitting Quick Track meant
            // staring at "Tracking active" for a fixed ~1.4s+ with nothing to tap.
            .then(
                if (outcome != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(28.dp)
                // .scale()/.alpha() read their values at composable-body scope, recomposing this
                // whole card (icon, both text lines) on every frame of these one-shot transitions
                // — graphicsLayer defers both to the draw phase instead, same visuals.
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = cardAlpha
                }
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            SpotVaultColors.Elevated,
                            SpotVaultColors.Elevated.copy(alpha = 0.92f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            SpotVaultColors.Primary.copy(alpha = 0.55f),
                            SpotVaultColors.Teal.copy(alpha = 0.35f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 32.dp, vertical = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StatusOrb(actionIcon = icon, outcome = outcome)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = outcome?.message ?: workingLabel,
                    color = SpotVaultColors.OnSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = outcome?.subtitle ?: workingSubtitle,
                    color = SpotVaultColors.Muted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun StatusOrb(actionIcon: ImageVector, outcome: RelayOutcome?) {
    val resultScale by animateFloatAsState(
        targetValue = if (outcome != null) 1f else 0.88f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "orbScale"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .graphicsLayer {
                scaleX = resultScale
                scaleY = resultScale
            }
            .clip(CircleShape)
            .background(
                when {
                    outcome == null -> SpotVaultColors.Primary.copy(alpha = 0.16f)
                    outcome.success -> SpotVaultColors.Teal.copy(alpha = 0.22f)
                    else -> SpotVaultColors.Danger.copy(alpha = 0.22f)
                }
            )
            .border(
                width = 1.dp,
                color = when {
                    outcome == null -> SpotVaultColors.Primary.copy(alpha = 0.35f)
                    outcome.success -> SpotVaultColors.Teal.copy(alpha = 0.45f)
                    else -> SpotVaultColors.Danger.copy(alpha = 0.45f)
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            outcome == null -> CircularProgressIndicator(
                color = SpotVaultColors.Primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(34.dp)
            )
            outcome.success -> Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = SpotVaultColors.Teal,
                modifier = Modifier.size(42.dp)
            )
            else -> Icon(
                Icons.Default.Close,
                contentDescription = null,
                tint = SpotVaultColors.Danger,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}
