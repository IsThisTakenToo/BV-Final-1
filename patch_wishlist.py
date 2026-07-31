import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

# Add focusManager and keyboardController if missing
if 'val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current' not in content[content.find('fun WishlistDialog'):content.find('fun HistoryDialog')]:
    content = content.replace(
        'val coroutineScope = rememberCoroutineScope()',
        'val coroutineScope = rememberCoroutineScope()\n    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current\n    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current'
    )

# Fix categoryExpanded in WishlistDialog
content = content.replace(
    'onExpandedChange = { categoryExpanded = !categoryExpanded },',
    'onExpandedChange = { categoryExpanded = !categoryExpanded; if(categoryExpanded) { keyboardController?.hide(); focusManager.clearFocus() } },'
)

# Fix tagExpanded in WishlistDialog
content = content.replace(
    'onExpandedChange = { tagExpanded = !tagExpanded },',
    'onExpandedChange = { tagExpanded = !tagExpanded; if(tagExpanded) { keyboardController?.hide(); focusManager.clearFocus() } },'
)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
print("done")
