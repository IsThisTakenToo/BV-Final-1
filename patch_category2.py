import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                        onValueChange = { 
                             category = it 
                        },
                        readOnly = true,"""

replacement = """                        onValueChange = { 
                             category = it 
                        },
                        readOnly = false,"""

if target in content:
    content = content.replace(target, replacement)
    print("Replaced category readOnly")
else:
    print("category target not found")

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
