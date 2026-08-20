package com.spotvault.app

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Widget configuration screen — shown when the Tag Filter widget is first placed, and again on
 * long-press "Edit" since the widget-info XML declares it `reconfigurable`. Picks one or more
 * tags — the widget shows every spot carrying at least one of them, deduplicated — same visual
 * language as the Vault's own tag chips (VaultTagFilterSheet's Tags tab): same colors, same chip
 * shape.
 */
class TagFilterWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Cancelled unless/until the user actually picks a tag and taps Save — if the launcher's
        // widget-placement flow is dismissed any other way (back button, task-switch), the
        // widget must not be added with no configuration at all.
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val prefs = getSharedPreferences("SpotVaultPrefs", MODE_PRIVATE)
        ThemeState.currentTheme = loadColorThemeFromPrefs(prefs)
        ThemeState.buttonStyle = loadButtonStyleFromPrefs(prefs)
        ThemeState.customPrimaryArgb = prefs.getInt("custom_primary_color", 0xFF6B2FFF.toInt())
        ThemeState.customAccentArgb = prefs.getInt("custom_accent_color", 0xFF00F0FF.toInt())
        // Same reasoning as QuickActionRelayActivity — this screen can cold-start straight from
        // placing the widget with no prior MainActivity launch in this process, so the font
        // needs its own explicit sync here too or this screen's chips/buttons show the default
        // typeface instead of whatever was actually picked in Settings.
        ThemeState.fontFamilyId = prefs.getString("app_font_family", AppFontOptions.DEFAULT_ID) ?: AppFontOptions.DEFAULT_ID
        SpotVaultColors.updateAmoled(prefs.getBoolean("amoled_black", false))

        // Exported config Activity — if App Lock is on, do not list tag names here. Route to
        // MainActivity (which shows the lock screen); launcher placement can be retried after unlock.
        if (prefs.getBoolean(APP_LOCK_ENABLED_PREF, false)) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
            finish()
            return
        }

        setContent {
            SpotVaultTheme(darkTheme = true) {
                TagFilterWidgetConfigScreen(
                    appWidgetId = appWidgetId,
                    onSave = { tagIds -> saveAndFinish(tagIds) },
                    onCancel = { finish() }
                )
            }
        }
    }

    private fun saveAndFinish(tagIds: Set<Int>) {
        lifecycleScope.launch {
            // Same lag this Activity's own pre-populate LaunchedEffect already guards against with
            // runCatching (Glance's id mapping can still be settling right after first placement) —
            // an uncaught throw here would crash the Activity at the worst possible moment, mid
            // widget-placement, instead of just finishing with the RESULT_CANCELED already set in
            // onCreate.
            val glanceId = runCatching {
                GlanceAppWidgetManager(this@TagFilterWidgetConfigActivity).getGlanceIdBy(appWidgetId)
            }.getOrNull()
            if (glanceId == null) {
                finish()
                return@launch
            }
            updateAppWidgetState(this@TagFilterWidgetConfigActivity, glanceId) { glancePrefs ->
                glancePrefs[TagFilterWidget.KEY_TAG_IDS] = tagIds.map { it.toString() }.toSet()
            }
            TagFilterWidget().update(this@TagFilterWidgetConfigActivity, glanceId)

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagFilterWidgetConfigScreen(
    appWidgetId: Int,
    onSave: (tagIds: Set<Int>) -> Unit,
    onCancel: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tagDao = remember { AppDatabase.getDatabase(context).tagDao() }
    val allTags by tagDao.getAllTags().collectAsState(initial = emptyList())
    var selectedTagIds by remember { mutableStateOf(setOf<Int>()) }

    // Pre-populate from the widget's current selection when this screen is reopened via
    // long-press "Edit" on an already-configured widget — without this, reconfiguring always
    // started from an empty selection and silently wiped whatever tags were already chosen the
    // moment Save was tapped, instead of letting the user adjust the existing picks.
    LaunchedEffect(appWidgetId) {
        val glanceId = runCatching {
            GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
        }.getOrNull() ?: return@LaunchedEffect
        val existingIds = runCatching {
            getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        }.getOrNull()?.get(TagFilterWidget.KEY_TAG_IDS)?.mapNotNull { it.toIntOrNull() }
        if (!existingIds.isNullOrEmpty()) {
            selectedTagIds = existingIds.toSet()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotVaultColors.Void)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                "Choose Tags",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = SpotVaultColors.Teal,
                letterSpacing = 0.5.sp
            )
            Text(
                "This widget shows every spot carrying at least one of the tags you pick — long-press it any time to change them.",
                color = SpotVaultColors.Muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
            )

            if (allTags.isEmpty()) {
                Text(
                    "No tags yet — add tags to a spot in the Vault first, then come back and place this widget.",
                    color = SpotVaultColors.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allTags, key = { it.id }) { tag ->
                        val selected = tag.id in selectedTagIds
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedTagIds = if (selected) selectedTagIds - tag.id else selectedTagIds + tag.id
                            },
                            label = { Text(tag.name, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = vaultTagChipColors(),
                            border = vaultTagChipBorder(selected),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SpotVaultButton(
                    onClick = { onSave(selectedTagIds) },
                    enabled = selectedTagIds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = spotVaultButtonShape()
                ) {
                    Text(
                        if (selectedTagIds.size > 1) "Save (${selectedTagIds.size} tags)" else "Save",
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = SpotVaultColors.Muted)
                }
            }
        }
    }
}
