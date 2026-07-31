import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target1 = """                        TimerSelectionDialog(
                            prefs = prefs,
                            isCamera = (photoFile != null),
                            ocrText = tempOcrTextForDialog,
                            onDismiss = {"""

replacement1 = """                        TimerSelectionDialog(
                            prefs = prefs,
                            isCamera = (photoFile != null),
                            ocrText = tempOcrTextForDialog,
                            prominentOcrText = tempProminentOcrTextForDialog,
                            onDismiss = {"""

target2 = """fun TimerSelectionDialog(
    prefs: android.content.SharedPreferences,
    isCamera: Boolean, 
    ocrText: String, 
    onDismiss: () -> Unit, 
    onRetake: () -> Unit, 
    onPin: (Int, String, String, String, Boolean) -> Unit
) {
    var timerMins by remember { mutableStateOf("") }
    var note by remember { mutableStateOf(TextFieldValue("")) }
    var editedOcrText by remember { mutableStateOf(TextFieldValue(text = ocrText, selection = TextRange(ocrText.length))) }
    var category by remember { mutableStateOf("") }"""

replacement2 = """fun TimerSelectionDialog(
    prefs: android.content.SharedPreferences,
    isCamera: Boolean, 
    ocrText: String, 
    prominentOcrText: String = "",
    onDismiss: () -> Unit, 
    onRetake: () -> Unit, 
    onPin: (Int, String, String, String, Boolean) -> Unit
) {
    var timerMins by remember { mutableStateOf("") }
    var note by remember { mutableStateOf(TextFieldValue("")) }
    var editedOcrText by remember { mutableStateOf(TextFieldValue(text = ocrText, selection = TextRange(ocrText.length))) }
    var category by remember { mutableStateOf(prominentOcrText) }"""

if target1 in content:
    content = content.replace(target1, replacement1)
    print("Replaced caller")
else:
    print("Caller target not found")

if target2 in content:
    content = content.replace(target2, replacement2)
    print("Replaced declaration")
else:
    print("Declaration target not found")

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
