package com.spotvault.app

import android.content.SharedPreferences

const val SMART_DEDUPLICATION_ENABLED_PREF = "smart_deduplication_enabled"
const val DEDUPLICATION_WINDOW_HOURS_PREF = "deduplication_window_hours"
const val DEFAULT_SMART_DEDUPLICATION_ENABLED = true
const val DEFAULT_DEDUPLICATION_WINDOW_HOURS = 24
const val MIN_DEDUPLICATION_WINDOW_HOURS = 6
const val MAX_DEDUPLICATION_WINDOW_HOURS = 72

// Two independent fixes taken at the same real spot can land up to ~20m apart at this app's
// own 10m "acceptable accuracy" bar (ACCEPTABLE_ACCURACY_METERS in TacticalQuickPins.kt), so a
// 15m radius would miss legitimate repeat saves. 25m comfortably covers that spread without
// reaching into "different parking space on the same block" territory.
const val DEDUPLICATION_MATCH_RADIUS_METERS = 25.0

fun isSmartDeduplicationEnabled(prefs: SharedPreferences): Boolean =
    prefs.getBoolean(SMART_DEDUPLICATION_ENABLED_PREF, DEFAULT_SMART_DEDUPLICATION_ENABLED)

fun loadDeduplicationWindowHours(prefs: SharedPreferences): Int =
    prefs.getInt(DEDUPLICATION_WINDOW_HOURS_PREF, DEFAULT_DEDUPLICATION_WINDOW_HOURS)
        .coerceIn(MIN_DEDUPLICATION_WINDOW_HOURS, MAX_DEDUPLICATION_WINDOW_HOURS)

/**
 * Finds the closest recent spot this new save should merge into, or null if none qualifies.
 * Never touches lat/lng itself — callers merge into the existing coordinates, not the new ones,
 * so repeated saves at a slightly drifting GPS fix can't walk the pin away from its true spot.
 */
suspend fun findDeduplicationMergeTarget(
    dao: LocationDao,
    prefs: SharedPreferences,
    lat: Double,
    lng: Double
): LocationSpot? {
    if (!isSmartDeduplicationEnabled(prefs)) return null
    if (lat == 0.0 && lng == 0.0) return null
    val windowHours = loadDeduplicationWindowHours(prefs)
    val cutoff = System.currentTimeMillis() - windowHours * 60 * 60 * 1000L
    return dao.getActiveSpotsSince(cutoff)
        .filter { it.lat != 0.0 || it.lng != 0.0 }
        .map { it to haversineDistanceMeters(it.lat, it.lng, lat, lng) }
        .filter { (_, distance) -> distance <= DEDUPLICATION_MATCH_RADIUS_METERS }
        .minByOrNull { (_, distance) -> distance }
        ?.first
}

/**
 * Folds a new save into an existing spot instead of inserting a duplicate row. Always bumps the
 * timestamp (that's the point — "still current as of now"); every other field only fills in if
 * the target's existing value is blank, so a repeat Quick Pin can never clobber a richer manual
 * save's real photo, notes, or title with a blank/generic placeholder. Coordinates are never
 * touched, so the merged pin stays anchored to its original, presumably-most-accurate fix.
 */
suspend fun mergeDeduplicatedSpot(
    dao: LocationDao,
    target: LocationSpot,
    newTimestamp: Long,
    newImagePath: String = "",
    newLocationDetails: String = "",
    newTitle: String = "",
    newVehicleId: Int? = null
): LocationSpot {
    val merged = target.copy(
        timestamp = newTimestamp,
        imagePath = if (target.imagePath.isBlank() && newImagePath.isNotBlank()) newImagePath else target.imagePath,
        locationDetails = if (target.locationDetails.isBlank() && newLocationDetails.isNotBlank()) newLocationDetails else target.locationDetails,
        title = if (target.title.isBlank() && newTitle.isNotBlank()) newTitle else target.title,
        vehicleId = target.vehicleId ?: newVehicleId
    )
    dao.updateSpot(merged)
    return merged
}
