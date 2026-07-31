import re

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    content = f.read()

target = """    Box(modifier = modifier.size(96.dp), contentAlignment = Alignment.Center) {
        // Outer glowing orb
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(SpotVaultColors.PrimaryBright.copy(alpha = alphaGlow), Color.Transparent)
                    ),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
        
        // Secondary glass layer for depth
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(SpotVaultColors.Glass, androidx.compose.foundation.shape.CircleShape)
                .border(1.dp, SpotVaultColors.PrimaryBright.copy(alpha=0.3f), androidx.compose.foundation.shape.CircleShape)
        )

        // Shimmering rotating rim
        Box(
            modifier = Modifier
                .size(76.dp)
                .graphicsLayer { rotationZ = rotation }
                .background(
                    brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                        colors = listOf(SpotVaultColors.PrimaryDeep, SpotVaultColors.Teal, SpotVaultColors.PrimaryBright, SpotVaultColors.PrimaryDeep)
                    ),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )

        // Inner stylized vault door / locked pin background
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(SpotVaultColors.Surface, androidx.compose.foundation.shape.CircleShape)
                .border(1.dp, SpotVaultColors.Outline.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_spotvault_mark),
                contentDescription = "Vault",
                tint = Color.Unspecified,
                modifier = Modifier.size(44.dp).scale(1.15f)
            )
        }
    }"""

replacement = """    Box(modifier = modifier.size(96.dp), contentAlignment = Alignment.Center) {
        // Outer glowing orb
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(SpotVaultColors.PrimaryBright.copy(alpha = alphaGlow), Color.Transparent)
                    ),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
        
        // Vault Door Canvas
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
            )

            // Outer rim rotating gradient highlight
            androidx.compose.ui.graphics.drawscope.withTransform({
                rotate(rotation, center)
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
            }
            
            // Inner vault styling
            drawCircle(
                color = SpotVaultColors.Surface,
                radius = radius * 0.85f,
                center = center
            )

            // Inner rotating locking wheel / bolt mechanism
            androidx.compose.ui.graphics.drawscope.withTransform({
                rotate(rotation * 1.5f, center) // Rotates slightly faster for effect
            }) {
                // Central locking hub
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(SpotVaultColors.PrimaryBright, SpotVaultColors.Teal),
                        center = center,
                        radius = radius * 0.35f
                    ),
                    radius = radius * 0.35f,
                    center = center
                )

                // Hub inner detailing
                drawCircle(
                    color = SpotVaultColors.Deep,
                    radius = radius * 0.15f,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )

                // Spokes/Bolts radiating outward
                val numBolts = 5
                for (i in 0 until numBolts) {
                    val angle = i * (360f / numBolts)
                    val angleRad = Math.toRadians(angle.toDouble())
                    
                    val innerX = center.x + (radius * 0.35f) * kotlin.math.cos(angleRad).toFloat()
                    val innerY = center.y + (radius * 0.35f) * kotlin.math.sin(angleRad).toFloat()
                    
                    val outerX = center.x + (radius * 0.7f) * kotlin.math.cos(angleRad).toFloat()
                    val outerY = center.y + (radius * 0.7f) * kotlin.math.sin(angleRad).toFloat()
                    
                    drawLine(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(SpotVaultColors.PrimaryBright, SpotVaultColors.Teal),
                            start = androidx.compose.ui.geometry.Offset(innerX, innerY),
                            end = androidx.compose.ui.geometry.Offset(outerX, outerY)
                        ),
                        start = androidx.compose.ui.geometry.Offset(innerX, innerY),
                        end = androidx.compose.ui.geometry.Offset(outerX, outerY),
                        strokeWidth = 4.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    
                    // Bolt heads at the end of the spokes
                    drawCircle(
                        color = SpotVaultColors.PrimaryBright,
                        radius = 4.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(outerX, outerY)
                    )
                }
            }

            // Static outer bolts (screws on the frame)
            val numOuterBolts = 8
            for (i in 0 until numOuterBolts) {
                val angle = i * (360f / numOuterBolts) + (360f / numOuterBolts / 2f)
                val angleRad = Math.toRadians(angle.toDouble())
                
                val boltX = center.x + (radius * 0.9f) * kotlin.math.cos(angleRad).toFloat()
                val boltY = center.y + (radius * 0.9f) * kotlin.math.sin(angleRad).toFloat()
                
                drawCircle(
                    color = SpotVaultColors.Outline.copy(alpha = 0.5f),
                    radius = 2.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(boltX, boltY)
                )
            }
        }
    }"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(content)
