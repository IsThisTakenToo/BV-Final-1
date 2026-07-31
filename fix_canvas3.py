import re

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    content = f.read()

# We know the offending functions are:
# 1. SpotVaultRadarScan
# 2. PremiumBadge
# 3. PremiumEmptyState

# For each, we'll find the function body, replace SpotVaultColors. with themeColors.,
# and add `val themeColors = LocalSpotVaultColors.current` if not already there.

def patch_func(func_name, content):
    pattern = r'(fun ' + func_name + r'\s*\([^)]*\)\s*\{)'
    match = re.search(pattern, content)
    if not match:
        return content
    
    start = match.end()
    # Find the end of the function by counting braces, or just doing a dumb replace up to next 'fun '
    next_func = content.find('\nfun ', start)
    if next_func == -1: next_func = len(content)
    
    func_body = content[start:next_func]
    
    # If not already has themeColors, inject it
    if 'val themeColors =' not in func_body:
        content = content[:start] + "\n    val themeColors = LocalSpotVaultColors.current" + content[start:]
        # update positions
        next_func += len("\n    val themeColors = LocalSpotVaultColors.current")
        func_body = content[start:next_func]
        
    func_body = func_body.replace('SpotVaultColors.', 'themeColors.')
    return content[:start] + func_body + content[next_func:]

content = patch_func('SpotVaultRadarScan', content)
content = patch_func('PremiumBadge', content)
content = patch_func('PremiumEmptyState', content)
content = patch_func('SpotVaultAmbientBackground', content)

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(content)
print("Patched SpotVaultDesign.kt with fix_canvas3.py")
