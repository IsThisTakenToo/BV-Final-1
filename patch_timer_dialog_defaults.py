import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """    var timerMins by remember { mutableStateOf("") }
    var note by remember { mutableStateOf(TextFieldValue("")) }
    var editedOcrText by remember { mutableStateOf(TextFieldValue(text = ocrText, selection = TextRange(ocrText.length))) }
    var category by remember { mutableStateOf(prominentOcrText) }
    var useNumericKeyboard by remember { mutableStateOf(false) }
    var isActiveTracking by remember { mutableStateOf(true) }"""

replacement = """    val defaultMins = prefs.getString("default_timer_mins", "") ?: ""
    var timerMins by remember { mutableStateOf(defaultMins) }
    var note by remember { mutableStateOf(TextFieldValue("")) }
    var editedOcrText by remember { mutableStateOf(TextFieldValue(text = ocrText, selection = TextRange(ocrText.length))) }
    var category by remember { mutableStateOf(prominentOcrText) }
    var useNumericKeyboard by remember { mutableStateOf(false) }
    val defaultTracking = prefs.getBoolean("default_active_tracking", true)
    var isActiveTracking by remember { mutableStateOf(defaultTracking) }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Patched TimerSelectionDialog defaults")
else:
    print("Timer defaults target not found")
