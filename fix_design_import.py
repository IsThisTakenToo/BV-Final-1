import re

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    content = f.read()

if "import androidx.compose.material.icons.filled.Settings" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Notifications", "import androidx.compose.material.icons.filled.Notifications\nimport androidx.compose.material.icons.filled.Settings")

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(content)

print("Fixed design imports")
