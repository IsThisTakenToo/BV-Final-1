with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    content = f.read()

content = content.replace("androidx.compose.ui.graphics.luminance(LocalSpotVaultColors.current.Primary)", "LocalSpotVaultColors.current.Primary.luminance()")

if "import androidx.compose.ui.graphics.luminance" not in content:
    content = content.replace("import androidx.compose.ui.graphics.Color", "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.luminance")

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content2 = f.read()

content2 = content2.replace("androidx.compose.ui.graphics.luminance(primary)", "primary.luminance()")

if "import androidx.compose.ui.graphics.luminance" not in content2:
    content2 = content2.replace("import androidx.compose.ui.graphics.Color", "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.luminance")

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content2)

print("done")
