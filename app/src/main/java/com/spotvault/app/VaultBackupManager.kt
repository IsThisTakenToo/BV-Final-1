package com.spotvault.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
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
    /** Cap on base64-embedded thumbnails in index.html — beyond this, cards link to
     * spots/.../photo.jpg so a multi-year vault can't OOM the export StringBuilder. */
    private const val MAX_HTML_EMBEDDED_PHOTOS = 40
    /** Skip embedding source JPEGs larger than this — still zip the file; card uses relative link. */
    private const val MAX_HTML_EMBED_SOURCE_BYTES = 8L * 1024 * 1024
    /** Cap gallery cards in index.html — past this, folders in the zip are still complete. */
    private const val MAX_HTML_GALLERY_CARDS = 200
    private const val MAX_HTML_NOTES_CHARS = 200
    /** Total decompressed payload across every zip entry — per-entry limit alone still allows
     * hundreds of large photos to OOM the process when all buffered into [extracted]. */
    internal const val MAX_BACKUP_TOTAL_BYTES = 512L * 1024 * 1024

    /** Like [ZipInputStream.readBytes] but aborts once [maxBytes] is exceeded instead of growing
     * an unbounded buffer for a corrupt or adversarial entry. */
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

    private fun spotSignature(timestamp: Long, lat: Double, lng: Double): String =
        "$timestamp|${"%.6f".format(Locale.US, lat)}|${"%.6f".format(Locale.US, lng)}"

    fun backupImagesDir(context: Context): File {
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
        val spotIds = dao.getAllSpotIdsOrdered()
        val vehicles = vehicleDao.getAllList()
        val vehicleIndexById = vehicles.mapIndexed { index, v -> v.id to index }.toMap()
        val resolver = context.contentResolver
        resolver.openOutputStream(outputUri)?.use { rawOut ->
            ZipOutputStream(BufferedOutputStream(rawOut)).use { zip ->
                val manifest = JSONObject().apply {
                    put("format", FORMAT)
                    put("version", VERSION)
                    put("exportedAt", System.currentTimeMillis())
                    put("spotCount", spotIds.size)
                    put("vehicleCount", vehicles.size)
                    put("layout", "spots/<folder>/ — photo.jpg, extra photos, notes.txt, and spot.json bundled per saved location; vehicles.json at the root")
                }
                writeZipText(zip, "manifest.json", manifest.toString(2))

                writeZipText(
                    zip,
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
                writeZipText(zip, "vehicles.json", vehiclesArray.toString(2))

                // Built incrementally in the loop below rather than collected into a
                // List<SpotExportSummary> holding every spot's raw photo bytes for the whole
                // export — see the comment right before buildSpotCardHtml() is called.
                val cardsHtml = StringBuilder()
                var visibleSpotCount = 0
                var galleryCardCount = 0
                // Path-only index — full SpotPhoto rows (id/createdAt) are unused for export.
                val photosBySpotId = spotPhotoDao.getAllPhotoPathRows().groupBy { it.spotId }
                val tagsBySpotId = tagDao.getAllSpotTagNames()
                    .groupBy({ it.locationId }, { it.name })
                // One full spot row at a time — years of 50k notepads must not all sit in RAM.
                spotIds.forEachIndexed { index, spotId ->
                    val spot = dao.getSpotById(spotId) ?: return@forEachIndexed
                    val folderName = spotFolderName(index, spot)
                    val folderPrefix = "spots/$folderName/"
                    var photoBytes: ByteArray? = null
                    val photoEntry = if (spot.imagePath.isNotEmpty()) {
                        val source = File(spot.imagePath)
                        if (source.exists()) {
                            val entryName = "${folderPrefix}photo.jpg"
                            writeZipFile(zip, entryName, source)
                            // Only buffer bytes when the HTML gallery will embed this photo —
                            // past MAX_HTML_EMBEDDED_PHOTOS the zip already has the file stream.
                            // Cap source size too — pre-compress legacy cameras can still be huge.
                            if (galleryCardCount < MAX_HTML_EMBEDDED_PHOTOS &&
                                !spot.isTrashed() &&
                                source.length() <= MAX_HTML_EMBED_SOURCE_BYTES
                            ) {
                                photoBytes = source.readBytes()
                            }
                            entryName
                        } else {
                            ""
                        }
                    } else {
                        ""
                    }

                    val extraPhotos = photosBySpotId[spot.id].orEmpty()
                    val extraPhotoNames = mutableListOf<String>()
                    extraPhotos.forEachIndexed { photoIndex, extra ->
                        val source = File(extra.path)
                        if (source.exists()) {
                            val name = "photo_${photoIndex + 2}.${source.extension.ifBlank { "jpg" }}"
                            writeZipFile(zip, "$folderPrefix$name", source)
                            extraPhotoNames.add(name)
                        }
                    }

                    val vehicleExportIndex = spot.vehicleId?.let { vehicleIndexById[it] }
                    val tagNames = tagsBySpotId[spot.id].orEmpty()
                    val spotJson = spotToJson(spot, photoEntry.removePrefix(folderPrefix), extraPhotoNames, vehicleExportIndex, tagNames)
                    writeZipText(zip, "${folderPrefix}spot.json", spotJson.toString(2))
                    writeZipText(zip, "${folderPrefix}notes.txt", buildSpotNotesText(spot, tagNames))

                    // Card HTML (with the photo downscaled+base64-embedded by buildSpotCardHtml)
                    // is built right here, immediately, while photoBytes is still in scope for
                    // this one spot — not stashed in a list for the whole export to process
                    // later. The old code kept every spot's full-resolution photo bytes alive
                    // simultaneously across the entire export (in a List<SpotExportSummary>),
                    // which meant a vault with thousands of photos — exactly what years of real
                    // use produces — could OOM mid-export. Since this whole function is wrapped
                    // in runCatching, that OOM was swallowed silently: every future scheduled
                    // backup after a vault crossed that threshold would fail the same way,
                    // forever, with "Auto Backup: On" still showing in Settings while it quietly
                    // stopped protecting anything. Building the card here bounds peak memory to
                    // one photo's worth, regardless of vault size.
                    if (!spot.isTrashed()) {
                        // Bound both embeds and card markup — years of spots must not grow
                        // index.html into a multi-hundred-MB StringBuilder.
                        if (galleryCardCount < MAX_HTML_GALLERY_CARDS) {
                            val embedBytes = if (galleryCardCount < MAX_HTML_EMBEDDED_PHOTOS) photoBytes else null
                            cardsHtml.append(buildSpotCardHtml(folderName, spot, photoEntry, embedBytes)).append('\n')
                            galleryCardCount++
                        }
                        visibleSpotCount++
                    }
                }

                writeZipText(zip, "prefs.json", serializeAllPrefs(prefs).toString(2))

                writeZipText(
                    zip,
                    "index.html",
                    buildHtmlIndexShell(cardsHtml.toString(), visibleSpotCount, galleryCardCount)
                )
            }
        } ?: error("Could not open backup destination")

        spotIds.size
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

        dao.getAllSpotFingerprintRows().forEach { s ->
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
        // One photo query + one tag-assignment query — not N+1 getForSpot/getTagsForSpot per spot.
        spotPhotoDao.getAllPhotoPathRows().forEach { p ->
            feed(p.spotId.toString()); feedSep()
            feed(p.path)
            feedEnd()
        }
        tagDao.getAllSpotTagNames().forEach { t ->
            feed(t.locationId.toString()); feedSep()
            feed(t.name)
            feedEnd()
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
        // Metadata (json/txt/html/prefs) stays in RAM; photo bytes are spilled to vault_images
        // during unzip so a years-old vault can't hold every JPEG in a Map at once.
        val extractedMeta = mutableMapOf<String, ByteArray>()
        val extractedPhotoPaths = mutableMapOf<String, String>()
        var manifestJson: JSONObject? = null
        var totalBytes = 0L
        val imagesDir = backupImagesDir(context)
        var photoSpillIndex = 0

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

        context.contentResolver.openInputStream(inputUri)?.use { rawIn ->
            ZipInputStream(BufferedInputStream(rawIn)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        when {
                            entry.name == "manifest.json" -> {
                                val bytes = zip.readEntryBytesLimited(entry.name, MAX_BACKUP_META_ENTRY_BYTES)
                                totalBytes += bytes.size
                                if (totalBytes > MAX_BACKUP_TOTAL_BYTES) {
                                    throw java.io.IOException(
                                        "Backup is larger than the ${MAX_BACKUP_TOTAL_BYTES / (1024 * 1024)} MB import limit"
                                    )
                                }
                                manifestJson = JSONObject(String(bytes, Charsets.UTF_8))
                            }
                            isPhotoZipEntry(entry.name) -> {
                                val extension = entry.name.substringAfterLast('.', "jpg")
                                val outFile = File(
                                    imagesDir,
                                    "import_spill_${System.currentTimeMillis()}_${photoSpillIndex++}.$extension"
                                )
                                zip.spillEntryToFile(
                                    entryName = entry.name,
                                    destination = outFile,
                                    maxEntryBytes = MAX_BACKUP_ENTRY_BYTES
                                ) { chunk ->
                                    totalBytes += chunk
                                    if (totalBytes > MAX_BACKUP_TOTAL_BYTES) {
                                        runCatching { outFile.delete() }
                                        throw java.io.IOException(
                                            "Backup is larger than the ${MAX_BACKUP_TOTAL_BYTES / (1024 * 1024)} MB import limit"
                                        )
                                    }
                                }
                                extractedPhotoPaths[entry.name] = outFile.absolutePath
                            }
                            else -> {
                                val bytes = zip.readEntryBytesLimited(entry.name, MAX_BACKUP_META_ENTRY_BYTES)
                                totalBytes += bytes.size
                                if (totalBytes > MAX_BACKUP_TOTAL_BYTES) {
                                    throw java.io.IOException(
                                        "Backup is larger than the ${MAX_BACKUP_TOTAL_BYTES / (1024 * 1024)} MB import limit"
                                    )
                                }
                                extractedMeta[entry.name] = bytes
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
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
        val photosToDeleteAfterCommit = mutableListOf<String>()
        val imported = try {
            db.withTransaction {
                if (replaceExisting) {
                    photosToDeleteAfterCommit.addAll(dao.getAllCoverImagePaths().filter { it.isNotEmpty() })
                    photosToDeleteAfterCommit.addAll(spotPhotoDao.getAllPhotoPaths().filter { it.isNotEmpty() })
                    dao.deleteAllHistory() // cascades to spot_photos via the FK
                    vehicleDao.deleteAll()
                }

                val existingSpotSignatures = dao.getAllSpotSignatureRows()
                    .map { spotSignature(it.timestamp, it.lat, it.lng) }
                    .toMutableSet()

                val vehicleIdMap = if (extractedMeta.containsKey("vehicles.json")) {
                    importVehicles(extractedMeta.getValue("vehicles.json"), vehicleDao)
                } else {
                    emptyList()
                }

                when {
                    version >= FOLDER_FORMAT_MIN_VERSION -> importFolderBackup(
                        extractedMeta,
                        extractedPhotoPaths,
                        imagesDir,
                        dao,
                        spotPhotoDao,
                        tagDao,
                        existingSpotSignatures,
                        vehicleIdMap,
                        expectedSpotCount = manifest.optInt("spotCount", -1)
                    )
                    else -> importLegacyBackup(
                        extractedMeta,
                        extractedPhotoPaths,
                        imagesDir,
                        dao,
                        prefs,
                        existingSpotSignatures
                    )
                }
            }
        } catch (e: Exception) {
            // Transaction rolled back — spilled photos from this import attempt are orphans.
            extractedPhotoPaths.values.forEach { path -> runCatching { File(path).delete() } }
            throw e
        }

        // Transaction committed successfully — now safe to actually reclaim the old photos' disk
        // space, since there's nothing left for a later failure in this import to roll back onto.
        photosToDeleteAfterCommit.forEach { path -> runCatching { File(path).delete() } }

        importPrefs(extractedMeta, prefs, clearNonExcludedFirst = replaceExisting)
        // Recomputed from scratch rather than trusted from assignTag()'s own incremental bumps
        // above — a replaceExisting wipe cascades every pre-existing spot's cross-refs away via
        // the FK with nothing decrementing the tags they carried, so every tag's usageCount would
        // otherwise stay stuck at its pre-wipe value forever, on top of whatever the just-imported
        // spots added. Harmless (a no-op) on a merge import where existing spots/cross-refs were
        // never touched.
        db.tagDao().recomputeAllUsageCounts()
        Result.success(imported)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** name.lowercase(), or the Bluetooth MAC when set — good enough to say "same vehicle" for
     * dedup purposes without a stable cross-install id. */
    private fun vehicleIdentity(name: String, bluetoothMac: String?): String =
        bluetoothMac?.uppercase(Locale.US)?.takeIf { it.isNotBlank() } ?: name.trim().lowercase(Locale.US)

    private suspend fun importVehicles(vehiclesJsonBytes: ByteArray, vehicleDao: VehicleDao): List<Int> {
        val arr = JSONArray(String(vehiclesJsonBytes, Charsets.UTF_8))
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
        for (i in 0 until arr.length()) {
            val v = arr.getJSONObject(i)
            val name = v.optString("name", "My Car")
            val bluetoothMac = v.optString("bluetoothMac", "").ifBlank { null }
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
                    iconKey = v.optString("iconKey", "car"),
                    notes = v.optString("notes", ""),
                    isDefault = false,
                    isArchived = v.optBoolean("isArchived", false),
                    bluetoothMac = bluetoothMac,
                    bluetoothName = v.optString("bluetoothName", "").ifBlank { null },
                    createdAt = v.optLong("createdAt", System.currentTimeMillis())
                )
            ).toInt()
            if (isDefault) newDefaultId = newId
            newIds.add(newId)
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
        extractedMeta: Map<String, ByteArray>,
        extractedPhotoPaths: Map<String, String>,
        imagesDir: File,
        dao: LocationDao,
        spotPhotoDao: SpotPhotoDao,
        tagDao: TagDao,
        existingSignatures: MutableSet<String>,
        vehicleIdMap: List<Int>,
        expectedSpotCount: Int
    ): Int {
        val spotJsonPaths = extractedMeta.keys
            .filter { it.matches(Regex("spots/[^/]+/spot\\.json")) }
            .sorted()

        var imported = 0
        spotJsonPaths.forEachIndexed { index, jsonPath ->
            val folderPrefix = jsonPath.removeSuffix("spot.json")
            val item = JSONObject(String(extractedMeta.getValue(jsonPath), Charsets.UTF_8))
            val imagePath = resolveSpotPhoto(extractedMeta, extractedPhotoPaths, folderPrefix, item, imagesDir, index)
            val newSpotId = insertSpotFromJson(dao, item, imagePath, existingSignatures, vehicleIdMap)
            if (newSpotId != null) {
                imported++
                val extraNames = item.optJSONArray("extraPhotos")
                if (extraNames != null) {
                    for (i in 0 until extraNames.length()) {
                        val name = extraNames.getString(i)
                        val entryName = folderPrefix + name
                        val spilled = extractedPhotoPaths[entryName]
                        if (spilled != null) {
                            spotPhotoDao.insert(SpotPhoto(spotId = newSpotId, path = spilled))
                            continue
                        }
                        val bytes = extractedMeta[entryName] ?: continue
                        val extension = name.substringAfterLast('.', "jpg")
                        val outFile = File(imagesDir, "import_extra_${System.currentTimeMillis()}_${index}_$i.$extension")
                        outFile.writeBytes(bytes)
                        spotPhotoDao.insert(SpotPhoto(spotId = newSpotId, path = outFile.absolutePath))
                    }
                }
                // Tag names, not ids — tags are globally unique by name (see TagEntity's unique
                // index), and assignTag() already finds-or-creates by name, so this needs no id
                // remapping table the way vehicles do. Also correct for a merge import: an
                // existing tag with the same name just gets this spot added as a new member.
                val tagNames = item.optJSONArray("tags")
                if (tagNames != null) {
                    for (i in 0 until tagNames.length()) {
                        tagDao.assignTag(newSpotId, tagNames.getString(i))
                    }
                }
            }
        }

        // manifest.json's own spotCount is the source of truth for "this backup genuinely has
        // zero spots" (e.g. importing/restoring a backup taken from an empty vault — the Google
        // Drive restore path can hit exactly this) — without checking it, that valid, correctly-
        // formatted zip looked identical to someone picking a completely unrelated file with no
        // spots/ folder at all, and both used to fail with the same "not a real backup" error.
        if (imported == 0 && spotJsonPaths.isEmpty() && expectedSpotCount != 0) {
            error("Backup contains no spot folders under spots/")
        }
        return imported
    }

    private suspend fun importLegacyBackup(
        extractedMeta: Map<String, ByteArray>,
        extractedPhotoPaths: Map<String, String>,
        imagesDir: File,
        dao: LocationDao,
        prefs: SharedPreferences,
        existingSignatures: MutableSet<String>
    ): Int {
        val spotsBytes = extractedMeta["spots.json"]
            ?: error("Backup is missing spots.json")
        val backup = JSONObject(String(spotsBytes, Charsets.UTF_8))
        val spotsArray = backup.getJSONArray("spots")

        var imported = 0
        for (i in 0 until spotsArray.length()) {
            val item = spotsArray.getJSONObject(i)
            val imageEntry = item.optString("imageEntry", "")
            val imagePath = when {
                imageEntry.isEmpty() -> ""
                extractedPhotoPaths.containsKey(imageEntry) -> extractedPhotoPaths.getValue(imageEntry)
                extractedMeta.containsKey(imageEntry) -> {
                    val outFile = File(imagesDir, "import_${System.currentTimeMillis()}_$i.jpg")
                    outFile.writeBytes(extractedMeta.getValue(imageEntry))
                    outFile.absolutePath
                }
                else -> ""
            }
            if (insertSpotFromJson(dao, item, imagePath, existingSignatures, emptyList()) != null) imported++
        }

        backup.optJSONObject("prefs")?.let { prefBackup ->
            applyPrefBackup(prefBackup, prefs)
        }

        return imported
    }

    private fun resolveSpotPhoto(
        extractedMeta: Map<String, ByteArray>,
        extractedPhotoPaths: Map<String, String>,
        folderPrefix: String,
        item: JSONObject,
        imagesDir: File,
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

        candidates.firstOrNull { extractedPhotoPaths.containsKey(it) }?.let { entryName ->
            return extractedPhotoPaths.getValue(entryName)
        }

        val entryName = candidates.firstOrNull { extractedMeta.containsKey(it) }
            ?: return ""

        val extension = entryName.substringAfterLast('.', "jpg")
        val outFile = File(imagesDir, "import_${System.currentTimeMillis()}_$index.$extension")
        outFile.writeBytes(extractedMeta.getValue(entryName))
        return outFile.absolutePath
    }

    /** Returns the new spot's row id on success, or null if it was skipped as a duplicate. */
    private suspend fun insertSpotFromJson(
        dao: LocationDao,
        item: JSONObject,
        imagePath: String,
        existingSignatures: MutableSet<String>,
        vehicleIdMap: List<Int>
    ): Int? {
        val timestamp = item.optLong("timestamp", System.currentTimeMillis())
        val lat = item.optDouble("lat", 0.0)
        val lng = item.optDouble("lng", 0.0)
        val signature = spotSignature(timestamp, lat, lng)

        if (!existingSignatures.add(signature)) {
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

        val newId = dao.insertSpotAndGetId(
            LocationSpot(
                id = 0,
                imagePath = imagePath,
                locationDetails = item.optString("locationDetails", ""),
                timestamp = timestamp,
                lat = lat,
                lng = lng,
                address = item.optString("address", ""),
                isFavorite = item.optBoolean("isFavorite", false),
                title = item.optString("title", ""),
                isWishlist = item.optBoolean("isWishlist", false),
                isVisited = item.optBoolean("isVisited", false),
                deletedAt = deletedAt,
                vehicleId = vehicleId,
                city = item.optString("city", ""),
                state = item.optString("state", ""),
                isArchived = item.optBoolean("isArchived", false),
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
                is Set<*> -> { entry.put("t", "set"); entry.put("v", JSONArray(value.filterIsInstance<String>())) }
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
                    "string" -> editor.putString(key, raw.getString("v"))
                    "set" -> {
                        val arr = raw.getJSONArray("v")
                        val set = mutableSetOf<String>()
                        for (i in 0 until arr.length()) set.add(arr.getString(i))
                        editor.putStringSet(key, set)
                    }
                }
                // Backward compatible with old backups, which stored a bare string array
                // directly under "custom_categories" / "custom_tags" with no type wrapper.
                is JSONArray -> {
                    val set = mutableSetOf<String>()
                    for (i in 0 until raw.length()) set.add(raw.getString(i))
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

    private fun writeZipText(zip: ZipOutputStream, name: String, text: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(text.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun writeZipFile(zip: ZipOutputStream, name: String, source: File) {
        zip.putNextEntry(ZipEntry(name))
        source.inputStream().use { input -> input.copyTo(zip) }
        zip.closeEntry()
    }

    /**
     * Downscales a spot photo for the HTML gallery and returns a data-URI so the
     * gallery works even when index.html is opened without the spots/ folders
     * (common when people open only the HTML from a zip viewer or Downloads).
     */
    private fun photoDataUri(photoBytes: ByteArray?): String? {
        if (photoBytes == null || photoBytes.isEmpty()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size, bounds)
            val maxEdge = 960
            var sample = 1
            while (
                bounds.outWidth / sample > maxEdge * 2 ||
                bounds.outHeight / sample > maxEdge * 2
            ) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size, opts)
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

    /** One gallery card's HTML, built from a single spot's data — [photoBytes] is only ever this
     * one spot's, read fresh from disk by the caller right before this runs, never stashed
     * alongside every other spot's for the whole export (see the call site's comment). */
    private fun buildSpotCardHtml(folderName: String, spot: LocationSpot, photoEntry: String, photoBytes: ByteArray?): String {
        val title = spot.title.ifBlank { "Saved Spot" }
        val location = spot.address.ifBlank {
            String.format(Locale.US, "%.6f, %.6f", spot.lat, spot.lng)
        }
        val notes = spot.locationDetails.trim().take(MAX_HTML_NOTES_CHARS)
        val dataUri = photoDataUri(photoBytes)
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

    private fun buildHtmlIndexShell(cardsHtml: String, spotCount: Int, galleryCardCount: Int): String {
        val exportedAt = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US).format(Date())
        val galleryNote = if (galleryCardCount < spotCount) {
            """<p class="meta" style="padding:0 24px 12px">Showing $galleryCardCount of $spotCount spots here — open the spots/ folders in this zip for the rest.</p>"""
        } else {
            ""
        }
        return """
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
                <div class="meta">Exported $exportedAt · $spotCount spots · each spot folder bundles photo + notes</div>
              </header>
              $galleryNote
              <main>
                $cardsHtml
              </main>
            </body>
            </html>
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
