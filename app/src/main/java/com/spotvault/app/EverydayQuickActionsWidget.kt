package com.spotvault.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.action.actionRunCallback
import android.graphics.Color
import android.content.Context
import android.content.Intent

class EverydayQuickActionsWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val context = LocalContext.current
            val prefs = currentState<Preferences>()
            val isTracking = prefs[KEY_IS_TRACKING]
                ?: ActiveTrackingHelper.isActive(context)
            // Observe revision + theme fingerprint so custom color saves recompose even when
            // tracking state is unchanged (same pattern as Premium + Vault Favorites widgets).
            val revision = prefs[KEY_REVISION] ?: 0L
            val themeCache = prefs[WidgetGlanceThemeKeys.THEME_CACHE] ?: ""

            val theme = remember(revision, themeCache) {
                try {
                    GlanceThemeManager.load(context, prefs)
                } catch (_: Exception) {
                    GlanceThemeManager.defaultTheme()
                }
            }

            val initialPrefs = remember { context.getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE) }
            EverydayQuickActionsContent(
                theme = theme,
                themeRevision = revision,
                isTracking = isTracking,
                isYinYangTheme = WidgetThemeHelper.effectiveThemeId(initialPrefs) == "yin_yang"
            )
        }
    }

    companion object {
        val KEY_IS_TRACKING = booleanPreferencesKey("is_tracking")
        val KEY_REVISION = longPreferencesKey("widget_revision")
    }
}

class EverydayQuickActionsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EverydayQuickActionsWidget()
}

private fun relayIntent(context: Context, action: String): Intent =
    Intent(context, QuickActionRelayActivity::class.java).apply {
        // Unique action + data — PendingIntent identity ignores extras.
        this.action = "com.spotvault.app.widget.RELAY_$action"
        data = android.net.Uri.parse("spotvault://widget/relay/$action")
        putExtra(QuickActionRelayActivity.EXTRA_ACTION, action)
        addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_ANIMATION or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        )
    }

private val WidgetGap = 6.dp
private val WidgetBackgroundInset = 12.dp // matches GlanceThemedBackground's 6dp-per-side padding

/** Live per-button pixel width for an N-column row, derived from the widget's actual current
 * size (only meaningful under [SizeMode.Exact] — see [EverydayQuickActionsWidget]) rather than a
 * fixed assumption, so the bitmap [GlanceThemedActionButton] renders is never stretched to fit. */
@Composable
private fun widgetSlotWidthPx(columns: Int): Int {
    val context = LocalContext.current
    val widthDp = LocalSize.current.width
    val slots = columns.coerceAtLeast(1)
    val available = (widthDp - WidgetBackgroundInset - WidgetGap * slots).coerceAtLeast(40.dp)
    val slotDp = (available / slots).coerceIn(40.dp, 220.dp)
    return WidgetThemeHelper.dpToPx(context, slotDp.value)
}

@Composable
private fun widgetButtonHeight(): Dp =
    (LocalSize.current.height - WidgetBackgroundInset).coerceIn(48.dp, 98.dp)

/**
 * Idle: Quick Pin | Quick Share | Quick Track — same actions as the in-app home row.
 * Tracking: Found | Navigate — shown while active tracking is running after Quick Track.
 */
