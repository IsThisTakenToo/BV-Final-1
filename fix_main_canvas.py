import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("Canvas(modifier = Modifier.fillMaxSize()) {", "androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {")

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
print("Patched MainActivity.kt Canvas")
