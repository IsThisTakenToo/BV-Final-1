package com.spotvault.app

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.room.InvalidationTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

/** First paint / scroll append size — enough for several date sections without loading years. */
internal const val VAULT_BROWSE_PAGE_SIZE = 150

/** Hard ceiling for filter/search modes that still need an in-memory scan of active spots. */
internal const val VAULT_BROWSE_FULL_CAP = 3000

/**
 * True when in-memory filter/sort needs every active spot (not just a newest/oldest prefix).
 * Default Newest/Oldest browse stays windowed; these modes expand to a full (notes-stripped) load.
 */
fun vaultBrowseNeedsFullDataset(
    searchQuery: String,
    sortBy: String,
    nearMeFilter: VaultNearMeFilter,
    selectedTag: String?,
    showFavoritesOnly: Boolean,
    selectedVehicleId: Int?
): Boolean {
    if (searchQuery.isNotBlank()) return true
    if (selectedTag != null) return true
    if (nearMeFilter != VaultNearMeFilter.OFF) return true
    if (showFavoritesOnly) return true
    if (selectedVehicleId != null) return true
    return sortBy == VaultSortOption.CLOSEST || sortBy == VaultSortOption.ALPHABETICAL
}

@Composable
internal fun rememberVaultInvalidationEpoch(): Long {
    val context = LocalContext.current
    var epoch by remember { mutableLongStateOf(0L) }
    DisposableEffect(context) {
        val db = AppDatabase.getDatabase(context)
        val mainHandler = Handler(Looper.getMainLooper())
        val observer = object : InvalidationTracker.Observer("location_history") {
            override fun onInvalidated(tables: Set<String>) {
                mainHandler.post { epoch += 1L }
            }
        }
        db.invalidationTracker.addObserver(observer)
        onDispose { db.invalidationTracker.removeObserver(observer) }
    }
    return epoch
}

/**
 * Windowed Vault list for the main tab. Only a short notes prefix is loaded so multi-KB
 * notepads stay out of list RAM. Room InvalidationTracker refreshes after any spot write.
 */
@Composable
fun rememberVaultBrowseSpots(
    dao: LocationDao,
    searchQuery: String,
    sortBy: String,
    nearMeFilter: VaultNearMeFilter,
    selectedTag: String?,
    showFavoritesOnly: Boolean,
    selectedVehicleId: Int?,
    listState: LazyListState,
    gridState: LazyStaggeredGridState,
    vaultViewMode: VaultViewMode
): List<LocationSpot> {
    val needsFull = vaultBrowseNeedsFullDataset(
        searchQuery = searchQuery,
        sortBy = sortBy,
        nearMeFilter = nearMeFilter,
        selectedTag = selectedTag,
        showFavoritesOnly = showFavoritesOnly,
        selectedVehicleId = selectedVehicleId
    )
    val vaultEpoch = rememberVaultInvalidationEpoch()
    var limit by remember { mutableIntStateOf(VAULT_BROWSE_PAGE_SIZE) }
    var spots by remember { mutableStateOf<List<LocationSpot>>(emptyList()) }
    var hasMore by remember { mutableStateOf(true) }

    LaunchedEffect(needsFull) {
        if (!needsFull) limit = VAULT_BROWSE_PAGE_SIZE
    }

    LaunchedEffect(vaultEpoch, needsFull, limit, sortBy) {
        val (loaded, more) = withContext(Dispatchers.IO) {
            val pinned = dao.getPinnedVaultSpotsForBrowse()
            if (needsFull) {
                val page = dao.getActiveVaultSpotsForBrowseCapped(VAULT_BROWSE_FULL_CAP)
                (pinned + page).distinctBy { it.id } to false
            } else {
                val rows = when (sortBy) {
                    VaultSortOption.OLDEST -> dao.getActiveVaultSpotsOldestPrefix(limit)
                    else -> dao.getActiveVaultSpotsNewestPrefix(limit)
                }
                (pinned + rows).distinctBy { it.id } to (rows.size >= limit)
            }
        }
        spots = loaded
        hasMore = more
    }

    LaunchedEffect(listState, gridState, vaultViewMode, needsFull, hasMore, spots.size, limit) {
        if (needsFull || !hasMore) return@LaunchedEffect
        snapshotFlow {
            when (vaultViewMode) {
                VaultViewMode.GRID -> {
                    val info = gridState.layoutInfo
                    val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val total = info.totalItemsCount
                    last to total
                }
                VaultViewMode.LIST -> {
                    val info = listState.layoutInfo
                    val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val total = info.totalItemsCount
                    last to total
                }
            }
        }
            .distinctUntilChanged()
            .collect { (lastVisible, total) ->
                if (total > 0 && lastVisible >= total - 8 && hasMore) {
                    limit = (limit + VAULT_BROWSE_PAGE_SIZE).coerceAtMost(VAULT_BROWSE_FULL_CAP)
                }
            }
    }

    return spots
}

/** Calendar dots — timestamps only (no spot rows in Compose). Pages through the DB so a
 * multi-year vault never materializes every timestamp Long at once. */
@Composable
fun rememberCalendarSpotDayStarts(dao: LocationDao, enabled: Boolean): Set<Long> {
    val vaultEpoch = rememberVaultInvalidationEpoch()
    var days by remember { mutableStateOf<Set<Long>>(emptySet()) }
    LaunchedEffect(enabled, vaultEpoch) {
        if (!enabled) {
            days = emptySet()
            return@LaunchedEffect
        }
        days = withContext(Dispatchers.IO) {
            val result = mutableSetOf<Long>()
            var afterId = 0
            while (true) {
                val page = dao.getActiveVaultTimestampsPage(afterId, 2_000)
                if (page.isEmpty()) break
                page.forEach { row ->
                    result.add(calendarDayStart(row.timestamp))
                    afterId = row.id
                }
            }
            result
        }
    }
    return days
}

/** Spots for one calendar day (notes-stripped). */
@Composable
fun rememberCalendarDaySpots(dao: LocationDao, dayStartMillis: Long?): List<LocationSpot> {
    val vaultEpoch = rememberVaultInvalidationEpoch()
    var spots by remember { mutableStateOf<List<LocationSpot>>(emptyList()) }
    LaunchedEffect(dayStartMillis, vaultEpoch) {
        val dayStart = dayStartMillis
        if (dayStart == null) {
            spots = emptyList()
            return@LaunchedEffect
        }
        val nextMidnight = java.util.Calendar.getInstance().apply {
            timeInMillis = dayStart
            add(java.util.Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        spots = withContext(Dispatchers.IO) {
            dao.getActiveVaultSpotsForDay(dayStart, nextMidnight)
        }
    }
    return spots
}

/** Favorites hub — favorites only, notes-stripped. */
@Composable
fun rememberFavoriteOverlaySpots(dao: LocationDao, enabled: Boolean): List<LocationSpot> {
    val vaultEpoch = rememberVaultInvalidationEpoch()
    var spots by remember { mutableStateOf<List<LocationSpot>>(emptyList()) }
    LaunchedEffect(enabled, vaultEpoch) {
        if (!enabled) {
            spots = emptyList()
            return@LaunchedEffect
        }
        spots = withContext(Dispatchers.IO) { dao.getFavoriteVaultSpotsForBrowse() }
    }
    return spots
}
