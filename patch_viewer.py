import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                // Bottom gradient for details and actions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f), Color.Black)
                            )
                        )
                        .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars)
                        .padding(top = 64.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
                ) {"""

replacement = """                // Bottom gradient for details and actions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.65).dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f), Color.Black)
                            )
                        )
                        .navigationBarsPadding()
                        .padding(top = 64.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
                ) {"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Patched FullScreenImageViewer Box")
else:
    print("Box target not found")
