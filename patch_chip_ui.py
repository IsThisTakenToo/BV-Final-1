import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

chip_ui_code = """
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChipSelectionRow(
    title: String,
    items: List<String>,
    selectedItem: String?,
    onItemSelected: (String) -> Unit,
    onCustomAdded: (String) -> Unit
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SpotVaultColors.OnSurface, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                val isSelected = (item == selectedItem) || (selectedItem?.contains(item) == true)
                androidx.compose.material3.Surface(
                    onClick = { onItemSelected(item) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) SpotVaultColors.Teal else SpotVaultColors.Elevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) SpotVaultColors.Teal else SpotVaultColors.Outline),
                ) {
                    Text(
                        text = item,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) SpotVaultColors.Ink else SpotVaultColors.OnSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            androidx.compose.material3.Surface(
                onClick = { showCustomDialog = true },
                shape = RoundedCornerShape(16.dp),
                color = SpotVaultColors.PrimaryDeep.copy(alpha=0.3f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.Teal.copy(alpha=0.5f)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Custom", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                }
            }
        }
    }
    
    if (showCustomDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Add Custom $title", fontWeight = FontWeight.Bold) },
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
                        onCustomAdded(customInput.trim())
                        onItemSelected(customInput.trim())
                    }
                    customInput = ""
                    showCustomDialog = false 
                }) { Text("Save", color = SpotVaultColors.Teal, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false; customInput = "" }) { Text("Cancel", color = SpotVaultColors.Muted) }
            },
            containerColor = SpotVaultColors.Surface,
            titleContentColor = SpotVaultColors.OnSurface,
            textContentColor = SpotVaultColors.OnSurface
        )
    }
}
"""

# Insert the code before TimerSelectionDialog
content = content.replace("@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)\n@Composable\nfun TimerSelectionDialog(", chip_ui_code + "\n@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)\n@Composable\nfun TimerSelectionDialog(")

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
