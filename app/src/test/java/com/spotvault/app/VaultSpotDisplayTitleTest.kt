package com.spotvault.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Auto-named spots store their title as "$label\n$date" (a literal embedded newline) — the Vault
 * card display strips everything from that newline on, showing just the label since the date is
 * already shown separately elsewhere on the card. See the SpotEditDialog fix that stopped this
 * same split from silently discarding the date suffix on save.
 */
class VaultSpotDisplayTitleTest {

    private fun spot(title: String) = LocationSpot(
        imagePath = "",
        locationDetails = "",
        timestamp = 0L,
        lat = 0.0,
        lng = 0.0,
        address = "",
        title = title
    )

    @Test
    fun autoNamedTitle_stripsDateSuffixForDisplay() {
        assertEquals("Downtown Garage", vaultSpotDisplayTitle(spot("Downtown Garage\nAug 14, 2026 3:00 PM")))
    }

    @Test
    fun plainTitle_withNoEmbeddedNewline_isUnchanged() {
        assertEquals("Downtown Garage", vaultSpotDisplayTitle(spot("Downtown Garage")))
    }

    @Test
    fun blankTitle_fallsBackToPlaceholder() {
        assertEquals("Saved spot", vaultSpotDisplayTitle(spot("")))
        assertEquals("Saved spot", vaultSpotDisplayTitle(spot("   ")))
    }

    @Test
    fun titleWithOnlyANewlinePrefix_fallsBackToPlaceholder() {
        // substringBefore('\n') on "\nAug 14" is "" — the label half is empty even though a
        // suffix exists, and must still fall back to the placeholder rather than show a blank title.
        assertEquals("Saved spot", vaultSpotDisplayTitle(spot("\nAug 14, 2026")))
    }

    @Test
    fun multipleEmbeddedNewlines_onlySplitsOnTheFirst() {
        assertEquals("Line one", vaultSpotDisplayTitle(spot("Line one\nLine two\nLine three")))
    }
}
