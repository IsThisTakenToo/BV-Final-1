package com.spotvault.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

/** Emoji badge for a saved spot — title-text hints only now that category is gone. */
fun vaultSpotCategoryEmoji(spot: LocationSpot): String {
    val titleLower = spot.title.lowercase(Locale.US)
    return when {
        titleLower.contains("parked truck") -> "🚗"
        titleLower.contains("boat ramp") -> "🚤"
        titleLower.contains("submerged") -> "🪵"
        titleLower.contains("catch") || titleLower.contains("strike") -> "🎣"
        titleLower.contains("blood") || titleLower.contains("hit trail") -> "🩸"
        titleLower.contains("tracks") || titleLower.contains("game trail") -> "🐾"
        titleLower.contains("scrape") || titleLower.contains("rub") ||
            titleLower.contains("bedding") -> "🦌"
        titleLower.contains("forage") -> "🍄"
        titleLower.contains("fresh water") -> "💧"
        titleLower.contains("camp") || titleLower.contains("shelter") -> "🏕️"
        titleLower.contains("intersection") -> "🔀"
        else -> "📍"
    }
}

object VaultSortOption {
    const val NEWEST = "Newest"
    const val OLDEST = "Oldest"
    const val CLOSEST = "Closest"
    const val ALPHABETICAL = "Alphabetical"

    val all = listOf(NEWEST, OLDEST, CLOSEST, ALPHABETICAL)
}

// Some keyboards/IMEs append an invisible VARIATION SELECTOR-16 (U+FE0F, forces emoji-style
// rendering) after an emoji and some don't — two visually-identical emoji can be different
// strings at the code-point level, which made a raw String.contains() silently fail to match an
// emoji tag against the same emoji typed into a search box. Stripping both selectors before
// comparing (only as a fallback, so it costs nothing on the common non-emoji path) fixes that
// without weakening the exact match already tried first.
private fun String.withoutVariationSelectors(): String =
    filterNot { it == '︎' || it == '️' }

fun searchTextMatches(text: String, query: String): Boolean {
    if (text.contains(query, ignoreCase = true)) return true
    val strippedQuery = query.withoutVariationSelectors()
    if (strippedQuery.isEmpty()) return false
    return text.withoutVariationSelectors().contains(strippedQuery, ignoreCase = true)
}

private const val METERS_PER_MILE = 1609.344

/** "Near Me" system filter chip — a 3-step cycle (tap to advance) instead of a single on/off
 * toggle, so switching between a walking-scale and driving-scale radius doesn't require opening
 * a settings menu. [next] wraps back to [OFF] after [DRIVING]. */
enum class VaultNearMeFilter(val radiusMiles: Double?, val label: String) {
    OFF(null, "📍 Near Me"),
    WALKING(1.0, "📍 Near Me (1 mi)"),
    DRIVING(5.0, "📍 Near Me (5 mi)");

    fun next(): VaultNearMeFilter = when (this) {
        OFF -> WALKING
        WALKING -> DRIVING
        DRIVING -> OFF
    }
}

fun filterAndSortVaultSpots(
    spots: List<LocationSpot>,
    searchQuery: String,
    showFavoritesOnly: Boolean,
    vehicleId: Int?,
    sortBy: String,
    userLocation: Pair<Double, Double>?,
    tagsBySpotId: Map<Int, List<TagEntity>> = emptyMap(),
    vehicleNameById: Map<Int, String> = emptyMap(),
    nearMeFilter: VaultNearMeFilter = VaultNearMeFilter.OFF,
    showPhotosOnly: Boolean = false
): List<LocationSpot> {
    val filtered = spots.filter { item ->
        val matchSearch = if (searchQuery.isNotBlank()) {
            // Everything visible about a spot is fair game here — address/city/state (also the
            // primary text on grid cards and the Location Browser), its tags, and its vehicle's
            // name — a search that ignored any of these could type-match something the user can
            // plainly see on the spot and still come back empty.
            item.title.contains(searchQuery, ignoreCase = true) ||
                item.locationDetails.contains(searchQuery, ignoreCase = true) ||
                item.address.contains(searchQuery, ignoreCase = true) ||
                item.city.contains(searchQuery, ignoreCase = true) ||
                item.state.contains(searchQuery, ignoreCase = true) ||
                tagsBySpotId[item.id]?.any { searchTextMatches(it.name, searchQuery) } == true ||
                item.vehicleId?.let { vehicleNameById[it]?.contains(searchQuery, ignoreCase = true) } == true
        } else {
            true
        }
        val matchFav = if (showFavoritesOnly) item.isFavorite else true
        val matchVehicle = vehicleId == null || item.vehicleId == vehicleId
        val matchPhotos = !showPhotosOnly || item.imagePath.isNotBlank()
        val matchNearMe = run {
            val radiusMiles = nearMeFilter.radiusMiles
            if (radiusMiles == null) {
                true
            } else {
                val user = userLocation
                user != null && spotSortDistanceMeters(user, item)?.let { it <= radiusMiles * METERS_PER_MILE } == true
            }
        }
        !item.isWishlist && !item.isArchived && matchSearch && matchFav && matchVehicle &&
            matchPhotos && matchNearMe
    }

    return filtered.sortedWith(
        Comparator { a, b ->
            when (sortBy) {
                VaultSortOption.OLDEST -> a.timestamp.compareTo(b.timestamp)
                VaultSortOption.CLOSEST -> {
                    val user = userLocation
                    if (user == null) {
                        b.timestamp.compareTo(a.timestamp)
                    } else {
                        val distA = spotSortDistanceMeters(user, a)
                        val distB = spotSortDistanceMeters(user, b)
                        when {
                            distA == null && distB == null -> b.timestamp.compareTo(a.timestamp)
                            distA == null -> 1
                            distB == null -> -1
                            else -> distA.compareTo(distB)
                        }
                    }
                }
                VaultSortOption.ALPHABETICAL -> {
                    val titleA = a.displayTitleForSort()
                    val titleB = b.displayTitleForSort()
                    titleA.compareTo(titleB, ignoreCase = true)
                }
                else -> b.timestamp.compareTo(a.timestamp)
            }
        }
    )
}

