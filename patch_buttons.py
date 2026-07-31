import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                        if (ocrText.isNotBlank()) {
                            Text(
                                text = "Text: $ocrText", 
                                fontSize = 12.sp, 
                                color = Color.White.copy(alpha=0.7f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (lat != 0f && lng != 0f) {
                                androidx.compose.material3.Button(
                                    onClick = {
                                        val uri = android.net.Uri.parse("google.navigation:q=$lat,$lng&mode=w")
                                        val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                        mapIntent.setPackage("com.google.android.apps.maps")
                                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(mapIntent)
                                        } else {
                                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = SpotVaultColors.Primary,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.padding(end=8.dp))
                                    Text("NAVIGATE", fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            if (onFoundClick != null) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = onFoundClick, 
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.PrimaryBright.copy(alpha=0.5f)),
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                        contentColor = SpotVaultColors.PrimaryBright
                                    )
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.padding(end=8.dp))
                                    Text("FOUND IT", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }"""

replacement = """                        if (ocrText.isNotBlank()) {
                            Text(
                                text = "Text: $ocrText", 
                                fontSize = 12.sp, 
                                color = Color.White.copy(alpha=0.7f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    } // end scrollable column
                    
                    // Fixed buttons at the bottom
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (lat != 0f && lng != 0f) {
                            androidx.compose.material3.Button(
                                onClick = {
                                    val uri = android.net.Uri.parse("google.navigation:q=$lat,$lng&mode=w")
                                    val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(mapIntent)
                                    } else {
                                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                                    }
                                },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = SpotVaultColors.Primary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.padding(end=8.dp))
                                Text("NAVIGATE", fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        if (onFoundClick != null) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = onFoundClick, 
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.PrimaryBright.copy(alpha=0.5f)),
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    contentColor = SpotVaultColors.PrimaryBright
                                )
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.padding(end=8.dp))
                                Text("FOUND IT", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } // end column wrapping both
            } // end box"""

if target in content:
    content = content.replace(target, replacement)
    
    # We also need to change the parent Column to wrap both the scrollable column and the fixed row
    # Let's do a targeted replace for the beginning of the block
    target_start = """                // Bottom gradient for details and actions
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
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {"""
                    
    replacement_start = """                // Bottom gradient for details and actions
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
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {"""
    
    content = content.replace(target_start, replacement_start)
    
    with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Patched bottom panel for fixed buttons")
else:
    print("Target not found")
