import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Silence Alarm", color = MaterialTheme.colorScheme.onError)
        }
    }
}"""

replacement = """            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Silence Alarm", color = MaterialTheme.colorScheme.onError)
        }
    }
        }
}"""
content = content.replace(target, replacement)
with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
