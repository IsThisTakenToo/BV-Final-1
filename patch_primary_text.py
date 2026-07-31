with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("containerColor = SpotVaultColors.Primary,\n                                        contentColor = Color.White", "containerColor = SpotVaultColors.Primary,\n                                        contentColor = SpotVaultColors.OnPrimary")
content = content.replace("containerColor = SpotVaultColors.Primary,\n                                    contentColor = Color.White", "containerColor = SpotVaultColors.Primary,\n                                    contentColor = SpotVaultColors.OnPrimary")
content = content.replace("containerColor = SpotVaultColors.Primary,\n                                contentColor = Color.White", "containerColor = SpotVaultColors.Primary,\n                                contentColor = SpotVaultColors.OnPrimary")
content = content.replace("containerColor = SpotVaultColors.Primary,\n                    contentColor = Color.White", "containerColor = SpotVaultColors.Primary,\n                    contentColor = SpotVaultColors.OnPrimary")

content = content.replace("Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)", "Icon(Icons.Default.Share, contentDescription = null, tint = SpotVaultColors.OnPrimary)")
content = content.replace("Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White)", "Icon(Icons.Default.LocationOn, contentDescription = null, tint = SpotVaultColors.OnPrimary)")

# Fix SpotVaultTheme onPrimary
content = content.replace("onPrimary = Color.White", "onPrimary = SpotVaultColors.OnPrimary")

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
print("done")
