import re

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    content = f.read()

# SpotVaultAmbientBackground Canvas
target1 = """    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(SpotVaultColors.Void)
        val w = size.width
        val h = size.height
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SpotVaultColors.Primary.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(w * (0.18f + drift * 0.08f), h * 0.12f),
                radius = w * 0.75f
            ),
            center = Offset(w * (0.18f + drift * 0.08f), h * 0.12f),
            radius = w * 0.75f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SpotVaultColors.Teal.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(w * (0.85f - drift * 0.06f), h * 0.78f),
                radius = w * 0.65f
            ),
            center = Offset(w * (0.85f - drift * 0.06f), h * 0.78f),
            radius = w * 0.65f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SpotVaultColors.PrimaryDeep.copy(alpha = 0.35f), Color.Transparent),"""

replacement1 = """    val themeColors = LocalSpotVaultColors.current
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(themeColors.void)
        val w = size.width
        val h = size.height
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(themeColors.primary.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(w * (0.18f + drift * 0.08f), h * 0.12f),
                radius = w * 0.75f
            ),
            center = Offset(w * (0.18f + drift * 0.08f), h * 0.12f),
            radius = w * 0.75f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(themeColors.teal.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(w * (0.85f - drift * 0.06f), h * 0.78f),
                radius = w * 0.65f
            ),
            center = Offset(w * (0.85f - drift * 0.06f), h * 0.78f),
            radius = w * 0.65f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(themeColors.primaryDeep.copy(alpha = 0.35f), Color.Transparent),"""
content = content.replace(target1, replacement1)


# SpotVaultRadarScan Canvas
target2 = """    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.toPx() / 2f
            val c = Offset(r, r)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SpotVaultColors.Teal.copy(alpha = 0.35f * pulse),
                        Color.Transparent
                    ),
                    center = c,
                    radius = r * 1.15f
                ),
                radius = r * 1.05f,
                center = c
            )
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(SpotVaultColors.PrimaryBright, SpotVaultColors.Primary, SpotVaultColors.PrimaryDeep)
                ),
                radius = r * 0.96f,
                center = c
            )
            drawCircle(color = SpotVaultColors.Deep, radius = r * 0.82f, center = c)
            
            // Sweep effect
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        SpotVaultColors.Primary,
                        SpotVaultColors.Teal,
                        SpotVaultColors.PrimaryBright,
                        SpotVaultColors.Primary,
                        Color.Transparent
                    ),"""

replacement2 = """    val themeColors = LocalSpotVaultColors.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.toPx() / 2f
            val c = Offset(r, r)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        themeColors.teal.copy(alpha = 0.35f * pulse),
                        Color.Transparent
                    ),
                    center = c,
                    radius = r * 1.15f
                ),
                radius = r * 1.05f,
                center = c
            )
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(themeColors.primaryBright, themeColors.primary, themeColors.primaryDeep)
                ),
                radius = r * 0.96f,
                center = c
            )
            drawCircle(color = themeColors.deep, radius = r * 0.82f, center = c)
            
            // Sweep effect
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        themeColors.primary,
                        themeColors.teal,
                        themeColors.primaryBright,
                        themeColors.primary,
                        Color.Transparent
                    ),"""
content = content.replace(target2, replacement2)


# SpotVaultRadarScan inner Canvas
target3 = """            for (i in 0 until 5) {
                val isMajor = i % 2 == 0
                val angle = (rotationAngle + i * 72f) * (Math.PI / 180f).toFloat()
                val px = c.x + kotlin.math.cos(angle) * (r * 0.5f)
                val py = c.y + kotlin.math.sin(angle) * (r * 0.5f)
                drawCircle(
                    color = if (isMajor) SpotVaultColors.Teal else SpotVaultColors.PrimaryBright,
                    radius = if (isMajor) 4.dp.toPx() else 2.dp.toPx(),
                    center = Offset(px, py)
                )
            }"""

