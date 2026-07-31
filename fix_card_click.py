import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

card_click_wrong = """                    androidx.compose.material3.ElevatedCard(
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

card_click_right = """                    androidx.compose.material3.ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable {
                            viewSpot = item
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = SpotVaultColors.Elevated)
                    ) {"""

content = content.replace(card_click_wrong, card_click_right)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
