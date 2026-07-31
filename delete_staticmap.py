import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = r"@Composable\nfun StaticMapPreview\(lat: Float, lng: Float, modifier: Modifier = Modifier\) \{[\s\S]*?\}\n\nfun shareLocation"
replacement = "fun shareLocation"

if re.search(target, content):
    content = re.sub(target, replacement, content)
    with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Deleted StaticMapPreview.")
else:
    print("Could not find StaticMapPreview using regex.")
