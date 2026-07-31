import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                    if (locationDetails.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))"""
replacement = """                    if (locationDetails.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))"""
content = content.replace(target, replacement)

target4 = """            Spacer(modifier = Modifier.height(12.dp))
        }

        // Bottom Actions row (Found It, Navigate) and Share below
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {"""
replacement4 = """            Spacer(modifier = Modifier.height(8.dp))
        }

        // Bottom Actions row (Found It, Navigate) and Share below
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {"""
content = content.replace(target4, replacement4)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
