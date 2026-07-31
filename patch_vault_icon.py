import re

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    content = f.read()

target1 = """    Box(modifier = modifier.size(80.dp), contentAlignment = Alignment.Center) {
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
                .size(70.dp)
                .background(SpotVaultColors.Glass, androidx.compose.foundation.shape.CircleShape)
                .border(1.dp, SpotVaultColors.PrimaryBright.copy(alpha=0.3f), androidx.compose.foundation.shape.CircleShape)
        )

        // Shimmering rotating rim
        Box(
            modifier = Modifier
                .size(64.dp)
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
                .size(58.dp)
                .background(SpotVaultColors.Surface, androidx.compose.foundation.shape.CircleShape)
                .border(1.dp, SpotVaultColors.Outline.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_spotvault_mark),
                contentDescription = "Vault",
                tint = Color.Unspecified,
                modifier = Modifier.size(38.dp).scale(1.15f)
            )
        }
    }"""
    
replacement1 = """    Box(modifier = modifier.size(96.dp), contentAlignment = Alignment.Center) {
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
                .size(70.dp)
                .background(SpotVaultColors.Surface, androidx.compose.foundation.shape.CircleShape)
                .border(1.dp, SpotVaultColors.Outline.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_spotvault_mark),
                contentDescription = "Vault",
                tint = Color.Unspecified,
                modifier = Modifier.size(46.dp).scale(1.15f)
            )
        }
    }"""

content = content.replace(target1, replacement1)

target2 = """@Composable
fun SpotVaultBottomBar(
    onVaultClick: () -> Unit,
    onAlertsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)"""
            
replacement2 = """@Composable
fun SpotVaultBottomBar(
    onVaultClick: () -> Unit,
    onAlertsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)"""
content = content.replace(target2, replacement2)

target3 = """        // Center Vault Button, visually popping out
        Box(
            modifier = Modifier
                .size(80.dp)"""
                
replacement3 = """        // Center Vault Button, visually popping out
        Box(
            modifier = Modifier
                .size(96.dp)"""
content = content.replace(target3, replacement3)

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(content)
