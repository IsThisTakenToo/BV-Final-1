package com.spotvault.app

import java.util.Locale

fun isResolvedShareAddress(address: String): Boolean {
    val trimmed = address.trim()
    return trimmed.isNotEmpty() &&
            !trimmed.equals("Loading address...", ignoreCase = true) &&
            !trimmed.equals("Unnamed Location", ignoreCase = true) &&
            !trimmed.equals("Unknown location", ignoreCase = true)
}

/** Opens natively in Apple Maps on iPhone (Universal Link); falls back to a working web map
 * preview on Android. Always keyed to the exact saved coordinates, never the geocoded address,
 * so the pin that opens is exactly where the spot was saved regardless of reverse-geocoding. */
fun buildAppleMapsLink(lat: Double, lng: Double): String {
    val formattedLat = String.format(Locale.US, "%.6f", lat)
    val formattedLng = String.format(Locale.US, "%.6f", lng)
    return "https://maps.apple.com/?ll=$formattedLat,$formattedLng&q=$formattedLat,$formattedLng"
}

/** Opens natively in the Google Maps app on Android; falls back to a Google Maps web view on
 * iPhone. Always keyed to the exact saved coordinates — see [buildAppleMapsLink]. */
fun buildGoogleMapsLink(lat: Double, lng: Double): String {
    val formattedLat = String.format(Locale.US, "%.6f", lat)
    val formattedLng = String.format(Locale.US, "%.6f", lng)
    return "https://www.google.com/maps/search/?api=1&query=$formattedLat,$formattedLng"
}

fun buildShareText(lat: Double, lng: Double, address: String, notes: String): String {
    val hasAddress = isResolvedShareAddress(address)
    val locationLine = if (hasAddress) {
        address.trim()
    } else {
        String.format(Locale.US, "%.5f, %.5f", lat, lng)
    }
    val trimmedNotes = notes.trim()

    return buildString {
        append(locationLine)
        if (trimmedNotes.isNotEmpty()) {
            append(" — ")
            append(trimmedNotes)
        }
        append("\niPhone: ")
        append(buildAppleMapsLink(lat, lng))
        append("\nAndroid: ")
        append(buildGoogleMapsLink(lat, lng))
    }
}