private fun LocationSpot.displayTitleForSort(): String =
    title.ifBlank { "Untitled" }

private fun spotSortDistanceMeters(
    user: Pair<Double, Double>,
    spot: LocationSpot
): Double? {
    if (spot.lat == 0.0 && spot.lng == 0.0) return null
    return haversineDistanceMeters(user.first, user.second, spot.lat, spot.lng)
}

/** Shared Vault tag map — one lightweight Flow instead of @Relation reloading every spot.
 * When [loadAllAssignments] is false, only tags for [spotIds] are loaded (windowed browse).
 * Search / tag-filter need the full active assignment index. */
@Composable
fun rememberVaultSpotIdToTags(
    tagDao: TagDao,
    spotIds: List<Int> = emptyList(),
    loadAllAssignments: Boolean = true
): Map<Int, List<TagEntity>> {
    val assignmentFlow = remember(loadAllAssignments) {
        if (loadAllAssignments) tagDao.observeActiveSpotTagAssignments()
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }
    val allRows by assignmentFlow.collectAsState(initial = emptyList())
    val scoped by produceState(emptyMap<Int, List<TagEntity>>(), spotIds, loadAllAssignments) {
        if (loadAllAssignments) {
            value = emptyMap()
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            if (spotIds.isEmpty()) {
                emptyMap()
            } else {
                spotIds.chunked(500)
                    .flatMap { chunk -> tagDao.getTagAssignmentsForSpots(chunk) }
                    .toTagsBySpotId()
            }
        }
    }
    return if (loadAllAssignments) {
        remember(allRows) { allRows.toTagsBySpotId() }
    } else {
        scoped
    }
}

/**
 * Filter/sort off the composition critical path. Search is lightly debounced so typing does not
 * re-scan thousands of spots on every keystroke; other inputs update immediately on Default.
 */
@Composable
fun rememberFilteredVaultSpots(
    spots: List<LocationSpot>,
    searchQuery: String,
    showFavoritesOnly: Boolean,
    vehicleId: Int?,
    sortBy: String,
    userLocation: Pair<Double, Double>?,
    tagsBySpotId: Map<Int, List<TagEntity>>,
    vehicleNameById: Map<Int, String>,
    nearMeFilter: VaultNearMeFilter,
    showPhotosOnly: Boolean,
    selectedTag: String?
): List<LocationSpot> {
    var debouncedSearch by remember { mutableStateOf(searchQuery) }
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            debouncedSearch = ""
        } else {
            delay(80)
            debouncedSearch = searchQuery
        }
    }

    val filtered by produceState(
        initialValue = emptyList(),
        spots,
        debouncedSearch,
        showFavoritesOnly,
        vehicleId,
        sortBy,
        userLocation,
        tagsBySpotId,
        vehicleNameById,
        nearMeFilter,
        showPhotosOnly,
        selectedTag
    ) {
        value = withContext(Dispatchers.Default) {
            val base = filterAndSortVaultSpots(
                spots = spots,
                searchQuery = debouncedSearch,
                showFavoritesOnly = showFavoritesOnly,
                vehicleId = vehicleId,
                sortBy = sortBy,
                userLocation = userLocation,
                tagsBySpotId = tagsBySpotId,
                vehicleNameById = vehicleNameById,
                nearMeFilter = nearMeFilter,
                showPhotosOnly = showPhotosOnly
            )
            val tag = selectedTag
            if (tag == null) {
                base
            } else {
                base.filter { spot ->
                    tagsBySpotId[spot.id]?.any { it.name.equals(tag, ignoreCase = true) } == true
                }
            }
        }
    }
    return filtered
}

// Category was removed entirely (data column, GPX/backup fields, search matching, widget
// subtitles, VaultCategoryStore/VaultCategoryDef, the category-group filter) — tags (see
// VaultTagManagerDialog in HistoryVaultDialog.kt, reachable from Settings → Vault → Manage Tags)
// are the only way spots get organized now.
