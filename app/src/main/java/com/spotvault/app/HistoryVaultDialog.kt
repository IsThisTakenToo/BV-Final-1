package com.spotvault.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.runtime.saveable.listSaver
import androidx.room.withTransaction
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val VAULT_SECTION_ORDER = listOf("Today", "Yesterday", "This Week", "This Month")
private const val TAG_CLOUD_CHIP_CAP = 80

fun calendarDayStart(timeMillis: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun vaultDateSection(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val spotDay = calendarDayStart(timestamp)
    val todayDay = calendarDayStart(now)
    // Calendar day math, not a fixed 86400000ms — across DST that fixed delta is 23h or 25h and
    // "Yesterday" / "This Week" bucketing silently drifts for a day.
    val yesterdayDay = Calendar.getInstance().apply {
        timeInMillis = todayDay
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis

    when {
        spotDay == todayDay -> return "Today"
        spotDay == yesterdayDay -> return "Yesterday"
    }

    val weekStartDay = Calendar.getInstance().apply {
        timeInMillis = now
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    if (spotDay >= weekStartDay && spotDay < yesterdayDay) return "This Week"

    val monthStartDay = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    if (spotDay >= monthStartDay && spotDay < weekStartDay) return "This Month"

    // Time-horizon strategy: recent stuff (above) stays broken out finely, but anything older
    // than "This Month" would otherwise accumulate one "[Month] [Year]" bucket per month forever
    // — after a few years of real use that's dozens of accordion rows to scroll past. Spots still
    // within the current calendar year group by month name alone (no year needed, it's implied);
    // anything from a past calendar year collapses to just that year, flat — not broken down by
    // month at all — so the section list stops growing linearly with how long the vault's existed
    // and instead grows by at most one new row per year.
    val spotYear = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.YEAR)
    val currentYear = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR)
    return if (spotYear == currentYear) {
        SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(timestamp))
    } else {
        spotYear.toString()
    }
}

fun groupSpotsByDateSection(spots: List<LocationSpot>): List<Pair<String, List<LocationSpot>>> {
    if (spots.isEmpty()) return emptyList()
    val grouped = spots.groupBy { vaultDateSection(it.timestamp) }
    val result = mutableListOf<Pair<String, List<LocationSpot>>>()

    VAULT_SECTION_ORDER.forEach { label ->
        grouped[label]?.takeIf { it.isNotEmpty() }?.let { result.add(label to it) }
    }

    grouped.keys
        .filter { it !in VAULT_SECTION_ORDER }
        .sortedByDescending { label ->
            grouped[label]?.maxOfOrNull { it.timestamp } ?: 0L
        }
        .forEach { label ->
            grouped[label]?.let { result.add(label to it) }
        }

    return result
}

