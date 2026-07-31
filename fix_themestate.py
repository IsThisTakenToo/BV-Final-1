import re

with open('app/src/main/java/com/spotvault/app/ThemeColors.kt', 'r') as f:
    content = f.read()

target = """object ThemeState {
    var currentTheme by androidx.compose.runtime.mutableStateOf("purple_teal")
    var isAmoled by androidx.compose.runtime.mutableStateOf(false)
}"""

replacement = """import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

object ThemeState {
    var currentTheme by androidx.compose.runtime.mutableStateOf("purple_teal")
    var isAmoled by androidx.compose.runtime.mutableStateOf(false)
}"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/spotvault/app/ThemeColors.kt', 'w') as f:
        f.write(content)
    print("Patched ThemeState")
else:
    print("ThemeState target not found")
