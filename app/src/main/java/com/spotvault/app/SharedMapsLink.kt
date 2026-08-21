package com.spotvault.app

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

/** Everything the "+ Add Spot" form needs once shared location text has been parsed and (if
 * possible) resolved to real coordinates. [lat]/[lng] are null when no coordinates could be
 * pulled from the shared text at all — the form falls back to requiring a typed address in that
 * case, same as if the user had opened it directly. [city]/[state] ride along with [addressHint]
 * from the same geocode call rather than being re-derived later — the saved spot's city/state
 * fields (used for Browse by Location, search, etc. same as any other spot) would otherwise stay
 * blank forever for anything added this way, since nothing else in the Add Spot form re-geocodes
 * a location that already came in pre-resolved. */
data class SharedSpotPayload(
    val title: String,
    val notesText: String,
    val lat: Double?,
    val lng: Double?,
    val addressHint: String,
    val city: String = "",
    val state: String = "",
    // Only ever non-blank when this payload is the result of AddFavoriteSpotDialog's own
    // "Search in Maps" round trip and the user had already attached a photo before tapping it —
    // see PendingFavoritePhoto below for how it gets here.
    val photoPath: String = ""
)

/** One-shot (time-bounded) hand-off for a photo the user already attached in Add Favorite Spot
 * *before* tapping "Search in Maps" — that button closes the form entirely (see
 * AddFavoriteSpotDialog's dismissAndCleanUp/dismissForMapsSearch), so without this the photo
 * would either get deleted as "abandoned" on the way out, or simply not exist on the fresh
 * dialog instance that reopens once the share comes back in. Time-bounded rather than a plain
 * one-shot flag: an unrelated share arriving much later in the same app session — the ordinary
 * "share a place straight from Google Maps" flow, not this round trip — should never pick up a
 * photo left over from a search the user gave up on ages ago. */
object PendingFavoritePhoto {
    private const val MAX_AGE_MILLIS = 5 * 60 * 1000L
    private var path: String? = null
    private var stashedAt: Long = 0L

    fun stash(photoPath: String) {
        path = photoPath.takeIf { it.isNotBlank() }
        stashedAt = System.currentTimeMillis()
    }

    /** Returns the stashed path if it's still fresh enough to plausibly be from an in-progress
     * search; otherwise cleans up the now-orphaned file itself and returns null either way, this
     * only ever pays out once. */
    fun consume(): String? {
        val result = path ?: return null
        path = null
        val age = System.currentTimeMillis() - stashedAt
        if (age in 0..MAX_AGE_MILLIS) return result
        runCatching { java.io.File(result).delete() }
        return null
    }

    /** Drop a stashed photo that was never consumed (user abandoned Search-in-Maps). Safe to call
     * on every app start — only deletes when the stash is past [MAX_AGE_MILLIS]. */
    fun purgeIfStale() {
        val result = path ?: return
        val age = System.currentTimeMillis() - stashedAt
        if (age in 0..MAX_AGE_MILLIS) return
        path = null
        runCatching { java.io.File(result).delete() }
    }
}

/** One-shot hand-off from MainActivity's Share-sheet intent handling (which runs before the Vault
 * screen, or even the Compose tree, necessarily exists yet) into FavoritesHubDialog, which is
 * nested several composables deep inside the Vault tab. A plain prop wouldn't reach that far
 * without threading a "pending shared spot" parameter through MainActivity's setContent,
 * SpotVaultMainScaffold, and SpotVaultNavHost — three composables with no other reason to know
 * this feature exists. [version] is the only actual Compose state; [deliver]/[consume] mirror the
 * plain-field-plus-version-counter pattern MainActivity already uses for widget deep-links
 * (pendingWidgetAction/widgetIntentVersion), just factored out since this needs to be read from a
 * composable MainActivity doesn't directly own. */
object PendingSharedSpot {
    private var payload: SharedSpotPayload? = null
    val version = mutableStateOf(0)

    fun deliver(spot: SharedSpotPayload) {
        payload = spot
        version.value++
    }

    /** Returns the pending payload and clears it — called exactly once, by whichever composable
     * is watching [version] when it changes (today, HistoryDialogContent). */
    fun consume(): SharedSpotPayload? {
        val result = payload
        payload = null
        return result
    }
}

// Short timeouts — this only ever needs to read a redirect's Location header, never a response
// body, so it should fail fast on a bad connection rather than hang the share-intent handling.
// 5s (the original value) turned out tight enough that a weak signal at the moment of sharing
// could time out before the redirect resolved — silently degrading to "no coordinates, no title
// found" (see handleSharedMapsText) even though the share itself was fine. 10s is still well
// short of feeling hung, with more room for a slow mobile-data round trip to actually finish.
private val shareLinkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
}

