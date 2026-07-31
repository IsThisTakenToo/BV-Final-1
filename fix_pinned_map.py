import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                    if (photoPath.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        androidx.compose.foundation.Image(
                            painter = coil.compose.rememberAsyncImagePainter(java.io.File(photoPath)),
                            contentDescription = "Saved Photo",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, SpotVaultColors.Primary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                                .clickable { showFullView = true }
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        StaticMapPreview(
                            lat = lat.toFloat(),
                            lng = lng.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, SpotVaultColors.Primary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                                .clickable { showFullView = true }
                        )
                    } else {
                        Spacer(modifier = Modifier.height(18.dp))
                        StaticMapPreview(
                            lat = lat.toFloat(),
                            lng = lng.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, SpotVaultColors.Primary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                                .clickable { showFullView = true }
                        )
                    }"""

replacement = """                    if (photoPath.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        androidx.compose.foundation.Image(
                            painter = coil.compose.rememberAsyncImagePainter(java.io.File(photoPath)),
                            contentDescription = "Saved Photo",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, SpotVaultColors.Primary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                                .clickable { showFullView = true }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .border(1.dp, SpotVaultColors.Primary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                    ) {
                        StaticMapPreview(
                            lat = lat.toFloat(),
                            lng = lng.toFloat(),
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f))
                                .clickable {
                                    val uri = android.net.Uri.parse("geo:0,0?q=$lat,$lng")
                                    val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(mapIntent)
                                    } else {
                                        try {
                                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Maps app not found", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(SpotVaultColors.Glass, RoundedCornerShape(12.dp))
                                    .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, tint = SpotVaultColors.PrimaryBright, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open Maps", color = SpotVaultColors.PrimaryBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
