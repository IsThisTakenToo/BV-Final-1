import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("photoFile = null\n                dialogSessionKey", "photoFile = null\n                tempOcrTextForDialog = \"\"\n                tempProminentOcrTextForDialog = \"\"\n                dialogSessionKey")
content = content.replace("private fun launchCamera() {\n        lifecycleScope.launch {", "private fun launchCamera() {\n        tempOcrTextForDialog = \"\"\n        tempProminentOcrTextForDialog = \"\"\n        lifecycleScope.launch {")
content = content.replace("if (currentPhotoPath.isEmpty()) {\n            showTimerDialog.value = true", "if (currentPhotoPath.isEmpty()) {\n            tempOcrTextForDialog = \"\"\n            tempProminentOcrTextForDialog = \"\"\n            showTimerDialog.value = true")

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
