import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target1 = """                // Bottom gradient for details and actions
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
                ) {
                    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {"""

replacement1 = """                // Bottom gradient for details
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
                        .padding(top = 64.dp, bottom = 120.dp, start = 24.dp, end = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {"""

content = content.replace(target1, replacement1)

target2 = """                    } // end scrollable column
                    
                    // Fixed buttons at the bottom
                    Row(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 16.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {"""

replacement2 = """                    } // end scrollable column
                }
                
                // Fixed buttons pinned to true bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black)
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {"""

content = content.replace(target2, replacement2)

target3 = """                                Text("FOUND IT", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } // end column wrapping both
            } // end box
            }
        } else {"""

replacement3 = """                                Text("FOUND IT", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } // end button Box
            } // end main Box
            }
        } else {"""

content = content.replace(target3, replacement3)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)

print("done")
