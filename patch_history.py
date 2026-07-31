import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

# 1. Header Wishlist Button
header_target = """            Button(
                onClick = { showWishlistDialog = true }, 
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = SpotVaultColors.Teal,
                    contentColor = SpotVaultColors.Ink
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Text("+ Wishlist", fontWeight = FontWeight.Bold)
            }"""

header_replacement = """            if (selectedTabIndex == 1) {
                Button(
                    onClick = { showWishlistDialog = true }, 
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = SpotVaultColors.Teal,
                        contentColor = SpotVaultColors.Ink
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text("+ Wishlist", fontWeight = FontWeight.Bold)
                }
            }"""

content = content.replace(header_target, header_replacement)


# 2. Thumbnail
thumb_target = """                            // Thumbnail
                            if (path.isNotEmpty()) {
                                androidx.compose.foundation.Image(
                                    painter = coil.compose.rememberAsyncImagePainter(java.io.File(path)),
                                    contentDescription = "Thumb",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                                )
                            } else if (item.lat != 0.0 && item.lng != 0.0) {
                                StaticMapPreview(
                                    lat = item.lat.toFloat(),
                                    lng = item.lng.toFloat(),
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                                )
                            }"""

thumb_replacement = """                            // Thumbnail
                            val hasValidPhoto = path.isNotEmpty() && java.io.File(path).exists()
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SpotVaultColors.Elevated)
                                    .border(1.dp, SpotVaultColors.Outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                    .clickable { viewSpot = item },
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasValidPhoto) {
                                    androidx.compose.foundation.Image(
                                        painter = coil.compose.rememberAsyncImagePainter(java.io.File(path)),
                                        contentDescription = "Thumb",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = SpotVaultColors.Muted.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("No photo", fontSize = 9.sp, color = SpotVaultColors.Muted.copy(alpha = 0.5f), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }"""

content = content.replace(thumb_target, thumb_replacement)


# 3. Card content (category, title, address, notes)
# The current is:
#                             Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
#                                 androidx.compose.material3.SuggestionChip(
#                                     onClick = {},
#                                     label = { Text(category, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
#                                     colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(containerColor = SpotVaultColors.Deep, labelColor = SpotVaultColors.Teal),
#                                     border = null,
#                                     shape = RoundedCornerShape(8.dp)
#                                 )
#                                 Text(date, fontSize = 12.sp, color = SpotVaultColors.Muted)
#                             }
#                             
#                             Spacer(modifier = Modifier.height(8.dp))
#                             
#                             if (item.title.isNotEmpty()) {
#                                 Text(item.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SpotVaultColors.OnSurface)
#                             }
#                             if (item.address.isNotEmpty()) {
#                                 Text(item.address, fontSize = 13.sp, color = SpotVaultColors.Muted, modifier = Modifier.padding(top=4.dp))
#                             }
#                             if (locationDetails.isNotEmpty()) {
#                                 Text("Notes: $locationDetails", fontSize = 13.sp, color = SpotVaultColors.Muted, modifier = Modifier.padding(top=4.dp))
#                             }
content_target = """                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.SuggestionChip(
                                    onClick = {},
                                    label = { Text(category, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(containerColor = SpotVaultColors.Deep, labelColor = SpotVaultColors.Teal),
                                    border = null,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Text(date, fontSize = 12.sp, color = SpotVaultColors.Muted)
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (item.title.isNotEmpty()) {
                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SpotVaultColors.OnSurface)
                            }
                            if (item.address.isNotEmpty()) {
                                Text(item.address, fontSize = 13.sp, color = SpotVaultColors.Muted, modifier = Modifier.padding(top=4.dp))
                            }
                            if (locationDetails.isNotEmpty()) {
                                Text("Notes: $locationDetails", fontSize = 13.sp, color = SpotVaultColors.Muted, modifier = Modifier.padding(top=4.dp))
                            }"""

content_replacement = """                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(SpotVaultColors.PrimaryDeep.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                                }
                                Text(date, fontSize = 12.sp, color = SpotVaultColors.Muted)
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            if (item.title.isNotEmpty()) {
                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = SpotVaultColors.OnSurface)
                            }
                            if (item.address.isNotEmpty()) {
                                Row(modifier = Modifier.padding(top=6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = SpotVaultColors.Teal.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(item.address, fontSize = 13.sp, color = SpotVaultColors.OnSurface.copy(alpha = 0.9f), lineHeight = 18.sp)
                                }
                            }
                            if (locationDetails.isNotEmpty()) {
                                Row(modifier = Modifier.padding(top=6.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = SpotVaultColors.PrimaryBright.copy(alpha = 0.8f), modifier = Modifier.size(14.dp).padding(top = 2.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(locationDetails, fontSize = 13.sp, color = SpotVaultColors.OnSurface.copy(alpha = 0.85f), lineHeight = 18.sp)
                                }
                            }"""

content = content.replace(content_target, content_replacement)


# Wait, when you click the whole card, it opens Map if item is not null. Let's fix that too.
card_click_target = """                    androidx.compose.material3.ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable {
                            viewSpot = item
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = SpotVaultColors.Elevated)
                    ) {"""

card_click_replacement = """                    androidx.compose.material3.ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable {
                            val uri = "geo:0,0?q=${item.lat},${item.lng}"
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Maps app not found", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = SpotVaultColors.Elevated)
                    ) {"""
content = content.replace(card_click_target, card_click_replacement)


with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
