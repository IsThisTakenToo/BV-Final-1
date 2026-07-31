import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

# Define the start and end of the FullScreenImageViewer function
start_pattern = r"@Composable\nfun FullScreenImageViewer\("
# Find the start index
start_idx = re.search(start_pattern, content).start()

# Now find the end index. It ends before "@Composable\nfun WishlistDialog"
end_pattern = r"@Composable\nfun WishlistDialog\("
end_idx = re.search(end_pattern, content).start()

target = content[start_idx:end_idx]

replacement = """@Composable
fun FullScreenImageViewer(
    imagePath: String, 
    ocrText: String, 
    note: String, 
    timestampStr: String,
    lat: Float,
    lng: Float,
    onDismiss: () -> Unit,
    onFoundClick: (() -> Unit)? = null,
    category: String = "",
    address: String = ""
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        if (imagePath.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = java.io.File(imagePath),
                    contentDescription = "Spot Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Top gradient for status bar and close button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                )

                // Top Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.Black.copy(alpha=0.4f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    
                    Text(
                        text = "Spot Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = { shareLocation(context, lat.toDouble(), lng.toDouble(), address, note, imagePath) },
                        modifier = Modifier.background(Color.Black.copy(alpha=0.4f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                }

                // Bottom gradient for details and actions
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
                }
            }
        } else {
            // New centered detail view layout
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SpotVaultColors.Background)
            ) {
                // Top Actions (Close and Share)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(SpotVaultColors.Surface, androidx.compose.foundation.shape.CircleShape).border(1.dp, SpotVaultColors.Outline.copy(alpha=0.3f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SpotVaultColors.OnSurface)
                    }
                    
                    Text(
                        text = "Spot Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = SpotVaultColors.OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = { shareLocation(context, lat.toDouble(), lng.toDouble(), address, note, imagePath) },
                        modifier = Modifier.background(SpotVaultColors.Surface, androidx.compose.foundation.shape.CircleShape).border(1.dp, SpotVaultColors.Outline.copy(alpha=0.3f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = SpotVaultColors.OnSurface)
                    }
                }

                // Centered content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(SpotVaultColors.Surface, androidx.compose.foundation.shape.CircleShape)
                            .border(1.dp, SpotVaultColors.PrimaryBright.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = SpotVaultColors.PrimaryBright, modifier = Modifier.size(36.dp))
                    }
                    
                    if (category.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(SpotVaultColors.PrimaryDeep.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(category, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                        }
                    }

                    if (address.isNotEmpty()) {
                        Text(
                            text = address,
                            style = MaterialTheme.typography.titleLarge,
                            color = SpotVaultColors.OnSurface,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    if (lat != 0f && lng != 0f) {
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.5f", lat)}, ${String.format(java.util.Locale.US, "%.5f", lng)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SpotVaultColors.Muted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    if (note.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(SpotVaultColors.Surface)
                                .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                .padding(20.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("Notes", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.PrimaryBright, modifier = Modifier.padding(bottom = 8.dp))
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = SpotVaultColors.OnSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = SpotVaultColors.Primary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("NAVIGATE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (onFoundClick != null) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = onFoundClick, 
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.PrimaryBright.copy(alpha=0.5f)),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.PrimaryBright)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("MARK AS FOUND", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
"""

content = content[:start_idx] + replacement + content[end_idx:]
with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
