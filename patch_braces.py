import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """        ) {
            Text("Silence Alarm", color = MaterialTheme.colorScheme.onError)
        }
    }
        }
}
@Composable"""

replacement = """        ) {
            Text("Silence Alarm", color = MaterialTheme.colorScheme.onError)
        }
    }
        }
    }
}
@Composable"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
