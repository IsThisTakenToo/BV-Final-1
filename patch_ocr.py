import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

# Add tempProminentOcrTextForDialog
content = content.replace("private var tempOcrTextForDialog: String = \"\"", "private var tempOcrTextForDialog: String = \"\"\n    private var tempProminentOcrTextForDialog: String = \"\"")

# Replace resets
content = content.replace('tempOcrTextForDialog = ""', 'tempOcrTextForDialog = ""\n                tempProminentOcrTextForDialog = ""')
# Wait, some places might not be indented by 16 spaces. Let's just do:
content = content.replace('tempOcrTextForDialog = ""', 'tempOcrTextForDialog = ""; tempProminentOcrTextForDialog = ""')

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
