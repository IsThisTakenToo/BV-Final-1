import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                ChipSelectionRow(
                    title = "Category / Folder",
                    items = allCategories,
                    selectedItem = category,
                    onItemSelected = { category = it },
                    onCustomAdded = { saveCustomCategory(it) }
                )
                
                ChipSelectionRow(
                    title = "Tags",
                    items = allTags,
                    selectedItem = null, // Tags are appended to notes
                    onItemSelected = { tag ->
                        val newText = if (isCamera) {
                            if (editedOcrText.text.isEmpty()) tag else "${editedOcrText.text}, $tag"
                        } else {
                            if (note.text.isEmpty()) tag else "${note.text}, $tag"
                        }
                        if (isCamera) editedOcrText = TextFieldValue(text = newText, selection = TextRange(newText.length))
                        else note = TextFieldValue(text = newText, selection = TextRange(newText.length))
                    },
                    onCustomAdded = { saveCustomTag(it) }
                )"""

replacement = """                // Editable Category Dropdown
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
                        },
                        readOnly = true,
                        placeholder = { Text("Choose a category") },
                        label = { Text("Folder / Category", fontWeight = FontWeight.Bold) },
                        trailingIcon = {
                            androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        colors = androidx.compose.material3.ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    
                    if (categoryExpanded) {
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            allCategories.forEach { cat ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                            // Custom Option
                            var showCustomCategoryDialog by remember { mutableStateOf(false) }
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("+ Custom", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showCustomCategoryDialog = true
                                }
                            )
                            
                            if (showCustomCategoryDialog) {
                                var customInput by remember { mutableStateOf("") }
                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = { 
                                        showCustomCategoryDialog = false 
                                        categoryExpanded = false
                                    },
                                    title = { Text("Add Custom Category", fontWeight = FontWeight.Bold) },
                                    text = {
                                        OutlinedTextField(
                                            value = customInput,
                                            onValueChange = { customInput = it },
                                            singleLine = true,
                                            label = { Text("Enter name") },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { 
                                            if (customInput.isNotBlank()) {
                                                saveCustomCategory(customInput.trim())
                                                category = customInput.trim()
                                            }
                                            showCustomCategoryDialog = false 
                                            categoryExpanded = false
                                        }) { Text("Save", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { 
                                            showCustomCategoryDialog = false 
                                            categoryExpanded = false
                                        }) { Text("Cancel", color = SpotVaultColors.Muted) }
                                    },
                                    containerColor = SpotVaultColors.Surface,
                                    titleContentColor = SpotVaultColors.OnSurface,
                                    textContentColor = SpotVaultColors.OnSurface
                                )
                            }
                        }
                    }
                }

                // Editable Tag Dropdown
                var tagExpanded by remember { mutableStateOf(false) }
                
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = tagExpanded,
                    onExpandedChange = { tagExpanded = !tagExpanded },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = { },
                        readOnly = true,
                        placeholder = { Text("Select to add tags") },
                        label = { Text("Tags (Appended to notes)", fontWeight = FontWeight.Bold) },
                        trailingIcon = {
                            androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded)
                        },
                        colors = androidx.compose.material3.ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    
                    if (tagExpanded) {
                        ExposedDropdownMenu(
                            expanded = tagExpanded,
                            onDismissRequest = { tagExpanded = false }
                        ) {
                            allTags.forEach { tag ->
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
                                        tagExpanded = false
                                    }
                                )
                            }
                            
                            // Custom Option
                            var showCustomTagDialog by remember { mutableStateOf(false) }
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("+ Custom", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showCustomTagDialog = true
                                }
                            )
                            
                            if (showCustomTagDialog) {
                                var customInput by remember { mutableStateOf("") }
                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = { 
                                        showCustomTagDialog = false 
                                        tagExpanded = false
                                    },
                                    title = { Text("Add Custom Tag", fontWeight = FontWeight.Bold) },
                                    text = {
                                        OutlinedTextField(
                                            value = customInput,
                                            onValueChange = { customInput = it },
                                            singleLine = true,
                                            label = { Text("Enter name") },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { 
                                            if (customInput.isNotBlank()) {
                                                val tag = customInput.trim()
                                                saveCustomTag(tag)
                                                val newText = if (isCamera) {
                                                    if (editedOcrText.text.isEmpty()) tag else "${editedOcrText.text}, $tag"
                                                } else {
                                                    if (note.text.isEmpty()) tag else "${note.text}, $tag"
                                                }
                                                if (isCamera) editedOcrText = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                                else note = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                            }
                                            showCustomTagDialog = false 
                                            tagExpanded = false
                                        }) { Text("Save", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { 
                                            showCustomTagDialog = false 
                                            tagExpanded = false
                                        }) { Text("Cancel", color = SpotVaultColors.Muted) }
                                    },
                                    containerColor = SpotVaultColors.Surface,
                                    titleContentColor = SpotVaultColors.OnSurface,
                                    textContentColor = SpotVaultColors.OnSurface
                                )
                            }
                        }
                    }
                }"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
