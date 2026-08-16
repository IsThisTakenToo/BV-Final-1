package com.spotvault.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun saveThemePref(
    prefs: SharedPreferences,
    context: android.content.Context,
    block: SharedPreferences.Editor.() -> Unit
) {
    WidgetThemeHelper.commitThemeChange(prefs, context.applicationContext, block)
}

private fun persistVaultDisplayName(
    prefs: SharedPreferences,
    context: android.content.Context,
    raw: String
): String {
    val normalized = normalizeVaultDisplayName(raw)
    prefs.edit().putString(VAULT_DISPLAY_NAME_PREF, normalized).apply()
    WidgetThemeHelper.refreshAllWidgets(context.applicationContext)
    return normalized
}

private enum class CustomThemeRole { PRIMARY, SECONDARY }

/** Matches the app's own auto-contrast threshold (see SpotVaultDesign.kt's OnPrimary/OnTeal) —
 * used here for the custom-theme preview card's button text, which resolves its color locally
 * from in-progress wheel state rather than through ThemeState/LocalSpotVaultColors, so it never
 * depends on composition-order timing to show the correct preview while dragging. */
private fun resolveCustomThemeTextColor(background: Color, mode: String): Color = when (mode) {
    "white" -> Color.White
    "black" -> Color.Black
    else -> if (background.luminance() > 0.4f) Color.Black else Color.White
}

private fun saveCustomThemeFromPicker(
    prefs: SharedPreferences,
    context: android.content.Context,
    customPrimary: Color,
    customAccent: Color,
    primaryTextMode: String = "auto",
    accentTextMode: String = "auto",
    applyAsActiveTheme: Boolean = true
) {
    ThemeState.customPrimaryArgb = customPrimary.toArgbInt()
    ThemeState.customAccentArgb = customAccent.toArgbInt()
    ThemeState.customOnPrimaryArgb = textModeToArgb(primaryTextMode)
    ThemeState.customOnAccentArgb = textModeToArgb(accentTextMode)
    if (applyAsActiveTheme) {
        ThemeState.currentTheme = "custom"
    }
    saveThemePref(prefs, context) {
        putBoolean(HAS_CUSTOM_THEME_PREF, true)
        putInt("custom_primary_color", customPrimary.toArgbInt())
        putInt("custom_accent_color", customAccent.toArgbInt())
        putString("custom_on_primary_mode", primaryTextMode)
        putString("custom_on_accent_mode", accentTextMode)
        if (applyAsActiveTheme) {
            putString("color_theme", "custom")
        }
    }
}

