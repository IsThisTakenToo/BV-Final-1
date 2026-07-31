import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """        prefs = getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        isPinned.value = prefs.getBoolean("is_pinned", false)
        com.spotvault.app.SpotVaultColors.updateAmoled(prefs.getBoolean("amoled_black", false))"""

replacement = """        prefs = getSharedPreferences("SpotVaultPrefs", Context.MODE_PRIVATE)
        isPinned.value = prefs.getBoolean("is_pinned", false)
        ThemeState.currentTheme = prefs.getString("color_theme", "purple_teal") ?: "purple_teal"
        ThemeState.isAmoled = prefs.getBoolean("amoled_black", false)"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Patched onCreate")
else:
    print("onCreate target not found")
