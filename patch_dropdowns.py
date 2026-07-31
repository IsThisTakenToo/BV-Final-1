import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

# Add the controllers at the beginning of TimerSelectionDialog
target1 = """    var isActiveTracking by remember { mutableStateOf(defaultTracking) }

    val defaultCategories = listOf("Parking", "Entrance", "Viewpoint", "Food", "Trailhead", "Landmark", "Hotel", "Restroom", "Other")"""

replacement1 = """    var isActiveTracking by remember { mutableStateOf(defaultTracking) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val defaultCategories = listOf("Parking", "Entrance", "Viewpoint", "Food", "Trailhead", "Landmark", "Hotel", "Restroom", "Other")"""

# Modify the onExpandedChange for Category in TimerSelectionDialog
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

# Modify the onExpandedChange for Tag in TimerSelectionDialog
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

content = content.replace(target1, replacement1)
# Only replace the last occurrence of target2 and target3 because there are two pairs in the file (one in HistoryDialog, one in TimerSelectionDialog), and we want the TimerSelectionDialog ones which are further down. 
# Alternatively, I can just replace all of them. But I need `keyboardController` in scope for the other ones. 
