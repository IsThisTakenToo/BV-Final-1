package com.spotvault.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hard cap for Glance list widgets. Glance LazyColumn still materializes bitmapped rows for the
 * binder payload — 80 full-width tablet rows OOMs / blows RemoteViews; ~12 is enough to scroll
 * while staying safe on large Exact-size widgets.
 */
internal const val WIDGET_SPOT_ROW_CAP = 12

/**
 * Home-screen widget: scrollable list of every Vault favorite.
 * Uses the same PreferencesGlanceStateDefinition + revision sync as the other widgets
 * so DB/theme changes refresh the list.
 */
class VaultFavoritesWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val context = LocalContext.current
            val prefs = currentState<Preferences>()
            val revision = prefs[KEY_REVISION] ?: 0L
            val themeCache = prefs[WidgetGlanceThemeKeys.THEME_CACHE] ?: ""
            val sortOrder = prefs[KEY_SORT_ORDER] ?: "newest"
            val initialPrefs = remember { context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE) }
            val isPremium = isPremiumUnlocked(initialPrefs)
            // A home-screen widget has no authentication of its own — listing every Favorite
            // spot's name here regardless of App Lock would leak exactly what App Lock exists to
            // hide, with no unlock required to see it. See PremiumGlanceWidget's matching fix.
            val appLockEnabled = initialPrefs.getBoolean(APP_LOCK_ENABLED_PREF, false)

            val theme = remember(revision, themeCache) {
                try {
                    GlanceThemeManager.load(context, prefs)
                } catch (_: Exception) {
                    GlanceThemeManager.defaultTheme()
                }
            }

            val favoritesState = produceState(
                initialValue = emptyList<VaultFavoriteEntry>(),
                revision, isPremium, sortOrder, appLockEnabled
            ) {
                // Caught rather than left to propagate out of provideGlance — an uncaught Room/IO
                // exception here would crash-loop the widget into "Problem loading widget"
                // instead of just showing an empty refresh.
                value = if (isPremium && !appLockEnabled) {
                    withContext(Dispatchers.IO) {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            val spots = if (sortOrder == "oldest") {
                                db.locationDao().getFavoriteSpotsOldest(WIDGET_SPOT_ROW_CAP)
                            } else {
                                db.locationDao().getFavoriteSpotsNewest(WIDGET_SPOT_ROW_CAP)
                            }
                            val vehicleDao = db.vehicleDao()
                            val vehicleNames = mutableMapOf<Int, String?>()
                            spots.map { spot ->
                                val vehicleName = spot.vehicleId?.let { id ->
                                    vehicleNames.getOrPut(id) { vehicleDao.getById(id)?.name }
                                }
                                VaultFavoriteEntry(spot, vehicleName)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("VaultFavoritesWidget", "Failed to load favorites", e)
                            emptyList()
                        }
                    }
                } else {
                    emptyList()
                }
            }

            if (!isPremium) {
                GlanceThemedBackground(theme = theme) {
                    PremiumLockedWidgetContent(
                        theme = theme,
                        widthDp = vaultFavoritesContentWidthDp().coerceAtLeast(160f),
                        heightDp = vaultFavoritesHeightDp().coerceAtLeast(120f)
                    )
                }
            } else if (appLockEnabled) {
                GlanceThemedBackground(theme = theme) {
                    AppLockedWidgetContent(
                        theme = theme,
                        widthDp = vaultFavoritesContentWidthDp().coerceAtLeast(160f),
                        heightDp = vaultFavoritesHeightDp().coerceAtLeast(120f)
                    )
                }
            } else {
                VaultFavoritesContent(
                    theme = theme,
                    sortOrder = sortOrder,
                    favorites = favoritesState.value
                )
            }
        }
    }

    companion object {
        val KEY_REVISION = longPreferencesKey("widget_revision")
        val KEY_SORT_ORDER = stringPreferencesKey("widget_sort_order")
    }
}

class VaultFavoritesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VaultFavoritesWidget()
}

class ToggleFavoritesSortAction : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: androidx.glance.action.ActionParameters) {
        androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[VaultFavoritesWidget.KEY_SORT_ORDER] ?: "newest"
            prefs[VaultFavoritesWidget.KEY_SORT_ORDER] = if (current == "newest") "oldest" else "newest"
        }
        VaultFavoritesWidget().update(context, glanceId)
    }
}

private val FavoritesHeaderHeight = 24.dp
private val FavoritesSortButtonWidth = 44.dp
private val FavoritesVaultButtonWidth = 54.dp
private val FavoritesHeaderButtonGap = 6.dp
private val FavoritesRowHeight = 56.dp
private val FavoritesRowMaxHeight = 150.dp
private val FavoritesEmptyHeight = 140.dp

@Composable
private fun vaultFavoritesHeightDp(): Float =
    (LocalSize.current.height - 12.dp).value.coerceAtLeast(0f)

@Composable
private fun favoritesRowHeight(): Dp {
    val available = (vaultFavoritesHeightDp() - FavoritesHeaderHeight.value - 8f).coerceAtLeast(0f)
    val visibleRows = 3f
    val perRow = (available / visibleRows - 3f).coerceIn(FavoritesRowHeight.value, FavoritesRowMaxHeight.value)
    return perRow.dp
}

@Composable
private fun vaultFavoritesContentWidthDp(): Float =
    (LocalSize.current.width - 12.dp).value.coerceAtLeast(0f)

