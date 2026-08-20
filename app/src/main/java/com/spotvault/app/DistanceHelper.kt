package com.spotvault.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

const val DISTANCE_UNIT_PREF = "distance_unit"
const val DEFAULT_DISTANCE_UNIT = "miles"

fun loadDistanceUnitFromPrefs(prefs: SharedPreferences): String {
    return when (prefs.getString(DISTANCE_UNIT_PREF, DEFAULT_DISTANCE_UNIT)) {
        "km" -> "km"
        else -> "miles"
    }
}

/** Earth-radius Haversine distance in meters — pure math, no network. */
fun haversineDistanceMeters(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double
): Double {
    val earthRadiusM = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2.0) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusM * c
}

fun formatDistanceAway(meters: Double, unit: String): String {
    return when (unit) {
        "km" -> {
            val km = meters / 1000.0
            if (km < 0.1) {
                String.format(Locale.US, "%.1f km away", 0.1)
            } else if (km >= 10.0) {
                String.format(Locale.US, "%.0f km away", km)
            } else {
                String.format(Locale.US, "%.1f km away", km)
            }
        }
        else -> {
            val miles = meters / 1609.344
            if (miles < 0.1) {
                String.format(Locale.US, "%.1f mi away", 0.1)
            } else if (miles >= 10.0) {
                String.format(Locale.US, "%.0f mi away", miles)
            } else {
                String.format(Locale.US, "%.1f mi away", miles)
            }
        }
    }
}

fun formatSpotDistanceLabel(
    userLat: Double,
    userLng: Double,
    spotLat: Double,
    spotLng: Double,
    unit: String
): String {
    val meters = haversineDistanceMeters(userLat, userLng, spotLat, spotLng)
    return formatDistanceAway(meters, unit)
}

/** Initial bearing from one coordinate to another, in degrees [0, 360). */
fun bearingDegrees(
    fromLat: Double,
    fromLng: Double,
    toLat: Double,
    toLng: Double
): Double {
    val lat1 = Math.toRadians(fromLat)
    val lat2 = Math.toRadians(toLat)
    val dLng = Math.toRadians(toLng - fromLng)
    val y = sin(dLng) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
    val degrees = Math.toDegrees(atan2(y, x))
    return ((degrees % 360.0) + 360.0) % 360.0
}

/** Shortest signed delta between two compass headings in degrees. */
fun shortestAngleDelta(fromDegrees: Float, toDegrees: Float): Float {
    var delta = (toDegrees - fromDegrees) % 360f
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f
    return delta
}

private val COMPASS_DIRECTIONS = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

fun formatBearingLabel(degrees: Double): String {
    val normalized = ((degrees % 360.0) + 360.0) % 360.0
    val index = ((normalized + 22.5) / 45.0).toInt() % 8
    return "${normalized.toInt()}° ${COMPASS_DIRECTIONS[index]}"
}

/** Large, navigation-focused distance readout (ft/mi or m/km). */
fun formatCompassDistance(meters: Double, unit: String): String {
    return when (unit) {
        "km" -> {
            val km = meters / 1000.0
            when {
                km < 1.0 -> String.format(Locale.US, "%.0f m", meters)
                km >= 10.0 -> String.format(Locale.US, "%.0f km", km)
                else -> String.format(Locale.US, "%.2f km", km)
            }
        }
        else -> {
            val feet = meters * 3.28084
            val miles = meters / 1609.344
            when {
                miles < 0.10 -> String.format(Locale.US, "%.0f ft", feet)
                miles >= 10.0 -> String.format(Locale.US, "%.0f mi", miles)
                else -> String.format(Locale.US, "%.2f mi", miles)
            }
        }
    }
}

@SuppressLint("MissingPermission")
suspend fun fetchLastKnownLocation(context: Context): Pair<Double, Double>? {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!fineGranted && !coarseGranted) return null

    return try {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        fused.lastLocation.await()?.let { it.latitude to it.longitude }
    } catch (_: Exception) {
        null
    }
}

/** Like [fetchLastKnownLocation], but returns null if the cached fix is older than
 * [maxAgeMillis] — for callers where showing a distance computed from a stale location would
 * actively mislead (e.g. the Premium widget's "X away" row), unlike [fetchLastKnownLocation]'s
 * other callers, which use it purely as a last-resort save fallback where even a somewhat-stale
 * location beats failing the save entirely. */
@SuppressLint("MissingPermission")
suspend fun fetchLastKnownLocationIfFresh(context: Context, maxAgeMillis: Long): Pair<Double, Double>? {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!fineGranted && !coarseGranted) return null

    return try {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        val location = fused.lastLocation.await() ?: return null
        val ageMs = System.currentTimeMillis() - location.time
        if (ageMs in 0..maxAgeMillis) location.latitude to location.longitude else null
    } catch (_: Exception) {
        null
    }
}

@Composable
fun rememberUserLocationForDistance(): Pair<Double, Double>? {
    val context = LocalContext.current
    var location by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    // Was a single one-shot fetch — every "X away" label built on this (called once per Vault
    // screen, not per row, so this cost is cheap) froze at whatever location was current the
    // moment the screen first composed and never updated again, silently drifting from reality
    // the longer the screen stayed open/backgrounded while the user actually moved. Re-reading
    // periodically (not a continuous requestLocationUpdates subscription — this is just the OS's
    // already-cached last-known fix, essentially free) keeps it reasonably fresh for as long as
    // the screen stays open without the battery cost of live tracking a "how far away" label
    // never needed in the first place.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            location = fetchLastKnownLocation(context) ?: location
            kotlinx.coroutines.delay(60_000L)
        }
    }

    return location
}

@Composable
fun rememberDistanceUnit(prefs: SharedPreferences): String {
    var unit by remember { mutableStateOf(loadDistanceUnitFromPrefs(prefs)) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == DISTANCE_UNIT_PREF) {
                unit = loadDistanceUnitFromPrefs(sharedPreferences)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    return unit
}

@Composable
fun SpotDistanceLabel(
    spotLat: Double,
    spotLng: Double,
    prefs: SharedPreferences,
    modifier: Modifier = Modifier
) {
    SpotDistanceLabel(
        spotLat = spotLat,
        spotLng = spotLng,
        prefs = prefs,
        modifier = modifier,
        userLocation = rememberUserLocationForDistance(),
        distanceUnit = rememberDistanceUnit(prefs)
    )
}

@Composable
fun SpotDistanceLabel(
    spotLat: Double,
    spotLng: Double,
    prefs: SharedPreferences,
    userLocation: Pair<Double, Double>?,
    distanceUnit: String,
    modifier: Modifier = Modifier
) {
    if (spotLat == 0.0 && spotLng == 0.0) return

    val label = remember(userLocation, distanceUnit, spotLat, spotLng) {
        userLocation?.let { (userLat, userLng) ->
            formatSpotDistanceLabel(userLat, userLng, spotLat, spotLng, distanceUnit)
        }
    }

    if (label != null) {
        Text(
            text = label,
            modifier = modifier,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = SpotVaultColors.Teal.copy(alpha = 0.88f),
            fontSize = 12.sp
        )
    }
}
