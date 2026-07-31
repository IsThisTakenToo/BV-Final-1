package com.spotvault.app

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

data class SpotVaultColorScheme(
    val Void: Color,
    val Deep: Color,
    val Surface: Color,
    val Elevated: Color,
    val Glass: Color,
    val Primary: Color,
    val PrimaryBright: Color,
    val PrimaryDeep: Color,
    val Teal: Color,
    val TealSoft: Color,
    val TealDeep: Color,
    val Ink: Color,
    val OnSurface: Color,
    val Muted: Color,
    val Outline: Color,
    val Danger: Color,
    val id: String
)

val PurpleTealColors = SpotVaultColorScheme(
    Void = Color(0xFF0C0714),
    Deep = Color(0xFF130B21),
    Surface = Color(0xFF1A1228),
    Elevated = Color(0xFF241938),
    Glass = Color(0xFF1E152D),
    Primary = Color(0xFF6B2FFF),
    PrimaryBright = Color(0xFF9D75FF),
    PrimaryDeep = Color(0xFF4A1BBF),
    Teal = Color(0xFF00F0FF),
    TealSoft = Color(0xFF85F8FF),
    TealDeep = Color(0xFF00B8CC),
    Ink = Color(0xFF0A0F1A),
    OnSurface = Color(0xFFF0EAF8),
    Muted = Color(0xFFA39CB4),
    Outline = Color(0xFF3B2E52),
    Danger = Color(0xFFFF4E64),
    id = "purple_teal"
)

val NeonPinkColors = SpotVaultColorScheme(
    Void = Color(0xFF12060D),
    Deep = Color(0xFF1B0912),
    Surface = Color(0xFF241019),
    Elevated = Color(0xFF321522),
    Glass = Color(0xFF2B121D),
    Primary = Color(0xFFFF008C),
    PrimaryBright = Color(0xFFFF5CB8),
    PrimaryDeep = Color(0xFFB80066),
    Teal = Color(0xFF00F0FF),
    TealSoft = Color(0xFF85F8FF),
    TealDeep = Color(0xFF00B8CC),
    Ink = Color(0xFF12040B),
    OnSurface = Color(0xFFFCEAF3),
    Muted = Color(0xFFB99AAC),
    Outline = Color(0xFF4A2438),
    Danger = Color(0xFFFF4E64),
    id = "neon_pink"
)

val LimeMagentaColors = SpotVaultColorScheme(
    Void = Color(0xFF0B140A),
    Deep = Color(0xFF121F10),
    Surface = Color(0xFF1A2917),
    Elevated = Color(0xFF243A20),
    Glass = Color(0xFF1F3319),
    Primary = Color(0xFFC6FF00),
    PrimaryBright = Color(0xFFDCFF5C),
    PrimaryDeep = Color(0xFF8FB800),
    Teal = Color(0xFFFF0080),
    TealSoft = Color(0xFFFF66B3),
    TealDeep = Color(0xFFCC0066),
    Ink = Color(0xFF0A1408),
    OnSurface = Color(0xFFF0FCE8),
    Muted = Color(0xFFA8C29E),
    Outline = Color(0xFF335C2C),
    Danger = Color(0xFFFF4E4E),
    id = "lime_magenta"
)

val CrimsonGoldColors = SpotVaultColorScheme(
    Void = Color(0xFF150A0D),
    Deep = Color(0xFF1F0F14),
    Surface = Color(0xFF29151B),
    Elevated = Color(0xFF391D25),
    Glass = Color(0xFF321A20),
    Primary = Color(0xFFFF2E63),
    PrimaryBright = Color(0xFFFF7A99),
    PrimaryDeep = Color(0xFFC2003F),
    Teal = Color(0xFFFFD100),
    TealSoft = Color(0xFFFFE566),
    TealDeep = Color(0xFFCCA700),
    Ink = Color(0xFF160A0C),
    OnSurface = Color(0xFFFCEAEE),
    Muted = Color(0xFFC29CA6),
    Outline = Color(0xFF522733),
    Danger = Color(0xFFFF3D3D),
    id = "crimson_gold"
)


val GoldCobaltColors = SpotVaultColorScheme(
    Void = Color(0xFF060A14),
    Deep = Color(0xFF0A1020),
    Surface = Color(0xFF10182C),
    Elevated = Color(0xFF17213A),
    Glass = Color(0xFF141C32),
    Primary = Color(0xFF2962FF),
    PrimaryBright = Color(0xFF6B94FF),
    PrimaryDeep = Color(0xFF1739B0),
    Teal = Color(0xFFFFD100),
    TealSoft = Color(0xFFFFE566),
    TealDeep = Color(0xFFCCA700),
    Ink = Color(0xFF0B0F1A),
    OnSurface = Color(0xFFEAF0FF),
    Muted = Color(0xFF9AA8C2),
    Outline = Color(0xFF29365A),
    Danger = Color(0xFFFF4E4E),
    id = "gold_cobalt"
)

val LocalSpotVaultColors = staticCompositionLocalOf { PurpleTealColors }

object ThemeState {
    var currentTheme by mutableStateOf("purple_teal")
    var isAmoled by mutableStateOf(false)
}
