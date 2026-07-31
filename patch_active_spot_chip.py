import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                        androidx.compose.material3.SuggestionChip(
                            onClick = {},
                            label = { Text(cat, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(containerColor = SpotVaultColors.Deep, labelColor = SpotVaultColors.PrimaryBright),
                            border = null,
                            shape = RoundedCornerShape(8.dp)
                        )"""

replacement = """                        Box(
                            modifier = Modifier
                                .background(SpotVaultColors.PrimaryDeep.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .border(1.dp, SpotVaultColors.Teal.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(cat, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SpotVaultColors.Teal)
                        }"""
content = content.replace(target, replacement)

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
