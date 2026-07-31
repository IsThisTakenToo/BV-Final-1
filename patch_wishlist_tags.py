import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target_lists = """    val defaultCategories = listOf("Parking", "Entrance", "Viewpoint", "Food", "Trailhead", "Landmark", "Hotel", "Restroom", "Other")
    var customCategories by remember { mutableStateOf(prefs.getStringSet("custom_categories", emptySet())?.toList() ?: emptyList()) }
    val allCategories = (defaultCategories + customCategories).distinct()"""

replacement_lists = """    val defaultCategories = listOf("Parking", "Entrance", "Viewpoint", "Food", "Trailhead", "Landmark", "Hotel", "Restroom", "Other")
    var customCategories by remember { mutableStateOf(prefs.getStringSet("custom_categories", emptySet())?.toList() ?: emptyList()) }
    val allCategories = (defaultCategories + customCategories).distinct()

    val defaultTags = listOf("Level 1", "Level 2", "Must See", "Near Entrance", "Scenic", "Indoor", "Outdoor")
    var customTags by remember { mutableStateOf(prefs.getStringSet("custom_tags", emptySet())?.toList() ?: emptyList()) }
    val allTags = (defaultTags + customTags).distinct()"""
content = content.replace(target_lists, replacement_lists)

target_amusement = """                // Smart Chips for Amusement Park
                if (category == "Amusement Park") {
                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Near Entrance", "Blue Lot", "Row 5").forEach { chipText ->
                            androidx.compose.material3.ElevatedSuggestionChip(
                                onClick = { 
                                    val newText = if (note.isEmpty()) chipText else "${note}, $chipText"
                                    note = newText
                                },
                                label = { Text(chipText, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }"""

replacement_tags_dropdown = """                // Editable Tag Dropdown
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
                                    text = { Text("+ Add new tag: \"$tagInput\"", color = SpotVaultColors.PrimaryBright, fontWeight = FontWeight.Bold) },
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
content = content.replace(target_amusement, replacement_tags_dropdown)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)

