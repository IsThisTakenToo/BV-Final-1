package com.spotvault.app

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.JsonReader
import android.util.JsonToken
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object VaultBackupManager {
    private const val FORMAT = "beaconvault"
    private const val VERSION = 3
    private const val FOLDER_FORMAT_MIN_VERSION = 2
    private const val LEGACY_VERSION = 1

    // Generous ceiling for one decompressed entry (a photo or the manifest/spot JSON) — nothing
    // this app ever writes into a backup comes close. Guards against a corrupted or maliciously
    // crafted zip claiming a tiny compressed size but an enormous decompressed one (a "zip bomb"):
    // ZipInputStream.readBytes() has no size limit of its own and would otherwise happily grow a
    // buffer until the process OOMs — which, being an Error rather than an Exception, would not
    // even be caught by this file's own `catch (e: Exception)` around the whole import.
    private const val MAX_BACKUP_ENTRY_BYTES = 200L * 1024 * 1024
    /** Meta JSON/txt/html entries are far smaller than photos — keep them tightly capped. */
    private const val MAX_BACKUP_META_ENTRY_BYTES = 16L * 1024 * 1024
    /** Root prefs.json stays in RAM — keep this tight so a corrupt blob can't balloon. */
    private const val MAX_BACKUP_ROOT_META_BYTES = 512L * 1024
    /** Cap on base64-embedded thumbnails in index.html — beyond this, cards link to
     * spots/.../photo.jpg so a multi-year vault can't OOM the export StringBuilder. */
    private const val MAX_HTML_EMBEDDED_PHOTOS = 16
    private const val MAX_HTML_EMBEDDED_PHOTOS_LOW_RAM = 6
    /** Skip embedding source JPEGs larger than this — still zip the file; card uses relative link. */
    private const val MAX_HTML_EMBED_SOURCE_BYTES = 4L * 1024 * 1024
    /** Cap gallery cards in index.html — past this, folders in the zip are still complete. */
    private const val MAX_HTML_GALLERY_CARDS = 200
    private const val MAX_HTML_NOTES_CHARS = 200
    /** Older single-file spots.json backups above this size need a re-export from a modern build. */
    private const val MAX_LEGACY_SPOTS_JSON_BYTES = 4L * 1024 * 1024
    private const val MAX_IMPORT_TAGS_PER_SPOT = 40
    private const val MAX_IMPORT_TAG_NAME_CHARS = 40
    private const val MAX_IMPORT_TITLE_CHARS = 200
    private const val MAX_IMPORT_ADDRESS_CHARS = 400
    private const val MAX_IMPORT_CITY_STATE_CHARS = 80
    private const val MAX_IMPORT_VEHICLES = 80
    /** Hard ceiling for spots imported from one backup — bounds Room WAL on replace restores. */
    private const val MAX_IMPORT_SPOTS = 10_000
    /** Merge imports commit in chunks so a multi-year merge doesn't hold one giant write lock. */
    private const val IMPORT_SPOT_COMMIT_BATCH = 100
    /** Total decompressed payload across every zip entry — per-entry limit alone still allows
     * hundreds of large photos to OOM the process when all buffered into [extracted]. */
    internal const val MAX_BACKUP_TOTAL_BYTES = 512L * 1024 * 1024

    private fun hashEntryName(entryName: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(entryName.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(40)
    }

    private fun spilledMetaFile(metaSpillDir: File, entryName: String): File =
        File(metaSpillDir, "m_${hashEntryName(entryName)}.bin")

    private fun spilledPhotoFile(imagesDir: File, entryName: String, extension: String): File {
        val ext = extension.filter { it.isLetterOrDigit() }.lowercase(Locale.US).take(8).ifBlank { "jpg" }
        return File(imagesDir, "import_spill_${hashEntryName(entryName)}.$ext")
    }

    private fun findSpilledPhoto(imagesDir: File, entryName: String): String? {
        val hash = hashEntryName(entryName)
        for (ext in listOf("jpg", "jpeg", "png", "webp", "bin")) {
            val f = File(imagesDir, "import_spill_$hash.$ext")
            if (f.exists()) return f.absolutePath
        }
        return null
    }

    private fun findSpilledMeta(metaSpillDir: File, entryName: String): String? {
        val f = spilledMetaFile(metaSpillDir, entryName)
        return f.takeIf { it.exists() }?.absolutePath
    }
    private fun ZipInputStream.readEntryBytesLimited(entryName: String, maxBytes: Long): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) {
                throw java.io.IOException("Backup entry \"$entryName\" is larger than the $maxBytes byte limit")
            }
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    /** Stream a zip entry straight to disk — photos must not peak as full ByteArrays in RAM. */
    private fun ZipInputStream.spillEntryToFile(
        entryName: String,
        destination: File,
        maxEntryBytes: Long,
        onBytes: (Long) -> Unit
    ) {
        val buffer = ByteArray(64 * 1024)
        var written = 0L
        destination.outputStream().use { out ->
            while (true) {
                val read = read(buffer)
                if (read < 0) break
                written += read
                if (written > maxEntryBytes) {
                    runCatching { destination.delete() }
                    throw java.io.IOException(
                        "Backup entry \"$entryName\" is larger than the $maxEntryBytes byte limit"
                    )
                }
                onBytes(read.toLong())
                out.write(buffer, 0, read)
            }
        }
    }

    /** Drain an entry we do not need (notes.txt / gallery HTML) without buffering it. */
    private fun ZipInputStream.discardEntry(entryName: String, maxEntryBytes: Long, onBytes: (Long) -> Unit) {
        val buffer = ByteArray(64 * 1024)
        var written = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            written += read
            if (written > maxEntryBytes) {
                throw java.io.IOException(
                    "Backup entry \"$entryName\" is larger than the $maxEntryBytes byte limit"
                )
            }
            onBytes(read.toLong())
        }
    }

    /** Root-level JSON that must stay in RAM for the import transaction. */
    private fun isRootMetaEntry(name: String): Boolean {
        val base = name.substringAfterLast('/')
        // vehicles.json and spots.json are spilled — only prefs stays as a small RAM blob.
        return base == "prefs.json"
    }

    /** Human-readable companions never used by import — discard during unzip. */
    private fun isDiscardableMetaEntry(name: String): Boolean {
        val lower = name.lowercase(Locale.US)
        return lower.endsWith("notes.txt") ||
            lower.endsWith("index.html") ||
            lower == "readme.txt" ||
            lower.endsWith("/readme.txt")
    }

    private fun isSpotJsonEntry(name: String): Boolean =
        name.matches(Regex("spots/[^/]+/spot\\.json"))

    private val BACKUP_EXCLUDED_PREF_KEYS = setOf(
        "auto_backup_tree_uri", "auto_backup_last_success", "auto_backup_enabled", "auto_backup_last_fingerprint",
        "auto_backup_last_error",
        "drive_connected", "drive_account_email", "drive_last_backup_success", "drive_last_backup_fingerprint",
        "drive_last_backup_error",
        // The full set of an in-progress active-tracking session's prefs, not just is_pinned —
        // restoring a backup used to leave is_pinned alone (excluded) but still overwrite these
        // with whatever was captured in the backup, so importing/restoring while a session is
        // genuinely running on this device could silently swap its displayed photo/location/
        // address for stale data from the backup's snapshot instead of leaving the live session
        // untouched, exactly the corruption excluding is_pinned alone was meant to prevent.
        "is_pinned", "is_alarm_ringing", "timer_end_time",
        "photo_path", "lat", "lng", "location_details", "category", "current_address", PINNED_VEHICLE_ID_PREF,
        "app_lock_enabled",
        // An entitlement, not a preference — it has to be re-earned on whatever device/account
        // actually holds it, not something that travels along for free inside a shared or
        // imported backup file once real purchase-gating is wired up (see PremiumGating.kt).
        PREMIUM_UNLOCKED_PREF
    )

    private fun backupImagesDir(context: Context): File {
        val dir = File(context.filesDir, "vault_images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun exportBackup(
        context: Context,
        dao: LocationDao,
        vehicleDao: VehicleDao,
        spotPhotoDao: SpotPhotoDao,
        tagDao: TagDao,
        prefs: SharedPreferences,
        outputUri: Uri
    ): Result<Int> = runCatching {
        val spotCount = dao.countAllSpotsIncludingDeleted()
        val vehicles = vehicleDao.getAllList()
        val vehicleIndexById = vehicles.mapIndexed { index, v -> v.id to index }.toMap()
        val lowRam = context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
        val maxHtmlEmbeds = if (lowRam) MAX_HTML_EMBEDDED_PHOTOS_LOW_RAM else MAX_HTML_EMBEDDED_PHOTOS
        val resolver = context.contentResolver
        resolver.openOutputStream(outputUri)?.use { rawOut ->
            ZipOutputStream(BufferedOutputStream(rawOut)).use { zip ->
                var exportBytes = 0L
                fun accountExport(bytes: Long) {
                    exportBytes += bytes
                    if (exportBytes > MAX_BACKUP_TOTAL_BYTES) {
                        throw java.io.IOException(
                            "Vault is larger than the ${MAX_BACKUP_TOTAL_BYTES / (1024 * 1024)} MB backup limit"
                        )
                    }
                }
                fun writeText(name: String, text: String) {
                    val bytes = text.toByteArray(Charsets.UTF_8)
                    accountExport(bytes.size.toLong())
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
                fun writeFile(name: String, source: File) {
                    accountExport(source.length())
                    zip.putNextEntry(ZipEntry(name))
                    source.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
                val manifest = JSONObject().apply {
                    put("format", FORMAT)
                    put("version", VERSION)
                    put("exportedAt", System.currentTimeMillis())
                    put("spotCount", spotCount)
                    put("vehicleCount", vehicles.size)
                    put("layout", "spots/<folder>/ — photo.jpg, extra photos, notes.txt, and spot.json bundled per saved location; vehicles.json at the root")
                }
                writeText("manifest.json", manifest.toString(2))

                writeText(
                    "README.txt",
                    """
                    DropPin Vault Backup
                    ==================
                    Each saved spot lives in its own folder under spots/.
                    Open a folder to see the photo, notes, and details together.

                      spots/001_parking-spot/
                        photo.jpg    — cover photo (if saved)
                        photo_2.jpg  — additional photos, if any were added
                        notes.txt    — title, address, and notes (easy to read)
                        spot.json    — full metadata for import

                      vehicles.json  — every vehicle you've added, at the root

                    Open index.html in a browser for a visual gallery.
                    Photos are embedded in the HTML so they show even if you
                    open the file without the spots/ folders nearby.
                    Import this .zip from DropPin Vault → Settings → Backup & Data.
                    """.trimIndent()
                )

                val vehiclesArray = JSONArray()
                vehicles.forEach { vehiclesArray.put(vehicleToJson(it)) }
                writeText("vehicles.json", vehiclesArray.toString(2))

                // Spill gallery cards to a temp file — holding every embedded thumbnail in a
                // StringBuilder triples into toString/template/toByteArray on years-old vaults.
                val cardsSpillFile = File(context.cacheDir, "backup_export_cards.html")
                runCatching { cardsSpillFile.delete() }
                var visibleSpotCount = 0
                var galleryCardCount = 0
                // Page spot ids + load extras/tags per spot — no full-vault maps in RAM.
                var exportIndex = 0
                var afterTimestamp = Long.MAX_VALUE
                var afterId = Int.MAX_VALUE
                cardsSpillFile.bufferedWriter(Charsets.UTF_8).let { cardsOut ->
                    try {
                    while (true) {
                        val idPage = dao.getSpotIdsNewestPage(afterTimestamp, afterId, 500)
                        if (idPage.isEmpty()) break
                        for (idRow in idPage) {
                            val spotId = idRow.id
                            val index = exportIndex++
                            afterTimestamp = idRow.timestamp
                            afterId = idRow.id
                            val spot = dao.getSpotById(spotId) ?: continue
                            val folderName = spotFolderName(index, spot)
                            val folderPrefix = "spots/$folderName/"
                            var embedSource: File? = null
                            val photoEntry = if (spot.imagePath.isNotEmpty()) {
                                val source = File(spot.imagePath)
                                if (source.exists()) {
                                    val entryName = "${folderPrefix}photo.jpg"
                                    writeFile(entryName, source)
                                    // Decode-from-file for HTML embeds — never readBytes() a
                                    // multi-MB JPEG just to downscale it again.
                                    if (galleryCardCount < maxHtmlEmbeds &&
                                        !spot.isTrashed() &&
                                        source.length() <= MAX_HTML_EMBED_SOURCE_BYTES
                                    ) {
                                        embedSource = source
                                    }
                                    entryName
                                } else {
                                    ""
                                }
                            } else {
                                ""
                            }

                            val extraPhotos = spotPhotoDao.getPathsForSpotCapped(spot.id)
                            val extraPhotoNames = mutableListOf<String>()
                            extraPhotos.forEachIndexed { photoIndex, extraPath ->
                                val source = File(extraPath)
                                if (source.exists()) {
                                    val name = "photo_${photoIndex + 2}.${source.extension.ifBlank { "jpg" }}"
                                    writeFile("$folderPrefix$name", source)
                                    extraPhotoNames.add(name)
                                }
                            }

                            val vehicleExportIndex = spot.vehicleId?.let { vehicleIndexById[it] }
                            val tagNames = tagDao.getTagsForSpot(spot.id).map { it.name }
                            val spotJson = spotToJson(spot, photoEntry.removePrefix(folderPrefix), extraPhotoNames, vehicleExportIndex, tagNames)
                            writeText("${folderPrefix}spot.json", spotJson.toString(2))
                            writeText("${folderPrefix}notes.txt", buildSpotNotesText(spot, tagNames))

                            if (!spot.isTrashed()) {
                                if (galleryCardCount < MAX_HTML_GALLERY_CARDS) {
                                    val card = buildSpotCardHtml(
                                        folderName,
                                        spot,
                                        photoEntry,
                                        embedSource
                                    )
                                    cardsOut.append(card).append('\n')
                                    galleryCardCount++
                                }
                                visibleSpotCount++
                            }
                        }
                    }
                    } finally {
                        cardsOut.close()
                    }
                }

                writeText("prefs.json", serializeAllPrefs(prefs).toString(2))

                // Stream index.html: header + spilled cards + footer — no full DOM string in RAM.
                val exportedAt = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US).format(Date())
                val galleryNote = if (galleryCardCount < visibleSpotCount) {
                    """<p class="meta" style="padding:0 24px 12px">Showing $galleryCardCount of $visibleSpotCount spots here — open the spots/ folders in this zip for the rest.</p>"""
                } else {
                    ""
                }
                val htmlHeader = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                      <meta charset="utf-8" />
                      <meta name="viewport" content="width=device-width, initial-scale=1" />
                      <title>DropPin Vault Backup</title>
                      <style>
                        body { margin: 0; font-family: Segoe UI, sans-serif; background: #07040f; color: #f4f0ff; }
                        header { padding: 28px 24px; background: linear-gradient(135deg, #2a1458, #0b3140); }
                        h1 { margin: 0 0 6px; font-size: 28px; }
                        .meta { color: #9b93b0; font-size: 14px; }
                        main { padding: 24px; display: grid; gap: 18px; }
                        .card { background: #120a22; border: 1px solid #342652; border-radius: 18px; overflow: hidden; }
                        .card img { width: 100%; display: block; max-height: 320px; object-fit: cover; }
                        .no-photo { padding: 28px; text-align: center; color: #8d84a8; background: #0d0818; }
                        .body { padding: 18px 20px 22px; }
                        .location { color: #7efcff; margin: 8px 0 0; }
                        .notes { color: #d8d2ea; line-height: 1.5; }
                        .links a, .folder a { color: #b794ff; }
                        .folder { margin-top: 10px; font-size: 13px; }
                      </style>
                    </head>
                    <body>
                      <header>
                        <h1>DropPin Vault Backup</h1>
                        <div class="meta">Exported $exportedAt · $visibleSpotCount spots · each spot folder bundles photo + notes</div>
                      </header>
                      $galleryNote
                      <main>
                    
                """.trimIndent()
                val htmlFooter = """
                      </main>
                    </body>
                    </html>
                """.trimIndent()
                val cardsLen = cardsSpillFile.takeIf { it.exists() }?.length() ?: 0L
                accountExport(htmlHeader.toByteArray(Charsets.UTF_8).size.toLong() + cardsLen +
                    htmlFooter.toByteArray(Charsets.UTF_8).size.toLong())
                zip.putNextEntry(ZipEntry("index.html"))
                zip.write(htmlHeader.toByteArray(Charsets.UTF_8))
                if (cardsSpillFile.exists()) {
                    cardsSpillFile.inputStream().use { it.copyTo(zip) }
                }
                zip.write(htmlFooter.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                runCatching { cardsSpillFile.delete() }
            }
        } ?: error("Could not open backup destination")

        spotCount
    }

    private fun LocationSpot.isTrashed(): Boolean = deletedAt != null

    /** Deterministic fingerprint of everything a backup would capture — every spot's fields,
     * every vehicle, every extra photo, and the full prefs blob (Quick Pins, categories, theme,
     * anything else customized). Two calls return the same value iff nothing backup-worthy
     * changed, so the auto-backup worker can skip writing an identical zip. */
    suspend fun computeFingerprint(
        dao: LocationDao,
        vehicleDao: VehicleDao,
        spotPhotoDao: SpotPhotoDao,
        tagDao: TagDao,
        prefs: SharedPreferences
    ): String {
        // Stream into the digest — a giant intermediate StringBuilder of every spot's notes +
        // prefs used to risk OOM on years-old vaults before SHA-256 ever ran.
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        fun feed(text: CharSequence) {
            digest.update(text.toString().toByteArray(Charsets.UTF_8))
        }
        fun feedSep() = digest.update('|'.code.toByte())
        fun feedEnd() {
            digest.update('|'.code.toByte())
            digest.update('|'.code.toByte())
        }

        var afterSpotId = 0
        while (true) {
            val page = dao.getSpotFingerprintRowsPage(afterSpotId, 2_000)
            if (page.isEmpty()) break
            page.forEach { s ->
                feed(s.id.toString()); feedSep()
                feed(s.imagePath); feedSep()
                // Prefix/suffix + length — notepad edits invalidate without loading full note bodies.
                feed(s.notesLength.toString()); feedSep()
                feed(s.notesPrefix); feedSep()
                feed(s.notesSuffix); feedSep()
                feed(s.timestamp.toString()); feedSep()
                feed(s.lat.toString()); feedSep()
                feed(s.lng.toString()); feedSep()
                feed(s.address); feedSep()
                feed(s.isFavorite.toString()); feedSep()
                feed(s.title); feedSep()
                feed(s.isWishlist.toString()); feedSep()
                feed(s.isVisited.toString()); feedSep()
                feed(s.deletedAt?.toString().orEmpty()); feedSep()
                feed(s.vehicleId?.toString().orEmpty()); feedSep()
                feed(s.city); feedSep()
                feed(s.state); feedSep()
                feed(s.isArchived.toString()); feedSep()
                feed(s.isPinned.toString())
                feedEnd()
            }
            afterSpotId = page.last().id
        }
        vehicleDao.getAllList().sortedBy { it.id }.forEach { v ->
            feed(v.id.toString()); feedSep()
            feed(v.name); feedSep()
            feed(v.colorArgb.toString()); feedSep()
            feed(v.iconKey); feedSep()
            feed(v.notes); feedSep()
            feed(v.isDefault.toString()); feedSep()
            feed(v.isArchived.toString()); feedSep()
            feed(v.bluetoothMac.orEmpty()); feedSep()
            feed(v.bluetoothName.orEmpty())
            feedEnd()
        }
        // Page photo paths — years of extras must not all sit in one List.
        var afterPhotoId = 0
        while (true) {
            val page = spotPhotoDao.getPhotoRowsPage(afterPhotoId, 2_000)
            if (page.isEmpty()) break
            page.forEach { p ->
                feed(p.spotId.toString()); feedSep()
                feed(p.path)
                feedEnd()
            }
            afterPhotoId = page.last().id
        }
        var afterTagRowId = 0L
        while (true) {
            val page = tagDao.getSpotTagNamesPage(afterTagRowId, 2_000)
            if (page.isEmpty()) break
            page.forEach { t ->
                feed(t.locationId.toString()); feedSep()
                feed(t.name)
                feedEnd()
            }
            afterTagRowId = page.last().rowId
        }
        feed(serializeAllPrefs(prefs).toString())

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun importBackup(
        context: Context,
        db: AppDatabase,
        dao: LocationDao,
        vehicleDao: VehicleDao,
        spotPhotoDao: SpotPhotoDao,
        tagDao: TagDao,
        prefs: SharedPreferences,
        inputUri: Uri,
        replaceExisting: Boolean
    ): Result<Int> = try {
        // Only prefs.json stays in RAM. Photos, spot.json, vehicles.json, and legacy spots.json
        // spill to disk under hash names — years of entries must not grow path HashMaps in RAM.
        val extractedMeta = mutableMapOf<String, ByteArray>()
        var manifestJson: JSONObject? = null
        var totalBytes = 0L
        val imagesDir = backupImagesDir(context)
        val metaSpillDir = File(context.cacheDir, "backup_import_meta").also { dir ->
            dir.mkdirs()
            deleteDirectoryContentsStreamed(dir)
        }
        val spotJsonListFile = File(metaSpillDir, "spot_json_entries.txt")
        val photoCleanupList = File(context.cacheDir, "backup_import_photo_cleanup.txt")
        runCatching { photoCleanupList.delete() }

        fun isPhotoZipEntry(name: String): Boolean {
            val lower = name.lowercase(Locale.US)
            if (lower.endsWith(".json") || lower.endsWith(".txt") || lower.endsWith(".html") ||
                lower == "prefs.json" || lower == "spots.json" || lower == "vehicles.json" ||
                lower == "manifest.json" || lower == "readme.txt"
            ) {
                return false
            }
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                lower.endsWith(".webp") || lower.contains("/photo")
        }

        fun accountBytes(chunk: Long) {
            totalBytes += chunk
            if (totalBytes > MAX_BACKUP_TOTAL_BYTES) {
                throw java.io.IOException(
                    "Backup is larger than the ${MAX_BACKUP_TOTAL_BYTES / (1024 * 1024)} MB import limit"
                )
            }
        }

        try {
        context.contentResolver.openInputStream(inputUri)?.use { rawIn ->
            ZipInputStream(BufferedInputStream(rawIn)).use { zip ->
                spotJsonListFile.bufferedWriter().use { spotListWriter ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            when {
                                entry.name == "manifest.json" -> {
                                    val bytes = zip.readEntryBytesLimited(
                                        entry.name,
                                        MAX_BACKUP_ROOT_META_BYTES
                                    )
                                    accountBytes(bytes.size.toLong())
                                    manifestJson = JSONObject(String(bytes, Charsets.UTF_8))
                                }
                                isPhotoZipEntry(entry.name) -> {
                                    val extension = entry.name.substringAfterLast('.', "jpg")
                                    val outFile = spilledPhotoFile(imagesDir, entry.name, extension)
                                    try {
                                        zip.spillEntryToFile(
                                            entryName = entry.name,
                                            destination = outFile,
                                            maxEntryBytes = MAX_BACKUP_ENTRY_BYTES,
                                            onBytes = { accountBytes(it) }
                                        )
                                    } catch (e: Exception) {
                                        runCatching { outFile.delete() }
                                        throw e
                                    }
                                    photoCleanupList.appendText(outFile.absolutePath + "\n")
                                }
                                isDiscardableMetaEntry(entry.name) -> {
                                    zip.discardEntry(entry.name, MAX_BACKUP_META_ENTRY_BYTES) {
                                        accountBytes(it)
                                    }
                                }
                                isSpotJsonEntry(entry.name) || !isRootMetaEntry(entry.name) -> {
                                    val outFile = spilledMetaFile(metaSpillDir, entry.name)
                                    try {
                                        zip.spillEntryToFile(
                                            entryName = entry.name,
                                            destination = outFile,
                                            maxEntryBytes = MAX_BACKUP_META_ENTRY_BYTES,
                                            onBytes = { accountBytes(it) }
                                        )
                                    } catch (e: Exception) {
                                        runCatching { outFile.delete() }
                                        throw e
                                    }
                                    if (isSpotJsonEntry(entry.name)) {
                                        spotListWriter.appendLine(entry.name)
                                    }
                                }
                                else -> {
                                    // prefs.json only — tightly capped so a corrupt blob can't balloon.
                                    val bytes = zip.readEntryBytesLimited(
                                        entry.name,
                                        MAX_BACKUP_ROOT_META_BYTES
                                    )
                                    accountBytes(bytes.size.toLong())
                                    extractedMeta[entry.name] = bytes
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
        } ?: error("Could not read backup file")

        val manifest = manifestJson ?: error("Backup is missing manifest.json")
        require(manifest.optString("format") == FORMAT) { "Not a DropPin Vault backup file" }

        val version = manifest.optInt("version", LEGACY_VERSION)

        // Everything that touches the database — the wipe-if-replacing step and every spot/
        // vehicle insert — runs as one atomic transaction. Without this, a crash partway through
        // (one malformed spot.json, disk full mid-write) left whatever had already run committed
        // on its own: with replaceExisting on, that could mean the old vault was already wiped
        // before the failure, so "Import failed" reported to the user while their vault sat
        // empty rather than restored to its prior state.
        // Collected here, deleted only after the transaction below actually commits — these are
        // real filesystem deletes with no rollback of their own. Doing them mid-transaction (the
        // old behavior) meant a failure anywhere later in the same transaction (a malformed
        // spot.json, disk full mid-write) rolled the SQL back to the old rows while leaving their
        // photos already deleted, so a reported "Import failed" could still silently orphan every
        // existing spot's photo.
        // Paths to delete after commit are spilled to a temp file — retaining every cover/extra
        // path in a List would peak RAM on a replace restore of a years-old vault.
        val pendingDeletesFile = File(context.cacheDir, "backup_pending_photo_deletes.txt")
        runCatching { pendingDeletesFile.delete() }

        // Refuse before wipe when the backup is too large to restore safely in one session.
        if (version >= FOLDER_FORMAT_MIN_VERSION && spotJsonListFile.exists()) {
            var listed = 0
            spotJsonListFile.bufferedReader().use { reader ->
                while (reader.readLine() != null) {
                    listed++
                    if (listed > MAX_IMPORT_SPOTS) {
                        error(
                            "This backup has more than $MAX_IMPORT_SPOTS spots and cannot be " +
                                "imported safely. Export a smaller vault or contact support."
                        )
                    }
                }
            }
        }

        val imported = try {
            if (replaceExisting) {
                // Wipe + import stay one transaction so a mid-restore failure rolls back to the
                // prior vault (photo deletes still deferred until after commit). Spot count is
                // capped above so WAL growth stays bounded.
                db.withTransaction {
                    pendingDeletesFile.bufferedWriter().use { writer ->
                        var afterCoverId = 0
                        while (true) {
                            val page = dao.getCoverImagePathRowsPage(afterCoverId, 2_000)
                            if (page.isEmpty()) break
                            page.forEach { row ->
                                if (row.imagePath.isNotEmpty()) {
                                    writer.appendLine(row.imagePath)
                                }
                            }
                            afterCoverId = page.last().id
                        }
                        var afterPhotoId = 0
                        while (true) {
                            val page = spotPhotoDao.getPhotoRowsPage(afterPhotoId, 2_000)
                            if (page.isEmpty()) break
                            page.forEach { writer.appendLine(it.path) }
                            afterPhotoId = page.last().id
                        }
                    }
                    dao.deleteAllHistory() // cascades to spot_photos via the FK
                    vehicleDao.deleteAll()

                    val vehiclesSpill = findSpilledMeta(metaSpillDir, "vehicles.json")?.let { File(it) }
                    val vehicleIdMap = if (vehiclesSpill != null) {
                        importVehiclesFromFile(vehiclesSpill, vehicleDao)
                    } else {
                        emptyList()
                    }

                    when {
                        version >= FOLDER_FORMAT_MIN_VERSION -> importFolderBackup(
                            metaSpillDir = metaSpillDir,
                            spotJsonListFile = spotJsonListFile,
                            imagesDir = imagesDir,
                            dao = dao,
                            spotPhotoDao = spotPhotoDao,
                            tagDao = tagDao,
                            vehicleIdMap = vehicleIdMap,
                            expectedSpotCount = manifest.optInt("spotCount", -1),
                            db = null,
                            commitBatchSize = 0
                        )
                        else -> importLegacyBackup(
                            metaSpillDir = metaSpillDir,
                            imagesDir = imagesDir,
                            dao = dao,
                            prefs = prefs,
                            maxSpots = MAX_IMPORT_SPOTS
                        )
                    }
                }
            } else {
                // Merge: vehicles first, then spot batches — partial merge on failure is OK and
                // avoids holding one multi-year write lock for the whole import.
                val vehicleIdMap = db.withTransaction {
                    val vehiclesSpill = findSpilledMeta(metaSpillDir, "vehicles.json")?.let { File(it) }
                    if (vehiclesSpill != null) {
                        importVehiclesFromFile(vehiclesSpill, vehicleDao)
                    } else {
                        emptyList()
                    }
                }
                when {
                    version >= FOLDER_FORMAT_MIN_VERSION -> importFolderBackup(
                        metaSpillDir = metaSpillDir,
                        spotJsonListFile = spotJsonListFile,
                        imagesDir = imagesDir,
                        dao = dao,
                        spotPhotoDao = spotPhotoDao,
                        tagDao = tagDao,
                        vehicleIdMap = vehicleIdMap,
                        expectedSpotCount = manifest.optInt("spotCount", -1),
                        db = db,
                        commitBatchSize = IMPORT_SPOT_COMMIT_BATCH
                    )
                    else -> importLegacyBackup(
                        metaSpillDir = metaSpillDir,
                        imagesDir = imagesDir,
                        dao = dao,
                        prefs = prefs,
                        maxSpots = MAX_IMPORT_SPOTS,
                        db = db,
                        commitBatchSize = IMPORT_SPOT_COMMIT_BATCH
                    )
                }
            }
        } catch (e: Exception) {
            // Replace rolls back entirely — spilled import photos are orphans. Merge may have
            // committed earlier batches; leave those files for the orphan sweep rather than
            // deleting paths that now belong to kept spots.
            if (replaceExisting && photoCleanupList.exists()) {
                photoCleanupList.forEachLine { path ->
                    if (path.isNotBlank()) runCatching { File(path).delete() }
                }
            }
            // Do not delete paths listed in pendingDeletesFile — those still belong to the
            // rolled-back vault rows. Only drop the path list itself.
            runCatching { pendingDeletesFile.delete() }
            throw e
        } finally {
            runCatching { photoCleanupList.delete() }
        }

        // Transaction committed successfully — now safe to actually reclaim the old photos' disk
        // space, since there's nothing left for a later failure in this import to roll back onto.
        if (pendingDeletesFile.exists()) {
            pendingDeletesFile.forEachLine { path ->
                if (path.isNotBlank()) runCatching { File(path).delete() }
            }
            runCatching { pendingDeletesFile.delete() }
        }

        importPrefs(extractedMeta, prefs, clearNonExcludedFirst = replaceExisting)
        // Recomputed from scratch rather than trusted from assignTag()'s own incremental bumps
        // above — a replaceExisting wipe cascades every pre-existing spot's cross-refs away via
        // the FK with nothing decrementing the tags they carried, so every tag's usageCount would
        // otherwise stay stuck at its pre-wipe value forever, on top of whatever the just-imported
        // spots added. Harmless (a no-op) on a merge import where existing spots/cross-refs were
        // never touched.
        db.tagDao().recomputeAllUsageCounts()
        if (replaceExisting) {
            // Free pages left by the wipe stay on disk until VACUUM — reclaim once restore commits.
            db.reclaimDiskAfterBulkWipe()
        }
        Result.success(imported)
        } finally {
            deleteDirectoryContentsStreamed(metaSpillDir)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** name.lowercase(), or the Bluetooth MAC when set — good enough to say "same vehicle" for
     * dedup purposes without a stable cross-install id. */
    private fun vehicleIdentity(name: String, bluetoothMac: String?): String =
        bluetoothMac?.uppercase(Locale.US)?.takeIf { it.isNotBlank() } ?: name.trim().lowercase(Locale.US)

    private suspend fun importVehiclesFromFile(vehiclesFile: File, vehicleDao: VehicleDao): List<Int> {
        val existing = vehicleDao.getAllList()
        val existingByIdentity = existing.associateBy { vehicleIdentity(it.name, it.bluetoothMac) }
        val newIds = mutableListOf<Int>()
        // Set via vehicleDao.setDefault() after the loop, not the raw insert below — insert()
        // has no idea any other vehicle exists, so a backup vehicle with isDefault=true landed as
        // a *second* isDefault=1 row alongside whatever the merge target already had, rather than
        // actually becoming the one default. VehicleStore's own saveVehicle()/insertNewVehicle()
        // already route every other "this vehicle is now default" write through setDefault() for
        // exactly this reason (it transactionally clears every other row first) — this was the one
        // path that bypassed it.
        var newDefaultId: Int? = null
        var vehicleCount = 0
        vehiclesFile.inputStream().use { stream ->
            JsonReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                reader.beginArray()
                while (reader.hasNext()) {
                    if (vehicleCount >= MAX_IMPORT_VEHICLES) {
                        reader.skipValue()
                        continue
                    }
                    val v = reader.readJsonObject()
                    vehicleCount++
                    val name = v.optString("name", "My Car").take(80)
                    val bluetoothMac = v.optString("bluetoothMac", "").ifBlank { null }?.take(64)
                    val already = existingByIdentity[vehicleIdentity(name, bluetoothMac)]
                    if (already != null) {
                        newIds.add(already.id)
                        continue
                    }
                    val isDefault = v.optBoolean("isDefault", false)
                    val newId = vehicleDao.insert(
                        Vehicle(
                            id = 0,
                            name = name,
                            colorArgb = v.optInt("colorArgb", VehicleColorOptions.first()),
                            iconKey = v.optString("iconKey", "car").take(32),
                            notes = v.optString("notes", "").take(2_000),
                            isDefault = false,
                            isArchived = v.optBoolean("isArchived", false),
                            bluetoothMac = bluetoothMac,
                            bluetoothName = v.optString("bluetoothName", "").ifBlank { null }?.take(80),
                            createdAt = v.optLong("createdAt", System.currentTimeMillis())
                        )
                    ).toInt()
                    if (isDefault) newDefaultId = newId
                    newIds.add(newId)
                }
                reader.endArray()
            }
        }
        newDefaultId?.let { vehicleDao.setDefault(it) }
        // A backup where no vehicle was ever marked default (older export, or one that had its
        // default vehicle archived/deleted since) would otherwise leave getDefault() returning
        // null despite active vehicles existing — every save-time vehicle resolver now trusts
        // that null strictly means "no default set", so restore needs the same guarantee any
        // other path that can end up default-less already provides.
        promoteDefaultVehicleIfNeeded(vehicleDao)
        return newIds
    }

    private suspend fun importFolderBackup(
        metaSpillDir: File,
        spotJsonListFile: File,
        imagesDir: File,
        dao: LocationDao,
        spotPhotoDao: SpotPhotoDao,
        tagDao: TagDao,
        vehicleIdMap: List<Int>,
        expectedSpotCount: Int,
        db: AppDatabase?,
        commitBatchSize: Int
    ): Int {
        var imported = 0
        var spotJsonCount = 0

        suspend fun importOneSpot(jsonPath: String, index: Int): Boolean {
            val folderPrefix = jsonPath.removeSuffix("spot.json")
            val item = JSONObject(readSpilledMetaRequired(metaSpillDir, jsonPath))
            val imagePath = resolveSpotPhoto(folderPrefix, item, imagesDir, metaSpillDir, index)
            val newSpotId = insertSpotFromJson(dao, item, imagePath, vehicleIdMap) ?: return false
            val extraNames = item.optJSONArray("extraPhotos")
            if (extraNames != null) {
                val maxExtras = minOf(extraNames.length(), MAX_EXTRA_PHOTOS_PER_SPOT)
                for (i in 0 until maxExtras) {
                    val name = extraNames.getString(i)
                    val entryName = folderPrefix + name
                    val spilled = findSpilledPhoto(imagesDir, entryName)
                    if (spilled != null) {
                        spotPhotoDao.insertExtraPhotoCapped(newSpotId, spilled)
                        continue
                    }
                    val spilledMeta = findSpilledMeta(metaSpillDir, entryName) ?: continue
                    val extension = name.substringAfterLast('.', "jpg")
                    val outFile = File(imagesDir, "import_extra_${System.currentTimeMillis()}_${index}_$i.$extension")
                    File(spilledMeta).copyTo(outFile, overwrite = true)
                    spotPhotoDao.insertExtraPhotoCapped(newSpotId, outFile.absolutePath)
                }
            }
            // Tag names, not ids — tags are globally unique by name (see TagEntity's unique
            // index), and assignTag() already finds-or-creates by name, so this needs no id
            // remapping table the way vehicles do. Also correct for a merge import: an
            // existing tag with the same name just gets this spot added as a new member.
            val tagNames = item.optJSONArray("tags")
            if (tagNames != null) {
                val maxTags = minOf(tagNames.length(), MAX_IMPORT_TAGS_PER_SPOT)
                for (i in 0 until maxTags) {
                    val name = tagNames.getString(i).trim().take(MAX_IMPORT_TAG_NAME_CHARS)
                    if (name.isNotEmpty()) tagDao.assignTag(newSpotId, name)
                }
            }
            return true
        }

        if (spotJsonListFile.exists()) {
            val reader = spotJsonListFile.bufferedReader()
            try {
                val batch = mutableListOf<Pair<String, Int>>()
                while (true) {
                    val jsonPath = reader.readLine() ?: break
                    if (jsonPath.isBlank()) continue
                    if (spotJsonCount >= MAX_IMPORT_SPOTS) break
                    val index = spotJsonCount
                    spotJsonCount++
                    if (db != null && commitBatchSize > 0) {
                        batch.add(jsonPath to index)
                        if (batch.size >= commitBatchSize) {
                            db.withTransaction {
                                batch.forEach { (path, idx) ->
                                    if (importOneSpot(path, idx)) imported++
                                }
                            }
                            batch.clear()
                        }
                    } else if (importOneSpot(jsonPath, index)) {
                        imported++
                    }
                }
                if (batch.isNotEmpty() && db != null && commitBatchSize > 0) {
                    db.withTransaction {
                        batch.forEach { (path, idx) ->
                            if (importOneSpot(path, idx)) imported++
                        }
                    }
                }
            } finally {
                reader.close()
            }
        }

        // manifest.json's own spotCount is the source of truth for "this backup genuinely has
        // zero spots" (e.g. importing/restoring a backup taken from an empty vault — the Google
        // Drive restore path can hit exactly this) — without checking it, that valid, correctly-
        // formatted zip looked identical to someone picking a completely unrelated file with no
        // spots/ folder at all, and both used to fail with the same "not a real backup" error.
        if (imported == 0 && spotJsonCount == 0 && expectedSpotCount != 0) {
            error("Backup contains no spot folders under spots/")
        }
        return imported
    }

    private suspend fun importLegacyBackup(
        metaSpillDir: File,
        imagesDir: File,
        dao: LocationDao,
        prefs: SharedPreferences,
        maxSpots: Int = MAX_IMPORT_SPOTS,
        db: AppDatabase? = null,
        commitBatchSize: Int = 0
    ): Int {
        val spotsFile = findSpilledMeta(metaSpillDir, "spots.json")?.let { File(it) }
            ?: error("Backup is missing spots.json")
        if (spotsFile.length() > MAX_LEGACY_SPOTS_JSON_BYTES) {
            error(
                "This older backup format is too large to import safely. " +
                    "Export again from a recent DropPin Vault build and retry."
            )
        }

        // Stream one spot object at a time — never materialize the full spots.json DOM.
        var imported = 0
        var processed = 0
        suspend fun importLegacyItem(item: JSONObject, i: Int): Boolean {
            val imageEntry = item.optString("imageEntry", "")
            val imagePath = when {
                imageEntry.isEmpty() -> ""
                else -> {
                    findSpilledPhoto(imagesDir, imageEntry)
                        ?: findSpilledMeta(metaSpillDir, imageEntry)?.let { metaPath ->
                            val outFile =
                                File(imagesDir, "import_${System.currentTimeMillis()}_$i.jpg")
                            File(metaPath).copyTo(outFile, overwrite = true)
                            outFile.absolutePath
                        }
                        ?: ""
                }
            }
            return insertSpotFromJson(dao, item, imagePath, emptyList()) != null
        }

        spotsFile.inputStream().let { stream ->
            try {
                val reader = JsonReader(InputStreamReader(stream, Charsets.UTF_8))
                try {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "spots" -> {
                                reader.beginArray()
                                val batch = mutableListOf<Pair<JSONObject, Int>>()
                                var i = 0
                                while (reader.hasNext()) {
                                    if (processed >= maxSpots) {
                                        while (reader.hasNext()) reader.skipValue()
                                        break
                                    }
                                    val item = reader.readJsonObject()
                                    processed++
                                    if (db != null && commitBatchSize > 0) {
                                        batch.add(item to i)
                                        if (batch.size >= commitBatchSize) {
                                            db.withTransaction {
                                                batch.forEach { (obj, idx) ->
                                                    if (importLegacyItem(obj, idx)) imported++
                                                }
                                            }
                                            batch.clear()
                                        }
                                    } else if (importLegacyItem(item, i)) {
                                        imported++
                                    }
                                    i++
                                }
                                if (batch.isNotEmpty() && db != null && commitBatchSize > 0) {
                                    db.withTransaction {
                                        batch.forEach { (obj, idx) ->
                                            if (importLegacyItem(obj, idx)) imported++
                                        }
                                    }
                                }
                                reader.endArray()
                            }
                            "prefs" -> applyPrefBackup(reader.readJsonObject(), prefs)
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                } finally {
                    reader.close()
                }
            } finally {
                stream.close()
            }
        }
        return imported
    }

    private fun readSpilledMetaRequired(metaSpillDir: File, entryName: String): String {
        val path = findSpilledMeta(metaSpillDir, entryName)
            ?: error("Backup is missing $entryName")
        val file = File(path)
        // Spill already enforced MAX_BACKUP_META_ENTRY_BYTES; keep a tighter ceiling for the
        // DOM parse so a fat/corrupt spot.json can't become a multi-MB JSONObject.
        if (file.length() > MAX_BACKUP_ROOT_META_BYTES) {
            error("Backup entry $entryName is too large to import safely")
        }
        return file.readText(Charsets.UTF_8)
    }

    /** One JSON object from [JsonReader] without buffering sibling objects. */
    private fun JsonReader.readJsonObject(): JSONObject {
        beginObject()
        val obj = JSONObject()
        while (hasNext()) {
            obj.put(nextName(), readJsonValue())
        }
        endObject()
        return obj
    }

    private fun JsonReader.readJsonValue(): Any? {
        return when (peek()) {
            JsonToken.BEGIN_OBJECT -> readJsonObject()
            JsonToken.BEGIN_ARRAY -> {
                beginArray()
                val arr = JSONArray()
                while (hasNext()) arr.put(readJsonValue())
                endArray()
                arr
            }
            JsonToken.STRING -> nextString()
            JsonToken.NUMBER -> {
                val raw = nextString()
                raw.toLongOrNull() ?: raw.toDoubleOrNull() ?: raw
            }
            JsonToken.BOOLEAN -> nextBoolean()
            JsonToken.NULL -> {
                nextNull()
                JSONObject.NULL
            }
            else -> {
                skipValue()
                null
            }
        }
    }

    private fun resolveSpotPhoto(
        folderPrefix: String,
        item: JSONObject,
        imagesDir: File,
        metaSpillDir: File,
        index: Int
    ): String {
        val candidates = listOf(
            item.optString("photoFile", ""),
            "photo.jpg",
            "photo.jpeg",
            "photo.png",
            "image.jpg"
        ).map { name ->
            when {
                name.isEmpty() -> ""
                name.contains('/') -> name
                else -> folderPrefix + name
            }
        }.filter { it.isNotEmpty() }.distinct()

        candidates.firstNotNullOfOrNull { findSpilledPhoto(imagesDir, it) }?.let { return it }

        candidates.firstNotNullOfOrNull { findSpilledMeta(metaSpillDir, it) }?.let { spilledPath ->
            val extension = spilledPath.substringAfterLast('.', "jpg")
            val outFile = File(imagesDir, "import_${System.currentTimeMillis()}_$index.$extension")
            File(spilledPath).copyTo(outFile, overwrite = true)
            return outFile.absolutePath
        }

        return ""
    }

    /** Returns the new spot's row id on success, or null if it was skipped as a duplicate. */
    private suspend fun insertSpotFromJson(
        dao: LocationDao,
        item: JSONObject,
        imagePath: String,
        vehicleIdMap: List<Int>
    ): Int? {
        val timestamp = item.optLong("timestamp", System.currentTimeMillis())
        val lat = item.optDouble("lat", 0.0)
        val lng = item.optDouble("lng", 0.0)

        // EXISTS against Room (visible inside this transaction) — no unbounded HashSet of every
        // existing vault signature for merge imports of multi-year backups.
        if (dao.hasSpotSignature(timestamp, lat, lng)) {
            // Same timestamp + location already accounted for — skip, and don't leave the
            // photo we already unzipped for this entry sitting around as an orphan.
            if (imagePath.isNotEmpty()) runCatching { File(imagePath).delete() }
            return null
        }

        val vehicleExportIndex = if (item.has("vehicleExportIndex") && !item.isNull("vehicleExportIndex")) {
            item.optInt("vehicleExportIndex", -1)
        } else {
            -1
        }
        val vehicleId = vehicleExportIndex.takeIf { it in vehicleIdMap.indices }?.let { vehicleIdMap[it] }
        val deletedAt = item.optLong("deletedAt", -1L).takeIf { it > 0 }
        // Trash and Archive are mutually exclusive in the UI — never import both flags set.
        val isArchived = if (deletedAt != null) false else item.optBoolean("isArchived", false)

        val newId = dao.insertSpotAndGetId(
            LocationSpot(
                id = 0,
                imagePath = imagePath,
                locationDetails = item.optString("locationDetails", "").take(NOTEPAD_MAX_CHARS),
                timestamp = timestamp,
                lat = lat,
                lng = lng,
                address = item.optString("address", "").take(MAX_IMPORT_ADDRESS_CHARS),
                isFavorite = item.optBoolean("isFavorite", false),
                title = item.optString("title", "").take(MAX_IMPORT_TITLE_CHARS),
                isWishlist = item.optBoolean("isWishlist", false),
                isVisited = item.optBoolean("isVisited", false),
                deletedAt = deletedAt,
                vehicleId = vehicleId,
                city = item.optString("city", "").take(MAX_IMPORT_CITY_STATE_CHARS),
                state = item.optString("state", "").take(MAX_IMPORT_CITY_STATE_CHARS),
                isArchived = isArchived,
                isPinned = item.optBoolean("isPinned", false)
            )
        )
        return newId.toInt()
    }

    private fun importPrefs(
        extracted: Map<String, ByteArray>,
        prefs: SharedPreferences,
        clearNonExcludedFirst: Boolean = false
    ) {
        val prefBytes = extracted["prefs.json"] ?: return
        val prefBackup = JSONObject(String(prefBytes, Charsets.UTF_8))
        // Replace restores used to only overlay keys present in the backup, so local-only prefs
        // (theme tweaks, toggles never captured) survived and mixed with the restored vault —
        // looking like a corrupt partial restore. Clearing non-excluded keys first makes replace
        // match the DB wipe semantics; merge imports leave local prefs alone.
        if (clearNonExcludedFirst) {
            val editor = prefs.edit()
            prefs.all.keys.forEach { key ->
                if (key !in BACKUP_EXCLUDED_PREF_KEYS) editor.remove(key)
            }
            editor.apply()
        }
        applyPrefBackup(prefBackup, prefs)
    }

    private fun applyPrefBackup(prefBackup: JSONObject, prefs: SharedPreferences) {
        restoreAllPrefs(prefBackup, prefs)
    }

    private fun serializeAllPrefs(prefs: SharedPreferences): JSONObject {
        val out = JSONObject()
        prefs.all.forEach { (key, value) ->
            if (key in BACKUP_EXCLUDED_PREF_KEYS) return@forEach
            val entry = JSONObject()
            when (value) {
                is Boolean -> { entry.put("t", "bool"); entry.put("v", value) }
                is Int -> { entry.put("t", "int"); entry.put("v", value) }
                is Long -> { entry.put("t", "long"); entry.put("v", value) }
                is Float -> { entry.put("t", "float"); entry.put("v", value.toDouble()) }
                // Cap runaway string prefs (legacy pin notes, etc.) so prefs.json can't balloon.
                is String -> {
                    if (value.length > 16_384) return@forEach
                    entry.put("t", "string"); entry.put("v", value)
                }
                is Set<*> -> {
                    val capped = value.filterIsInstance<String>().filter { it.length <= 512 }.take(500)
                    entry.put("t", "set"); entry.put("v", JSONArray(capped))
                }
                else -> return@forEach
            }
            out.put(key, entry)
        }
        return out
    }

    private fun restoreAllPrefs(json: JSONObject, prefs: SharedPreferences) {
        val editor = prefs.edit()
        json.keys().forEach { key ->
            if (key in BACKUP_EXCLUDED_PREF_KEYS) return@forEach
            when (val raw = json.get(key)) {
                is JSONObject -> when (raw.optString("t")) {
                    "bool" -> editor.putBoolean(key, raw.getBoolean("v"))
                    "int" -> editor.putInt(key, raw.getInt("v"))
                    "long" -> editor.putLong(key, raw.getLong("v"))
                    "float" -> editor.putFloat(key, raw.getDouble("v").toFloat())
                    "string" -> {
                        var value = raw.getString("v")
                        // Match serializeAllPrefs — skip runaway strings that would bloat prefs.
                        if (value.length > 16_384) return@forEach
                        // Live writers already use prefsSafe* — restore must re-clamp so a
                        // generous backup cannot reintroduce pin/Glance binder pressure.
                        value = when (key) {
                            "current_address" -> prefsSafeAddress(value)
                            "location_details" -> prefsSafeLocationDetails(value)
                            "photo_path" -> prefsSafePhotoPath(value)
                            "alarm_sound_uri" -> prefsSafeAlarmSoundUri(value)
                            else -> value
                        }
                        editor.putString(key, value)
                    }
                    "set" -> {
                        val arr = raw.getJSONArray("v")
                        val set = mutableSetOf<String>()
                        val maxItems = minOf(arr.length(), 500)
                        for (i in 0 until maxItems) {
                            val s = arr.getString(i)
                            if (s.length <= 512) set.add(s)
                        }
                        editor.putStringSet(key, set)
                    }
                }
                // Backward compatible with old backups, which stored a bare string array
                // directly under "custom_categories" / "custom_tags" with no type wrapper.
                is JSONArray -> {
                    val set = mutableSetOf<String>()
                    val maxItems = minOf(raw.length(), 500)
                    for (i in 0 until maxItems) {
                        val s = raw.getString(i)
                        if (s.length <= 512) set.add(s)
                    }
                    editor.putStringSet(key, set)
                }
                else -> {}
            }
        }
        editor.apply()
    }

    private fun spotToJson(
        spot: LocationSpot,
        photoFile: String,
        extraPhotoNames: List<String> = emptyList(),
        vehicleExportIndex: Int? = null,
        tagNames: List<String> = emptyList()
    ): JSONObject {
        return JSONObject().apply {
            put("id", spot.id)
            put("photoFile", photoFile)
            put("extraPhotos", JSONArray(extraPhotoNames))
            put("tags", JSONArray(tagNames))
            put("locationDetails", spot.locationDetails)
            put("timestamp", spot.timestamp)
            put("lat", spot.lat)
            put("lng", spot.lng)
            put("address", spot.address)
            put("city", spot.city)
            put("state", spot.state)
            put("isFavorite", spot.isFavorite)
            put("title", spot.title)
            put("isWishlist", spot.isWishlist)
            put("isVisited", spot.isVisited)
            put("deletedAt", spot.deletedAt ?: JSONObject.NULL)
            put("isArchived", spot.isArchived)
            put("isPinned", spot.isPinned)
            put("vehicleExportIndex", vehicleExportIndex ?: JSONObject.NULL)
        }
    }

    private fun vehicleToJson(vehicle: Vehicle): JSONObject {
        return JSONObject().apply {
            put("name", vehicle.name)
            put("colorArgb", vehicle.colorArgb)
            put("iconKey", vehicle.iconKey)
            put("notes", vehicle.notes)
            put("isDefault", vehicle.isDefault)
            put("isArchived", vehicle.isArchived)
            put("bluetoothMac", vehicle.bluetoothMac ?: "")
            put("bluetoothName", vehicle.bluetoothName ?: "")
            put("createdAt", vehicle.createdAt)
        }
    }

    private fun spotFolderName(index: Int, spot: LocationSpot): String {
        val rawTitle = spot.title.ifBlank { "saved-spot" }
        val slug = rawTitle
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(40)
            .ifBlank { "spot" }
        return String.format(Locale.US, "%03d_%s", index + 1, slug)
    }

    private fun buildSpotNotesText(spot: LocationSpot, tagNames: List<String> = emptyList()): String {
        val title = spot.title.ifBlank { "Saved Spot" }
        val location = spot.address.ifBlank {
            String.format(Locale.US, "%.6f, %.6f", spot.lat, spot.lng)
        }
        val savedAt = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US).format(Date(spot.timestamp))
        val notes = spot.locationDetails.trim()

        return buildString {
            appendLine(title)
            appendLine("=".repeat(title.length.coerceAtMost(48)))
            appendLine()
            appendLine("Saved: $savedAt")
            appendLine("Location: $location")
            if (tagNames.isNotEmpty()) {
                appendLine("Tags: ${tagNames.joinToString(", ") { "#$it" }}")
            }
            if (spot.lat != 0.0 || spot.lng != 0.0) {
                val lat = String.format(Locale.US, "%.6f", spot.lat)
                val lng = String.format(Locale.US, "%.6f", spot.lng)
                appendLine("Coordinates: $lat, $lng")
                appendLine("Apple Maps: https://maps.apple.com/?ll=$lat,$lng")
                appendLine("Google Maps: https://www.google.com/maps/search/?api=1&query=$lat,$lng")
            }
            appendLine()
            appendLine("Notes")
            appendLine("-----")
            appendLine(if (notes.isNotEmpty()) notes else "(No notes saved)")
            appendLine()
            appendLine("Photo: see photo.jpg in this folder (if present)")
        }
    }

    /**
     * Downscales a spot photo for the HTML gallery and returns a data-URI so the
     * gallery works even when index.html is opened without the spots/ folders
     * (common when people open only the HTML from a zip viewer or Downloads).
     * Decodes from [source] with inSampleSize — never buffers the full JPEG in RAM.
     */
    private fun photoDataUriFromFile(source: File?): String? {
        if (source == null || !source.exists()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.absolutePath, bounds)
            val maxEdge = 960
            var sample = 1
            while (
                bounds.outWidth / sample > maxEdge * 2 ||
                bounds.outHeight / sample > maxEdge * 2
            ) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = BitmapFactory.decodeFile(source.absolutePath, opts)
                ?: return@runCatching null
            val scaled = if (decoded.width > maxEdge || decoded.height > maxEdge) {
                val scale = maxEdge.toFloat() / maxOf(decoded.width, decoded.height)
                val w = (decoded.width * scale).toInt().coerceAtLeast(1)
                val h = (decoded.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decoded, w, h, true).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
            scaled.recycle()
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        }.getOrNull()
    }

    /** One gallery card's HTML — [embedSource] is decoded once via [photoDataUriFromFile]. */
    private fun buildSpotCardHtml(folderName: String, spot: LocationSpot, photoEntry: String, embedSource: File?): String {
        val title = spot.title.ifBlank { "Saved Spot" }
        val location = spot.address.ifBlank {
            String.format(Locale.US, "%.6f, %.6f", spot.lat, spot.lng)
        }
        val notes = spot.locationDetails.trim().take(MAX_HTML_NOTES_CHARS)
        val dataUri = photoDataUriFromFile(embedSource)
        val imageBlock = when {
            dataUri != null -> """<img src="$dataUri" alt="Spot photo" />"""
            photoEntry.isNotEmpty() -> {
                // Fallback relative path if embedding somehow fails but the file exists.
                """<img src="spots/$folderName/photo.jpg" alt="Spot photo" />"""
            }
            else -> """<div class="no-photo">No photo saved</div>"""
        }
        val notesBlock = if (notes.isNotEmpty()) {
            """<p class="notes"><strong>Notes:</strong> ${escapeHtml(notes)}</p>"""
        } else {
            ""
        }
        val mapsBlock = if (spot.lat != 0.0 || spot.lng != 0.0) {
            val lat = String.format(Locale.US, "%.6f", spot.lat)
            val lng = String.format(Locale.US, "%.6f", spot.lng)
            """<p class="links"><a href="https://maps.apple.com/?ll=$lat,$lng">Apple Maps</a> · <a href="https://www.google.com/maps/search/?api=1&query=$lat,$lng">Google Maps</a></p>"""
        } else {
            ""
        }
        val folderLink = """<p class="folder"><a href="spots/$folderName/">Open folder</a> · <a href="spots/$folderName/notes.txt">Read notes.txt</a></p>"""
        return """
        <article class="card">
          $imageBlock
          <div class="body">
            <h2>${escapeHtml(title)}</h2>
            <p class="location">${escapeHtml(location)}</p>
            $notesBlock
            $mapsBlock
            $folderLink
          </div>
        </article>
        """.trimIndent()
    }

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    internal fun buildSpotNotesTextForExport(spot: LocationSpot): String = buildSpotNotesText(spot)

    internal fun spotToJsonForExport(spot: LocationSpot, photoFile: String = ""): JSONObject =
        spotToJson(spot, photoFile)

    internal fun spotLocationDetailsFromJson(json: JSONObject): String =
        json.optString("locationDetails", "")
}
