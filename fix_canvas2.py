import re

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    content = f.read()

def replacer(match):
    return match.group(0).replace("SpotVaultColors.", "themeColors.")

# SpotVaultRadarScan
start = content.find("fun SpotVaultRadarScan")
end = content.find("}", start) + 1000 # Just an approximation for the function body
end = content.find("fun ", start + 20)
if end == -1: end = len(content)

func_content = content[start:end]
new_func_content = func_content.replace("SpotVaultColors.", "themeColors.")
new_func_content = new_func_content.replace("fun SpotVaultRadarScan(", "fun SpotVaultRadarScan(") # just in case
# Wait, I need to inject `val themeColors = LocalSpotVaultColors.current` inside the composable!
new_func_content = new_func_content.replace("fun SpotVaultRadarScan(\n    modifier: Modifier = Modifier\n) {", "fun SpotVaultRadarScan(\n    modifier: Modifier = Modifier\n) {\n    val themeColors = LocalSpotVaultColors.current")
content = content[:start] + new_func_content + content[end:]


# PremiumBadge (I see SpotVaultColors.PrimaryDeep, etc. around line 528 still exist)
start = content.find("fun PremiumBadge")
end = content.find("fun ", start + 20)
if end == -1: end = len(content)

func_content = content[start:end]
new_func_content = func_content.replace("SpotVaultColors.", "themeColors.")
# PremiumBadge already has `val themeColors = LocalSpotVaultColors.current` because my target6 worked.
content = content[:start] + new_func_content + content[end:]

# PremiumEmptyState (line 494 still has SpotVaultColors.PrimaryBright)
start = content.find("fun PremiumEmptyState")
end = content.find("fun ", start + 20)
if end == -1: end = len(content)

func_content = content[start:end]
new_func_content = func_content.replace("SpotVaultColors.", "themeColors.")
new_func_content = new_func_content.replace("fun PremiumEmptyState(", "fun PremiumEmptyState(") 
new_func_content = new_func_content.replace("fun PremiumEmptyState(\n    modifier: Modifier = Modifier,\n    onPremiumClick: () -> Unit\n) {", "fun PremiumEmptyState(\n    modifier: Modifier = Modifier,\n    onPremiumClick: () -> Unit\n) {\n    val themeColors = LocalSpotVaultColors.current")
content = content[:start] + new_func_content + content[end:]

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(content)
print("Second patch complete")
