package com.spotvault.app

import androidx.room.Room
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Regression coverage for the tag usageCount drift bug: permanently deleting a tagged spot
 * cascades its location_tag_cross_ref rows away via the FK, but nothing decrements the tag's
 * hand-maintained usageCount counter on its own — it only ever climbed, never fell, until
 * [TagDao.recomputeAllUsageCounts] (wired into every hard-delete path) was added to derive the
 * true count from surviving cross-refs instead of trusting the counter.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TagUsageCountDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var tagDao: TagDao
    private lateinit var locationDao: LocationDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tagDao = db.tagDao()
        locationDao = db.locationDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sampleSpot(): LocationSpot = LocationSpot(
        imagePath = "",
        locationDetails = "",
        timestamp = 1_700_000_000_000L,
        lat = 37.7749,
        lng = -122.4194,
        address = "123 Market St"
    )

    @Test
    fun hardDeletingTaggedSpot_leavesCountStale_untilRecompute() = runTest {
        val spotId = locationDao.insertSpotAndGetId(sampleSpot()).toInt()
        val tag = tagDao.createTag("Garage")!!
        tagDao.assignTag(spotId, "Garage")
        assertEquals(1, tagDao.getTagById(tag.id)?.usageCount)

        // Hard-delete cascades the cross-ref away via the FK, but usageCount has no trigger of
        // its own tied to that cascade — it stays stale at 1 until something explicitly recomputes it.
        locationDao.deleteSpot(locationDao.getSpotById(spotId)!!)
        assertEquals(0, tagDao.countCrossRefsForTag(tag.id))
        assertEquals(1, tagDao.getTagById(tag.id)?.usageCount)

        tagDao.recomputeAllUsageCounts()
        assertEquals(0, tagDao.getTagById(tag.id)?.usageCount)
    }

    @Test
    fun recomputeAllUsageCounts_leavesUntouchedTagsAlone() = runTest {
        val spotId = locationDao.insertSpotAndGetId(sampleSpot()).toInt()
        val kept = tagDao.createTag("Kept")!!
        tagDao.assignTag(spotId, "Kept")

        tagDao.recomputeAllUsageCounts()

        assertEquals(1, tagDao.getTagById(kept.id)?.usageCount)
    }

    @Test
    fun renameTag_mergingIntoExisting_countsDistinctSpotsNotSumOfBothTags() = runTest {
        // spot1 carries only "garage"; spot2 already carries both "garage" and "home" — so after
        // merging "garage" into "home", exactly 2 distinct spots carry "home" (spot1 newly moved,
        // spot2 already there) — not 3, which naively summing usageCount(garage)=2 +
        // usageCount(home)=1 would have produced.
        val spot1 = locationDao.insertSpotAndGetId(sampleSpot()).toInt()
        val spot2 = locationDao.insertSpotAndGetId(sampleSpot()).toInt()
        val garage = tagDao.createTag("garage")!!
        tagDao.createTag("home")

        tagDao.assignTag(spot1, "garage")
        tagDao.assignTag(spot2, "garage")
        tagDao.assignTag(spot2, "home")

        tagDao.renameTag(garage.id, "home")

        val merged = tagDao.findByName("home")
        assertEquals(2, merged?.usageCount)
        // The old "garage" row is gone entirely, not left behind at zero.
        assertEquals(null, tagDao.findByName("garage"))
    }

    @Test
    fun getSpotsForTags_deduplicatesSpotMatchingMultipleSelectedTags() = runTest {
        val spot1 = locationDao.insertSpotAndGetId(sampleSpot()).toInt()
        val spot2 = locationDao.insertSpotAndGetId(sampleSpot()).toInt()
        val work = tagDao.createTag("work")!!
        val urgent = tagDao.createTag("urgent")!!

        tagDao.assignTag(spot1, "work")
        tagDao.assignTag(spot1, "urgent") // spot1 carries both selected tags
        tagDao.assignTag(spot2, "work")

        val results = tagDao.getSpotsForTags(listOf(work.id, urgent.id))

        assertEquals(2, results.size) // spot1 once, not twice, despite matching both tags
        assertEquals(setOf(spot1, spot2), results.map { it.id }.toSet())
    }
}
