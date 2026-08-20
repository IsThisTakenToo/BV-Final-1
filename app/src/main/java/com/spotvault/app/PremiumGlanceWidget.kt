package com.spotvault.app

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.runtime.produceState
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.action.actionRunCallback

data class PremiumHistoryEntry(
    val spot: LocationSpot,
    val vehicleName: String?
)

data class PremiumWidgetState(
    val theme: GlanceWidgetTheme,
    val isPremium: Boolean,
    val isTracking: Boolean,
    /** When true, this widget renders [PremiumAppLockedContent] instead of any real tracking/
     * recent-spot content — a home-screen widget has no authentication of its own, so showing a
     * tracked spot's photo/address (or the Recent list) here would leak exactly what App Lock
     * exists to hide, with no unlock required to see it. */
    val appLockEnabled: Boolean = false,
    /** Newest first (DB order) — [PremiumDefaultContent] reverses this for display so the most
     * recent entry lands at the bottom of the list, closest to the button cluster. */
    val recentSpots: List<PremiumHistoryEntry>,
    val autoParkEnabled: Boolean,
    val motionEnabled: Boolean,
    val trackingPhotoPath: String,
    val trackingAddress: String,
    val trackingLat: Double,
    val trackingLng: Double,
    /** Yin Yang is explicitly black & white — its own theme colors already read correctly
     * against each other, so the tracking screen's forced-white text/icon treatment (see
     * [PremiumTrackingContent]) is skipped for it rather than overriding a deliberately
     * monochrome theme's own choices. */
    val isYinYangTheme: Boolean = false,
    /** Cached last-known location (not a live GPS subscription) used only to show a rough
     * distance-away label on each Recent row — null when unavailable/no permission, in which
     * case the row simply omits the label. */
    val userLat: Double? = null,
    val userLng: Double? = null,
    val distanceUnit: String = DEFAULT_DISTANCE_UNIT
)

private const val PREMIUM_HISTORY_LIMIT = 3

class PremiumGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val context = LocalContext.current
            val prefs = currentState<Preferences>()

            val initialPrefs = remember { context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE) }
            val revision = prefs[KEY_REVISION] ?: 0L
            val themeCache = prefs[WidgetGlanceThemeKeys.THEME_CACHE] ?: ""
            val isPremium = prefs[KEY_IS_PREMIUM] ?: isPremiumUnlocked(initialPrefs)
            val isTracking = prefs[KEY_IS_TRACKING] ?: ActiveTrackingHelper.isActive(context)
            // Read live, not cached through Glance prefs like the tracking snapshot fields below —
            // this is a simple, rarely-changed toggle, not something that needs a torn-read-safe
            // snapshot the way photo/address/lat/lng (captured together for one tracking session)
            // do.
            val appLockEnabled = initialPrefs.getBoolean(APP_LOCK_ENABLED_PREF, false)
            val autoParkEnabled = prefs[KEY_AUTO_PARK_ENABLED] ?: isAutoParkEnabled(initialPrefs)
            val motionEnabled = prefs[KEY_MOTION_ENABLED] ?: isMotionAutoParkEnabled(initialPrefs)
            val trackingPhotoPath = prefs[KEY_TRACKING_PHOTO_PATH] ?: (if (isTracking) initialPrefs.getString("photo_path", "") ?: "" else "")
            val trackingAddress = prefs[KEY_TRACKING_ADDRESS] ?: (if (isTracking) initialPrefs.getString("current_address", "") ?: "" else "")
            val trackingLat = prefs[KEY_TRACKING_LAT] ?: (if (isTracking) ActiveTrackingHelper.pinnedCoordinates(context)?.first ?: 0.0 else 0.0)
            val trackingLng = prefs[KEY_TRACKING_LNG] ?: (if (isTracking) ActiveTrackingHelper.pinnedCoordinates(context)?.second ?: 0.0 else 0.0)

            val theme = remember(revision, themeCache) {
                try {
                    GlanceThemeManager.load(context, prefs)
                } catch (_: Exception) {
                    GlanceThemeManager.defaultTheme()
                }
            }

            val recentSpotsState = produceState(
                initialValue = emptyList<PremiumHistoryEntry>(),
                revision, isPremium, isTracking, appLockEnabled
            ) {
                if (isPremium && !isTracking && !appLockEnabled) {
                    // A thrown exception here (disk I/O hiccup, transient Room error) would
                    // otherwise propagate out of provideGlance and crash-loop the widget into
                    // "Problem loading widget" — falling back to an empty list just means this
                    // refresh shows no recent spots instead, and the next successful refresh
                    // repaints normally.
                    value = withContext(Dispatchers.IO) {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            val spots = db.locationDao().getRecentVaultSpots(PREMIUM_HISTORY_LIMIT)
                            val vehicleDao = db.vehicleDao()
                            val vehicleNames = mutableMapOf<Int, String?>()
                            spots.map { spot ->
                                val vehicleName = spot.vehicleId?.let { id ->
                                    vehicleNames.getOrPut(id) { vehicleDao.getById(id)?.name }
                                }
                                PremiumHistoryEntry(spot, vehicleName)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("PremiumGlanceWidget", "Failed to load recent spots", e)
                            emptyList()
                        }
                    }
                } else {
                    value = emptyList()
                }
            }

            // Cached last-known location, fetched once per widget refresh — not a live/continuous
            // subscription — solely to show a rough "X away" label on Recent rows. Rejects a
            // fix older than 10 minutes rather than showing a distance computed from a location
            // that old — renderHistoryRowBitmap already omits the label entirely when this is
            // null, so a stale/missing fix here means the label just doesn't render.
            val userLocationState = produceState(
                initialValue = null as Pair<Double, Double>?,
                key1 = isPremium,
                key2 = isTracking
            ) {
                value = if (isPremium && !isTracking) fetchLastKnownLocationIfFresh(context, 10L * 60 * 1000) else null
            }
            val distanceUnit = remember { loadDistanceUnitFromPrefs(initialPrefs) }

            val state = PremiumWidgetState(
                theme = theme,
                isPremium = isPremium,
                isTracking = isTracking,
                appLockEnabled = appLockEnabled,
                recentSpots = recentSpotsState.value,
                autoParkEnabled = autoParkEnabled,
                motionEnabled = motionEnabled,
                trackingPhotoPath = trackingPhotoPath,
                trackingAddress = trackingAddress,
                trackingLat = trackingLat,
                trackingLng = trackingLng,
                isYinYangTheme = WidgetThemeHelper.effectiveThemeId(initialPrefs) == "yin_yang",
                userLat = userLocationState.value?.first,
                userLng = userLocationState.value?.second,
                distanceUnit = distanceUnit
            )

            PremiumWidgetContent(state)
        }
    }

    companion object {
        val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        val KEY_IS_TRACKING = booleanPreferencesKey("is_tracking")
        val KEY_AUTO_PARK_ENABLED = booleanPreferencesKey("auto_park_enabled")
        val KEY_MOTION_ENABLED = booleanPreferencesKey("motion_enabled")
        val KEY_TRACKING_PHOTO_PATH = stringPreferencesKey("tracking_photo_path")
        val KEY_TRACKING_ADDRESS = stringPreferencesKey("tracking_address")
        val KEY_TRACKING_LAT = doublePreferencesKey("tracking_lat")
        val KEY_TRACKING_LNG = doublePreferencesKey("tracking_lng")
        val KEY_REVISION = longPreferencesKey("widget_revision")
        suspend fun loadPremiumWidgetState(context: Context): PremiumWidgetState {
            val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
            val theme = try {
                GlanceThemeManager.load(context)
            } catch (_: Exception) {
                GlanceThemeManager.defaultTheme()
            }
            val isPremium = isPremiumUnlocked(prefs)
            val isTracking = ActiveTrackingHelper.isActive(context)
            val appLockEnabled = prefs.getBoolean(APP_LOCK_ENABLED_PREF, false)
            val recent = if (isPremium && !isTracking && !appLockEnabled) {
                withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(context)
                    val spots = db.locationDao().getRecentVaultSpots(PREMIUM_HISTORY_LIMIT)
                    val vehicleDao = db.vehicleDao()
                    val vehicleNames = mutableMapOf<Int, String?>()
                    spots.map { spot ->
                        val vehicleName = spot.vehicleId?.let { id ->
                            vehicleNames.getOrPut(id) { vehicleDao.getById(id)?.name }
                        }
                        PremiumHistoryEntry(spot, vehicleName)
                    }
                }
            } else {
                emptyList()
            }
            val coords = if (isTracking) ActiveTrackingHelper.pinnedCoordinates(context) else null
            return PremiumWidgetState(
                theme = theme,
                isPremium = isPremium,
                isTracking = isTracking,
                appLockEnabled = appLockEnabled,
                recentSpots = recent,
                autoParkEnabled = isAutoParkEnabled(prefs),
                motionEnabled = isMotionAutoParkEnabled(prefs),
                trackingPhotoPath = if (isTracking) prefs.getString("photo_path", "") ?: "" else "",
                trackingAddress = if (isTracking) prefs.getString("current_address", "") ?: "" else "",
                trackingLat = coords?.first ?: 0.0,
                trackingLng = coords?.second ?: 0.0,
                isYinYangTheme = WidgetThemeHelper.effectiveThemeId(prefs) == "yin_yang"
            )
        }
    }
}

class PremiumGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PremiumGlanceWidget()
}

private val PremiumWidgetGap = 4.dp
private val PremiumToggleHeight = 58.dp
private val PremiumActionHeight = 96.dp
private val PremiumCompactHeight = 52.dp
private val PremiumHistoryMinHeight = 56.dp
private val PremiumHistoryMaxHeight = 150.dp
private val PremiumFooterHeight = 36.dp
private val PremiumSectionLabelHeight = 24.dp
private val PremiumPhotoMinHeight = 72.dp
private val PremiumPhotoMaxHeight = 320.dp

// Tracking screen's action cluster: two rows of two — Compass + Show Map on top, Found +
// Navigate (the two actions that actually end a tracking session) below, each pair sharing its
// row at equal width via premiumActionSlot's defaultWeight(), same as every other 2-up row in
// this widget. Found/Navigate used to each get their own full-width row instead — besides eating
// far more vertical space than two half-width rows need, the bitmap those buttons render into was
// sized via premiumSlotWidthPx(columns = 1), whose 56–180dp clamp (tuned for the narrow slots
// columns=2/3/4 actually produce) let a full-width slot request a bitmap far narrower than the
// box Glance actually laid the button out in; Image's ContentScale.FillBounds then stretched that
// undersized bitmap out to fill the real width, which is what made the label read as "massively
// stretched." Slotting Found/Navigate into the same columns=2 width Compass/Show Map already use
// avoids the mismatch entirely, not just the vertical space. Deliberately its own smaller constant
// rather than reusing the 96dp PremiumActionHeight (that one's tuned for a single row of 4).
private val PremiumTrackingPrimaryHeight = 64.dp
private val PremiumTrackingSubActionHeight = 56.dp
private val PremiumTrackingClusterHeight =
    PremiumTrackingSubActionHeight + 6.dp + PremiumTrackingPrimaryHeight

/** Reads the widget's live *content* bounds — [LocalSize] minus [GlanceThemedBackground]'s own
 * 6dp-per-side inset — so every size calculation below reflects the area content actually gets
 * placed in, not the widget's raw outer bounds. Only accurate under [SizeMode.Exact] or resize. */
@Composable
private fun premiumWidgetWidthDp(): Float = (LocalSize.current.width - 12.dp).value.coerceAtLeast(0f)

@Composable
private fun premiumWidgetHeightDp(): Float = (LocalSize.current.height - 12.dp).value.coerceAtLeast(0f)

/** Width of one equal slot in an N-column row, derived from the live widget size. */
@Composable
private fun premiumSlotWidthPx(columns: Int): Int {
    val context = LocalContext.current
    val widgetWidth = premiumWidgetWidthDp().dp
    val slots = columns.coerceAtLeast(1)
    val available = widgetWidth - PremiumWidgetGap * (slots - 1)
    val slotDp = (available / slots).value.coerceIn(56f, 180f)
    return WidgetThemeHelper.dpToPx(context, slotDp)
}

/** Whether there's enough vertical room to also show the secondary row without cramming the
 * button cluster — hidden rather than squeezed on short widgets. */
@Composable
private fun premiumShowSecondaryRow(): Boolean = premiumWidgetHeightDp().dp >= 240.dp

// "Open Vault" is the only way to reach the Vault from this widget, so it no longer requires
// there to be history first — an empty vault still needs a way in.
@Composable
private fun premiumShowFooter(): Boolean = premiumWidgetHeightDp().dp >= 230.dp

/** Height given to the tracking hero (photo or placeholder) — leftover after the Compass/Show
 * Map row plus the stacked Found and Navigate rows below it. */
@Composable
private fun premiumHeroHeight(): Dp {
    val total = premiumWidgetHeightDp().dp
    val reserved = PremiumTrackingClusterHeight + 4.dp
    return (total - reserved).coerceIn(PremiumPhotoMinHeight, PremiumPhotoMaxHeight)
}

@Composable
private fun PremiumWidgetContent(state: PremiumWidgetState) {
    GlanceThemedBackground(theme = state.theme) {
        when {
            !state.isPremium -> PremiumLockedContent(state.theme)
            state.appLockEnabled -> PremiumAppLockedContent(state.theme)
            state.isTracking -> PremiumTrackingContent(state)
            else -> PremiumDefaultContent(state)
        }
    }
}

