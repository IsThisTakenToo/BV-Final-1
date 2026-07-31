import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(22.dp),
            color = SpotVaultColors.Deep,
            contentColor = SpotVaultColors.TealSoft,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(SpotVaultColors.Teal.copy(alpha = 0.6f), SpotVaultColors.Primary.copy(alpha = 0.4f))
                    ),
                    RoundedCornerShape(22.dp)
                )
                .animateContentSize(androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            ))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(32.dp), tint = SpotVaultColors.Teal)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Time Remaining", style = MaterialTheme.typography.labelMedium, color = SpotVaultColors.Muted)
                    Text(remainingText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = SpotVaultColors.OnSurface)
                }
                IconButton(onClick = { showEditTimer = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Timer", tint = SpotVaultColors.PrimaryBright)
                }
                IconButton(onClick = { 
                    currentEndTime = 0L
                    val intent = android.content.Intent(context, TimerService::class.java).apply {
                        action = "CANCEL_TIMER"
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel Timer", tint = SpotVaultColors.Danger)
                }
            }
        }"""

replacement = """        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(8.dp),
            color = SpotVaultColors.PrimaryDeep.copy(alpha = 0.3f),
            contentColor = SpotVaultColors.TealSoft,
            modifier = Modifier
                .border(
                    1.dp,
                    SpotVaultColors.PrimaryBright.copy(alpha = 0.6f),
                    RoundedCornerShape(8.dp)
                )
                .animateContentSize(androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            ))
        ) {
            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = SpotVaultColors.PrimaryBright)
                Spacer(modifier = Modifier.width(6.dp))
                Text(remainingText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SpotVaultColors.OnSurface)
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.foundation.clickable(onClick = { showEditTimer = true }) {
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
                }
            }
        }"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
