import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target1 = """                    // Timer Section
                    Spacer(modifier = Modifier.height(12.dp))
                    TimerComponent(prefs = prefs, isAlarmRinging = isAlarmRinging, isPinned = true)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Bottom Actions row (Found It, Navigate) and Share below
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Row("""

replacement1 = """                }
            }
        }

        // Bottom Actions row (Found It, Navigate) and Share below
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            TimerComponent(
                prefs = prefs, 
                isAlarmRinging = isAlarmRinging, 
                isPinned = true,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row("""

content = content.replace(target1, replacement1)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