replacement3 = """            for (i in 0 until 5) {
                val isMajor = i % 2 == 0
                val angle = (rotationAngle + i * 72f) * (Math.PI / 180f).toFloat()
                val px = c.x + kotlin.math.cos(angle) * (r * 0.5f)
                val py = c.y + kotlin.math.sin(angle) * (r * 0.5f)
                drawCircle(
                    color = if (isMajor) themeColors.teal else themeColors.primaryBright,
                    radius = if (isMajor) 4.dp.toPx() else 2.dp.toPx(),
                    center = Offset(px, py)
                )
            }"""
content = content.replace(target3, replacement3)

target4 = """            drawCircle(
                color = SpotVaultColors.PrimaryBright.copy(alpha = 0.95f),
                radius = 6.dp.toPx(),
                center = Offset(
                    c.x + kotlin.math.cos((rotationAngle - 45f) * (Math.PI / 180f)).toFloat() * (r * 0.7f),
                    c.y + kotlin.math.sin((rotationAngle - 45f) * (Math.PI / 180f)).toFloat() * (r * 0.7f)
                )
            )"""

replacement4 = """            drawCircle(
                color = themeColors.primaryBright.copy(alpha = 0.95f),
                radius = 6.dp.toPx(),
                center = Offset(
                    c.x + kotlin.math.cos((rotationAngle - 45f) * (Math.PI / 180f)).toFloat() * (r * 0.7f),
                    c.y + kotlin.math.sin((rotationAngle - 45f) * (Math.PI / 180f)).toFloat() * (r * 0.7f)
                )
            )"""
content = content.replace(target4, replacement4)

target5 = """            // Inner core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SpotVaultColors.PrimaryBright, SpotVaultColors.PrimaryDeep),
                    center = c,
                    radius = r * 0.15f
                ),
                radius = r * 0.15f,
                center = c
            )
            drawCircle(color = SpotVaultColors.Void, radius = r * 0.12f, center = c)
            drawCircle(color = SpotVaultColors.Teal, radius = r * 0.07f, center = c)
        }
    }"""
    
replacement5 = """            // Inner core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(themeColors.primaryBright, themeColors.primaryDeep),
                    center = c,
                    radius = r * 0.15f
                ),
                radius = r * 0.15f,
                center = c
            )
            drawCircle(color = themeColors.void, radius = r * 0.12f, center = c)
            drawCircle(color = themeColors.teal, radius = r * 0.07f, center = c)
        }
    }"""
content = content.replace(target5, replacement5)


# PremiumBadge Canvas
target6 = """        // Vault Door Canvas
        androidx.compose.foundation.Canvas(modifier = Modifier.size(76.dp)) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // Base Metallic Door (Blended steel/teal/purple)
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        SpotVaultColors.Surface,
                        SpotVaultColors.Deep,
                        SpotVaultColors.PrimaryDeep
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )"""
            
replacement6 = """        val themeColors = LocalSpotVaultColors.current
        // Vault Door Canvas
        androidx.compose.foundation.Canvas(modifier = Modifier.size(76.dp)) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // Base Metallic Door (Blended steel/teal/purple)
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        themeColors.surface,
                        themeColors.deep,
                        themeColors.primaryDeep
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )"""
content = content.replace(target6, replacement6)

target7 = """            // Outer rim rotating gradient highlight
            withTransform({
                rotate(rotation)
            }) {
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                        colors = listOf(
                            SpotVaultColors.PrimaryDeep,
                            SpotVaultColors.PrimaryBright.copy(alpha = 0.8f),
                            SpotVaultColors.PrimaryDeep,
                            SpotVaultColors.Teal.copy(alpha = 0.8f),
                            SpotVaultColors.PrimaryDeep
                        ),
                        center = center
                    ),
                    radius = radius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx())
                )
            }"""

replacement7 = """            // Outer rim rotating gradient highlight
            withTransform({
                rotate(rotation)
            }) {
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                        colors = listOf(
                            themeColors.primaryDeep,
                            themeColors.primaryBright.copy(alpha = 0.8f),
                            themeColors.primaryDeep,
                            themeColors.teal.copy(alpha = 0.8f),
                            themeColors.primaryDeep
                        ),
                        center = center
                    ),
                    radius = radius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx())
                )
            }"""
content = content.replace(target7, replacement7)


with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(content)
print("Patched SpotVaultDesign.kt Canvas calls")
