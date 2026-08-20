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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
    val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val disclaimerVerticalPad = if (screenHeightDp >= 700) 12.dp else 8.dp

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
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
                AdaptiveTabletContainer(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        SpotVaultBottomBar(
                            onVaultClick = { navController.navigateMainTab(SpotVaultRoutes.VAULT) },
                            onSettingsClick = { navController.navigateMainTab(SpotVaultRoutes.SETTINGS) },
                            onAppearanceClick = { navController.navigateMainTab(SpotVaultRoutes.SETTINGS_APPEARANCE) },
                            activeDestination = activeDestination,
                            showSettings = true
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            SpotVaultColors.Primary.copy(alpha = 0.45f),
                                            SpotVaultColors.Teal.copy(alpha = 0.45f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Text(
                            text = "USE AT YOUR OWN RISK. DropPin Vault IS NOT RESPONSIBLE FOR LOST LOCATIONS OR INACCURATE DATA.",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            lineHeight = 11.sp,
                            color = SpotVaultColors.Muted.copy(alpha = 0.65f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = disclaimerVerticalPad),
                            textAlign = TextAlign.Center,
                            maxLines = 4
                        )
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
                navController = navController,
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
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(spotId, fallbackLat, fallbackLng) {
        withContext(Dispatchers.IO) {
            if (spotId > 0) {
                dao.getSpotById(spotId)?.let { spot ->
                    lat = spot.lat
                    lng = spot.lng
                    title = spot.title.ifBlank { "Saved Spot" }
                    subtitle = spot.address
                }
            } else if (fallbackLat != 0.0 || fallbackLng != 0.0) {
                lat = fallbackLat
                lng = fallbackLng
                title = "Active Spot"
                subtitle = prefs.getString("current_address", "") ?: ""
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
        prefs = prefs,
        onBack = onBack,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun SavedSpotDetailRoute(
    spotId: Int,
    dao: LocationDao,
    prefs: android.content.SharedPreferences,
    navController: NavHostController,
    onDismiss: () -> Unit,
    onShareRequest: (ShareSpotPayload) -> Unit
) {
    var spot by remember(spotId) { mutableStateOf<LocationSpot?>(null) }
    var isLoading by remember(spotId) { mutableStateOf(true) }
    var showEditDialog by remember(spotId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(spotId) {
        spot = withContext(Dispatchers.IO) {
            dao.getSpotById(spotId)
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
            spot = loaded.copy(locationDetails = updatedNotes)
        },
        onNavigateToCompass = {
            navController.navigateToCompass(spotId = loaded.id)
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
            currentNotes = loaded.locationDetails,
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
                    // its own observeSpotById query, but that never flows back into this outer
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
                    locationDetails = newNotes,
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
