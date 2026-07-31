import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target1 = """                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(22.dp))"""
replacement1 = """                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(22.dp))"""
content = content.replace(target1, replacement1)

target2 = """                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(105.dp)
                            .clip(RoundedCornerShape(22.dp))"""
replacement2 = """                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(22.dp))"""
content = content.replace(target2, replacement2)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
