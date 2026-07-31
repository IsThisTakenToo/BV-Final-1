import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("void = androidx.compose.ui.graphics.Color.Black", "Void = androidx.compose.ui.graphics.Color.Black")
content = content.replace("deep = androidx.compose.ui.graphics.Color.Black", "Deep = androidx.compose.ui.graphics.Color.Black")
content = content.replace("surface = androidx.compose.ui.graphics.Color(0xFF080808)", "Surface = androidx.compose.ui.graphics.Color(0xFF080808)")

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
print("Patched MainActivity Amoled copy")
