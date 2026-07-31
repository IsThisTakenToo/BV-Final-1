with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target1 = """    val themeColors = when (ThemeState.currentTheme) {
        "teal_blue" -> TealBlueColors
        "amber_copper" -> AmberCopperColors
        else -> PurpleTealColors
    }"""
replacement1 = """    val themeColors = when (ThemeState.currentTheme) {
        "neon_pink" -> NeonPinkColors
        "lime_magenta" -> LimeMagentaColors
        "crimson_gold" -> CrimsonGoldColors
        else -> PurpleTealColors
    }"""

if target1 in content:
    content = content.replace(target1, replacement1)
else:
    print("Failed to replace target 1")

target2 = """                            val themes = listOf(
                                "purple_teal" to Color(0xFF6B2FFF),
                                "teal_blue" to Color(0xFF00B4D8),
                                "amber_copper" to Color(0xFFFF9100)
                            )
                            themes.forEach { (id, color) ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(color, androidx.compose.foundation.shape.CircleShape)
                                        .border(2.dp, if (ThemeState.currentTheme == id) SpotVaultColors.OnSurface else Color.Transparent, androidx.compose.foundation.shape.CircleShape)
                                        .clickable {
                                            ThemeState.currentTheme = id
                                            prefs.edit().putString("color_theme", id).apply()
                                        }
                                ) {"""
replacement2 = """                            val themes = listOf(
                                Triple("purple_teal", Color(0xFF6B2FFF), Color(0xFF00F0FF)),
                                Triple("neon_pink", Color(0xFFFF008C), Color(0xFF00F0FF)),
                                Triple("lime_magenta", Color(0xFFC6FF00), Color(0xFFFF0080)),
                                Triple("crimson_gold", Color(0xFFFF2E63), Color(0xFFFFD100))
                            )
                            themes.forEach { (id, primary, teal) ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(primary, teal)), androidx.compose.foundation.shape.CircleShape)
                                        .border(2.dp, if (ThemeState.currentTheme == id) SpotVaultColors.OnSurface else Color.Transparent, androidx.compose.foundation.shape.CircleShape)
                                        .clickable {
                                            ThemeState.currentTheme = id
                                            prefs.edit().putString("color_theme", id).apply()
                                        }
                                ) {"""

if target2 in content:
    content = content.replace(target2, replacement2)
else:
    print("Failed to replace target 2")

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
print("done")