@Composable
fun VaultNoPhotoPlaceholder(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val prefs = remember {
        context.getSharedPreferences("SpotVaultPrefs", android.content.Context.MODE_PRIVATE)
    }
    val themeKey = GlanceThemeManager.load(context).cacheKey()
    val iconId = AppIconManager.currentIconId(prefs)
    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.roundToPx().coerceAtLeast(1) }
        val heightPx = with(density) { maxHeight.roundToPx().coerceAtLeast(1) }
        val placeholder = remember(widthPx, heightPx, themeKey, iconId) {
            PremiumWidgetRenderer.renderNoPhotoThumbnailBitmap(context, widthPx, heightPx)
        }
        Image(
            bitmap = placeholder.asImageBitmap(),
            contentDescription = "No photo",
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun VaultSpotThumbnail(
    item: LocationSpot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    fillMaxSize: Boolean = true
) {
    val context = LocalContext.current
    val path = item.imagePath
    // remember(path) — File.exists() on every recomposition was sync main-thread I/O for every
    // visible Vault thumb (and again whenever the parent refiltered), which scales poorly with a
    // large vault. Coil still handles a missing file gracefully if the path goes stale mid-session.
    val hasValidPhoto = remember(path) {
        path.isNotEmpty() && runCatching { File(path).exists() }.getOrDefault(false)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(SpotVaultColors.Glass)
            .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.3f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (hasValidPhoto) {
            Image(
                painter = coil.compose.rememberAsyncImagePainter(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(File(path))
                        .size(160, 160)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = "Spot photo",
                contentScale = ContentScale.Crop,
                modifier = if (fillMaxSize) Modifier.fillMaxSize() else Modifier.matchParentSize()
            )
        } else {
            VaultNoPhotoPlaceholder(
                modifier = if (fillMaxSize) Modifier.fillMaxSize() else Modifier.matchParentSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSpotSwipeContainer(
    item: LocationSpot,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    gestureEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onArchive()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = gestureEnabled,
        enableDismissFromEndToStart = gestureEnabled,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                // Muted/secondary rather than a theme accent — archiving isn't the same class of
                // action as Delete (still reversible, not attention-grabbing the way a destructive
                // swipe should be), so it shouldn't compete visually with Delete's Danger color.
                SwipeToDismissBoxValue.StartToEnd -> SpotVaultColors.Outline
                SwipeToDismissBoxValue.EndToStart -> SpotVaultColors.Danger.copy(alpha = 0.9f)
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color),
                contentAlignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        Icon(
                            Icons.Default.Archive,
                            contentDescription = "Archive",
                            tint = SpotVaultColors.OnSurface,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = SpotVaultColors.OnSurface,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                    else -> Unit
                }
            }
        },
        content = { content() }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VaultHistorySpotCard(
    item: LocationSpot,
    vehicle: Vehicle?,
    tags: List<TagEntity>,
    allTags: List<TagEntity>,
    tagDao: TagDao,
    prefs: SharedPreferences,
    selectedItems: Set<Int>,
    selectionModeActive: Boolean,
    onViewSpot: (LocationSpot) -> Unit,
    onShareRequest: (ShareSpotPayload) -> Unit,
    onSelectionChange: (Set<Int>) -> Unit,
    onSelectionModeActiveChange: (Boolean) -> Unit,
    coroutineScope: CoroutineScope,
    dao: LocationDao,
    userLocation: Pair<Double, Double>? = null,
    distanceUnit: String? = null
) {
    val context = LocalContext.current
    val categoryEmoji = vaultSpotCategoryEmoji(item)
    val displayTitle = vaultSpotDisplayTitle(item)
    // Matches the exact "Today" boundary the section headers above these cards already use
    // (calendarDayStart/vaultDateSection) — a spot sitting under a "Today" header showing its
    // own full date too was redundant with the header it's grouped under; the date only earns
    // its place once the spot isn't from today, i.e. once it's no longer obvious from context.
    val date = if (calendarDayStart(item.timestamp) == calendarDayStart(System.currentTimeMillis())) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(item.timestamp))
    } else {
        SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.getDefault()).format(Date(item.timestamp))
    }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddPhotoDialog by remember { mutableStateOf(false) }
    var showTagEditor by remember { mutableStateOf(false) }

    fun toggleSelection() {
        onSelectionChange(
            if (selectedItems.contains(item.id)) selectedItems - item.id
            else selectedItems + item.id
        )
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionModeActive) toggleSelection() else onViewSpot(item)
                },
                onLongClick = {
                    onSelectionModeActiveChange(true)
                    onSelectionChange(selectedItems + item.id)
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = SpotVaultColors.Elevated)
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(52.dp)) {
                VaultSpotThumbnail(
                    item = item,
                    onClick = {
                        if (selectionModeActive) toggleSelection() else onViewSpot(item)
                    },
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(SpotVaultColors.Surface.copy(alpha = 0.92f))
                        .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = categoryEmoji, fontSize = 11.sp)
                }
                if (item.isFavorite) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Favorited",
                        tint = SpotVaultColors.Danger,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Top row: title on the left, overflow menu (and selection checkbox) pinned to
                // the right edge.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SpotVaultColors.OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectionModeActive) {
                        Checkbox(
                            checked = selectedItems.contains(item.id),
                            onCheckedChange = { isChecked ->
                                onSelectionChange(
                                    if (isChecked) selectedItems + item.id
                                    else selectedItems - item.id
                                )
                            },
                            colors = spotVaultCheckboxColors(),
                            modifier = Modifier.scale(0.85f)
                        )
                    } else {
                        var menuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More actions", tint = SpotVaultColors.Muted)
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                VaultSpotOverflowMenuItems(
                                    item = item,
                                    onEdit = { showEditDialog = true },
                                    onShareRequest = onShareRequest,
                                    onAddPhoto = { showAddPhotoDialog = true },
                                    onEditTags = { showTagEditor = true },
                                    onDismissMenu = { menuExpanded = false },
                                    coroutineScope = coroutineScope,
                                    dao = dao
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Middle row: Notes on the left (the same text that otherwise sat on its own line
                // further down, disconnected from this row), one-tap Favorite/Active actions on
                // the right. A plain Spacer(weight(1f)) here — instead of the removed category
                // text's old slot — read as a stray gap with nothing across from the icons; using
                // that space for Notes instead gives it a purpose and keeps the icons right-aligned
                // exactly the way the spacer alone did. Falls back to the same spacer when there's
                // no notes, so the icons stay aligned either way.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.locationDetails.isNotEmpty()) {
                        Text(
                            text = item.locationDetails,
                            fontSize = 13.sp,
                            color = SpotVaultColors.OnSurface.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    IconButton(
                        onClick = {
                            val spotId = item.id
                            coroutineScope.launch(Dispatchers.IO) { dao.toggleFavorite(spotId) }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (item.isFavorite) "Remove Favorite" else "Add Favorite",
                            tint = if (item.isFavorite) SpotVaultColors.Danger else SpotVaultColors.Muted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            val spotId = item.id
                            coroutineScope.launch(Dispatchers.IO) { dao.togglePinned(spotId) }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.VerticalAlignTop,
                            contentDescription = if (item.isPinned) "Remove from Active" else "Make Active",
                            tint = if (item.isPinned) SpotVaultColors.Teal else SpotVaultColors.Muted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                VehicleSpotBadge(vehicle = vehicle)

                if (tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    VaultTagChipsRow(tags = tags, modifier = Modifier.padding(top = 2.dp))
                }

                Spacer(modifier = Modifier.height(2.dp))
                // Bottom row: date on the left, distance pushed to the far right.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = date,
                        fontSize = 12.sp,
                        color = SpotVaultColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (distanceUnit != null) {
                        SpotDistanceLabel(
                            spotLat = item.lat,
                            spotLng = item.lng,
                            prefs = prefs,
                            userLocation = userLocation,
                            distanceUnit = distanceUnit
                        )
                    } else {
                        SpotDistanceLabel(
                            spotLat = item.lat,
                            spotLng = item.lng,
                            prefs = prefs
                        )
                    }
                }
            }
        }
    }
    if (showEditDialog) {
        SpotEditDialog(
            prefs = prefs,
            spotId = item.id,
            currentTitle = item.title,
            currentTimestamp = item.timestamp,
            currentNotes = rememberFullSpotNotes(dao, item.id, item.locationDetails),
            currentCity = item.city,
            currentState = item.state,
            currentVehicleId = item.vehicleId,
            onDismiss = { showEditDialog = false },
            onSave = { newTitle, newTimestamp, newNotes, newCity, newState, newVehicleId ->
                val spotId = item.id
                coroutineScope.launch(Dispatchers.IO) {
                    // Re-fetches rather than item.copy(...) — item is a snapshot from whenever
                    // this card last recomposed, which a fast enough sequence of actions (e.g.
                    // attaching a photo, then opening Edit and saving before the list's Flow had
                    // caught up) could beat. copy()-ing a stale snapshot would silently write its
                    // old imagePath back over the new one, erasing a photo that had just been
                    // added. Reading the current row immediately before the update closes that
                    // window regardless of how stale item itself is by the time Save is tapped.
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
                showEditDialog = false
            }
        )
    }
    if (showAddPhotoDialog) {
        VaultAddPhotoDialog(item = item, onDismiss = { showAddPhotoDialog = false })
    }
    if (showTagEditor) {
        VaultTagEditorDialog(
            item = item,
            currentTags = tags,
            allTags = allTags,
            tagDao = tagDao,
            coroutineScope = coroutineScope,
            onDismiss = { showTagEditor = false }
        )
    }
}

@Composable
internal fun rememberFullSpotNotes(dao: LocationDao, spotId: Int, preview: String): String {
    val notes by produceState(initialValue = preview, spotId) {
        value = withContext(Dispatchers.IO) {
            dao.getSpotById(spotId)?.locationDetails ?: preview
        }
    }
    return notes
}

@Composable
private fun SpotEditSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = SpotVaultColors.Muted,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun spotEditFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedTextColor = SpotVaultColors.OnSurface,
    focusedTextColor = SpotVaultColors.OnSurface,
    unfocusedLabelColor = SpotVaultColors.Muted,
    unfocusedBorderColor = SpotVaultColors.Outline,
    focusedLabelColor = SpotVaultColors.Teal,
    focusedBorderColor = SpotVaultColors.Teal,
    cursorColor = SpotVaultColors.Teal
)

/** Unified vault spot editor — title, tags, date/time, and notes in one polished dialog. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpotEditDialog(
    prefs: SharedPreferences,
    spotId: Int,
    // The spot's raw, unmodified title — NOT vaultSpotDisplayTitle's already-stripped version.
    // This dialog does its own splitting around the auto-naming date suffix (see titleSuffix
    // below) so it can preserve that suffix on save; handing it an already-stripped title here
    // would silently discard that suffix from the stored title on every single edit.
    currentTitle: String,
    currentTimestamp: Long,
    currentNotes: String,
    currentCity: String,
    currentState: String,
    currentVehicleId: Int?,
    onDismiss: () -> Unit,
    onSave: (title: String, timestamp: Long, notes: String, city: String, state: String, vehicleId: Int?) -> Unit
) {
    val context = LocalContext.current
    val tagDao = remember { AppDatabase.getDatabase(context).tagDao() }
    val vehicleDao = remember { AppDatabase.getDatabase(context).vehicleDao() }
    val tagCoroutineScope = rememberCoroutineScope()
    val allTags by tagDao.getAllTags().collectAsState(initial = emptyList())
    val currentTags by tagDao.getTagsForSpotFlow(spotId).collectAsState(initial = emptyList())
    val allVehicles by vehicleDao.observeAll().collectAsState(initial = emptyList())
    var showTagPicker by remember { mutableStateOf(false) }
    var showVehiclePicker by remember { mutableStateOf(false) }
    var vehicleId by remember { mutableStateOf(currentVehicleId) }
    // currentTitle is the raw stored title, which for the default "Label - Date & Time" auto-pin
    // naming format embeds a literal "\n<date>" suffix (see buildQuickPinSpot) — vaultSpotDisplayTitle
    // strips that for card display since the timestamp already shows separately there, but this
    // editor used to be handed that already-stripped string directly as both the field's starting
    // value AND (unchanged) what got written back on Save, silently discarding the date suffix from
    // the stored title on literally any edit, even one that never touched the Title field at all.
    // Splitting/rejoining around the first '\n' here means the field only ever shows/edits the
    // human-readable label — exactly what it already looked like — while the invisible suffix, if
    // there was one, survives the save untouched.
    val titleSuffix = remember(currentTitle) {
        currentTitle.indexOf('\n').let { idx -> if (idx >= 0) currentTitle.substring(idx) else "" }
    }
    var title by remember { mutableStateOf(currentTitle.substringBefore('\n')) }
    var timestamp by remember { mutableStateOf(currentTimestamp) }
    var notes by remember { mutableStateOf(currentNotes) }
    var city by remember { mutableStateOf(currentCity) }
    var state by remember { mutableStateOf(currentState) }
    var showNotepad by remember { mutableStateOf(false) }
    val dateLabel = remember(timestamp) { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp)) }
    val timeLabel = remember(timestamp) { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp)) }

    fun pickDate() {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                cal.set(year, month, day)
                timestamp = cal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun pickTime() {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        android.app.TimePickerDialog(
            context,
            { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                timestamp = cal.timeInMillis
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        ).show()
    }

    PremiumDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Spot", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = SpotVaultColors.OnSurface)
        },
        content = {
            // PremiumDialog caps content height but doesn't scroll it — Title + Tags (which can
            // wrap to several lines with enough tags) + Date/Time + City/State + Notes routinely
            // exceeds that cap on a shorter phone, silently clipping the lower fields (typically
            // Notes) with no way to reach them. Same fix as AddQuietZoneDialog.
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Column {
                    SpotEditSectionLabel("Title")
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 120) title = it },
                        placeholder = { Text("Spot title", color = SpotVaultColors.Muted.copy(alpha = 0.7f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = spotEditFieldColors(),
                        trailingIcon = {
                            VoiceMicButton(
                                onResult = { spoken -> title = spoken.take(120) },
                                prompt = "Dictate title…"
                            )
                        }
                    )
                }
                Column {
                    SpotEditSectionLabel("Tags")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentTags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                currentTags.forEach { tag ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SpotVaultColors.Teal.copy(alpha = 0.18f))
                                            .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(tag.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        // Box-wrapped so the tappable area is meaningfully bigger
                                        // than the 14dp icon itself (was the icon's own bare
                                        // bounds — well under Android's touch-target guidance)
                                        // without inflating the chip's own tight visual size.
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clickable {
                                                    tagCoroutineScope.launch(Dispatchers.IO) {
                                                        tagDao.removeTag(spotId, tag.id)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove ${tag.name}",
                                                tint = SpotVaultColors.Teal,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // FlowRow, not Row — a long vehicle name (up to 40 chars) alongside the
                        // "Add Tag" chip had nothing to stop it from pushing past the dialog's
                        // own width, unlike every other chip row in this file (e.g. the tags
                        // FlowRow right above this).
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                    .clickable { showTagPicker = true }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Sell, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Tag", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.OnSurface)
                            }
                            val assignedVehicle = allVehicles.firstOrNull { it.id == vehicleId }
                            if (assignedVehicle != null) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SpotVaultColors.Teal.copy(alpha = 0.18f))
                                        .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .clickable { showVehiclePicker = true }
                                        .padding(start = 10.dp, top = 7.dp, bottom = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${vehicleIconEmoji(assignedVehicle.iconKey)} ${assignedVehicle.name}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SpotVaultColors.Teal,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    // Box-wrapped so the tappable area is meaningfully bigger
                                    // than the 14dp icon itself — same fix as the tag chips'
                                    // remove button above.
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable { vehicleId = null },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear vehicle",
                                            tint = SpotVaultColors.Teal,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                        .clickable { showVehiclePicker = true }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Vehicle", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.OnSurface)
                                }
                            }
                        }
                    }
                }
                Column {
                    SpotEditSectionLabel("Date & Time")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpotVaultOutlinedButton(
                            onClick = { pickDate() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.OnSurface)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = SpotVaultColors.Teal)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(dateLabel, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, color = SpotVaultColors.OnSurface)
                        }
                        SpotVaultOutlinedButton(
                            onClick = { pickTime() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.OnSurface)
                        ) {
                            Text(timeLabel, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SpotVaultColors.OnSurface)
                        }
                    }
                }
                Column {
                    SpotEditSectionLabel("City & State")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { if (it.length <= 60) city = it },
                            placeholder = { Text("City", color = SpotVaultColors.Muted.copy(alpha = 0.7f)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = spotEditFieldColors(),
                            trailingIcon = {
                                VoiceMicButton(onResult = { spoken -> city = spoken.take(60) }, prompt = "Speak city…")
                            }
                        )
                        OutlinedTextField(
                            value = state,
                            onValueChange = { if (it.length <= 60) state = it },
                            placeholder = { Text("State / Region", color = SpotVaultColors.Muted.copy(alpha = 0.7f)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = spotEditFieldColors(),
                            trailingIcon = {
                                VoiceMicButton(onResult = { spoken -> state = spoken.take(60) }, prompt = "Speak state or region…")
                            }
                        )
                    }
                }
                Column {
                    SpotEditSectionLabel("Notes")
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it.take(NOTEPAD_MAX_CHARS) },
                        placeholder = { Text("Add notes…", color = SpotVaultColors.Muted.copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 2,
                        colors = spotEditFieldColors(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                VoiceMicButton(
                                    onResult = { spoken ->
                                        notes = (notes + if (notes.isBlank()) spoken else " $spoken")
                                            .take(NOTEPAD_MAX_CHARS)
                                    },
                                    prompt = "Dictate notes…"
                                )
                                IconButton(onClick = { showNotepad = true }) {
                                    Icon(
                                        Icons.Default.OpenInFull,
                                        contentDescription = "Open notepad editor",
                                        tint = SpotVaultColors.Teal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            SpotVaultButton(
                onClick = {
                    onSave(
                        title.trim() + titleSuffix,
                        timestamp,
                        notes.trim().take(NOTEPAD_MAX_CHARS),
                        city.trim(),
                        state.trim(),
                        vehicleId
                    )
                },
                enabled = title.isNotBlank(),
                shape = spotVaultButtonShape(),
                modifier = Modifier.height(44.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SpotVaultColors.Teal)
            }
        }
    )

    if (showNotepad) {
        NotepadEditorDialog(
            initialNotes = notes,
            onDismiss = { showNotepad = false },
            onSave = { notes = it.take(NOTEPAD_MAX_CHARS) },
            fieldLabel = "Notes",
            contextAddress = currentTitle
        )
    }

    if (showTagPicker) {
        SpotEditTagPickerSheet(
            spotId = spotId,
            currentTags = currentTags,
            allTags = allTags,
            tagDao = tagDao,
            coroutineScope = tagCoroutineScope,
            onDismiss = { showTagPicker = false }
        )
    }

    if (showVehiclePicker) {
        SpotEditVehiclePickerSheet(
            vehicles = allVehicles,
            selectedVehicleId = vehicleId,
            onSelectedVehicleIdChange = { vehicleId = it },
            onDismiss = { showVehiclePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpotEditVehiclePickerSheet(
    vehicles: List<Vehicle>,
    selectedVehicleId: Int?,
    onSelectedVehicleIdChange: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val visibleVehicles = remember(vehicles) { vehicles.filter { !it.isArchived } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpotVaultColors.Elevated,
        contentColor = SpotVaultColors.OnSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                "Assign Vehicle",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = SpotVaultColors.Teal,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (visibleVehicles.isEmpty()) {
                Text(
                    "No vehicles yet — add one from Settings.",
                    color = SpotVaultColors.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    VaultFilterVehicleRow(
                        label = "No Vehicle",
                        emoji = null,
                        selected = selectedVehicleId == null,
                        onClick = { onSelectedVehicleIdChange(null); onDismiss() }
                    )
                    visibleVehicles.forEach { vehicle ->
                        VaultFilterVehicleRow(
                            label = vehicle.name,
                            emoji = vehicleIconEmoji(vehicle.iconKey),
                            selected = selectedVehicleId == vehicle.id,
                            onClick = { onSelectedVehicleIdChange(vehicle.id); onDismiss() }
                        )
                    }
                }
            }
        }
    }
}

/** Same smart tag cloud as the Vault's "Filter by Tag" sheet and the Snap/Pin "Add Tag" picker
 * (search, Frequently Used / All Tags split, create-new-from-search) — chips toggle multiple
 * tags on/off, and every toggle writes straight to [TagDao] since, unlike the Snap/Pin flow, this
 * spot already exists. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SpotEditTagPickerSheet(
    spotId: Int,
    currentTags: List<TagEntity>,
    allTags: List<TagEntity>,
    tagDao: TagDao,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }
    val filteredTags = remember(allTags, search) {
        if (search.isBlank()) allTags else allTags.filter { searchTextMatches(it.name, search) }
    }
    val frequentTags = remember(allTags) { allTags.sortedByDescending { it.usageCount }.take(10) }
    val remainingTagsAlpha = remember(allTags, frequentTags) {
        val frequentIds = frequentTags.map { it.id }.toSet()
        allTags.filter { it.id !in frequentIds }.sortedBy { it.name.lowercase() }
    }
    val currentTagIds = remember(currentTags) { currentTags.map { it.id }.toSet() }
    val trimmedSearch = search.trim()
    val exactMatchExists = remember(allTags, trimmedSearch) {
        trimmedSearch.isNotEmpty() && allTags.any { it.name.equals(trimmedSearch, ignoreCase = true) }
    }

    fun toggleTag(tag: TagEntity) {
        coroutineScope.launch(Dispatchers.IO) {
            if (currentTagIds.contains(tag.id)) tagDao.removeTag(spotId, tag.id) else tagDao.assignTag(spotId, tag.name)
        }
    }

    fun createTagFromSearch() {
        val name = trimmedSearch
        if (name.isEmpty() || exactMatchExists) return
        coroutineScope.launch(Dispatchers.IO) { tagDao.assignTag(spotId, name) }
        search = ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpotVaultColors.Elevated,
        contentColor = SpotVaultColors.OnSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                "Edit Tags",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = SpotVaultColors.Teal,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search or add a tag…", color = SpotVaultColors.Muted.copy(alpha = 0.7f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SpotVaultColors.Teal) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (trimmedSearch.isNotEmpty() && !exactMatchExists) {
                            IconButton(onClick = { createTagFromSearch() }) {
                                Icon(Icons.Default.Add, contentDescription = "Create tag \"$trimmedSearch\"", tint = SpotVaultColors.Teal)
                            }
                        }
                        VoiceMicButton(onResult = { search = it }, prompt = "Speak a tag…")
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = spotEditFieldColors()
            )
            if (trimmedSearch.isNotEmpty() && !exactMatchExists) {
                Text(
                    "Tap + to create \"$trimmedSearch\" as a new tag",
                    color = SpotVaultColors.Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            when {
                allTags.isEmpty() -> Text(
                    "No tags yet — create one above.",
                    color = SpotVaultColors.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
                filteredTags.isEmpty() && trimmedSearch.isNotEmpty() -> Text(
                    "No tags match \"$search\"",
                    color = SpotVaultColors.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
                trimmedSearch.isNotEmpty() -> FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tagsToShow = filteredTags.take(TAG_CLOUD_CHIP_CAP)
                    tagsToShow.forEach { tag ->
                        SpotEditTagChip(tag = tag, selected = currentTagIds.contains(tag.id), onClick = { toggleTag(tag) })
                    }
                    if (filteredTags.size > TAG_CLOUD_CHIP_CAP) {
                        Text(
                            "Showing $TAG_CLOUD_CHIP_CAP of ${filteredTags.size} — refine search",
                            color = SpotVaultColors.Muted,
                            fontSize = 11.sp
                        )
                    }
                }
                else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (frequentTags.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "FREQUENTLY USED",
                                color = SpotVaultColors.Muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                frequentTags.forEach { tag ->
                                    SpotEditTagChip(tag = tag, selected = currentTagIds.contains(tag.id), onClick = { toggleTag(tag) })
                                }
                            }
                        }
                    }
                    if (remainingTagsAlpha.isNotEmpty()) {
                        if (frequentTags.isNotEmpty()) {
                            HorizontalDivider(color = SpotVaultColors.Outline.copy(alpha = 0.25f))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (frequentTags.isNotEmpty()) {
                                Text(
                                    "ALL TAGS",
                                    color = SpotVaultColors.Muted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            FlowRow(
                                modifier = Modifier
                                    .heightIn(max = 220.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Cap eager chip count — search still finds the rest.
                                remainingTagsAlpha.take(TAG_CLOUD_CHIP_CAP).forEach { tag ->
                                    SpotEditTagChip(tag = tag, selected = currentTagIds.contains(tag.id), onClick = { toggleTag(tag) })
                                }
                            }
                            if (remainingTagsAlpha.size > TAG_CLOUD_CHIP_CAP) {
                                Text(
                                    "Showing ${TAG_CLOUD_CHIP_CAP} of ${remainingTagsAlpha.size} — search to find more",
                                    color = SpotVaultColors.Muted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            SpotVaultButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = spotVaultButtonShape()
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SpotEditTagChip(tag: TagEntity, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(tag.name, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        colors = vaultTagChipColors(),
        border = vaultTagChipBorder(selected),
        shape = RoundedCornerShape(10.dp)
    )
}

@Composable
private fun VaultSpotOverflowMenuItems(
    item: LocationSpot,
    onEdit: () -> Unit,
    onShareRequest: (ShareSpotPayload) -> Unit,
    onAddPhoto: () -> Unit,
    onEditTags: () -> Unit,
    onDismissMenu: () -> Unit,
    coroutineScope: CoroutineScope,
    dao: LocationDao
) {
    DropdownMenuItem(
        text = { Text("Edit") },
        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
        onClick = {
            onDismissMenu()
            onEdit()
        }
    )
    DropdownMenuItem(
        text = { Text("Share") },
        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
        onClick = {
            onDismissMenu()
            onShareRequest(
                ShareSpotPayload(
                    lat = item.lat,
                    lng = item.lng,
                    address = item.address,
                    title = vaultSpotDisplayTitle(item),
                    imagePath = item.imagePath
                )
            )
        }
    )
    DropdownMenuItem(
        text = { Text("Add Photo") },
        leadingIcon = { Icon(Icons.Default.AddAPhoto, contentDescription = null) },
        onClick = {
            onDismissMenu()
            onAddPhoto()
        }
    )
    DropdownMenuItem(
        text = { Text("Edit Tags") },
        leadingIcon = { Icon(Icons.Default.Sell, contentDescription = null) },
        onClick = {
            onDismissMenu()
            onEditTags()
        }
    )
    if (item.lat != 0.0 || item.lng != 0.0) {
        val context = LocalContext.current
        DropdownMenuItem(
            text = { Text("Refresh Address") },
            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
            onClick = {
                onDismissMenu()
                val spotId = item.id
                coroutineScope.launch(Dispatchers.IO) {
                    val geocoded = reverseGeocodeAddress(context, item.lat, item.lng)
                    if (geocoded.full.startsWith("Lat:")) {
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Still couldn't reach a network — try again in a bit", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    val current = dao.getSpotById(spotId) ?: return@launch
                    val newTitle = if (isPlaceholderCoordinateTitle(current.title, current.lat, current.lng)) {
                        quickActionSpotTitle(geocoded, current.lat, current.lng)
                    } else {
                        current.title
                    }
                    dao.updateSpot(current.copy(address = geocoded.full, city = geocoded.city, state = geocoded.state, title = newTitle))
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Address updated", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
    DropdownMenuItem(
        text = { Text("Archive") },
        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
        onClick = {
            onDismissMenu()
            val spotId = item.id
            coroutineScope.launch(Dispatchers.IO) { dao.archiveSpot(spotId) }
            VaultUndoSnackbar.show("Spot archived") {
                withContext(Dispatchers.IO) { dao.unarchiveSpotIfArchived(spotId) }
            }
        }
    )
    // Same instant-action-plus-Undo shape as Archive right above — softDeleteSpot is already
    // fully reversible (it's exactly how Recently Deleted works), so there's nothing a blocking
    // "are you sure?" dialog would protect against here that Undo doesn't already cover, and it
    // makes single-spot delete completely discoverable instead of only reachable via swipe.
    DropdownMenuItem(
        text = { Text("Delete", color = SpotVaultColors.Danger) },
        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = SpotVaultColors.Danger) },
        onClick = {
            onDismissMenu()
            val spotId = item.id
            coroutineScope.launch(Dispatchers.IO) { dao.softDeleteSpot(spotId) }
            VaultUndoSnackbar.show("Spot deleted") {
                withContext(Dispatchers.IO) { dao.restoreSpotIfSoftDeleted(spotId) }
            }
        }
    )
}

// --- Tag system UI: filter chips, tag-cloud filter sheet, per-card chips, per-spot editor ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun vaultTagChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = SpotVaultColors.Deep,
    labelColor = SpotVaultColors.Muted,
    selectedContainerColor = SpotVaultColors.Teal.copy(alpha = 0.22f),
    selectedLabelColor = SpotVaultColors.Teal
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun vaultTagChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = SpotVaultColors.Outline.copy(alpha = 0.45f),
    selectedBorderColor = SpotVaultColors.Teal.copy(alpha = 0.65f)
)

/** System filter chip style — transparent fill, visible stroke — distinct from the solid
 * dark-surface fill user tag chips use, so "Near Me"/"Photos" read as built-in filters rather
 * than blending into the user's own tags. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun vaultSystemChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Color.Transparent,
    labelColor = SpotVaultColors.Muted,
    selectedContainerColor = SpotVaultColors.Teal.copy(alpha = 0.14f),
    selectedLabelColor = SpotVaultColors.Teal
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun vaultSystemChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderWidth = 1.dp,
    selectedBorderWidth = 1.5.dp,
    borderColor = SpotVaultColors.Outline.copy(alpha = 0.7f),
    selectedBorderColor = SpotVaultColors.Teal.copy(alpha = 0.85f)
)

/** Top filter row: hardcoded system filters (Near Me, Photos — outlined) on the left, a divider,
 * then a Favorites toggle plus the user's 5 most-used tags (solid) on the right, replacing the
 * old category dropdown. Tapping a chip instantly filters the list. Only quick-filter chips live
 * here — opening the full tag cloud is a separate control up in [VaultSearchSortRow], not a
 * second filter icon duplicated into this row. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultTagFilterBar(
    topTags: List<TagEntity>,
    showFavoritesOnly: Boolean,
    onShowFavoritesOnlyChange: (Boolean) -> Unit,
    nearMeFilter: VaultNearMeFilter,
    onNearMeFilterChange: (VaultNearMeFilter) -> Unit,
    showPhotosOnly: Boolean,
    onShowPhotosOnlyChange: (Boolean) -> Unit,
    selectedTag: String?,
    onSelectedTagChange: (String?) -> Unit,
    activeVehicleLabel: String?,
    onClearVehicle: () -> Unit,
    modifier: Modifier = Modifier,
    // False for the main Vault screen (its own gold Star button in the header opens the Favorites
    // Hub instead — see vaultHeaderRow in MainActivity.kt) and for FavoritesHubDialog's own use of
    // VaultFilterableSpotList (every spot shown there already has isFavorite = true, so the chip
    // would just be a dead toggle with nothing left to filter). True everywhere else.
    showFavoritesChip: Boolean = true
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 3-step cycle on tap: Off -> Walking (1 mi) -> Driving (5 mi) -> Off, so switching scale
        // never needs a settings menu.
        FilterChip(
            selected = nearMeFilter != VaultNearMeFilter.OFF,
            onClick = { onNearMeFilterChange(nearMeFilter.next()) },
            label = { Text(nearMeFilter.label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            colors = vaultSystemChipColors(),
            border = vaultSystemChipBorder(nearMeFilter != VaultNearMeFilter.OFF),
            shape = RoundedCornerShape(10.dp)
        )
        FilterChip(
            selected = showPhotosOnly,
            onClick = { onShowPhotosOnlyChange(!showPhotosOnly) },
            label = { Text("📸 Photos", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            colors = vaultSystemChipColors(),
            border = vaultSystemChipBorder(showPhotosOnly),
            shape = RoundedCornerShape(10.dp)
        )
        if (showFavoritesChip) {
            VerticalDivider(
                modifier = Modifier.height(20.dp),
                color = SpotVaultColors.Outline.copy(alpha = 0.35f)
            )
            FilterChip(
                selected = showFavoritesOnly,
                onClick = { onShowFavoritesOnlyChange(!showFavoritesOnly) },
                label = { Text("⭐ Favorites", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                colors = vaultTagChipColors(),
                border = vaultTagChipBorder(showFavoritesOnly),
                shape = RoundedCornerShape(10.dp)
            )
        }
        topTags.forEach { tag ->
            val selected = selectedTag.equals(tag.name, ignoreCase = true)
            FilterChip(
                selected = selected,
                onClick = { onSelectedTagChange(if (selected) null else tag.name) },
                label = {
                    Text(
                        tag.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = vaultTagChipColors(),
                border = vaultTagChipBorder(selected),
                shape = RoundedCornerShape(10.dp)
            )
        }
        // Vehicle filtering itself now lives in the tag sheet's Vehicles tab (see
        // VaultTagFilterSheet) — this chip is just a visible "a vehicle filter is active" flag
        // and quick-clear, since otherwise there'd be no way to tell a vehicle filter was on
        // without reopening the sheet.
        if (activeVehicleLabel != null) {
            FilterChip(
                selected = true,
                onClick = onClearVehicle,
                label = {
                    Text(activeVehicleLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Clear vehicle filter", modifier = Modifier.size(14.dp)) },
                colors = vaultTagChipColors(),
                border = vaultTagChipBorder(true),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

private enum class VaultFilterSheetTab { TAGS, VEHICLES }

/** Full A-Z tag cloud in a bottom sheet, with a pinned search field that filters it live — plus
 * a second, smaller tab that swaps the same sheet over to a Vehicle filter list. Tags is the
 * default/wider tab since it's the primary way to filter the Vault; Vehicles is the secondary,
 * narrower one off to the right, not a separate control of its own anymore. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun VaultTagFilterSheet(
    allTags: List<TagEntity>,
    selectedTag: String?,
    onSelectedTagChange: (String?) -> Unit,
    vehicles: List<Vehicle>,
    selectedVehicleId: Int?,
    onSelectedVehicleIdChange: (Int?) -> Unit,
    showArchivedVehicles: Boolean,
    onShowArchivedVehiclesChange: (Boolean) -> Unit,
    tagDao: TagDao,
    vehicleDao: VehicleDao,
    locationDao: LocationDao,
    coroutineScope: CoroutineScope,
    initialTab: VaultFilterSheetTab = VaultFilterSheetTab.TAGS,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(initialTab) }
    var showTagManager by remember { mutableStateOf(false) }
    var showAddVehicleFromSheet by remember { mutableStateOf(false) }
    val filteredTags = remember(allTags, search) {
        if (search.isBlank()) allTags else allTags.filter { searchTextMatches(it.name, search) }
    }
    // Only meaningful once a user has accumulated enough tags for the cloud to get unwieldy —
    // the top 10 most-assigned surface first, everything else follows alphabetically below a
    // divider, so a heavy tagger isn't stuck scanning hundreds of chips for the ones they
    // actually use. Collapses back to a single flat list (via filteredTags) the moment they type.
    val frequentTags = remember(allTags) { allTags.sortedByDescending { it.usageCount }.take(10) }
    val remainingTagsAlpha = remember(allTags, frequentTags) {
        val frequentIds = frequentTags.map { it.id }.toSet()
        allTags.filter { it.id !in frequentIds }.sortedBy { it.name.lowercase() }
    }
    val trimmedSearch = search.trim()
    val exactMatchExists = remember(allTags, trimmedSearch) {
        trimmedSearch.isNotEmpty() && allTags.any { it.name.equals(trimmedSearch, ignoreCase = true) }
    }

    fun createTagFromSearch() {
        val name = trimmedSearch
        if (name.isEmpty() || exactMatchExists) return
        coroutineScope.launch(Dispatchers.IO) { tagDao.createTag(name) }
        onSelectedTagChange(name)
        search = ""
    }
    val visibleVehicles = remember(vehicles, showArchivedVehicles) {
        if (showArchivedVehicles) vehicles else vehicles.filter { !it.isArchived }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpotVaultColors.Elevated,
        contentColor = SpotVaultColors.OnSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (tab == VaultFilterSheetTab.TAGS) "Filter by Tag" else "Filter by Vehicle",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = SpotVaultColors.Teal,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f)
                )
                // Two unequal tabs, not a 50/50 segmented control — Tags is the primary surface
                // this sheet exists for; Vehicles is a smaller secondary option riding along.
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpotVaultColors.Deep)
                        .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(VaultFilterSheetTab.TAGS to "Tags", VaultFilterSheetTab.VEHICLES to "Vehicles").forEach { (value, label) ->
                        val selected = tab == value
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selected) SpotVaultColors.Teal.copy(alpha = 0.22f) else Color.Transparent)
                                .clickable { tab = value }
                                .padding(horizontal = if (value == VaultFilterSheetTab.TAGS) 20.dp else 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                label,
                                fontSize = if (value == VaultFilterSheetTab.TAGS) 16.sp else 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) SpotVaultColors.Teal else SpotVaultColors.Muted
                            )
                        }
                    }
                }
            }

            if (tab == VaultFilterSheetTab.TAGS) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search or add a tag…", color = SpotVaultColors.Muted.copy(alpha = 0.7f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SpotVaultColors.Teal) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (trimmedSearch.isNotEmpty() && !exactMatchExists) {
                                IconButton(onClick = { createTagFromSearch() }) {
                                    Icon(Icons.Default.Add, contentDescription = "Create tag \"$trimmedSearch\"", tint = SpotVaultColors.Teal)
                                }
                            }
                            VoiceMicButton(onResult = { search = it }, prompt = "Speak a tag…")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = spotEditFieldColors()
                )
                if (trimmedSearch.isNotEmpty() && !exactMatchExists) {
                    Text(
                        "Tap + to create \"$trimmedSearch\" as a new tag",
                        color = SpotVaultColors.Muted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showTagManager = true },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Sell, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Manage Tags", color = SpotVaultColors.Teal, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                when {
                    allTags.isEmpty() -> Text(
                        "No tags yet — add tags to a spot from its ⋮ menu, or create one above.",
                        color = SpotVaultColors.Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                    filteredTags.isEmpty() -> Text(
                        "No tags match \"$search\"",
                        color = SpotVaultColors.Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                    trimmedSearch.isNotEmpty() -> FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tagsToShow = filteredTags.take(TAG_CLOUD_CHIP_CAP)
                        tagsToShow.forEach { tag ->
                            VaultFilterTagChip(tag = tag, selectedTag = selectedTag, onSelectedTagChange = onSelectedTagChange, onDismiss = onDismiss)
                        }
                        if (filteredTags.size > TAG_CLOUD_CHIP_CAP) {
                            Text(
                                "Showing $TAG_CLOUD_CHIP_CAP of ${filteredTags.size} — refine search",
                                color = SpotVaultColors.Muted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (frequentTags.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "FREQUENTLY USED",
                                    color = SpotVaultColors.Muted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    frequentTags.forEach { tag ->
                                        VaultFilterTagChip(tag = tag, selectedTag = selectedTag, onSelectedTagChange = onSelectedTagChange, onDismiss = onDismiss)
                                    }
                                }
                            }
                        }
                        if (remainingTagsAlpha.isNotEmpty()) {
                            if (frequentTags.isNotEmpty()) {
                                HorizontalDivider(color = SpotVaultColors.Outline.copy(alpha = 0.25f))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (frequentTags.isNotEmpty()) {
                                    Text(
                                        "ALL TAGS",
                                        color = SpotVaultColors.Muted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                FlowRow(
                                    modifier = Modifier
                                        .heightIn(max = 220.dp)
                                        .verticalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    remainingTagsAlpha.take(TAG_CLOUD_CHIP_CAP).forEach { tag ->
                                        VaultFilterTagChip(tag = tag, selectedTag = selectedTag, onSelectedTagChange = onSelectedTagChange, onDismiss = onDismiss)
                                    }
                                }
                                if (remainingTagsAlpha.size > TAG_CLOUD_CHIP_CAP) {
                                    Text(
                                        "Showing ${TAG_CLOUD_CHIP_CAP} of ${remainingTagsAlpha.size} — search to find more",
                                        color = SpotVaultColors.Muted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                if (visibleVehicles.isEmpty()) {
                    Text(
                        "No vehicles yet — add one from Settings.",
                        color = SpotVaultColors.Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        VaultFilterVehicleRow(
                            label = "All Vehicles",
                            emoji = null,
                            selected = selectedVehicleId == null,
                            onClick = { onSelectedVehicleIdChange(null); onDismiss() }
                        )
                        visibleVehicles.forEach { vehicle ->
                            VaultFilterVehicleRow(
                                label = vehicle.name,
                                emoji = vehicleIconEmoji(vehicle.iconKey),
                                selected = selectedVehicleId == vehicle.id,
                                onClick = { onSelectedVehicleIdChange(vehicle.id); onDismiss() }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showAddVehicleFromSheet = true },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Vehicle", color = SpotVaultColors.Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (vehicles.any { it.isArchived }) {
                        TextButton(onClick = { onShowArchivedVehiclesChange(!showArchivedVehicles) }) {
                            Text(
                                if (showArchivedVehicles) "Hide archived vehicles" else "Show archived vehicles",
                                color = SpotVaultColors.Muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTagManager) {
        VaultTagManagerDialog(
            tagDao = tagDao,
            coroutineScope = coroutineScope,
            onDismiss = { showTagManager = false }
        )
    }

    if (showAddVehicleFromSheet) {
        BackHandler(onBack = { showAddVehicleFromSheet = false })
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAddVehicleFromSheet = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            EnsureDialogEdgeToEdge()
            val density = androidx.compose.ui.platform.LocalDensity.current
            val statusPad = with(density) { SystemBarInsets.statusBarPx.toDp() }
            val navPad = with(density) { SystemBarInsets.navigationBarPx.toDp() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SpotVaultColors.Void)
                    .padding(top = statusPad, bottom = navPad)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                VehicleEditScreen(
                    vehicleId = null,
                    vehicleDao = vehicleDao,
                    locationDao = locationDao,
                    onBack = { showAddVehicleFromSheet = false }
                )
            }
        }
    }
}

@Composable
private fun VaultFilterTagChip(
    tag: TagEntity,
    selectedTag: String?,
    onSelectedTagChange: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val selected = selectedTag.equals(tag.name, ignoreCase = true)
    FilterChip(
        selected = selected,
        onClick = {
            onSelectedTagChange(if (selected) null else tag.name)
            onDismiss()
        },
        label = { Text(tag.name, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
        colors = vaultTagChipColors(),
        border = vaultTagChipBorder(selected),
        shape = RoundedCornerShape(10.dp)
    )
}

@Composable
private fun VaultFilterVehicleRow(
    label: String,
    emoji: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) SpotVaultColors.Teal.copy(alpha = 0.14f) else SpotVaultColors.Deep.copy(alpha = 0.5f))
            .border(
                1.dp,
                if (selected) SpotVaultColors.Teal.copy(alpha = 0.5f) else SpotVaultColors.Outline.copy(alpha = 0.35f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (emoji != null) {
            Text(emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) SpotVaultColors.Teal else SpotVaultColors.OnSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(18.dp))
        }
    }
}

/** Small chip row for a location card: capped at 2 tags plus a "+N" overflow indicator so a
 * heavily-tagged spot never grows the card's height. */
@Composable
fun VaultTagChipsRow(tags: List<TagEntity>, modifier: Modifier = Modifier) {
    if (tags.isEmpty()) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tags.take(2).forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SpotVaultColors.Teal.copy(alpha = 0.16f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    tag.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SpotVaultColors.Teal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (tags.size > 2) {
            Text(
                "+${tags.size - 2}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SpotVaultColors.Muted
            )
        }
    }
}

/** Assign/remove tags on one spot — reached from its ⋮ menu ("Edit Tags"). */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VaultTagEditorDialog(
    item: LocationSpot,
    currentTags: List<TagEntity>,
    allTags: List<TagEntity>,
    tagDao: TagDao,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit
) {
    var newTagInput by remember { mutableStateOf("") }
    // Recomputed from currentTags every recomposition, not local mutable state — currentTags
    // itself flows straight from the live Room Flow through the parent card, so toggling a chip
    // here (write, then let the Flow round-trip back down) already reflects instantly without
    // this dialog needing to track its own copy of what's assigned.
    val currentTagIds = remember(currentTags) { currentTags.map { it.id }.toSet() }

    fun toggleTag(tag: TagEntity) {
        coroutineScope.launch(Dispatchers.IO) {
            if (currentTagIds.contains(tag.id)) tagDao.removeTag(item.id, tag.id) else tagDao.assignTag(item.id, tag.name)
        }
    }

    fun addNewTag() {
        val trimmed = newTagInput.trim()
        if (trimmed.isEmpty()) return
        coroutineScope.launch(Dispatchers.IO) { tagDao.assignTag(item.id, trimmed) }
        newTagInput = ""
    }

    VaultOverlayDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Edit Tags",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = SpotVaultColors.Teal,
                letterSpacing = 0.5.sp
            )
            if (allTags.isEmpty()) {
                Text("No tags yet — add your first one below.", color = SpotVaultColors.Muted, fontSize = 13.sp)
            } else {
                Text(
                    "Tap a tag to add or remove it from this spot.",
                    color = SpotVaultColors.Muted,
                    fontSize = 12.sp
                )
                Box(
                    modifier = Modifier
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tagsForPicker = remember(allTags, newTagInput) {
                            val filtered = if (newTagInput.isBlank()) allTags
                            else allTags.filter { it.name.contains(newTagInput, ignoreCase = true) }
                            filtered.take(TAG_CLOUD_CHIP_CAP) to filtered.size
                        }
                        tagsForPicker.first.forEach { tag ->
                            val selected = currentTagIds.contains(tag.id)
                            FilterChip(
                                selected = selected,
                                onClick = { toggleTag(tag) },
                                label = {
                                    Text(tag.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = vaultTagChipColors(),
                                border = vaultTagChipBorder(selected),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        if (tagsForPicker.second > TAG_CLOUD_CHIP_CAP) {
                            Text(
                                "Showing $TAG_CLOUD_CHIP_CAP of ${tagsForPicker.second} — type to find more",
                                color = SpotVaultColors.Muted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = newTagInput,
                onValueChange = { if (it.length <= 30) newTagInput = it },
                placeholder = { Text("New tag…", color = SpotVaultColors.Muted.copy(alpha = 0.7f)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = spotEditFieldColors(),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (newTagInput.isNotBlank()) {
                            IconButton(onClick = { addNewTag() }) {
                                Icon(Icons.Default.Add, contentDescription = "Add tag", tint = SpotVaultColors.Teal)
                            }
                        }
                        VoiceMicButton(
                            onResult = { spoken -> newTagInput = spoken; addNewTag() },
                            prompt = "Speak a tag…"
                        )
                    }
                }
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Done", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Settings → Vault's tag management screen — the replacement for the old "Manage Categories"
 * entry. Rename/delete every tag globally; renaming onto an existing tag's name merges the two
 * (TagDao.renameTag) rather than leaving duplicates. */
@Composable
fun VaultTagManagerDialog(
    tagDao: TagDao,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit
) {
    val allTags by tagDao.getAllTags().collectAsState(initial = emptyList())
    var newTagInput by remember { mutableStateOf("") }
    var renamingTag by remember { mutableStateOf<TagEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<TagEntity?>(null) }
    var pendingDeleteAllEmpty by remember { mutableStateOf(false) }
    var tagSearch by remember { mutableStateOf("") }
    var showEmptyOnly by remember { mutableStateOf(false) }

    // Most-used first (secondary alpha sort keeps ties stable) — with hundreds of tags, the
    // ones actually worth managing surface before the long tail of one-off/unused ones.
    val sortedTags = remember(allTags) {
        allTags.sortedWith(compareByDescending<TagEntity> { it.usageCount }.thenBy { it.name.lowercase() })
    }
    val visibleTags = remember(sortedTags, tagSearch, showEmptyOnly) {
        sortedTags.filter { tag ->
            (!showEmptyOnly || tag.usageCount == 0) &&
                (tagSearch.isBlank() || searchTextMatches(tag.name, tagSearch))
        }
    }
    val emptyTagCount = remember(allTags) { allTags.count { it.usageCount == 0 } }

    fun addTag() {
        val name = newTagInput.trim()
        if (name.isEmpty()) return
        coroutineScope.launch(Dispatchers.IO) { tagDao.createTag(name) }
        newTagInput = ""
    }

    PremiumDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Tags", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        content = {
            val tagListMaxHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.55f).dp
                .coerceIn(240.dp, 560.dp)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = tagListMaxHeight),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Assign tags from any spot's ⋮ menu, or filter the Vault by tag from the filter icon.",
                        color = SpotVaultColors.Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                if (allTags.isEmpty()) {
                    item {
                        Text(
                            "No tags yet — add one below, or tag a spot in the Vault.",
                            color = SpotVaultColors.Muted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    item {
                        OutlinedTextField(
                            value = tagSearch,
                            onValueChange = { tagSearch = it },
                            placeholder = { Text("Search tags…", color = SpotVaultColors.Muted.copy(alpha = 0.7f)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SpotVaultColors.Teal) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = spotEditFieldColors()
                        )
                    }
                    if (emptyTagCount > 0) {
                        item {
                            FilterChip(
                                selected = showEmptyOnly,
                                onClick = { showEmptyOnly = !showEmptyOnly },
                                label = { Text("Show Empty ($emptyTagCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = if (showEmptyOnly) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = vaultTagChipColors(),
                                border = vaultTagChipBorder(showEmptyOnly),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    if (showEmptyOnly && visibleTags.isNotEmpty()) {
                        item {
                            TextButton(
                                onClick = { pendingDeleteAllEmpty = true },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = SpotVaultColors.Danger, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Delete All (${visibleTags.size})", color = SpotVaultColors.Danger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (visibleTags.isEmpty()) {
                        item {
                            Text(
                                if (showEmptyOnly) "No unused tags — nice and tidy." else "No tags match \"$tagSearch\"",
                                color = SpotVaultColors.Muted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }
                items(visibleTags, key = { it.id }) { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpotVaultColors.Elevated.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                tag.name,
                                color = SpotVaultColors.OnSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                "${tag.usageCount} spot${if (tag.usageCount == 1) "" else "s"}",
                                color = SpotVaultColors.Muted,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = { renamingTag = tag; renameInput = tag.name },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename", tint = SpotVaultColors.Teal, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { pendingDelete = tag },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SpotVaultColors.Danger, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { if (it.length <= 30) newTagInput = it },
                        placeholder = { Text("New tag…", color = SpotVaultColors.Muted.copy(alpha = 0.7f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = spotEditFieldColors(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (newTagInput.isNotBlank()) {
                                    IconButton(onClick = { addTag() }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add tag", tint = SpotVaultColors.Teal)
                                    }
                                }
                                VoiceMicButton(onResult = { newTagInput = it; addTag() }, prompt = "Speak a tag…")
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            SpotVaultButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = spotVaultButtonShape()
            ) { Text("Done", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {}
    )

    renamingTag?.let { tag ->
        AlertDialog(
            onDismissRequest = { renamingTag = null },
            containerColor = SpotVaultColors.Surface,
            titleContentColor = SpotVaultColors.OnSurface,
            title = { Text("Rename \"${tag.name}\"") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { if (it.length <= 30) renameInput = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = spotEditFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newName = renameInput.trim()
                        if (newName.isNotEmpty()) {
                            coroutineScope.launch(Dispatchers.IO) { tagDao.renameTag(tag.id, newName) }
                        }
                        renamingTag = null
                    }
                ) { Text("Save", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { renamingTag = null }) { Text("Cancel") }
            }
        )
    }

    pendingDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = SpotVaultColors.Surface,
            titleContentColor = SpotVaultColors.OnSurface,
            textContentColor = SpotVaultColors.Muted,
            title = { Text("Delete \"${tag.name}\"?") },
            text = { Text("Removes this tag from every spot it's on (${tag.usageCount} spot${if (tag.usageCount == 1) "" else "s"}). The spots themselves aren't affected.") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch(Dispatchers.IO) { tagDao.deleteTag(tag.id) }
                    pendingDelete = null
                }) { Text("Delete", color = SpotVaultColors.Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (pendingDeleteAllEmpty) {
        // Snapshot the ids at confirm-time — visibleTags could otherwise shift mid-deletion
        // as each tagDao.deleteTag call flows back through getAllTags() and recomputes it.
        val idsToDelete = visibleTags.map { it.id }
        AlertDialog(
            onDismissRequest = { pendingDeleteAllEmpty = false },
            containerColor = SpotVaultColors.Surface,
            titleContentColor = SpotVaultColors.OnSurface,
            textContentColor = SpotVaultColors.Muted,
            title = { Text("Delete ${idsToDelete.size} unused tag${if (idsToDelete.size == 1) "" else "s"}?") },
            text = { Text("These tags have zero spots on them. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        idsToDelete.forEach { id -> tagDao.deleteTag(id) }
                    }
                    pendingDeleteAllEmpty = false
                }) { Text("Delete All", color = SpotVaultColors.Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteAllEmpty = false }) { Text("Cancel") }
            }
        )
    }
}

private fun findMainActivity(context: Context): MainActivity? {
    var host: Context? = context
    while (host is android.content.ContextWrapper) {
        if (host is MainActivity) return host
        host = host.baseContext
    }
    return host as? MainActivity
}


@Composable
fun VaultAddPhotoDialog(item: LocationSpot, onDismiss: () -> Unit) {
    val context = LocalContext.current
    VaultOverlayDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Add Photo",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = SpotVaultColors.Teal,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Attach a photo to this vault entry.",
                fontSize = 13.sp,
                color = SpotVaultColors.Muted,
                lineHeight = 18.sp
            )
            SpotVaultButton(
                onClick = {
                    findMainActivity(context)?.launchCameraForSpot(item)
                    onDismiss()
                },
                shape = spotVaultButtonShape(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Take Photo", fontWeight = FontWeight.Bold)
            }
            SpotVaultOutlinedButton(
                onClick = {
                    findMainActivity(context)?.launchGalleryPickerForSpot(item)
                    onDismiss()
                },
                shape = spotVaultButtonShape(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Add from Gallery", fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Cancel", color = SpotVaultColors.Teal)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VaultHistorySpotGridCard(
    item: LocationSpot,
    tags: List<TagEntity>,
    allTags: List<TagEntity>,
    tagDao: TagDao,
    prefs: SharedPreferences,
    selectedItems: Set<Int>,
    selectionModeActive: Boolean,
    onViewSpot: (LocationSpot) -> Unit,
    onShareRequest: (ShareSpotPayload) -> Unit,
    onSelectionChange: (Set<Int>) -> Unit,
    onSelectionModeActiveChange: (Boolean) -> Unit,
    coroutineScope: CoroutineScope,
    dao: LocationDao
) {
    val address = item.address.ifBlank { item.title }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddPhotoDialog by remember { mutableStateOf(false) }
    var showTagEditor by remember { mutableStateOf(false) }

    fun toggleSelection() {
        onSelectionChange(
            if (selectedItems.contains(item.id)) selectedItems - item.id
            else selectedItems + item.id
        )
    }

    // Same combinedClickable pattern as the list card (VaultHistorySpotCard) — grid used to
    // always call onViewSpot on tap regardless of an active multi-select session, and had no
    // long-press of its own to start one, so switching from list to grid view mid-selection (or
    // just using grid first) meant tapping a card opened it instead of toggling it like every
    // other selection surface in the Vault does.
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionModeActive) toggleSelection() else onViewSpot(item)
                },
                onLongClick = {
                    onSelectionModeActiveChange(true)
                    onSelectionChange(selectedItems + item.id)
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = SpotVaultColors.Elevated)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.82f)
        ) {
            VaultSpotThumbnail(
                item = item,
                onClick = { if (selectionModeActive) toggleSelection() else onViewSpot(item) },
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp)
            )

            // Grid view had no way to favorite/unfavorite at all — this used to be a plain
            // read-only Icon shown only when already favorited (no tap target, and invisible
            // otherwise), even though the list card right next to it in this same file has a
            // working favorite toggle. Now a real IconButton, always visible, same as the list
            // card's — backed by a translucent scrim since it sits directly over a photo
            // thumbnail rather than a solid card background.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(SpotVaultColors.Surface.copy(alpha = 0.85f), CircleShape)
            ) {
                IconButton(
                    onClick = {
                        val spotId = item.id
                        coroutineScope.launch(Dispatchers.IO) { dao.toggleFavorite(spotId) }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (item.isFavorite) "Remove Favorite" else "Add Favorite",
                        tint = if (item.isFavorite) SpotVaultColors.Danger else SpotVaultColors.Muted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                if (selectionModeActive) {
                    Box(
                        modifier = Modifier
                            .background(SpotVaultColors.Surface.copy(alpha = 0.88f), RoundedCornerShape(8.dp))
                    ) {
                        Checkbox(
                            checked = selectedItems.contains(item.id),
                            onCheckedChange = { isChecked ->
                                onSelectionChange(
                                    if (isChecked) selectedItems + item.id
                                    else selectedItems - item.id
                                )
                            },
                            colors = spotVaultCheckboxColors(),
                            modifier = Modifier.scale(0.85f)
                        )
                    }
                } else {
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SpotVaultColors.Void.copy(alpha = 0.65f))
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More actions", tint = SpotVaultColors.OnSurface, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            VaultSpotOverflowMenuItems(
                                item = item,
                                onEdit = { showEditDialog = true },
                                onShareRequest = onShareRequest,
                                onAddPhoto = { showAddPhotoDialog = true },
                                onEditTags = { showTagEditor = true },
                                onDismissMenu = { menuExpanded = false },
                                coroutineScope = coroutineScope,
                                dao = dao
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                SpotVaultColors.Void.copy(alpha = 0.55f),
                                SpotVaultColors.Void.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (address.isNotEmpty()) {
                        Text(
                            address,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SpotVaultColors.OnSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 15.sp
                        )
                    }
                    if (tags.isNotEmpty()) {
                        VaultTagChipsRow(tags = tags)
                    }
                }
            }
        }
    }
    if (showEditDialog) {
        SpotEditDialog(
            prefs = prefs,
            spotId = item.id,
            currentTitle = item.title,
            currentTimestamp = item.timestamp,
            currentNotes = rememberFullSpotNotes(dao, item.id, item.locationDetails),
            currentCity = item.city,
            currentState = item.state,
            currentVehicleId = item.vehicleId,
            onDismiss = { showEditDialog = false },
            onSave = { newTitle, newTimestamp, newNotes, newCity, newState, newVehicleId ->
                val spotId = item.id
                coroutineScope.launch(Dispatchers.IO) {
                    // Re-fetches rather than item.copy(...) — item is a snapshot from whenever
                    // this card last recomposed, which a fast enough sequence of actions (e.g.
                    // attaching a photo, then opening Edit and saving before the list's Flow had
                    // caught up) could beat. copy()-ing a stale snapshot would silently write its
                    // old imagePath back over the new one, erasing a photo that had just been
                    // added. Reading the current row immediately before the update closes that
                    // window regardless of how stale item itself is by the time Save is tapped.
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
                showEditDialog = false
            }
        )
    }
    if (showAddPhotoDialog) {
        VaultAddPhotoDialog(item = item, onDismiss = { showAddPhotoDialog = false })
    }
    if (showTagEditor) {
        VaultTagEditorDialog(
            item = item,
            currentTags = tags,
            allTags = allTags,
            tagDao = tagDao,
            coroutineScope = coroutineScope,
            onDismiss = { showTagEditor = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VaultSectionStickyHeader(
    sectionTitle: String,
    count: Int,
    collapsed: Boolean,
    onToggle: () -> Unit,
    // Long-pressing any section header selects every spot in every currently-expanded section
    // (never a collapsed one, even if you long-press its own header while it's collapsed — the
    // point is "select what I can see", not "select this specific section regardless of state").
    // No-op default keeps every other VaultSectionStickyHeader call site (Recently Deleted,
    // Archived Spots — neither supports multi-select) working unchanged.
    onLongPress: () -> Unit = {}
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (collapsed) -90f else 0f,
        animationSpec = tween(200),
        label = "vaultSectionChevron"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpotVaultColors.Surface)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onToggle, onLongClick = onLongPress)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = if (collapsed) "Expand" else "Collapse",
            tint = SpotVaultColors.Teal,
            // .rotate(Float) reads chevronRotation at composable-body scope, recomposing this
            // whole header row on every frame of the 200ms toggle tween — graphicsLayer defers
            // the same read to the draw phase instead, same visuals.
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = chevronRotation }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = sectionTitle.uppercase(Locale.getDefault()),
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 1.2.sp,
            color = SpotVaultColors.Teal
        )
        if (collapsed) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$count",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = SpotVaultColors.Muted
            )
        }
    }
}

@Composable
fun VaultSpotEntry(
    item: LocationSpot,
    vehicle: Vehicle?,
    tags: List<TagEntity>,
    allTags: List<TagEntity>,
    tagDao: TagDao,
    prefs: SharedPreferences,
    selectedItems: Set<Int>,
    selectionModeActive: Boolean = false,
    onSelectionModeActiveChange: (Boolean) -> Unit = {},
    vaultViewMode: VaultViewMode,
    onViewSpot: (LocationSpot) -> Unit,
    onShareRequest: (ShareSpotPayload) -> Unit,
    onSelectionChange: (Set<Int>) -> Unit,
    onSwipeArchive: (LocationSpot) -> Unit,
    onSwipeDelete: (LocationSpot) -> Unit,
    coroutineScope: CoroutineScope,
    dao: LocationDao,
    swipeGestureEnabled: Boolean = true,
    userLocation: Pair<Double, Double>? = null,
    distanceUnit: String? = null
) {
    VaultSpotSwipeContainer(
        item = item,
        onArchive = { onSwipeArchive(item) },
        onDelete = { onSwipeDelete(item) },
        gestureEnabled = swipeGestureEnabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (vaultViewMode) {
            VaultViewMode.LIST -> VaultHistorySpotCard(
                item = item,
                vehicle = vehicle,
                tags = tags,
                allTags = allTags,
                tagDao = tagDao,
                prefs = prefs,
                selectedItems = selectedItems,
                selectionModeActive = selectionModeActive,
                onViewSpot = onViewSpot,
                onShareRequest = onShareRequest,
                onSelectionChange = onSelectionChange,
                onSelectionModeActiveChange = onSelectionModeActiveChange,
                coroutineScope = coroutineScope,
                dao = dao,
                userLocation = userLocation,
                distanceUnit = distanceUnit
            )
            VaultViewMode.GRID -> VaultHistorySpotGridCard(
                item = item,
                tags = tags,
                allTags = allTags,
                tagDao = tagDao,
                prefs = prefs,
                selectedItems = selectedItems,
                selectionModeActive = selectionModeActive,
                onViewSpot = onViewSpot,
                onShareRequest = onShareRequest,
                onSelectionChange = onSelectionChange,
                onSelectionModeActiveChange = onSelectionModeActiveChange,
                coroutineScope = coroutineScope,
                dao = dao
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryVaultTabPage(
    prefs: SharedPreferences,
    dao: LocationDao,
    isPinned: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortBy: String,
    onSortByChange: (String) -> Unit,
    showFavoritesOnly: Boolean,
    onShowFavoritesOnlyChange: (Boolean) -> Unit,
    selectedVehicleId: Int?,
    onSelectedVehicleIdChange: (Int?) -> Unit,
    selectedItems: Set<Int>,
    onSelectedItemsChange: (Set<Int>) -> Unit,
    onShowDeleteConfirm: () -> Unit,
    onSwipeDeleteSpot: (LocationSpot) -> Unit,
    onShareRequest: (ShareSpotPayload) -> Unit,
    onViewSpot: (LocationSpot) -> Unit,
    vaultViewMode: VaultViewMode,
    onVaultViewModeChange: (VaultViewMode) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    // The screen's own title/dropdown/icon row — rendered by the caller today, but pulled in as
    // a slot so landscape can place it above the filters in a left-hand column instead of full
    // width above everything, without duplicating the state this composable already owns.
    headerContent: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val userLocation = rememberUserLocationForDistance()
    val distanceUnit = rememberDistanceUnit(prefs)
    val vehicleDao = remember { AppDatabase.getDatabase(context).vehicleDao() }
    val allVehicles by vehicleDao.observeAll().collectAsState(initial = emptyList())
    var showArchivedVehicles by remember { mutableStateOf(false) }
    val vehicleById = remember(allVehicles) { allVehicles.associateBy { it.id } }
    var selectionModeActive by remember { mutableStateOf(false) }

    val tagDao = remember { AppDatabase.getDatabase(context).tagDao() }
    val topTags by tagDao.getTopTags().collectAsState(initial = emptyList())
    val allTags by tagDao.getAllTags().collectAsState(initial = emptyList())
    // rememberSaveable, not remember, for everything below the user actively picks — matches
    // the identical filter state in VaultFilterableSpotList (Favorites Hub/Calendar/Location
    // Browser), which already survives rotation the same way. This tab was the one place still
    // silently dropping the selection back to Off on a config change.
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var showTagFilterSheet by remember { mutableStateOf(false) }
    var nearMeFilter by rememberSaveable(
        stateSaver = androidx.compose.runtime.saveable.Saver(
            save = { it.name },
            restore = { VaultNearMeFilter.valueOf(it) }
        )
    ) { mutableStateOf(VaultNearMeFilter.OFF) }
    var showPhotosOnly by rememberSaveable { mutableStateOf(false) }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val gridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()

    // Windowed browse (notes stripped) — replaces collecting getAllHistory() into Compose.
    val historyList = rememberVaultBrowseSpots(
        dao = dao,
        searchQuery = searchQuery,
        sortBy = sortBy,
        nearMeFilter = nearMeFilter,
        selectedTag = selectedTag,
        showFavoritesOnly = showFavoritesOnly,
        selectedVehicleId = selectedVehicleId,
        listState = listState,
        gridState = gridState,
        vaultViewMode = vaultViewMode
    )
    val spotIdToTags = rememberVaultSpotIdToTags(
        tagDao = tagDao,
        spotIds = historyList.map { it.id }
    )
    val pinnedSpots = remember(historyList) {
        historyList.filter { it.isPinned && it.deletedAt == null && !it.isArchived && !it.isWishlist }
            .sortedByDescending { it.timestamp }
    }

    LaunchedEffect(selectedItems) {
        if (selectedItems.isEmpty()) selectionModeActive = false
    }

    // Bulk actions (Delete/Export/GPX) resolve selectedItems against the full unfiltered
    // historyList by id, so changing the filter or search out from under an active selection
    // used to leave "N Selected" pointing at spots that had scrolled out of view or were no
    // longer even shown under the new filter — tapping Delete would silently act on spots the
    // user couldn't currently see and likely didn't mean to include. Clearing the selection
    // whenever what's actually on screen changes keeps "N Selected" honest about what Delete/
    // Export will actually touch.
    LaunchedEffect(searchQuery, showFavoritesOnly, selectedVehicleId, sortBy, selectedTag, nearMeFilter, showPhotosOnly) {
        if (selectedItems.isNotEmpty()) onSelectedItemsChange(emptySet())
    }

    // If the vehicle the active filter points at gets archived or deleted while it's selected,
    // the filter chip's own dropdown either hides that vehicle or (once none are left) disappears
    // entirely — leaving no visible control to clear a filter that's now silently hiding every
    // spot (deleting a vehicle nulls out vehicleId on its spots, so nothing can match a stale id
    // anymore either way). Resetting here as soon as the selection stops resolving to a real,
    // active vehicle keeps the Vault from looking permanently empty for no visible reason.
    LaunchedEffect(allVehicles, selectedVehicleId) {
        val id = selectedVehicleId
        if (id != null && allVehicles.none { it.id == id && !it.isArchived }) {
            onSelectedVehicleIdChange(null)
        }
    }

    val filteredList = rememberFilteredVaultSpots(
        spots = historyList,
        searchQuery = searchQuery,
        showFavoritesOnly = showFavoritesOnly,
        vehicleId = selectedVehicleId,
        sortBy = sortBy,
        userLocation = userLocation,
        tagsBySpotId = spotIdToTags,
        vehicleNameById = vehicleById.mapValues { it.value.name },
        nearMeFilter = nearMeFilter,
        showPhotosOnly = showPhotosOnly,
        selectedTag = selectedTag
    )

    // NEWEST/OLDEST are themselves date-based, so bucketing by date section (each bucket already
    // ordered newest/oldest-first within itself) matches what the sort menu promises. CLOSEST and
    // ALPHABETICAL have nothing to do with date, though — bucketing those the same way used to
    // mean a spot 40 miles away saved today could rank above one 50 meters away saved last month,
    // because "Today" as a section always sorted first regardless of which sort was picked. A
    // single flat section (still collapsible, like any other) actually orders the full list by
    // whatever the sort menu says instead of only within each date bucket.
    val sectionedSpots = remember(filteredList, sortBy) {
        if (sortBy == VaultSortOption.CLOSEST || sortBy == VaultSortOption.ALPHABETICAL) {
            if (filteredList.isEmpty()) emptyList() else listOf("Results" to filteredList)
        } else {
            groupSpotsByDateSection(filteredList)
        }
    }

    // Only the newest section starts expanded — normally "Today", but when there's nothing
    // from today the newest section is whatever's next (e.g. "Yesterday"), so that opens
    // instead rather than leaving every section collapsed. seenSections tracks which section
    // titles have already had their default applied, so a section the user manually
    // re-expanded doesn't snap back shut on the next recomposition, while a brand-new section
    // (e.g. the clock rolling into "Yesterday") still opens collapsed the first time it
    // appears if it's no longer the newest one.
    val seenSections = remember { mutableSetOf<String>() }
    var collapsedSections by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(sectionedSpots.map { it.first }) {
        val newestSectionTitle = sectionedSpots.firstOrNull()?.first
        sectionedSpots.map { it.first }.forEach { title ->
            if (seenSections.add(title) && title != newestSectionTitle) {
                collapsedSections = collapsedSections + title
            }
        }
    }

    val onSwipeArchive: (LocationSpot) -> Unit = { spot ->
        val spotId = spot.id
        coroutineScope.launch(Dispatchers.IO) { dao.archiveSpot(spotId) }
        VaultUndoSnackbar.show("Spot archived") {
            withContext(Dispatchers.IO) { dao.unarchiveSpotIfArchived(spotId) }
        }
    }

    // Shared between both view modes so spotContent (defined once, used by both) can always
    // tell whether the currently-active list is mid-scroll. Swipe-to-delete/favorite and
    // vertical scroll both start from a drag gesture, so a slow, not-perfectly-vertical drag
    // can otherwise be ambiguous between the two and visibly jitter as they fight over it — a
    // fast fling has an unambiguous velocity/direction and never shows the problem, which
    // matches the reported "slow = jittery, fast = fine" pattern exactly. Disabling the swipe
    // gesture while a scroll is actually in progress removes the competition entirely.
    val isVaultScrolling = if (vaultViewMode == VaultViewMode.GRID) {
        gridState.isScrollInProgress
    } else {
        listState.isScrollInProgress
    }

    val spotContent: @Composable (LocationSpot) -> Unit = { item ->
        VaultSpotEntry(
            item = item,
            vehicle = item.vehicleId?.let { vehicleById[it] },
            tags = spotIdToTags[item.id].orEmpty(),
            allTags = allTags,
            tagDao = tagDao,
            prefs = prefs,
            selectedItems = selectedItems,
            selectionModeActive = selectionModeActive,
            onSelectionModeActiveChange = { selectionModeActive = it },
            vaultViewMode = vaultViewMode,
            onViewSpot = onViewSpot,
            onShareRequest = onShareRequest,
            onSelectionChange = onSelectedItemsChange,
            onSwipeArchive = onSwipeArchive,
            onSwipeDelete = onSwipeDeleteSpot,
            coroutineScope = coroutineScope,
            dao = dao,
            // Every other single-item action (the overflow menu, tap-to-view) is already
            // suppressed in favor of the checkbox/bulk-action UI the moment selection mode is
            // active — a swipe firing an immediate single-item archive/delete was the one
            // exception, letting an accidental drag while trying to check a box silently act on
            // an item outside the batch the user is actually building.
            swipeGestureEnabled = !isVaultScrolling && !selectionModeActive,
            userLocation = userLocation,
            distanceUnit = distanceUnit
        )
    }

    val vaultListHeader: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isPinned && searchQuery.isEmpty()) {
                VaultActivePinBlock(prefs = prefs, context = context)
            }
            VaultSearchSortRow(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                sortBy = sortBy,
                onSortByChange = onSortByChange,
                vaultViewMode = vaultViewMode,
                onVaultViewModeChange = onVaultViewModeChange,
                prefs = prefs,
                onOpenTagSheet = { showTagFilterSheet = true }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VaultTagFilterBar(
                    topTags = topTags,
                    showFavoritesOnly = showFavoritesOnly,
                    onShowFavoritesOnlyChange = onShowFavoritesOnlyChange,
                    nearMeFilter = nearMeFilter,
                    onNearMeFilterChange = { nearMeFilter = it },
                    showPhotosOnly = showPhotosOnly,
                    onShowPhotosOnlyChange = { showPhotosOnly = it },
                    selectedTag = selectedTag,
                    onSelectedTagChange = { selectedTag = it },
                    activeVehicleLabel = allVehicles.firstOrNull { it.id == selectedVehicleId }
                        ?.let { "${vehicleIconEmoji(it.iconKey)} ${it.name}" },
                    onClearVehicle = { onSelectedVehicleIdChange(null) },
                    modifier = Modifier.weight(1f),
                    // The gold Star button in the header (vaultHeaderRow, MainActivity.kt) opens
                    // the Favorites Hub now — the scrollable filter row doesn't need its own
                    // Favorites chip duplicating that entry point.
                    showFavoritesChip = false
                )
            }
            if (pinnedSpots.isNotEmpty() && searchQuery.isEmpty()) {
                VaultPinnedSpotsRow(
                    pinnedSpots = pinnedSpots,
                    userLocation = userLocation,
                    distanceUnit = distanceUnit,
                    onViewSpot = onViewSpot,
                    dao = dao,
                    coroutineScope = coroutineScope
                )
            }
            if (selectedItems.isNotEmpty()) {
                VaultSelectionBar(
                    selectedCount = selectedItems.size,
                    historyList = historyList,
                    selectedItems = selectedItems,
                    onSelectAll = {
                        onSelectedItemsChange(
                            sectionedSpots
                                .filterNot { (title, _) -> title in collapsedSections }
                                .flatMap { (_, sectionSpots) -> sectionSpots.map { it.id } }
                                .toSet()
                        )
                    },
                    onShowDeleteConfirm = onShowDeleteConfirm
                )
            }
        }
    }

    // Extracted so both the portrait (stacked) and landscape (side-by-side) arrangements below
    // can render the exact same feed logic instead of forking it — only the surrounding layout
    // differs between them, never the feed itself.
    val feedContent: @Composable ColumnScope.() -> Unit = {
        if (filteredList.isEmpty()) {
            VaultEmptyState(
                showFavoritesOnly = showFavoritesOnly,
                selectedCategoryLabel = null
            )
        } else if (vaultViewMode == VaultViewMode.GRID) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyVerticalStaggeredGrid(
                    // Adaptive, not Fixed(2) — a fixed column count looks right on a portrait
                    // phone (where this was tuned) but stays stuck at 2 abnormally wide/short
                    // cards on a phone rotated to landscape or on a tablet, both of which have
                    // plenty of extra width this grid was never using. 160dp keeps exactly 2
                    // columns at the phone-portrait widths this always ran at before, and grows
                    // from there as real width becomes available.
                    columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                        .padding(end = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp,
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    sectionedSpots.forEach { (sectionTitle, spots) ->
                        val isCollapsed = sectionTitle in collapsedSections
                        item(key = "header_$sectionTitle", span = StaggeredGridItemSpan.FullLine) {
                            VaultSectionStickyHeader(
                                sectionTitle = sectionTitle,
                                count = spots.size,
                                collapsed = isCollapsed,
                                onToggle = {
                                    collapsedSections = if (isCollapsed) {
                                        collapsedSections - sectionTitle
                                    } else {
                                        collapsedSections + sectionTitle
                                    }
                                },
                                onLongPress = {
                                    val expandedIds = sectionedSpots
                                        .filterNot { (title, _) -> title in collapsedSections }
                                        .flatMap { (_, sectionSpots) -> sectionSpots.map { it.id } }
                                        .toSet()
                                    onSelectedItemsChange(expandedIds)
                                    selectionModeActive = true
                                }
                            )
                        }
                        if (!isCollapsed) {
                            items(spots, key = { it.id }) { item ->
                                spotContent(item)
                            }
                        }
                    }
                }
                // Grid mode had no scroll indicator of its own at all before this — an
                // "obvious glowing slider" (emphasizeHint pulses the glow harder/faster) is
                // the only cue a 2-column staggered grid gets that there's more below, since
                // the bounce-arrow hint reads oddly next to a grid rather than a single list.
                val canScrollGrid = gridState.canScrollForward || gridState.canScrollBackward
                if (canScrollGrid) {
                    PremiumVerticalScrollbar(
                        gridState = gridState,
                        emphasizeHint = true,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp)
                            .width(6.dp)
                    )
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                        .padding(end = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    sectionedSpots.forEach { (sectionTitle, spots) ->
                        val isCollapsed = sectionTitle in collapsedSections
                        stickyHeader(key = "header_$sectionTitle") {
                            VaultSectionStickyHeader(
                                sectionTitle = sectionTitle,
                                count = spots.size,
                                collapsed = isCollapsed,
                                onToggle = {
                                    collapsedSections = if (isCollapsed) {
                                        collapsedSections - sectionTitle
                                    } else {
                                        collapsedSections + sectionTitle
                                    }
                                },
                                onLongPress = {
                                    val expandedIds = sectionedSpots
                                        .filterNot { (title, _) -> title in collapsedSections }
                                        .flatMap { (_, sectionSpots) -> sectionSpots.map { it.id } }
                                        .toSet()
                                    onSelectedItemsChange(expandedIds)
                                    selectionModeActive = true
                                }
                            )
                        }
                        if (!isCollapsed) {
                            items(spots, key = { it.id }) { item ->
                                spotContent(item)
                            }
                        }
                    }
                }
                val canScrollList = listState.canScrollForward || listState.canScrollBackward
                if (canScrollList) {
                    PremiumVerticalScrollbar(
                        listState = listState,
                        emphasizeHint = true,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp)
                            .width(6.dp)
                    )
                }
                // Overlaid rather than a layout sibling below the list — canScrollForward
                // flickers right as you approach the bottom of the list, and a sibling here
                // would mount/unmount on every flicker, resizing the LazyColumn's height
                // mid-scroll and producing exactly the stutter/glitch this was reported as.
                if (listState.canScrollForward) {
                    PremiumScrollMoreHint(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp),
                        includeNavBarInset = false
                    )
                }
            }
        }
    }

    if (needsCompactHeightLayout()) {
        // Side-by-side instead of stacked: a phone rotated to landscape has plenty of width but
        // much less height than the portrait layout this screen was designed around — stacking
        // the header, search, filters, and pinned row above the feed the same way used to leave
        // almost no room for the feed itself, forcing an awkward scroll through squished controls
        // just to see any spots. The same trap can happen in portrait too on a short enough
        // window (compact split-screen, a small device), which is why this checks
        // needsCompactHeightLayout() rather than orientation alone. The filter/header column
        // scrolls on its own since it can still run taller than a short window; the feed gets its
        // own full-height area next to it either way.
        Row(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                headerContent()
                vaultListHeader()
            }
            Column(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
                    .padding(start = 12.dp)
            ) {
                feedContent()
            }
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            headerContent()
            vaultListHeader()
            feedContent()
        }
    }

    if (showTagFilterSheet) {
        VaultTagFilterSheet(
            allTags = allTags,
            selectedTag = selectedTag,
            onSelectedTagChange = { selectedTag = it },
            vehicles = allVehicles,
            selectedVehicleId = selectedVehicleId,
            onSelectedVehicleIdChange = onSelectedVehicleIdChange,
            showArchivedVehicles = showArchivedVehicles,
            onShowArchivedVehiclesChange = { showArchivedVehicles = it },
            tagDao = tagDao,
            vehicleDao = vehicleDao,
            locationDao = dao,
            coroutineScope = coroutineScope,
            onDismiss = { showTagFilterSheet = false }
        )
    }
}

/** Compact horizontally-scrolling row of user-pinned spots ("bitty squares"), shown above the
 * chronological feed's own date headers so the handful of spots someone cares about most stay
 * one glance away regardless of where they land in the full sorted list below. */
@Composable
private fun VaultPinnedSpotsRow(
    pinnedSpots: List<LocationSpot>,
    userLocation: Pair<Double, Double>?,
    distanceUnit: String,
    onViewSpot: (LocationSpot) -> Unit,
    dao: LocationDao,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "ACTIVE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = SpotVaultColors.Muted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(pinnedSpots, key = { it.id }) { spot ->
                VaultPinnedSpotCard(
                    spot = spot,
                    distanceLabel = userLocation?.let {
                        formatSpotDistanceLabel(it.first, it.second, spot.lat, spot.lng, distanceUnit)
                            .removeSuffix(" away")
                    },
                    onClick = { onViewSpot(spot) },
                    onUnpin = {
                        val spotId = spot.id
                        coroutineScope.launch(Dispatchers.IO) { dao.setPinned(spotId, false) }
                        VaultUndoSnackbar.show("Removed from Active") {
                            withContext(Dispatchers.IO) { dao.setPinned(spotId, true) }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VaultPinnedSpotCard(
    spot: LocationSpot,
    distanceLabel: String?,
    onClick: () -> Unit,
    onUnpin: () -> Unit
) {
    val path = spot.imagePath
    val hasValidPhoto = remember(path) {
        path.isNotEmpty() && runCatching { File(path).exists() }.getOrDefault(false)
    }
    val shape = RoundedCornerShape(14.dp)
    val label = spot.title

    Box(
        modifier = Modifier
            .size(90.dp)
            .clip(shape)
            .background(SpotVaultColors.Glass)
            .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.3f), shape)
            .clickable(onClick = onClick)
    ) {
        if (hasValidPhoto) {
            Image(
                painter = coil.compose.rememberAsyncImagePainter(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(File(path))
                        .size(180, 180)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            VaultNoPhotoPlaceholder(modifier = Modifier.fillMaxSize())
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                    )
                )
        )
        // Box-wrapped so the tappable area is meaningfully bigger than the 16dp icon itself —
        // same touch-target fix as the tag/vehicle chip remove buttons.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(32.dp)
                .clickable(onClick = onUnpin),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.VerticalAlignBottom,
                contentDescription = "Remove from Active",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(16.dp)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp)
        ) {
            Text(
                label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (distanceLabel != null) {
                Text(
                    distanceLabel,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun VaultActivePinBlock(prefs: SharedPreferences, context: android.content.Context) {
    val addr = prefs.getString("current_address", "") ?: ""
    val title = prefs.getString("location_details", "") ?: ""
    val lat = prefs.getCoord("lat").toFloat()
    val lng = prefs.getCoord("lng").toFloat()

    Text(
        "ACTIVE SPOT",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = SpotVaultColors.PrimaryBright,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .border(1.dp, SpotVaultColors.PrimaryBright, RoundedCornerShape(16.dp)),
        colors = CardDefaults.elevatedCardColors(containerColor = SpotVaultColors.Elevated),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(SpotVaultColors.PrimaryBright, androidx.compose.foundation.shape.CircleShape)
                            .size(8.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Now Tracking", fontSize = 10.sp, color = SpotVaultColors.PrimaryBright, fontWeight = FontWeight.Bold)
                }
            }
            if (title.isNotEmpty()) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SpotVaultColors.OnSurface, modifier = Modifier.padding(top = 8.dp))
            }
            if (addr.isNotEmpty()) {
                Text(addr, fontSize = 13.sp, color = SpotVaultColors.Muted, modifier = Modifier.padding(top = 4.dp))
            }
            SpotVaultButton(
                onClick = {
                    val uri = "geo:0,0?q=$lat,$lng"
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))
                    try {
                        context.startActivity(intent)
                    } catch (e: android.content.ActivityNotFoundException) {
                        android.widget.Toast.makeText(context, "No map app found", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = spotVaultButtonShape(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = SpotVaultColors.Deep,
                    contentColor = SpotVaultColors.Teal
                )
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                Text("Navigate to Active Spot")
            }
        }
    }
}

@Composable
private fun VaultSearchSortRow(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortBy: String,
    onSortByChange: (String) -> Unit,
    vaultViewMode: VaultViewMode,
    onVaultViewModeChange: (VaultViewMode) -> Unit,
    prefs: SharedPreferences,
    onOpenTagSheet: () -> Unit,
    // Off for the Calendar/Location Browser drill-down lists — that button persists the choice
    // to the same prefs key the main Vault tab reads (saveVaultViewMode), so leaving it on here
    // would let a change made from inside a day/city view silently change the main Vault's own
    // view mode too, which isn't what's being edited from this screen.
    showViewModeToggle: Boolean = true
) {
    var expandedSort by remember { mutableStateOf(false) }

    val deepColor = SpotVaultColors.Deep
    val tealColor = SpotVaultColors.Teal
    val outlineColor = SpotVaultColors.Outline
    val onSurfaceColor = SpotVaultColors.OnSurface
    val mutedColor = SpotVaultColors.Muted

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Custom-built compact pill instead of Material3's OutlinedTextField — that default
        // reserves enough internal padding for a label + helper text that this field never uses,
        // which made it noticeably taller than a plain search bar needs to be (compare Facebook's,
        // Google's, etc. — all a slim pill, not a full form field). Building the decoration by
        // hand with BasicTextField is what actually lets the height come down; wrapping
        // OutlinedTextField in a smaller height modifier doesn't work; its intrinsic content
        // height (driven by internal padding, not this modifier) ignores anything shorter than
        // its own built-in minimum.
        val searchInteractionSource = remember { MutableInteractionSource() }
        val isSearchFocused by searchInteractionSource.collectIsFocusedAsState()
        BasicTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.weight(1f).height(44.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, color = onSurfaceColor),
            cursorBrush = SolidColor(tealColor),
            interactionSource = searchInteractionSource,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(percent = 50))
                        .background(if (isSearchFocused) deepColor.copy(alpha = 0.65f) else deepColor.copy(alpha = 0.45f))
                        .border(
                            width = 1.dp,
                            color = if (isSearchFocused) tealColor else outlineColor.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(percent = 50)
                        )
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = tealColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search spots…",
                                color = mutedColor.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = mutedColor, modifier = Modifier.size(15.dp))
                        }
                    }
                    VoiceMicButton(
                        onResult = onSearchQueryChange,
                        prompt = "Speak to search…",
                        tint = tealColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        )

        Box {
            IconButton(
                onClick = { expandedSort = true },
                modifier = Modifier
                    .size(44.dp)
                    .background(deepColor, RoundedCornerShape(14.dp))
                    .border(1.dp, outlineColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = tealColor)
            }
            DropdownMenu(expanded = expandedSort, onDismissRequest = { expandedSort = false }) {
                VaultSortOption.all.forEach { sortOption ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                sortOption,
                                fontWeight = if (sortBy == sortOption) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSortByChange(sortOption)
                            expandedSort = false
                        }
                    )
                }
            }
        }

        // The single control that opens the full A-Z tag cloud — VaultTagFilterBar below only
        // ever holds quick-filter chips (Favorites, top tags), not a second entry point into the
        // same sheet.
        IconButton(
            onClick = onOpenTagSheet,
            modifier = Modifier
                .size(44.dp)
                .background(deepColor, RoundedCornerShape(14.dp))
                .border(1.dp, outlineColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
        ) {
            Icon(Icons.Default.FilterList, contentDescription = "All tags", tint = tealColor)
        }

        if (showViewModeToggle) {
            IconButton(
                onClick = {
                    val nextMode = if (vaultViewMode == VaultViewMode.LIST) VaultViewMode.GRID else VaultViewMode.LIST
                    onVaultViewModeChange(nextMode)
                    saveVaultViewMode(prefs, nextMode)
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(SpotVaultColors.Deep, RoundedCornerShape(14.dp))
                    .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            ) {
                Icon(
                    imageVector = if (vaultViewMode == VaultViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                    contentDescription = if (vaultViewMode == VaultViewMode.LIST) "Switch to grid view" else "Switch to list view",
                    tint = SpotVaultColors.Teal
                )
            }
        }
    }
}

@Composable
private fun VaultSelectionBar(
    selectedCount: Int,
    historyList: List<LocationSpot>,
    selectedItems: Set<Int>,
    // Selects every spot currently visible on screen — i.e. everything under an expanded
    // section, not every spot in the database — same logic each section header's own long-press
    // already used, just exposed here as a discoverable button instead of only a hidden gesture.
    onSelectAll: () -> Unit,
    onShowDeleteConfirm: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${selectedCount} Selected", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold)
        Row {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "Select all visible", tint = SpotVaultColors.Teal)
            }
            IconButton(
                onClick = {
                    val itemsToExport = historyList.filter { selectedItems.contains(it.id) }
                    val shareText = buildString {
                        for ((index, item) in itemsToExport.withIndex()) {
                            if (index > 0) append("\n\n---\n\n")
                            append(
                                buildShareText(
                                    lat = item.lat,
                                    lng = item.lng,
                                    address = item.address,
                                    title = vaultSpotDisplayTitle(item)
                                )
                            )
                            // Binder TransactionTooLarge risk on large multi-select shares.
                            if (length >= 16_384) {
                                append("\n\n…(+${itemsToExport.size - index - 1} more)")
                                break
                            }
                        }
                    }
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Export Spots"))
                }
            ) {
                Icon(Icons.Default.Share, contentDescription = "Export", tint = SpotVaultColors.Teal)
            }
            IconButton(onClick = onShowDeleteConfirm) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun VaultEmptyState(
    showFavoritesOnly: Boolean,
    selectedCategoryLabel: String?,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String = "Snap a photo or drop a pin to save a spot."
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = SpotVaultColors.Muted.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title ?: when {
                showFavoritesOnly -> "No Favorite Spots"
                selectedCategoryLabel != null -> "No spots in $selectedCategoryLabel"
                else -> "Your Vault is Empty"
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SpotVaultColors.OnSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = SpotVaultColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

private data class VaultCalendarDayCell(
    val dayOfMonth: Int?,
    val dayStartMillis: Long?,
    val isCurrentMonth: Boolean
)

private fun vaultSpotDayStarts(spots: List<LocationSpot>): Set<Long> =
    spots.filter { !it.isWishlist }.map { calendarDayStart(it.timestamp) }.toSet()

private fun buildVaultMonthGridCells(year: Int, month: Int): List<VaultCalendarDayCell> {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val firstDayOfWeek = cal.firstDayOfWeek
    val startOffset = (cal.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells = mutableListOf<VaultCalendarDayCell>()

    repeat(startOffset) {
        cells.add(VaultCalendarDayCell(dayOfMonth = null, dayStartMillis = null, isCurrentMonth = false))
    }
    for (day in 1..daysInMonth) {
        cal.set(Calendar.DAY_OF_MONTH, day)
        cells.add(
            VaultCalendarDayCell(
                dayOfMonth = day,
                dayStartMillis = calendarDayStart(cal.timeInMillis),
                isCurrentMonth = true
            )
        )
    }
    while (cells.size % 7 != 0) {
        cells.add(VaultCalendarDayCell(dayOfMonth = null, dayStartMillis = null, isCurrentMonth = false))
    }
    return cells
}

@Composable
private fun VaultOverlayDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BackHandler(onBack = onDismissRequest)
    val dialogShape = spotVaultDialogShape()
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        EnsureDialogEdgeToEdge()
        // Own Dialog window — WindowInsets.statusBars/navigationBars can't be trusted from in
        // here (same reason TimerSelectionDialog/NotepadEditorDialog read SystemBarInsets
        // instead), so this reads the Activity's measured insets rather than
        // .statusBarsPadding()/.navigationBarsPadding(), which could silently report 0 and let
        // this card get clipped by or drawn under the real system bars.
        val density = LocalDensity.current
        // Same floor + hedge as the Pin/Snap save screen's own fix (see its comment on
        // localNavBarPx): SystemBarInsets can be momentarily stale the instant this Dialog first
        // composes, and third-party nav bar customizers (Nav Star and similar) draw their own bar
        // as an accessibility overlay that never shows up in WindowInsets at all, on top of
        // whichever real measurement wins. The coerceAtLeast(24.dp) + 24.dp gives every vault
        // dialog built on this (Favorites Hub, Location Browser, Archived Spots, Tag Editor, etc.)
        // the same guaranteed-minimum clearance regardless of phone/tablet or orientation, instead
        // of trusting a single reading that can read 0.
        val localNavBarPx = WindowInsets.navigationBars.getBottom(density)
        val statusPad = with(density) { SystemBarInsets.statusBarPx.toDp() }.coerceAtLeast(24.dp)
        val navPad = with(density) {
            maxOf(SystemBarInsets.navigationBarPx, localNavBarPx).toDp()
        }.coerceAtLeast(24.dp) + 24.dp
        // Left/right — a camera cutout or a gesture-nav swipe-exclusion zone that sits on the top/
        // bottom edge in portrait moves to a *side* edge in landscape. Without this, the card below
        // was centered only within a box that had already silently lost real width to the cutout on
        // one side, which reads as "shoved toward the opposite corner" rather than simply centered
        // in a slightly narrower space.
        val leftPad = with(density) { SystemBarInsets.leftPx.toDp() }.coerceAtLeast(8.dp)
        val rightPad = with(density) { SystemBarInsets.rightPx.toDp() }.coerceAtLeast(8.dp)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                // decorFitsSystemWindows = false means this Dialog's own window never
                // auto-resizes/pans for the keyboard — without this, any text field inside
                // (e.g. VaultTagEditorDialog's new-tag field) could end up hidden behind the IME
                // with no way to see what's being typed.
                .imePadding()
                .padding(
                    top = statusPad + 24.dp,
                    // navPad already carries its own +24dp buffer above, baked in the same way
                    // the Pin/Snap screen bakes it into navPad directly rather than adding it twice.
                    bottom = navPad,
                    start = leftPad + 16.dp,
                    end = rightPad + 16.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth(0.92f)
                    .adaptiveMaxContentWidth()
                    .heightIn(max = maxHeight)
                    .clip(dialogShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(SpotVaultColors.Elevated, SpotVaultColors.Surface, SpotVaultColors.Deep)
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                SpotVaultColors.PrimaryBright.copy(alpha = 0.55f),
                                SpotVaultColors.Teal.copy(alpha = 0.35f),
                                SpotVaultColors.Outline.copy(alpha = 0.3f)
                            )
                        ),
                        shape = dialogShape
                    )
                    .padding(20.dp)
            ) {
                content()
            }
            // This Dialog renders in its own raised window, on top of the main Activity window
            // the app-root VaultUndoSnackbarHost lives in — without a host scoped to this window
            // too, an Undo action taken from inside here (delete/archive on Favorites Hub,
            // Location Browser, etc.) would show correctly but be completely hidden behind this
            // dialog's own opaque surface. See VaultUndoSnackbar's own doc comment.
            VaultUndoSnackbarHost()
        }
    }
}

@Composable
fun VaultCalendarDialog(
    spotDayStarts: Set<Long>,
    monthsWithSpots: Set<Int>,
    onDismiss: () -> Unit,
    onDaySelected: (Long) -> Unit
) {
    val spotDays = spotDayStarts
    val todayStart = remember { calendarDayStart(System.currentTimeMillis()) }
    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val weekDayLabels = remember {
        val cal = Calendar.getInstance()
        val firstDay = cal.firstDayOfWeek
        (0 until 7).map { offset ->
            cal.apply {
                set(Calendar.DAY_OF_WEEK, ((firstDay - 1 + offset) % 7) + 1)
            }
            SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
        }
    }

    var displayedMonth by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        )
    }
    var showMonthYearPicker by remember { mutableStateOf(false) }

    val year = displayedMonth.get(Calendar.YEAR)
    val month = displayedMonth.get(Calendar.MONTH)
    val monthCells = remember(year, month) { buildVaultMonthGridCells(year, month) }
    val gridRows = (monthCells.size + 6) / 7

    VaultOverlayDialog(onDismissRequest = onDismiss) {
        // Scrollable — VaultOverlayDialog itself caps this content's height at whatever's
        // actually available (heightIn(max = maxHeight)) but doesn't scroll it, so a short window
        // (landscape, or any compact-height case) used to just clip the bottom rows of the day
        // grid off with no way to reach them. Safe to nest the LazyVerticalGrid below inside this
        // scroll because that grid already has an explicit fixed height of its own
        // ((gridRows * 44).dp) rather than filling/weighting its parent — no infinite-height
        // measurement conflict.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spot Calendar",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = SpotVaultColors.Teal,
                    letterSpacing = 1.sp
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(SpotVaultColors.Surface.copy(alpha = 0.75f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SpotVaultColors.OnSurface)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Days with saved spots are marked. Tap a highlighted day to view its spots.",
                fontSize = 12.sp,
                color = SpotVaultColors.Muted,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        displayedMonth = (displayedMonth.clone() as Calendar).apply {
                            add(Calendar.MONTH, -1)
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(SpotVaultColors.Deep, CircleShape)
                        .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous month",
                        tint = SpotVaultColors.Teal
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showMonthYearPicker = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthFormatter.format(displayedMonth.time),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SpotVaultColors.OnSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Jump to a different month or year",
                        tint = SpotVaultColors.Teal,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = {
                        displayedMonth = (displayedMonth.clone() as Calendar).apply {
                            add(Calendar.MONTH, 1)
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(SpotVaultColors.Deep, CircleShape)
                        .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next month",
                        tint = SpotVaultColors.Teal
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Capped cell size instead of letting 7 columns stretch to fill the dialog's own
            // width — each day cell is a square (aspectRatio(1f) in VaultCalendarDayCellView), so
            // a wider dialog used to mean bigger AND taller cells, growing the grid's real
            // rendered height well past the (gridRows * 44).dp this used to assume it'd need.
            // Landscape's much wider dialog (nothing like phone-portrait's ~92% of a narrow
            // screen) is exactly when that mismatch showed up: cells "zoomed" in, and with a
            // fixed grid height still sized for ~44dp cells, only the first couple of rows fit
            // before the rest got clipped. Capping keeps the calendar the same comfortable size
            // on any wide screen — it just sits centered with more breathing room on either side
            // instead of stretching — while BoxWithConstraints still lets it shrink below that cap
            // on the narrowest phones instead of overflowing them.
            val calendarCellGap = 4.dp
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val calendarCellSize = ((maxWidth - calendarCellGap * 6) / 7).coerceAtMost(42.dp)
                val calendarGridWidth = calendarCellSize * 7 + calendarCellGap * 6
                val calendarGridHeight = calendarCellSize * gridRows + calendarCellGap * (gridRows - 1).coerceAtLeast(0)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.width(calendarGridWidth)) {
                        weekDayLabels.forEach { label ->
                            Text(
                                text = label.uppercase(Locale.getDefault()),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpotVaultColors.Muted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .width(calendarGridWidth)
                            .height(calendarGridHeight),
                        userScrollEnabled = false,
                        horizontalArrangement = Arrangement.spacedBy(calendarCellGap),
                        verticalArrangement = Arrangement.spacedBy(calendarCellGap)
                    ) {
                        items(monthCells) { cell ->
                            VaultCalendarDayCellView(
                                cell = cell,
                                hasSpots = cell.dayStartMillis?.let { it in spotDays } == true,
                                isToday = cell.dayStartMillis == todayStart,
                                onClick = {
                                    cell.dayStartMillis?.let(onDaySelected)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMonthYearPicker) {
        VaultMonthYearPickerDialog(
            initialYear = year,
            initialMonth = month,
            monthsWithSpots = monthsWithSpots,
            onDismiss = { showMonthYearPicker = false },
            onConfirm = { pickedYear, pickedMonth ->
                displayedMonth = (displayedMonth.clone() as Calendar).apply {
                    set(Calendar.YEAR, pickedYear)
                    set(Calendar.MONTH, pickedMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                showMonthYearPicker = false
            }
        )
    }
}

/** Quick jump to any month/year — the prev/next arrows on the calendar itself only step one
 * month at a time, which makes reaching something even a year or two back tedious. Year has its
 * own prev/next since there's no natural bound on how far back a jump might need to go; months
 * are a flat 3x4 grid since there's only ever 12 of them. */
@Composable
private fun VaultMonthYearPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    monthsWithSpots: Set<Int>,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit
) {
    var pickedYear by remember { mutableStateOf(initialYear) }
    var pickedMonth by remember { mutableStateOf(initialMonth) }
    val monthLabels = remember {
        val cal = Calendar.getInstance()
        (0 until 12).map { m ->
            cal.set(Calendar.MONTH, m)
            SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
        }
    }

    VaultOverlayDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Jump to Month",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = SpotVaultColors.Teal,
                letterSpacing = 0.5.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { pickedYear -= 1 },
                    modifier = Modifier
                        .size(36.dp)
                        .background(SpotVaultColors.Deep, CircleShape)
                        .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous year", tint = SpotVaultColors.Teal)
                }
                Text(
                    text = pickedYear.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SpotVaultColors.OnSurface,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                IconButton(
                    onClick = { pickedYear += 1 },
                    modifier = Modifier
                        .size(36.dp)
                        .background(SpotVaultColors.Deep, CircleShape)
                        .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next year", tint = SpotVaultColors.Teal)
                }
            }

            // A plain 3x4 grid of Rows, not LazyVerticalGrid — a fixed 12-item grid has no need
            // to be lazy, and giving LazyVerticalGrid a guessed fixed height was what clipped the
            // bottom row instead of just sizing to its own content.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                monthLabels.chunked(4).forEachIndexed { rowIndex, rowLabels ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowLabels.forEachIndexed { colIndex, label ->
                            val m = rowIndex * 4 + colIndex
                            val selected = m == pickedMonth
                            val hasSpots = (pickedYear * 100 + m) in monthsWithSpots
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.6f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) SpotVaultColors.Teal.copy(alpha = 0.18f) else SpotVaultColors.Deep)
                                    .border(
                                        1.dp,
                                        if (selected) SpotVaultColors.Teal.copy(alpha = 0.65f) else SpotVaultColors.Outline.copy(alpha = 0.4f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { pickedMonth = m },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) SpotVaultColors.Teal else SpotVaultColors.OnSurface
                                )
                                if (hasSpots) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 5.dp)
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (selected) SpotVaultColors.Teal else SpotVaultColors.PrimaryBright)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel", color = SpotVaultColors.Muted)
                }
                SpotVaultButton(
                    onClick = { onConfirm(pickedYear, pickedMonth) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = spotVaultButtonShape()
                ) {
                    Text("Go", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun VaultCalendarDayCellView(
    cell: VaultCalendarDayCell,
    hasSpots: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val day = cell.dayOfMonth
    if (day == null) {
        Spacer(modifier = Modifier.aspectRatio(1f))
        return
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isToday -> SpotVaultColors.PrimaryDeep.copy(alpha = 0.35f)
                    hasSpots -> SpotVaultColors.Teal.copy(alpha = 0.12f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (hasSpots) {
                    Modifier
                        .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                fontSize = 13.sp,
                fontWeight = if (hasSpots) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    !cell.isCurrentMonth -> SpotVaultColors.Muted.copy(alpha = 0.4f)
                    hasSpots -> SpotVaultColors.OnSurface
                    else -> SpotVaultColors.Muted
                }
            )
            if (hasSpots) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(SpotVaultColors.Teal, CircleShape)
                )
            }
        }
    }
}

/** Full Vault list experience — search, sort (including Closest, by distance from the user),
 * Favorites/tag/vehicle filtering, swipe-to-favorite/delete, the same overflow menu (Edit/Share/
 * Add Photo/Edit Tags/Favorite), and bulk select/export/delete — scoped to a caller-
 * supplied subset of spots instead of the whole Vault. Used by both the Calendar day-results
 * dialog and the Location Browser's per-city entries so that once someone's narrowed down to "a
 * day" or "a city," they get the exact same tools they'd reach for in the main Vault to narrow
 * further (by tag, by favorite, by distance) instead of a stripped-down read-only list. State
 * (search/sort/filters) is owned locally here rather than lifted, since these are transient
 * overlays, not a persistent tab. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VaultFilterableSpotList(
    baseSpots: List<LocationSpot>,
    prefs: SharedPreferences,
    dao: LocationDao,
    selectedItems: Set<Int>,
    onSelectedItemsChange: (Set<Int>) -> Unit,
    onShowDeleteConfirm: () -> Unit,
    onSwipeDeleteSpot: (LocationSpot) -> Unit,
    onShareRequest: (ShareSpotPayload) -> Unit,
    onViewSpot: (LocationSpot) -> Unit,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    emptyTitle: String = "No spots here yet",
    emptySubtitle: String = "Nothing saved matches yet.",
    // FavoritesHubDialog passes true — baseSpots there is already isFavorite-only, so the chip
    // would just be a dead toggle with nothing left to filter.
    hideFavoritesFilter: Boolean = false,
    // FavoritesHubDialog also floats its own "+ Add Spot" FAB in this list's bottom-right corner
    // (outside this composable, in the Box that hosts both) — with no reserved space for it, the
    // empty-state message ended up centered right underneath it, its last line covered outright.
    // Zero everywhere else, since nothing else stacks a FAB over this list.
    contentBottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val userLocation = rememberUserLocationForDistance()
    val distanceUnit = rememberDistanceUnit(prefs)
    val vehicleDao = remember { AppDatabase.getDatabase(context).vehicleDao() }
    val allVehicles by vehicleDao.observeAll().collectAsState(initial = emptyList())
    var showArchivedVehicles by rememberSaveable { mutableStateOf(false) }
    val vehicleById = remember(allVehicles) { allVehicles.associateBy { it.id } }
    var selectionModeActive by remember { mutableStateOf(false) }

    val tagDao = remember { AppDatabase.getDatabase(context).tagDao() }
    val topTags by tagDao.getTopTags().collectAsState(initial = emptyList())
    val allTags by tagDao.getAllTags().collectAsState(initial = emptyList())

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortBy by rememberSaveable { mutableStateOf(VaultSortOption.NEWEST) }
    var showFavoritesOnly by rememberSaveable { mutableStateOf(false) }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedVehicleId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showTagFilterSheet by remember { mutableStateOf(false) }
    var nearMeFilter by rememberSaveable(
        stateSaver = androidx.compose.runtime.saveable.Saver(
            save = { it.name },
            restore = { VaultNearMeFilter.valueOf(it) }
        )
    ) { mutableStateOf(VaultNearMeFilter.OFF) }
    var showPhotosOnly by rememberSaveable { mutableStateOf(false) }

    val spotIdToTags = rememberVaultSpotIdToTags(
        tagDao = tagDao,
        spotIds = baseSpots.map { it.id }
    )

    LaunchedEffect(selectedItems) {
        if (selectedItems.isEmpty()) selectionModeActive = false
    }
    LaunchedEffect(searchQuery, showFavoritesOnly, selectedVehicleId, sortBy, selectedTag, nearMeFilter, showPhotosOnly) {
        if (selectedItems.isNotEmpty()) onSelectedItemsChange(emptySet())
    }
    LaunchedEffect(allVehicles, selectedVehicleId) {
        val id = selectedVehicleId
        if (id != null && allVehicles.none { it.id == id && !it.isArchived }) {
            selectedVehicleId = null
        }
    }

    val filteredList = rememberFilteredVaultSpots(
        spots = baseSpots,
        searchQuery = searchQuery,
        showFavoritesOnly = showFavoritesOnly,
        vehicleId = selectedVehicleId,
        sortBy = sortBy,
        userLocation = userLocation,
        tagsBySpotId = spotIdToTags,
        vehicleNameById = vehicleById.mapValues { it.value.name },
        nearMeFilter = nearMeFilter,
        showPhotosOnly = showPhotosOnly,
        selectedTag = selectedTag
    )

    // Same "flat list for CLOSEST/ALPHABETICAL, date-bucketed otherwise" rule as the main Vault
    // — a spot 50 meters away shouldn't lose to one 40 miles away just because "Today" always
    // sorts first regardless of which sort is actually picked.
    val sectionedSpots = remember(filteredList, sortBy) {
        if (sortBy == VaultSortOption.CLOSEST || sortBy == VaultSortOption.ALPHABETICAL) {
            if (filteredList.isEmpty()) emptyList() else listOf("Results" to filteredList)
        } else {
            groupSpotsByDateSection(filteredList)
        }
    }
    // Only the newest section starts expanded — see the matching comment in the main Vault's
    // sectioning above for why this is dynamic rather than hardcoded to "Today".
    val seenSections = remember { mutableSetOf<String>() }
    var collapsedSections by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(sectionedSpots.map { it.first }) {
        val newestSectionTitle = sectionedSpots.firstOrNull()?.first
        sectionedSpots.map { it.first }.forEach { title ->
            if (seenSections.add(title) && title != newestSectionTitle) {
                collapsedSections = collapsedSections + title
            }
        }
    }

    val onSwipeArchive: (LocationSpot) -> Unit = { spot ->
        val spotId = spot.id
        coroutineScope.launch(Dispatchers.IO) { dao.archiveSpot(spotId) }
        VaultUndoSnackbar.show("Spot archived") {
            withContext(Dispatchers.IO) { dao.unarchiveSpotIfArchived(spotId) }
        }
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    val headerContent: @Composable () -> Unit = {
        VaultSearchSortRow(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = prefsSafeSearchQuery(it) },
            sortBy = sortBy,
            onSortByChange = { sortBy = it },
            vaultViewMode = VaultViewMode.LIST,
            onVaultViewModeChange = {},
            prefs = prefs,
            onOpenTagSheet = { showTagFilterSheet = true },
            showViewModeToggle = false
        )
        Spacer(modifier = Modifier.height(6.dp))
        VaultTagFilterBar(
            topTags = topTags,
            showFavoritesOnly = showFavoritesOnly,
            onShowFavoritesOnlyChange = { showFavoritesOnly = it },
            nearMeFilter = nearMeFilter,
            onNearMeFilterChange = { nearMeFilter = it },
            showPhotosOnly = showPhotosOnly,
            onShowPhotosOnlyChange = { showPhotosOnly = it },
            selectedTag = selectedTag,
            onSelectedTagChange = { selectedTag = it },
            activeVehicleLabel = allVehicles.firstOrNull { it.id == selectedVehicleId }
                ?.let { "${vehicleIconEmoji(it.iconKey)} ${it.name}" },
            onClearVehicle = { selectedVehicleId = null },
            modifier = Modifier.fillMaxWidth(),
            showFavoritesChip = !hideFavoritesFilter
        )
        if (selectedItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            VaultSelectionBar(
                selectedCount = selectedItems.size,
                historyList = baseSpots,
                selectedItems = selectedItems,
                onSelectAll = {
                    onSelectedItemsChange(
                        sectionedSpots
                            .filterNot { (title, _) -> title in collapsedSections }
                            .flatMap { (_, sectionSpots) -> sectionSpots.map { it.id } }
                            .toSet()
                    )
                },
                onShowDeleteConfirm = onShowDeleteConfirm
            )
        }
    }

    val feedContent: @Composable ColumnScope.() -> Unit = {
        if (filteredList.isEmpty()) {
            VaultEmptyState(
                showFavoritesOnly = showFavoritesOnly,
                selectedCategoryLabel = null,
                title = if (baseSpots.isEmpty()) emptyTitle else "No matches",
                subtitle = if (baseSpots.isEmpty()) emptySubtitle else "Try a different search, sort, or filter.",
                modifier = Modifier.weight(1f, fill = false).padding(bottom = contentBottomPadding)
            )
        } else {
            Box(modifier = Modifier.weight(1f, fill = false).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp + contentBottomPadding)
                ) {
                    sectionedSpots.forEach { (sectionTitle, spots) ->
                        val isCollapsed = sectionTitle in collapsedSections
                        stickyHeader(key = "header_$sectionTitle") {
                            VaultSectionStickyHeader(
                                sectionTitle = sectionTitle,
                                count = spots.size,
                                collapsed = isCollapsed,
                                onToggle = {
                                    collapsedSections = if (isCollapsed) {
                                        collapsedSections - sectionTitle
                                    } else {
                                        collapsedSections + sectionTitle
                                    }
                                },
                                onLongPress = {
                                    val expandedIds = sectionedSpots
                                        .filterNot { (title, _) -> title in collapsedSections }
                                        .flatMap { (_, sectionSpots) -> sectionSpots.map { it.id } }
                                        .toSet()
                                    onSelectedItemsChange(expandedIds)
                                    selectionModeActive = true
                                }
                            )
                        }
                        if (!isCollapsed) {
                            items(spots, key = { it.id }) { item ->
                                VaultSpotEntry(
                                    item = item,
                                    vehicle = item.vehicleId?.let { vehicleById[it] },
                                    tags = spotIdToTags[item.id].orEmpty(),
                                    allTags = allTags,
                                    tagDao = tagDao,
                                    prefs = prefs,
                                    selectedItems = selectedItems,
                                    selectionModeActive = selectionModeActive,
                                    onSelectionModeActiveChange = { selectionModeActive = it },
                                    vaultViewMode = VaultViewMode.LIST,
                                    onViewSpot = onViewSpot,
                                    onShareRequest = onShareRequest,
                                    onSelectionChange = onSelectedItemsChange,
                                    onSwipeArchive = onSwipeArchive,
                                    onSwipeDelete = onSwipeDeleteSpot,
                                    coroutineScope = coroutineScope,
                                    dao = dao,
                                    // Same reasoning as the main Vault tab's identical fix — see
                                    // its comment.
                                    swipeGestureEnabled = !listState.isScrollInProgress && !selectionModeActive,
                                    userLocation = userLocation,
                                    distanceUnit = distanceUnit
                                )
                            }
                        }
                    }
                }
                val canScrollList = listState.canScrollForward || listState.canScrollBackward
                if (canScrollList) {
                    PremiumVerticalScrollbar(
                        listState = listState,
                        emphasizeHint = true,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp)
                            .width(6.dp)
                    )
                }
                if (listState.canScrollForward) {
                    PremiumScrollMoreHint(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp),
                        includeNavBarInset = false
                    )
                }
            }
        }
    }

    if (needsCompactHeightLayout()) {
        // Side by side instead of stacked — same reasoning as the main Vault tab: the search/
        // sort row and filter chips above the list used to eat most of a short window's scarce
        // height before the list even started, leaving only a sliver (sometimes a single row)
        // for the actual spots. The header gets its own scrollable column; the list gets a full-
        // height column next to it instead of whatever was left over underneath the header.
        Row(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                headerContent()
            }
            Column(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
                    .padding(start = 12.dp)
            ) {
                feedContent()
            }
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            headerContent()
            Spacer(modifier = Modifier.height(8.dp))
            feedContent()
        }
    }

    if (showTagFilterSheet) {
        VaultTagFilterSheet(
            allTags = allTags,
            selectedTag = selectedTag,
            onSelectedTagChange = { selectedTag = it },
            vehicles = allVehicles,
            selectedVehicleId = selectedVehicleId,
            onSelectedVehicleIdChange = { selectedVehicleId = it },
            showArchivedVehicles = showArchivedVehicles,
            onShowArchivedVehiclesChange = { showArchivedVehicles = it },
            tagDao = tagDao,
            vehicleDao = vehicleDao,
            locationDao = dao,
            coroutineScope = coroutineScope,
            onDismiss = { showTagFilterSheet = false }
        )
    }
}

@Composable
fun VaultCalendarDayResultsDialog(
    dayStartMillis: Long,
    daySpots: List<LocationSpot>,
    prefs: SharedPreferences,
    dao: LocationDao,
    onDismiss: () -> Unit,
    onViewSpot: (LocationSpot) -> Unit,
    onShareRequest: (ShareSpotPayload) -> Unit,
    onSwipeDelete: (LocationSpot) -> Unit,
    selectedItems: Set<Int>,
    onSelectionChange: (Set<Int>) -> Unit,
    onShowDeleteConfirm: () -> Unit,
    coroutineScope: CoroutineScope
) {
    val dayLabel = remember(dayStartMillis) {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(dayStartMillis))
    }

    // Height comes from VaultOverlayDialog itself (heightIn(max = maxHeight), already measured
    // from the real available space inside the dialog's own status/nav-bar-aware insets) rather
    // than a second, independent screenHeightDp-based cap here — two competing height formulas
    // meant this dialog's actual max height depended on which one happened to be tighter for a
    // given device/orientation instead of always matching the same budget every other
    // VaultOverlayDialog-based sheet already gets.
    VaultOverlayDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Saved Spots",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = SpotVaultColors.Teal,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = dayLabel,
                        fontSize = 12.sp,
                        color = SpotVaultColors.Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(SpotVaultColors.Surface.copy(alpha = 0.75f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SpotVaultColors.OnSurface)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            VaultFilterableSpotList(
                baseSpots = daySpots,
                prefs = prefs,
                dao = dao,
                selectedItems = selectedItems,
                onSelectedItemsChange = onSelectionChange,
                onShowDeleteConfirm = onShowDeleteConfirm,
                onSwipeDeleteSpot = onSwipeDelete,
                onShareRequest = onShareRequest,
                onViewSpot = onViewSpot,
                coroutineScope = coroutineScope,
                modifier = Modifier.weight(1f, fill = false),
                emptyTitle = "No spots saved on this day",
                emptySubtitle = "Snap a photo or drop a pin to save one."
            )
        }
    }
}

/** Permanently deletes [spot] and its on-disk photos — both the cover [LocationSpot.imagePath]
 * and every extra photo attached via [SpotPhotoDao]. [dao.deleteSpot] cascades the spot_photos
 * *rows* away via the FK on its own, but never touches the actual JPEG files those rows pointed
 * at — those have to be deleted here first, while the rows (and thus the paths) still exist to
 * read. Matches the same cleanup the 30-day auto-purge in MainActivity.onCreate already does;
 * this is the manual "Delete Forever" equivalent of that automatic purge and needs to leave the
 * same result on disk, not just in the database. */
private suspend fun permanentlyDeleteSpotAndPhotos(dao: LocationDao, spotPhotoDao: SpotPhotoDao, spot: LocationSpot) {
    permanentlyDeleteSpotsAndPhotos(dao, spotPhotoDao, listOf(spot))
}

/** Path-first bulk wipe — same disk cleanup as one-by-one, without N photo-table round trips. */
private suspend fun permanentlyDeleteSpotsAndPhotos(
    dao: LocationDao,
    spotPhotoDao: SpotPhotoDao,
    spots: List<LocationSpot>
) {
    if (spots.isEmpty()) return
    spots.forEach { spot ->
        if (spot.imagePath.isNotEmpty()) {
            runCatching { File(spot.imagePath).delete() }
        }
    }
    spots.map { it.id }.chunked(500).forEach { chunk ->
        spotPhotoDao.getPathsForSpots(chunk).forEach { path ->
            runCatching { File(path).delete() }
        }
    }
    dao.deleteSpots(spots)
}

private suspend fun loadTagsBySpotIds(tagDao: TagDao, spotIds: List<Int>): Map<Int, List<TagEntity>> {
    if (spotIds.isEmpty()) return emptyMap()
    return spotIds.chunked(500)
        .flatMap { chunk -> tagDao.getTagAssignmentsForSpots(chunk) }
        .toTagsBySpotId()
}

@Composable
fun RecentlyDeletedDialog(
    dao: LocationDao,
    spotPhotoDao: SpotPhotoDao,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val tagDao = remember { AppDatabase.getDatabase(context).tagDao() }
    val coroutineScope = rememberCoroutineScope()
    var deletedSpots by remember { mutableStateOf<List<LocationSpot>>(emptyList()) }
    var deletedTotalCount by remember { mutableIntStateOf(0) }
    var hasMoreDeleted by remember { mutableStateOf(true) }
    var loadingMoreDeleted by remember { mutableStateOf(false) }
    var tagsBySpotId by remember { mutableStateOf<Map<Int, List<TagEntity>>>(emptyMap()) }
    var spotPendingPermanentDelete by remember { mutableStateOf<LocationSpot?>(null) }
    var showRestoreAllConfirm by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    suspend fun refreshDeleted() {
        val (spots, total) = withContext(Dispatchers.IO) {
            dao.getRecentlyDeletedPage(beforeDeletedAt = -1L, beforeId = 0, limit = SECONDARY_BROWSE_PAGE_SIZE) to
                dao.countRecentlyDeleted()
        }
        val tags = withContext(Dispatchers.IO) {
            loadTagsBySpotIds(tagDao, spots.map { it.id })
        }
        deletedSpots = spots
        deletedTotalCount = total
        hasMoreDeleted = spots.size >= SECONDARY_BROWSE_PAGE_SIZE && spots.size < total
        tagsBySpotId = tags
    }

    suspend fun loadMoreDeleted() {
        if (loadingMoreDeleted || !hasMoreDeleted) return
        if (deletedSpots.size >= SECONDARY_BROWSE_FULL_CAP) {
            hasMoreDeleted = false
            return
        }
        val anchor = deletedSpots.lastOrNull() ?: return
        val beforeDeletedAt = anchor.deletedAt ?: return
        loadingMoreDeleted = true
        try {
            val page = withContext(Dispatchers.IO) {
                dao.getRecentlyDeletedPage(
                    beforeDeletedAt = beforeDeletedAt,
                    beforeId = anchor.id,
                    limit = SECONDARY_BROWSE_PAGE_SIZE
                )
            }
            if (page.isEmpty()) {
                hasMoreDeleted = false
            } else {
                val existing = deletedSpots.map { it.id }.toHashSet()
                val newRows = page.filter { it.id !in existing }
                deletedSpots = (deletedSpots + newRows).take(SECONDARY_BROWSE_FULL_CAP)
                val tags = withContext(Dispatchers.IO) {
                    loadTagsBySpotIds(tagDao, newRows.map { it.id })
                }
                tagsBySpotId = tagsBySpotId + tags
                hasMoreDeleted = page.size >= SECONDARY_BROWSE_PAGE_SIZE &&
                    deletedSpots.size < deletedTotalCount &&
                    deletedSpots.size < SECONDARY_BROWSE_FULL_CAP
            }
        } finally {
            loadingMoreDeleted = false
        }
    }

    LaunchedEffect(Unit) {
        refreshDeleted()
    }

    LaunchedEffect(listState, hasMoreDeleted, deletedSpots.size) {
        if (!hasMoreDeleted) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last to info.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, total) ->
                if (total > 0 && lastVisible >= total - 8) {
                    loadMoreDeleted()
                }
            }
    }

    if (showRestoreAllConfirm) {
        PremiumDialog(
            onDismissRequest = { showRestoreAllConfirm = false },
            title = {
                Text("Restore All?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.OnSurface)
            },
            content = {
                Text(
                    "All $deletedTotalCount spot${if (deletedTotalCount == 1) "" else "s"} in Recently Deleted will be restored to the Vault.",
                    color = SpotVaultColors.Muted
                )
            },
            confirmButton = {
                SpotVaultButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            dao.restoreAllDeleted()
                            refreshDeleted()
                        }
                        showRestoreAllConfirm = false
                    }
                ) { Text("Restore All") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreAllConfirm = false }) {
                    Text("Cancel", color = SpotVaultColors.Teal)
                }
            }
        )
    }

    if (showDeleteAllConfirm) {
        PremiumDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = {
                Text("Delete All Forever?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.OnSurface)
            },
            content = {
                Text(
                    "All $deletedTotalCount spot${if (deletedTotalCount == 1) "" else "s"} will be permanently deleted and cannot be recovered.",
                    color = SpotVaultColors.Muted
                )
            },
            confirmButton = {
                SpotVaultButton(
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            // Delete All Forever pages until empty so rows past the browse window
                            // (and their JPEGs) are never left behind.
                            while (true) {
                                val page = dao.getRecentlyDeleted()
                                if (page.isEmpty()) break
                                permanentlyDeleteSpotsAndPhotos(dao, spotPhotoDao, page)
                            }
                            tagDao.recomputeAllUsageCounts()
                            refreshDeleted()
                        }
                        showDeleteAllConfirm = false
                    }
                ) { Text("Delete All Forever") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirm = false }) {
                    Text("Cancel", color = SpotVaultColors.Teal)
                }
            }
        )
    }

    spotPendingPermanentDelete?.let { spot ->
        PremiumDialog(
            onDismissRequest = { spotPendingPermanentDelete = null },
            title = {
                Text(
                    "Delete Forever?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SpotVaultColors.OnSurface
                )
            },
            content = {
                Text(
                    "This spot will be permanently deleted and cannot be recovered.",
                    color = SpotVaultColors.Muted
                )
            },
            confirmButton = {
                SpotVaultButton(
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            permanentlyDeleteSpotAndPhotos(dao, spotPhotoDao, spot)
                            tagDao.recomputeAllUsageCounts()
                            refreshDeleted()
                        }
                        spotPendingPermanentDelete = null
                    }
                ) { Text("Delete Forever") }
            },
            dismissButton = {
                TextButton(onClick = { spotPendingPermanentDelete = null }) {
                    Text("Cancel", color = SpotVaultColors.Teal)
                }
            }
        )
    }

    // Height comes from VaultOverlayDialog itself — see the matching comment on
    // VaultCalendarDayResultsDialog for why a second, independent screenHeightDp-based cap here
    // was redundant and could disagree with it.
    VaultOverlayDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Recently Deleted",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = SpotVaultColors.Teal,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Spots are kept for 30 days before being permanently removed.",
                        fontSize = 12.sp,
                        color = SpotVaultColors.Muted,
                        lineHeight = 16.sp
                    )
                    if (deletedTotalCount > deletedSpots.size) {
                        Text(
                            text = "Showing ${deletedSpots.size} of $deletedTotalCount",
                            fontSize = 12.sp,
                            color = SpotVaultColors.Muted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(SpotVaultColors.Surface.copy(alpha = 0.75f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SpotVaultColors.OnSurface)
                }
            }

            if (deletedSpots.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showRestoreAllConfirm = true }) {
                        Text("Restore All", color = SpotVaultColors.Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showDeleteAllConfirm = true }) {
                        Text("Delete All Forever", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (deletedSpots.isEmpty()) {
                Text(
                    text = "No recently deleted spots.",
                    color = SpotVaultColors.Muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(deletedSpots, key = { it.id }) { spot ->
                        val displayTitle = vaultSpotDisplayTitle(spot).ifBlank {
                            spot.address.ifBlank { "Untitled spot" }
                        }
                        val deletedLabel = spot.deletedAt?.let { dateFormatter.format(Date(it)) } ?: ""
                        val spotTags = tagsBySpotId[spot.id].orEmpty()
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = SpotVaultColors.Elevated)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    VaultSpotThumbnail(
                                        item = spot,
                                        onClick = {},
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = displayTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = SpotVaultColors.OnSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (spot.locationDetails.isNotBlank()) {
                                            Text(
                                                text = spot.locationDetails,
                                                fontSize = 12.sp,
                                                color = SpotVaultColors.Muted,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                        if (spotTags.isNotEmpty()) {
                                            VaultTagChipsRow(tags = spotTags, modifier = Modifier.padding(top = 4.dp))
                                        }
                                        if (deletedLabel.isNotEmpty()) {
                                            Text(
                                                text = "Deleted $deletedLabel",
                                                fontSize = 11.sp,
                                                color = SpotVaultColors.Muted,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                                // Full card width, not squeezed beside the 64dp thumbnail — on a
                                // narrow phone that used to leave "Delete Forever" little enough
                                // room to wrap to two lines (TextButton's label has no maxLines),
                                // growing card height unevenly between cards.
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                dao.restoreSpotIfSoftDeleted(spot.id)
                                                refreshDeleted()
                                            }
                                        }
                                    ) {
                                        Text("Restore", color = SpotVaultColors.Teal, fontSize = 13.sp)
                                    }
                                    TextButton(onClick = { spotPendingPermanentDelete = spot }) {
                                        Text(
                                            "Delete Forever",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Dedicated home for every starred spot. The "⭐ Favorites" chip on the main Vault screen used to
 * just filter the existing list in place — favorites were only ever "a filter," with no way to
 * add somewhere you want to go without first physically standing there and saving it live. This
 * gives favorites their own screen (built on the exact same [VaultFilterableSpotList] every other
 * narrowed-down Vault view already uses — same search/sort/swipe-to-favorite/overflow menu, so
 * nothing about editing a favorite is different from editing any other spot) plus a "+ Add Spot"
 * FAB that only exists here — the one place in the app a spot can be created from a typed address
 * instead of the device's current GPS fix. */
@Composable
fun FavoritesHubDialog(
    favoriteSpots: List<LocationSpot>,
    favoriteTotalCount: Int = favoriteSpots.size,
    dao: LocationDao,
    prefs: SharedPreferences,
    onDismiss: () -> Unit,
    onViewSpot: (LocationSpot) -> Unit,
    onShareRequest: (ShareSpotPayload) -> Unit,
    coroutineScope: CoroutineScope,
    // Non-null exactly once, right after a share from Google Maps (or another app) resolves —
    // see SharedMapsLink.kt. Auto-opens the Add Spot form pre-filled instead of making the user
    // notice and tap the FAB themselves.
    pendingSharedSpot: SharedSpotPayload? = null,
    onPendingSharedSpotConsumed: () -> Unit = {}
) {
    var selectedItems by remember { mutableStateOf(setOf<Int>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddFavorite by remember { mutableStateOf(false) }

    // Shared by the confirm dialog's own Delete button and the confirm_delete=false instant path
    // below — soft-deletes then offers Undo, same as the main Vault's own delete handling.
    fun deleteSpots(ids: Set<Int>) {
        selectedItems = emptySet()
        coroutineScope.launch(Dispatchers.IO) {
            ids.forEach { dao.softDeleteSpot(it) }
        }
        VaultUndoSnackbar.show(if (ids.size == 1) "Spot deleted" else "${ids.size} spots deleted") {
            withContext(Dispatchers.IO) { ids.forEach { dao.restoreSpotIfSoftDeleted(it) } }
        }
    }

    LaunchedEffect(pendingSharedSpot) {
        if (pendingSharedSpot != null) showAddFavorite = true
    }

    VaultOverlayDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "⭐ Favorites",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = SpotVaultColors.Teal,
                        letterSpacing = 0.5.sp
                    )
                    if (favoriteTotalCount > favoriteSpots.size) {
                        Text(
                            text = "Showing ${favoriteSpots.size} of $favoriteTotalCount",
                            fontSize = 12.sp,
                            color = SpotVaultColors.Muted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SpotVaultColors.Muted)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.weight(1f, fill = false)) {
                VaultFilterableSpotList(
                    baseSpots = favoriteSpots,
                    prefs = prefs,
                    dao = dao,
                    selectedItems = selectedItems,
                    onSelectedItemsChange = { selectedItems = it },
                    // Bulk delete from the top action bar always skips the confirmation modal —
                    // same reasoning as the main Vault: soft-delete is already reversible via
                    // Recently Deleted, and now Undo, so a blocking dialog for it is redundant.
                    onShowDeleteConfirm = { deleteSpots(selectedItems) },
                    onSwipeDeleteSpot = { spot ->
                        // Single-spot swipe delete still respects the user's own "Confirm before
                        // deleting" setting — only the bulk action above skips it unconditionally.
                        if (prefs.getBoolean("confirm_delete", true)) {
                            selectedItems = setOf(spot.id)
                            showDeleteConfirm = true
                        } else {
                            deleteSpots(setOf(spot.id))
                        }
                    },
                    onShareRequest = onShareRequest,
                    onViewSpot = onViewSpot,
                    coroutineScope = coroutineScope,
                    modifier = Modifier.fillMaxWidth(),
                    emptyTitle = "No favorites yet",
                    emptySubtitle = "Tap + below to save a place you want to visit, or star any saved spot from its own menu.",
                    hideFavoritesFilter = true,
                    // Clears the FAB below (56dp button + 16dp margin) — without this the empty
                    // state's own centered text ended up right underneath it, its last line
                    // covered outright.
                    contentBottomPadding = 72.dp
                )
                FloatingActionButton(
                    onClick = { showAddFavorite = true },
                    containerColor = SpotVaultColors.Teal,
                    contentColor = SpotVaultColors.OnTeal,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Spot")
                }
            }
        }
    }

    if (showAddFavorite) {
        AddFavoriteSpotDialog(
            dao = dao,
            initialData = pendingSharedSpot,
            onDismiss = {
                showAddFavorite = false
                if (pendingSharedSpot != null) onPendingSharedSpotConsumed()
            },
            // No post-save photo prompt — the form itself already has Camera/Gallery buttons, so
            // there's nothing left for a follow-up VaultAddPhotoDialog to offer here.
            onSaved = {}
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = SpotVaultColors.Surface,
            titleContentColor = SpotVaultColors.OnSurface,
            textContentColor = SpotVaultColors.Muted,
            title = { Text("Delete ${selectedItems.size} spot${if (selectedItems.size == 1) "" else "s"}?") },
            text = { Text("These move to Recently Deleted and can be restored for a while before they're gone for good.") },
            confirmButton = {
                TextButton(onClick = {
                    val toDelete = selectedItems
                    showDeleteConfirm = false
                    deleteSpots(toDelete)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = SpotVaultColors.Teal) }
            }
        )
    }
}

/** "+ Add Spot" form inside [FavoritesHubDialog] — the only place in the app a spot is created
 * from a typed address instead of the device's current GPS fix. Tags reuse the exact same
 * [SaveScreenTagField]/[SaveScreenTagPickerSheet] picker the normal Snap/Pin save screen uses for
 * a not-yet-created spot, for the same reason: this spot has no id to attach tags to until after
 * the insert, so both just collect plain tag names and the caller assigns them via TagDao once a
 * real id exists. */
/** Opens the user's own maps app on a plain-text place search — used by [AddFavoriteSpotDialog]'s
 * "Search in Maps" fallback for queries the Android Geocoder can't resolve (named businesses
 * especially). No specific app targeted (no setPackage()) — same reasoning as every other outbound
 * maps intent in this app: locking it to Google Maps only narrows which devices this can succeed
 * on, with no upside, and this app already supports sharing a result back in from any maps app,
 * not just Google's. */
private fun launchMapsSearch(context: Context, query: String): Boolean {
    val uri = android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(query)}")
    return try {
        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
        true
    } catch (e: android.content.ActivityNotFoundException) {
        false
    }
}

/** Whichever of the two ways [AddFavoriteSpotDialog] can end up with a location — resolved via
 * [geocodeAddress] from typed text, or already known exactly from a Google Maps share — normalized
 * to one shape so the save logic below only has to handle it once. */
private data class ResolvedFavoriteLocation(
    val lat: Double,
    val lng: Double,
    val address: String,
    val city: String,
    val state: String
)

@Composable
private fun AddFavoriteSpotDialog(
    dao: LocationDao,
    onDismiss: () -> Unit,
    onSaved: (LocationSpot) -> Unit,
    // Prefill from a Google Maps share (see SharedMapsLink.kt) — null for the normal "+ Add Spot"
    // FAB flow, where every field starts blank.
    initialData: SharedSpotPayload? = null
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val tagDao = remember { db.tagDao() }
    val allTags by tagDao.getAllTags().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf(initialData?.title.orEmpty()) }
    var addressText by remember { mutableStateOf(initialData?.addressHint.orEmpty()) }
    var notes by remember { mutableStateOf(initialData?.notesText.orEmpty()) }
    var selectedTags by remember { mutableStateOf(listOf<String>()) }
    var isSaving by remember { mutableStateOf(false) }
    var isResolving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // The one thing Save actually needs — set either straight from a shared Maps link's exact
    // coordinates, or by explicitly tapping "Find Address" below (never automatically, and never
    // as a side effect of Save itself: the Geocoder's best-guess match for an ambiguous query is
    // wrong often enough that silently saving whatever it returned, with no chance to notice, was
    // the actual problem here — Save now only ever commits a location the user has already seen
    // confirmed on screen). Cleared the moment the address field is edited, since at that point
    // the user is asking to resolve a different place, not confirm the one already found.
    var resolvedCoordinates by remember {
        mutableStateOf(initialData?.lat?.let { lat -> initialData.lng?.let { lng -> lat to lng } })
    }
    var resolvedAddressLabel by remember { mutableStateOf(initialData?.addressHint.orEmpty()) }
    var resolvedCity by remember { mutableStateOf(initialData?.city.orEmpty()) }
    var resolvedState by remember { mutableStateOf(initialData?.state.orEmpty()) }

    // Carries over a photo attached before tapping "Search in Maps" — see PendingFavoritePhoto
    // and dismissForMapsSearch below for how it survives that round trip through another app.
    var photoPath by remember { mutableStateOf(initialData?.photoPath.orEmpty()) }
    var isCapturingPhoto by remember { mutableStateOf(false) }
    var showPhotoChooser by remember { mutableStateOf(false) }
    val formLocked = isSaving || isResolving || isCapturingPhoto
    val prefs = remember { context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE) }

    // Camera capture, mirroring MainActivity's own pendingPhotoPath/AppLockGate pattern for
    // pre-save photo capture (this spot doesn't have an id yet either) — the pending path is
    // rememberSaveable so process death mid-camera still recovers the file into photoPath.
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        AppLockGate.end()
        val path = pendingPhotoPath
        pendingPhotoPath = null
        isCapturingPhoto = false
        val file = path?.let { File(it) }
        if (success && file != null && file.exists()) {
            compressCapturedPhoto(file.absolutePath)
            photoPath = file.absolutePath
        } else {
            runCatching { file?.delete() }
            android.widget.Toast.makeText(context, "Photo canceled.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun startCamera() {
        val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
        val newFile = File(imagesDir, "favorite_${System.currentTimeMillis()}.jpg")
        pendingPhotoPath = newFile.absolutePath
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", newFile)
        try {
            takePictureLauncher.launch(uri)
        } catch (e: android.content.ActivityNotFoundException) {
            // Mirrors takePictureLauncher's own cleanup — an immediate launch failure never
            // reaches that callback, so this has to release AppLockGate itself.
            pendingPhotoPath = null
            isCapturingPhoto = false
            AppLockGate.end()
            android.widget.Toast.makeText(context, "No camera app found on this device.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startCamera()
        } else {
            AppLockGate.end()
            isCapturingPhoto = false
            val activity = context as? Activity
            if (activity != null && isPermissionPermanentlyDenied(activity, prefs, Manifest.permission.CAMERA)) {
                showPermissionSettingsDialog(activity, "Camera access is needed to attach a photo. Enable it for DropPin Vault in Settings.")
            } else {
                android.widget.Toast.makeText(context, "Camera permission is required to attach a photo.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun requestCameraCapture() {
        isCapturingPhoto = true
        // Covers both the permission dialog and the camera capture that can follow it — either
        // one taking the foreground while App Lock is on must not let AppLockScreen swap in and
        // tear down this composable's pendingPhotoPath state mid-flight.
        AppLockGate.begin()
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (cameraGranted) {
            startCamera()
        } else {
            markPermissionRequested(prefs, Manifest.permission.CAMERA)
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Gallery pick — no runtime permission needed (system Photo Picker), but still wrapped in
    // AppLockGate since it's another external-app round trip holding onto this dialog's
    // Compose-scoped state, same reasoning as the camera capture above.
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        AppLockGate.end()
        isCapturingPhoto = false
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
                val destFile = File(imagesDir, "favorite_gallery_${System.currentTimeMillis()}.jpg")
                val copied = try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyToLimited(output, MAX_GALLERY_IMPORT_BYTES)
                        }
                    }
                    destFile.exists() && destFile.length() > 0
                } catch (e: Exception) {
                    false
                }
                if (copied) {
                    if (compressCapturedPhoto(destFile.absolutePath)) {
                        withContext(Dispatchers.Main) { photoPath = destFile.absolutePath }
                    } else {
                        runCatching { destFile.delete() }
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Couldn't add that photo. Try again.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runCatching { destFile.delete() }
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Couldn't add that photo. Try again.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun launchGallery() {
        isCapturingPhoto = true
        AppLockGate.begin()
        galleryLauncher.launch(
            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // A photo captured/picked here but never saved (Cancel, back gesture, tapping outside) would
    // otherwise sit on disk forever, referenced by nothing — Save is the only path that actually
    // attaches it to a spot.
    fun dismissAndCleanUp() {
        if (photoPath.isNotBlank()) {
            runCatching { File(photoPath).delete() }
        }
        onDismiss()
    }

    // "Search in Maps" closes this form too (see its own onClick below for why), but unlike a
    // real Cancel the user hasn't abandoned anything — they're mid-task, just stepping out to
    // find the address. Stashing rather than deleting means the photo comes back attached to the
    // fresh dialog instance that reopens once they share the result back in, instead of getting
    // wiped out by the cleanup above along with everything else that flow closes.
    fun dismissForMapsSearch() {
        if (photoPath.isNotBlank()) {
            PendingFavoritePhoto.stash(photoPath)
        }
        onDismiss()
    }

    VaultOverlayDialog(onDismissRequest = { if (!formLocked) dismissAndCleanUp() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Add Favorite Spot",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = SpotVaultColors.Teal,
                letterSpacing = 0.5.sp
            )
            Column {
                SpotEditSectionLabel("Title")
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    placeholder = { Text("Mom's House, Cabin…", color = SpotVaultColors.Muted.copy(alpha = 0.7f)) },
                    singleLine = true,
                    enabled = !formLocked,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = spotEditFieldColors(),
                    trailingIcon = {
                        VoiceMicButton(onResult = { spoken -> title = spoken.take(120) }, prompt = "Dictate spot title…")
                    }
                )
            }
            Column {
                SpotEditSectionLabel("Address to Search")
                OutlinedTextField(
                    value = addressText,
                    onValueChange = {
                        if (it.length <= 400) {
                            addressText = it; errorMessage = null; resolvedCoordinates = null; resolvedCity = ""; resolvedState = ""
                        }
                    },
                    placeholder = { Text("123 Main St, City, State", color = SpotVaultColors.Muted.copy(alpha = 0.7f)) },
                    singleLine = true,
                    isError = errorMessage != null,
                    enabled = !formLocked,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = spotEditFieldColors(),
                    trailingIcon = {
                        VoiceMicButton(
                            onResult = { spoken ->
                                addressText = spoken.take(400)
                                errorMessage = null; resolvedCoordinates = null; resolvedCity = ""; resolvedState = ""
                            },
                            prompt = "Dictate address…"
                        )
                    }
                )
            }
            if (resolvedCoordinates != null) {
                Text(
                    "📍 Confirmed: $resolvedAddressLabel — edit the address above to search a different place.",
                    color = SpotVaultColors.Muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Still useful for a real, well-formed address — Geocoder is fine at those.
                    // What it's genuinely unreliable on is a bare business name ("Waffle House,"
                    // which of many chain locations?), which is exactly what "Search in Maps"
                    // beside it is for. Either way this only ever finds-and-shows a match; it
                    // never saves anything itself — see the Save button below.
                    TextButton(
                        onClick = {
                            val query = addressText.trim()
                            if (query.isEmpty()) {
                                errorMessage = "Enter an address to search."
                                return@TextButton
                            }
                            if (!android.location.Geocoder.isPresent()) {
                                errorMessage = "Address search isn't available on this device."
                                return@TextButton
                            }
                            isResolving = true
                            errorMessage = null
                            coroutineScope.launch {
                                val geocoded = withContext(Dispatchers.IO) { geocodeAddress(context, query) }
                                isResolving = false
                                if (geocoded == null) {
                                    errorMessage = "Couldn't find that address — check it or try Search in Maps instead."
                                } else {
                                    resolvedCoordinates = geocoded.lat to geocoded.lng
                                    resolvedAddressLabel = geocoded.formattedAddress
                                    resolvedCity = geocoded.city
                                    resolvedState = geocoded.state
                                    addressText = geocoded.formattedAddress
                                }
                            }
                        },
                        enabled = !formLocked
                    ) {
                        if (isResolving) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = SpotVaultColors.Teal)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Find Address", color = SpotVaultColors.Teal, fontSize = 13.sp)
                    }
                    TextButton(
                        onClick = {
                            val query = addressText.trim().ifBlank { title.trim() }
                            if (query.isBlank()) {
                                errorMessage = "Type a name or address above first."
                                return@TextButton
                            }
                            // The user is about to leave the app entirely — without this, there's
                            // nothing on screen telling them what to actually do once they're in
                            // Maps, or how what they find there gets back into this form at all.
                            android.widget.Toast.makeText(
                                context,
                                "Find your spot, then tap Share ➔ DropPin Vault to bring it back!",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            if (!launchMapsSearch(context, query)) {
                                android.widget.Toast.makeText(context, "No maps app found on this device.", android.widget.Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            // Closes rather than staying open — title/addressText/etc. are seeded
                            // once, from initialData, when this dialog first composes. Leaving it
                            // open and just updating pendingSharedSpot on the way back wouldn't
                            // re-seed anything; closing it means the share-back reopens a fresh
                            // instance that does. dismissForMapsSearch (not dismissAndCleanUp)
                            // stashes any photo already attached here so it comes back attached
                            // to that fresh instance too, instead of getting deleted on the way out.
                            dismissForMapsSearch()
                        },
                        enabled = !formLocked
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search in Maps", color = SpotVaultColors.Teal, fontSize = 13.sp)
                    }
                }
            }
            // Tags and Photo side by side rather than each getting a full-width section of their
            // own — mirrors how the real Snap/Pin save screen already pairs Tags with Vehicles in
            // a weighted Row instead of stacking every "extra" field full width, and neither one
            // actually needs the full card width to begin with.
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SpotEditSectionLabel("Tags")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SaveScreenTagField(
                            selectedTags = selectedTags,
                            onSelectedTagsChange = { selectedTags = it },
                            allTags = allTags,
                            modifier = Modifier.wrapContentWidth()
                        )
                        SaveScreenSelectedTagsRow(
                            selectedTags = selectedTags,
                            onSelectedTagsChange = { selectedTags = it }
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    SpotEditSectionLabel("Photo")
                    if (photoPath.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                        .data(photoPath)
                                        .size(108, 108)
                                        .crossfade(true)
                                        .build()
                                ),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    runCatching { File(photoPath).delete() }
                                    photoPath = ""
                                },
                                enabled = !formLocked,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = SpotVaultColors.Danger, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        // One button instead of separate Camera/Gallery icons — tapping it asks
                        // which source, same chooser VaultAddPhotoDialog already uses everywhere
                        // else a photo gets attached, rather than a second, different pattern here.
                        TextButton(
                            onClick = { showPhotoChooser = true },
                            enabled = !formLocked,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            if (isCapturingPhoto) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = SpotVaultColors.Teal)
                            } else {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Photo", color = SpotVaultColors.Teal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Column {
                SpotEditSectionLabel("Notes")
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(NOTEPAD_MAX_CHARS) },
                    placeholder = { Text("Add notes…", color = SpotVaultColors.Muted.copy(alpha = 0.7f)) },
                    minLines = 2,
                    maxLines = 2,
                    enabled = !formLocked,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = spotEditFieldColors(),
                    trailingIcon = {
                        VoiceMicButton(
                            onResult = { spoken ->
                                notes = (notes + if (notes.isBlank()) spoken else " $spoken")
                                    .take(NOTEPAD_MAX_CHARS)
                            },
                            prompt = "Dictate notes…"
                        )
                    }
                )
            }
            if (errorMessage != null) {
                Text(errorMessage.orEmpty(), color = SpotVaultColors.Danger, fontSize = 12.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SpotVaultOutlinedButton(
                    onClick = { if (!formLocked) dismissAndCleanUp() },
                    enabled = !formLocked,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = spotVaultButtonShape()
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold, color = SpotVaultColors.Muted)
                }
                SpotVaultButton(
                    onClick = {
                        val knownCoordinates = resolvedCoordinates
                        // Deliberately does not fall back to geocoding here — Save only ever commits
                        // a location the user has already seen confirmed on screen (via "Find Address"
                        // or a Maps share), never one it silently resolves itself first. An ambiguous
                        // typed query landing on the Geocoder's best-guess match, saved immediately
                        // with no chance to notice it was wrong, was the actual bug this replaces.
                        if (knownCoordinates == null) {
                            errorMessage = "Find the address above (or Search in Maps) before saving."
                            return@SpotVaultButton
                        }
                        val trimmedTitle = title.trim().take(120)
                        val trimmedNotes = notes.trim().take(NOTEPAD_MAX_CHARS)
                        val resolvedAddress = resolvedAddressLabel.ifBlank { addressText.trim() }
                        isSaving = true
                        errorMessage = null
                        coroutineScope.launch {
                            val resolved = ResolvedFavoriteLocation(knownCoordinates.first, knownCoordinates.second, resolvedAddress, resolvedCity, resolvedState)
                            // One transaction, not two separate DAO calls — without this, a coroutine
                            // cancellation (leaving the dialog, process death) landing between the
                            // insert and the tag-assignment loop left a favorite saved with none of
                            // its chosen tags attached, silently, with nothing to indicate any of
                            // them didn't take.
                            val savedSpot = withContext(Dispatchers.IO) {
                                db.withTransaction {
                                    val newId = dao.insertSpotAndGetId(
                                        LocationSpot(
                                            imagePath = photoPath,
                                            locationDetails = trimmedNotes,
                                            timestamp = System.currentTimeMillis(),
                                            lat = resolved.lat,
                                            lng = resolved.lng,
                                            address = prefsSafeAddress(resolved.address),
                                            isFavorite = true,
                                            title = prefsSafeTitle(trimmedTitle),
                                            city = resolved.city.take(SPOT_CITY_STATE_MAX_CHARS),
                                            state = resolved.state.take(SPOT_CITY_STATE_MAX_CHARS)
                                        )
                                    ).toInt()
                                    selectedTags.forEach { tagName -> tagDao.assignTag(newId, tagName) }
                                    dao.getSpotById(newId)
                                }
                            }
                            isSaving = false
                            if (savedSpot != null) {
                                onSaved(savedSpot)
                            } else {
                                // The insert itself succeeded (or the transaction would have thrown) —
                                // this only means the immediate read-back after didn't find it, which
                                // the dialog closing already can't un-happen, so a Toast is the only
                                // way left to actually tell the user rather than setting error text
                                // on a composable about to leave composition, which would never render.
                                android.widget.Toast.makeText(
                                    context,
                                    "Saved, but couldn't reload it right away — check your Vault.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                            onDismiss()
                        }
                    },
                    enabled = !formLocked,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = spotVaultButtonShape()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = SpotVaultColors.ButtonLabel)
                    } else {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Same look as VaultAddPhotoDialog (the chooser every other "add a photo" entry point in the
    // app already uses) — Take Photo / Add from Gallery / Cancel — just wired to this dialog's own
    // pre-save capture functions above instead of VaultAddPhotoDialog's post-save, spot-id-based ones.
    if (showPhotoChooser) {
        VaultOverlayDialog(onDismissRequest = { showPhotoChooser = false }) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Add Photo",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = SpotVaultColors.Teal,
                    letterSpacing = 0.5.sp
                )
                Text(
                    "Attach a photo to this spot.",
                    fontSize = 13.sp,
                    color = SpotVaultColors.Muted,
                    lineHeight = 18.sp
                )
                SpotVaultButton(
                    onClick = {
                        showPhotoChooser = false
                        requestCameraCapture()
                    },
                    shape = spotVaultButtonShape(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Take Photo", fontWeight = FontWeight.Bold)
                }
                SpotVaultOutlinedButton(
                    onClick = {
                        showPhotoChooser = false
                        launchGallery()
                    },
                    shape = spotVaultButtonShape(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Add from Gallery", fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = { showPhotoChooser = false },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cancel", color = SpotVaultColors.Teal)
                }
            }
        }
    }
}

@Composable
fun ArchivedSpotsDialog(
    dao: LocationDao,
    spotPhotoDao: SpotPhotoDao,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val tagDao = remember { AppDatabase.getDatabase(context).tagDao() }
    val coroutineScope = rememberCoroutineScope()
    var archivedSpots by remember { mutableStateOf<List<LocationSpot>>(emptyList()) }
    var archivedTotalCount by remember { mutableIntStateOf(0) }
    var hasMoreArchived by remember { mutableStateOf(true) }
    var loadingMoreArchived by remember { mutableStateOf(false) }
    var tagsBySpotId by remember { mutableStateOf<Map<Int, List<TagEntity>>>(emptyMap()) }
    var spotPendingPermanentDelete by remember { mutableStateOf<LocationSpot?>(null) }
    var showUnarchiveAllConfirm by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    suspend fun refreshArchived() {
        val (spots, total) = withContext(Dispatchers.IO) {
            dao.getArchivedSpotsPage(beforeTimestamp = -1L, beforeId = 0, limit = SECONDARY_BROWSE_PAGE_SIZE) to
                dao.countArchivedSpots()
        }
        val tags = withContext(Dispatchers.IO) {
            loadTagsBySpotIds(tagDao, spots.map { it.id })
        }
        archivedSpots = spots
        archivedTotalCount = total
        hasMoreArchived = spots.size >= SECONDARY_BROWSE_PAGE_SIZE && spots.size < total
        tagsBySpotId = tags
    }

    suspend fun loadMoreArchived() {
        if (loadingMoreArchived || !hasMoreArchived) return
        if (archivedSpots.size >= SECONDARY_BROWSE_FULL_CAP) {
            hasMoreArchived = false
            return
        }
        val anchor = archivedSpots.lastOrNull() ?: return
        loadingMoreArchived = true
        try {
            val page = withContext(Dispatchers.IO) {
                dao.getArchivedSpotsPage(
                    beforeTimestamp = anchor.timestamp,
                    beforeId = anchor.id,
                    limit = SECONDARY_BROWSE_PAGE_SIZE
                )
            }
            if (page.isEmpty()) {
                hasMoreArchived = false
            } else {
                val existing = archivedSpots.map { it.id }.toHashSet()
                val newRows = page.filter { it.id !in existing }
                archivedSpots = (archivedSpots + newRows).take(SECONDARY_BROWSE_FULL_CAP)
                val tags = withContext(Dispatchers.IO) {
                    loadTagsBySpotIds(tagDao, newRows.map { it.id })
                }
                tagsBySpotId = tagsBySpotId + tags
                hasMoreArchived = page.size >= SECONDARY_BROWSE_PAGE_SIZE &&
                    archivedSpots.size < archivedTotalCount &&
                    archivedSpots.size < SECONDARY_BROWSE_FULL_CAP
            }
        } finally {
            loadingMoreArchived = false
        }
    }

    LaunchedEffect(Unit) {
        refreshArchived()
    }

    LaunchedEffect(listState, hasMoreArchived, archivedSpots.size) {
        if (!hasMoreArchived) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last to info.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, total) ->
                if (total > 0 && lastVisible >= total - 8) {
                    loadMoreArchived()
                }
            }
    }

    if (showUnarchiveAllConfirm) {
        PremiumDialog(
            onDismissRequest = { showUnarchiveAllConfirm = false },
            title = {
                Text("Unarchive All?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.OnSurface)
            },
            content = {
                Text(
                    "All $archivedTotalCount spot${if (archivedTotalCount == 1) "" else "s"} will return to the main Vault.",
                    color = SpotVaultColors.Muted
                )
            },
            confirmButton = {
                SpotVaultButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            dao.unarchiveAllSpots()
                            refreshArchived()
                        }
                        showUnarchiveAllConfirm = false
                    }
                ) { Text("Unarchive All") }
            },
            dismissButton = {
                TextButton(onClick = { showUnarchiveAllConfirm = false }) {
                    Text("Cancel", color = SpotVaultColors.Teal)
                }
            }
        )
    }

    if (showDeleteAllConfirm) {
        PremiumDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = {
                Text("Delete All Forever?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.OnSurface)
            },
            content = {
                Text(
                    "All $archivedTotalCount spot${if (archivedTotalCount == 1) "" else "s"} will be permanently deleted and cannot be recovered.",
                    color = SpotVaultColors.Muted
                )
            },
            confirmButton = {
                SpotVaultButton(
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            // UI list is capped at 500 — page until empty so archive #501+
                            // (and photos) are not left undeletable forever.
                            while (true) {
                                val page = dao.getArchivedSpots()
                                if (page.isEmpty()) break
                                permanentlyDeleteSpotsAndPhotos(dao, spotPhotoDao, page)
                            }
                            tagDao.recomputeAllUsageCounts()
                            refreshArchived()
                        }
                        showDeleteAllConfirm = false
                    }
                ) { Text("Delete All Forever") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirm = false }) {
                    Text("Cancel", color = SpotVaultColors.Teal)
                }
            }
        )
    }

    spotPendingPermanentDelete?.let { spot ->
        PremiumDialog(
            onDismissRequest = { spotPendingPermanentDelete = null },
            title = {
                Text(
                    "Delete Forever?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SpotVaultColors.OnSurface
                )
            },
            content = {
                Text(
                    "This spot will be permanently deleted and cannot be recovered.",
                    color = SpotVaultColors.Muted
                )
            },
            confirmButton = {
                SpotVaultButton(
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            permanentlyDeleteSpotAndPhotos(dao, spotPhotoDao, spot)
                            tagDao.recomputeAllUsageCounts()
                            refreshArchived()
                        }
                        spotPendingPermanentDelete = null
                    }
                ) { Text("Delete Forever") }
            },
            dismissButton = {
                TextButton(onClick = { spotPendingPermanentDelete = null }) {
                    Text("Cancel", color = SpotVaultColors.Teal)
                }
            }
        )
    }

    // Height comes from VaultOverlayDialog itself — see the matching comment on
    // VaultCalendarDayResultsDialog for why a second, independent screenHeightDp-based cap here
    // was redundant and could disagree with it.
    VaultOverlayDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Archived Spots",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = SpotVaultColors.Teal,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Hidden from the Vault but kept forever until you unarchive or delete them.",
                        fontSize = 12.sp,
                        color = SpotVaultColors.Muted,
                        lineHeight = 16.sp
                    )
                    if (archivedTotalCount > archivedSpots.size) {
                        Text(
                            text = "Showing ${archivedSpots.size} of $archivedTotalCount",
                            fontSize = 12.sp,
                            color = SpotVaultColors.Muted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(SpotVaultColors.Surface.copy(alpha = 0.75f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SpotVaultColors.OnSurface)
                }
            }

            if (archivedSpots.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showUnarchiveAllConfirm = true }) {
                        Text("Unarchive All", color = SpotVaultColors.Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showDeleteAllConfirm = true }) {
                        Text("Delete All Forever", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (archivedSpots.isEmpty()) {
                Text(
                    text = "No archived spots.",
                    color = SpotVaultColors.Muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(archivedSpots, key = { it.id }) { spot ->
                        val displayTitle = vaultSpotDisplayTitle(spot).ifBlank {
                            spot.address.ifBlank { "Untitled spot" }
                        }
                        val savedLabel = dateFormatter.format(Date(spot.timestamp))
                        val spotTags = tagsBySpotId[spot.id].orEmpty()
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = SpotVaultColors.Elevated)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    VaultSpotThumbnail(
                                        item = spot,
                                        onClick = {},
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = displayTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = SpotVaultColors.OnSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (spot.locationDetails.isNotBlank()) {
                                            Text(
                                                text = spot.locationDetails,
                                                fontSize = 12.sp,
                                                color = SpotVaultColors.Muted,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                        if (spotTags.isNotEmpty()) {
                                            VaultTagChipsRow(tags = spotTags, modifier = Modifier.padding(top = 4.dp))
                                        }
                                        Text(
                                            text = "Saved $savedLabel",
                                            fontSize = 11.sp,
                                            color = SpotVaultColors.Muted,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                dao.unarchiveSpotIfArchived(spot.id)
                                                refreshArchived()
                                            }
                                        }
                                    ) {
                                        Text("Unarchive", color = SpotVaultColors.Teal, fontSize = 13.sp)
                                    }
                                    TextButton(onClick = { spotPendingPermanentDelete = spot }) {
                                        Text(
                                            "Delete Forever",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// Location Browser — State list -> City list -> per-city entry list with bulk actions.
// ---------------------------------------------------------------------------------

internal data class SpotLocationParts(val state: String, val city: String)

/** City/state are read straight from the spot's own [LocationSpot.city]/[LocationSpot.state]
 * columns whenever present — those come directly from the geocoder's own structured
 * locality/adminArea fields at save time (see [buildGeocodedAddress]), so they're accurate
 * regardless of country. For older spots saved before those columns existed, or anything still
 * awaiting [resolveStuckSpotAddresses], falls back to parsing the flattened address text — but
 * only when there are at least 3 comma-separated segments, a real signal of "street, city,
 * region" structure. A 2-segment address is fundamentally ambiguous (it could be "city, country"
 * just as easily as "street, city" with the region omitted — plenty of real getAddressLine(0)
 * results outside the US look like the former, e.g. many UK city addresses have no
 * county/state-equivalent segment at all), so guessing which one it is at that length used to
 * confidently mislabel non-US spots rather than admit uncertainty. Anything unparseable, or too
 * ambiguous to trust, lands in "Unknown" — resolved for real the next time
 * [resolveStuckSpotAddresses] runs and re-geocodes it properly. */
internal fun spotLocationParts(spot: LocationSpot): SpotLocationParts {
    if (spot.city.isNotBlank() || spot.state.isNotBlank()) {
        return SpotLocationParts(
            state = spot.state.ifBlank { "Unknown" },
            city = spot.city.ifBlank { "Unknown" }
        )
    }
    val address = spot.address.trim()
    if (address.isEmpty() || address.startsWith("Lat:")) return SpotLocationParts("Unknown", "Unknown")
    val parts = address.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return if (parts.size >= 3) {
        SpotLocationParts(state = parts.last(), city = parts[parts.size - 2])
    } else {
        SpotLocationParts("Unknown", "Unknown")
    }
}

/** Retries geocoding for every spot missing real city/state data — either still stuck on the raw
 * "Lat: x, Lng: y" fallback ([reverseGeocodeAddress] never resolved a real address for them,
 * usually a one-off network/geocoder hiccup at save time — underground parking, a dead zone), or
 * a spot that has address text but blank city/state columns (older spots saved before those
 * columns existed). This used to also cover that second case with a one-time migration that
 * guessed city/state by splitting the flattened address text on commas and trusting position —
 * "last segment is state, one before it is city." That's a US-shaped assumption: a real
 * getAddressLine(0) result follows each country's own local format, and plenty of them don't even
 * have a state/county-equivalent segment at all (many UK city addresses, for one) — for anyone
 * outside that shape it silently wrote a wrong value into city/state rather than leaving it
 * blank. Re-running the real geocoder here, off the spot's own already-stored lat/lng, gets the
 * actual structured locality/adminArea fields straight from Android's Geocoder instead of
 * guessing from a formatted string — correct for any country, not just a better guess.
 *
 * Not gated behind a one-time "done" flag — this runs on every app launch (see MainActivity's
 * onCreate) instead, so a spot that failed to resolve (e.g. offline at the time) gets retried
 * next launch rather than exactly once for the entire lifetime of the install. Cheap: only
 * touches rows still genuinely missing city/state, and self-limiting, since a spot drops out of
 * its own filter the moment it resolves. */
suspend fun resolveStuckSpotAddresses(context: Context, dao: LocationDao) {
    // Cap per launch so a vault with hundreds of stuck rows cannot storm the geocoder + Room
    // invalidation tracker for minutes. Prefer active vault spots; deleted/archived can wait.
    // Bulk-mutation suppress — each updateSpot used to debounce-kick a full widget refresh chain
    // (often once per geocode) on every cold start for spots that stay stuck offline.
    WidgetThemeHelper.withBulkVaultMutation(context) {
        val needsResolution = dao.getSpotsNeedingAddressResolution(MAX_STUCK_ADDRESS_RESOLUTIONS_PER_LAUNCH)
        needsResolution.forEach { spot ->
            val geocoded = reverseGeocodeAddress(context, spot.lat, spot.lng)
            if (!geocoded.full.startsWith("Lat:")) {
                // address/city/state used to be the only fields this touched — the spot's displayed
                // title (what actually shows in the Vault list) was left permanently stuck on its
                // original "30.3536, -88.5245" placeholder even after this resolved a real address
                // behind the scenes. Only overwrites the title when it's still that exact placeholder,
                // so a title the user typed or edited is never touched.
                val newTitle = if (isPlaceholderCoordinateTitle(spot.title, spot.lat, spot.lng)) {
                    quickActionSpotTitle(geocoded, spot.lat, spot.lng)
                } else {
                    spot.title
                }
                // Re-fetches rather than spot.copy(...) — this loops over every stuck spot doing a
                // real network geocode per iteration, easily slow enough for the user to edit (or
                // attach a photo to) the exact spot this is currently resolving in the background.
                // copy()-ing the snapshot taken at the top of this function would silently revert
                // whatever they'd just changed.
                val current = dao.getSpotById(spot.id) ?: return@forEach
                dao.updateSpot(current.copy(address = geocoded.full, city = geocoded.city, state = geocoded.state, title = newTitle))
            }
        }
    }
}

private const val MAX_STUCK_ADDRESS_RESOLUTIONS_PER_LAUNCH = 25

private sealed class LocationBrowserLevel {
    data object States : LocationBrowserLevel()
    data class Cities(val state: String) : LocationBrowserLevel()
    data class Entries(val state: String, val city: String) : LocationBrowserLevel()
}

/** Encodes the drill-down depth as a 0/1/2-element list so it can ride out the composable being
 * disposed and rebuilt (e.g. tapping a spot pushes SPOT_DETAIL, which tears this down; without
 * this, back would always land back on the top-level States list instead of where you were). */
private val LocationBrowserLevelSaver = listSaver<LocationBrowserLevel, String>(
    save = { level ->
        when (level) {
            is LocationBrowserLevel.States -> emptyList()
            is LocationBrowserLevel.Cities -> listOf(level.state)
            is LocationBrowserLevel.Entries -> listOf(level.state, level.city)
        }
    },
    restore = { saved ->
        when (saved.size) {
            0 -> LocationBrowserLevel.States
            1 -> LocationBrowserLevel.Cities(saved[0])
            else -> LocationBrowserLevel.Entries(saved[0], saved[1])
        }
    }
)

/** State list -> city list -> per-city entry list, all in the same adaptive window used
 * elsewhere in the Vault. Tapping a row drills in; the header back arrow (or system back)
 * steps back up one level instead of closing outright. */
@Composable
fun VaultLocationBrowserDialog(
    dao: LocationDao,
    prefs: SharedPreferences,
    onDismiss: () -> Unit,
    onViewSpot: (LocationSpot) -> Unit,
    onShareRequest: (ShareSpotPayload) -> Unit,
    coroutineScope: CoroutineScope
) {
    var level by rememberSaveable(stateSaver = LocationBrowserLevelSaver) { mutableStateOf<LocationBrowserLevel>(LocationBrowserLevel.States) }
    var selectedItems by remember { mutableStateOf(setOf<Int>()) }
    var selectionModeActive by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val vaultEpoch = rememberVaultInvalidationEpoch()

    var stateRows by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var cityRows by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var entrySpots by remember { mutableStateOf<List<LocationSpot>>(emptyList()) }
    var entryTotalCount by remember { mutableIntStateOf(0) }
    var entryLimit by remember { mutableIntStateOf(SECONDARY_BROWSE_PAGE_SIZE) }

    LaunchedEffect(level) {
        entryLimit = SECONDARY_BROWSE_PAGE_SIZE
    }

    LaunchedEffect(level, vaultEpoch, entryLimit) {
        when (val l = level) {
            is LocationBrowserLevel.States -> {
                stateRows = withContext(Dispatchers.IO) {
                    dao.getActiveStateCounts().map { it.name to it.count }
                }
            }
            is LocationBrowserLevel.Cities -> {
                cityRows = withContext(Dispatchers.IO) {
                    dao.getActiveCityCounts(l.state).map { it.name to it.count }
                }
            }
            is LocationBrowserLevel.Entries -> {
                val (spots, total) = withContext(Dispatchers.IO) {
                    dao.getActiveVaultSpotsForCity(l.state, l.city, entryLimit) to
                        dao.countActiveVaultSpotsForCity(l.state, l.city)
                }
                entrySpots = spots
                entryTotalCount = total
            }
        }
    }

    fun exitEntriesSelection() {
        selectedItems = emptySet()
        selectionModeActive = false
    }

    // Shared by the confirm dialog's own Delete button and the confirm_delete=false instant
    // path below — soft-deletes then offers Undo, same as the main Vault's own delete handling.
    fun deleteSpots(ids: Set<Int>) {
        exitEntriesSelection()
        coroutineScope.launch(Dispatchers.IO) {
            ids.forEach { dao.softDeleteSpot(it) }
        }
        VaultUndoSnackbar.show(if (ids.size == 1) "Spot deleted" else "${ids.size} spots deleted") {
            withContext(Dispatchers.IO) { ids.forEach { dao.restoreSpotIfSoftDeleted(it) } }
        }
    }

    fun stepBack() {
        when (val l = level) {
            is LocationBrowserLevel.States -> onDismiss()
            is LocationBrowserLevel.Cities -> level = LocationBrowserLevel.States
            is LocationBrowserLevel.Entries -> {
                exitEntriesSelection()
                level = LocationBrowserLevel.Cities(l.state)
            }
        }
    }

    val title = when (val l = level) {
        is LocationBrowserLevel.States -> "Browse by Location"
        is LocationBrowserLevel.Cities -> l.state
        is LocationBrowserLevel.Entries -> "${l.city}, ${l.state}"
    }

    // Height comes from VaultOverlayDialog itself — see the matching comment on
    // VaultCalendarDayResultsDialog for why a second, independent screenHeightDp-based cap here
    // was redundant and could disagree with it.
    VaultOverlayDialog(onDismissRequest = { stepBack() }) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (level != LocationBrowserLevel.States) {
                        IconButton(onClick = { stepBack() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = SpotVaultColors.Teal)
                        }
                    }
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = SpotVaultColors.OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SpotVaultColors.Muted)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (val l = level) {
                is LocationBrowserLevel.States -> {
                    LocationBrowserRowList(
                        rows = stateRows,
                        emptyMessage = "No saved spots yet.",
                        onRowClick = { state, _ -> level = LocationBrowserLevel.Cities(state) }
                    )
                }
                is LocationBrowserLevel.Cities -> {
                    LocationBrowserRowList(
                        rows = cityRows,
                        emptyMessage = "No cities found.",
                        onRowClick = { city, _ -> level = LocationBrowserLevel.Entries(l.state, city) }
                    )
                }
                is LocationBrowserLevel.Entries -> {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        if (entryTotalCount > entrySpots.size) {
                            Text(
                                text = "Showing ${entrySpots.size} of $entryTotalCount",
                                fontSize = 12.sp,
                                color = SpotVaultColors.Muted,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        VaultFilterableSpotList(
                        baseSpots = entrySpots,
                        prefs = prefs,
                        dao = dao,
                        selectedItems = selectedItems,
                        onSelectedItemsChange = { selectedItems = it },
                        // Bulk delete from the top action bar always skips the confirmation modal
                        // — same reasoning as the main Vault: soft-delete is already reversible via
                        // Recently Deleted, and now Undo, so a blocking dialog for it is redundant.
                        onShowDeleteConfirm = { deleteSpots(selectedItems) },
                        onSwipeDeleteSpot = { spot ->
                            // Single-spot swipe delete still respects the user's own "Confirm
                            // before deleting" setting — only bulk delete above skips it always.
                            if (prefs.getBoolean("confirm_delete", true)) {
                                selectedItems = setOf(spot.id)
                                showDeleteConfirm = true
                            } else {
                                deleteSpots(setOf(spot.id))
                            }
                        },
                        onShareRequest = onShareRequest,
                        // Deliberately doesn't call onDismiss() here — unlike the explicit Close
                        // button and stepBack() above, opening a spot isn't the user leaving the
                        // browser. showLocationBrowser and this composable's own `level` drill-down
                        // are both rememberSaveable (see MainActivity.kt's HistoryDialogContent),
                        // so leaving this dialog open just means it's still there, at the same
                        // State/City drill-down, once the user backs out of spot detail — matching
                        // how the Calendar day-results dialog already behaves.
                        onViewSpot = onViewSpot,
                        coroutineScope = coroutineScope,
                        modifier = Modifier.weight(1f, fill = false),
                        emptyTitle = "No spots here yet",
                        emptySubtitle = "Spots saved in ${l.city} will show up here."
                        )
                        if (entrySpots.size < entryTotalCount &&
                            entrySpots.size < SECONDARY_BROWSE_FULL_CAP
                        ) {
                            TextButton(
                                onClick = {
                                    entryLimit = (entryLimit + SECONDARY_BROWSE_PAGE_SIZE)
                                        .coerceAtMost(SECONDARY_BROWSE_FULL_CAP)
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Show more", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = SpotVaultColors.Surface,
            titleContentColor = SpotVaultColors.OnSurface,
            textContentColor = SpotVaultColors.Muted,
            title = { Text("Delete ${selectedItems.size} spot${if (selectedItems.size == 1) "" else "s"}?") },
            text = { Text("These move to Recently Deleted and can be restored for a while before they're gone for good.") },
            confirmButton = {
                TextButton(onClick = {
                    val toDelete = selectedItems
                    showDeleteConfirm = false
                    deleteSpots(toDelete)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = SpotVaultColors.Teal) }
            }
        )
    }
}

/** Shared row-list rendering for the States and Cities levels — name, count, chevron-forward. */
@Composable
private fun LocationBrowserRowList(
    rows: List<Pair<String, Int>>,
    emptyMessage: String,
    onRowClick: (String, Int) -> Unit
) {
    if (rows.isEmpty()) {
        Text(emptyMessage, color = SpotVaultColors.Muted, modifier = Modifier.padding(vertical = 32.dp))
        return
    }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val maxListHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.5f).dp
        .coerceIn(220.dp, 520.dp)
    Box(modifier = Modifier.heightIn(max = maxListHeight)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(end = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 4.dp)
        ) {
            items(rows, key = { it.first }) { (name, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SpotVaultColors.Elevated.copy(alpha = 0.55f))
                        .clickable { onRowClick(name, count) }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, color = SpotVaultColors.OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$count", color = SpotVaultColors.Muted, fontSize = 13.sp, modifier = Modifier.padding(end = 6.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = SpotVaultColors.Teal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        val canScrollList = listState.canScrollForward || listState.canScrollBackward
        if (canScrollList) {
            PremiumVerticalScrollbar(
                listState = listState,
                emphasizeHint = true,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
                    .width(6.dp)
            )
        }
    }
    if (listState.canScrollForward) {
        AnimatedScrollDownHint(modifier = Modifier.padding(top = 2.dp))
    }
}

