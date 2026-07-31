import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target1 = """                    if (locationDetails.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))"""
replacement1 = """                    if (locationDetails.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))"""
content = content.replace(target1, replacement1)

target2 = """                    if (photoPath.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))"""
replacement2 = """                    if (photoPath.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))"""
content = content.replace(target2, replacement2)

target3 = """                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(105.dp)"""
replacement3 = """                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(105.dp)"""
content = content.replace(target3, replacement3)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
