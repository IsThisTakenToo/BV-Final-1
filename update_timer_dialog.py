import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

# Update TimerSelectionDialog Categories and Tags lists
target_lists = """    val defaultCategories = listOf("Parking Garage", "Street Parking", "Amusement Park", "Hotel")
    var savedCustomCategories by remember { mutableStateOf(prefs.getStringSet("custom_categories", setOf())?.toList() ?: emptyList()) }
    val allCategories = (defaultCategories + savedCustomCategories).distinct()

    val defaultTags = listOf("Level 1", "Level 2", "Blue Lot", "Red Lot", "Row 5", "Near Entrance")
    var savedCustomTags by remember { mutableStateOf(prefs.getStringSet("custom_tags", setOf())?.toList() ?: emptyList()) }
    val allTags = (defaultTags + savedCustomTags).distinct()"""

replacement_lists = """    val defaultCategories = listOf("Parking", "Entrance", "Viewpoint", "Food", "Trailhead", "Landmark", "Hotel", "Restroom", "Other")
    var savedCustomCategories by remember { mutableStateOf(prefs.getStringSet("custom_categories", setOf())?.toList() ?: emptyList()) }
    val allCategories = (defaultCategories + savedCustomCategories).distinct()

    val defaultTags = listOf("Level 1", "Level 2", "Must See", "Near Entrance", "Scenic", "Indoor", "Outdoor")
    var savedCustomTags by remember { mutableStateOf(prefs.getStringSet("custom_tags", setOf())?.toList() ?: emptyList()) }
    val allTags = (defaultTags + savedCustomTags).distinct()"""

content = content.replace(target_lists, replacement_lists)


# Update Tag Dropdown
target_tag_dropdown = """                // Editable Tag Dropdown
                var tagInput by remember { mutableStateOf("") }
                var tagExpanded by remember { mutableStateOf(false) }
                
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = tagExpanded,
                    onExpandedChange = { tagExpanded = !tagExpanded },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { 
                             tagInput = it 
                             tagExpanded = true
                        },
                        label = { Text("Add Tag (e.g. Level 1)") },
                        trailingIcon = {
                            if (tagInput.isNotEmpty()) {
                                IconButton(onClick = {
                                    saveCustomTag(tagInput)
                                    val newText = if (isCamera) {
                                        if (editedOcrText.text.isEmpty()) tagInput else "${editedOcrText.text}, $tagInput"
                                    } else {
                                        if (note.text.isEmpty()) tagInput else "${note.text}, $tagInput"
                                    }
                                    if (isCamera) editedOcrText = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                    else note = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                    tagInput = ""
                                    tagExpanded = false
                                }) { Icon(Icons.Default.Add, contentDescription = "Add Tag", tint = SpotVaultColors.Teal) }
                            } else {
                                androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded)
                            }
                        },
                        colors = androidx.compose.material3.ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    
                    val filteredTags = allTags.filter { it.contains(tagInput, ignoreCase = true) }
                    if (filteredTags.isNotEmpty() && tagExpanded) {
                        ExposedDropdownMenu(
                            expanded = tagExpanded,
                            onDismissRequest = { tagExpanded = false }
                        ) {
                            filteredTags.forEach { tag ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(tag) },
                                    onClick = {
                                        val newText = if (isCamera) {
                                            if (editedOcrText.text.isEmpty()) tag else "${editedOcrText.text}, $tag"
                                        } else {
                                            if (note.text.isEmpty()) tag else "${note.text}, $tag"
                                        }
                                        if (isCamera) editedOcrText = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                        else note = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                        tagInput = ""
                                        tagExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }"""

