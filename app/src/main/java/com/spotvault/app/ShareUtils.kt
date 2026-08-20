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
 * preview on Android. Keyed to the resolved street address when there is one — Maps shows a
 * proper named/titled pin for an address query, where a bare coordinate query just shows an
 * unlabeled marker — and falls back to the exact saved coordinates otherwise (a manual pin with
 * no geocoded address, or a still-loading/unknown one; see [isResolvedShareAddress]). */
fun buildAppleMapsLink(lat: Double, lng: Double, address: String = ""): String {
    if (isResolvedShareAddress(address)) {
        return "https://maps.apple.com/?q=${android.net.Uri.encode(address.trim())}"
    }
    val formattedLat = String.format(Locale.US, "%.6f", lat)
    val formattedLng = String.format(Locale.US, "%.6f", lng)
    return "https://maps.apple.com/?ll=$formattedLat,$formattedLng&q=$formattedLat,$formattedLng"
}

/** Opens natively in the Google Maps app on Android; falls back to a Google Maps web view on
 * iPhone. Same address-first, coordinate-fallback behavior as [buildAppleMapsLink]. */
fun buildGoogleMapsLink(lat: Double, lng: Double, address: String = ""): String {
    if (isResolvedShareAddress(address)) {
        return "https://www.google.com/maps/search/?api=1&query=${android.net.Uri.encode(address.trim())}"
    }
    val formattedLat = String.format(Locale.US, "%.6f", lat)
    val formattedLng = String.format(Locale.US, "%.6f", lng)
    return "https://www.google.com/maps/search/?api=1&query=$formattedLat,$formattedLng"
}

/** Clean "digital business card" format: title line, then an address line — but only when there
 * actually is one *and* it says something the title line didn't already say (an untitled spot
 * whose title fell back to its own address would otherwise print that address twice in a row) —
 * then a blank line, then both map links. Deliberately excludes notes: those routinely carry
 * things that only make sense inside the app (e.g. "Shared link: <the original shortlink>" for a
 * spot added via a Maps share), which just cluttered the message with a redundant link alongside
 * the two this function already builds.
 *
 * The map links themselves are always coordinate-based here (no address passed to
 * [buildGoogleMapsLink]/[buildAppleMapsLink]), even though the address is already right there —
 * a URL-encoded street address turns those links into a wall of %20/%2C escapes that reads as
 * spammy in a text message, whereas raw coordinates stay short and clean. The readable address
 * is already covered by the address line above; the links just need to open the right pin. */
fun buildShareText(lat: Double, lng: Double, address: String, title: String = ""): String {
    val hasAddress = isResolvedShareAddress(address)
    val addressLine = if (hasAddress) address.trim() else ""
    val coordinateLine = String.format(Locale.US, "%.5f, %.5f", lat, lng)
    val titleLine = title.trim().ifBlank { addressLine.ifBlank { coordinateLine } }
    val showAddressLine = addressLine.isNotEmpty() && !addressLine.equals(titleLine, ignoreCase = true)

    return buildString {
        append(titleLine)
        if (showAddressLine) {
            append("\n")
            append(addressLine)
        }
        append("\n\n🤖 Android: ")
        append(buildGoogleMapsLink(lat, lng))
        append("\n🍎 iPhone: ")
        append(buildAppleMapsLink(lat, lng))
    }
}
