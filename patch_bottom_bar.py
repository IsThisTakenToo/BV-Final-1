import re

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'r') as f:
    content = f.read()

content = content.replace("onAlertsClick: () -> Unit", "onSettingsClick: () -> Unit")
content = content.replace("onClick = onAlertsClick,", "onClick = onSettingsClick,")
content = content.replace('Icon(Icons.Default.Notifications, contentDescription = "Alerts"', 'Icon(Icons.Default.Settings, contentDescription = "Settings"')

with open('app/src/main/java/com/spotvault/app/SpotVaultDesign.kt', 'w') as f:
    f.write(content)
print("Patched SpotVaultDesign.kt")
