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
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars)
                        .padding(top = 64.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (note.isNotBlank()) {
                            Text("Notes", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(note, fontSize = 16.sp, color = Color.White)
                        }
                        
                        if (ocrText.isNotBlank()) {
                            Text("Detected Text", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(ocrText, fontSize = 14.sp, color = Color.White.copy(alpha=0.8f))
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (lat != 0f && lng != 0f) {
                                androidx.compose.material3.FilledTonalButton(
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
                                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.padding(end=8.dp))
                                    Text("Navigate", fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            if (onFoundClick != null) {
                                Button(
                                    onClick = onFoundClick, 
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.padding(end=8.dp))
                                    Text("Found It", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }"""

replacement = """                // Bottom gradient for details and actions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars)
                        .padding(top = 64.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (category.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .background(SpotVaultColors.PrimaryDeep.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(category, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                            }
                        }

                        if (address.isNotEmpty()) {
                            Text(
                                text = address,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        } else if (lat != 0f && lng != 0f) {
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.5f", lat)}, ${String.format(java.util.Locale.US, "%.5f", lng)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha=0.7f)
                            )
                        }

                        if (note.isNotBlank()) {
                            Text(
                                text = note, 
                                fontSize = 14.sp, 
                                color = Color.White.copy(alpha=0.9f),
                                maxLines = 3,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        
                        if (ocrText.isNotBlank()) {
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

if target in content:
    print("Replacing target...")
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
        f.write(content)
else:
    print("Target not found!")
