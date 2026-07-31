import re

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    content = f.read()

target = """@Composable
fun SpotVaultAmbientBackground(modifier: Modifier = Modifier) {
    val themeColors = LocalSpotVaultColors.current

    val transition = rememberInfiniteTransition(label = "ambient")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(themeColors.Void)
        val w = size.width
        val h = size.height
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(themeColors.Primary.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(w * (0.18f + drift * 0.08f), h * 0.12f),
                radius = w * 0.75f
            ),
            center = Offset(w * (0.18f + drift * 0.08f), h * 0.12f),
            radius = w * 0.75f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(themeColors.Teal.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(w * (0.85f - drift * 0.06f), h * 0.78f),
                radius = w * 0.65f
            ),
            center = Offset(w * (0.85f - drift * 0.06f), h * 0.78f),
            radius = w * 0.65f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(themeColors.PrimaryDeep.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = w * 0.55f
            ),
            center = Offset(w * 0.5f, h * 0.45f),
            radius = w * 0.55f
        )
    }
}"""

replacement = """@Composable
fun SpotVaultAmbientBackground(modifier: Modifier = Modifier) {
    val themeColors = LocalSpotVaultColors.current

    val transition = rememberInfiniteTransition(label = "ambient")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )
    
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .androidx.compose.ui.draw.drawWithCache {
                val w = size.width
                val h = size.height
                
                val radius1 = w * 0.75f
                val brush1 = Brush.radialGradient(
                    colors = listOf(themeColors.Primary.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset.Zero,
                    radius = radius1
                )
                
                val radius2 = w * 0.65f
                val brush2 = Brush.radialGradient(
                    colors = listOf(themeColors.Teal.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset.Zero,
                    radius = radius2
                )
                
                val radius3 = w * 0.55f
                val brush3 = Brush.radialGradient(
                    colors = listOf(themeColors.PrimaryDeep.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = radius3
                )
                
                onDrawBehind {
                    drawRect(themeColors.Void)
                    
                    androidx.compose.ui.graphics.drawscope.withTransform({
                        translate(left = w * (0.18f + drift * 0.08f), top = h * 0.12f)
                    }) {
                        drawCircle(brush = brush1, radius = radius1, center = Offset.Zero)
                    }
                    
                    androidx.compose.ui.graphics.drawscope.withTransform({
                        translate(left = w * (0.85f - drift * 0.06f), top = h * 0.78f)
                    }) {
                        drawCircle(brush = brush2, radius = radius2, center = Offset.Zero)
                    }
                    
                    drawCircle(brush = brush3, radius = radius3, center = Offset(w * 0.5f, h * 0.45f))
                }
            }
    )
}"""

if target in content:
    content = content.replace(target, replacement)
    print("Replaced!")
else:
    print("Not found!")

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(content)

