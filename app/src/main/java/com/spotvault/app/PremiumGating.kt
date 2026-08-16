package com.spotvault.app

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val PREMIUM_UNLOCKED_PREF = "premium_unlocked"

fun isPremiumUnlocked(prefs: SharedPreferences): Boolean =
    prefs.getBoolean(PREMIUM_UNLOCKED_PREF, false)

/** The single free option kept available in each locked Appearance picker — everything else
 * requires [PREMIUM_UNLOCKED_PREF]. Color Theme and Found Splash Style are the exceptions with
 * more than one free option. Every id here is already each list's default entry, so nothing
 * about the actual defaults needed to change to make the free tier match what's unlocked out of
 * the box. Fonts are entirely free — see [FontFamilyPicker] in SettingsCategoryContent.kt, which
 * no longer consults this object at all. */
object PremiumFreeTier {
    val freeColorThemeIds = setOf("gold_cobalt", "purple_teal")
    const val freeButtonStyleId = "classic"
    const val freeVaultIconStyleId = "classic"
    const val freeCompassStyleId = "classic"
    val freeBackgroundPatternId = BackgroundPattern.GRADIENT_MESH.id
    val freeAppIconId = AppIcon.DEFAULT.id
    val freeHapticPatternId = HapticPattern.CRISP_CLICK.id
    // Default stays the actual default; None is free alongside it so a no-splash preference
    // never requires Premium either.
    val freeSplashStyleIds = setOf(SplashStyle.NONE.id, SplashStyle.DEFAULT.id)
    val freeFoundSplashStyleIds = setOf(FoundSplashStyle.CLASSIC.id, FoundSplashStyle.NONE.id)
}

/** Resolves a stored appearance-picker id against current entitlement, falling back to [freeId]
 * if [storedId] is a locked (non-free) option and premium isn't unlocked. Ungated reads of these
 * prefs — trusting whatever's stored without rechecking entitlement — are exactly how a stale
 * selection would keep rendering forever once real purchase-gating exists: a lapsed/refunded
 * purchase, or a merge-imported backup carrying someone else's stored id (see
 * VaultBackupManager's BACKUP_EXCLUDED_PREF_KEYS, which excludes the entitlement itself but not
 * every picker id that could reference a locked option). Premium is currently just an open
 * Settings toggle with no real billing wired up yet, so this is a no-op in practice today — every
 * caller either already re-derives `storedId` fresh each read or updates its own state on toggle,
 * so nothing needs to react retroactively; it's the singular place this logic needs to live once
 * a real entitlement can actually lapse. */
fun premiumGatedId(prefs: SharedPreferences, storedId: String, freeId: String): String =
    if (storedId != freeId && !isPremiumUnlocked(prefs)) freeId else storedId

/** Same idea as [premiumGatedId], for a picker with more than one free option (Color Theme,
 * Found Splash Style) — falls back to [defaultFreeId] specifically, not just "any free id",
 * since a fallback should always land on the one both lists agree is the actual default. */
fun premiumGatedId(prefs: SharedPreferences, storedId: String, freeIds: Set<String>, defaultFreeId: String): String =
    if (storedId !in freeIds && !isPremiumUnlocked(prefs)) defaultFreeId else storedId

/** Reverts any currently-active premium-only cosmetic selection to its free default the instant
 * Premium is turned off, and updates [ThemeState] directly so every already-mounted screen
 * (bottom-nav vault icon, the compass screen, the app's own theme) reflects it immediately —
 * without this, [ThemeState] only re-syncs from prefs in MainActivity's onCreate/onResume, so a
 * mid-session downgrade would otherwise leave a premium look rendering everywhere with no
 * entitlement behind it until the app is fully restarted. Call this BEFORE writing
 * [PREMIUM_UNLOCKED_PREF] to false, on the same [android.content.SharedPreferences.Editor] the
 * caller is about to commit, so the downgrade and the entitlement flag land in one commit and one
 * widget refresh cycle rather than two. Reads every field fresh from [prefs] rather than trusting
 * whatever's currently in [ThemeState] — it's called with premium still true in prefs at the
 * moment of the read, so this always sees the actual stored id, gating aside. A no-op for any
 * field that's already on its free option. */
