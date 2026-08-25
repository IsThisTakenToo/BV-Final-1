package com.spotvault.app

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

/**
 * Parses imported GPX waypoints into the SpotVault database schema. Complements — does not
 * replace — ZIP backup.
 */
object GpxParser {

    /** Hard cap so a huge Garmin/Gaia export cannot OOM mid-parse or flood the vault. */
    const val MAX_IMPORT_WAYPOINTS = 1000
    /** Whole-file ceiling — XmlPullParser buffers each text node fully, so a giant &lt;desc&gt;
     * still OOMs before [readText]'s char cap can help. */
    private const val MAX_GPX_FILE_BYTES = 8L * 1024 * 1024

    data class ImportResult(
        val imported: Int,
        val truncated: Boolean,
        /** True if the file's XML structure broke down partway through (truncated download,
         * corrupted file, invalid encoding) — [imported] still reflects every waypoint that
         * parsed successfully *before* that point, not zero. Distinct from [truncated], which
         * means the well-formed file simply had more waypoints than [MAX_IMPORT_WAYPOINTS]. */
        val parseError: Boolean = false
    )

    private val ISO8601_PATTERNS = listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    )

    /** Parse GPX XML text into [LocationSpot] rows ready for [LocationDao.insertSpot]. */
    fun parseGpx(xml: String, maxWaypoints: Int = MAX_IMPORT_WAYPOINTS): List<LocationSpot> {
        if (xml.isBlank()) return emptyList()
        if (xml.length > MAX_GPX_FILE_BYTES) {
            throw java.io.IOException(
                "GPX file is larger than the ${MAX_GPX_FILE_BYTES / (1024 * 1024)} MB import limit"
            )
        }
        return parseGpxResult(xml.byteInputStream(Charsets.UTF_8), maxWaypoints).spots
    }

    /** Parse GPX from an [InputStream] using [XmlPullParser]. Stops once [maxWaypoints] is hit. */
    fun parseGpx(inputStream: InputStream, maxWaypoints: Int = MAX_IMPORT_WAYPOINTS): List<LocationSpot> =
        parseGpxResult(inputStream, maxWaypoints).spots

    private data class ParseResult(val spots: List<LocationSpot>, val truncated: Boolean, val parseError: Boolean = false)

    private fun parseGpxResult(inputStream: InputStream, maxWaypoints: Int): ParseResult {
        val limited = CountingInputStream(inputStream, MAX_GPX_FILE_BYTES)
        val parser = android.util.Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(limited, null)

        val waypoints = mutableListOf<LocationSpot>()
        var eventType = parser.eventType
        var truncated = false

        var lat: Double? = null
        var lng: Double? = null
        var name: String? = null
        var desc: String? = null
        var cmt: String? = null
        var type: String? = null
        var time: String? = null
        var inWaypoint = false

        fun flushWaypoint() {
            if (waypoints.size >= maxWaypoints) {
                truncated = true
                return
            }
            val wLat = lat ?: return
            val wLng = lng ?: return
            // toDoubleOrNull() only screens out non-numeric text — "1e400"-style overflow still
            // parses to a real (non-finite) Double, and nothing here previously stopped an
            // out-of-range lat/lon (a typo, or a deliberately crafted file) from being saved as a
            // real Vault spot. Same bar SharedMapsLink already applies to shared map links.
            if (!wLat.isFinite() || !wLng.isFinite() || wLat !in -90.0..90.0 || wLng !in -180.0..180.0) return
            waypoints.add(
                LocationSpot(
                    id = 0,
                    imagePath = "",
                    locationDetails = desc.orEmpty().take(NOTEPAD_MAX_CHARS),
                    timestamp = parseGpxTime(time),
                    lat = wLat,
                    lng = wLng,
                    address = cmt.orEmpty().take(SPOT_ADDRESS_MAX_CHARS),
                    isFavorite = false,
                    // <type> used to become the fallback category when <name> was blank; folded
                    // into the title fallback chain instead now that category is gone.
                    title = name.orEmpty().ifBlank { type.orEmpty() }.take(SPOT_TITLE_MAX_CHARS),
                    isWishlist = false,
                    isVisited = false
                )
            )
            if (waypoints.size >= maxWaypoints) truncated = true
        }

        fun resetWaypointFields() {
            lat = null
            lng = null
            name = null
            desc = null
            cmt = null
            type = null
            time = null
        }

        // A structurally broken document (truncated download, corrupted file, bad encoding) makes
        // XmlPullParser throw partway through rather than just handing back an odd value — that
        // used to propagate straight out of parseGpxResult and abort the entire import, discarding
        // every waypoint already parsed before the break, even from an otherwise-fine 900-waypoint
        // file with one corrupted byte near the end. Catching it here keeps whatever was already
        // successfully parsed instead of throwing all of it away; parseError tells the caller this
        // was a partial result, not a fully successful one, so it can say so rather than reporting
        // false success.
        var parseError = false
        try {
            parseLoop@ while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    // Deliberately excludes trkpt — a recorded GPS track log can contain thousands
                    // of points logged automatically every few seconds (a single hike or drive),
                    // and importing each as its own Vault spot would flood the vault with junk
                    // with no way to undo it. wpt/rtept are actual placed waypoints, not a raw
                    // track log.
                    XmlPullParser.START_TAG -> when (localName(parser)) {
                        "wpt", "rtept" -> {
                            if (waypoints.size >= maxWaypoints) {
                                truncated = true
                                break@parseLoop
                            }
                            inWaypoint = true
                            lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                            lng = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        }
                        "name" -> if (inWaypoint) name = readText(parser)
                        "desc" -> if (inWaypoint) desc = readText(parser)
                        "cmt" -> if (inWaypoint) cmt = readText(parser)
                        "type" -> if (inWaypoint) type = readText(parser)
                        "time" -> if (inWaypoint) time = readText(parser)
                    }
                    XmlPullParser.END_TAG -> when (localName(parser)) {
                        "wpt", "rtept" -> {
                            flushWaypoint()
                            resetWaypointFields()
                            inWaypoint = false
                            if (truncated) break@parseLoop
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Purely synchronous parsing loop (no suspend calls reachable inside it), so there is
            // no coroutine cancellation to special-case here — anything caught is a genuine parse
            // failure (malformed/truncated XML), not the coroutine being torn down.
            parseError = true
        }

        return ParseResult(waypoints, truncated, parseError)
    }

    /** Parse GPX from a SAF [Uri] and insert waypoints. Caps at [MAX_IMPORT_WAYPOINTS]. */
    suspend fun importSpotsFromUri(context: Context, dao: LocationDao, uri: Uri): ImportResult {
        val parsed = context.contentResolver.openInputStream(uri)?.use { stream ->
            parseGpxResult(stream, MAX_IMPORT_WAYPOINTS)
        } ?: throw IllegalStateException("Unable to open input stream for GPX import.")

        var imported = 0
        parsed.spots.forEach { spot ->
            dao.insertSpot(spot)
            imported++
        }
        return ImportResult(imported = imported, truncated = parsed.truncated, parseError = parsed.parseError)
    }

    private fun parseGpxTime(raw: String?): Long {
        if (raw.isNullOrBlank()) return System.currentTimeMillis()
        for (pattern in ISO8601_PATTERNS) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                return format.parse(raw.trim())?.time ?: continue
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return try {
            Instant.parse(raw.trim()).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun localName(parser: XmlPullParser): String {
        return parser.name?.substringAfter(':').orEmpty()
    }

    /** Reads the plain-text body of a &lt;name&gt;/&lt;desc&gt;/&lt;cmt&gt;/&lt;type&gt;/&lt;time&gt;
     * element and leaves the parser positioned at that same element's own END_TAG. GPX defines
     * these as plain text, but nothing stops a malformed or adversarial file from nesting another
     * element inside one instead (e.g. `&lt;desc&gt;x&lt;name&gt;evil&lt;/name&gt;&lt;/desc&gt;`) —
     * the old implementation assumed exactly one TEXT event followed immediately by its own
     * END_TAG, and calling `nextTag()` on anything else landed on the nested element's START_TAG,
     * desyncing the caller's flat `inWaypoint` state machine from the parser's real position.
     * Tracking [elementDepth] and only stopping at an END_TAG that matches it (the nested tag's
     * own END_TAG is one level deeper) correctly skips over any such subtree instead. */
    private fun readText(parser: XmlPullParser): String {
        val elementDepth = parser.depth
        var result = ""
        var captured = false
        var event = parser.next()
        while (true) {
            when (event) {
                XmlPullParser.TEXT -> if (!captured) {
                    // Copy at most NOTEPAD_MAX_CHARS from the parser's char buffer — avoids a
                    // second full-size String via parser.text. The file-level [CountingInputStream]
                    // is what prevents a multi-hundred-MB text node from being buffered at all.
                    val holder = IntArray(2)
                    val buf = parser.getTextCharacters(holder)
                    result = if (buf != null && holder[1] > 0) {
                        val start = holder[0]
                        val len = minOf(holder[1], NOTEPAD_MAX_CHARS)
                        String(buf, start, len)
                    } else {
                        parser.text.orEmpty().take(NOTEPAD_MAX_CHARS)
                    }
                    captured = true
                }
                XmlPullParser.END_TAG -> if (parser.depth == elementDepth) return result.trim()
                XmlPullParser.END_DOCUMENT -> return result.trim()
            }
            event = parser.next()
        }
    }

    /** Rejects GPX payloads larger than [maxBytes] as they are read — XmlPullParser has no
     * per-text-node size limit of its own. */
    private class CountingInputStream(
        private val wrapped: InputStream,
        private val maxBytes: Long
    ) : InputStream() {
        private var total = 0L

        private fun account(n: Int): Int {
            if (n <= 0) return n
            total += n
            if (total > maxBytes) {
                throw java.io.IOException(
                    "GPX file is larger than the ${maxBytes / (1024 * 1024)} MB import limit"
                )
            }
            return n
        }

        override fun read(): Int {
            val b = wrapped.read()
            if (b >= 0) account(1)
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int =
            account(wrapped.read(b, off, len))

        override fun close() = wrapped.close()
    }

}
