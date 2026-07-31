with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                            onDismiss = {
                                showTimerDialog.value = false
                                processPhotoAndPin(0L, "", tempOcrTextForDialog, "Other")
                            },"""
replacement = """                            onDismiss = {
                                showTimerDialog.value = false
                                photoFile?.delete()
                                photoFile = null
                            },"""

if target in content:
    content = content.replace(target, replacement)
else:
    print("Failed to replace target 1")

target2 = """                            onRetake = {
                                showTimerDialog.value = false
                                checkPermissionsAndAction(isCamera = true)
                            },"""
replacement2 = """                            onRetake = {
                                showTimerDialog.value = false
                                photoFile?.delete()
                                photoFile = null
                                checkPermissionsAndAction(isCamera = true)
                            },"""

if target2 in content:
    content = content.replace(target2, replacement2)
else:
    print("Failed to replace target 2")

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
print("done")
