package com.spotvault.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceHelperTest {

    @Test
    fun haversine_samePoint_isZero() {
        val meters = haversineDistanceMeters(40.7128, -74.0060, 40.7128, -74.0060)
        assertEquals(0.0, meters, 0.001)
    }

    @Test
    fun haversine_knownDistance_isReasonable() {
        // NYC to Philadelphia ~129 km
        val meters = haversineDistanceMeters(40.7128, -74.0060, 39.9526, -75.1652)
        assertTrue(meters in 120_000.0..140_000.0)
    }

    @Test
    fun formatDistanceAway_milesAndKm() {
        assertEquals("0.4 mi away", formatDistanceAway(1609.344 * 0.4, "miles"))
        assertEquals("0.6 km away", formatDistanceAway(600.0, "km"))
    }

    @Test
    fun formatDistanceAway_zeroMeters_neverShowsZeroPointZero() {
        // Standing right on top of a saved spot still reads as "0.1 mi/km away" rather than
        // "0.0" — a bare zero would look like a broken/unmeasured label, not "you're here."
        assertEquals("0.1 mi away", formatDistanceAway(0.0, "miles"))
        assertEquals("0.1 km away", formatDistanceAway(0.0, "km"))
    }

    @Test
    fun formatDistanceAway_boundaryBetweenDecimalAndWholeNumberFormatting() {
        // Just under 10 keeps one decimal; 10 and above drops to a whole number.
        assertEquals("9.9 mi away", formatDistanceAway(1609.344 * 9.9, "miles"))
        assertEquals("10 mi away", formatDistanceAway(1609.344 * 10.0, "miles"))
        assertEquals("9.9 km away", formatDistanceAway(9900.0, "km"))
        assertEquals("10 km away", formatDistanceAway(10_000.0, "km"))
    }

    @Test
    fun bearingDegrees_samePoint_isZeroNotNaN() {
        // Degenerate input (no direction to point in) must resolve to a real number, not NaN —
        // atan2(0, 0) is defined as 0 in both Java and the IEEE spec this relies on, but this
        // locks that assumption in rather than leaving it implicit.
        val bearing = bearingDegrees(40.7128, -74.0060, 40.7128, -74.0060)
        assertEquals(0.0, bearing, 0.001)
    }

    @Test
    fun bearingDegrees_dueNorthAndDueSouth() {
        assertEquals(0.0, bearingDegrees(0.0, 0.0, 1.0, 0.0), 0.5)
        assertEquals(180.0, bearingDegrees(1.0, 0.0, 0.0, 0.0), 0.5)
    }

    @Test
    fun bearingDegrees_alwaysNormalizedToZeroThrough360() {
        // Points where the raw atan2 result would be negative (west-of-origin) must still come
        // back in [0, 360), not as a negative degree value the UI/compass math isn't expecting.
        val bearing = bearingDegrees(0.0, 0.0, 0.0, -1.0)
        assertTrue(bearing in 0.0..360.0)
        assertEquals(270.0, bearing, 0.5)
    }

    @Test
    fun formatBearingLabel_cardinalAndWraparoundBoundaries() {
        assertEquals("0° N", formatBearingLabel(0.0))
        // Exactly on the N/NE boundary rounds up to NE, consistently with every other boundary.
        assertEquals("22° NE", formatBearingLabel(22.5))
        assertEquals("45° NE", formatBearingLabel(45.0))
        // Just under the wraparound back to N.
        assertEquals("359° N", formatBearingLabel(359.9))
        // Negative and >360 inputs both normalize the same way as bearingDegrees does.
        assertEquals("0° N", formatBearingLabel(-360.0))
        assertEquals("90° E", formatBearingLabel(450.0))
    }

    @Test
    fun shortestAngleDelta_wrapsAroundZeroDegreesTheShortWay() {
        // 350 -> 10 is a short +20 turn through 0, not a -340 turn the long way around.
        assertEquals(20f, shortestAngleDelta(350f, 10f), 0.01f)
        // 10 -> 350 is the mirror image: a short -20 turn.
        assertEquals(-20f, shortestAngleDelta(10f, 350f), 0.01f)
    }

    @Test
    fun shortestAngleDelta_zeroDelta_isZero() {
        assertEquals(0f, shortestAngleDelta(90f, 90f), 0.01f)
    }

    @Test
    fun shortestAngleDelta_oppositeHeadings_isPlusOrMinus180() {
        val delta = shortestAngleDelta(0f, 180f)
        assertTrue(delta == 180f || delta == -180f)
    }
}
