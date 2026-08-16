package com.spotvault.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class VaultBackupNotesTest {

    @Test
    fun buildSpotNotesText_preservesMultilineNotes() {
        val multilineNotes = "Arrived at north lot.\n\nGate code: 4521\nPark near the blue sign."
        val spot = sampleSpot(locationDetails = multilineNotes)

        val exported = VaultBackupManager.buildSpotNotesTextForExport(spot)

        assertTrue(exported.contains("Notes\n-----"))
        assertTrue(exported.contains(multilineNotes))
        assertTrue(exported.contains("Arrived at north lot."))
        assertTrue(exported.contains("Gate code: 4521"))
    }

    @Test
    fun spotJsonRoundtrip_preservesNewlinesInLocationDetails() {
        val multilineNotes = "Paragraph one.\n\nParagraph two.\nLine three."
        val spot = sampleSpot(locationDetails = multilineNotes)
        val json = VaultBackupManager.spotToJsonForExport(spot)

        val restored = VaultBackupManager.spotLocationDetailsFromJson(json)

        assertEquals(multilineNotes, restored)
    }

    private fun sampleSpot(locationDetails: String): LocationSpot {
        return LocationSpot(
            id = 7,
            imagePath = "/tmp/photo.jpg",
            locationDetails = locationDetails,
            timestamp = 1_700_000_000_000L,
            lat = 37.7749,
            lng = -122.4194,
            address = "123 Market St, San Francisco",
            title = "Test Spot"
        )
    }
}
