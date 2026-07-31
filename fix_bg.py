import re

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    content = f.read()

# Add imports
if "import androidx.compose.ui.draw.drawWithCache" not in content:
    content = content.replace("import androidx.compose.ui.draw.clip", "import androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.drawWithCache\nimport androidx.compose.ui.graphics.drawscope.translate")


target = """    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .androidx.compose.ui.draw.drawWithCache {
                val w = size.width"""

replacement = """    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width"""

content = content.replace(target, replacement)

target2 = """                    androidx.compose.ui.graphics.drawscope.withTransform({
                        translate(left = w * (0.18f + drift * 0.08f), top = h * 0.12f)
                    }) {
                        drawCircle(brush = brush1, radius = radius1, center = Offset.Zero)
                    }
                    
                    androidx.compose.ui.graphics.drawscope.withTransform({
                        translate(left = w * (0.85f - drift * 0.06f), top = h * 0.78f)
                    }) {
                        drawCircle(brush = brush2, radius = radius2, center = Offset.Zero)
                    }"""

replacement2 = """                    withTransform({
                        translate(left = w * (0.18f + drift * 0.08f), top = h * 0.12f)
                    }) {
                        drawCircle(brush = brush1, radius = radius1, center = Offset.Zero)
                    }
                    
                    withTransform({
                        translate(left = w * (0.85f - drift * 0.06f), top = h * 0.78f)
                    }) {
                        drawCircle(brush = brush2, radius = radius2, center = Offset.Zero)
                    }"""

content = content.replace(target2, replacement2)

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(content)

