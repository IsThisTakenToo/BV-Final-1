with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    content = f.read()

target = """    val Danger: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.Danger
}"""
replacement = """    val Danger: Color @androidx.compose.runtime.Composable get() = LocalSpotVaultColors.current.Danger
    val OnPrimary: Color @androidx.compose.runtime.Composable get() = if (androidx.compose.ui.graphics.luminance(LocalSpotVaultColors.current.Primary) > 0.4f) LocalSpotVaultColors.current.Ink else androidx.compose.ui.graphics.Color.White
}"""

if target in content:
    content = content.replace(target, replacement)
else:
    print("Failed to replace target 1")

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(content)
print("done")
