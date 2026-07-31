import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                    if (category.isNotEmpty() && category != "Other") {
                        androidx.compose.material3.FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text(category, fontWeight = FontWeight.SemiBold) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpotVaultColors.PrimaryDeep,
                                selectedLabelColor = SpotVaultColors.TealSoft
                            ),
                            modifier = Modifier.padding(top = 14.dp)
                        )
                    }"""

replacement = """                    if (category.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(top = 14.dp)
                                .background(SpotVaultColors.PrimaryDeep.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(category, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                        }
                    }"""
content = content.replace(target, replacement)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
