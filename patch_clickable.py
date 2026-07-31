import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                androidx.compose.foundation.clickable(onClick = { showEditTimer = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Timer", tint = SpotVaultColors.PrimaryBright, modifier = Modifier.size(16.dp).padding(2.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.foundation.clickable(onClick = { 
                    currentEndTime = 0L
                    val intent = android.content.Intent(context, TimerService::class.java).apply {
                        action = "CANCEL_TIMER"
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel Timer", tint = SpotVaultColors.Danger, modifier = Modifier.size(16.dp).padding(2.dp))
                }"""

replacement = """                Icon(Icons.Default.Edit, contentDescription = "Edit Timer", tint = SpotVaultColors.PrimaryBright, modifier = Modifier.size(16.dp).padding(2.dp).clickable { showEditTimer = true })
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Close, contentDescription = "Cancel Timer", tint = SpotVaultColors.Danger, modifier = Modifier.size(16.dp).padding(2.dp).clickable { 
                    currentEndTime = 0L
                    val intent = android.content.Intent(context, TimerService::class.java).apply {
                        action = "CANCEL_TIMER"
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                })"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
