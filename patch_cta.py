import re

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    content = f.read()

target1 = """fun GradientCtaCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 168.dp,
    tealDominant: Boolean = false
) {"""

replacement1 = """fun GradientCtaCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 168.dp,
    tealDominant: Boolean = false,
    titleColor: Color? = null
) {"""

content = content.replace(target1, replacement1)

target2 = """            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (tealDominant) SpotVaultColors.Ink else SpotVaultColors.OnSurface
            )"""

replacement2 = """            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = titleColor ?: (if (tealDominant) SpotVaultColors.Ink else SpotVaultColors.OnSurface)
            )"""

content = content.replace(target2, replacement2)

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(content)

