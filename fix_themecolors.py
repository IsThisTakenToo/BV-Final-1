import re

with open('app/src/main/java/com/spotvault/app/ThemeColors.kt', 'r') as f:
    content = f.read()

content = content.replace("val void", "val Void")
content = content.replace("val deep", "val Deep")
content = content.replace("val surface", "val Surface")
content = content.replace("val elevated", "val Elevated")
content = content.replace("val glass", "val Glass")
content = content.replace("val primary", "val Primary")
content = content.replace("val primaryBright", "val PrimaryBright")
content = content.replace("val primaryDeep", "val PrimaryDeep")
content = content.replace("val teal", "val Teal")
content = content.replace("val tealSoft", "val TealSoft")
content = content.replace("val tealDeep", "val TealDeep")
content = content.replace("val ink", "val Ink")
content = content.replace("val onSurface", "val OnSurface")
content = content.replace("val muted", "val Muted")
content = content.replace("val outline", "val Outline")
content = content.replace("val danger", "val Danger")

# Also need to fix the instantiation parameters:
content = content.replace("void =", "Void =")
content = content.replace("deep =", "Deep =")
content = content.replace("surface =", "Surface =")
content = content.replace("elevated =", "Elevated =")
content = content.replace("glass =", "Glass =")
content = content.replace("primary =", "Primary =")
content = content.replace("primaryBright =", "PrimaryBright =")
content = content.replace("primaryDeep =", "PrimaryDeep =")
content = content.replace("teal =", "Teal =")
content = content.replace("tealSoft =", "TealSoft =")
content = content.replace("tealDeep =", "TealDeep =")
content = content.replace("ink =", "Ink =")
content = content.replace("onSurface =", "OnSurface =")
content = content.replace("muted =", "Muted =")
content = content.replace("outline =", "Outline =")
content = content.replace("danger =", "Danger =")

with open('app/src/main/java/com/spotvault/app/ThemeColors.kt', 'w') as f:
    f.write(content)
print("Patched ThemeColors.kt")
