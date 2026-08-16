package com.spotvault.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortScreenNavScaleTest {

    @Test
    fun normalPhoneHeight_isExactlyOne() {
        assertEquals(1f, shortScreenNavScale(700), 0.001f)
        assertEquals(1f, shortScreenNavScale(750), 0.001f)
        assertEquals(1f, shortScreenNavScale(800), 0.001f)
        assertEquals(1f, shortScreenNavScale(900), 0.001f)
    }

    @Test
    fun shortPhone_scalesDown_proportionally() {
        assertEquals(0.9285714f, shortScreenNavScale(650), 0.001f)
        assertTrue(shortScreenNavScale(600) < 1f)
        assertEquals(0.75f, shortScreenNavScale(525), 0.001f)
        assertEquals(0.75f, shortScreenNavScale(480), 0.001f)
    }
}
