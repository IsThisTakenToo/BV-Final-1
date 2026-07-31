import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                // ExposedDropdownMenuBox for Categories
                var categoryExpanded by remember { mutableStateOf(false) }
                
                @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
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
                                    val updated = (prefs.getStringSet("custom_categories", emptySet()) ?: emptySet()) + category
                                    prefs.edit().putStringSet("custom_categories", updated).apply()
                                    customCategories = updated.toList()
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
                                    text = { Text("+ Add new category: \\"$category\\"", color = SpotVaultColors.PrimaryBright, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        val updated = (prefs.getStringSet("custom_categories", emptySet()) ?: emptySet()) + category
                                        prefs.edit().putStringSet("custom_categories", updated).apply()
                                        customCategories = updated.toList()
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
                }
                
                // Editable Tag Dropdown
                var tagInput by remember { mutableStateOf("") }
                var tagExpanded by remember { mutableStateOf(false) }
                
                @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
                        label = { Text("Add Tag (e.g. Must See)") },
                        trailingIcon = {
                            if (tagInput.isNotEmpty()) {
                                IconButton(onClick = {
                                    if (tagInput.isNotBlank() && !allTags.any { it.equals(tagInput, ignoreCase = true) }) {
                                        val updated = (prefs.getStringSet("custom_tags", emptySet()) ?: emptySet()) + tagInput
                                        prefs.edit().putStringSet("custom_tags", updated).apply()
                                        customTags = updated.toList()
                                    }
                                    note = if (note.isEmpty()) tagInput else "${note}, $tagInput"
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
                                    text = { Text("+ Add new tag: \\"$tagInput\\"", color = SpotVaultColors.PrimaryBright, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        val updated = (prefs.getStringSet("custom_tags", emptySet()) ?: emptySet()) + tagInput
                                        prefs.edit().putStringSet("custom_tags", updated).apply()
                                        customTags = updated.toList()
                                        note = if (note.isEmpty()) tagInput else "${note}, $tagInput"
                                        tagInput = ""
                                        tagExpanded = false
                                    }
                                )
                            }
                            filteredTags.forEach { tag ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(tag) },
                                    onClick = {
                                        note = if (note.isEmpty()) tag else "${note}, $tag"
                                        tagInput = ""
                                        tagExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }"""

replacement = """                ChipSelectionRow(
                    title = "Category / Folder",
                    items = allCategories,
                    selectedItem = category,
                    onItemSelected = { category = it },
                    onCustomAdded = { 
                        if (it.isNotBlank() && !allCategories.contains(it)) {
                            val updated = (prefs.getStringSet("custom_categories", emptySet()) ?: emptySet()) + it
                            prefs.edit().putStringSet("custom_categories", updated).apply()
                            customCategories = updated.toList()
                        }
                    }
                )
                
                ChipSelectionRow(
                    title = "Tags",
                    items = allTags,
                    selectedItem = null,
                    onItemSelected = { tag ->
                        note = if (note.isEmpty()) tag else "${note}, $tag"
                    },
                    onCustomAdded = { 
                        if (it.isNotBlank() && !allTags.contains(it)) {
                            val updated = (prefs.getStringSet("custom_tags", emptySet()) ?: emptySet()) + it
                            prefs.edit().putStringSet("custom_tags", updated).apply()
                            customTags = updated.toList()
                        }
                    }
                )"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)

