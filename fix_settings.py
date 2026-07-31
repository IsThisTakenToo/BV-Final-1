import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("androidx.compose.foundation.clickable { onPickRingtone() }", "clickable { onPickRingtone() }")
content = content.replace("Icons.Default.ArrowForward", "androidx.compose.material.icons.filled.ArrowForward")
content = content.replace("val spots = dao.getAllHistory()", "val spots = dao.getHistoryList()")
content = content.replace("dao.deleteAll()", "dao.deleteAllHistory()")

if "import androidx.compose.foundation.clickable" not in content:
    content = content.replace("import androidx.compose.foundation.layout.*", "import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.clickable")

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    design_content = f.read()

design_content = design_content.replace("Icons.Default.Settings", "androidx.compose.material.icons.filled.Settings")

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(design_content)

print("Fixed settings bugs")
