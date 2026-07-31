import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

with open('patch_target.txt', 'r') as f:
    target = f.read()

replacement = """                ChipSelectionRow(
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
                )
"""

# The target might have a trailing newline, so let's be careful.
content = content.replace(target, replacement)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
