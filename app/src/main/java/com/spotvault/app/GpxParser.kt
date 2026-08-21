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
        val truncated: Boolean
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

    private data class ParseResult(val spots: List<LocationSpot>, val truncated: Boolean)

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

        parseLoop@ while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                // Deliberately excludes trkpt — a recorded GPS track log can contain thousands of
                // points logged automatically every few seconds (a single hike or drive), and
                // importing each as its own Vault spot would flood the vault with junk with no
                // way to undo it. wpt/rtept are actual placed waypoints, not a raw track log.
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

        return ParseResult(waypoints, truncated)
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
        return ImportResult(imported = imported, truncated = parsed.truncated)
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

    private fun readText(parser: XmlPullParser): String {
        if (parser.next() != XmlPullParser.TEXT) return ""
        // Copy at most NOTEPAD_MAX_CHARS from the parser's char buffer — avoids a second
        // full-size String via parser.text. The file-level [CountingInputStream] is what
        // prevents a multi-hundred-MB text node from being buffered in the first place.
        val holder = IntArray(2)
        val buf = parser.getTextCharacters(holder)
        val result = if (buf != null && holder[1] > 0) {
            val start = holder[0]
            val len = minOf(holder[1], NOTEPAD_MAX_CHARS)
            String(buf, start, len)
        } else {
            parser.text.orEmpty().take(NOTEPAD_MAX_CHARS)
        }
        parser.nextTag()
        return result.trim()
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
