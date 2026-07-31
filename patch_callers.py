import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target1 = """        FullScreenImageViewer(
            imagePath = spot.imagePath,
            ocrText = "",
            note = spot.locationDetails,
            timestampStr = spot.timestamp.toString(),
            lat = spot.lat.toFloat(),
            lng = spot.lng.toFloat(),
            onDismiss = { viewSpot = null }
        )"""

replacement1 = """        FullScreenImageViewer(
            imagePath = spot.imagePath,
            ocrText = "",
            note = spot.locationDetails,
            timestampStr = spot.timestamp.toString(),
            lat = spot.lat.toFloat(),
            lng = spot.lng.toFloat(),
            onDismiss = { viewSpot = null },
            category = spot.category,
            address = spot.address
        )"""
content = content.replace(target1, replacement1)

target2 = """        FullScreenImageViewer(
            imagePath = photoPath,
            ocrText = "",
            note = locationDetails,
            timestampStr = System.currentTimeMillis().toString(),
            lat = lat.toFloat(),
            lng = lng.toFloat(),
            onDismiss = { showFullView = false },
            onFoundClick = onFoundClick
        )"""

replacement2 = """        FullScreenImageViewer(
            imagePath = photoPath,
            ocrText = "",
            note = locationDetails,
            timestampStr = System.currentTimeMillis().toString(),
            lat = lat.toFloat(),
            lng = lng.toFloat(),
            onDismiss = { showFullView = false },
            onFoundClick = onFoundClick,
            category = category,
            address = currentAddress
        )"""
content = content.replace(target2, replacement2)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
