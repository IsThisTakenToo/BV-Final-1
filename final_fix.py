import re

# 1. Fix ThemeColors.kt
with open('app/src/main/java/com/spotvault/app/ThemeColors.kt', 'r') as f:
    tc_content = f.read()

tc_content = tc_content.replace("import androidx.compose.runtime.getValue\nimport androidx.compose.runtime.setValue\n", "")
tc_content = tc_content.replace("package com.spotvault.app\n", "package com.spotvault.app\n\nimport androidx.compose.runtime.getValue\nimport androidx.compose.runtime.setValue\n")

with open('app/src/main/java/com/spotvault/app/ThemeColors.kt', 'w') as f:
    f.write(tc_content)

# 2. Fix SpotVaultTheme in MainActivity.kt
with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    main_content = f.read()

def replace_spotvaultcolors_with_themecolors(content, start_str, end_str):
    start = content.find(start_str)
    end = content.find(end_str, start) + len(end_str)
    body = content[start:end]
    body = body.replace("SpotVaultColors.", "themeColors.")
    return content[:start] + body + content[end:]

main_content = replace_spotvaultcolors_with_themecolors(main_content, "val darkColors = darkColorScheme(", "        onError = Color.White\n    )")
main_content = replace_spotvaultcolors_with_themecolors(main_content, "val lightColors = lightColorScheme(", "        onError = Color.White\n    )")

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(main_content)

# 3. Clean up SpotVaultDesign.kt Conflicting declarations
with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    sd_content = f.read()

# I had multiple val themeColors injected!
# Replace multiple contiguous `val themeColors = LocalSpotVaultColors.current` with just one.
# Also remove `val themeColors = LocalthemeColors.current`
sd_content = sd_content.replace("    val themeColors = LocalthemeColors.current\n", "")
sd_content = sd_content.replace("    val themeColors = LocalSpotVaultColors.current\n    val themeColors = LocalSpotVaultColors.current\n", "    val themeColors = LocalSpotVaultColors.current\n")
sd_content = sd_content.replace("    val themeColors = LocalSpotVaultColors.current\n    val themeColors = LocalSpotVaultColors.current\n", "    val themeColors = LocalSpotVaultColors.current\n") # do it twice just in case
sd_content = sd_content.replace("    val themeColors = LocalSpotVaultColors.current\n    val themeColors = LocalSpotVaultColors.current\n", "    val themeColors = LocalSpotVaultColors.current\n")

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(sd_content)

print("Final Fix applied")
