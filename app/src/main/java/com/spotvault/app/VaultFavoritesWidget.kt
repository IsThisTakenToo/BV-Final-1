package com.spotvault.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            val initialPrefs = remember { context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE) }
            val isPremium = isPremiumUnlocked(initialPrefs)

            val theme = remember(revision, themeCache) {
                try {
                    GlanceThemeManager.load(context, prefs)
                } catch (_: Exception) {
                    GlanceThemeManager.defaultTheme()
                }
            }

            val favoritesState = produceState(
                initialValue = emptyList<VaultFavoriteEntry>(),
                key1 = revision,
                key2 = isPremium
            ) {
                value = if (isPremium) {
                    withContext(Dispatchers.IO) {
                        val db = AppDatabase.getDatabase(context)
                        val spots = db.locationDao().getFavoriteSpots()
                        val vehicleDao = db.vehicleDao()
                        val vehicleNames = mutableMapOf<Int, String?>()
                        spots.map { spot ->
                            val vehicleName = spot.vehicleId?.let { id ->
                                vehicleNames.getOrPut(id) { vehicleDao.getById(id)?.name }
                            }
                            VaultFavoriteEntry(spot, vehicleName)
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
            } else {
                VaultFavoritesContent(
                    theme = theme,
                    favorites = favoritesState.value
                )
            }
        }
    }

    companion object {
        val KEY_REVISION = longPreferencesKey("widget_revision")
    }
}

class VaultFavoritesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VaultFavoritesWidget()
}

private val FavoritesHeaderHeight = 24.dp
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
    favorites: List<VaultFavoriteEntry>
) {
    val context = LocalContext.current
    val widthPx = WidgetThemeHelper.dpToPx(context, vaultFavoritesContentWidthDp())

    GlanceThemedBackground(theme = theme) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            FavoritesHeader(
                theme = theme,
                widthPx = widthPx,
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
    modifier: GlanceModifier
) {
    val context = LocalContext.current
    val heightPx = WidgetThemeHelper.dpToPx(context, FavoritesHeaderHeight.value)
    val bitmap = remember(theme.cacheKey(), widthPx, heightPx) {
        PremiumWidgetRenderer.renderSectionLabelBitmap(
            context,
            "Vault Favorites",
            android.graphics.Color.WHITE,
            widthPx,
            heightPx
        )
    }
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = "Vault Favorites",
        modifier = modifier,
        contentScale = ContentScale.FillBounds
    )
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
