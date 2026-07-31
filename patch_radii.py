import re

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    content = f.read()

content = content.replace("val radius1 = w * 0.75f", "val radius1 = w * 0.55f")
content = content.replace("val radius2 = w * 0.65f", "val radius2 = w * 0.45f")
content = content.replace("val radius3 = w * 0.55f", "val radius3 = w * 0.35f")

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(content)

