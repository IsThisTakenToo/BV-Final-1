import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """        // Bottom Actions row (Found It, Navigate) and Share below
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            TimerComponent(
                prefs = prefs, 
                isAlarmRinging = isAlarmRinging, 
                isPinned = true,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),"""

replacement = """        // Bottom Actions row (Found It, Navigate) and Share below
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
