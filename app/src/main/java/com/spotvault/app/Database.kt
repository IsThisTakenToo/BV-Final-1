package com.spotvault.app

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "vehicles",
    indices = [Index("bluetoothMac"), Index("isDefault"), Index("isArchived")]
)
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorArgb: Int,
    val iconKey: String,
    val notes: String = "",
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val bluetoothMac: String? = null,
    val bluetoothName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "location_history",
    indices = [Index("timestamp"), Index("deletedAt"), Index("isFavorite"), Index("vehicleId"), Index("isArchived")]
)
data class LocationSpot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String,
    val locationDetails: String,
    val timestamp: Long,
    val lat: Double,
    val lng: Double,
    val address: String,
    val isFavorite: Boolean = false,
    val title: String = "",
    val isWishlist: Boolean = false,
    val isVisited: Boolean = false,
    val deletedAt: Long? = null,
    val vehicleId: Int? = null,
    val city: String = "",
    val state: String = "",
    // Hidden from the main Vault but kept forever — distinct from deletedAt's "Recently Deleted,
    // auto-purged after N days" flow. Archiving and soft-deleting are independent: a spot can in
    // principle be both, though the UI never offers that combination directly.
    val isArchived: Boolean = false,
    /** Surfaces the spot in the Vault's pinned "bitty squares" row, independent of isFavorite. */
    val isPinned: Boolean = false
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val usageCount: Int = 0
)

