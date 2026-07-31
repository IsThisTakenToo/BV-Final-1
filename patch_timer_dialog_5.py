import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target2 = """    PremiumDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isCamera) Icons.Default.CameraAlt else Icons.Default.LocationOn, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(28.dp).padding(end = 8.dp))
                Text(if (isCamera) "Save Photo & Pin" else "Save Location", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.OnSurface)
            }
        },
        content = {
            Column {"""

replacement2 = """    PremiumDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isCamera) Icons.Default.CameraAlt else Icons.Default.LocationOn, contentDescription = null, tint = SpotVaultColors.Teal, modifier = Modifier.size(28.dp).padding(end = 8.dp))
                Text(if (isCamera) "Save Photo & Pin" else "Save Location", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.OnSurface)
            }
        },
        content = {
            Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(vertical = 4.dp)) {"""

content = content.replace(target2, replacement2)
with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
