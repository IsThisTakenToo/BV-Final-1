package com.spotvault.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Every persisted "which style did the user pick" id goes through one of these fromId lookups on
 * every read — a null pref (never set), a garbage/corrupted value, or an id from a since-removed
 * style (renamed/retired between versions) must all resolve to a safe default instead of crashing
 * or silently rendering nothing.
 */
class StyleIdFallbackTest {

    @Test
    fun splashStyle_nullId_fallsBackToDefault() {
        assertEquals(SplashStyle.DEFAULT, SplashStyle.fromId(null))
    }

    @Test
    fun splashStyle_unknownGarbageId_fallsBackToDefault() {
        assertEquals(SplashStyle.DEFAULT, SplashStyle.fromId("not_a_real_style_id"))
    }

    @Test
    fun splashStyle_legacyRetiredIds_migrateToReplacementStyle() {
        // These three ids were once real styles that got renamed/merged — old prefs written
        // before the rename still carry the old id and must resolve to today's replacement, not
        // silently fall back to Default (which would look like a downgrade to anyone who'd
        // actually picked a non-default style before the rename).
        assertEquals(SplashStyle.TRIANGULATE, SplashStyle.fromId("grid"))
        assertEquals(SplashStyle.TRIANGULATE, SplashStyle.fromId("pin_drop"))
        assertEquals(SplashStyle.VAULT_BLOOM, SplashStyle.fromId("icon_surge"))
        assertEquals(SplashStyle.SIGNAL_FORGE, SplashStyle.fromId("screen_crack"))
    }

    @Test
    fun splashStyle_currentValidId_roundTrips() {
        SplashStyle.entries.forEach { style ->
            assertEquals(style, SplashStyle.fromId(style.id))
        }
    }

    @Test
    fun foundSplashStyle_nullOrUnknownId_fallsBackToClassic() {
        assertEquals(FoundSplashStyle.CLASSIC, FoundSplashStyle.fromId(null))
        assertEquals(FoundSplashStyle.CLASSIC, FoundSplashStyle.fromId("not_a_real_style_id"))
    }

    @Test
    fun foundSplashStyle_currentValidId_roundTrips() {
        FoundSplashStyle.entries.forEach { style ->
            assertEquals(style, FoundSplashStyle.fromId(style.id))
        }
    }

    @Test
    fun autoDeleteInterval_nullOrUnknownId_fallsBackToMonth() {
        assertEquals(AutoDeleteInterval.MONTH, AutoDeleteInterval.fromId(null))
        assertEquals(AutoDeleteInterval.MONTH, AutoDeleteInterval.fromId("not_a_real_interval_id"))
    }

    @Test
    fun autoDeleteInterval_currentValidId_roundTrips() {
        AutoDeleteInterval.entries.forEach { interval ->
            assertEquals(interval, AutoDeleteInterval.fromId(interval.id))
        }
    }
}
