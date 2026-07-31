import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """        GradientCtaCard(
            title = "Snap Beacon",
            subtitle = "Photo + OCR",
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onSnapClick()
            },
            height = 176.dp,
            tealDominant = false,
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    SpotVaultColors.Primary.copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = SpotVaultColors.PrimaryBright
                    )
                }
            }
        )"""

replacement = """        val isLimeMagenta = LocalSpotVaultColors.current.id == "lime_magenta"
        val customDarkColor = SpotVaultColors.Ink
        val iconTint = if (isLimeMagenta) customDarkColor else SpotVaultColors.PrimaryBright
        val titleColor = if (isLimeMagenta) customDarkColor else null

        GradientCtaCard(
            title = "Snap Beacon",
            subtitle = "Photo + OCR",
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onSnapClick()
            },
            height = 176.dp,
            tealDominant = false,
            titleColor = titleColor,
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    SpotVaultColors.Primary.copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = iconTint
                    )
                }
            }
        )"""

if target in content:
    content = content.replace(target, replacement)
    print("Replaced target in MainActivity")
else:
    print("Could not find target in MainActivity")

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)

