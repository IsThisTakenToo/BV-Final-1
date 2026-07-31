import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target1 = """                    if (photoPath.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))"""
replacement1 = """                    if (photoPath.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))"""
content = content.replace(target1, replacement1)

target2 = """                    }
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)"""
replacement2 = """                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(105.dp)"""
content = content.replace(target2, replacement2)

target3 = """                    }
                    
                    // Timer Section
                    Spacer(modifier = Modifier.height(18.dp))
                    TimerComponent(prefs = prefs, isAlarmRinging = isAlarmRinging, isPinned = true)"""
replacement3 = """                    }
                    
                    // Timer Section
                    Spacer(modifier = Modifier.height(12.dp))
                    TimerComponent(prefs = prefs, isAlarmRinging = isAlarmRinging, isPinned = true)"""
content = content.replace(target3, replacement3)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