/** Result of splitting a share's raw text apart. [addressLine] is always populated whenever there
 * was any non-URL text at all — even a single-line share ("Waffle House" alone, with the URL
 * elsewhere in the string) puts that same text in both [name] and [addressLine], since it's worth
 * a geocode attempt either way (see [handleSharedMapsText]). */
private data class ParsedShareText(val name: String, val url: String?, val addressLine: String)

/** Splits a share's text into a name, a URL, and a search-worthy address string. Finds the URL
 * anywhere in the text and removes exactly that substring, rather than classifying whole lines as
 * "the URL line" — a share where the title and URL land on the *same* line with no newline
 * between them (plausible depending on exactly how a given map app formats EXTRA_TEXT) made a
 * line-classifying parser discard the entire line as "just a URL," losing the title text embedded
 * in it. Removing the URL substring directly is robust to that regardless of whether the rest of
 * the text is one line or several. Not every app includes a name — sharing a raw dropped pin is
 * often just the link — so [ParsedShareText.name] can come back blank; the caller falls back to a
 * generic title in that case. [url] is null only when there's no URL in the text at all *and* it
 * doesn't look like a bare "lat, lng" pair either (handled by the caller instead, since that case
 * has no URL to carry forward). */
private fun parseSharedMapsText(text: String): ParsedShareText {
    val urlMatch = Regex("""https?://\S+""").find(text)
        ?: return ParsedShareText(name = text.trim(), url = null, addressLine = "")
    // Trailing punctuation from whatever formatted the message this was copied out of
    // ("...goo.gl/xyz." at the end of a sentence) — not part of the URL itself.
    val url = urlMatch.value.trimEnd('.', ',', ')', ']', '>')

    val leftoverLines = text.replace(urlMatch.value, "")
        .split("\n")
        .map { it.trim().trim('-', '—', ':').trim() }
        .filter { it.isNotBlank() }

    return when {
        leftoverLines.isEmpty() -> ParsedShareText(name = "", url = url, addressLine = "")
        leftoverLines.size == 1 -> ParsedShareText(name = leftoverLines[0], url = url, addressLine = leftoverLines[0])
        else -> ParsedShareText(name = leftoverLines[0], url = url, addressLine = leftoverLines.drop(1).joinToString(", "))
    }
}

/** A handful of apps (mostly older/simpler map tools, or a location copy-pasted by hand) share
 * bare coordinates with no URL at all — "37.7749, -122.4194". Deliberately anchored to the whole
 * trimmed text rather than searching for a coordinate-shaped substring anywhere in it: this share
 * target shows up for *any* app's plain-text share, not just mapping apps, and a loose search
 * could false-positive on two comma-separated numbers in something completely unrelated (a price,
 * a phone number). Requiring the entire share to be just a coordinate pair keeps this to
 * genuinely coordinate-only shares. */
private fun extractBareLatLng(text: String): Pair<Double, Double>? {
    val match = Regex("""^\(?\s*(-?\d{1,3}\.\d+)\s*,\s*(-?\d{1,3}\.\d+)\s*\)?$""").find(text.trim()) ?: return null
    val lat = match.groupValues[1].toDoubleOrNull() ?: return null
    val lng = match.groupValues[2].toDoubleOrNull() ?: return null
    return lat to lng
}

/** Pulls coordinates straight out of a URL's own text — Google Maps' "@lat,lng,zoom" in its
 * full-length place links, Waze's "ll=lat,lng" (URL-encoded as "ll=lat%2Clng" on the wire, hence
 * decoding below), Apple Maps' "ll=lat,lng", or a bare "geo:lat,lng" URI. Reading these directly
 * is exact and free, unlike geocoding a place name back into coordinates, which would introduce
 * error and doesn't need to exist at all when the link already has the real number. Deliberately
 * not tied to any one mapping service's URL shape — new patterns can be added here without
 * touching anything else in this file. */
private fun extractLatLngFromMapsUrl(url: String): Pair<Double, Double>? {
    // Query-string values arrive percent-encoded ("%2C" for the comma between lat and lng) —
    // decoding first means every pattern below only ever has to match the plain, human-readable
    // form once, instead of every pattern needing its own encoded/unencoded variant.
    val decoded = runCatching { URLDecoder.decode(url, "UTF-8") }.getOrDefault(url)

    val patterns = listOf(
        Regex("""@(-?\d{1,3}\.\d+),(-?\d{1,3}\.\d+)"""),
        Regex("""[?&](?:q|ll|daddr|destination)=(-?\d{1,3}\.\d+),(-?\d{1,3}\.\d+)"""),
        Regex("""geo:(-?\d{1,3}\.\d+),(-?\d{1,3}\.\d+)""")
    )
    for (pattern in patterns) {
        pattern.find(decoded)?.let { m ->
            val lat = m.groupValues[1].toDoubleOrNull()
            val lng = m.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null &&
                lat in -90.0..90.0 && lng in -180.0..180.0
            ) {
                return lat to lng
            }
        }
    }
    return null
}

