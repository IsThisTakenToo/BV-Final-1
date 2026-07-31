import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target1 = """    var isActiveTracking by remember { mutableStateOf(defaultTracking) }

    val defaultCategories = listOf("Parking", "Entrance", "Viewpoint", "Food", "Trailhead", "Landmark", "Hotel", "Restroom", "Other")"""

replacement1 = """    var isActiveTracking by remember { mutableStateOf(defaultTracking) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val defaultCategories = listOf("Parking", "Entrance", "Viewpoint", "Food", "Trailhead", "Landmark", "Hotel", "Restroom", "Other")"""

content = content.replace(target1, replacement1)

# we only want to modify the second match of target2 and target3
# Actually, TimerSelectionDialog starts around line 2056.

target2 = """                var categoryExpanded by remember { mutableStateOf(false) }
                
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {"""

replacement2 = """                var categoryExpanded by remember { mutableStateOf(false) }
                
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { 
                        categoryExpanded = !categoryExpanded
                        if (categoryExpanded) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {"""

parts2 = content.split(target2)
if len(parts2) == 3:
    content = parts2[0] + target2 + parts2[1] + replacement2 + parts2[2]
else:
    print("target2 count:", len(parts2) - 1)

target3 = """                var tagExpanded by remember { mutableStateOf(false) }
                
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = tagExpanded,
                    onExpandedChange = { tagExpanded = !tagExpanded },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {"""

replacement3 = """                var tagExpanded by remember { mutableStateOf(false) }
                
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = tagExpanded,
                    onExpandedChange = { 
                        tagExpanded = !tagExpanded
                        if (tagExpanded) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {"""

parts3 = content.split(target3)
if len(parts3) == 3:
    content = parts3[0] + target3 + parts3[1] + replacement3 + parts3[2]
elif len(parts3) == 2:
    # Just one? Oh wait, in HistoryDialog it has an @OptIn annotation in front of it! Let's check!
    print("target3 count is 1, let's just replace it")
    content = content.replace(target3, replacement3)
else:
    print("target3 count:", len(parts3) - 1)


with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)

print("Patched dropdowns")
