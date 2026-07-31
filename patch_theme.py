import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """@Composable
fun SpotVaultTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val darkColors = darkColorScheme("""

replacement = """@Composable
fun SpotVaultTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val baseColors = when (ThemeState.currentTheme) {
        "teal_blue" -> TealBlueColors
        "amber_copper" -> AmberCopperColors
        else -> PurpleTealColors
    }
    
    val themeColors = if (ThemeState.isAmoled) {
        baseColors.copy(
            void = androidx.compose.ui.graphics.Color.Black,
            deep = androidx.compose.ui.graphics.Color.Black,
            surface = androidx.compose.ui.graphics.Color(0xFF080808)
        )
    } else {
        baseColors
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalSpotVaultColors provides themeColors) {
        val darkColors = darkColorScheme("""

if target in content:
    content = content.replace(target, replacement)
    
    # Now we must match the closing brace of SpotVaultTheme.
    # It looks like:
    #     MaterialTheme(
    #         colorScheme = colors,
    #         typography = SpotVaultTypography,
    #         content = content
    #     )
    # }
    
    target_end = """    MaterialTheme(
        colorScheme = colors,
        typography = SpotVaultTypography,
        content = content
    )
}"""

    replacement_end = """    MaterialTheme(
        colorScheme = colors,
        typography = SpotVaultTypography,
        content = content
    )
    }
}"""
    
    content = content.replace(target_end, replacement_end)
    with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Patched SpotVaultTheme")
else:
    print("SpotVaultTheme not found")
