import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """@Composable
fun TimerComponent(prefs: android.content.SharedPreferences, isAlarmRinging: Boolean, isPinned: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentEndTime by remember(isPinned) { mutableStateOf(prefs.getLong("timer_end_time", 0L)) }
    
    if (currentEndTime > System.currentTimeMillis()) {"""

replacement = """@Composable
fun TimerComponent(
    prefs: android.content.SharedPreferences, 
    isAlarmRinging: Boolean, 
    isPinned: Boolean,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentEndTime by remember(isPinned) { mutableStateOf(prefs.getLong("timer_end_time", 0L)) }
    
    if (currentEndTime > System.currentTimeMillis() || isAlarmRinging) {
        Column(modifier = modifier) {
            if (currentEndTime > System.currentTimeMillis()) {"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
