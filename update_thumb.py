import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

old_thumb_box = """                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SpotVaultColors.Elevated)
                                    .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                    .clickable { viewSpot = item },
                                contentAlignment = Alignment.Center
                            ) {"""

new_thumb_box = """                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SpotVaultColors.Glass)
                                    .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .clickable { viewSpot = item },
                                contentAlignment = Alignment.Center
                            ) {"""

content = content.replace(old_thumb_box, new_thumb_box)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
