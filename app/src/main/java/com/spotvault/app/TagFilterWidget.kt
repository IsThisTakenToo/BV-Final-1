package com.spotvault.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TagWidgetEntry(val spot: LocationSpot, val vehicleName: String?)

/**
 * Home-screen widget: scrollable list of every spot carrying one chosen tag. Reconfigurable —
 * long-press to change which tag it follows via [TagFilterWidgetConfigActivity]. Same
 * PreferencesGlanceStateDefinition + revision-sync pattern as the other three widgets so DB/theme
 * changes refresh it identically (see WidgetThemeHelper.syncGlanceState/refreshAllWidgetsAwait
 * and Database.kt's InvalidationTracker.Observer, both updated to also cover this widget).
 */
class TagFilterWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val context = LocalContext.current
            val prefs = currentState<Preferences>()
            val revision = prefs[KEY_REVISION] ?: 0L
            val themeCache = prefs[WidgetGlanceThemeKeys.THEME_CACHE] ?: ""
            val sortOrder = prefs[KEY_SORT_ORDER] ?: "newest"
            val tagIds = (prefs[KEY_TAG_IDS] ?: emptySet()).mapNotNull { it.toIntOrNull() }
            val initialPrefs = remember { context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE) }
            val isPremium = isPremiumUnlocked(initialPrefs)
            // A home-screen widget has no authentication of its own — listing every tagged spot's
            // name here regardless of App Lock would leak exactly what App Lock exists to hide,
            // with no unlock required to see it. See PremiumGlanceWidget's matching fix.
            val appLockEnabled = initialPrefs.getBoolean(APP_LOCK_ENABLED_PREF, false)

            val theme = remember(revision, themeCache) {
                try {
                    GlanceThemeManager.load(context, prefs)
                } catch (_: Exception) {
                    GlanceThemeManager.defaultTheme()
                }
            }

            // Names are resolved fresh from the tags table on every refresh rather than cached
            // alongside the ids — a tag rename would otherwise leave this widget showing a stale
            // label until reconfigured, exactly the kind of drift storing derived text invites.
            // Both produceState blocks below catch Room/IO failures rather than letting them
            // propagate out of provideGlance — an uncaught exception here would crash-loop the
            // widget into "Problem loading widget" instead of just showing an empty refresh.
            val tagNamesState = produceState(initialValue = emptyList<String>(), tagIds, isPremium, appLockEnabled) {
                value = if (isPremium && tagIds.isNotEmpty() && !appLockEnabled) {
                    withContext(Dispatchers.IO) {
                        try {
                            val tagDao = AppDatabase.getDatabase(context).tagDao()
                            tagIds.mapNotNull { tagDao.getTagById(it)?.name }
                        } catch (e: Exception) {
                            android.util.Log.e("TagFilterWidget", "Failed to load tag names", e)
                            emptyList()
                        }
                    }
                } else {
                    emptyList()
                }
            }

            val entriesState = produceState(
                emptyList<TagWidgetEntry>(),
                revision,
                tagIds,
                isPremium,
                sortOrder,
                appLockEnabled
            ) {
                value = if (!isPremium || tagIds.isEmpty() || appLockEnabled) {
                    emptyList()
                } else {
                    withContext(Dispatchers.IO) {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            val spots = if (sortOrder == "oldest") {
                                db.tagDao().getSpotsForTagsOldest(tagIds, WIDGET_SPOT_ROW_CAP)
                            } else {
                                db.tagDao().getSpotsForTagsNewest(tagIds, WIDGET_SPOT_ROW_CAP)
                            }
                            val vehicleDao = db.vehicleDao()
                            val vehicleNames = mutableMapOf<Int, String?>()
                            spots.map { spot ->
                                val vehicleName = spot.vehicleId?.let { vid ->
                                    vehicleNames.getOrPut(vid) { vehicleDao.getById(vid)?.name }
                                }
                                TagWidgetEntry(spot, vehicleName)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("TagFilterWidget", "Failed to load tag entries", e)
                            emptyList()
                        }
                    }
                }
            }

            if (!isPremium) {
                GlanceThemedBackground(theme = theme) {
                    PremiumLockedWidgetContent(
                        theme = theme,
                        widthDp = tagFilterContentWidthDp().coerceAtLeast(160f),
                        heightDp = tagFilterHeightDp().coerceAtLeast(120f)
                    )
                }
            } else if (appLockEnabled) {
                GlanceThemedBackground(theme = theme) {
                    AppLockedWidgetContent(
                        theme = theme,
                        widthDp = tagFilterContentWidthDp().coerceAtLeast(160f),
                        heightDp = tagFilterHeightDp().coerceAtLeast(120f)
                    )
                }
            } else {
                TagFilterContent(
                    theme = theme,
                    tagNames = tagNamesState.value,
                    entries = entriesState.value,
                    sortOrder = sortOrder,
                    configured = tagIds.isNotEmpty()
                )
            }
        }
    }

    companion object {
        val KEY_REVISION = longPreferencesKey("widget_revision")
        val KEY_TAG_IDS = stringSetPreferencesKey("tag_filter_tag_ids")
        val KEY_SORT_ORDER = stringPreferencesKey("widget_sort_order")
    }
}

class TagFilterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TagFilterWidget()
}

class ToggleTagFilterSortAction : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: androidx.glance.action.ActionParameters) {
        androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[TagFilterWidget.KEY_SORT_ORDER] ?: "newest"
            prefs[TagFilterWidget.KEY_SORT_ORDER] = if (current == "newest") "oldest" else "newest"
        }
        TagFilterWidget().update(context, glanceId)
    }
}

private val TagFilterHeaderHeight = 24.dp
private val TagFilterSortButtonWidth = 44.dp
private val TagFilterVaultButtonWidth = 54.dp
private val TagFilterHeaderButtonGap = 6.dp
private val TagFilterRowHeight = 56.dp
private val TagFilterRowMaxHeight = 150.dp
private val TagFilterEmptyHeight = 140.dp

@Composable
private fun tagFilterHeightDp(): Float =
    (LocalSize.current.height - 12.dp).value.coerceAtLeast(0f)

@Composable
private fun tagFilterRowHeight(): Dp {
    val available = (tagFilterHeightDp() - TagFilterHeaderHeight.value - 8f).coerceAtLeast(0f)
    val visibleRows = 3f
    val perRow = (available / visibleRows - 3f).coerceIn(TagFilterRowHeight.value, TagFilterRowMaxHeight.value)
    return perRow.dp
}

@Composable
private fun tagFilterContentWidthDp(): Float =
    (LocalSize.current.width - 12.dp).value.coerceAtLeast(0f)

/** Fully-formatted header/empty-state label for however many tags are selected — "#work" for
 * one, "#work, #home" for a couple, and a plain count once naming them all would otherwise run
 * long enough to crowd out the rest of the header on a narrow widget. Owns its own "#" styling
 * end to end so callers never need to add one themselves. */
private fun combinedTagLabel(tagNames: List<String>): String = when {
    tagNames.isEmpty() -> ""
    tagNames.size <= 2 -> tagNames.joinToString(", ") { "#$it" }
    else -> "${tagNames.size} tags"
}

