import re

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    content = f.read()

target = """                Text(
                    text = "PIN · SECURE · RECALL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.6.sp,
                    color = SpotVaultColors.Teal.copy(alpha = 0.85f)
                )"""

replacement = """                Text(
                    text = "SAVE · SHARE · NAVIGATE · RECALL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.0.sp,
                    color = SpotVaultColors.Teal.copy(alpha = 0.85f)
                )"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(content)

