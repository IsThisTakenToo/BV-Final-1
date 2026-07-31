import re

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    content = f.read()

target1 = """        // Inner stylized vault door / locked pin background
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
        }"""
        
replacement1 = """        // Inner stylized vault door / locked pin background
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
        }"""

content = content.replace(target1, replacement1)

target2 = """        // Background Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(SpotVaultColors.Surface.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
        )"""

replacement2 = """        // Background Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(SpotVaultColors.Surface.copy(alpha = 0.5f), RoundedCornerShape(30.dp))
                .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.2f), RoundedCornerShape(30.dp))
        )"""

content = content.replace(target2, replacement2)

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(content)
