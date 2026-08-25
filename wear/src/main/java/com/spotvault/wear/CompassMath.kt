package com.spotvault.wear

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Deliberately mirrors app/src/main/java/com/spotvault/app/DistanceHelper.kt's own
 * haversineDistanceMeters/bearingDegrees/shortestAngleDelta/formatDistanceAway exactly — same
 * formulas, same behavior — so the watch's compass agrees with the phone's. Duplicated rather than
 * shared since :wear has no dependency on :app. */

fun haversineDistanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadiusM = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2.0) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusM * c
}

fun bearingDegrees(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): Double {
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

fun formatDistanceAway(meters: Double, unit: String): String {
    return when (unit) {
        "km" -> {
            val km = meters / 1000.0
            if (km < 0.1) {
                String.format(Locale.US, "%.1f km", 0.1)
            } else if (km >= 10.0) {
                String.format(Locale.US, "%.0f km", km)
            } else {
                String.format(Locale.US, "%.1f km", km)
            }
        }
        else -> {
            val miles = meters / 1609.344
            if (miles < 0.1) {
                String.format(Locale.US, "%.1f mi", 0.1)
            } else if (miles >= 10.0) {
                String.format(Locale.US, "%.0f mi", miles)
            } else {
                String.format(Locale.US, "%.1f mi", miles)
            }
        }
    }
}
