package com.spotvault.app

import android.content.SharedPreferences

const val SMART_DEDUPLICATION_ENABLED_PREF = "smart_deduplication_enabled"
const val DEDUPLICATION_WINDOW_HOURS_PREF = "deduplication_window_hours"
const val DEFAULT_SMART_DEDUPLICATION_ENABLED = true
const val DEFAULT_DEDUPLICATION_WINDOW_HOURS = 24
const val MIN_DEDUPLICATION_WINDOW_HOURS = 6
const val MAX_DEDUPLICATION_WINDOW_HOURS = 72

const val DEDUPLICATION_RADIUS_METERS_PREF = "deduplication_radius_meters"
const val DEFAULT_DEDUPLICATION_RADIUS_METERS = 25
// Two independent fixes taken at the same real spot can land up to ~20m apart at this app's own
// 10m "acceptable accuracy" bar (ACCEPTABLE_ACCURACY_METERS in TacticalQuickPins.kt), so anything
// tighter than this floor would start missing legitimate repeat saves. The ceiling is where the
// radius would start reaching into "different parking space on the same block" territory instead
// of the same spot re-saved.
const val MIN_DEDUPLICATION_RADIUS_METERS = 10
const val MAX_DEDUPLICATION_RADIUS_METERS = 60
const val DEDUPLICATION_RADIUS_STEP_METERS = 5

fun isSmartDeduplicationEnabled(prefs: SharedPreferences): Boolean =
    prefs.getBoolean(SMART_DEDUPLICATION_ENABLED_PREF, DEFAULT_SMART_DEDUPLICATION_ENABLED)

fun loadDeduplicationWindowHours(prefs: SharedPreferences): Int =
    prefs.getInt(DEDUPLICATION_WINDOW_HOURS_PREF, DEFAULT_DEDUPLICATION_WINDOW_HOURS)
        .coerceIn(MIN_DEDUPLICATION_WINDOW_HOURS, MAX_DEDUPLICATION_WINDOW_HOURS)

fun loadDeduplicationRadiusMeters(prefs: SharedPreferences): Int =
    prefs.getInt(DEDUPLICATION_RADIUS_METERS_PREF, DEFAULT_DEDUPLICATION_RADIUS_METERS)
        .coerceIn(MIN_DEDUPLICATION_RADIUS_METERS, MAX_DEDUPLICATION_RADIUS_METERS)

const val DEDUPLICATION_RADIUS_UNIT_FEET_PREF = "deduplication_radius_unit_feet"

/** Meters-vs-feet display toggle for the merge-distance slider specifically — independent of the
 * app-wide Distance Units setting (miles/km, for long-distance "X away" labels). The slider's
 * stored value and step size always stay in meters either way; this only changes how that value
 * is displayed. Defaults to feet for anyone on the app's default "miles" distance unit and meters
 * for "km", so it starts in whichever system the user already seems to prefer. */
fun loadDeduplicationRadiusUsesFeet(prefs: SharedPreferences): Boolean =
    prefs.getBoolean(DEDUPLICATION_RADIUS_UNIT_FEET_PREF, loadDistanceUnitFromPrefs(prefs) != "km")

fun metersToFeet(meters: Int): Int = Math.round(meters * 3.28084f)

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
    val radiusMeters = loadDeduplicationRadiusMeters(prefs)
    val cutoff = System.currentTimeMillis() - windowHours * 60 * 60 * 1000L
    val bestId = dao.getActiveSpotCoordsSince(cutoff)
        .filter { it.lat != 0.0 || it.lng != 0.0 }
        .map { it to haversineDistanceMeters(it.lat, it.lng, lat, lng) }
        .filter { (_, distance) -> distance <= radiusMeters }
        .minByOrNull { (_, distance) -> distance }
        ?.first
        ?.id
        ?: return null
    // Full row only for the winner — distance scan must not load every notepad in the window.
    return dao.getSpotById(bestId)
}

/**
 * Folds a new save into an existing spot instead of inserting a duplicate row. Always bumps the
 * timestamp (that's the point — "still current as of now"); every other field only fills in if
 * the target's existing value is blank, so a repeat Quick Pin can never clobber a richer manual
 * save's real photo, notes, or title with a blank/generic placeholder. Coordinates are never
 * touched, so the merged pin stays anchored to its original, presumably-most-accurate fix.
 *
 * A freshly taken [newImagePath] that loses out to an existing primary photo is never simply
 * thrown away — it's attached as an extra photo on the merged spot (via [spotPhotoDao]) instead,
 * so the picture the user just took is still there in the Vault gallery rather than silently
 * vanishing (and leaking its file on disk with nothing left pointing at it).
 */
suspend fun mergeDeduplicatedSpot(
    dao: LocationDao,
    spotPhotoDao: SpotPhotoDao,
    target: LocationSpot,
    newTimestamp: Long,
    newImagePath: String = "",
    newLocationDetails: String = "",
    newTitle: String = "",
    newVehicleId: Int? = null
): LocationSpot {
    val newPhotoBecomesPrimary = target.imagePath.isBlank() && newImagePath.isNotBlank()
    val merged = target.copy(
        timestamp = newTimestamp,
        imagePath = if (newPhotoBecomesPrimary) newImagePath else target.imagePath,
        locationDetails = if (target.locationDetails.isBlank() && newLocationDetails.isNotBlank()) newLocationDetails else target.locationDetails,
        title = if (target.title.isBlank() && newTitle.isNotBlank()) newTitle else target.title,
        vehicleId = target.vehicleId ?: newVehicleId
    )
    dao.updateSpot(merged)
    if (!newPhotoBecomesPrimary && newImagePath.isNotBlank()) {
        spotPhotoDao.insertExtraPhotoCapped(target.id, newImagePath)
    }
    return merged
}