@Composable
private fun VaultFavoritesContent(
    theme: GlanceWidgetTheme,
    sortOrder: String,
    favorites: List<VaultFavoriteEntry>
) {
    val context = LocalContext.current
    val widthPx = WidgetThemeHelper.dpToPx(context, vaultFavoritesContentWidthDp())

    GlanceThemedBackground(theme = theme) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            FavoritesHeader(
                theme = theme,
                widthPx = widthPx,
                sortOrder = sortOrder,
                modifier = GlanceModifier.fillMaxWidth().height(FavoritesHeaderHeight)
            )

            if (favorites.isEmpty()) {
                FavoritesEmpty(
                    theme = theme,
                    widthPx = widthPx,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .padding(top = 8.dp)
                )
            } else {
                val rowHeight = favoritesRowHeight()
                LazyColumn(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .padding(top = 4.dp)
                ) {
                    items(
                        items = favorites,
                        itemId = { it.spot.id.toLong() }
                    ) { entry ->
                        FavoriteRow(
                            entry = entry,
                            theme = theme,
                            rowWidthPx = widthPx,
                            rowHeight = rowHeight,
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(rowHeight)
                                .padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesHeader(
    theme: GlanceWidgetTheme,
    widthPx: Int,
    sortOrder: String,
    modifier: GlanceModifier
) {
    val context = LocalContext.current
    val heightPx = WidgetThemeHelper.dpToPx(context, FavoritesHeaderHeight.value)

    val sortButtonWidthPx = WidgetThemeHelper.dpToPx(context, FavoritesSortButtonWidth.value)
    val vaultButtonWidthPx = WidgetThemeHelper.dpToPx(context, FavoritesVaultButtonWidth.value)
    val gapPx = WidgetThemeHelper.dpToPx(context, FavoritesHeaderButtonGap.value)
    // Row is [label][sort button][gap][vault button] — reserve exactly that fixed width, not a
    // guessed value, or the label bitmap gets rendered at the wrong width and FillBounds
    // stretches it against the box Glance actually lays out.
    val labelWidthPx = (widthPx - sortButtonWidthPx - vaultButtonWidthPx - gapPx).coerceAtLeast(1)

    val labelBitmap = remember(theme.cacheKey(), labelWidthPx, heightPx) {
        PremiumWidgetRenderer.renderSectionLabelBitmap(
            context,
            "Vault Favorites",
            android.graphics.Color.WHITE,
            labelWidthPx,
            heightPx
        )
    }

    val sortBitmap = remember(theme.cacheKey(), sortOrder, sortButtonWidthPx, heightPx) {
        PremiumWidgetRenderer.renderSortButtonBitmap(context, android.graphics.Color.WHITE, sortButtonWidthPx, heightPx, sortOrder == "oldest")
    }

    val vaultBitmap = remember(theme.cacheKey(), vaultButtonWidthPx, heightPx) {
        PremiumWidgetRenderer.renderVaultButtonBitmap(context, android.graphics.Color.WHITE, vaultButtonWidthPx, heightPx)
    }

    androidx.glance.layout.Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(labelBitmap),
            contentDescription = "Vault Favorites",
            modifier = GlanceModifier.defaultWeight().height(FavoritesHeaderHeight),
            contentScale = ContentScale.FillBounds
        )
        Image(
            provider = ImageProvider(sortBitmap),
            contentDescription = if (sortOrder == "oldest") "Sorted oldest first, tap to sort newest first" else "Sorted newest first, tap to sort oldest first",
            modifier = GlanceModifier.width(FavoritesSortButtonWidth).height(FavoritesHeaderHeight).clickable(
                androidx.glance.appwidget.action.actionRunCallback<ToggleFavoritesSortAction>()
            ),
            contentScale = ContentScale.Fit
        )
        androidx.glance.layout.Spacer(modifier = GlanceModifier.width(FavoritesHeaderButtonGap))
        Image(
            provider = ImageProvider(vaultBitmap),
            contentDescription = "Open Vault",
            modifier = GlanceModifier.width(FavoritesVaultButtonWidth).height(FavoritesHeaderHeight).clickable(
                actionStartActivity(PremiumWidgetIntents.openVault(context))
            ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun FavoritesEmpty(
    theme: GlanceWidgetTheme,
    widthPx: Int,
    modifier: GlanceModifier
) {
    val context = LocalContext.current
    val heightPx = WidgetThemeHelper.dpToPx(context, FavoritesEmptyHeight.value)
    val bitmap = remember(theme.cacheKey(), widthPx, heightPx) {
        VaultFavoritesRenderer.renderEmptyBitmap(context, theme, widthPx, heightPx)
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "No favorites yet",
            modifier = GlanceModifier.fillMaxWidth().height(FavoritesEmptyHeight),
            contentScale = ContentScale.FillBounds
        )
    }
}

@Composable
private fun FavoriteRow(
    entry: VaultFavoriteEntry,
    theme: GlanceWidgetTheme,
    rowWidthPx: Int,
    rowHeight: Dp,
    modifier: GlanceModifier
) {
    val context = LocalContext.current
    val spot = entry.spot
    val heightPx = WidgetThemeHelper.dpToPx(context, rowHeight.value)
    val bitmap = remember(
        spot.id,
        spot.timestamp,
        spot.title,
        spot.address,
        spot.locationDetails,
        spot.imagePath,
        entry.vehicleName,
        theme.cacheKey(),
        rowWidthPx,
        heightPx
    ) {
        VaultFavoritesRenderer.renderFavoriteRowBitmap(context, entry, theme, rowWidthPx, heightPx)
    }
    Box(
        modifier = modifier.clickable(
            actionStartActivity(PremiumWidgetIntents.openSpot(context, spot.id))
        ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = spot.title.ifBlank { "Favorite spot" },
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}
