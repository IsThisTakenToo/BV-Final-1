import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

with open('exact_block.txt', 'r') as f:
    lines = f.readlines()

# The map box starts at:
#                     Spacer(modifier = Modifier.height(8.dp))
#                     Box(
# And ends at the closing brace of the main row `} // End row/column before 

target = "".join(lines[12:61])
print("Target to replace:\n" + target)

replacement = """                }
            }
        }"""

if target in content:
    print("Found exact target!")
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
        f.write(content)
else:
    print("Still didn't find exact target...")