/** Most mapping apps' Share button hands over a shortened link (Google's "maps.app.goo.gl",
 * Waze's "waze.com/ul/...", etc.), not the long URL that actually has coordinates in it — those
 * only show up after following the redirect. Deliberately not limited to a specific allowlist of
 * known short-link hosts: any http(s) URL where a direct read found nothing is worth one redirect
 * hop, since that's cheap and covers services this app has no specific knowledge of (this is the
 * "Waze or whatever" case). Returns the original URL unchanged — never throws, never hangs longer
 * than the client's 5s timeout — if it's not http(s), the request fails, or it times out; any of
 * those just means the caller finds no coordinates on the result either, same as a share that
 * never had any to begin with.
 *
 * Uses GET, not HEAD — maps.app.goo.gl is Firebase Dynamic Links infrastructure, which resolves
 * its redirect target through server-side logic that inspects the request (User-Agent among other
 * things) to decide what to hand back; a bare HEAD request to that kind of endpoint isn't
 * guaranteed to trigger the same redirect a real client following the link would get, and silently
 * returning the *original* short URL unchanged reads identically to "no coordinates found" further
 * down the pipeline. A real User-Agent header for the same reason — an obviously non-browser
 * client is exactly what this kind of endpoint's request-inspection logic exists to treat
 * differently. The response body is never read (OkHttp discards it unread when the response is
 * closed), so this costs one redirect hop's worth of bandwidth either way. */
private suspend fun resolveRedirect(url: String): String {
    if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) return url
    return withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .get()
                .build()
            shareLinkHttpClient.newCall(request).execute().use { response -> response.request.url.toString() }
        }.getOrDefault(url)
    }
}

/** Pulls a readable place name straight out of a resolved Google Maps URL's own path —
 * ".../maps/place/Waffle+House/@lat,lng,..." or ".../maps/dir/Waffle+House/..." — for the case
 * (this is the common one for a maps.app.goo.gl business share) where EXTRA_TEXT carries nothing
 * but the shortlink itself, with no title text anywhere in the shared string at all. Same
 * URLDecoder.decode()-on-the-whole-URL approach as extractLatLngFromMapsUrl for the same reason:
 * Maps' own URLs use "+" for spaces even inside the path, which isn't standard URL-path encoding
 * but is what's actually there in practice. */
private fun extractTitleFromMapsUrl(url: String): String? {
    val decoded = runCatching { URLDecoder.decode(url, "UTF-8") }.getOrDefault(url)
    val name = Regex("""/(?:place|dir)/([^/@?]+)""").find(decoded)?.groupValues?.get(1)?.trim()
    if (name.isNullOrBlank()) return null
    // A raw dropped-pin share resolves to ".../place/37.7749,-122.4194/..." — coordinates
    // standing in for a name there aren't a real place name worth showing as a title.
    if (Regex("""^-?\d{1,3}\.\d+,-?\d{1,3}\.\d+$""").matches(name)) return null
    return name
}

/** Full pipeline from a Share-sheet ACTION_SEND text payload to a ready-to-prefill
 * [SharedSpotPayload], handed to [PendingSharedSpot] for FavoritesHubDialog to pick up. Deliberately
 * catches everything — a malformed share, an unreachable host, a redirect to something that isn't
 * even a URL, or any other surprise from an app this was never specifically tested against should
 * degrade to "the Add Spot form opens with nothing pre-filled" rather than crash the app the
 * instant someone shares a location into it. */