@Composable
private fun TagFilterContent(
    theme: GlanceWidgetTheme,
    tagNames: List<String>,
    entries: List<TagWidgetEntry>,
    sortOrder: String,
    configured: Boolean
) {
    val context = LocalContext.current
    val widthPx = WidgetThemeHelper.dpToPx(context, tagFilterContentWidthDp())
    val label = combinedTagLabel(tagNames)

    GlanceThemedBackground(theme = theme) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            TagFilterHeader(
                theme = theme,
                label = if (configured) label else "Tag Filter",
                widthPx = widthPx,
                sortOrder = sortOrder,
                modifier = GlanceModifier.fillMaxWidth().height(TagFilterHeaderHeight)
            )

            if (entries.isEmpty()) {
                TagFilterEmpty(
                    theme = theme,
                    configured = configured,
                    tagName = label,
                    widthPx = widthPx,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .padding(top = 8.dp)
                )
            } else {
                val rowHeight = tagFilterRowHeight()
                LazyColumn(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .padding(top = 4.dp)
                ) {
                    items(
                        items = entries,
                        itemId = { it.spot.id.toLong() }
                    ) { entry ->
                        TagFilterRow(
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
private fun TagFilterHeader(
    theme: GlanceWidgetTheme,
    label: String,
    widthPx: Int,
    sortOrder: String,
    modifier: GlanceModifier
) {
    val context = LocalContext.current
    val heightPx = WidgetThemeHelper.dpToPx(context, TagFilterHeaderHeight.value)

    val sortButtonWidthPx = WidgetThemeHelper.dpToPx(context, TagFilterSortButtonWidth.value)
    val vaultButtonWidthPx = WidgetThemeHelper.dpToPx(context, TagFilterVaultButtonWidth.value)
    val gapPx = WidgetThemeHelper.dpToPx(context, TagFilterHeaderButtonGap.value)
    // Row is [label][sort button][gap][vault button] — reserve exactly that fixed width, not a
    // guessed value, or the label bitmap gets rendered at the wrong width and FillBounds
    // stretches it against the box Glance actually lays out.
    val labelWidthPx = (widthPx - sortButtonWidthPx - vaultButtonWidthPx - gapPx).coerceAtLeast(1)

    val labelBitmap = remember(theme.cacheKey(), label, labelWidthPx, heightPx) {
        PremiumWidgetRenderer.renderSectionLabelBitmap(
            context,
            label,
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
            contentDescription = label,
            modifier = GlanceModifier.defaultWeight().height(TagFilterHeaderHeight),
            contentScale = ContentScale.FillBounds
        )
        Image(
            provider = ImageProvider(sortBitmap),
            contentDescription = if (sortOrder == "oldest") "Sorted oldest first, tap to sort newest first" else "Sorted newest first, tap to sort oldest first",
            modifier = GlanceModifier.width(TagFilterSortButtonWidth).height(TagFilterHeaderHeight).clickable(
                androidx.glance.appwidget.action.actionRunCallback<ToggleTagFilterSortAction>()
            ),
            contentScale = ContentScale.Fit
        )
        androidx.glance.layout.Spacer(modifier = GlanceModifier.width(TagFilterHeaderButtonGap))
        Image(
            provider = ImageProvider(vaultBitmap),
            contentDescription = "Open Vault",
            modifier = GlanceModifier.width(TagFilterVaultButtonWidth).height(TagFilterHeaderHeight).clickable(
                actionStartActivity(PremiumWidgetIntents.openVault(context))
            ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun TagFilterEmpty(
    theme: GlanceWidgetTheme,
    configured: Boolean,
    tagName: String,
    widthPx: Int,
    modifier: GlanceModifier
) {
    val context = LocalContext.current
    val heightPx = WidgetThemeHelper.dpToPx(context, TagFilterEmptyHeight.value)
    val bitmap = remember(theme.cacheKey(), configured, tagName, widthPx, heightPx) {
        TagFilterRenderer.renderEmptyBitmap(context, theme, configured, tagName, widthPx, heightPx)
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = if (configured) "No spots tagged $tagName" else "No tag chosen yet",
            modifier = GlanceModifier.fillMaxWidth().height(TagFilterEmptyHeight),
            contentScale = ContentScale.FillBounds
        )
    }
}

@Composable
private fun TagFilterRow(
    entry: TagWidgetEntry,
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
        spot.isFavorite,
        entry.vehicleName,
        theme.cacheKey(),
        rowWidthPx,
        heightPx
    ) {
        PremiumWidgetRenderer.renderSpotRowBitmap(
            context = context,
            spot = spot,
            vehicleName = entry.vehicleName,
            theme = theme,
            widthPx = rowWidthPx,
            heightPx = heightPx,
            favoriteStar = spot.isFavorite,
            fallbackTitle = "Tagged spot"
        )
    }
    Box(
        modifier = modifier.clickable(
            actionStartActivity(PremiumWidgetIntents.openSpot(context, spot.id))
        ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = spot.title.ifBlank { "Tagged spot" },
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}

/** Bitmap rendering for the Tag Filter Glance widget's empty state — the row bitmap itself is
 * shared directly with [PremiumWidgetRenderer.renderSpotRowBitmap], same as Vault Favorites. */
private object TagFilterRenderer {
    fun renderEmptyBitmap(
        context: Context,
        theme: GlanceWidgetTheme,
        configured: Boolean,
        tagName: String,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val (safeW, safeH) = WidgetThemeHelper.clampWidgetBitmapDims(widthPx, heightPx)
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = safeW.toFloat()
        val h = safeH.toFloat()
        val pad = 8f * density
        val plateRect = RectF(pad, pad, w - pad, h * 0.52f)
        PremiumWidgetRenderer.drawNoPhotoSpotPlaceholder(canvas, context, plateRect, theme, density)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.palette.onSurface
            textSize = (h * 0.125f).coerceIn(13f * density, 18f * density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.palette.muted
            textSize = (h * 0.095f).coerceIn(10f * density, 14f * density)
            textAlign = Paint.Align.CENTER
        }
        val title = if (configured) "No spots tagged $tagName" else "No tag chosen"
        val body = if (configured) "Tag a spot in the Vault to see it here" else "Long-press this widget to pick a tag"
        canvas.drawText(title, w / 2f, h * 0.66f, titlePaint)
        canvas.drawText(body, w / 2f, h * 0.78f, bodyPaint)
        return bitmap
    }
}
