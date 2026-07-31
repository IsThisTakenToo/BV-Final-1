import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("AMOLED True Black", color = SpotVaultColors.OnSurface)
                            androidx.compose.material3.Switch(
                                checked = amoledBlack,
                                onCheckedChange = { 
                                    amoledBlack = it
                                    prefs.edit().putBoolean("amoled_black", it).apply()
                                    SpotVaultColors.updateAmoled(it)
                                }
                            )
                        }"""

replacement = """                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("AMOLED True Black", color = SpotVaultColors.OnSurface)
                            androidx.compose.material3.Switch(
                                checked = amoledBlack,
                                onCheckedChange = { 
                                    amoledBlack = it
                                    prefs.edit().putBoolean("amoled_black", it).apply()
                                    ThemeState.isAmoled = it
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Color Theme", color = SpotVaultColors.OnSurface)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                            // Theme 1: Purple/Teal
                            ThemeSwatch(
                                isSelected = ThemeState.currentTheme == "purple_teal",
                                primary = Color(0xFF8C52FF),
                                accent = Color(0xFF00E5FF),
                                onClick = {
                                    ThemeState.currentTheme = "purple_teal"
                                    prefs.edit().putString("color_theme", "purple_teal").apply()
                                }
                            )
                            // Theme 2: Teal/Blue
                            ThemeSwatch(
                                isSelected = ThemeState.currentTheme == "teal_blue",
                                primary = Color(0xFF00BFA5),
                                accent = Color(0xFF448AFF),
                                onClick = {
                                    ThemeState.currentTheme = "teal_blue"
                                    prefs.edit().putString("color_theme", "teal_blue").apply()
                                }
                            )
                            // Theme 3: Amber/Copper
                            ThemeSwatch(
                                isSelected = ThemeState.currentTheme == "amber_copper",
                                primary = Color(0xFFFFAB00),
                                accent = Color(0xFFFF6E40),
                                onClick = {
                                    ThemeState.currentTheme = "amber_copper"
                                    prefs.edit().putString("color_theme", "amber_copper").apply()
                                }
                            )
                        }"""

if target in content:
    content = content.replace(target, replacement)
    
    # We also need to add ThemeSwatch composable outside SettingsDialog
    swatch_code = """
@Composable
fun ThemeSwatch(isSelected: Boolean, primary: Color, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(primary)
            .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.White else SpotVaultColors.Outline, CircleShape)
            .clickable { onClick() }
    ) {
        // Inner half circle for accent
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                size = androidx.compose.ui.geometry.Size(size.width / 2f, size.height)
            )
        }
    }
}
"""
    # Append swatch_code at the end of the file
    content += swatch_code
    
    with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Patched SettingsDialog")
else:
    print("SettingsDialog target not found")