suspend fun handleSharedMapsText(context: Context, sharedText: String) {
    runCatching {
        // Callers (MainActivity's ingestSharedMapsIntent) launch this on the Main dispatcher, same
        // as the rest of that intent-handling code — reverseGeocodeAddress's pre-Android-13
        // fallback path makes a genuinely blocking geocoder call with no dispatch of its own, so
        // without this it would freeze the UI thread on every device below API 33.
        withContext(Dispatchers.IO) {
            // Bound pathological EXTRA_TEXT before any regex/URL parse work.
            val boundedShared = sharedText.take(8_192)
            val parsed = parseSharedMapsText(boundedShared)
            var rawName = parsed.name.take(512)
            val rawUrl = parsed.url?.take(2_048)

            var coordinates = rawUrl?.let { extractLatLngFromMapsUrl(it) }
            var resolvedUrl: String? = null
            if (coordinates == null && rawUrl != null) {
                resolvedUrl = resolveRedirect(rawUrl)
                coordinates = extractLatLngFromMapsUrl(resolvedUrl)
            }
            // Google Maps' shortlink shares frequently carry nothing but the link itself in
            // EXTRA_TEXT — no title, no address, nothing else at all. The resolved URL's own path
            // is the only remaining source for a real name in that case, so this only runs when
            // the shared text genuinely didn't have one already.
            if (rawName.isBlank() && resolvedUrl != null) {
                extractTitleFromMapsUrl(resolvedUrl)?.let { rawName = it }
            }
            // No URL at all in the share — the only other shape worth recognizing is the share
            // being nothing but a bare coordinate pair.
            if (coordinates == null && rawUrl == null) {
                coordinates = extractBareLatLng(boundedShared)
            }
            // The best available text to search on if the URL didn't already hand over exact
            // coordinates — the parsed address line when there was real leftover text, otherwise
            // whatever's in name (parseSharedMapsText sets these to the same string for a
            // single-line share, so this is effectively just "whichever one is non-blank").
            val searchText = parsed.addressLine.ifBlank { rawName }.trim().take(512)

            // Nothing usable found anywhere — rather than deliver an empty/near-empty payload
            // (which would still pop the Add Spot form open over whatever the user was doing),
            // leave it alone entirely; a share this app can't make sense of shouldn't interrupt
            // them for no benefit.
            if (rawUrl == null && coordinates == null && searchText.isBlank()) return@withContext

            val addressHint: String
            var city = ""
            var state = ""
            if (coordinates != null) {
                // Exact coordinates already in hand (from the URL) — reverse-geocode them into a
                // readable address rather than geocoding anything, so this can't drift from the
                // point Maps actually pointed to. City/state ride along from the same call rather
                // than being dropped — without them, a spot saved this way would show up in the
                // Vault with no city/state at all, unlike any other spot.
                val reverseGeocoded = runCatching { reverseGeocodeAddress(context, coordinates.first, coordinates.second) }.getOrNull()
                addressHint = prefsSafeAddress(reverseGeocoded?.full ?: searchText)
                city = reverseGeocoded?.city.orEmpty().take(SPOT_CITY_STATE_MAX_CHARS)
                state = reverseGeocoded?.state.orEmpty().take(SPOT_CITY_STATE_MAX_CHARS)
            } else if (searchText.isNotBlank()) {
                // No coordinates in the URL — either it was a shortlink that never resolved to a
                // "@lat,lng"-bearing page (maps.app.goo.gl links commonly don't, and neither do a
                // lot of *resolved* business-listing URLs, which increasingly use a place-ID
                // reference instead of embedding coordinates directly), or there was no URL at
                // all. This always attempts the geocoder on whatever text is available regardless
                // of whether it looks like a full address or just a bare name — a failed attempt
                // costs nothing (the field is left exactly as it would've been anyway), and a
                // successful one is what actually unlocks Save immediately instead of leaving it
                // blocked on a form that looks like it should already have everything it needs.
                val geocoded = geocodeAddress(context, searchText)
                if (geocoded != null) {
                    coordinates = geocoded.lat to geocoded.lng
                    addressHint = prefsSafeAddress(geocoded.formattedAddress)
                    city = geocoded.city.take(SPOT_CITY_STATE_MAX_CHARS)
                    state = geocoded.state.take(SPOT_CITY_STATE_MAX_CHARS)
                } else {
                    addressHint = prefsSafeAddress(searchText)
                }
            } else {
                addressHint = ""
            }

            PendingSharedSpot.deliver(
                SharedSpotPayload(
                    title = prefsSafeTitle(rawName.ifBlank { "Shared Spot" }),
                    // Used to prefill Notes with "Shared link: <url>" — nothing in the app or the
                    // saved spot itself ever reads that back out, so it was just clutter sitting
                    // in the one field the user would actually want to type their own notes into.
                    notesText = "",
                    lat = coordinates?.first,
                    lng = coordinates?.second,
                    city = city,
                    state = state,
                    addressHint = addressHint,
                    photoPath = PendingFavoritePhoto.consume().orEmpty()
                )
            )
        }
    }.onFailure { e ->
        // CancellationException specifically rethrown, not logged as a "failure" — this runs on
        // lifecycleScope, so the Activity being torn down mid-resolve cancels it normally; letting
        // that propagate is what correctly participates in structured concurrency instead of
        // silently swallowing it and logging what looks like an error for an entirely expected,
        // harmless cancellation.
        if (e is kotlinx.coroutines.CancellationException) throw e
        Log.e("SharedMapsLink", "Failed to handle shared location text", e)
    }
}
