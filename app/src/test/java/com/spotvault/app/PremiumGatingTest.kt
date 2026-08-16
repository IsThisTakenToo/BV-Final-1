package com.spotvault.app

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * premiumGatedId is what stops a lapsed/refunded entitlement (or a merge-imported backup carrying
 * someone else's stored picker id) from keeping a locked cosmetic rendering forever — every
 * appearance picker read goes through it. These lock in both overloads' gating at the boundary:
 * locked vs. free selection, crossed with entitled vs. not.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PremiumGatingTest {

    private fun prefs(premiumUnlocked: Boolean) =
        RuntimeEnvironment.getApplication()
            .getSharedPreferences("PremiumGatingTest_${System.nanoTime()}", Context.MODE_PRIVATE)
            .apply { edit().putBoolean(PREMIUM_UNLOCKED_PREF, premiumUnlocked).apply() }

    @Test
    fun lockedSelection_withoutPremium_fallsBackToFreeId() {
        val result = premiumGatedId(prefs(premiumUnlocked = false), storedId = "wild", freeId = "classic")
        assertEquals("classic", result)
    }

    @Test
    fun lockedSelection_withPremium_passesThrough() {
        val result = premiumGatedId(prefs(premiumUnlocked = true), storedId = "wild", freeId = "classic")
        assertEquals("wild", result)
    }

    @Test
    fun freeSelection_alwaysPassesThrough_regardlessOfEntitlement() {
        assertEquals("classic", premiumGatedId(prefs(premiumUnlocked = false), storedId = "classic", freeId = "classic"))
        assertEquals("classic", premiumGatedId(prefs(premiumUnlocked = true), storedId = "classic", freeId = "classic"))
    }

    @Test
    fun multiFreeIdOverload_lockedSelection_withoutPremium_fallsBackToDefault() {
        val freeIds = setOf("gold_cobalt", "purple_teal")
        val result = premiumGatedId(prefs(premiumUnlocked = false), storedId = "sunset", freeIds = freeIds, defaultFreeId = "gold_cobalt")
        assertEquals("gold_cobalt", result)
    }

    @Test
    fun multiFreeIdOverload_secondFreeOption_passesThroughWithoutPremium() {
        // Not every free tier has just one option (Color Theme, Found Splash Style) — the
        // non-default free id must not get incorrectly gated back to the default.
        val freeIds = setOf("gold_cobalt", "purple_teal")
        val result = premiumGatedId(prefs(premiumUnlocked = false), storedId = "purple_teal", freeIds = freeIds, defaultFreeId = "gold_cobalt")
        assertEquals("purple_teal", result)
    }
}
