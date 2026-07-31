import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("androidx.compose.material.icons.filled.ArrowForward", "Icons.Default.ArrowForward")
if "import androidx.compose.material.icons.filled.ArrowForward" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Delete", "import androidx.compose.material.icons.filled.Delete\nimport androidx.compose.material.icons.filled.ArrowForward")

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    design_content = f.read()

design_content = design_content.replace("androidx.compose.material.icons.filled.Settings", "Icons.Default.Settings")
if "import androidx.compose.material.icons.filled.Settings" not in design_content:
    design_content = design_content.replace("import androidx.compose.material.icons.filled.Favorite", "import androidx.compose.material.icons.filled.Favorite\nimport androidx.compose.material.icons.filled.Settings")

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(design_content)

print("Fixed icons")
