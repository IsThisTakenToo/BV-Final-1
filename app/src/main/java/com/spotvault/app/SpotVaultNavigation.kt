package com.spotvault.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Navigation routes for the main SpotVault shell. */
object SpotVaultRoutes {
    const val HOME = "home"
    const val VAULT = "vault"
    const val SETTINGS = "settings"
    const val SETTINGS_APPEARANCE = "settings_appearance"
    const val SETTINGS_VAULT = "settings_vault"
    const val SETTINGS_AUTOPARK = "settings_autopark"
    const val SETTINGS_HELP = "settings_help"
    const val SETTINGS_PREMIUM = "settings_premium"
    const val SPOT_DETAIL = "spot/{spotId}"
    const val ACTIVE_SPOT = "active_spot"
    const val COMPASS = "compass/{spotId}?lat={lat}&lng={lng}"

    /** Destinations where the bottom navigation bar stays visible. */
    val TOP_LEVEL_ROUTES = setOf(HOME)

    fun spotDetail(spotId: Int): String = "spot/$spotId"

    fun compass(spotId: Int, lat: Double = 0.0, lng: Double = 0.0): String {
        // Fixed-decimal, not raw Double.toString() — coordinates near 0 (close to the equator or
        // prime meridian) render in scientific notation ("1.23E-8"), which the "lat={lat}" nav
        // argument can't parse back into a Float.
        val latStr = String.format(java.util.Locale.US, "%.7f", lat)
        val lngStr = String.format(java.util.Locale.US, "%.7f", lng)
        return "compass/$spotId?lat=$latStr&lng=$lngStr"
    }

    fun baseRoute(route: String?): String? = route?.substringBefore("/")

    fun shouldShowBottomBar(route: String?): Boolean {
        val base = baseRoute(route) ?: return true
        return base in TOP_LEVEL_ROUTES
    }
}

