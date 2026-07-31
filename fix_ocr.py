import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace('tempOcrTextForDialog = ""', 'tempOcrTextForDialog = ""; tempProminentOcrTextForDialog = ""')

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
