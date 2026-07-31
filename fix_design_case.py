import re

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    content = f.read()

content = content.replace("themeColors.void", "themeColors.Void")
content = content.replace("themeColors.deep", "themeColors.Deep")
content = content.replace("themeColors.surface", "themeColors.Surface")
content = content.replace("themeColors.elevated", "themeColors.Elevated")
content = content.replace("themeColors.glass", "themeColors.Glass")
content = content.replace("themeColors.primary", "themeColors.Primary")
content = content.replace("themeColors.primaryBright", "themeColors.PrimaryBright")
content = content.replace("themeColors.primaryDeep", "themeColors.PrimaryDeep")
content = content.replace("themeColors.teal", "themeColors.Teal")
content = content.replace("themeColors.tealSoft", "themeColors.TealSoft")
content = content.replace("themeColors.tealDeep", "themeColors.TealDeep")
content = content.replace("themeColors.ink", "themeColors.Ink")
content = content.replace("themeColors.onSurface", "themeColors.OnSurface")
content = content.replace("themeColors.muted", "themeColors.Muted")
content = content.replace("themeColors.outline", "themeColors.Outline")
content = content.replace("themeColors.danger", "themeColors.Danger")

# Check SpotVaultColors @Composable get()
# They should still use the uppercase properties from LocalSpotVaultColors.current
content = content.replace("LocalSpotVaultColors.current.void", "LocalSpotVaultColors.current.Void")
content = content.replace("LocalSpotVaultColors.current.deep", "LocalSpotVaultColors.current.Deep")
content = content.replace("LocalSpotVaultColors.current.surface", "LocalSpotVaultColors.current.Surface")
content = content.replace("LocalSpotVaultColors.current.elevated", "LocalSpotVaultColors.current.Elevated")
content = content.replace("LocalSpotVaultColors.current.glass", "LocalSpotVaultColors.current.Glass")
content = content.replace("LocalSpotVaultColors.current.primary", "LocalSpotVaultColors.current.Primary")
content = content.replace("LocalSpotVaultColors.current.primaryBright", "LocalSpotVaultColors.current.PrimaryBright")
content = content.replace("LocalSpotVaultColors.current.primaryDeep", "LocalSpotVaultColors.current.PrimaryDeep")
content = content.replace("LocalSpotVaultColors.current.teal", "LocalSpotVaultColors.current.Teal")
content = content.replace("LocalSpotVaultColors.current.tealSoft", "LocalSpotVaultColors.current.TealSoft")
content = content.replace("LocalSpotVaultColors.current.tealDeep", "LocalSpotVaultColors.current.TealDeep")
content = content.replace("LocalSpotVaultColors.current.ink", "LocalSpotVaultColors.current.Ink")
content = content.replace("LocalSpotVaultColors.current.onSurface", "LocalSpotVaultColors.current.OnSurface")
content = content.replace("LocalSpotVaultColors.current.muted", "LocalSpotVaultColors.current.Muted")
content = content.replace("LocalSpotVaultColors.current.outline", "LocalSpotVaultColors.current.Outline")
content = content.replace("LocalSpotVaultColors.current.danger", "LocalSpotVaultColors.current.Danger")

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(content)
print("Patched SpotVaultDesign.kt case")
