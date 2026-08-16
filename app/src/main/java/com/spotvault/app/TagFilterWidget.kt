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
            val tagIds = (prefs[KEY_TAG_IDS] ?: emptySet()).mapNotNull { it.toIntOrNull() }
            val initialPrefs = remember { context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE) }
            val isPremium = isPremiumUnlocked(initialPrefs)

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
            val tagNamesState = produceState(initialValue = emptyList<String>(), key1 = tagIds, key2 = isPremium) {
                value = if (isPremium && tagIds.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        val tagDao = AppDatabase.getDatabase(context).tagDao()
                        tagIds.mapNotNull { tagDao.getTagById(it)?.name }
                    }
                } else {
                    emptyList()
                }
            }

            val entriesState = produceState(
                initialValue = emptyList<TagWidgetEntry>(),
                key1 = revision,
                key2 = tagIds,
                key3 = isPremium
            ) {
                value = if (!isPremium || tagIds.isEmpty()) {
                    emptyList()
                } else {
                    withContext(Dispatchers.IO) {
                        val db = AppDatabase.getDatabase(context)
                        val spots = db.tagDao().getSpotsForTags(tagIds)
                        val vehicleDao = db.vehicleDao()
                        val vehicleNames = mutableMapOf<Int, String?>()
                        spots.map { spot ->
                            val vehicleName = spot.vehicleId?.let { vid ->
                                vehicleNames.getOrPut(vid) { vehicleDao.getById(vid)?.name }
                            }
                            TagWidgetEntry(spot, vehicleName)
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
            } else {
                TagFilterContent(
                    theme = theme,
                    tagNames = tagNamesState.value,
                    entries = entriesState.value,
                    configured = tagIds.isNotEmpty()
                )
            }
        }
    }

    companion object {
        val KEY_REVISION = longPreferencesKey("widget_revision")
        val KEY_TAG_IDS = stringSetPreferencesKey("tag_filter_tag_ids")
    }
}

class TagFilterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TagFilterWidget()
}

private val TagFilterHeaderHeight = 24.dp
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
    modifier: GlanceModifier
) {
    val context = LocalContext.current
    val heightPx = WidgetThemeHelper.dpToPx(context, TagFilterHeaderHeight.value)
    val bitmap = remember(theme.cacheKey(), label, widthPx, heightPx) {
        PremiumWidgetRenderer.renderSectionLabelBitmap(
            context,
            label,
            android.graphics.Color.WHITE,
            widthPx,
            heightPx
        )
    }
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = label,
        modifier = modifier,
        contentScale = ContentScale.FillBounds
    )
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
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
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