@Composable
fun AppearanceSettingsContent(prefs: SharedPreferences, onNavigateToCategory: (String) -> Unit = {}) {
    val settingsContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var premiumUnlocked by remember { mutableStateOf(isPremiumUnlocked(prefs)) }
    val goToPremium: () -> Unit = { onNavigateToCategory("premium") }
    var dynamicColorEnabled by remember { mutableStateOf(ThemeState.dynamicColorEnabled) }
    val dynamicColorAvailable = ThemeState.dynamicColorAvailable
    var splashStyleId by remember {
        mutableStateOf(
            premiumGatedId(
                prefs,
                prefs.getString("splash_style", SplashStyle.DEFAULT.id) ?: SplashStyle.DEFAULT.id,
                PremiumFreeTier.freeSplashStyleIds,
                SplashStyle.DEFAULT.id
            )
        )
    }
    var foundSplashStyleId by remember {
        mutableStateOf(loadFoundSplashStyleFromPrefs(prefs).id)
    }
    var foundSplashTitle by remember { mutableStateOf(loadFoundSplashTitleFromPrefs(prefs)) }
    var savedFoundSplashTitle by remember { mutableStateOf(loadFoundSplashTitleFromPrefs(prefs)) }
    var foundSplashSubtitle by remember { mutableStateOf(loadFoundSplashSubtitleFromPrefs(prefs)) }
    var savedFoundSplashSubtitle by remember { mutableStateOf(loadFoundSplashSubtitleFromPrefs(prefs)) }
    var appIconId by remember { mutableStateOf(AppIconManager.currentIconId(prefs)) }
    var pendingIconChange by remember { mutableStateOf<AppIcon?>(null) }
    var amoledBlack by remember { mutableStateOf(prefs.getBoolean("amoled_black", false)) }
    var reduceAnimations by remember { mutableStateOf(prefs.getBoolean("reduce_animations", false)) }
    var vaultIconStyle by remember {
        mutableStateOf(premiumGatedId(prefs, prefs.getString("vault_icon_style", "classic") ?: "classic", PremiumFreeTier.freeVaultIconStyleId))
    }
    var compassStyle by remember {
        mutableStateOf(premiumGatedId(prefs, prefs.getString("compass_style", CompassStyle.CLASSIC.id) ?: CompassStyle.CLASSIC.id, PremiumFreeTier.freeCompassStyleId))
    }
    var fontFamilyId by remember { mutableStateOf(prefs.getString("app_font_family", AppFontOptions.DEFAULT_ID) ?: AppFontOptions.DEFAULT_ID) }
    var backgroundPatternId by remember {
        mutableStateOf(premiumGatedId(
            prefs,
            normalizeBackgroundPatternId(prefs.getString("background_pattern", BackgroundPattern.GRADIENT_MESH.id)),
            PremiumFreeTier.freeBackgroundPatternId
        ))
    }
    var buttonStyle by remember { mutableStateOf(loadButtonStyleFromPrefs(prefs)) }
    var textScale by remember { mutableStateOf(prefs.getFloat("text_size_scale", 1.15f)) }
    var vaultDisplayName by remember { mutableStateOf(loadVaultDisplayNameFromPrefs(prefs)) }
    var savedVaultDisplayName by remember { mutableStateOf(loadVaultDisplayNameFromPrefs(prefs)) }
    var vaultNameSavedPulse by remember { mutableStateOf(false) }
    var customPrimary by remember {
        mutableStateOf(Color(prefs.getInt("custom_primary_color", 0xFF6B2FFF.toInt())))
    }
    var customAccent by remember {
        mutableStateOf(Color(prefs.getInt("custom_accent_color", 0xFF00F0FF.toInt())))
    }
    var savedCustomPrimary by remember {
        mutableStateOf(Color(prefs.getInt("custom_primary_color", 0xFF6B2FFF.toInt())))
    }
    var savedCustomAccent by remember {
        mutableStateOf(Color(prefs.getInt("custom_accent_color", 0xFF00F0FF.toInt())))
    }
    var hasCustomTheme by remember { mutableStateOf(prefs.getBoolean(HAS_CUSTOM_THEME_PREF, false)) }
    var showCustomPicker by remember { mutableStateOf(false) }
    var primaryTextMode by remember { mutableStateOf(prefs.getString("custom_on_primary_mode", "auto") ?: "auto") }
    var accentTextMode by remember { mutableStateOf(prefs.getString("custom_on_accent_mode", "auto") ?: "auto") }

    val latestVaultDisplayName by rememberUpdatedState(vaultDisplayName)
    val latestSavedVaultDisplayName by rememberUpdatedState(savedVaultDisplayName)
    val latestShowCustomPicker by rememberUpdatedState(showCustomPicker)
    val latestCustomPrimary by rememberUpdatedState(customPrimary)
    val latestCustomAccent by rememberUpdatedState(customAccent)
    val latestPrimaryTextMode by rememberUpdatedState(primaryTextMode)
    val latestAccentTextMode by rememberUpdatedState(accentTextMode)

    // Live preview while the picker is open — every wheel drag pushes straight into ThemeState
    // so the whole app (not just the little swatch here) shows the color as it's being picked,
    // rather than only after Save is tapped. This doesn't touch SharedPreferences at all, so
    // nothing is actually committed until Save runs (or the DisposableEffect below, which
    // already treats leaving the screen with the picker open as an implicit save — unchanged
    // from before, just now visible the whole time instead of only at the end).
    LaunchedEffect(showCustomPicker, customPrimary, customAccent, primaryTextMode, accentTextMode) {
        if (showCustomPicker) {
            ThemeState.customPrimaryArgb = customPrimary.toArgbInt()
            ThemeState.customAccentArgb = customAccent.toArgbInt()
            ThemeState.customOnPrimaryArgb = textModeToArgb(primaryTextMode)
            ThemeState.customOnAccentArgb = textModeToArgb(accentTextMode)
            ThemeState.currentTheme = "custom"
        }
    }

    LaunchedEffect(Unit) {
        if (prefs.getString("color_theme", "gold_cobalt") == "custom" && !prefs.getBoolean(HAS_CUSTOM_THEME_PREF, false)) {
            prefs.edit().putBoolean(HAS_CUSTOM_THEME_PREF, true).apply()
            hasCustomTheme = true
        }
    }

    LaunchedEffect(vaultNameSavedPulse) {
        if (vaultNameSavedPulse) {
            delay(1500)
            vaultNameSavedPulse = false
        }
    }

    // The custom color picker is a conditionally-rendered block inline in this screen's own
    // composition, not a real Dialog window — without this, a back gesture while it's open would
    // fall through to whatever handles back at the screen/Activity level (closing all of
    // Settings, or the app) instead of just collapsing the picker. Mirrors the Save button
    // exactly, rather than just closing it, so a back-swipe doesn't silently discard an
    // in-progress edit the same way leaving the screen already doesn't (see the DisposableEffect
    // below).
    BackHandler(enabled = showCustomPicker) {
        saveCustomThemeFromPicker(
            prefs, settingsContext, customPrimary, customAccent,
            primaryTextMode, accentTextMode, applyAsActiveTheme = true
        )
        savedCustomPrimary = customPrimary
        savedCustomAccent = customAccent
        hasCustomTheme = true
        showCustomPicker = false
    }

    DisposableEffect(Unit) {
        onDispose {
            if (normalizeVaultDisplayName(latestVaultDisplayName) != latestSavedVaultDisplayName) {
                persistVaultDisplayName(prefs, settingsContext, latestVaultDisplayName)
            }
            if (latestShowCustomPicker) {
                saveCustomThemeFromPicker(
                    prefs,
                    settingsContext,
                    latestCustomPrimary,
                    latestCustomAccent,
                    latestPrimaryTextMode,
                    latestAccentTextMode,
                    applyAsActiveTheme = true
                )
            }
        }
    }

    val presetThemes = getSeasonallySortedItems(
        listOf(
            ColorThemeOption("gold_cobalt", Color(0xFF2962FF), Color(0xFFFFD100)),
            ColorThemeOption("purple_teal", Color(0xFF6B2FFF), Color(0xFF00F0FF)),
            ColorThemeOption("camo_hunt", Color(0xFF5C6B2F), Color(0xFFFF6A00)),
            ColorThemeOption("forest_nature", Color(0xFF2E7D4F), Color(0xFF29B6F6)),
            ColorThemeOption("neon_pink", Color(0xFFFF008C), Color(0xFF00F0FF)),
            ColorThemeOption("yin_yang", Color(0xFF252525), Color(0xFFE8E8E8)),
            ColorThemeOption("crimson_gold", Color(0xFFFF2E63), Color(0xFFFFD100)),
            ColorThemeOption("halloween", Color(0xFFFF7A1A), Color(0xFF9B30FF), activeMonths = listOf(10)),
            ColorThemeOption("christmas", Color(0xFFE0233D), Color(0xFF1FA34D), activeMonths = listOf(11, 12))
        )
    ) { it.activeMonths }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsSectionCard(
            title = "Color & Motion",
            subtitle = "Themes, contrast, and animation",
            padding = 10.dp,
            contentSpacing = 6.dp
        ) {
            val vaultNameIsDirty = normalizeVaultDisplayName(vaultDisplayName) != savedVaultDisplayName
            val vaultNameSaveEnabled = vaultNameIsDirty && !vaultNameSavedPulse
            val vaultNameCheckAlpha by animateFloatAsState(
                targetValue = if (vaultNameSavedPulse) 1f else 0f,
                animationSpec = tween(250),
                label = "vaultNameSavedCheck"
            )
            OutlinedTextField(
                value = vaultDisplayName,
                onValueChange = { input ->
                    if (input.length <= MAX_VAULT_DISPLAY_NAME_LENGTH) {
                        vaultDisplayName = input
                        vaultNameSavedPulse = false
                    }
                },
                label = { Text("Vault Name") },
                supportingText = {
                    Text(
                        "Home header, splash & widget (max $MAX_VAULT_DISPLAY_NAME_LENGTH chars)",
                        color = SpotVaultColors.Muted,
                        fontSize = 11.sp
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VoiceMicButton(
                            onResult = { spoken ->
                                if (spoken.length <= MAX_VAULT_DISPLAY_NAME_LENGTH) {
                                    vaultDisplayName = spoken
                                    vaultNameSavedPulse = false
                                }
                            },
                            prompt = "Speak vault name…"
                        )
                        IconButton(
                            onClick = {
                                val normalized = persistVaultDisplayName(prefs, settingsContext, vaultDisplayName)
                                vaultDisplayName = normalized
                                savedVaultDisplayName = normalized
                                vaultNameSavedPulse = true
                            },
                            enabled = vaultNameSaveEnabled
                        ) {
                            // Teal.copy(alpha = vaultNameCheckAlpha) used to build the tint Color
                            // at composable-body scope, recomposing this whole trailing icon
                            // section on every frame of the 250ms fade — the animated part moved
                            // into graphicsLayer (deferred to draw time), tint itself now just
                            // picks between two fixed colors that don't change per-frame.
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Save vault name",
                                tint = if (vaultNameSavedPulse || vaultNameSaveEnabled) {
                                    SpotVaultColors.Teal
                                } else {
                                    SpotVaultColors.Muted.copy(alpha = 0.4f)
                                },
                                modifier = Modifier.graphicsLayer {
                                    alpha = if (vaultNameSavedPulse) vaultNameCheckAlpha else 1f
                                }
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && vaultNameIsDirty) {
                            val normalized = persistVaultDisplayName(prefs, settingsContext, vaultDisplayName)
                            vaultDisplayName = normalized
                            savedVaultDisplayName = normalized
                            vaultNameSavedPulse = true
                        }
                    },
                singleLine = true,
                enabled = premiumUnlocked,
                shape = RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = SpotVaultColors.OnSurface,
                    unfocusedLabelColor = SpotVaultColors.Muted,
                    unfocusedBorderColor = SpotVaultColors.Outline,
                    focusedLabelColor = SpotVaultColors.Teal,
                    focusedBorderColor = SpotVaultColors.Teal,
                    disabledTextColor = SpotVaultColors.Muted,
                    disabledLabelColor = SpotVaultColors.Muted.copy(alpha = 0.6f),
                    disabledBorderColor = SpotVaultColors.Outline.copy(alpha = 0.4f)
                )
            )
            if (!premiumUnlocked) {
                PremiumLockBanner(onUpgradeClick = goToPremium)
            }

            Text("Color Theme", color = SpotVaultColors.OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            val colorThemeScrollState = rememberScrollState()
            Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(colorThemeScrollState),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                presetThemes.forEach { (id, primary, accent) ->
                    val locked = !premiumUnlocked && id !in PremiumFreeTier.freeColorThemeIds
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val themeLabel = PresetThemeDisplayNames[id] ?: id
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(listOf(primary, accent)),
                                    CircleShape
                                )
                                .alpha(if (locked) 0.4f else 1f)
                                .border(
                                    2.dp,
                                    if (ThemeState.currentTheme == id) SpotVaultColors.OnSurface else Color.Transparent,
                                    CircleShape
                                )
                                // Selection was conveyed only by a border appearing around the
                                // swatch, and the swatch itself carried no name at all — TalkBack
                                // read an unlabeled, unstated button for every single theme.
                                .semantics {
                                    contentDescription = if (locked) "$themeLabel, Premium" else themeLabel
                                    selected = ThemeState.currentTheme == id
                                    role = Role.RadioButton
                                }
                                .clickable {
                                    if (locked) {
                                        goToPremium()
                                        return@clickable
                                    }
                                    // Color theme only ever changes colors — button style and
                                    // vault icon are the user's own separate picks, never
                                    // auto-swapped by picking a color swatch.
                                    ThemeState.currentTheme = id
                                    saveThemePref(prefs, settingsContext) { putString("color_theme", id) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (ThemeState.currentTheme == id) {
                                val checkColor = if (primary.luminance() > 0.4f) Color.Black else Color.White
                                Icon(Icons.Default.Check, contentDescription = null, tint = checkColor, modifier = Modifier.size(20.dp))
                            }
                            if (locked) {
                                PremiumLockBadge(modifier = Modifier.align(Alignment.Center))
                            }
                        }
                        Text(
                            text = PresetThemeDisplayNames[id] ?: "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SpotVaultColors.Muted,
                            maxLines = 1
                        )
                    }
                }

                // No blank placeholder here before a custom theme exists — the "+" button below
                // sits right after the presets until a custom swatch exists, then this column
                // appears and pushes "+" over to the right.
                if (hasCustomTheme) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(savedCustomPrimary, savedCustomAccent)
                                    ),
                                    CircleShape
                                )
                                .alpha(if (!premiumUnlocked) 0.4f else 1f)
                                .border(
                                    2.dp,
                                    if (ThemeState.currentTheme == "custom") SpotVaultColors.OnSurface else Color.Transparent,
                                    CircleShape
                                )
                                .semantics {
                                    contentDescription = if (!premiumUnlocked) "Custom theme, Premium" else "Custom theme"
                                    selected = ThemeState.currentTheme == "custom"
                                    role = Role.RadioButton
                                }
                                .clickable {
                                    if (!premiumUnlocked) {
                                        goToPremium()
                                        return@clickable
                                    }
                                    ThemeState.currentTheme = "custom"
                                    ThemeState.customPrimaryArgb = savedCustomPrimary.toArgbInt()
                                    ThemeState.customAccentArgb = savedCustomAccent.toArgbInt()
                                    saveThemePref(prefs, settingsContext) { putString("color_theme", "custom") }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (ThemeState.currentTheme == "custom") {
                                val checkColor = if (savedCustomPrimary.luminance() > 0.4f) Color.Black else Color.White
                                Icon(Icons.Default.Check, contentDescription = null, tint = checkColor, modifier = Modifier.size(20.dp))
                            }
                            if (!premiumUnlocked) {
                                PremiumLockBadge(modifier = Modifier.align(Alignment.Center))
                            }
                        }
                        Text("Custom", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SpotVaultColors.Muted, maxLines = 1)
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(SpotVaultColors.Deep, CircleShape)
                            .border(2.dp, SpotVaultColors.Outline.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                if (!premiumUnlocked) {
                                    goToPremium()
                                    return@clickable
                                }
                                customPrimary = savedCustomPrimary
                                customAccent = savedCustomAccent
                                showCustomPicker = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!premiumUnlocked) {
                            PremiumLockBadge()
                        } else {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Create custom theme",
                                tint = SpotVaultColors.Teal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Text("New", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SpotVaultColors.Muted, maxLines = 1)
                }
            }
            TrailingScrollHint(colorThemeScrollState, modifier = Modifier.align(Alignment.CenterEnd))
            }
            if (!premiumUnlocked) {
                PremiumLockBanner(onUpgradeClick = goToPremium, modifier = Modifier.padding(top = 4.dp))
            }

            if (showCustomPicker) {
                // Which color the wheel/slider/text-mode controls below are currently editing —
                // one full-width wheel shared by both roles instead of two half-width wheels
                // side by side. Trades "see both colors' wheels at once" for a noticeably bigger,
                // more precise wheel and a shorter dialog overall.
                var editingRole by remember { mutableStateOf(CustomThemeRole.PRIMARY) }
                val editingColor = if (editingRole == CustomThemeRole.PRIMARY) customPrimary else customAccent
                val editingTextMode = if (editingRole == CustomThemeRole.PRIMARY) primaryTextMode else accentTextMode

                // Live preview: actual button mockups in their real selected text colors, not
                // just a gradient strip — the whole point is showing what "Auto"/White/Black
                // actually looks like against whatever color the wheel is currently on, for both
                // roles at once regardless of which one is being edited right now.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(customPrimary)
                            .border(
                                width = if (editingRole == CustomThemeRole.PRIMARY) 2.dp else 0.dp,
                                color = SpotVaultColors.OnSurface.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Primary Button",
                            color = resolveCustomThemeTextColor(customPrimary, primaryTextMode),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(customAccent)
                            .border(
                                width = if (editingRole == CustomThemeRole.SECONDARY) 2.dp else 0.dp,
                                color = SpotVaultColors.OnSurface.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Secondary Button",
                            color = resolveCustomThemeTextColor(customAccent, accentTextMode),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Role toggle — switches which color (and text mode) every control below acts on.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(SpotVaultColors.Elevated.copy(alpha = 0.6f))
                        .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                ) {
                    listOf(
                        CustomThemeRole.PRIMARY to "Primary Color",
                        CustomThemeRole.SECONDARY to "Secondary Color"
                    ).forEach { (role, text) ->
                        val selected = editingRole == role
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) SpotVaultColors.Teal.copy(alpha = 0.18f) else Color.Transparent)
                                .clickable { editingRole = role }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text,
                                color = if (selected) SpotVaultColors.Teal else SpotVaultColors.Muted,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Single full-width wheel + brightness slider, bound to whichever role is
                // selected above. keyed on editingRole so the wheel's internal hue/saturation/
                // value state is fully torn down and recreated from editingColor on every role
                // switch, instead of relying on its selectedColor-vs-lastAppliedColor resync
                // check — that made switching feel one-directional in practice, since dragging
                // the wheel updates lastAppliedColor to match the just-emitted color, so flipping
                // back to a role whose color happens to equal what's already tracked can skip the
                // resync entirely.
                key(editingRole) {
                    ColorWheelPicker(
                        label = "",
                        selectedColor = editingColor,
                        onColorChange = { color ->
                            if (editingRole == CustomThemeRole.PRIMARY) customPrimary = color else customAccent = color
                        },
                        modifier = Modifier.padding(top = 6.dp),
                        wheelSize = 190.dp,
                        blackShortcut = true,
                        showLabel = false
                    )
                }
                TextColorModeRow(
                    label = "Button Text",
                    mode = editingTextMode,
                    onModeChange = { mode ->
                        if (editingRole == CustomThemeRole.PRIMARY) primaryTextMode = mode else accentTextMode = mode
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(SpotVaultColors.Primary, SpotVaultColors.Teal)
                            )
                        )
                        .semantics {
                            contentDescription = "Save custom theme"
                            role = Role.Button
                        }
                        .clickable {
                            saveCustomThemeFromPicker(
                                prefs, settingsContext, customPrimary, customAccent,
                                primaryTextMode, accentTextMode, applyAsActiveTheme = true
                            )
                            savedCustomPrimary = customPrimary
                            savedCustomAccent = customAccent
                            hasCustomTheme = true
                            showCustomPicker = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save",
                        fontWeight = FontWeight.Bold,
                        color = SpotVaultColors.OnPrimary
                    )
                }
            }

            SettingsToggleRow(
                title = "Material You / Dynamic Color",
                subtitle = when {
                    !premiumUnlocked -> "Premium — match your theme to your wallpaper's colors"
                    !dynamicColorAvailable -> "Requires Android 12 or newer"
                    else -> "Extracts your wallpaper's colors and applies them as your theme"
                },
                checked = dynamicColorEnabled,
                onCheckedChange = { checked ->
                    if (!premiumUnlocked) {
                        goToPremium()
                    } else if (dynamicColorAvailable) {
                        dynamicColorEnabled = checked
                        ThemeState.dynamicColorEnabled = checked
                        prefs.edit().putBoolean("dynamic_color_enabled", checked).apply()
                        if (checked) refreshDynamicColors(settingsContext)
                    }
                }
            )
            if (!premiumUnlocked) {
                PremiumLockBanner(onUpgradeClick = goToPremium, modifier = Modifier.padding(top = 4.dp))
            }

            Text(
                "Button Style",
                color = SpotVaultColors.OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Shape for action buttons across the app",
                color = SpotVaultColors.Muted,
                fontSize = 12.sp
            )
            ButtonStylePicker(
                selectedStyle = buttonStyle,
                onStyleSelected = { style ->
                    buttonStyle = style
                    ThemeState.buttonStyle = style
                    WidgetThemeHelper.commitThemeChange(prefs, settingsContext) {
                        putString("button_style", style)
                    }
                },
                isLocked = { id -> !premiumUnlocked && id != PremiumFreeTier.freeButtonStyleId },
                onLockedClick = goToPremium
            )
            if (!premiumUnlocked) {
                PremiumLockBanner(onUpgradeClick = goToPremium)
            }

            Text(
                "Vault Icon",
                color = SpotVaultColors.OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Choose the animated vault on your bottom bar",
                color = SpotVaultColors.Muted,
                fontSize = 12.sp
            )
            VaultIconStylePicker(
                selectedStyle = vaultIconStyle,
                onStyleSelected = { style ->
                    vaultIconStyle = style
                    ThemeState.vaultIconStyle = style
                    prefs.edit().putString("vault_icon_style", style).apply()
                },
                isLocked = { id -> !premiumUnlocked && id != PremiumFreeTier.freeVaultIconStyleId },
                onLockedClick = goToPremium
            )
            if (!premiumUnlocked) {
                PremiumLockBanner(onUpgradeClick = goToPremium)
            }

            Text(
                "Compass Face",
                color = SpotVaultColors.OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Style used by the compass navigation screen",
                color = SpotVaultColors.Muted,
                fontSize = 12.sp
            )
            CompassStylePicker(
                selectedStyleId = compassStyle,
                onStyleSelected = { style ->
                    compassStyle = style.id
                    ThemeState.compassStyle = style.id
                    prefs.edit().putString("compass_style", style.id).apply()
                },
                isLocked = { id -> !premiumUnlocked && id != PremiumFreeTier.freeCompassStyleId },
                onLockedClick = goToPremium
            )
            if (!premiumUnlocked) {
                PremiumLockBanner(onUpgradeClick = goToPremium)
            }

            SettingsToggleRow(
                title = "AMOLED True Black",
                subtitle = if (premiumUnlocked) "Pure black backgrounds on OLED screens" else "Premium — pure black backgrounds on OLED screens",
                checked = amoledBlack,
                onCheckedChange = { checked ->
                    if (!premiumUnlocked) {
                        goToPremium()
                    } else {
                        amoledBlack = checked
                        WidgetThemeHelper.commitThemeChange(prefs, settingsContext) {
                            putBoolean("amoled_black", checked)
                        }
                        SpotVaultColors.updateAmoled(checked)
                    }
                }
            )

            SettingsToggleRow(
                title = "Reduce Animations",
                subtitle = if (premiumUnlocked) "Minimize motion across the app" else "Premium — minimize motion across the app",
                checked = reduceAnimations,
                onCheckedChange = { checked ->
                    if (!premiumUnlocked) {
                        goToPremium()
                    } else {
                        reduceAnimations = checked
                        ThemeState.reduceAnimations = checked
                        prefs.edit().putBoolean("reduce_animations", checked).apply()
                    }
                }
            )
            if (!premiumUnlocked) {
                PremiumLockBanner(onUpgradeClick = goToPremium)
            }
        }

        SettingsSectionCard(title = "Text Size", subtitle = "Scales text across the whole app", padding = 10.dp, contentSpacing = 6.dp) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Compact" to 1.0f, "Default" to 1.15f, "Extra Large" to 1.3f).forEach { (label, scale) ->
                    val selected = kotlin.math.abs(textScale - scale) < 0.01f
                    if (selected) {
                        SpotVaultButton(
                            onClick = {
                                textScale = scale
                                ThemeState.textScale = scale
                                prefs.edit().putFloat("text_size_scale", scale).apply()
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                        ) {
                            Text(label, fontSize = 12.sp, lineHeight = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    } else {
                        SpotVaultOutlinedButton(
                            onClick = {
                                textScale = scale
                                ThemeState.textScale = scale
                                prefs.edit().putFloat("text_size_scale", scale).apply()
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                        ) {
                            Text(label, fontSize = 12.sp, lineHeight = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        SettingsSectionCard(title = "Font", subtitle = "Applies everywhere — buttons, headers, and body text", padding = 10.dp, contentSpacing = 6.dp) {
            // Every font is free — unlike the other Appearance pickers, there's no lock/upgrade
            // banner here at all.
            FontFamilyPicker(
                selectedFontId = fontFamilyId,
                onFontSelected = { option ->
                    fontFamilyId = option.id
                    ThemeState.fontFamilyId = option.id
                    prefs.edit().putString("app_font_family", option.id).apply()
                },
                isLocked = { false },
                onLockedClick = goToPremium
            )
        }

        SettingsSectionCard(title = "Background", subtitle = "The pattern behind every screen in the app", padding = 10.dp, contentSpacing = 6.dp) {
            BackgroundPatternPicker(
                selectedPatternId = backgroundPatternId,
                onPatternSelected = { pattern ->
                    backgroundPatternId = pattern.id
                    ThemeState.backgroundPatternId = pattern.id
                    prefs.edit().putString("background_pattern", pattern.id).apply()
                },
                isLocked = { id -> !premiumUnlocked && id != PremiumFreeTier.freeBackgroundPatternId },
                onLockedClick = goToPremium
            )
            if (!premiumUnlocked) {
                PremiumLockBanner(onUpgradeClick = goToPremium, modifier = Modifier.padding(top = 4.dp))
            }
        }

        SettingsSectionCard(
            title = "App Icon",
            subtitle = "Changes your home screen icon — closes the app for a moment to apply",
            padding = 10.dp,
            contentSpacing = 6.dp
        ) {
            AppIconPicker(
                selectedIconId = appIconId,
                onIconSelected = { icon -> pendingIconChange = icon },
                isLocked = { id -> !premiumUnlocked && id != PremiumFreeTier.freeAppIconId },
                onLockedClick = goToPremium,
                premiumUnlocked = premiumUnlocked
            )
            if (!premiumUnlocked) {
                PremiumLockBanner(onUpgradeClick = goToPremium, modifier = Modifier.padding(top = 4.dp))
            }
        }

        SettingsSectionCard(
            title = "Splash Screen",
            subtitle = "The intro animation shown when the app opens",
            padding = 10.dp,
            contentSpacing = 6.dp
        ) {
            SplashStylePicker(
                selectedStyleId = splashStyleId,
                onStyleSelected = { style ->
                    splashStyleId = style.id
                    prefs.edit().putString("splash_style", style.id).apply()
                },
                appIcon = AppIcon.fromId(appIconId),
                isLocked = { id -> !premiumUnlocked && id !in PremiumFreeTier.freeSplashStyleIds },
                onLockedClick = goToPremium
            )
            if (!premiumUnlocked) {
                PremiumLockBanner(onUpgradeClick = goToPremium, modifier = Modifier.padding(top = 4.dp))
            }
        }

        SettingsSectionCard(
            title = "Found Splash Screen",
            subtitle = "The celebration shown when you mark a spot Found — in-app and from widgets",
            padding = 10.dp,
            contentSpacing = 6.dp
        ) {
            FoundSplashStylePicker(
                selectedStyleId = foundSplashStyleId,
                onStyleSelected = { style ->
                    foundSplashStyleId = style.id
                    prefs.edit().putString("found_splash_style", style.id).apply()
                },
                isLocked = { id -> !premiumUnlocked && id !in PremiumFreeTier.freeFoundSplashStyleIds },
                onLockedClick = goToPremium,
                previewTitle = foundSplashTitle.ifBlank { DEFAULT_FOUND_SPLASH_TITLE },
                previewSubtitle = foundSplashSubtitle.ifBlank { DEFAULT_FOUND_SPLASH_SUBTITLE }
            )
            if (!premiumUnlocked) {
                PremiumLockBanner(onUpgradeClick = goToPremium, modifier = Modifier.padding(top = 4.dp))
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                val titleDirty = foundSplashTitle != savedFoundSplashTitle
                OutlinedTextField(
                    value = foundSplashTitle,
                    onValueChange = { input ->
                        if (input.length <= 40 && premiumUnlocked) foundSplashTitle = input
                    },
                    label = { Text("Title") },
                    placeholder = { Text(DEFAULT_FOUND_SPLASH_TITLE, color = SpotVaultColors.Muted.copy(alpha = 0.7f)) },
                    singleLine = true,
                    enabled = premiumUnlocked,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            VoiceMicButton(
                                onResult = { spoken -> if (spoken.length <= 40 && premiumUnlocked) foundSplashTitle = spoken },
                                prompt = "Speak a title…"
                            )
                            IconButton(
                                onClick = {
                                    prefs.edit().putString("found_splash_title", foundSplashTitle.ifBlank { DEFAULT_FOUND_SPLASH_TITLE }).apply()
                                    savedFoundSplashTitle = foundSplashTitle
                                },
                                enabled = titleDirty
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Save title",
                                    tint = if (titleDirty) SpotVaultColors.Teal else SpotVaultColors.Muted.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                )
                if (!premiumUnlocked) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { goToPremium() }
                    )
                    PremiumLockBadge(modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp))
                }
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                val subtitleDirty = foundSplashSubtitle != savedFoundSplashSubtitle
                OutlinedTextField(
                    value = foundSplashSubtitle,
                    onValueChange = { input ->
                        if (input.length <= 80 && premiumUnlocked) foundSplashSubtitle = input
                    },
                    label = { Text("Subtitle") },
                    placeholder = { Text(DEFAULT_FOUND_SPLASH_SUBTITLE, color = SpotVaultColors.Muted.copy(alpha = 0.7f)) },
                    singleLine = true,
                    enabled = premiumUnlocked,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            VoiceMicButton(
                                onResult = { spoken -> if (spoken.length <= 80 && premiumUnlocked) foundSplashSubtitle = spoken },
                                prompt = "Speak a subtitle…"
                            )
                            IconButton(
                                onClick = {
                                    prefs.edit().putString("found_splash_subtitle", foundSplashSubtitle.ifBlank { DEFAULT_FOUND_SPLASH_SUBTITLE }).apply()
                                    savedFoundSplashSubtitle = foundSplashSubtitle
                                },
                                enabled = subtitleDirty
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Save subtitle",
                                    tint = if (subtitleDirty) SpotVaultColors.Teal else SpotVaultColors.Muted.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                )
                if (!premiumUnlocked) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { goToPremium() }
                    )
                    PremiumLockBadge(modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp))
                }
            }
        }
    }

    pendingIconChange?.let { icon ->
        AlertDialog(
            onDismissRequest = { pendingIconChange = null },
            containerColor = SpotVaultColors.Surface,
            titleContentColor = SpotVaultColors.OnSurface,
            textContentColor = SpotVaultColors.Muted,
            title = { Text("Change App Icon?") },
            text = {
                Text("Switching to \"${icon.label}\" will close DropPin Vault for a moment so Android can apply the new icon — just reopen it from your home screen right after.")
            },
            confirmButton = {
                TextButton(onClick = {
                    AppIconManager.applyIcon(settingsContext, prefs, icon)
                    appIconId = icon.id
                    pendingIconChange = null
                    coroutineScope.launch {
                        // setComponentEnabledSetting() returning doesn't mean the change has
                        // fully propagated — system_server still has to persist it and notify
                        // the launcher afterward, with no public API for this process to wait on
                        // that. 400ms wasn't enough headroom for that follow-up work to finish
                        // before this process died on at least some devices/launchers (Samsung's
                        // One UI in particular is known to be slower than stock Android for this
                        // exact activity-alias pattern) — more headroom here costs nothing since
                        // the app is about to die either way.
                        delay(1200)
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                }) {
                    Text("Change Icon", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingIconChange = null }) {
                    Text("Cancel", color = SpotVaultColors.Muted)
                }
            }
        )
    }
}

/** Each card renders its own label in that font — a genuine live preview, not just a name.
 * Horizontally scrollable so ten font options don't turn into a tall wall of rows. */
@Composable
private fun FontFamilyPicker(
    selectedFontId: String,
    onFontSelected: (AppFontOptions.Option) -> Unit,
    modifier: Modifier = Modifier,
    isLocked: (String) -> Boolean = { false },
    onLockedClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxWidth()) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppFontOptions.all.forEach { option ->
            val selected = option.id == selectedFontId
            val locked = isLocked(option.id)
            val family = remember(option.id) { AppFontOptions.familyFor(option.id) }
            Box(modifier = Modifier.width(132.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (locked) 0.45f else 1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) SpotVaultColors.Teal.copy(alpha = 0.12f) else SpotVaultColors.Elevated.copy(alpha = 0.5f))
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) SpotVaultColors.Teal else SpotVaultColors.Outline.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { if (locked) onLockedClick() else onFontSelected(option) }
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option.label,
                        fontFamily = family,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (selected) SpotVaultColors.Teal else SpotVaultColors.OnSurface,
                        maxLines = 1
                    )
                    if (selected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = SpotVaultColors.Teal,
                            modifier = Modifier.padding(start = 4.dp).size(14.dp)
                        )
                    }
                }
                Text(
                    text = "The quick fox jumps",
                    fontFamily = family,
                    fontSize = 12.sp,
                    color = SpotVaultColors.Muted,
                    maxLines = 2
                )
            }
            if (locked) {
                PremiumLockBadge(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
            }
        }
    }
    TrailingScrollHint(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
fun WidgetsSettingsContent(prefs: SharedPreferences) {
    val settingsContext = LocalContext.current
    var widgetThemeSync by remember { mutableStateOf(prefs.getBoolean("widget_theme_sync", true)) }
    var widgetRefreshOnOpen by remember { mutableStateOf(prefs.getBoolean("widget_refresh_on_open", true)) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionCard(
            title = "Widget Behavior",
            subtitle = "Launcher widgets that mirror your vault"
        ) {
            SettingsToggleRow(
                title = "Sync Widget Colors With Theme",
                subtitle = "Widgets match your in-app color theme",
                checked = widgetThemeSync,
                onCheckedChange = {
                    widgetThemeSync = it
                    saveThemePref(prefs, settingsContext) { putBoolean("widget_theme_sync", it) }
                }
            )
            SettingsToggleRow(
                title = "Refresh Widgets on App Open",
                subtitle = "Force widgets to update immediately when opening the app, bypassing Android's 30-minute background limit.",
                checked = widgetRefreshOnOpen,
                onCheckedChange = {
                    widgetRefreshOnOpen = it
                    prefs.edit().putBoolean("widget_refresh_on_open", it).apply()
                }
            )
            Text(
                "Available Launcher Widgets",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SpotVaultColors.Teal,
                letterSpacing = 0.4.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Column(
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Quick Actions (the 4x1) is the one widget that's free — Favorites, Tag Filter,
                // and the Premium Dashboard all require Premium to unlock and show a "Unlock
                // Premium" card instead of live content until then.
                val launcherWidgets = listOf(
                    Triple("Quick Actions", "Pin, Share, and Track in one tap.", false),
                    Triple("Favorites", "A scrollable list of your starred spots.", true),
                    Triple("Tag Filter", "View spots matching one or more tags you pick.", true),
                    Triple("Premium Dashboard", "Recent history, quick toggles, and live tracking views.", true)
                )
                launcherWidgets.forEach { (label, description, isPremiumWidget) ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("•  ", color = SpotVaultColors.Muted, fontSize = 13.sp, lineHeight = 18.sp)
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = SpotVaultColors.OnSurface)) {
                                    append(label)
                                }
                                if (isPremiumWidget) {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)) {
                                        append(" (Premium)")
                                    }
                                }
                                withStyle(SpanStyle(color = SpotVaultColors.Muted)) {
                                    append(": $description")
                                }
                            },
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
                Text(
                    "Quick Actions is free. The other widgets need Premium — placing one shows an Unlock Premium card until you do.",
                    color = SpotVaultColors.Muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        SettingsSectionCard(
            title = "Quick Settings Tiles",
            subtitle = "Pin or track from your notification shade — no need to open the app"
        ) {
            Text(
                "Quick Pin silently saves your current location in one tap. Quick Track starts active tracking, then tap again to mark it Found.",
                color = SpotVaultColors.Muted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            // Shared by both buttons below — same requestAddTileService flow, just aimed at a
            // different service class with a label that actually matches it. Passing "Quick Pin"
            // for QuickTrackTileService (or vice versa) is exactly what made the system's add-tile
            // prompt show the wrong name before these were split into two real services.
            fun addQuickSettingsTile(serviceClass: Class<out android.service.quicksettings.TileService>, label: String) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    val statusBarManager = settingsContext.getSystemService(android.app.StatusBarManager::class.java)
                    // The system's own add-tile dialog isn't always a clear enough signal on its
                    // own (easy to miss, and varies by OEM) — resultCallback hands back a real
                    // result code, so this shows its own explicit toast instead of assuming the
                    // dialog was confirmation enough. DIALOG_DISMISSED (user backed out without
                    // choosing) intentionally shows nothing.
                    statusBarManager?.requestAddTileService(
                        android.content.ComponentName(settingsContext, serviceClass),
                        label,
                        android.graphics.drawable.Icon.createWithResource(settingsContext, R.drawable.ic_spotvault_monochrome),
                        settingsContext.mainExecutor,
                        java.util.function.Consumer { result ->
                            val message = when (result) {
                                android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                                    "$label tile added — check your notification shade."
                                android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                                    "$label tile is already in your notification shade."
                                android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
                                    "$label tile wasn't added."
                                else -> null
                            }
                            if (message != null) {
                                android.widget.Toast.makeText(settingsContext, message, android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                } else {
                    android.widget.Toast.makeText(
                        settingsContext,
                        "Pull down your notification shade, tap Edit, then add \"$label\".",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
            SpotVaultButton(
                onClick = { addQuickSettingsTile(QuickPinTileService::class.java, "Quick Pin") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = spotVaultButtonShape()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Quick Pin Tile", fontWeight = FontWeight.Bold)
            }
            SpotVaultButton(
                onClick = { addQuickSettingsTile(QuickTrackTileService::class.java, "Quick Track") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = spotVaultButtonShape()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Quick Track Tile", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PremiumSettingsContent(prefs: SharedPreferences) {
    val settingsContext = LocalContext.current
    var premiumUnlocked by remember { mutableStateOf(isPremiumUnlocked(prefs)) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionCard(
            title = "Premium Unlocks",
            subtitle = "Upgrade options available across the app — more coming to other tabs over time"
        ) {
            // Toggle only — real Google Play Billing purchase-gating still needs to be wired later.
            SettingsToggleRow(
                title = "Unlock Everything",
                subtitle = "Every color theme, button style, vault icon, compass face, font, background, app icon, and the Premium Dashboard, Vault Favorites, and Tag Filter widgets — all at once",
                checked = premiumUnlocked,
                onCheckedChange = {
                    premiumUnlocked = it
                    // commitThemeChange refreshes every placed widget so an already-placed
                    // Premium widget flips out of its locked state right away instead of
                    // needing to be removed and re-added to pick up the change.
                    WidgetThemeHelper.commitThemeChange(prefs, settingsContext.applicationContext) {
                        // Reverting locked cosmetics BEFORE flipping the entitlement flag itself,
                        // in the same commit — revertLockedCosmeticsToFree reads the currently
                        // active selections while premium is still true in prefs, then falls them
                        // back to free defaults (and updates ThemeState so it takes effect
                        // instantly, not just on next launch) whenever it's turned off.
                        if (!it) {
                            revertLockedCosmeticsToFree(prefs, this)
                        }
                        putBoolean(PREMIUM_UNLOCKED_PREF, it)
                    }
                }
            )
        }
    }
}

@Composable
fun VaultSettingsContent(prefs: SharedPreferences, dao: LocationDao) {
    val settingsContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var spotCount by remember { mutableStateOf(0) }
    var defaultActiveTracking by remember { mutableStateOf(prefs.getBoolean("default_active_tracking", true)) }
    var defaultTimerMins by remember { mutableStateOf(prefs.getString("default_timer_mins", "") ?: "") }
    var defaultSort by remember { mutableStateOf(prefs.getString("default_sort_order", "Newest") ?: "Newest") }
    var confirmDelete by remember { mutableStateOf(prefs.getBoolean("confirm_delete", true)) }
    var autoDeleteEnabled by remember { mutableStateOf(prefs.getBoolean("auto_delete_enabled", false)) }
    var autoDeleteInterval by remember { mutableStateOf(prefs.getString("auto_delete_interval", AutoDeleteInterval.MONTH.id) ?: AutoDeleteInterval.MONTH.id) }
    var showAutoDeleteIntervalDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showTagManager by remember { mutableStateOf(false) }
    var showArchivedSpots by remember { mutableStateOf(false) }
    var autoDeleteIntervalMenuExpanded by remember { mutableStateOf(false) }
    val tagDao = remember { AppDatabase.getDatabase(settingsContext).tagDao() }
    val spotPhotoDao = remember { AppDatabase.getDatabase(settingsContext).spotPhotoDao() }

    LaunchedEffect(Unit) {
        spotCount = withContext(Dispatchers.IO) {
            dao.countNonWishlistSpots()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionCard(
            title = "Vault Snapshot",
            subtitle = "Quick stats from your secure history"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Saved Spots", color = SpotVaultColors.Muted, fontSize = 13.sp)
                    Text(
                        "$spotCount",
                        color = SpotVaultColors.OnSurface,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Active Theme", color = SpotVaultColors.Muted, fontSize = 13.sp)
                    Text(
                        ThemeState.currentTheme.replace('_', ' ').replaceFirstChar { it.uppercase() },
                        color = SpotVaultColors.PrimaryBright,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        SettingsSectionCard(
            title = "Auto Delete",
            subtitle = "Automatically move old spots to Recently Deleted"
        ) {
            SettingsToggleRow(
                title = "Auto Delete",
                subtitle = "Favorites are never touched — purged spots still land in Recently Deleted first",
                checked = autoDeleteEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        // Turning on requires picking a retention window first — see
                        // AutoDeleteIntervalPickerDialog below. Turning off needs no confirmation.
                        showAutoDeleteIntervalDialog = true
                    } else {
                        autoDeleteEnabled = false
                        prefs.edit().putBoolean("auto_delete_enabled", false).apply()
                        cancelAutoDelete(settingsContext)
                    }
                }
            )
            if (showAutoDeleteIntervalDialog) {
                AutoDeleteIntervalPickerDialog(
                    initialInterval = AutoDeleteInterval.fromId(autoDeleteInterval),
                    onConfirm = { interval ->
                        autoDeleteEnabled = true
                        autoDeleteInterval = interval.id
                        prefs.edit()
                            .putBoolean("auto_delete_enabled", true)
                            .putString("auto_delete_interval", interval.id)
                            .apply()
                        scheduleAutoDelete(settingsContext)
                        showAutoDeleteIntervalDialog = false
                    },
                    onDismiss = { showAutoDeleteIntervalDialog = false }
                )
            }
            if (autoDeleteEnabled) {
                Box {
                    SettingsActionRow(
                        title = "Delete Spots",
                        subtitle = AutoDeleteInterval.fromId(autoDeleteInterval).label,
                        onClick = { autoDeleteIntervalMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = autoDeleteIntervalMenuExpanded,
                        onDismissRequest = { autoDeleteIntervalMenuExpanded = false }
                    ) {
                        AutoDeleteInterval.entries.forEach { interval ->
                            DropdownMenuItem(
                                text = { Text(interval.label) },
                                onClick = {
                                    autoDeleteInterval = interval.id
                                    prefs.edit().putString("auto_delete_interval", interval.id).apply()
                                    autoDeleteIntervalMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        SettingsSectionCard(title = "Tags", subtitle = "Rename or delete tags used to organize spots across the Vault") {
            SpotVaultOutlinedButton(
                onClick = { showTagManager = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Sell, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.padding(end = 8.dp))
                Text("Manage Tags", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold)
            }
        }

        SettingsSectionCard(title = "Archive", subtitle = "Spots hidden from the Vault but kept forever") {
            SpotVaultOutlinedButton(
                onClick = { showArchivedSpots = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Archive, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.padding(end = 8.dp))
                Text("Archived Spots", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold)
            }
        }

        SettingsSectionCard(
            title = "Save Defaults",
            subtitle = "How new spots behave out of the box"
        ) {
            SettingsToggleRow(
                title = "Default Save Mode",
                subtitle = if (defaultActiveTracking) "Active Tracking (live timer)" else "Quiet Save (vault only)",
                checked = defaultActiveTracking,
                onCheckedChange = {
                    defaultActiveTracking = it
                    prefs.edit().putBoolean("default_active_tracking", it).apply()
                }
            )
            OutlinedTextField(
                value = defaultTimerMins,
                onValueChange = {
                    defaultTimerMins = it
                    prefs.edit().putString("default_timer_mins", it).apply()
                },
                label = { Text("Default Timer (minutes)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        }

        SettingsSectionCard(
            title = "Vault Behavior",
            subtitle = "Sorting, safety, and feedback"
        ) {
            Box {
                SettingsActionRow(
                    title = "Default Sort Order",
                    subtitle = defaultSort,
                    onClick = { showSortMenu = true }
                )
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    VaultSortOption.all.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                defaultSort = option
                                prefs.edit().putString("default_sort_order", option).apply()
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
            SettingsToggleRow(
                title = "Confirm Before Delete",
                subtitle = "Ask before removing vault spots",
                checked = confirmDelete,
                onCheckedChange = {
                    confirmDelete = it
                    prefs.edit().putBoolean("confirm_delete", it).apply()
                }
            )
        }

        SettingsSectionCard(
            title = "Danger Zone",
            subtitle = "Destructive actions"
        ) {
            ClearAllVaultDataButton(dao = dao, spotPhotoDao = spotPhotoDao, coroutineScope = coroutineScope)
        }
    }

    if (showArchivedSpots) {
        ArchivedSpotsDialog(dao = dao, spotPhotoDao = spotPhotoDao, onDismiss = { showArchivedSpots = false })
    }

    if (showTagManager) {
        VaultTagManagerDialog(tagDao = tagDao, coroutineScope = coroutineScope, onDismiss = { showTagManager = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPinSettingsContent(prefs: SharedPreferences) {
    var showInstantActionsRow by remember { mutableStateOf(prefs.getBoolean("show_instant_actions_row", true)) }
    var quickPinAutoClose by remember { mutableStateOf(prefs.getBoolean("quick_pin_auto_close", true)) }
    var quickPinAutoPhoto by remember { mutableStateOf(prefs.getBoolean("quick_pin_auto_photo", false)) }
    var quickTrackAutoPhoto by remember { mutableStateOf(prefs.getBoolean("quick_track_auto_photo", false)) }
    var quickPinNamingFormat by remember { mutableStateOf(prefs.getString("quick_pin_naming_format", "datetime") ?: "datetime") }
    var namingExpanded by remember { mutableStateOf(false) }
    // "Category" was retired from these labels — this only ever controlled whether the Quick
    // Pin's own label (e.g. "Parking", "Restroom") was followed by a date/time, coordinates, or
    // nothing else in the auto-generated title; it never had anything to do with the old
    // category system.
    val namingOptions = listOf(
        "datetime" to "Label - Date & Time",
        "coordinates" to "Label - Coordinates",
        "label_only" to "Label Only"
    )
    val currentNamingLabel = namingOptions.firstOrNull { it.first == quickPinNamingFormat }?.second
        ?: "Label - Date & Time"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionCard(
            title = "Quick-Pin Behavior",
            subtitle = "Tactical quick-save sheet and auto-naming"
        ) {
            SettingsToggleRow(
                title = "Auto-Close After Save",
                subtitle = "Off keeps the Quick-Pin sheet open so you can drop several pins in a row.",
                checked = quickPinAutoClose,
                onCheckedChange = {
                    quickPinAutoClose = it
                    prefs.edit().putBoolean("quick_pin_auto_close", it).apply()
                }
            )
            ExposedDropdownMenuBox(
                expanded = namingExpanded,
                onExpandedChange = { namingExpanded = it }
            ) {
                OutlinedTextField(
                    value = currentNamingLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Auto-Pin Naming") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = namingExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                )
                DropdownMenu(
                    expanded = namingExpanded,
                    onDismissRequest = { namingExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    namingOptions.forEach { (value, optionLabel) ->
                        DropdownMenuItem(
                            text = { Text(optionLabel) },
                            onClick = {
                                quickPinNamingFormat = value
                                prefs.edit().putString("quick_pin_naming_format", value).apply()
                                namingExpanded = false
                            }
                        )
                    }
                }
            }
            Text(
                "Only applies to your custom Quick Pins from the Manage sheet — the plain Quick Pin and Quick Track buttons always use your street address instead.",
                color = SpotVaultColors.Muted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        SettingsSectionCard(
            title = "Auto-Attach Photo",
            subtitle = "Only applies to the Quick Pin / Quick Track buttons inside the app — quick actions launched from the home screen widget always save without opening the camera."
        ) {
            SettingsToggleRow(
                title = "Auto-Attach Photo — Quick Pin",
                subtitle = "Opens the camera first, then saves quietly with the photo attached",
                checked = quickPinAutoPhoto,
                onCheckedChange = {
                    quickPinAutoPhoto = it
                    prefs.edit().putBoolean("quick_pin_auto_photo", it).apply()
                }
            )
            SettingsToggleRow(
                title = "Auto-Attach Photo — Quick Track",
                subtitle = "Opens the camera first, then jumps straight to the active-tracking screen with the photo",
                checked = quickTrackAutoPhoto,
                onCheckedChange = {
                    quickTrackAutoPhoto = it
                    prefs.edit().putBoolean("quick_track_auto_photo", it).apply()
                }
            )
        }

        SettingsSectionCard(
            title = "Home Screen Shortcuts",
            subtitle = "The everyday quick-action row below Snap & Pin"
        ) {
            SettingsToggleRow(
                title = "Quick Pin / Quick Track Row",
                subtitle = "The two one-tap buttons below Snap & Pin on the home screen",
                checked = showInstantActionsRow,
                onCheckedChange = {
                    showInstantActionsRow = it
                    prefs.edit().putBoolean("show_instant_actions_row", it).apply()
                }
            )
        }
    }
}

@Composable
fun TimerSettingsContent(
    prefs: SharedPreferences,
    onPickRingtone: () -> Unit,
    onNavigateToCategory: (String) -> Unit = {}
) {
    val settingsContext = LocalContext.current
    var premiumUnlocked by remember { mutableStateOf(isPremiumUnlocked(prefs)) }
    val goToPremium: () -> Unit = { onNavigateToCategory("premium") }
    var vibrationEnabled by remember { mutableStateOf(prefs.getBoolean("vibration_enabled", true)) }
    var timerEarlyWarning by remember { mutableStateOf(prefs.getBoolean("timer_early_warning", true)) }
    var showOnLockScreen by remember { mutableStateOf(prefs.getBoolean("show_on_lock_screen", true)) }
    var keepScreenOn by remember { mutableStateOf(prefs.getBoolean("keep_screen_on", false)) }
    var clickSoundId by remember { mutableStateOf(prefs.getString("click_sound", AppSounds.ClickSound.NONE.id) ?: AppSounds.ClickSound.NONE.id) }
    var vaultSoundId by remember { mutableStateOf(prefs.getString("vault_sound", AppSounds.VaultSound.NONE.id) ?: AppSounds.VaultSound.NONE.id) }
    var hapticFeedback by remember { mutableStateOf(prefs.getBoolean("haptic_feedback", false)) }
    var hapticPattern by remember {
        mutableStateOf(premiumGatedId(prefs, prefs.getString("haptic_pattern", HapticPattern.CRISP_CLICK.id) ?: HapticPattern.CRISP_CLICK.id, PremiumFreeTier.freeHapticPatternId))
    }
    var batteryOptDisabled by remember { mutableStateOf(isBatteryOptimizationDisabled(settingsContext)) }

    // requestBatteryOptimizationExemption() below just fires the system settings screen and
    // returns immediately — the immediate isBatteryOptimizationDisabled() re-check right after it
    // reads the state before the user has even seen that screen, let alone answered it, so it
    // always reads as still-disabled. There's no launcher callback for a system settings screen
    // either (the user backs out on their own), so nothing else updates this state when they
    // return — which is why the button used to require a second tap: that second tap's own
    // immediate check just happened to land after the real grant. Same fix already used by
    // Automatic Parking settings for this identical case — re-read on ON_RESUME instead.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                batteryOptDisabled = isBatteryOptimizationDisabled(settingsContext)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Active Tracking's pinned timer runs as a foreground service — OEMs like Samsung and
        // Xiaomi still kill it in the background unless the app is exempted from battery
        // optimization. Auto-Park's settings screen already has this same prompt for its own
        // Bluetooth/WorkManager background work; this is the equivalent for the timer/tracking
        // path, which had no nudge of its own before.
        SettingsSectionCard(
            title = "Background Reliability",
            subtitle = "Keep the tracking timer alive when the app isn't in the foreground"
        ) {
            PermissionStatusRow(
                title = "Battery optimization disabled",
                granted = batteryOptDisabled,
                explanation = "Some phones kill the active-tracking notification in the background unless DropPin Vault is exempted. This is the main reliability fix on Samsung, Xiaomi, etc."
            )
            if (!batteryOptDisabled) {
                AutoParkActionButton(
                    text = "Disable Battery Optimization",
                    icon = Icons.Default.BatteryAlert,
                    onClick = {
                        requestBatteryOptimizationExemption(settingsContext)
                        batteryOptDisabled = isBatteryOptimizationDisabled(settingsContext)
                    }
                )
            }
            // Same reasoning as Automatic Parking settings' identical note — standard battery-
            // optimization exemption doesn't cover every OEM's own separate background-app
            // allowlist (Samsung's "Sleeping apps," Huawei's "Protected apps," etc.).
            Text(
                "Some phones also have their own separate battery/background app list (sometimes called Autostart, Protected Apps, or similar) — check your phone's battery settings if the tracking notification still isn't staying alive after enabling this.",
                color = SpotVaultColors.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        SettingsSectionCard(
            title = "Timers & Notifications",
            subtitle = "Timer and notification behavior"
        ) {
            SettingsActionRow(
                title = "Alert Sound",
                subtitle = "Tone played when a timer expires",
                onClick = onPickRingtone
            )
            SettingsToggleRow(
                title = "Vibration",
                subtitle = "Vibrate when timer alerts fire",
                checked = vibrationEnabled,
                onCheckedChange = {
                    vibrationEnabled = it
                    prefs.edit().putBoolean("vibration_enabled", it).apply()
                }
            )
            SettingsToggleRow(
                title = "10-Minute Timer Warning",
                subtitle = "Heads-up notification before a pin expires",
                checked = timerEarlyWarning,
                onCheckedChange = {
                    timerEarlyWarning = it
                    prefs.edit().putBoolean("timer_early_warning", it).apply()
                }
            )
            SettingsToggleRow(
                title = "Show on Lock Screen",
                subtitle = "Show the active-tracking notification with full details when your phone is locked",
                checked = showOnLockScreen,
                onCheckedChange = {
                    showOnLockScreen = it
                    prefs.edit().putBoolean("show_on_lock_screen", it).apply()
                }
            )
            SettingsToggleRow(
                title = "Keep Screen On While Tracking",
                subtitle = "Prevent sleep during an active pin timer",
                checked = keepScreenOn,
                onCheckedChange = {
                    keepScreenOn = it
                    prefs.edit().putBoolean("keep_screen_on", it).apply()
                }
            )
        }

        SettingsSectionCard(
            title = "Haptic Feedback",
            subtitle = "Tactile response on buttons, toggles, and menus across the app"
        ) {
            SettingsToggleRow(
                title = "Enable Haptics",
                subtitle = "Vibrate on taps throughout the app",
                checked = hapticFeedback,
                onCheckedChange = {
                    hapticFeedback = it
                    prefs.edit().putBoolean("haptic_feedback", it).apply()
                }
            )
            Text(
                "Feel — tap one to preview",
                color = SpotVaultColors.OnSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )
            HapticPatternPicker(
                selectedPatternId = hapticPattern,
                onPatternSelected = { pattern ->
                    hapticPattern = pattern.id
                    prefs.edit().putString("haptic_pattern", pattern.id).apply()
                },
                isLocked = { id -> !premiumUnlocked && id != PremiumFreeTier.freeHapticPatternId },
                onLockedClick = goToPremium
            )
            if (!premiumUnlocked) {
                PremiumLockBanner(onUpgradeClick = goToPremium, modifier = Modifier.padding(top = 4.dp))
            }
        }

        SettingsSectionCard(
            title = "Custom Sounds",
            subtitle = "High-quality sound effects built directly into the app — works perfectly offline."
        ) {
            Text("Button Click", color = SpotVaultColors.OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                "Plays on buttons, toggles, and menu selections across the app",
                color = SpotVaultColors.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            SoundPicker(
                options = AppSounds.ClickSound.entries.map { it.id to it.label },
                selectedId = clickSoundId,
                onSelect = { id ->
                    clickSoundId = id
                    prefs.edit().putString("click_sound", id).apply()
                    AppSounds.preview(settingsContext, id)
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text("Vault Sound", color = SpotVaultColors.OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                "Plays when you open the Vault",
                color = SpotVaultColors.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            SoundPicker(
                options = AppSounds.VaultSound.entries.map { it.id to it.label },
                selectedId = vaultSoundId,
                onSelect = { id ->
                    vaultSoundId = id
                    prefs.edit().putString("vault_sound", id).apply()
                    AppSounds.previewVaultSound(settingsContext, id)
                }
            )
        }
    }
}

/** Auto lets the app keep picking readable text by luminance; White/Black force it — for when
 * a wheel-picked color makes the auto choice look wrong. */
@Composable
private fun TextColorModeRow(
    label: String,
    mode: String,
    onModeChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(label, color = SpotVaultColors.Muted, fontSize = 13.sp)
        Row(
            modifier = Modifier
                .padding(top = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("auto" to "Auto", "white" to "White", "black" to "Black").forEach { (value, text) ->
                val selected = mode == value
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) SpotVaultColors.Teal.copy(alpha = 0.16f) else SpotVaultColors.Elevated.copy(alpha = 0.6f))
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) SpotVaultColors.Teal else SpotVaultColors.Outline.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .clickable { onModeChange(value) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) SpotVaultColors.Teal else SpotVaultColors.OnSurface
                    )
                }
            }
        }
    }
}

/** Tapping a row selects it and immediately plays a preview, via [onSelect]. */
@Composable
private fun SoundPicker(
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (id, label) ->
            val selected = id == selectedId
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) SpotVaultColors.Teal.copy(alpha = 0.16f) else SpotVaultColors.Elevated.copy(alpha = 0.6f))
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) SpotVaultColors.Teal else SpotVaultColors.Outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .clickable { onSelect(id) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) SpotVaultColors.Teal else SpotVaultColors.OnSurface,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun GpsSettingsContent(prefs: SharedPreferences) {
    var highAccuracyGps by remember { mutableStateOf(prefs.getBoolean("high_accuracy_gps", true)) }
    var autoFetchAddress by remember { mutableStateOf(prefs.getBoolean("auto_fetch_address", true)) }
    var distanceUnit by remember { mutableStateOf(loadDistanceUnitFromPrefs(prefs)) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionCard(
            title = "Location & Precision",
            subtitle = "Accuracy, addressing, and measurement units"
        ) {
            SettingsToggleRow(
                title = "High Accuracy GPS",
                subtitle = "Best pin precision (uses more battery)",
                checked = highAccuracyGps,
                onCheckedChange = {
                    highAccuracyGps = it
                    prefs.edit().putBoolean("high_accuracy_gps", it).apply()
                }
            )
            Text(
                "Note: GPS resolves fastest outdoors with a clear view of the sky.",
                color = SpotVaultColors.Muted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            SettingsToggleRow(
                title = "Auto-Resolve Addresses",
                subtitle = "Turn saved coordinates into street addresses",
                checked = autoFetchAddress,
                onCheckedChange = {
                    autoFetchAddress = it
                    prefs.edit().putBoolean("auto_fetch_address", it).apply()
                }
            )
            Text("Distance Units", color = SpotVaultColors.OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                "Preferred measurement for distances across the app.",
                color = SpotVaultColors.Muted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            DistanceUnitSegmentedToggle(
                selectedUnit = distanceUnit,
                onUnitSelected = { unit ->
                    distanceUnit = unit
                    prefs.edit().putString(DISTANCE_UNIT_PREF, unit).apply()
                }
            )
        }
    }
}

/** Destructive "wipe everything" action — shared so the exact same button and two-step
 * (confirm, then type DELETE) safeguard shows up identically wherever it's offered, rather than
 * drifting apart as two hand-copied implementations. */
@Composable
fun ClearAllVaultDataButton(dao: LocationDao, spotPhotoDao: SpotPhotoDao, coroutineScope: kotlinx.coroutines.CoroutineScope) {
    val context = LocalContext.current
    val tagDao = remember { AppDatabase.getDatabase(context).tagDao() }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showClearDataTypeConfirm by remember { mutableStateOf(false) }
    var clearDataConfirmText by remember { mutableStateOf("") }

    SpotVaultButton(
        onClick = { showClearDataConfirm = true },
        colors = ButtonDefaults.buttonColors(
            containerColor = SpotVaultColors.Danger.copy(alpha = 0.18f),
            contentColor = SpotVaultColors.Danger
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Clear All Vault Data", fontWeight = FontWeight.Bold)
    }

    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            containerColor = SpotVaultColors.Surface,
            titleContentColor = SpotVaultColors.OnSurface,
            textContentColor = SpotVaultColors.Muted,
            title = { Text("Clear Vault Data?") },
            text = { Text("This will permanently delete all saved locations and photos, except spots you've archived. This cannot be undone.") },
            confirmButton = {
                SpotVaultButton(
                    onClick = {
                        showClearDataConfirm = false
                        clearDataConfirmText = ""
                        showClearDataTypeConfirm = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotVaultColors.Danger)
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) {
                    Text("Cancel", color = SpotVaultColors.Teal)
                }
            }
        )
    }

    if (showClearDataTypeConfirm) {
        val deleteConfirmed = clearDataConfirmText.trim().equals("DELETE", ignoreCase = true)
        AlertDialog(
            onDismissRequest = {
                showClearDataTypeConfirm = false
                clearDataConfirmText = ""
            },
            containerColor = SpotVaultColors.Surface,
            titleContentColor = SpotVaultColors.OnSurface,
            textContentColor = SpotVaultColors.Muted,
            title = { Text("Type DELETE to Confirm") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This is your final warning. All vault spots and photos will be erased permanently — archived spots are kept.")
                    OutlinedTextField(
                        value = clearDataConfirmText,
                        onValueChange = { clearDataConfirmText = it },
                        label = { Text("Type DELETE") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                SpotVaultButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            // Extra photos (beyond the cover imagePath) have to be cleaned up
                            // before the bulk delete below — deleteAllNonArchivedHistory() cascades
                            // the spot_photos *rows* away via the FK, but never touches the actual
                            // JPEG files those rows pointed at, and once the rows are gone so are
                            // the paths needed to find them. Same leak class as RecentlyDeletedDialog/
                            // ArchivedSpotsDialog's "Delete Forever" had — "Clear All" is the bulk
                            // version of that exact operation and needs the same file cleanup.
                            val spots = dao.getHistoryList().filter { !it.isArchived }
                            spots.forEach { spot ->
                                if (spot.imagePath.isNotEmpty()) {
                                    runCatching { File(spot.imagePath).delete() }
                                }
                                spotPhotoDao.getForSpot(spot.id).forEach { extra ->
                                    runCatching { File(extra.path).delete() }
                                }
                            }
                            dao.deleteAllNonArchivedHistory()
                            tagDao.recomputeAllUsageCounts()
                        }
                        showClearDataTypeConfirm = false
                        clearDataConfirmText = ""
                    },
                    enabled = deleteConfirmed,
                    colors = ButtonDefaults.buttonColors(containerColor = SpotVaultColors.Danger)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showClearDataTypeConfirm = false
                    clearDataConfirmText = ""
                }) {
                    Text("Cancel", color = SpotVaultColors.Teal)
                }
            }
        )
    }
}

/** The frictionless-default backup path offered during onboarding, also reachable here for
 * anyone who skipped it then or wants to check on / disconnect it later. Distinct from the
 * "Backup & Restore" card below it — that one needs a folder picked by hand (SAF) and is the
 * power-user option; this one needs nothing but a Google sign-in and then runs itself weekly via
 * [DriveAutoBackupWorker]. */
@Composable
fun GoogleDriveBackupSection(prefs: SharedPreferences) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    var connected by remember { mutableStateOf(prefs.getBoolean("drive_connected", false)) }
    var accountEmail by remember { mutableStateOf(prefs.getString("drive_account_email", null)) }
    var lastBackupMillis by remember { mutableStateOf(prefs.getLong("drive_last_backup_success", 0L)) }
    var driveState by remember { mutableStateOf<DriveOnboardingState>(DriveOnboardingState.Idle) }
    var consentContinuation by remember { mutableStateOf<((Intent?) -> Unit)?>(null) }
    var showDisconnectConfirm by remember { mutableStateOf(false) }

    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val cont = consentContinuation
        consentContinuation = null
        cont?.invoke(result.data)
    }

    fun connect() {
        val act = activity ?: return
        driveState = DriveOnboardingState.Working
        // Held for the whole call, not just around consentLauncher — the underlying Credential
        // Manager sign-in sheet can take the foreground on its own too, before intentSender is
        // even reached, and App Lock's onStop can't otherwise tell that apart from the user
        // actually leaving the app.
        AppLockGate.begin()
        coroutineScope.launch {
            try {
                val db = AppDatabase.getDatabase(act)
                val result = DriveSyncManager.connectAndSync(act, db, prefs) { intentSender ->
                    suspendCancellableCoroutine { cont ->
                        consentContinuation = { data -> cont.resume(data) }
                        consentLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                }
                result.fold(
                    onSuccess = { outcome ->
                        when (outcome) {
                            is DriveSyncManager.ConnectOutcome.Connected -> {
                                connected = true
                                accountEmail = outcome.email
                                // uploadBackup()/downloadAndRestore() have already returned by this
                                // point, so a fresh upload (if one happened) has already written
                                // this pref — read it back rather than assuming "now", which
                                // papered over uploadBackup() not actually persisting it and made a
                                // restore-only connect look like a backup.
                                lastBackupMillis = prefs.getLong("drive_last_backup_success", 0L)
                                driveState = DriveOnboardingState.Connected(outcome.email, outcome.restoredCount)
                            }
                            is DriveSyncManager.ConnectOutcome.ConflictFound ->
                                driveState = DriveOnboardingState.NeedsConflictResolution(outcome.email, outcome.accessToken)
                        }
                    },
                    onFailure = { driveState = DriveOnboardingState.Failed(it.message ?: "Couldn't connect to Google Drive") }
                )
            } finally {
                // finally, not right after the call — an unexpected exception here must still
                // release the gate, or App Lock would never re-lock again for the rest of the
                // session.
                AppLockGate.end()
            }
        }
    }

    fun resolveConflict(email: String, accessToken: String, choice: DriveSyncManager.ConflictChoice) {
        val act = activity ?: return
        driveState = DriveOnboardingState.Working
        // Not strictly needed today — resolveConflict() reuses the token from connect() and
        // never launches its own consent screen — held anyway so a future change to
        // downloadAndRestore/uploadBackup that does need re-consent doesn't silently reintroduce
        // the exact "App Lock tears down the pending result" bug this pattern exists to prevent.
        AppLockGate.begin()
        coroutineScope.launch {
            try {
                val db = AppDatabase.getDatabase(act)
                val result = DriveSyncManager.resolveConflict(act, db, prefs, email, accessToken, choice)
                result.fold(
                    onSuccess = { restoredCount ->
                        connected = true
                        accountEmail = email
                        lastBackupMillis = prefs.getLong("drive_last_backup_success", 0L)
                        driveState = DriveOnboardingState.Connected(email, restoredCount)
                    },
                    onFailure = { driveState = DriveOnboardingState.Failed(it.message ?: "Couldn't finish connecting Google Drive") }
                )
            } finally {
                AppLockGate.end()
            }
        }
    }

    fun backUpNow() {
        val act = activity ?: return
        driveState = DriveOnboardingState.Working
        AppLockGate.begin()
        coroutineScope.launch {
            try {
                val db = AppDatabase.getDatabase(act)
                val result = DriveSyncManager.backUpNow(act, db, prefs) { intentSender ->
                    suspendCancellableCoroutine { cont ->
                        consentContinuation = { data -> cont.resume(data) }
                        consentLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                }
                result.fold(
                    onSuccess = {
                        lastBackupMillis = prefs.getLong("drive_last_backup_success", 0L)
                        // Only Working/Failed are actually checked by the UI below — Idle just
                        // means "nothing to report," which is exactly right for a plain
                        // successful re-backup (unlike the initial connect, there's no
                        // restored-count to show here).
                        driveState = DriveOnboardingState.Idle
                    },
                    onFailure = { driveState = DriveOnboardingState.Failed(it.message ?: "Couldn't back up to Google Drive") }
                )
            } finally {
                AppLockGate.end()
            }
        }
    }

    SettingsSectionCard(
        title = "Cloud Sync (Google Drive)",
        subtitle = "Automatic, invisible weekly backup — no folder to pick, restorable if you ever lose this phone"
    ) {
        if (connected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDone, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Connected as ${accountEmail ?: "your Google account"}", color = SpotVaultColors.OnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        if (lastBackupMillis > 0) {
                            "Last backup: ${SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US).format(Date(lastBackupMillis))}"
                        } else {
                            "No backup uploaded yet"
                        },
                        color = SpotVaultColors.Muted,
                        fontSize = 12.sp
                    )
                }
            }
            if (driveState is DriveOnboardingState.Failed) {
                Text((driveState as DriveOnboardingState.Failed).message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
            Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpotVaultOutlinedButton(
                    onClick = { backUpNow() },
                    enabled = driveState !is DriveOnboardingState.Working && driveState !is DriveOnboardingState.NeedsConflictResolution,
                    modifier = Modifier.weight(1f)
                ) {
                    if (driveState is DriveOnboardingState.Working) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SpotVaultColors.Teal)
                    } else {
                        Text("Back Up Now", color = SpotVaultColors.Teal)
                    }
                }
                SpotVaultOutlinedButton(
                    onClick = { showDisconnectConfirm = true },
                    enabled = driveState !is DriveOnboardingState.Working,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.Danger),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Disconnect", color = SpotVaultColors.Danger)
                }
            }
        } else {
            Text(
                "If this phone is ever lost, broken, or replaced, an automatic backup means your saved spots aren't.",
                color = SpotVaultColors.Muted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            if (driveState is DriveOnboardingState.Failed) {
                Text((driveState as DriveOnboardingState.Failed).message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
            SpotVaultOutlinedButton(
                onClick = { connect() },
                enabled = driveState !is DriveOnboardingState.Working && driveState !is DriveOnboardingState.NeedsConflictResolution,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) {
                if (driveState is DriveOnboardingState.Working || driveState is DriveOnboardingState.NeedsConflictResolution) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SpotVaultColors.Teal)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.padding(end = 8.dp))
                    Text("Connect Google Drive", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDisconnectConfirm) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirm = false },
            containerColor = SpotVaultColors.Surface,
            titleContentColor = SpotVaultColors.OnSurface,
            textContentColor = SpotVaultColors.Muted,
            title = { Text("Disconnect Google Drive?") },
            text = { Text("Automatic weekly backups will stop. Your existing backup stays in your Drive until you delete it yourself — this only stops DropPin Vault from updating it.") },
            confirmButton = {
                SpotVaultButton(
                    onClick = {
                        cancelDriveAutoBackup(context)
                        prefs.edit().putBoolean("drive_connected", false).apply()
                        connected = false
                        driveState = DriveOnboardingState.Idle
                        showDisconnectConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotVaultColors.Danger)
                ) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirm = false }) {
                    Text("Cancel", color = SpotVaultColors.Teal)
                }
            }
        )
    }

    val conflictState = driveState
    if (conflictState is DriveOnboardingState.NeedsConflictResolution) {
        // No dismiss-without-choosing — see DriveConflictDialog in WelcomeOnboarding.kt for why
        // neither side can be picked automatically. This is the same choice, just reachable from
        // Settings for anyone who skipped Drive during onboarding and connects later.
        AlertDialog(
            onDismissRequest = {},
            containerColor = SpotVaultColors.Surface,
            titleContentColor = SpotVaultColors.OnSurface,
            textContentColor = SpotVaultColors.Muted,
            title = { Text("Which backup do you want to keep?") },
            text = {
                Text("This Google account already has a DropPin Vault backup, and this device already has spots saved locally too. Restoring will replace what's on this device with the Drive backup. Overwriting will replace the Drive backup with what's on this device.")
            },
            confirmButton = {
                SpotVaultButton(onClick = { resolveConflict(conflictState.email, conflictState.accessToken, DriveSyncManager.ConflictChoice.RESTORE_FROM_DRIVE) }) {
                    Text("Restore from Drive", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { resolveConflict(conflictState.email, conflictState.accessToken, DriveSyncManager.ConflictChoice.OVERWRITE_DRIVE_BACKUP) }) {
                    Text("Overwrite Backup", color = SpotVaultColors.Danger)
                }
            }
        )
    }
}

@Composable
fun DataSettingsContent(
    prefs: SharedPreferences,
    dao: LocationDao,
    onAppLockChanged: (Boolean) -> Unit = {}
) {
    val settingsContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val vehicleDao = remember { AppDatabase.getDatabase(settingsContext).vehicleDao() }
    val spotPhotoDao = remember { AppDatabase.getDatabase(settingsContext).spotPhotoDao() }
    val vaultDisplayName = remember { loadVaultDisplayNameFromPrefs(prefs) }
    var appLockEnabled by remember { mutableStateOf(prefs.getBoolean(APP_LOCK_ENABLED_PREF, false)) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var autoBackupEnabled by remember { mutableStateOf(prefs.getBoolean("auto_backup_enabled", false)) }
    var autoBackupTreeUri by remember { mutableStateOf(prefs.getString("auto_backup_tree_uri", null)) }
    var autoBackupFrequency by remember { mutableStateOf(prefs.getString("auto_backup_frequency", "weekly") ?: "weekly") }
    val lastBackupMillis = prefs.getLong("auto_backup_last_success", 0L)

    // AppLockGate.begin()/end() around every SAF picker below — App Lock's re-lock-on-background
    // check can't otherwise tell "the app briefly lost the foreground to a picker it launched
    // itself" apart from "the user actually left the app," and re-locking mid-flow used to tear
    // down (and lose the result of) whichever picker was in flight. See AppLockGate's own doc.
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        AppLockGate.end()
        uri?.let { outputUri ->
            coroutineScope.launch(Dispatchers.IO) {
                val result = VaultBackupManager.exportBackup(settingsContext, dao, vehicleDao, spotPhotoDao, prefs, outputUri)
                val message = result.fold(
                    onSuccess = { count -> "Exported $count spots to backup." },
                    onFailure = { "Export failed: ${it.message ?: "Unknown error"}" }
                )
                withContext(Dispatchers.Main) {
                    backupMessage = message
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        AppLockGate.end()
        if (uri != null) {
            pendingImportUri = uri
            showImportConfirm = true
        }
    }

    val gpxImportLauncher = rememberGpxImportLauncher(dao = dao) { result ->
        AppLockGate.end()
        if (result != null) {
            backupMessage = result.fold(
                onSuccess = { count -> "Imported $count waypoints from GPX." },
                onFailure = { "GPX import failed: ${it.message ?: "Unknown error"}" }
            )
        }
    }

    val pickBackupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        AppLockGate.end()
        if (uri != null) {
            settingsContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            autoBackupTreeUri = uri.toString()
            prefs.edit().putString("auto_backup_tree_uri", uri.toString()).apply()
            if (autoBackupEnabled) scheduleAutoBackup(settingsContext, autoBackupFrequency)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSectionCard(
            title = "Security",
            subtitle = "Control who can open the app itself"
        ) {
            SettingsToggleRow(
                title = "App Lock",
                subtitle = "Require fingerprint, face, or device PIN to open DropPin Vault.",
                checked = appLockEnabled,
                onCheckedChange = { turningOn ->
                    // Checked here too, not just as a self-heal inside requestAppUnlock() — that
                    // fallback already guarantees nobody gets permanently stuck, but without this
                    // check turning App Lock on with nothing enrolled would still show a broken
                    // AppLockScreen at least once before it self-disables on the next unlock
                    // attempt. Catching it here means that screen is never shown at all.
                    val blockedReason = if (!turningOn) {
                        null
                    } else {
                        val canAuth = androidx.biometric.BiometricManager.from(settingsContext).canAuthenticate(
                            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )
                        if (canAuth != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
                            "Set up a screen lock or fingerprint in Android Settings first to use App Lock."
                        } else {
                            null
                        }
                    }
                    if (blockedReason != null) {
                        android.widget.Toast.makeText(settingsContext, blockedReason, android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        appLockEnabled = turningOn
                        prefs.edit().putBoolean(APP_LOCK_ENABLED_PREF, turningOn).apply()
                        onAppLockChanged(turningOn)
                    }
                }
            )
        }

        GoogleDriveBackupSection(prefs = prefs)

        // Split from what used to be one "Backup & Restore" card mixing three different things —
        // a one-off manual export, a one-off manual import, and an entire recurring-schedule
        // sub-flow with its own location/frequency/status — into two focused cards, so each one
        // reads as a single clear choice instead of a wall of controls.
        SettingsSectionCard(
            title = "Manual Backup",
            subtitle = "Export or import your full vault as a single .zip file, whenever you want"
        ) {
            SettingsActionRow(
                title = "Export Vault Backup",
                subtitle = "ZIP with each spot in its own folder — photo, notes.txt, and details together",
                onClick = {
                    AppLockGate.begin()
                    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
                        .format(Date())
                    exportBackupLauncher.launch("DropPinVault_Backup_$stamp.zip")
                }
            )
            SettingsActionRow(
                title = "Import Vault Backup",
                subtitle = "Restore spots, vehicles, and photos from a DropPin Vault .zip backup",
                onClick = {
                    AppLockGate.begin()
                    importBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                }
            )
            Text(
                "Exports a universally readable .zip archive containing all your spots, photos, and an interactive HTML gallery.",
                color = SpotVaultColors.Muted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        SettingsSectionCard(
            title = "Scheduled Local Backup",
            subtitle = "Automatically save a backup archive to a local folder on your device on a regular schedule."
        ) {
            SettingsToggleRow(
                title = "Automatic Backup",
                subtitle = "Turn on scheduled backups to your chosen folder",
                checked = autoBackupEnabled,
                onCheckedChange = { enabled ->
                    autoBackupEnabled = enabled
                    prefs.edit().putBoolean("auto_backup_enabled", enabled).apply()
                    if (enabled && autoBackupTreeUri != null) {
                        scheduleAutoBackup(settingsContext, autoBackupFrequency)
                    } else if (!enabled) {
                        cancelAutoBackup(settingsContext)
                    }
                }
            )
            if (autoBackupEnabled) {
                SettingsActionRow(
                    title = "Backup Location",
                    subtitle = autoBackupTreeUri?.let { "Set — tap to change" } ?: "Not set — tap to choose where backups are saved",
                    onClick = {
                        AppLockGate.begin()
                        pickBackupFolderLauncher.launch(null)
                    }
                )
                SettingsSelectRow(
                    title = "Backup Frequency",
                    selectedLabel = if (autoBackupFrequency == "daily") "Daily" else "Weekly",
                    options = listOf("daily" to "Daily", "weekly" to "Weekly"),
                    onSelect = { value ->
                        autoBackupFrequency = value
                        prefs.edit().putString("auto_backup_frequency", value).apply()
                        if (autoBackupTreeUri != null) scheduleAutoBackup(settingsContext, value)
                    }
                )
                Text(
                    text = if (lastBackupMillis > 0) {
                        "Last backup: ${SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US).format(Date(lastBackupMillis))}"
                    } else {
                        "No automatic backup has run yet"
                    },
                    color = SpotVaultColors.Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        SettingsSectionCard(
            title = "GPX Waypoints",
            subtitle = "Bring waypoints in from another GPS app or mapping tool"
        ) {
            SettingsActionRow(
                title = "Import GPX Waypoints",
                subtitle = "Add waypoints from a GPX file into your vault — coordinates and notes only, no photos",
                onClick = {
                    AppLockGate.begin()
                    gpxImportLauncher.launchGpxImport()
                }
            )
        }

        SettingsSectionCard(
            title = "Data & Privacy",
            subtitle = "Destructive actions"
        ) {
            ClearAllVaultDataButton(dao = dao, spotPhotoDao = spotPhotoDao, coroutineScope = coroutineScope)
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(vaultDisplayName, color = SpotVaultColors.OnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Version 1.0.26", color = SpotVaultColors.Muted, fontSize = 13.sp)
        }
    }

    if (showImportConfirm && pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirm = false
                pendingImportUri = null
            },
            containerColor = SpotVaultColors.Surface,
            titleContentColor = SpotVaultColors.OnSurface,
            textContentColor = SpotVaultColors.Muted,
            title = { Text("Import Backup?") },
            text = { Text("This will add all spots from the backup file into your vault. Existing spots will remain unless you clear them first.") },
            confirmButton = {
                SpotVaultButton(
                    onClick = {
                        val uri = pendingImportUri
                        showImportConfirm = false
                        pendingImportUri = null
                        if (uri != null) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val result = VaultBackupManager.importBackup(
                                    settingsContext,
                                    AppDatabase.getDatabase(settingsContext),
                                    dao,
                                    vehicleDao,
                                    spotPhotoDao,
                                    prefs,
                                    uri,
                                    replaceExisting = false
                                )
                                val message = result.fold(
                                    onSuccess = { count -> "Imported $count spots from backup." },
                                    onFailure = { "Import failed: ${it.message ?: "Unknown error"}" }
                                )
                                withContext(Dispatchers.Main) {
                                    backupMessage = message
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotVaultColors.Primary)
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    pendingImportUri = null
                }) {
                    Text("Cancel", color = SpotVaultColors.Teal)
                }
            }
        )
    }

    backupMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { backupMessage = null },
            containerColor = SpotVaultColors.Surface,
            titleContentColor = SpotVaultColors.OnSurface,
            textContentColor = SpotVaultColors.Muted,
            title = { Text("Backup") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { backupMessage = null }) {
                    Text("OK", color = SpotVaultColors.Teal)
                }
            }
        )
    }
}

@Composable
fun HelpGuideSettingsContent() {
    data class HelpItem(val icon: androidx.compose.ui.graphics.vector.ImageVector, val question: String, val answer: String)

    val topics = listOf(
        "Getting Started" to listOf(
            HelpItem(Icons.Default.CameraAlt, "Snap", "Takes a photo and saves your exact location together — the fastest way to remember a spot you'll want to find again. Reads any visible text in the photo automatically (OCR) so it's searchable later."),
            HelpItem(Icons.Default.LocationOn, "Pin", "Saves your location without a photo, if you just need the spot marked quickly."),
            HelpItem(Icons.Default.Inventory2, "The Vault", "Every spot you've saved lives here — searchable, filterable, sortable, and organized by date. Switch between list and grid view from the toolbar."),
            HelpItem(Icons.Default.FlashOn, "Quick Pin & Quick Track (home screen)", "Two one-tap buttons on the home screen for when you're in a hurry — Quick Pin saves your spot immediately with no dialog, Quick Track does the same but also starts an active tracking timer with a persistent notification. Both also work from the home-screen widget."),
            HelpItem(Icons.Default.Share, "Share button (Snap/Pin confirmation)", "The small share button next to Pin on the Snap/Pin confirmation screen shares your current location as text — or with the photo attached, if you got there via Snap — without needing to finish saving the pin first."),
            HelpItem(Icons.Default.Mic, "Voice input", "Tap the microphone icon on any text field — tags, notes, titles, and more — to dictate instead of typing.")
        ),
        "Quick Pins" to listOf(
            HelpItem(Icons.Default.PushPin, "Quick Pin", "One-tap silent save at your current location — no camera, no dialog. Great for marking something fast while you're moving."),
            HelpItem(Icons.Default.NotificationsActive, "Quick Track", "Same one-tap save, but keeps a persistent notification reminding you to find your way back — tap \"Found\" once you're there, or navigate straight to it with the Navigate or Compass button."),
            HelpItem(Icons.Default.Edit, "Managing your pins", "Tap \"Manage\" inside the Quick-Pin sheet to add your own custom pins with any emoji, reorder them, or delete ones you don't use."),
            HelpItem(Icons.Default.FormatSize, "Auto-naming format", "Choose how new pins get titled by default in Quick-Pin Behavior settings — by label and date/time, label and coordinates, or label only.")
        ),
        "The Vault" to listOf(
            HelpItem(Icons.Default.FilterList, "Filtering", "Favorites and your most-used tags show as quick chips right above your spots — tap the filter icon next to them for the full tag cloud with search, or to switch to filtering by vehicle instead."),
            HelpItem(Icons.Default.GridView, "List vs. Grid view", "Switch between a scrollable list and a two-column photo grid from the toolbar above your spots — pick whichever you scan faster."),
            HelpItem(Icons.Default.Star, "Favorites", "Swipe or tap the star on any spot to favorite it — favorites are protected from Auto Delete and easy to filter to."),
            HelpItem(Icons.Default.Map, "Browse by Location", "Tap the map icon above your spots to see every saved pin plotted on a map, grouped by state/region and city — each entry has the same ⋮ menu (edit, share, add photo, edit tags, favorite) as the main Vault list."),
            HelpItem(Icons.Default.DateRange, "Browse by Calendar", "Tap the calendar icon to jump to any day you've saved spots on and see just that day's results — tap the month/year at the top to jump further back."),
            HelpItem(Icons.Default.Sell, "Tags", "Assign any number of tags to a spot from its ⋮ menu — type to reuse an existing tag or create a new one. Rename or delete tags globally from Settings → Vault → Manage Tags."),
            HelpItem(Icons.Default.Edit, "Editing a spot", "Tap Edit from a spot's ⋮ menu or its full-screen photo view to change its title, date, city, state, or notes."),
            HelpItem(Icons.Default.History, "Recently Deleted", "Deleted spots land here first, not gone for good — restore them or clear them out permanently whenever you're ready."),
            HelpItem(Icons.Default.AutoDelete, "Auto Delete", "Turn on Auto Delete (in Vault settings, or the pill next to Recently Deleted) to automatically move spots older than a timeframe you pick into Recently Deleted. Favorites and archived spots are never touched."),
            HelpItem(Icons.Default.Archive, "Archive", "Hide a spot from the main Vault without deleting it — archived spots are kept forever, never shown in Auto Delete, and won't be touched by Clear All Vault Data. Archive a spot from its ⋮ menu, and view or restore archived spots from Settings → Vault → Archived Spots."),
            HelpItem(Icons.Default.Inventory2, "Vault Snapshot", "A quick stat card at the top of Vault settings showing your total saved spots and current active theme.")
        ),
        "Vehicles & Automatic Parking" to listOf(
            HelpItem(Icons.Default.DirectionsCar, "Vehicles", "Add each of your vehicles with its own name, color, and icon, and link it to that car's Bluetooth so DropPin Vault knows which vehicle just disconnected."),
            HelpItem(Icons.Default.Bluetooth, "Automatic Parking", "When your phone disconnects from a linked vehicle's Bluetooth, DropPin Vault automatically saves a parking pin and starts active tracking — even if the app isn't open. Turn it on in Automatic Parking settings."),
            HelpItem(Icons.AutoMirrored.Filled.DirectionsWalk, "Motion & Fitness", "An extra layer on top of Automatic Parking — while connected to a linked car's Bluetooth, DropPin Vault watches for the moment you start walking away and caches a precise fix right then, for a more accurate pin than waiting for Bluetooth to fully disconnect."),
            HelpItem(Icons.Default.LocationOn, "Quiet Zones", "Mark areas like home or work as Quiet Zones so Automatic Parking skips saving there entirely, or saves silently without a notification."),
            HelpItem(Icons.Default.Bluetooth, "Home screen toggles", "Two small buttons near the top of the home screen let you flip Automatic Parking and Motion & Fitness on or off at a glance, with a link to their full settings.")
        ),
        "Security & Backup" to listOf(
            HelpItem(Icons.Default.Lock, "App Lock", "Require your fingerprint, face, or device PIN before DropPin Vault opens — turn it on under Security in Backup, Data & Privacy settings."),
            HelpItem(Icons.Default.Storage, "Manual Backup", "Export your entire vault — every spot, photo, and note — to a single .zip file you control, and import it back on this device or a new one at any time."),
            HelpItem(Icons.Default.Storage, "Scheduled Local Backup", "Point DropPin Vault at a folder and it'll keep a fresh backup there automatically on a schedule you choose, no manual exports needed."),
            HelpItem(Icons.Default.CloudDone, "Cloud Sync (Google Drive)", "The frictionless, no-folder-to-pick option — connect once (during onboarding or from Settings → Data → Cloud Sync) and DropPin Vault automatically backs itself up weekly to a hidden folder in your own Google Drive, invisible to every other app. If this phone is ever lost, broken, or replaced, connecting the same account on a new one offers to restore it automatically. Entirely optional, and separate from the Manual Backup and Scheduled Local Backup options above — use any combination, or none."),
            HelpItem(Icons.Default.FileUpload, "GPX Waypoints", "Import waypoints from a GPX file — exported from Garmin, Gaia, or another GPS tool — into your vault. Coordinates and notes only, no photos."),
            HelpItem(Icons.Default.Delete, "Clear All Vault Data", "A permanent, type-to-confirm wipe of every saved spot and photo — available in both Vault settings and Backup, Data & Privacy. There's no undo, so export a backup first if you're not sure.")
        ),
        "Appearance & Themes" to listOf(
            HelpItem(Icons.Default.Palette, "Color Theme", "Pick from several preset color palettes, or build your own with the color wheel — everything in the app follows whatever you choose. Two presets are free; the rest, plus the custom color wheel, are Premium."),
            HelpItem(Icons.Default.Palette, "Material You / Dynamic Color", "Premium — extracts real colors from your phone's current wallpaper (Android 12+) and applies them as your theme automatically, the same way your launcher and other Material You apps do."),
            HelpItem(Icons.Default.Category, "Button Style", "Change the shape of buttons across the app — soft rounded corners, a full pill/capsule shape, cut corners, and more playful options. Premium beyond the default Classic shape."),
            HelpItem(Icons.Default.Inventory2, "Vault Icon Style", "Pick which animated vault dial shows on your bottom navigation bar. Premium beyond the default Classic style."),
            HelpItem(Icons.Default.Explore, "Compass Face Style", "Choose the look of the compass dial used on the Compass navigation screen — classic, arrow, dot, sci-fi, and more. Premium beyond the default Classic face."),
            HelpItem(Icons.Default.Palette, "AMOLED True Black", "Premium — switches dark backgrounds to pure black, which can save real battery on OLED/AMOLED screens."),
            HelpItem(Icons.Default.Palette, "Reduce Animations", "Premium — minimizes motion across the app for a calmer feel or better performance on older devices."),
            HelpItem(Icons.Default.FormatSize, "Text Size", "Scale text across the entire app up or down from Compact to Extra Large."),
            HelpItem(Icons.Default.FormatSize, "Font", "Switch the typeface used everywhere in the app, all built into Android so nothing needs downloading — every option is free."),
            HelpItem(Icons.Default.Palette, "Background Patterns", "Premium (beyond the default) — the animated pattern behind every screen: gradient mesh, aurora bands, hex tessellation, light leaks, and more."),
            HelpItem(Icons.Default.Widgets, "App Icon", "Premium (beyond the default) — pick from preset launcher icons."),
            HelpItem(Icons.Default.FlashOn, "Splash Screen", "Premium (beyond the default) — signal forge, vault door, radar scan, triangulate, beacon wake, vault bloom, haunted gate, and winter gate intros using your chosen app icon."),
            HelpItem(Icons.Default.CheckCircle, "Found Splash Screen", "Premium (beyond the default) — fireworks, confetti, victory ripples, star burst, champagne pop, All Hallows' Found, and Yuletide Found celebrations when you mark a spot Found from the app or widgets."),
            HelpItem(Icons.Default.Edit, "Vault Name", "Rename your vault anything you like — it shows on the home screen, splash screen, and widgets.")
        ),
        "Sounds & Alerts" to listOf(
            HelpItem(Icons.Default.Notifications, "Alert Sound", "The tone played when an active-tracking timer expires — pick any ringtone on your device."),
            HelpItem(Icons.Default.Notifications, "Vibration & Lock Screen", "Control whether timer alerts vibrate, warn you 10 minutes early, and show full details on your lock screen."),
            HelpItem(Icons.Default.Notifications, "Haptic Feedback", "Premium (beyond the default pattern) — a physical buzz on taps throughout the app. Choose from several distinct feels, or turn it off entirely."),
            HelpItem(Icons.Default.Notifications, "Custom Sounds", "Real recorded sound effects (not synthesized) for button clicks and for opening the Vault — pick your favorite from several free options for each.")
        ),
        "Location & Precision" to listOf(
            HelpItem(Icons.Default.LocationOn, "High Accuracy GPS", "Uses more battery in exchange for the tightest possible location fix — on by default."),
            HelpItem(Icons.Default.Map, "Auto-Resolve Addresses", "Automatically turns your saved coordinates into a readable street address."),
            HelpItem(Icons.Default.LocationOn, "Distance Units", "Show saved spots' distance from you in miles or kilometers throughout the Vault."),
            HelpItem(Icons.Default.Share, "Sharing a Spot", "Share any saved spot's location — and optionally its photo — through your phone's normal share sheet to any app.")
        ),
        "Compass" to listOf(
            HelpItem(Icons.Default.Explore, "Navigating back to a spot", "Tap the Compass button on an active pin or any saved spot to open a full-screen compass pointing you straight back to it, with live distance and heading."),
            HelpItem(Icons.Default.Palette, "Compass Face Style", "Change the dial's look in Appearance & Themes — the style you pick there is exactly what shows up here.")
        ),
        "Widgets" to listOf(
            HelpItem(Icons.Default.Widgets, "Everyday Quick Actions widget", "Free — add it from your home screen's widget picker for one-tap Quick Pin, Quick Share, and Quick Track. After Quick Track, the widget switches to Found and Navigate until you clear tracking."),
            HelpItem(Icons.Default.Widgets, "Vault Favorites widget", "Premium — a scrollable list of every starred spot. Tap any entry to open it in the Vault. Placing this widget without Premium shows an Unlock Premium card instead."),
            HelpItem(Icons.Default.Sell, "Tag Filter widget", "Premium — pick one or more tags when you add or reconfigure it; the widget shows every saved spot carrying at least one of them, deduplicated, and stays in sync as you add, remove, or retag spots. Placing this widget without Premium shows an Unlock Premium card instead."),
            HelpItem(Icons.Default.Star, "Premium Dashboard widget", "Premium — a larger widget with your most recent Vault entries (tap to open), Snap/Pin/Share Photo shortcuts, Bluetooth Auto and Motion toggles, and a tracking mode with photo plus Found, Navigate, and Compass."),
            HelpItem(Icons.Default.Palette, "Widget theming", "Widgets automatically match your in-app color theme and button shape when \"Sync Widget Colors With Theme\" is on in Widget settings."),
            HelpItem(Icons.Default.FlashOn, "Quick Settings Tiles", "Add a Quick Pin tile (silently saves your current location in one tap) or a Quick Track tile (toggles active tracking, tap again to mark Found) to your phone's Quick Settings (swipe-down) panel — no need to open the app. See Settings → Widgets for how to add either one.")
        ),
        "Premium Unlocks" to listOf(
            HelpItem(Icons.Default.Star, "What's included", "Extra color themes, button styles, vault icons, compass faces, fonts, background patterns, app icons, splash screens, found splash screens, Material You, AMOLED True Black, Reduce Animations, and the full haptic pattern library."),
            HelpItem(Icons.Default.Star, "Unlocking everything", "One toggle in Premium Unlocks settings unlocks every premium option across the app at once.")
        )
    )

    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        topics.forEach { (sectionTitle, items) ->
            val sectionExpanded = expandedSections[sectionTitle] ?: false
            SettingsSectionCard(
                title = sectionTitle,
                subtitle = null,
                collapsible = true,
                expanded = sectionExpanded,
                onExpandChange = { expandedSections[sectionTitle] = !sectionExpanded }
            ) {
                items.forEach { item ->
                    var expanded by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { expanded = !expanded }
                            .padding(vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(SpotVaultColors.Teal.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(item.icon, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(16.dp))
                                }
                                Text(item.question, color = SpotVaultColors.OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            }
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = SpotVaultColors.Teal
                            )
                        }
                        if (expanded) {
                            Text(
                                item.answer,
                                color = SpotVaultColors.Muted,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutSettingsContent() {
    data class IconCredit(val library: String, val license: String, val url: String, val usedFor: String)
    data class SoundCredit(val name: String, val author: String)

    val credits = listOf(
        IconCredit("Font Awesome Free", "CC BY 4.0", "fontawesome.com", "Vehicle icons (car, SUV/van, pickup, motorcycle, bicycle, boat, electric)"),
        IconCredit("Material Symbols", "Apache License 2.0", "fonts.google.com/icons", "Vehicle icons (scooter) and general app icons"),
        IconCredit("Tabler Icons", "MIT License", "tabler.io/icons", "Vehicle icons (other / steering wheel)")
    )

    val vaultSoundCredits = listOf(
        SoundCredit("Vault Door Creak", "Vrmaa"),
        SoundCredit("Airlock Hiss", "NeoSpica"),
        SoundCredit("Lock Click", "Pixeliota"),
        SoundCredit("Safe Door", "Lezaarth")
    )
    val clickSoundCredits = listOf(
        SoundCredit("Sharp Click", "Earth_Cord"),
        SoundCredit("Mouse Click", "Aphom000"),
        SoundCredit("Sci-Fi", "Crash_358"),
        SoundCredit("8-Bit Blip", "Kickhat"),
        SoundCredit("Wood Knock", "NoisyRedFox"),
        SoundCredit("Bubble Pop", "Arttim"),
        SoundCredit("Glass Break", "Bricklover")
    )

    val context = LocalContext.current
    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: android.content.ActivityNotFoundException) {
            android.widget.Toast.makeText(context, "No browser found to open this link.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(DEFAULT_VAULT_DISPLAY_NAME, color = SpotVaultColors.OnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Version 1.0.26", color = SpotVaultColors.Muted, fontSize = 13.sp)
        }

        SettingsSectionCard(title = "Legal", subtitle = "How DropPin Vault handles your data") {
            SettingsActionRow(
                title = "Privacy Policy",
                subtitle = "droppinvault.com/privacy",
                onClick = { openUrl("https://droppinvault.com/privacy") }
            )
            SettingsActionRow(
                title = "Terms of Service",
                subtitle = "droppinvault.com/terms",
                onClick = { openUrl("https://droppinvault.com/terms") }
            )
            SettingsActionRow(
                title = "Website",
                subtitle = "droppinvault.com",
                onClick = { openUrl("https://droppinvault.com") }
            )
        }

        SettingsSectionCard(
            title = "Open Source Icon Credits",
            subtitle = "Some vehicle icons are drawn from these free, open source libraries"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                credits.forEach { credit ->
                    Column {
                        Text(credit.library, color = SpotVaultColors.OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(credit.usedFor, color = SpotVaultColors.Muted, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
                        Text("${credit.license} · ${credit.url}", color = SpotVaultColors.PrimaryBright.copy(alpha = 0.75f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }

        SettingsSectionCard(
            title = "Sound Credits",
            subtitle = "Every button and vault sound is a real recording from freesound.org, released under a CC0 license — free to use with no attribution required, but full credit to the original creators anyway:"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("VAULT SOUNDS", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.8.sp)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    vaultSoundCredits.forEach { credit ->
                        Text("${credit.name} — ${credit.author}", color = SpotVaultColors.Muted, fontSize = 13.sp)
                    }
                }
                Text("BUTTON SOUNDS", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.8.sp, modifier = Modifier.padding(top = 4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    clickSoundCredits.forEach { credit ->
                        Text("${credit.name} — ${credit.author}", color = SpotVaultColors.Muted, fontSize = 13.sp)
                    }
                }
            }
        }

        SettingsSectionCard(title = "Thank You", subtitle = "") {
            Text(
                "$DEFAULT_VAULT_DISPLAY_NAME wouldn't look nearly this good without the icon designers who release their work for free. Full credit and thanks to Font Awesome, Google's Material Symbols team, and Tabler Icons — and to every freesound.org creator whose CC0 recordings give the app its sound.",
                color = SpotVaultColors.Muted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