@Composable
private fun PremiumLockedContent(theme: GlanceWidgetTheme) {
    PremiumLockedWidgetContent(
        theme = theme,
        widthDp = premiumWidgetWidthDp().coerceAtLeast(240f),
        heightDp = premiumWidgetHeightDp().coerceAtLeast(160f)
    )
}

@Composable
private fun PremiumAppLockedContent(theme: GlanceWidgetTheme) {
    AppLockedWidgetContent(
        theme = theme,
        widthDp = premiumWidgetWidthDp().coerceAtLeast(160f),
        heightDp = premiumWidgetHeightDp().coerceAtLeast(120f)
    )
}

@Composable
private fun PremiumTrackingContent(state: PremiumWidgetState) {
    val context = LocalContext.current
    val subSlotWidthPx = premiumSlotWidthPx(columns = 2)
    val widgetWidthPx = WidgetThemeHelper.dpToPx(context, premiumWidgetWidthDp())
    val heroHeight = premiumHeroHeight()
    val heroHeightPx = WidgetThemeHelper.dpToPx(context, heroHeight.value)
    // Was hardcoded to a flat white-on-primary/black-on-accent split regardless of theme — fine
    // for a dark primary/bright accent pairing, but wrong the moment a theme's Primary is itself
    // bright (e.g. a light green), where every other button on that same color (Snap, Quick Pin)
    // already correctly renders a dark icon via this exact palette.onPrimary/onAccent contrast
    // computation. Using it here too instead of white/black keeps Compass/Found in step with
    // every other same-colored button in the theme, not just Yin Yang's.
    val white = android.graphics.Color.WHITE
    val foundFg = state.theme.palette.onPrimary
    val compassFg = state.theme.palette.onPrimary
    val navigateFg = state.theme.palette.onAccent
    val mapFg = state.theme.palette.onAccent
    val logoColor = if (state.isYinYangTheme) state.theme.palette.onSurface else white
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        if (state.trackingPhotoPath.isNotBlank()) {
            val photo = remember(
                state.trackingPhotoPath,
                state.trackingAddress,
                state.trackingLat,
                state.trackingLng,
                widgetWidthPx,
                heroHeightPx,
                state.theme.cacheKey(),
                logoColor
            ) {
                PremiumWidgetRenderer.renderTrackingPhotoBitmap(
                    context,
                    state.trackingPhotoPath,
                    state.theme,
                    widgetWidthPx,
                    heroHeightPx,
                    address = state.trackingAddress,
                    lat = state.trackingLat,
                    lng = state.trackingLng,
                    overlayColorArgb = logoColor
                )
            }
            photo?.let {
                Image(
                    provider = ImageProvider(it),
                    contentDescription = "Tracking photo",
                    modifier = GlanceModifier.fillMaxWidth().height(heroHeight)
                )
            }
        } else {
            val infoCard = remember(
                state.trackingAddress,
                state.trackingLat,
                state.trackingLng,
                widgetWidthPx,
                heroHeightPx,
                state.theme.cacheKey(),
                logoColor
            ) {
                PremiumWidgetRenderer.renderTrackingInfoCardBitmap(
                    context,
                    state.theme,
                    state.trackingAddress,
                    state.trackingLat,
                    state.trackingLng,
                    widgetWidthPx,
                    heroHeightPx,
                    logoColorArgb = logoColor,
                    overlayColorArgb = logoColor
                )
            }
            Image(
                provider = ImageProvider(infoCard),
                contentDescription = "Tracking location",
                modifier = GlanceModifier.fillMaxWidth().height(heroHeight)
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        // Compass + Show Map share a row at half the height of Found/Navigate below — secondary
        // actions, sized and positioned to read that way instead of competing equally.
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(PremiumTrackingSubActionHeight)
        ) {
            GlanceThemedActionButton(
                label = "Compass",
                theme = state.theme,
                backgroundArgb = state.theme.palette.primary,
                foregroundArgb = compassFg,
                icon = WidgetThemeHelper.QuickActionIcon.COMPASS,
                modifier = premiumActionSlot(PremiumTrackingSubActionHeight),
                slotWidthPx = subSlotWidthPx,
                buttonHeightDp = PremiumTrackingSubActionHeight,
                expandVertically = false,
                compactLabel = false,
                onClick = actionStartActivity(
                    PremiumWidgetIntents.openCompass(context, state.trackingLat, state.trackingLng)
                )
            )
            GlanceThemedActionButton(
                // A literal space here makes the button-label renderer treat this as a two-word,
                // two-line label — sized to fit two stacked lines in this row's short height, which
                // is why "Show Map" rendered far tinier than "Compass" right next to it despite
                // both using identical button params. A non-breaking space keeps it as one line,
                // sized the same way "Compass" already is.
                label = "Show Map",
                theme = state.theme,
                backgroundArgb = state.theme.palette.accent,
                foregroundArgb = mapFg,
                icon = WidgetThemeHelper.QuickActionIcon.MAP,
                modifier = premiumActionSlot(PremiumTrackingSubActionHeight),
                slotWidthPx = subSlotWidthPx,
                buttonHeightDp = PremiumTrackingSubActionHeight,
                expandVertically = false,
                compactLabel = false,
                onClick = actionStartActivity(showMapIntent(context, state.trackingLat, state.trackingLng, state.trackingAddress))
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        // Found and Navigate — the two actions that actually end a tracking session — share a row
        // at equal width, same as Compass/Show Map above rather than each claiming a full-width
        // row of their own (see the comment on PremiumTrackingClusterHeight for why that also
        // fixes the stretched-label bug, not just the vertical space).
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(PremiumTrackingPrimaryHeight)
        ) {
            GlanceThemedActionButton(
                label = "Found",
                theme = state.theme,
                backgroundArgb = state.theme.palette.primary,
                foregroundArgb = foundFg,
                icon = WidgetThemeHelper.QuickActionIcon.FOUND,
                modifier = premiumActionSlot(PremiumTrackingPrimaryHeight),
                slotWidthPx = subSlotWidthPx,
                buttonHeightDp = PremiumTrackingPrimaryHeight,
                expandVertically = false,
                compactLabel = false,
                onClick = actionRunCallback<FoundActionCallback>()
            )
            GlanceThemedActionButton(
                label = "Navigate",
                theme = state.theme,
                backgroundArgb = state.theme.palette.accent,
                foregroundArgb = navigateFg,
                icon = WidgetThemeHelper.QuickActionIcon.NAVIGATE,
                modifier = premiumActionSlot(PremiumTrackingPrimaryHeight),
                slotWidthPx = subSlotWidthPx,
                buttonHeightDp = PremiumTrackingPrimaryHeight,
                expandVertically = false,
                compactLabel = false,
                onClick = actionStartActivity(relayIntent(context, QuickActionRelayActivity.ACTION_NAVIGATE))
            )
        }
    }
}

/**
 * Vault entries live at the top (the thing worth looking at); every action button lives in one
 * cluster pinned to the bottom (the thing worth reaching for with a thumb) — swapped from the
 * old top-to-bottom "toggles, primary buttons, secondary buttons, history" stack, which put the
 * least useful controls first and buried the history at the bottom.
 */
@Composable
private fun PremiumDefaultContent(state: PremiumWidgetState) {
    val context = LocalContext.current
    val twoColWidthPx = premiumSlotWidthPx(columns = 2)
    val widgetWidthPx = WidgetThemeHelper.dpToPx(context, premiumWidgetWidthDp())
    val showSecondary = premiumShowSecondaryRow()

    val desiredHistoryCount = state.recentSpots.size.coerceAtMost(PREMIUM_HISTORY_LIMIT)
    val hasHistory = desiredHistoryCount > 0
    val showFooter = premiumShowFooter()

    // Everything the history section needs to share the widget with: the button cluster at the
    // bottom (toggle row always, secondary row only when premiumShowSecondaryRow() allows it,
    // primary row always) plus its own section label and footer link, if shown.
    val clusterHeight = PremiumToggleHeight.value + 4f +
        (if (showSecondary) PremiumCompactHeight.value + 6f else 0f) +
        PremiumActionHeight.value + 6f
    val labelHeight = if (hasHistory) PremiumSectionLabelHeight.value + 2f else 0f
    val footerHeight = if (showFooter) PremiumFooterHeight.value + 3f else 0f
    val availableForHistory = (premiumWidgetHeightDp() - clusterHeight - labelHeight - footerHeight).coerceAtLeast(0f)
    val maxRowsThatFit = if (desiredHistoryCount == 0) {
        0
    } else {
        (availableForHistory / (PremiumHistoryMinHeight.value + 3f)).toInt().coerceAtLeast(0)
    }
    val historyRowCount = desiredHistoryCount.coerceAtMost(maxRowsThatFit)
    // Rows grow to fill whatever's left over instead of sitting at one small fixed height — a
    // couple of recent spots on a tall widget get big, easy-to-see thumbnails instead of the
    // same cramped 42dp row a five-entry list would use.
    val historyRowHeight = if (historyRowCount > 0) {
        (availableForHistory / historyRowCount - 3f).coerceIn(PremiumHistoryMinHeight.value, PremiumHistoryMaxHeight.value).dp
    } else {
        PremiumHistoryMinHeight
    }
    // Newest-first from the DB, reversed here so the most recent entry sits at the bottom of the
    // list — right next to the button cluster it's visually leading into — rather than at the top.
    val visibleHistory = state.recentSpots.take(historyRowCount).reversed()

    Column(modifier = GlanceModifier.fillMaxSize()) {
        // Recent starts at the top — leftover stretch grows the history rows (up to max height)
        // and any remainder sits between Open Vault and the button cluster, not as empty space
        // above the section label.
        Column(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
            if (historyRowCount > 0) {
                SectionLabel(text = "Recent", theme = state.theme, widthPx = widgetWidthPx)
                visibleHistory.forEach { entry ->
                    PremiumHistoryRow(
                        entry = entry,
                        theme = state.theme,
                        rowWidthPx = widgetWidthPx,
                        renderHeightHint = historyRowHeight,
                        userLat = state.userLat,
                        userLng = state.userLng,
                        distanceUnit = state.distanceUnit,
                        modifier = GlanceModifier.fillMaxWidth().height(historyRowHeight).padding(vertical = 1.dp),
                        onClick = actionStartActivity(PremiumWidgetIntents.openSpot(context, entry.spot.id))
                    )
                }
                if (showFooter) {
                    VaultFooterButton(theme = state.theme, widthPx = widgetWidthPx, context = context)
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
            } else {
                Column(
                    modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.glance.text.Text(
                        text = "Your Vault is empty",
                        style = androidx.glance.text.TextStyle(
                            color = state.theme.mutedProvider(),
                            fontWeight = androidx.glance.text.FontWeight.Medium
                        )
                    )
                }
                if (showFooter) {
                    VaultFooterButton(theme = state.theme, widthPx = widgetWidthPx, context = context)
                }
            }
        }

        // The inter-row gap is a Spacer, not .padding(bottom = …) on the row itself — that padding
        // was being applied *inside* the row's own fixed .height(), shrinking its usable content
        // area while the button inside still asked for the row's full nominal height. The mismatch
        // meant the bottom few dp of every toggle/chip button was clipped by its own row's bounds
        // — the actual "buttons cut off at the bottom" bug, distinct from the earlier page-level
        // overflow issue.
        // Icon-only BT / Motion — compact badges centered in the row (equal spacers on the sides
        // and between), so they read as status chips rather than full-width text buttons.
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(PremiumToggleHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = GlanceModifier.defaultWeight())
            // Share (Quick Share's own action/icon) | BT Auto | Motion — evenly spaced and
            // centered as one group, colors alternating (Share matches Motion's accent, BT Auto
            // keeps primary) so the two ends of the row match rather than all three or the outer
            // two clashing with the middle one.
            PremiumToggleChip(
                label = "Share",
                active = true,
                theme = state.theme,
                themeColor = state.theme.palette.accent,
                onThemeColor = state.theme.palette.onAccent,
                icon = PremiumWidgetRenderer.ChipIcon.SHARE,
                contentDescription = "Quick Share",
                // No override needed anymore — renderToggleChipBitmap's "white icon when active"
                // rule now uses onThemeColorArgb (state.theme.palette.onAccent, passed above)
                // instead of a hardcoded white, so it already resolves to the theme-correct
                // contrast color on its own, the same fix applied to Compass/Found/BT Auto/Motion.
                onClick = actionStartActivity(relayIntent(context, QuickActionRelayActivity.ACTION_SHARE))
            )
            Spacer(modifier = GlanceModifier.width(20.dp))
            PremiumToggleChip(
                label = "BT Auto",
                active = state.autoParkEnabled,
                theme = state.theme,
                themeColor = state.theme.palette.primary,
                onThemeColor = state.theme.palette.onPrimary,
                icon = PremiumWidgetRenderer.ChipIcon.BLUETOOTH,
                onClick = actionRunCallback<ToggleBluetoothActionCallback>()
            )
            Spacer(modifier = GlanceModifier.width(20.dp))
            PremiumToggleChip(
                label = "Motion",
                active = state.motionEnabled,
                theme = state.theme,
                themeColor = state.theme.palette.accent,
                onThemeColor = state.theme.palette.onAccent,
                icon = PremiumWidgetRenderer.ChipIcon.MOTION,
                onClick = actionRunCallback<ToggleMotionActionCallback>()
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
        }
        // A weighted spacer here (splitting leftover space with the history section's own
        // trailing gap above the toggle row) ended up making this gap huge on taller widgets —
        // a small fixed gap instead, bigger than the original nearly-invisible 4dp but bounded.
        Spacer(modifier = GlanceModifier.height(14.dp))
        // Same real button shape/theme system as the primary row (GlanceThemedActionButton, the
        // app's actual button style — capsule/chamfer/wild/etc., not a simplified pill chip) —
        // Snap pairs with Primary like Quick Pin below it, Pin pairs with the accent like Quick
        // Track.
        if (showSecondary) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(PremiumCompactHeight)
            ) {
                GlanceThemedActionButton(
                    label = "Snap",
                    theme = state.theme,
                    backgroundArgb = state.theme.palette.primary,
                    foregroundArgb = state.theme.palette.onPrimary,
                    icon = WidgetThemeHelper.QuickActionIcon.CAMERA,
                    modifier = premiumActionSlot(PremiumCompactHeight),
                    slotWidthPx = twoColWidthPx,
                    buttonHeightDp = PremiumCompactHeight,
                    expandVertically = false,
                    compactLabel = false,
                    labelEmphasis = true,
                    primaryAction = false,
                    premiumPolish = true,
                    onClick = actionStartActivity(PremiumWidgetIntents.widgetAction(context, PremiumWidgetIntents.ACTION_SNAP))
                )
                GlanceThemedActionButton(
                    label = "Pin",
                    theme = state.theme,
                    backgroundArgb = state.theme.palette.accent,
                    // Black icon + text together — matches how the in-app "Pin" button (teal-
                    // dominant, OnTeal content color) actually looks: one consistent dark color
                    // on the bright fill, not a white icon paired with black text.
                    foregroundArgb = android.graphics.Color.BLACK,
                    icon = WidgetThemeHelper.QuickActionIcon.PIN,
                    modifier = premiumActionSlot(PremiumCompactHeight),
                    slotWidthPx = twoColWidthPx,
                    buttonHeightDp = PremiumCompactHeight,
                    expandVertically = false,
                    compactLabel = false,
                    labelEmphasis = true,
                    primaryAction = false,
                    premiumPolish = true,
                    onClick = actionStartActivity(PremiumWidgetIntents.widgetAction(context, PremiumWidgetIntents.ACTION_PIN_ONLY))
                )
            }
            Spacer(modifier = GlanceModifier.height(6.dp))
        }
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(PremiumActionHeight)
        ) {
            GlanceThemedActionButton(
                label = "Quick Pin",
                theme = state.theme,
                backgroundArgb = state.theme.palette.primary,
                foregroundArgb = state.theme.palette.onPrimary,
                icon = WidgetThemeHelper.QuickActionIcon.PIN_DROP,
                modifier = premiumActionSlot(PremiumActionHeight),
                slotWidthPx = twoColWidthPx,
                buttonHeightDp = PremiumActionHeight,
                expandVertically = false,
                compactLabel = false,
                labelEmphasis = true,
                primaryAction = true,
                premiumPolish = true,
                onClick = actionStartActivity(relayIntent(context, QuickActionRelayActivity.ACTION_PIN))
            )
            GlanceThemedActionButton(
                label = "Quick Track",
                theme = state.theme,
                // Black icon + text together on this bright accent (gold) fill, matching Pin.
                foregroundArgb = android.graphics.Color.BLACK,
                backgroundArgb = state.theme.palette.accent,
                icon = WidgetThemeHelper.QuickActionIcon.TRACK,
                modifier = premiumActionSlot(PremiumActionHeight),
                slotWidthPx = twoColWidthPx,
                buttonHeightDp = PremiumActionHeight,
                expandVertically = false,
                compactLabel = false,
                labelEmphasis = true,
                primaryAction = true,
                premiumPolish = true,
                onClick = actionStartActivity(relayIntent(context, QuickActionRelayActivity.ACTION_TRACK))
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, theme: GlanceWidgetTheme, widthPx: Int) {
    val context = LocalContext.current
    val heightPx = WidgetThemeHelper.dpToPx(context, PremiumSectionLabelHeight.value)
    val bitmap = remember(text, theme.cacheKey(), widthPx, heightPx) {
        PremiumWidgetRenderer.renderSectionLabelBitmap(
            context,
            text,
            android.graphics.Color.WHITE,
            widthPx,
            heightPx
        )
    }
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = null,
        modifier = GlanceModifier.fillMaxWidth().height(PremiumSectionLabelHeight)
    )
}

@Composable
private fun VaultFooterButton(theme: GlanceWidgetTheme, widthPx: Int, context: Context) {
    val heightPx = WidgetThemeHelper.dpToPx(context, PremiumFooterHeight.value)
    val bitmap = remember(theme.cacheKey(), widthPx, heightPx) {
        PremiumWidgetRenderer.renderVaultFooterButtonBitmap(context, theme, widthPx, heightPx)
    }
    BoxClickable(
        bitmap = bitmap,
        description = "Open Vault",
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(PremiumFooterHeight)
            .padding(top = 2.dp),
        onClick = actionStartActivity(PremiumWidgetIntents.openVault(context))
    )
}

@Composable
private fun PremiumToggleChip(
    label: String,
    active: Boolean,
    theme: GlanceWidgetTheme,
    themeColor: Int,
    onThemeColor: Int,
    icon: PremiumWidgetRenderer.ChipIcon,
    onClick: Action,
    contentDescription: String = "$label: ${if (active) "On" else "Off"}",
    iconColorOverrideArgb: Int? = null
) {
    val context = LocalContext.current
    val sizePx = WidgetThemeHelper.dpToPx(context, PremiumToggleHeight.value)
    val bitmap = remember(label, active, theme.cacheKey(), themeColor, onThemeColor, sizePx, icon, iconColorOverrideArgb) {
        PremiumWidgetRenderer.renderToggleChipBitmap(
            context, label, active,
            themeColor, onThemeColor,
            theme.palette.surface, theme.palette.muted,
            sizePx, sizePx,
            icon = icon,
            iconColorOverrideArgb = iconColorOverrideArgb
        )
    }
    BoxClickable(
        bitmap,
        contentDescription,
        GlanceModifier.size(PremiumToggleHeight),
        onClick
    )
}

@Composable
private fun PremiumHistoryRow(
    entry: PremiumHistoryEntry,
    theme: GlanceWidgetTheme,
    rowWidthPx: Int,
    renderHeightHint: Dp,
    userLat: Double?,
    userLng: Double?,
    distanceUnit: String,
    modifier: GlanceModifier,
    onClick: Action
) {
    val context = LocalContext.current
    val spot = entry.spot
    val heightPx = WidgetThemeHelper.dpToPx(context, renderHeightHint.value)
    val bitmap = remember(spot.id, spot.timestamp, spot.title, spot.address, spot.imagePath, entry.vehicleName, theme.cacheKey(), rowWidthPx, heightPx, userLat, userLng, distanceUnit) {
        PremiumWidgetRenderer.renderHistoryRowBitmap(context, entry, theme, rowWidthPx, heightPx, userLat, userLng, distanceUnit)
    }
    BoxClickable(
        bitmap = bitmap,
        description = spot.title.ifBlank { "Saved spot" },
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
private fun BoxClickable(
    bitmap: android.graphics.Bitmap,
    description: String,
    modifier: GlanceModifier,
    onClick: Action
) {
    androidx.glance.layout.Box(
        modifier = modifier.clickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = description,
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}

/** Same "geo:" pin-drop the in-app Show Map button uses — built directly from the tracked
 * spot's own coordinates rather than routing through a live-GPS-resolving relay activity, since
 * the coordinates we want to show are already known here. With App Lock on, this must not hand
 * the tracked location to an external maps app straight off the home screen with no unlock at
 * all — same bypass TimerService's "Show Map" notification action guards against — so it routes
 * through MainActivity's own App Lock gate instead, same as the Compass button beside it. */
private fun showMapIntent(context: Context, lat: Double, lng: Double, address: String): Intent {
    val prefs = context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
    if (prefs.getBoolean(APP_LOCK_ENABLED_PREF, false)) {
        return PremiumWidgetIntents.openMapsShow(context, lat, lng)
    }
    val label = address.ifBlank { "Tracked Spot" }
    val uri = android.net.Uri.parse("geo:0,0?q=$lat,$lng(${android.net.Uri.encode(label)})")
    return Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

private fun relayIntent(context: Context, action: String): Intent =
    QuickActionRelayActivity.intentForAction(context, action)

private fun RowScope.premiumActionSlot(height: Dp): GlanceModifier =
    GlanceModifier.defaultWeight().height(height).padding(horizontal = 2.dp)
