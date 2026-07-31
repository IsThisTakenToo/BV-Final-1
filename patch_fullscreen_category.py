import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target_signature = """@Composable
fun FullScreenImageViewer(
    imagePath: String, 
    ocrText: String, 
    note: String, 
    timestampStr: String,
    lat: Float,
    lng: Float,
    onDismiss: () -> Unit,
    onFoundClick: (() -> Unit)? = null
) {"""

replacement_signature = """@Composable
fun FullScreenImageViewer(
    imagePath: String, 
    ocrText: String, 
    note: String, 
    timestampStr: String,
    lat: Float,
    lng: Float,
    onDismiss: () -> Unit,
    onFoundClick: (() -> Unit)? = null,
    category: String = ""
) {"""
content = content.replace(target_signature, replacement_signature)

target_ui = """                Text(timestampStr, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))"""

replacement_ui = """                Text(timestampStr, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                if (category.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .background(SpotVaultColors.PrimaryDeep.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(category, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))"""
content = content.replace(target_ui, replacement_ui)

target_usage1 = """        FullScreenImageViewer(
            imagePath = viewSpot.imagePath,
            ocrText = viewSpot.ocrText,
            note = viewSpot.locationDetails,
            timestampStr = sdf.format(java.util.Date(viewSpot.timestamp)),
            lat = viewSpot.latitude.toFloat(),
            lng = viewSpot.longitude.toFloat(),
            onDismiss = { viewSpot = null }
        )"""

replacement_usage1 = """        FullScreenImageViewer(
            imagePath = viewSpot.imagePath,
            ocrText = viewSpot.ocrText,
            note = viewSpot.locationDetails,
            timestampStr = sdf.format(java.util.Date(viewSpot.timestamp)),
            lat = viewSpot.latitude.toFloat(),
            lng = viewSpot.longitude.toFloat(),
            onDismiss = { viewSpot = null },
            category = viewSpot.category
        )"""
content = content.replace(target_usage1, replacement_usage1)

target_usage2 = """        FullScreenImageViewer(
            imagePath = photoPath,
            ocrText = "",
            note = locationDetails,
            timestampStr = "",
            lat = lat.toFloat(),
            lng = lng.toFloat(),
            onDismiss = { showFullView = false }
        )"""

replacement_usage2 = """        FullScreenImageViewer(
            imagePath = photoPath,
            ocrText = "",
            note = locationDetails,
            timestampStr = "",
            lat = lat.toFloat(),
            lng = lng.toFloat(),
            onDismiss = { showFullView = false },
            category = category
        )"""
content = content.replace(target_usage2, replacement_usage2)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