/** Toggle or navigate to a main tab, popping back to home when the same tab is tapped again. */
fun NavController.navigateMainTab(route: String) {
    val currentBase = SpotVaultRoutes.baseRoute(currentBackStackEntry?.destination?.route)
    if (currentBase == route) {
        popBackStack(SpotVaultRoutes.HOME, inclusive = false)
    } else {
        navigate(route) {
            popUpTo(SpotVaultRoutes.HOME) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}

fun NavController.navigateToSpotDetail(spotId: Int) {
    navigate(SpotVaultRoutes.spotDetail(spotId)) {
        launchSingleTop = true
    }
}

fun NavController.navigateToActiveSpot() {
    navigate(SpotVaultRoutes.ACTIVE_SPOT) {
        launchSingleTop = true
    }
}

fun NavController.navigateToCompass(spotId: Int, lat: Double = 0.0, lng: Double = 0.0) {
    navigate(SpotVaultRoutes.compass(spotId, lat, lng)) {
        launchSingleTop = true
    }
}

/** Maps the current route to a bottom-bar highlight state. */
fun bottomNavDestinationForRoute(route: String?): BottomNavDestination? =
    when (SpotVaultRoutes.baseRoute(route)) {
        SpotVaultRoutes.VAULT -> BottomNavDestination.VAULT
        SpotVaultRoutes.SETTINGS, SpotVaultRoutes.SETTINGS_APPEARANCE, SpotVaultRoutes.SETTINGS_VAULT, SpotVaultRoutes.SETTINGS_AUTOPARK, SpotVaultRoutes.SETTINGS_HELP, SpotVaultRoutes.SETTINGS_PREMIUM -> BottomNavDestination.SETTINGS
        else -> null
    }

@Composable
fun SpotVaultMainScaffold(
    isPinned: Boolean,
    prefs: android.content.SharedPreferences,
    dao: LocationDao,
    onSnapClick: () -> Unit,
    onPinOnlyClick: () -> Unit,
    onFoundClick: () -> Unit,
    onShareRequest: (ShareSpotPayload) -> Unit,
    onPickRingtone: () -> Unit,
    onAppLockChanged: (Boolean) -> Unit,
    onRequestLocationPermission: () -> Unit,
    onClearPinnedSpot: () -> Unit,
    navController: NavHostController = rememberNavController(),
    initialNavRoute: String? = null,
    initialSpotId: Int = -1,
    initialCompassLat: Double? = null,
    initialCompassLng: Double? = null,
    modifier: Modifier = Modifier,
    // The real bottom-bar height (already including the system nav bar inset it applies to
    // itself) — passed through so overlays anchored to the bottom edge (the Vault undo snackbar)
    // can sit above both instead of rendering underneath them.
    contentOverlays: @Composable (bottomInset: Dp) -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = SpotVaultRoutes.shouldShowBottomBar(currentRoute)
    val activeDestination = bottomNavDestinationForRoute(currentRoute)
    // Same isGenuineTablet() check that already decides content-width capping everywhere else —
    // see its own doc for why reusing exactly this one, rather than a separately-tuned width
    // threshold, matters. useRail only changes anything on the same routes the bottom bar would
    // otherwise show on (showBottomBar) — Vault/Settings/etc. still take over the full window on
    // every screen size for now, same as today; this section only swaps which navigation chrome
    // appears on top of the tab-root screen, not the two-pane list/detail work that comes after it.
    val useRail = showBottomBar && isGenuineTablet()

    LaunchedEffect(initialNavRoute) {
        initialNavRoute?.let { route ->
            val resolved = when (route) {
                "map" -> SpotVaultRoutes.HOME
                else -> route
            }
            navController.navigateMainTab(resolved)
        }
    }

    // Deliberately its own effect, not folded into initialNavRoute above — that one is keyed on
    // the resolved route *string*, so a second share arriving while already past the first one
    // (e.g. the user left the Vault tab in between) would resolve to the same "vault" value as
    // before and silently never re-fire, since LaunchedEffect only restarts when its key actually
    // changes. PendingSharedSpot.version is a counter precisely so every single share is a
    // distinct key, guaranteeing this always re-navigates regardless of what the user did between
    // shares or whether they'd already been sent to the Vault tab once before.
    //
    // Deliberately checks the current route BEFORE calling navigateMainTab, rather than always
    // calling it — navigateMainTab's own "already on this tab" branch does popBackStack(HOME),
    // which is the correct behavior for an actual user re-tapping the Vault icon (reset to the
    // tab's root), but is exactly wrong here: the Favorites Hub / Add Spot form is normally still
    // open and on-screen when a share arrives (that's the whole point — the user left FROM there
    // to search in Maps), and popping to Home would tear down that entire open dialog instead of
    // leaving it alone. Only navigate when the share woke the app up from genuinely outside the
    // Vault tab.
    LaunchedEffect(PendingSharedSpot.version.value) {
        if (PendingSharedSpot.version.value > 0) {
            val currentBase = SpotVaultRoutes.baseRoute(navController.currentBackStackEntry?.destination?.route)
            if (currentBase != SpotVaultRoutes.VAULT) {
                navController.navigateMainTab(SpotVaultRoutes.VAULT)
            }
        }
    }

    LaunchedEffect(initialSpotId) {
        if (initialSpotId >= 0) {
            navController.navigateToSpotDetail(initialSpotId)
        }
    }

    LaunchedEffect(initialCompassLat, initialCompassLng) {
        val lat = initialCompassLat
        val lng = initialCompassLng
        if (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) {
            navController.navigateToCompass(spotId = -1, lat = lat, lng = lng)
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        // Collapses to zero size (not just invisible) whenever useRail is false — which is every
        // phone, always — so this AnimatedVisibility contributes nothing to the Row's layout on
        // the entire non-tablet install base. The Scaffold below is completely unaffected by this
        // wrapping Row on phones: it still receives the full available width via weight(1f).
        AnimatedVisibility(
            visible = useRail,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(320, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(280)),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(220))
        ) {
            SpotVaultNavigationRail(
                onVaultClick = { navController.navigateMainTab(SpotVaultRoutes.VAULT) },
                onSettingsClick = { navController.navigateMainTab(SpotVaultRoutes.SETTINGS) },
                onAppearanceClick = { navController.navigateMainTab(SpotVaultRoutes.SETTINGS_APPEARANCE) },
                activeDestination = activeDestination,
                showSettings = true
            )
        }

        Scaffold(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            containerColor = Color.Transparent,
            bottomBar = {
                // Same showBottomBar-driven visibility/animation regardless of useRail — only the
                // *content* of this slot changes: the rail already covers navigation once it's
                // showing, so this becomes a disclaimer-only footer instead of duplicating
                // SpotVaultBottomBar's buttons a second time next to the rail.
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(320, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(280)),
                    exit = slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(220))
                ) {
                    // Rail mode's bottomBar slot used to hold only VaultDisclaimerStrip (the rail
                    // itself already covers navigation) — now that the disclaimer's gone, there's
                    // nothing left for this slot to show at all.
                    if (!useRail) {
                        AdaptiveTabletContainer(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    // navigationBarsPadding() alone lands this bar's bottom edge
                                    // flush against the gesture inset boundary — fine on a device
                                    // with soft nav buttons (there's a solid nav bar to rest on),
                                    // but on edge-to-edge gesture nav the VAULT label ended up
                                    // right at the swipe-up strip with zero breathing room. The
                                    // disclaimer strip this bar used to sit above happened to
                                    // supply that cushion as a side effect; restoring it directly
                                    // now that the disclaimer's gone.
                                    .padding(bottom = 8.dp)
                            ) {
                                SpotVaultBottomBar(
                                    onVaultClick = { navController.navigateMainTab(SpotVaultRoutes.VAULT) },
                                    onSettingsClick = { navController.navigateMainTab(SpotVaultRoutes.SETTINGS) },
                                    onAppearanceClick = { navController.navigateMainTab(SpotVaultRoutes.SETTINGS_APPEARANCE) },
                                    activeDestination = activeDestination,
                                    showSettings = true
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            SpotVaultNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                isPinned = isPinned,
                prefs = prefs,
                dao = dao,
                onSnapClick = onSnapClick,
                onPinOnlyClick = onPinOnlyClick,
                onFoundClick = onFoundClick,
                onShareRequest = onShareRequest,
                onPickRingtone = onPickRingtone,
                onAppLockChanged = onAppLockChanged,
                onRequestLocationPermission = onRequestLocationPermission,
                onClearPinnedSpot = onClearPinnedSpot
            )
            contentOverlays(innerPadding.calculateBottomPadding())
        }
    }
}

/** Detail pane placeholder for a two-pane list+detail layout (genuine tablets/unfolded foldables)
 * before any spot has been tapped in the list pane yet, or right after the shown spot's
 * onDismiss clears the selection. Deliberately minimal — this is a resting state, not a screen
 * with its own actions. Not private — also reused by Favorites Hub's own two-pane branch
 * (HistoryVaultDialog.kt), with its own [message] override. */
@Composable
internal fun VaultDetailEmptyState(message: String = "Select a spot to view its details") {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = SpotVaultColors.Muted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun SpotVaultNavHost(
    navController: NavHostController,
    isPinned: Boolean,
    prefs: android.content.SharedPreferences,
    dao: LocationDao,
    onSnapClick: () -> Unit,
    onPinOnlyClick: () -> Unit,
    onFoundClick: () -> Unit,
    onShareRequest: (ShareSpotPayload) -> Unit,
    onPickRingtone: () -> Unit,
    onAppLockChanged: (Boolean) -> Unit,
    onRequestLocationPermission: () -> Unit,
    onClearPinnedSpot: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = SpotVaultRoutes.HOME,
        modifier = modifier.fillMaxSize()
    ) {
        composable(SpotVaultRoutes.HOME) {
            SpotVaultScreen(
                modifier = Modifier.fillMaxSize(),
                isPinned = isPinned,
                onSnapClick = onSnapClick,
                onPinOnlyClick = onPinOnlyClick,
                onFoundClick = onFoundClick,
                onPhotoClick = { navController.navigateToActiveSpot() },
                onShareRequest = onShareRequest,
                prefs = prefs,
                dao = dao,
                onRequestLocationPermission = onRequestLocationPermission,
                onViewSpot = { spot -> navController.navigateToSpotDetail(spot.id) },
                onNavigateToCompass = { spotId, lat, lng ->
                    navController.navigateToCompass(spotId, lat, lng)
                },
                onOpenAutoParkSettings = { navController.navigateMainTab(SpotVaultRoutes.SETTINGS_AUTOPARK) },
                onOpenHelpSettings = { navController.navigateMainTab(SpotVaultRoutes.SETTINGS_HELP) }
            )
        }

        composable(SpotVaultRoutes.VAULT) {
            BackHandler { navController.popBackStack() }
            if (isWideEnoughForTwoPane()) {
                // Two-pane only for the main Vault list itself, and only once the window is
                // actually wide enough for it (see isWideEnoughForTwoPane's own doc — this is
                // deliberately a materially higher bar than isGenuineTablet/AdaptiveTabletContainer
                // use, not the same threshold). Deliberately NOT wrapped in AdaptiveTabletContainer
                // either way, since that caps content to a single centered 600dp column, exactly
                // the opposite of what a side-by-side layout needs on a wide window. Location
                // Browser, Archived Spots, Calendar, and Tag Editor are all overlay Dialogs
                // rendered *inside* HistoryDialogContent itself, not separate routes — none of
                // that changes here, so opening any of them still shows its own existing
                // full-screen-or-capped treatment untouched, on top of whichever pane it was
                // opened from. That's an intentional, separate scope for later, not an oversight.
                // Favorites Hub is the one exception: it now has its own independent two-pane
                // branch (HistoryVaultDialog.kt's FavoritesHubDialog), gated on this same
                // isWideEnoughForTwoPane() check, so it no longer inherits the single-pane
                // treatment described above when opened from in here.
                var selectedSpotId by rememberSaveable { mutableStateOf(-1) }
                // Fixed list-pane width, not a percentage of the Row — a percentage split scales
                // the list pane down right alongside the window, which is exactly backwards at
                // the *low* end of the Expanded range (a plain weight(0.42f) at exactly 840dp,
                // isWideEnoughForTwoPane's own floor, would still only be ~353dp) and wastes
                // space at the high end (a 2000dp desktop-class window would hand the list pane
                // an absurd 840dp while the detail pane goes relatively starved). 380dp is
                // comfortably wider than this component already renders correctly at on a
                // phone (360-430dp is the normal single-pane range), so nothing about
                // HistoryDialogContent's own internal layout needs to change to host it here.
                // TwoPaneRow (not a plain Row) so the gap between panes actually clears a
                // foldable's hinge instead of guessing a fixed padding — see its own doc.
                TwoPaneRow(
                    modifier = Modifier.fillMaxSize(),
                    listPane = {
                        HistoryDialogContent(
                            onDismiss = { navController.popBackStack() },
                            dao = dao,
                            prefs = prefs,
                            isPinned = isPinned,
                            // Selecting a spot updates local pane state instead of navigating —
                            // this composable (and all 18+ pieces of state it owns: search,
                            // sort, multi-select, the calendar/location-browser overlays, etc.)
                            // simply never gets disposed while browsing in two-pane mode, unlike
                            // the phone path below where every spot tap tears the whole screen
                            // down and rebuilds it on the way back.
                            onViewSpot = { spot -> selectedSpotId = spot.id },
                            onShareRequest = onShareRequest,
                            onOpenSettings = { navController.navigateMainTab(SpotVaultRoutes.SETTINGS_VAULT) }
                        )
                    },
                    detailPane = {
                        if (selectedSpotId >= 0) {
                            // Reused as-is from the phone nav route below — it already takes a
                            // plain spotId + onDismiss callback rather than reading them from
                            // backstack args, so no changes were needed to support this. Its own
                            // existing "spot no longer found → onDismiss" handling (a live delete
                            // from the list pane, or Auto Delete, while this pane has it open)
                            // already exists for the single-pane case and applies here unchanged.
                            SavedSpotDetailRoute(
                                spotId = selectedSpotId,
                                dao = dao,
                                prefs = prefs,
                                onNavigateToCompass = { id -> navController.navigateToCompass(spotId = id) },
                                onDismiss = { selectedSpotId = -1 },
                                onShareRequest = onShareRequest
                            )
                        } else {
                            VaultDetailEmptyState()
                        }
                    }
                )
            } else {
                AdaptiveTabletContainer(modifier = Modifier.fillMaxSize()) {
                    HistoryDialogContent(
                            onDismiss = { navController.popBackStack() },
                            dao = dao,
                            prefs = prefs,
                            isPinned = isPinned,
                            onViewSpot = { spot -> navController.navigateToSpotDetail(spot.id) },
                            onShareRequest = onShareRequest,
                            onOpenSettings = { navController.navigateMainTab(SpotVaultRoutes.SETTINGS_VAULT) }
                    )
                }
            }
        }

        composable(SpotVaultRoutes.SETTINGS) {
            BackHandler { navController.popBackStack() }
            SettingsDialog(
                onDismiss = { navController.popBackStack() },
                prefs = prefs,
                dao = dao,
                onPickRingtone = onPickRingtone,
                onAppLockChanged = onAppLockChanged,
                embeddedInMainNav = true
            )
        }

        composable(SpotVaultRoutes.SETTINGS_APPEARANCE) {
            BackHandler { navController.popBackStack() }
            SettingsDialog(
                onDismiss = { navController.popBackStack() },
                prefs = prefs,
                dao = dao,
                onPickRingtone = onPickRingtone,
                onAppLockChanged = onAppLockChanged,
                embeddedInMainNav = true,
                initialCategoryId = "appearance"
            )
        }

        composable(SpotVaultRoutes.SETTINGS_VAULT) {
            BackHandler { navController.popBackStack() }
            SettingsDialog(
                onDismiss = { navController.popBackStack() },
                prefs = prefs,
                dao = dao,
                onPickRingtone = onPickRingtone,
                onAppLockChanged = onAppLockChanged,
                embeddedInMainNav = true,
                initialCategoryId = "vault"
            )
        }

        composable(SpotVaultRoutes.SETTINGS_AUTOPARK) {
            BackHandler { navController.popBackStack() }
            SettingsDialog(
                onDismiss = { navController.popBackStack() },
                prefs = prefs,
                dao = dao,
                onPickRingtone = onPickRingtone,
                onAppLockChanged = onAppLockChanged,
                embeddedInMainNav = true,
                initialCategoryId = "autopark"
            )
        }

        composable(SpotVaultRoutes.SETTINGS_HELP) {
            BackHandler { navController.popBackStack() }
            SettingsDialog(
                onDismiss = { navController.popBackStack() },
                prefs = prefs,
                dao = dao,
                onPickRingtone = onPickRingtone,
                onAppLockChanged = onAppLockChanged,
                embeddedInMainNav = true,
                initialCategoryId = "help"
            )
        }

        composable(SpotVaultRoutes.SETTINGS_PREMIUM) {
            BackHandler { navController.popBackStack() }
            SettingsDialog(
                onDismiss = { navController.popBackStack() },
                prefs = prefs,
                dao = dao,
                onPickRingtone = onPickRingtone,
                onAppLockChanged = onAppLockChanged,
                embeddedInMainNav = true,
                initialCategoryId = "premium"
            )
        }

        composable(
            route = SpotVaultRoutes.SPOT_DETAIL,
            arguments = listOf(navArgument("spotId") { type = NavType.IntType })
        ) { backStackEntry ->
            val spotId = backStackEntry.arguments?.getInt("spotId") ?: return@composable
            BackHandler { navController.popBackStack() }
            SavedSpotDetailRoute(
                spotId = spotId,
                dao = dao,
                prefs = prefs,
                onNavigateToCompass = { id -> navController.navigateToCompass(spotId = id) },
                onDismiss = { navController.popBackStack() },
                onShareRequest = onShareRequest
            )
        }

        composable(
            route = SpotVaultRoutes.COMPASS,
            arguments = listOf(
                navArgument("spotId") { type = NavType.IntType },
                navArgument("lat") {
                    type = NavType.FloatType
                    defaultValue = 0f
                },
                navArgument("lng") {
                    type = NavType.FloatType
                    defaultValue = 0f
                }
            ),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(380, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(300, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(tween(220))
            },
            popEnterTransition = {
                fadeIn(tween(280, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(340, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(260, easing = FastOutSlowInEasing))
            }
        ) { backStackEntry ->
            val spotId = backStackEntry.arguments?.getInt("spotId") ?: return@composable
            val fallbackLat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 0.0
            val fallbackLng = backStackEntry.arguments?.getFloat("lng")?.toDouble() ?: 0.0
            BackHandler { navController.popBackStack() }
            CompassSpotRoute(
                spotId = spotId,
                fallbackLat = fallbackLat,
                fallbackLng = fallbackLng,
                dao = dao,
                prefs = prefs,
                onBack = { navController.popBackStack() }
            )
        }

        composable(SpotVaultRoutes.ACTIVE_SPOT) {
            BackHandler { navController.popBackStack() }
            ActiveSpotDetailRoute(
                prefs = prefs,
                navController = navController,
                onDismiss = { navController.popBackStack() },
                onFoundClick = {
                    navController.popBackStack()
                    onClearPinnedSpot()
                },
                onShareRequest = onShareRequest
            )
        }
    }
}

@Composable
private fun CompassSpotRoute(
    spotId: Int,
    fallbackLat: Double,
    fallbackLng: Double,
    dao: LocationDao,
    prefs: android.content.SharedPreferences,
    onBack: () -> Unit
) {
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var floorLevel by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(spotId, fallbackLat, fallbackLng) {
        withContext(Dispatchers.IO) {
            if (spotId > 0) {
                dao.getSpotByIdLite(spotId)?.let { spot ->
                    lat = spot.lat
                    lng = spot.lng
                    title = spot.title.ifBlank { "Saved Spot" }
                    subtitle = spot.address
                    floorLevel = spot.floorLevel
                }
            } else if (fallbackLat != 0.0 || fallbackLng != 0.0) {
                lat = fallbackLat
                lng = fallbackLng
                title = "Active Spot"
                subtitle = prefs.getString("current_address", "") ?: ""
                floorLevel = prefs.getString("floor_level", null)
            }
        }
        isLoading = false
    }

    val targetLat = lat
    val targetLng = lng
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }
    if (targetLat == null || targetLng == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    CompassNavigationScreen(
        targetLat = targetLat,
        targetLng = targetLng,
        targetTitle = title,
        targetSubtitle = subtitle,
        floorLevel = floorLevel,
        prefs = prefs,
        onBack = onBack,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
internal fun SavedSpotDetailRoute(
    spotId: Int,
    dao: LocationDao,
    prefs: android.content.SharedPreferences,
    // A plain callback rather than a NavHostController directly — Favorites Hub's own two-pane
    // detail pane (HistoryVaultDialog.kt) reuses this composable but is reached through a chain
    // of plain callbacks (HistoryDialogContent -> FavoritesHubDialog), not a NavHost destination,
    // so it has no NavController of its own to pass. Null hides the Compass button entirely
    // (see FullScreenImageViewer's own onNavigateToCompass != null gate) rather than wiring it to
    // a no-op — that's the deliberate, lower-risk choice for Favorites Hub's embedded pane specifically.
    onNavigateToCompass: ((Int) -> Unit)?,
    onDismiss: () -> Unit,
    onShareRequest: (ShareSpotPayload) -> Unit
) {
    var spot by remember(spotId) { mutableStateOf<LocationSpot?>(null) }
    var isLoading by remember(spotId) { mutableStateOf(true) }
    var showEditDialog by remember(spotId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(spotId) {
        spot = withContext(Dispatchers.IO) {
            dao.getSpotByIdLite(spotId)
        }
        isLoading = false
    }

    val loaded = spot
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }
    if (loaded == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    FullScreenImageViewer(
        imagePath = loaded.imagePath,
        ocrText = "",
        note = loaded.locationDetails,
        timestampStr = loaded.timestamp.toString(),
        lat = loaded.lat.toFloat(),
        lng = loaded.lng.toFloat(),
        onDismiss = onDismiss,
        onFoundClick = null,
        address = loaded.address,
        prefs = prefs,
        onShareRequest = onShareRequest,
        modifier = Modifier.fillMaxSize(),
        spotId = loaded.id,
        onNotesPersisted = { updatedNotes ->
            val spotId = loaded.id
            // Re-fetches rather than loaded.copy(...) — same staleness risk as the Edit dialog's
            // onSave below, and the same fix.
            scope.launch(Dispatchers.IO) {
                val current = dao.getSpotById(spotId) ?: return@launch
                dao.updateSpot(current.copy(locationDetails = updatedNotes))
            }
            spot = loaded.copy(locationDetails = updatedNotes.take(160))
        },
        onNavigateToCompass = onNavigateToCompass?.let { callback ->
            { callback(loaded.id) }
        },
        spotItem = loaded,
        onEditRequest = { showEditDialog = true }
    )

    if (showEditDialog) {
        // Same full editor (title/category/date/city/state/notes) the Vault's own ⋮ → Edit
        // opens — this used to only be reachable from the Vault list, so the detail screen's
        // Edit button silently fell back to just the notes pencil, which could only ever touch
        // one field.
        SpotEditDialog(
            prefs = prefs,
            spotId = loaded.id,
            currentTitle = loaded.title,
            currentTimestamp = loaded.timestamp,
            currentNotes = rememberFullSpotNotes(dao, loaded.id, loaded.locationDetails),
            currentCity = loaded.city,
            currentState = loaded.state,
            currentVehicleId = loaded.vehicleId,
            onDismiss = { showEditDialog = false },
            onSave = { newTitle, newTimestamp, newNotes, newCity, newState, newVehicleId ->
                val spotId = loaded.id
                scope.launch(Dispatchers.IO) {
                    // Re-fetches rather than reusing `loaded` for the actual write — loaded is a
                    // one-time snapshot from when this screen opened (see the LaunchedEffect
                    // above) and never refreshes on its own. A photo attached from this same
                    // screen's own "Add Photo" button (FullScreenImageViewer shows it live via
                    // its own observeSpotByIdLite query, but that never flows back into this outer
                    // loaded/spot state) would otherwise get silently erased the moment Edit was
                    // saved — copy()-ing the stale `loaded` here would write its still-blank
                    // imagePath straight back over the one just attached. The optimistic local
                    // update below still starts from `loaded` — that's fine, it only drives
                    // fields this screen itself displays directly, not imagePath, which always
                    // comes from FullScreenImageViewer's own live value regardless of this one.
                    val current = dao.getSpotById(spotId) ?: return@launch
                    dao.updateSpot(
                        current.copy(
                            title = newTitle,
                            timestamp = newTimestamp,
                            locationDetails = newNotes,
                            city = newCity,
                            state = newState,
                            vehicleId = newVehicleId
                        )
                    )
                }
                spot = loaded.copy(
                    title = newTitle,
                    timestamp = newTimestamp,
                    locationDetails = newNotes.take(160),
                    city = newCity,
                    state = newState,
                    vehicleId = newVehicleId
                )
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun ActiveSpotDetailRoute(
    prefs: android.content.SharedPreferences,
    navController: NavHostController,
    onDismiss: () -> Unit,
    onFoundClick: () -> Unit,
    onShareRequest: (ShareSpotPayload) -> Unit
) {
    val lat = prefs.getCoord("lat")
    val lng = prefs.getCoord("lng")
    FullScreenImageViewer(
        imagePath = prefs.getString("photo_path", "") ?: "",
        ocrText = "",
        note = prefs.getString("location_details", "") ?: "",
        timestampStr = System.currentTimeMillis().toString(),
        lat = lat.toFloat(),
        lng = lng.toFloat(),
        onDismiss = onDismiss,
        onFoundClick = onFoundClick,
        address = prefs.getString("current_address", "") ?: "",
        prefs = prefs,
        onShareRequest = onShareRequest,
        modifier = Modifier.fillMaxSize(),
        spotId = -1,
        onNotesPersisted = null,
        onNavigateToCompass = {
            navController.navigateToCompass(spotId = -1, lat = lat, lng = lng)
        }
    )
}