replacement_tag_dropdown = """                // Editable Tag Dropdown
                var tagInput by remember { mutableStateOf("") }
                var tagExpanded by remember { mutableStateOf(false) }
                
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = tagExpanded,
                    onExpandedChange = { tagExpanded = !tagExpanded },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { 
                             tagInput = it 
                             tagExpanded = true
                        },
                        label = { Text("Add Tag (e.g. Level 1)") },
                        trailingIcon = {
                            if (tagInput.isNotEmpty()) {
                                IconButton(onClick = {
                                    saveCustomTag(tagInput)
                                    val newText = if (isCamera) {
                                        if (editedOcrText.text.isEmpty()) tagInput else "${editedOcrText.text}, $tagInput"
                                    } else {
                                        if (note.text.isEmpty()) tagInput else "${note.text}, $tagInput"
                                    }
                                    if (isCamera) editedOcrText = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                    else note = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                    tagInput = ""
                                    tagExpanded = false
                                }) { Icon(Icons.Default.Add, contentDescription = "Add Tag", tint = SpotVaultColors.Teal) }
                            } else {
                                androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded)
                            }
                        },
                        colors = androidx.compose.material3.ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    
                    val filteredTags = allTags.filter { it.contains(tagInput, ignoreCase = true) }
                    if (tagExpanded) {
                        ExposedDropdownMenu(
                            expanded = tagExpanded,
                            onDismissRequest = { tagExpanded = false }
                        ) {
                            if (tagInput.isNotEmpty() && !allTags.any { it.equals(tagInput, ignoreCase = true) }) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("+ Add new tag: \"$tagInput\"", color = SpotVaultColors.PrimaryBright, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        saveCustomTag(tagInput)
                                        val newText = if (isCamera) {
                                            if (editedOcrText.text.isEmpty()) tagInput else "${editedOcrText.text}, $tagInput"
                                        } else {
                                            if (note.text.isEmpty()) tagInput else "${note.text}, $tagInput"
                                        }
                                        if (isCamera) editedOcrText = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                        else note = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                        tagInput = ""
                                        tagExpanded = false
                                    }
                                )
                            }
                            filteredTags.forEach { tag ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(tag) },
                                    onClick = {
                                        val newText = if (isCamera) {
                                            if (editedOcrText.text.isEmpty()) tag else "${editedOcrText.text}, $tag"
                                        } else {
                                            if (note.text.isEmpty()) tag else "${note.text}, $tag"
                                        }
                                        if (isCamera) editedOcrText = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                        else note = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                        tagInput = ""
                                        tagExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }"""
content = content.replace(target_tag_dropdown, replacement_tag_dropdown)

# Update Category Dropdown
target_cat_dropdown = """                // Editable Category Dropdown
                var categoryExpanded by remember { mutableStateOf(false) }
                
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { 
                             category = it 
                             categoryExpanded = true
                        },
                        placeholder = { Text("Choose or create a category") },
                        label = { Text("Folder / Category", fontWeight = FontWeight.Bold) },
                        trailingIcon = {
                            androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        colors = androidx.compose.material3.ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    
                    val filteredCats = allCategories.filter { it.contains(category, ignoreCase = true) }
                    if (filteredCats.isNotEmpty() && categoryExpanded) {
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            filteredCats.forEach { cat ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }"""

replacement_cat_dropdown = """                // Editable Category Dropdown
                var categoryExpanded by remember { mutableStateOf(false) }
                
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { 
                             category = it 
                             categoryExpanded = true
                        },
                        placeholder = { Text("Choose or create a category") },
                        label = { Text("Folder / Category", fontWeight = FontWeight.Bold) },
                        trailingIcon = {
                            if (category.isNotEmpty() && !allCategories.any { it.equals(category, ignoreCase = true) }) {
                                IconButton(onClick = {
                                    saveCustomCategory(category)
                                    categoryExpanded = false
                                }) { Icon(Icons.Default.Add, contentDescription = "Add Category", tint = SpotVaultColors.Teal) }
                            } else {
                                androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                            }
                        },
                        colors = androidx.compose.material3.ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    
                    val filteredCats = allCategories.filter { it.contains(category, ignoreCase = true) }
                    if (categoryExpanded) {
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            if (category.isNotEmpty() && !allCategories.any { it.equals(category, ignoreCase = true) }) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("+ Add new category: \"$category\"", color = SpotVaultColors.PrimaryBright, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        saveCustomCategory(category)
                                        categoryExpanded = false
                                    }
                                )
                            }
                            filteredCats.forEach { cat ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }"""
content = content.replace(target_cat_dropdown, replacement_cat_dropdown)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)

