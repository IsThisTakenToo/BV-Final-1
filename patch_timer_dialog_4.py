import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target1 = """    val defaultCategories = listOf("Parking", "Entrance", "Viewpoint", "Food", "Trailhead", "Landmark", "Hotel", "Restroom", "Other")"""
replacement1 = """    val defaultCategories = listOf("Parking", "Entrance", "Viewpoint", "Food", "Trailhead", "Landmark", "Hotel", "Restroom", "Other", "Uncategorized")"""
content = content.replace(target1, replacement1)

target2 = """        content = {
            Column {"""
replacement2 = """        content = {
            Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {"""
# We need to make sure we only replace this specific occurrence in TimerSelectionDialog.
# Wait, this matches all PremiumDialog usages if there are others. Let's just find the index.
