package com.spotvault.app

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "location_history")
data class LocationSpot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String,
    val locationDetails: String,
    val category: String,
    val timestamp: Long,
    val lat: Double,
    val lng: Double,
    val address: String,
    val isFavorite: Boolean = false,
    val title: String = "",
    val isWishlist: Boolean = false,
    val isVisited: Boolean = false
)

@Entity(tableName = "location_archive")
data class ArchivedSpot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String,
    val locationDetails: String,
    val category: String,
    val timestamp: Long,
    val lat: Double,
    val lng: Double,
    val address: String,
    val isFavorite: Boolean = false,
    val title: String = "",
    val isWishlist: Boolean = false,
    val isVisited: Boolean = false,
    val archivedAt: Long = System.currentTimeMillis()
)

@Dao
interface LocationDao {
    @Query("SELECT * FROM location_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<LocationSpot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpot(spot: LocationSpot)

    @Update
    suspend fun updateSpot(spot: LocationSpot)

    @Delete
    suspend fun deleteSpot(spot: LocationSpot)
    
    @Query("DELETE FROM location_history")
    suspend fun deleteAllHistory()

    @Query("DELETE FROM location_history WHERE isFavorite = 0")
    suspend fun deleteNonFavoriteHistory()

    @Query("SELECT * FROM location_history ORDER BY timestamp DESC")
    suspend fun getHistoryList(): List<LocationSpot>

    // Archive
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchive(spot: ArchivedSpot)

    @Query("SELECT * FROM location_history WHERE timestamp < :timeThreshold")
    suspend fun getSpotsOlderThan(timeThreshold: Long): List<LocationSpot>
    
    @Query("DELETE FROM location_history WHERE id = :id")
    suspend fun deleteSpotById(id: Int)
}

@Database(entities = [LocationSpot::class, ArchivedSpot::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spotvault_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
