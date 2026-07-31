import re

# 1. Fix ThemeColors.kt imports
with open('app/src/main/java/com/spotvault/app/ThemeColors.kt', 'r') as f:
    tc_content = f.read()

tc_content = tc_content.replace("import androidx.compose.runtime.getValue\nimport androidx.compose.runtime.setValue\n", "")
tc_content = "import androidx.compose.runtime.getValue\nimport androidx.compose.runtime.setValue\n" + tc_content

with open('app/src/main/java/com/spotvault/app/ThemeColors.kt', 'w') as f:
    f.write(tc_content)


# 2. Fix SpotVaultDesign.kt Canvas issues
with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    sd_content = f.read()

# Fix LocalthemeColors mistake
sd_content = sd_content.replace("LocalthemeColors", "LocalSpotVaultColors")

# Function to patch specific composables
def patch_composable(name, content):
    start = content.find("fun " + name)
    if start == -1: return content
    
    end = content.find("\nfun ", start + 20)
    if end == -1: end = len(content)
    
    body = content[start:end]
    # Replace SpotVaultColors with themeColors in the body
    body = body.replace("SpotVaultColors.", "themeColors.")
    
    # Inject val themeColors = LocalSpotVaultColors.current if missing
    if "val themeColors = LocalSpotVaultColors.current" not in body:
        # Find the first {
        brace_idx = body.find("{")
        if brace_idx != -1:
            body = body[:brace_idx+1] + "\n    val themeColors = LocalSpotVaultColors.current" + body[brace_idx+1:]
            
    return content[:start] + body + content[end:]

sd_content = patch_composable('SpotVaultAmbientBackground', sd_content)
sd_content = patch_composable('SpotVaultRadarScan', sd_content)
sd_content = patch_composable('PremiumEmptyState', sd_content)
sd_content = patch_composable('PremiumBadge', sd_content)

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(sd_content)

print("Fixed everything")