@Composable
private fun EverydayQuickActionsContent(
    theme: GlanceWidgetTheme,
    themeRevision: Long,
    isTracking: Boolean,
    isYinYangTheme: Boolean = false
) {
    val context = LocalContext.current
    val columns = if (isTracking) 2 else 3
    val slotWidthPx = widgetSlotWidthPx(columns)
    val buttonHeight = widgetButtonHeight()
    // White while tracking — Yin Yang is explicitly black & white already, so its own
    // theme-computed colors are used instead of overriding them with a flat white.
    val foundFg = if (isYinYangTheme) theme.palette.onPrimary else Color.WHITE
    // Black icon + text on the accent (gold) fill, matching the Premium widget's Navigate —
    // white read poorly against a fill this bright.
    val navigateFg = if (isYinYangTheme) theme.palette.onAccent else Color.BLACK
    GlanceThemedBackground(theme = theme) {
        Row(modifier = GlanceModifier.fillMaxSize()) {
            if (isTracking) {
                GlanceThemedActionButton(
                    label = "Found",
                    theme = theme,
                    backgroundArgb = theme.palette.primary,
                    foregroundArgb = foundFg,
                    icon = WidgetThemeHelper.QuickActionIcon.FOUND,
                    modifier = widgetActionSlot(),
                    slotWidthPx = slotWidthPx,
                    buttonHeightDp = buttonHeight,
                    compactLabel = false,
                    labelEmphasis = true,
                    primaryAction = true,
                    premiumPolish = true,
                    themeRevision = themeRevision,
                    onClick = actionRunCallback<FoundActionCallback>()
                )
                GlanceThemedActionButton(
                    label = "Navigate",
                    theme = theme,
                    backgroundArgb = theme.palette.accent,
                    foregroundArgb = navigateFg,
                    icon = WidgetThemeHelper.QuickActionIcon.NAVIGATE,
                    modifier = widgetActionSlot(),
                    slotWidthPx = slotWidthPx,
                    buttonHeightDp = buttonHeight,
                    compactLabel = false,
                    labelEmphasis = true,
                    primaryAction = true,
                    premiumPolish = true,
                    themeRevision = themeRevision,
                    onClick = actionStartActivity(relayIntent(context, QuickActionRelayActivity.ACTION_NAVIGATE))
                )
            } else {
                GlanceThemedActionButton(
                    label = "Quick Pin",
                    theme = theme,
                    backgroundArgb = theme.palette.primary,
                    foregroundArgb = theme.palette.onPrimary,
                    icon = WidgetThemeHelper.QuickActionIcon.PIN_DROP,
                    modifier = widgetActionSlot(),
                    slotWidthPx = slotWidthPx,
                    buttonHeightDp = buttonHeight,
                    compactLabel = false,
                    labelEmphasis = true,
                    primaryAction = true,
                    premiumPolish = true,
                    themeRevision = themeRevision,
                    onClick = actionStartActivity(relayIntent(context, QuickActionRelayActivity.ACTION_PIN))
                )
                GlanceThemedActionButton(
                    label = "Quick Share",
                    theme = theme,
                    backgroundArgb = theme.palette.surface,
                    // White icon + text — the theme-computed primary color read too close to the
                    // dark surface fill to be easily legible.
                    foregroundArgb = Color.WHITE,
                    icon = WidgetThemeHelper.QuickActionIcon.SHARE,
                    outlined = true,
                    borderColorArgb = Color.argb(
                        128,
                        Color.red(theme.palette.primary),
                        Color.green(theme.palette.primary),
                        Color.blue(theme.palette.primary)
                    ),
                    modifier = widgetActionSlot(),
                    slotWidthPx = slotWidthPx,
                    buttonHeightDp = buttonHeight,
                    compactLabel = false,
                    labelEmphasis = true,
                    premiumPolish = true,
                    themeRevision = themeRevision,
                    onClick = actionStartActivity(relayIntent(context, QuickActionRelayActivity.ACTION_SHARE))
                )
                GlanceThemedActionButton(
                    label = "Quick Track",
                    theme = theme,
                    backgroundArgb = theme.palette.accent,
                    // Black icon + text together, matching the Premium widget's Pin/Quick Track.
                    foregroundArgb = Color.BLACK,
                    icon = WidgetThemeHelper.QuickActionIcon.TRACK,
                    modifier = widgetActionSlot(),
                    slotWidthPx = slotWidthPx,
                    buttonHeightDp = buttonHeight,
                    compactLabel = false,
                    labelEmphasis = true,
                    primaryAction = true,
                    premiumPolish = true,
                    themeRevision = themeRevision,
                    onClick = actionStartActivity(relayIntent(context, QuickActionRelayActivity.ACTION_TRACK))
                )
            }
        }
    }
}