fun revertLockedCosmeticsToFree(prefs: SharedPreferences, editor: SharedPreferences.Editor) {
    val currentTheme = normalizeColorThemeId(prefs.getString("color_theme", "gold_cobalt"))
    if (currentTheme !in PremiumFreeTier.freeColorThemeIds) {
        ThemeState.currentTheme = "gold_cobalt"
        editor.putString("color_theme", "gold_cobalt")
    }
    val vaultIconStyle = when (val saved = prefs.getString("vault_icon_style", "classic") ?: "classic") {
        "gear" -> "maglock"
        else -> saved
    }
    if (vaultIconStyle != PremiumFreeTier.freeVaultIconStyleId) {
        ThemeState.vaultIconStyle = PremiumFreeTier.freeVaultIconStyleId
        editor.putString("vault_icon_style", PremiumFreeTier.freeVaultIconStyleId)
    }
    val compassStyle = prefs.getString("compass_style", CompassStyle.CLASSIC.id) ?: CompassStyle.CLASSIC.id
    if (compassStyle != PremiumFreeTier.freeCompassStyleId) {
        ThemeState.compassStyle = PremiumFreeTier.freeCompassStyleId
        editor.putString("compass_style", PremiumFreeTier.freeCompassStyleId)
    }
    val buttonStyle = normalizeButtonStyle(prefs.getString("button_style", "classic"))
    if (buttonStyle != PremiumFreeTier.freeButtonStyleId) {
        ThemeState.buttonStyle = PremiumFreeTier.freeButtonStyleId
        editor.putString("button_style", PremiumFreeTier.freeButtonStyleId)
    }
    val backgroundPatternId = normalizeBackgroundPatternId(prefs.getString("background_pattern", BackgroundPattern.GRADIENT_MESH.id))
    if (backgroundPatternId != PremiumFreeTier.freeBackgroundPatternId) {
        ThemeState.backgroundPatternId = PremiumFreeTier.freeBackgroundPatternId
        editor.putString("background_pattern", PremiumFreeTier.freeBackgroundPatternId)
    }
}

/** Subtle "there's more this way" affordance for a horizontally-scrolling picker row — a small
 * translucent chevron badge, shown only while [scrollState] actually has more content to reveal
 * and gone once you've scrolled all the way. Meant to sit at `Alignment.CenterEnd` of a Box that
 * wraps the scrollable Row. */
@Composable
fun TrailingScrollHint(scrollState: ScrollState, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = scrollState.canScrollForward,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(SpotVaultColors.Void.copy(alpha = 0.6f))
                .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "More options — scroll right",
                tint = SpotVaultColors.Teal.copy(alpha = 0.9f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Small overlay badge for a locked picker cell — a filled circle with a lock glyph, meant to
 * sit in a Box's top-end corner over otherwise-normal (just dimmed) cell content. */
@Composable
fun PremiumLockBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .background(SpotVaultColors.Void.copy(alpha = 0.9f), CircleShape)
            .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Lock, contentDescription = "Premium", tint = SpotVaultColors.Muted, modifier = Modifier.size(11.dp))
    }
}

/** Shown under any Appearance section that has locked options — one reusable banner, tap
 * anywhere on it to jump to Premium Unlocks. */
@Composable
fun PremiumLockBanner(onUpgradeClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SpotVaultColors.Primary.copy(alpha = 0.12f))
            .border(1.dp, SpotVaultColors.Primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable(onClick = onUpgradeClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.WorkspacePremium,
            contentDescription = null,
            tint = SpotVaultColors.Primary,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text("More options with Premium", color = SpotVaultColors.OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("Tap to unlock the rest in Premium Unlocks", color = SpotVaultColors.Muted, fontSize = 11.sp)
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = SpotVaultColors.Muted,
            modifier = Modifier.size(16.dp)
        )
    }
}
