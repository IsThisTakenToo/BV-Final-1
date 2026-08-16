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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
            val glanceId = GlanceAppWidgetManager(this@TagFilterWidgetConfigActivity).getGlanceIdBy(appWidgetId)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                FlowRow(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allTags.forEach { tag ->
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