@Entity(
    tableName = "location_tag_cross_ref",
    primaryKeys = ["locationId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = LocationSpot::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("locationId"), Index("tagId")]
)
data class LocationTagCrossRef(
    val locationId: Int,
    val tagId: Int
)

/** A saved spot together with every tag assigned to it, joined through [LocationTagCrossRef]. */
data class LocationWithTags(
    @Embedded val spot: LocationSpot,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = LocationTagCrossRef::class,
            parentColumn = "locationId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)

/** Lightweight tag assignment row — avoids reloading every [LocationSpot] via @Relation just to
 * build a spot→tags map for Vault filtering (the Vault already collects spots separately). */
data class SpotTagAssignment(
    val locationId: Int,
    val tagId: Int,
    val name: String,
    val usageCount: Int
)

fun List<SpotTagAssignment>.toTagsBySpotId(): Map<Int, List<TagEntity>> =
    groupBy { it.locationId }.mapValues { (_, rows) ->
        rows.map { TagEntity(id = it.tagId, name = it.name, usageCount = it.usageCount) }
            .distinctBy { it.id }
    }

@Entity(
    tableName = "spot_photos",
    foreignKeys = [
        ForeignKey(
            entity = LocationSpot::class,
            parentColumns = ["id"],
            childColumns = ["spotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("spotId")]
)
data class SpotPhoto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val spotId: Int,
    val path: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles WHERE isArchived = 0 ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles ORDER BY isArchived ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Vehicle>>

    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM vehicles WHERE isArchived = 0")
    suspend fun countActive(): Int

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getById(id: Int): Vehicle?

    @Query("SELECT * FROM vehicles WHERE isDefault = 1 AND isArchived = 0 LIMIT 1")
    suspend fun getDefault(): Vehicle?

    @Query("SELECT * FROM vehicles WHERE UPPER(bluetoothMac) = UPPER(:mac) AND isArchived = 0 LIMIT 1")
    suspend fun findByBluetoothMac(mac: String): Vehicle?

    @Query("SELECT * FROM vehicles WHERE isArchived = 0 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getMostRecentActive(): Vehicle?

    @Query("SELECT * FROM vehicles WHERE isArchived = 0")
    suspend fun getActiveList(): List<Vehicle>

    @Query("SELECT * FROM vehicles")
    suspend fun getAllList(): List<Vehicle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vehicle: Vehicle): Long

    @Update
    suspend fun update(vehicle: Vehicle)

    @Query("UPDATE vehicles SET isDefault = 0")
    suspend fun clearAllDefaults()

    @Query("UPDATE vehicles SET isDefault = 1 WHERE id = :id")
    suspend fun markDefault(id: Int)

    @Transaction
    suspend fun setDefault(id: Int) {
        clearAllDefaults()
        markDefault(id)
    }

    // Deliberately does NOT clear bluetoothMac/bluetoothName — AutoParkWorker already checks
    // vehicle.isArchived on its own and skips archived vehicles, so wiping the pairing here was
    // redundant for that purpose and only served to permanently destroy it. Un-archiving used to
    // mean re-pairing from scratch even though nothing about the pairing itself had changed.
    @Query("UPDATE vehicles SET isArchived = 1, isDefault = 0 WHERE id = :id")
    suspend fun archive(id: Int)

    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM vehicles")
    suspend fun deleteAll()
}

/** Lightweight GROUP BY row for Location Browser state/city lists. */
data class NamedCount(
    val name: String,
    val count: Int
)

/** Cover path for Auto Delete hard-purge — avoids loading full notes. */
data class DeletedSpotCover(
    val id: Int,
    val imagePath: String
)

/** Coords-only row for smart dedup distance checks. */
data class SpotCoords(
    val id: Int,
    val lat: Double,
    val lng: Double
)

/** Fingerprint row: spot id ↔ tag name without loading full tag entities per spot. */
data class SpotTagNameRow(
    val locationId: Int,
    val name: String
)

/** Dedup signature without loading notes — used during backup import merge checks. */
data class SpotSignatureRow(
    val timestamp: Long,
    val lat: Double,
    val lng: Double
)

/**
 * Notes-free fingerprint projection — prefix/suffix + length catch notepad edits without
 * holding every full 50k note body in RAM at once.
 */
data class SpotFingerprintRow(
    val id: Int,
    val imagePath: String,
    val notesLength: Int,
    val notesPrefix: String,
    val notesSuffix: String,
    val timestamp: Long,
    val lat: Double,
    val lng: Double,
    val address: String,
    val isFavorite: Boolean,
    val title: String,
    val isWishlist: Boolean,
    val isVisited: Boolean,
    val deletedAt: Long?,
    val vehicleId: Int?,
    val city: String,
    val state: String,
    val isArchived: Boolean,
    val isPinned: Boolean
)

@Dao
interface LocationDao {
    @Query("SELECT * FROM location_history WHERE deletedAt IS NULL AND isArchived = 0 ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<LocationSpot>>

    /**
     * Notes-stripped active vault rows for browse UI. Only a short `locationDetails` prefix is
     * kept so list/edit previews still work without holding multi-KB notepads in RAM; full notes
     * are loaded via [getSpotById] on save.
     */
    @Query(
        """
        SELECT id, imagePath, SUBSTR(locationDetails, 1, 160) AS locationDetails, timestamp, lat, lng, address, isFavorite, title,
               isWishlist, isVisited, deletedAt, vehicleId, city, state, isArchived, isPinned
        FROM location_history
        WHERE deletedAt IS NULL AND isArchived = 0 AND isWishlist = 0
        ORDER BY timestamp DESC
        """
    )
    suspend fun getActiveVaultSpotsForBrowse(): List<LocationSpot>

    @Query(
        """
        SELECT id, imagePath, SUBSTR(locationDetails, 1, 160) AS locationDetails, timestamp, lat, lng, address, isFavorite, title,
               isWishlist, isVisited, deletedAt, vehicleId, city, state, isArchived, isPinned
        FROM location_history
        WHERE deletedAt IS NULL AND isArchived = 0 AND isWishlist = 0
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    suspend fun getActiveVaultSpotsForBrowseCapped(limit: Int): List<LocationSpot>

    /** Timestamps only — calendar month dots must not pull every spot row into RAM. */
    @Query(
        """
        SELECT timestamp FROM location_history
        WHERE deletedAt IS NULL AND isArchived = 0 AND isWishlist = 0
        """
    )
    suspend fun getActiveVaultTimestamps(): List<Long>

    @Query(
        """
        SELECT id, imagePath, SUBSTR(locationDetails, 1, 160) AS locationDetails, timestamp, lat, lng, address, isFavorite, title,
               isWishlist, isVisited, deletedAt, vehicleId, city, state, isArchived, isPinned
        FROM location_history
        WHERE deletedAt IS NULL AND isArchived = 0 AND isWishlist = 0
          AND timestamp >= :dayStart AND timestamp < :dayEnd
        ORDER BY timestamp DESC
        LIMIT 3000
        """
    )
    suspend fun getActiveVaultSpotsForDay(dayStart: Long, dayEnd: Long): List<LocationSpot>

    @Query(
        """
        SELECT id, imagePath, SUBSTR(locationDetails, 1, 160) AS locationDetails, timestamp, lat, lng, address, isFavorite, title,
               isWishlist, isVisited, deletedAt, vehicleId, city, state, isArchived, isPinned
        FROM location_history
        WHERE deletedAt IS NULL AND isArchived = 0 AND isWishlist = 0 AND isFavorite = 1
        ORDER BY timestamp DESC
        LIMIT 3000
        """
    )
    suspend fun getFavoriteVaultSpotsForBrowse(): List<LocationSpot>

    @Query(
        """
        SELECT CASE WHEN length(trim(state)) = 0 THEN 'Unknown' ELSE state END AS name,
               COUNT(*) AS count
        FROM location_history
        WHERE deletedAt IS NULL AND isArchived = 0 AND isWishlist = 0
        GROUP BY CASE WHEN length(trim(state)) = 0 THEN 'Unknown' ELSE state END
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    suspend fun getActiveStateCounts(): List<NamedCount>

    @Query(
        """
        SELECT CASE WHEN length(trim(city)) = 0 THEN 'Unknown' ELSE city END AS name,
               COUNT(*) AS count
        FROM location_history
        WHERE deletedAt IS NULL AND isArchived = 0 AND isWishlist = 0
          AND CASE WHEN length(trim(state)) = 0 THEN 'Unknown' ELSE state END = :state
        GROUP BY CASE WHEN length(trim(city)) = 0 THEN 'Unknown' ELSE city END
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    suspend fun getActiveCityCounts(state: String): List<NamedCount>

    @Query(
        """
        SELECT id, imagePath, SUBSTR(locationDetails, 1, 160) AS locationDetails, timestamp, lat, lng, address, isFavorite, title,
               isWishlist, isVisited, deletedAt, vehicleId, city, state, isArchived, isPinned
        FROM location_history
        WHERE deletedAt IS NULL AND isArchived = 0 AND isWishlist = 0
          AND CASE WHEN length(trim(state)) = 0 THEN 'Unknown' ELSE state END = :state
          AND CASE WHEN length(trim(city)) = 0 THEN 'Unknown' ELSE city END = :city
        ORDER BY timestamp DESC
        LIMIT 3000
        """
    )
    suspend fun getActiveVaultSpotsForCity(state: String, city: String): List<LocationSpot>

    @Query(
        """
        SELECT * FROM location_history
        WHERE deletedAt IS NULL AND isArchived = 0
          AND city = '' AND state = ''
          AND (lat != 0 OR lng != 0)
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    suspend fun getSpotsNeedingAddressResolution(limit: Int): List<LocationSpot>

    @Query("SELECT COUNT(*) FROM location_history")
    suspend fun countAllSpotsIncludingDeleted(): Int

    @Query("SELECT timestamp, lat, lng FROM location_history")
    suspend fun getAllSpotSignatureRows(): List<SpotSignatureRow>

    @Query("SELECT id FROM location_history ORDER BY timestamp DESC")
    suspend fun getAllSpotIdsOrdered(): List<Int>

    @Query(
        """
        SELECT id, imagePath,
               length(locationDetails) AS notesLength,
               substr(locationDetails, 1, 64) AS notesPrefix,
               CASE WHEN length(locationDetails) <= 64 THEN ''
                    ELSE substr(locationDetails, length(locationDetails) - 63, 64) END AS notesSuffix,
               timestamp, lat, lng, address, isFavorite, title, isWishlist, isVisited, deletedAt,
               vehicleId, city, state, isArchived, isPinned
        FROM location_history
        ORDER BY id ASC
        """
    )
    suspend fun getAllSpotFingerprintRows(): List<SpotFingerprintRow>

    @Query(
        """
        SELECT id, imagePath, SUBSTR(locationDetails, 1, 160) AS locationDetails, timestamp, lat, lng, address, isFavorite, title,
               isWishlist, isVisited, deletedAt, vehicleId, city, state, isArchived, isPinned
        FROM location_history
        WHERE deletedAt IS NULL AND isArchived = 0 AND isWishlist = 0
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    suspend fun getActiveVaultSpotsNewestPrefix(limit: Int): List<LocationSpot>

    @Query(
        """
        SELECT id, imagePath, SUBSTR(locationDetails, 1, 160) AS locationDetails, timestamp, lat, lng, address, isFavorite, title,
               isWishlist, isVisited, deletedAt, vehicleId, city, state, isArchived, isPinned
        FROM location_history
        WHERE deletedAt IS NULL AND isArchived = 0 AND isWishlist = 0
        ORDER BY timestamp ASC
        LIMIT :limit
        """
    )
    suspend fun getActiveVaultSpotsOldestPrefix(limit: Int): List<LocationSpot>

    @Query(
        """
        SELECT id, imagePath, SUBSTR(locationDetails, 1, 160) AS locationDetails, timestamp, lat, lng, address, isFavorite, title,
               isWishlist, isVisited, deletedAt, vehicleId, city, state, isArchived, isPinned
        FROM location_history
        WHERE deletedAt IS NULL AND isArchived = 0 AND isWishlist = 0 AND isPinned = 1
        ORDER BY timestamp DESC
        """
    )
    suspend fun getPinnedVaultSpotsForBrowse(): List<LocationSpot>

    /** Live single-spot observation — the full-screen viewer uses this so a photo attached from
     * its own "Add Photo" button (imagePath goes from blank to set) shows up immediately instead
     * of only after the screen is reopened. */
    @Query("SELECT * FROM location_history WHERE id = :spotId LIMIT 1")
    fun observeSpotById(spotId: Int): Flow<LocationSpot?>

    @Query("SELECT * FROM location_history WHERE id = :spotId LIMIT 1")
    suspend fun getSpotById(spotId: Int): LocationSpot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpot(spot: LocationSpot)

    /** Same insert, but returns the generated row id — needed when a caller (backup import)
     * has to attach follow-up rows, like extra photos, to the spot it just created. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpotAndGetId(spot: LocationSpot): Long

    @Query("SELECT * FROM location_history ORDER BY timestamp DESC")
    suspend fun getAllHistoryIncludingDeleted(): List<LocationSpot>

    /** Cover JPEG paths only — used by orphan-photo sweep (avoids loading full spot rows + notes). */
    @Query("SELECT imagePath FROM location_history WHERE length(imagePath) > 0")
    suspend fun getAllCoverImagePaths(): List<String>

    /** Cover paths for Clear All (active + soft-deleted; archived kept forever). */
    @Query("SELECT imagePath FROM location_history WHERE isArchived = 0 AND length(imagePath) > 0")
    suspend fun getCoverImagePathsForClearAll(): List<String>

    @Update
    suspend fun updateSpot(spot: LocationSpot)

    @Delete
    suspend fun deleteSpot(spot: LocationSpot)
    
    @Query("DELETE FROM location_history")
    suspend fun deleteAllHistory()

    // Used by "Clear All Vault Data" — archived spots are exempt so the Danger Zone can't
    // silently break the archive feature's "kept forever" promise. Backup-restore's full wipe
    // (VaultBackupManager) deliberately uses deleteAllHistory() instead, since that's an explicit
    // opt-in "replace my whole vault" operation where archived spots can come back from the backup.
    @Query("DELETE FROM location_history WHERE isArchived = 0")
    suspend fun deleteAllNonArchivedHistory()

    @Query("DELETE FROM location_history WHERE isFavorite = 0")
    suspend fun deleteNonFavoriteHistory()

    // deletedAt IS NULL matters here — without it, "Saved Spots" in Vault Snapshot kept counting
    // spots the user had already moved to Recently Deleted (and for up to 30 more days after
    // Auto Delete hard-purged them), overstating the count against what the Vault list itself
    // actually shows.
    @Query("SELECT COUNT(*) FROM location_history WHERE isWishlist = 0 AND deletedAt IS NULL AND isArchived = 0")
    suspend fun countNonWishlistSpots(): Int

    @Query("SELECT * FROM location_history WHERE deletedAt IS NULL AND isArchived = 0 ORDER BY timestamp DESC")
    suspend fun getHistoryList(): List<LocationSpot>

    @Query("SELECT * FROM location_history WHERE deletedAt IS NULL AND isArchived = 0 AND isWishlist = 0 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentVaultSpots(limit: Int = 5): List<LocationSpot>

    @Query(
        """
        SELECT * FROM location_history
        WHERE deletedAt IS NULL AND isArchived = 0 AND isFavorite = 1 AND isWishlist = 0
        ORDER BY timestamp DESC
        """
    )
    suspend fun getFavoriteSpots(): List<LocationSpot>

    /** Widget-only bound — Glance cannot safely materialize unbounded favorite rows + thumbnails. */
    @Query(
        """
        SELECT * FROM location_history
        WHERE deletedAt IS NULL AND isArchived = 0 AND isFavorite = 1 AND isWishlist = 0
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    suspend fun getFavoriteSpotsNewest(limit: Int): List<LocationSpot>

    @Query(
        """
        SELECT * FROM location_history
        WHERE deletedAt IS NULL AND isArchived = 0 AND isFavorite = 1 AND isWishlist = 0
        ORDER BY timestamp ASC
        LIMIT :limit
        """
    )
    suspend fun getFavoriteSpotsOldest(limit: Int): List<LocationSpot>

    @Query(
        """
        SELECT id, imagePath, SUBSTR(locationDetails, 1, 160) AS locationDetails, timestamp, lat, lng, address, isFavorite, title,
               isWishlist, isVisited, deletedAt, vehicleId, city, state, isArchived, isPinned
        FROM location_history
        WHERE deletedAt IS NOT NULL
        ORDER BY deletedAt DESC
        LIMIT 3000
        """
    )
    suspend fun getRecentlyDeleted(): List<LocationSpot>

    @Query("SELECT COUNT(*) FROM location_history WHERE deletedAt IS NOT NULL")
    suspend fun countRecentlyDeleted(): Int

    @Query("UPDATE location_history SET deletedAt = :now WHERE id = :spotId")
    suspend fun softDeleteSpot(spotId: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE location_history SET deletedAt = NULL WHERE id = :spotId")
    suspend fun restoreSpot(spotId: Int)

    /** Undo-safe restore — no-ops if the row was permanently purged or already restored. */
    suspend fun restoreSpotIfSoftDeleted(spotId: Int): Boolean {
        val spot = getSpotById(spotId) ?: return false
        if (spot.deletedAt == null) return false
        restoreSpot(spotId)
        return true
    }

    @Query("UPDATE location_history SET deletedAt = NULL WHERE deletedAt IS NOT NULL")
    suspend fun restoreAllDeleted()

    // Archived spots are hidden from the main Vault but kept forever (no auto-purge), unlike
    // deletedAt's "Recently Deleted" flow.
    @Query(
        """
        SELECT id, imagePath, SUBSTR(locationDetails, 1, 160) AS locationDetails, timestamp, lat, lng, address, isFavorite, title,
               isWishlist, isVisited, deletedAt, vehicleId, city, state, isArchived, isPinned
        FROM location_history
        WHERE isArchived = 1
        ORDER BY timestamp DESC
        LIMIT 3000
        """
    )
    suspend fun getArchivedSpots(): List<LocationSpot>

    @Query("UPDATE location_history SET isArchived = 1 WHERE id = :spotId")
    suspend fun archiveSpot(spotId: Int)

    @Query("UPDATE location_history SET isArchived = 0 WHERE id = :spotId")
    suspend fun unarchiveSpot(spotId: Int)

    /** Undo-safe unarchive — no-ops if the row was deleted forever or is no longer archived. */
    suspend fun unarchiveSpotIfArchived(spotId: Int): Boolean {
        val spot = getSpotById(spotId) ?: return false
        if (!spot.isArchived) return false
        unarchiveSpot(spotId)
        return true
    }

    @Query("UPDATE location_history SET isArchived = 0 WHERE isArchived = 1")
    suspend fun unarchiveAllSpots()

    @Query(
        """
        SELECT id, imagePath FROM location_history
        WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff
        """
    )
    suspend fun getDeletedCoverPathsOlderThan(cutoff: Long): List<DeletedSpotCover>

    @Query(
        """
        SELECT p.path FROM spot_photos AS p
        INNER JOIN location_history AS s ON s.id = p.spotId
        WHERE s.deletedAt IS NOT NULL AND s.deletedAt < :cutoff
        """
    )
    suspend fun getExtraPhotoPathsDeletedOlderThan(cutoff: Long): List<String>

    @Query("DELETE FROM location_history WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeDeletedOlderThan(cutoff: Long)

    @Query("UPDATE location_history SET vehicleId = NULL WHERE vehicleId = :vehicleId")
    suspend fun clearVehicleId(vehicleId: Int)

    @Query("UPDATE location_history SET isPinned = :pinned WHERE id = :spotId")
    suspend fun setPinned(spotId: Int, pinned: Boolean)

    /** Atomic flip — rapid taps before the Flow recomposes cannot apply the same polarity twice. */
    @Query("UPDATE location_history SET isPinned = NOT isPinned WHERE id = :spotId")
    suspend fun togglePinned(spotId: Int)

    // A single-column UPDATE, not updateSpot(item.copy(isFavorite = ...)) — the swipe/card
    // favorite toggle only ever had a possibly-stale in-memory LocationSpot to copy() from (the
    // list's last recomposition), and copy()-ing a stale snapshot silently wrote every one of its
    // other columns back too — including imagePath, which is exactly how a photo attached
    // moments earlier could vanish again the next time someone tapped the favorite star.
    @Query("UPDATE location_history SET isFavorite = :favorite WHERE id = :spotId")
    suspend fun setFavorite(spotId: Int, favorite: Boolean)

    @Query("UPDATE location_history SET isFavorite = NOT isFavorite WHERE id = :spotId")
    suspend fun toggleFavorite(spotId: Int)

    @Query(
        """
        SELECT id FROM location_history
        WHERE timestamp < :cutoff
          AND deletedAt IS NULL
          AND isFavorite = 0
          AND isWishlist = 0
          AND isArchived = 0
        """
    )
    suspend fun getAutoDeleteCandidateIds(cutoff: Long): List<Int>

    @Query(
        """
        SELECT id, lat, lng FROM location_history
        WHERE deletedAt IS NULL AND isArchived = 0 AND isWishlist = 0 AND timestamp >= :sinceTimestamp
        """
    )
    suspend fun getActiveSpotCoordsSince(sinceTimestamp: Long): List<SpotCoords>
}

@Dao
interface TagDao {
    /** The user's most-assigned tags, for the Vault's quick-filter chip row. */
    @Query("SELECT * FROM tags ORDER BY usageCount DESC, name COLLATE NOCASE ASC LIMIT 5")
    fun getTopTags(): Flow<List<TagEntity>>

    /** Every tag, A-Z, for the full tag-cloud filter sheet. */
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Transaction
    @Query("SELECT * FROM location_history WHERE deletedAt IS NULL AND isArchived = 0 ORDER BY timestamp DESC")
    fun getAllLocationsWithTags(): Flow<List<LocationWithTags>>

    /** Active-vault tag assignments only — no embedded spot payload (see [SpotTagAssignment]). */
    @Query(
        """
        SELECT ref.locationId AS locationId, tags.id AS tagId, tags.name AS name, tags.usageCount AS usageCount
        FROM location_tag_cross_ref AS ref
        INNER JOIN tags ON tags.id = ref.tagId
        INNER JOIN location_history AS spot ON spot.id = ref.locationId
        WHERE spot.deletedAt IS NULL AND spot.isArchived = 0 AND spot.isWishlist = 0
        ORDER BY tags.name COLLATE NOCASE ASC
        """
    )
    fun observeActiveSpotTagAssignments(): Flow<List<SpotTagAssignment>>

    @Query(
        """
        SELECT ref.locationId AS locationId, tags.name AS name
        FROM location_tag_cross_ref AS ref
        INNER JOIN tags ON tags.id = ref.tagId
        ORDER BY ref.locationId ASC, tags.name COLLATE NOCASE ASC
        """
    )
    suspend fun getAllSpotTagNames(): List<SpotTagNameRow>

    /** One-shot spot list for the Tag Filter widget's selected tags — every spot carrying at
     * least one of [tagIds], deduplicated (DISTINCT) so a spot matching more than one selected
     * tag doesn't appear twice. Used by the widget, which (like the other widgets) does a plain
     * suspend query on refresh rather than collecting a Flow. */
    @Query(
        """
        SELECT DISTINCT location_history.* FROM location_history
        INNER JOIN location_tag_cross_ref ON location_history.id = location_tag_cross_ref.locationId
        WHERE location_tag_cross_ref.tagId IN (:tagIds) AND location_history.deletedAt IS NULL AND location_history.isArchived = 0
        ORDER BY location_history.timestamp DESC
        """
    )
    suspend fun getSpotsForTags(tagIds: List<Int>): List<LocationSpot>

    @Query(
        """
        SELECT DISTINCT location_history.* FROM location_history
        INNER JOIN location_tag_cross_ref ON location_history.id = location_tag_cross_ref.locationId
        WHERE location_tag_cross_ref.tagId IN (:tagIds) AND location_history.deletedAt IS NULL AND location_history.isArchived = 0
        ORDER BY location_history.timestamp DESC
        LIMIT :limit
        """
    )
    suspend fun getSpotsForTagsNewest(tagIds: List<Int>, limit: Int): List<LocationSpot>

    @Query(
        """
        SELECT DISTINCT location_history.* FROM location_history
        INNER JOIN location_tag_cross_ref ON location_history.id = location_tag_cross_ref.locationId
        WHERE location_tag_cross_ref.tagId IN (:tagIds) AND location_history.deletedAt IS NULL AND location_history.isArchived = 0
        ORDER BY location_history.timestamp ASC
        LIMIT :limit
        """
    )
    suspend fun getSpotsForTagsOldest(tagIds: List<Int>, limit: Int): List<LocationSpot>

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllTagsList(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id = :tagId LIMIT 1")
    suspend fun getTagById(tagId: Int): TagEntity?

    /** Live tag list for one spot — the Edit dialog's Tags section. */
    @Query(
        """
        SELECT tags.* FROM tags
        INNER JOIN location_tag_cross_ref ON tags.id = location_tag_cross_ref.tagId
        WHERE location_tag_cross_ref.locationId = :spotId
        ORDER BY tags.name COLLATE NOCASE ASC
        """
    )
    fun getTagsForSpotFlow(spotId: Int): Flow<List<TagEntity>>

    /** One-shot version of [getTagsForSpotFlow] — used by backup export, which runs outside
     * Compose and just needs a single snapshot per spot, not an ongoing subscription. */
    @Query(
        """
        SELECT tags.* FROM tags
        INNER JOIN location_tag_cross_ref ON tags.id = location_tag_cross_ref.tagId
        WHERE location_tag_cross_ref.locationId = :spotId
        ORDER BY tags.name COLLATE NOCASE ASC
        """
    )
    suspend fun getTagsForSpot(spotId: Int): List<TagEntity>

    /** Creates a tag with zero spots attached yet — used by "+ New Tag" entry points (the tag
     * filter sheet, the tag manager) where the user wants a tag to exist before assigning it to
     * anything, unlike [assignTag] which always creates-and-attaches in one step. */
    @Transaction
    suspend fun createTag(name: String): TagEntity? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        findByName(trimmed)?.let { return it }
        val id = insertTag(TagEntity(name = trimmed)).toInt()
        return TagEntity(id = id, name = trimmed)
    }

    @Query("UPDATE tags SET name = :newName WHERE id = :tagId")
    suspend fun renameTagRaw(tagId: Int, newName: String)

    /** Renaming to a name that collides with an existing tag merges into it instead of leaving
     * two tags with the same name — every cross-ref pointing at the old tag id is repointed to
     * the surviving one first (its own NOT IN guard skips a spot already tagged both ways, rather
     * than duplicating that cross-ref), the survivor's usageCount is recomputed from its actual
     * remaining cross-refs (not summed from the two old counts — a spot tagged both ways before
     * the merge doesn't gain a second cross-ref, so summing overstated how many distinct spots
     * actually carry the merged tag), then the old row is deleted (cascading its now-empty
     * cross-refs). */
    @Transaction
    suspend fun renameTag(tagId: Int, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val existing = findByName(trimmed)
        if (existing == null || existing.id == tagId) {
            renameTagRaw(tagId, trimmed)
            return
        }
        mergeTagCrossRefs(fromTagId = tagId, toTagId = existing.id)
        deleteTag(tagId)
        setUsageCount(existing.id, countCrossRefsForTag(existing.id))
    }

    @Query("SELECT COUNT(*) FROM location_tag_cross_ref WHERE tagId = :tagId")
    suspend fun countCrossRefsForTag(tagId: Int): Int

    @Query("UPDATE tags SET usageCount = :count WHERE id = :tagId")
    suspend fun setUsageCount(tagId: Int, count: Int)

    /** Recomputes every tag's usageCount from its actual surviving cross-refs, rather than
     * trusting the hand-maintained counter — permanently deleting a spot (Delete Forever, Clear
     * All Vault Data, backup restore's wipe, the 30-day Recently Deleted auto-purge) cascades its
     * location_tag_cross_ref rows away via the FK, but nothing decrements the tags it carried, so
     * usageCount only ever climbed, never fell, on every one of those paths. Call after any bulk
     * or individual permanent spot deletion — cheap even called liberally, since the tags table is
     * never going to be large. */
    @Query(
        """
        UPDATE tags SET usageCount = (
            SELECT COUNT(*) FROM location_tag_cross_ref WHERE location_tag_cross_ref.tagId = tags.id
        )
        """
    )
    suspend fun recomputeAllUsageCounts()

    @Query("UPDATE location_tag_cross_ref SET tagId = :toTagId WHERE tagId = :fromTagId AND locationId NOT IN (SELECT locationId FROM location_tag_cross_ref WHERE tagId = :toTagId)")
    suspend fun mergeTagCrossRefs(fromTagId: Int, toTagId: Int)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: Int)

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("UPDATE tags SET usageCount = usageCount + 1 WHERE id = :tagId")
    suspend fun incrementUsage(tagId: Int)

    @Query("UPDATE tags SET usageCount = MAX(usageCount - 1, 0) WHERE id = :tagId")
    suspend fun decrementUsage(tagId: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(ref: LocationTagCrossRef): Long

    @Query("DELETE FROM location_tag_cross_ref WHERE locationId = :locationId AND tagId = :tagId")
    suspend fun deleteCrossRef(locationId: Int, tagId: Int): Int

    /** Assigns a (possibly brand-new) tag by name to a spot, creating the tag row if needed and
     * bumping its usage count — but only once per spot/tag pair, since the cross-ref insert is
     * silently ignored on a duplicate and usage is only bumped when that insert actually landed. */
    @Transaction
    suspend fun assignTag(locationId: Int, tagName: String) {
        val trimmed = tagName.trim()
        if (trimmed.isEmpty()) return
        val tagId = findByName(trimmed)?.id ?: insertTag(TagEntity(name = trimmed)).toInt()
        val inserted = insertCrossRef(LocationTagCrossRef(locationId, tagId))
        if (inserted != -1L) {
            incrementUsage(tagId)
        }
    }

    @Transaction
    suspend fun removeTag(locationId: Int, tagId: Int) {
        if (deleteCrossRef(locationId, tagId) > 0) {
            decrementUsage(tagId)
        }
    }
}

@Dao
interface SpotPhotoDao {
    @Query("SELECT * FROM spot_photos WHERE spotId = :spotId ORDER BY createdAt ASC")
    fun observeForSpot(spotId: Int): Flow<List<SpotPhoto>>

    @Query("SELECT * FROM spot_photos WHERE spotId = :spotId ORDER BY createdAt ASC")
    suspend fun getForSpot(spotId: Int): List<SpotPhoto>

    @Query("SELECT * FROM spot_photos ORDER BY spotId ASC, id ASC")
    suspend fun getAllPhotos(): List<SpotPhoto>

    @Query("SELECT path FROM spot_photos")
    suspend fun getAllPhotoPaths(): List<String>

    @Query(
        """
        SELECT p.path FROM spot_photos AS p
        INNER JOIN location_history AS s ON s.id = p.spotId
        WHERE s.isArchived = 0
        """
    )
    suspend fun getPhotoPathsForClearAll(): List<String>

    @Insert
    suspend fun insert(photo: SpotPhoto): Long

    @Query("DELETE FROM spot_photos WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Database(
    entities = [
        LocationSpot::class, Vehicle::class, SpotPhoto::class,
        TagEntity::class, LocationTagCrossRef::class
    ],
    version = 18,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun spotPhotoDao(): SpotPhotoDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS track_points")
                db.execSQL("DROP TABLE IF EXISTS tracks")
                db.execSQL("DROP TABLE IF EXISTS offline_map_areas")
            }
        }

        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS vehicles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        colorArgb INTEGER NOT NULL,
                        iconKey TEXT NOT NULL,
                        notes TEXT NOT NULL DEFAULT '',
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        isArchived INTEGER NOT NULL DEFAULT 0,
                        bluetoothMac TEXT,
                        bluetoothName TEXT,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vehicles_bluetoothMac ON vehicles(bluetoothMac)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vehicles_isDefault ON vehicles(isDefault)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vehicles_isArchived ON vehicles(isArchived)")
                db.execSQL("ALTER TABLE location_history ADD COLUMN vehicleId INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_location_history_vehicleId ON location_history(vehicleId)")
            }
        }

        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS spot_photos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        spotId INTEGER NOT NULL,
                        path TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(spotId) REFERENCES location_history(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_spot_photos_spotId ON spot_photos(spotId)")
            }
        }

        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE location_history ADD COLUMN city TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE location_history ADD COLUMN state TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE location_history
                    SET category = 'Quick Actions'
                    WHERE lower(category) IN ('quick pin', 'quick track')
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tags (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        usageCount INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags(name)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS location_tag_cross_ref (
                        locationId INTEGER NOT NULL,
                        tagId INTEGER NOT NULL,
                        PRIMARY KEY(locationId, tagId),
                        FOREIGN KEY(locationId) REFERENCES location_history(id) ON DELETE CASCADE,
                        FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_location_tag_cross_ref_locationId ON location_tag_cross_ref(locationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_location_tag_cross_ref_tagId ON location_tag_cross_ref(tagId)")
            }
        }

        val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE location_history ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_location_history_isArchived ON location_history(isArchived)")
            }
        }

        val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE location_history ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Drops the vestigial "location_archive" table backing the now-deleted ArchivedSpot
        // entity — archiving has actually worked via LocationSpot.isArchived on location_history
        // for a while now; this table was dead weight nothing ever wrote to. A plain DROP TABLE
        // rather than fallbackToDestructiveMigration, so real data in every other table (spots,
        // vehicles, tags, photos) survives the upgrade untouched.
        val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS location_archive")
            }
        }

        // Drops the vestigial "category" column — the free-text label ("Drop Pinned", "Quick
        // Actions", or whatever was picked at save time) that used to render as a subtitle under
        // every Vault card's title. It was never editable after the fact (the Edit dialog never
        // had a field for it), so a spot saved through any zero-tap shortcut was stuck with a
        // generic placeholder forever. Removed everywhere it was surfaced (Vault cards, widgets,
        // search, GPX/backup) rather than just hidden. SQLite has no direct DROP COLUMN on the
        // API levels this app still supports, so this rebuilds the table the standard way: create
        // the new shape, copy every other column over, drop the old table, rename, then recreate
        // the indices Room expects to already exist on the renamed table.
        val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE location_history_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        imagePath TEXT NOT NULL,
                        locationDetails TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        lat REAL NOT NULL,
                        lng REAL NOT NULL,
                        address TEXT NOT NULL,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        title TEXT NOT NULL DEFAULT '',
                        isWishlist INTEGER NOT NULL DEFAULT 0,
                        isVisited INTEGER NOT NULL DEFAULT 0,
                        deletedAt INTEGER,
                        vehicleId INTEGER,
                        city TEXT NOT NULL DEFAULT '',
                        state TEXT NOT NULL DEFAULT '',
                        isArchived INTEGER NOT NULL DEFAULT 0,
                        isPinned INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO location_history_new (
                        id, imagePath, locationDetails, timestamp, lat, lng, address,
                        isFavorite, title, isWishlist, isVisited, deletedAt, vehicleId,
                        city, state, isArchived, isPinned
                    )
                    SELECT
                        id, imagePath, locationDetails, timestamp, lat, lng, address,
                        isFavorite, title, isWishlist, isVisited, deletedAt, vehicleId,
                        city, state, isArchived, isPinned
                    FROM location_history
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE location_history")
                db.execSQL("ALTER TABLE location_history_new RENAME TO location_history")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_location_history_timestamp ON location_history(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_location_history_deletedAt ON location_history(deletedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_location_history_isFavorite ON location_history(isFavorite)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_location_history_vehicleId ON location_history(vehicleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_location_history_isArchived ON location_history(isArchived)")
            }
        }

        val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Browse filters always use deletedAt + isArchived + isWishlist + timestamp/pinned.
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_location_history_browse ON location_history(deletedAt, isArchived, isWishlist, timestamp)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_location_history_isPinned ON location_history(isPinned)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            // The inner check matters: without it, two threads that both observe INSTANCE == null
            // before either enters the synchronized block would — once serialized by the lock —
            // each build and assign their own AppDatabase, with the second silently replacing the
            // first. Anything already holding a reference to (or a Flow from) the first instance
            // — e.g. a DAO Flow collected moments earlier on the main thread — would stop seeing
            // writes made through the second, since Room's InvalidationTracker is per-instance.
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spotvault_database"
                ).addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18)
                    // Destructive fallback only on a *downgrade* (schema version decreases —
                    // realistically only a dev/debug scenario, never an organic user update). A
                    // forward schema bump with no matching Migration now crashes loudly instead
                    // of silently wiping every saved spot in production — the crash gets caught
                    // in testing; the silent data loss wouldn't be caught until a user reported it.
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
                    .also { db ->
                        // Widgets (the Premium one's "Recent" list especially) only ever repaint
                        // when explicitly told to — a title/category/favorite edit made in-app
                        // has no reason to know a widget even exists, so it never called that
                        // refresh itself and an already-placed widget just showed stale data
                        // until its next 30-minute periodic update. Hooking Room's own
                        // change-tracking instead of hunting down every dao.updateSpot() call
                        // site covers this (and any future one) automatically.
                        val appContext = context.applicationContext
                        db.invalidationTracker.addObserver(
                            // "tags"/"location_tag_cross_ref" added for the Tag Filter widget —
                            // same reasoning as "location_history" above: assigning/removing a
                            // tag anywhere in the app has no reason to know that widget exists.
                            object : InvalidationTracker.Observer("location_history", "tags", "location_tag_cross_ref") {
                                override fun onInvalidated(tables: Set<String>) {
                                    // Debounced — Vault edits can invalidate these tables many
                                    // times in one user action; the full widget refresh chain is
                                    // expensive and doesn't need to restart on every write.
                                    WidgetThemeHelper.refreshAllWidgetsDebounced(appContext)
                                }
                            }
                        )
                    }
            }
        }
    }
}
