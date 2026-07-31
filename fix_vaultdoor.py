import re

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    sd_content = f.read()

def patch_composable(name, content):
    start = content.find("fun " + name)
    if start == -1: return content
    
    end = content.find("\nfun ", start + 20)
    if end == -1: end = len(content)
    
    body = content[start:end]
    body = body.replace("SpotVaultColors.", "themeColors.")
    
    if "val themeColors = LocalSpotVaultColors.current" not in body:
        brace_idx = body.find("{")
        if brace_idx != -1:
            body = body[:brace_idx+1] + "\n    val themeColors = LocalSpotVaultColors.current" + body[brace_idx+1:]
            
    return content[:start] + body + content[end:]

sd_content = patch_composable('VaultDoorButton', sd_content)

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(sd_content)

print("Fixed VaultDoorButton")
