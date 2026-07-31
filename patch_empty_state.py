import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """        // List
        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text(if (showFavoritesOnly) "No favorite pins." else "No recent pins.", color = SpotVaultColors.Muted)
            }
        }"""

replacement = """        // List
        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (selectedTabIndex == 1) Icons.Default.BookmarkBorder else Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = SpotVaultColors.Muted.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (selectedTabIndex == 1) "Your Wishlist is Empty" else if (showFavoritesOnly) "No Favorite Spots" else "Your Vault is Empty",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpotVaultColors.OnSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedTabIndex == 1) "Save places you want to visit later." else "Snap a photo or drop a pin to save a spot.",
                        fontSize = 14.sp,
                        color = SpotVaultColors.Muted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)

