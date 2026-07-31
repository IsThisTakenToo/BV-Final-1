import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

# 1. Bring content block down
target_1 = """    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding()
    ) {
        // Main content scrollable
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            GlassSurface("""

replacement_1 = """    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main content scrollable
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            GlassSurface("""
content = content.replace(target_1, replacement_1)

# 2. Fix the Share Spot button position
target_2 = """            Spacer(modifier = Modifier.height(20.dp))
            Spacer(modifier = Modifier.height(100.dp))
        }

        // Bottom Actions row (Found It, Navigate) and Share below"""

# We remove the huge Spacer(100.dp) that forces scroll space at the bottom (maybe we keep a smaller one).
# Actually, the user says: "Position the Share Spot button so it sits cleanly just above the bottom navigation / vault area."
# Let's just adjust the bottom area. Wait, there is no bottom navigation in PinnedStateView. The "Vault area" is probably at the bottom of SpotVaultScreen.
# Currently Bottom Actions row is outside the scrollable Column.
replacement_2 = """            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom Actions row (Found It, Navigate) and Share below"""
content = content.replace(target_2, replacement_2)

target_share = """            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = { shareLocation(context, lat.toDouble(), lng.toDouble(), currentAddress, locationDetails, photoPath) },
                modifier = Modifier.align(Alignment.CenterHorizontally).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.PrimaryBright.copy(alpha=0.5f)),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.PrimaryBright)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Spot", fontWeight = FontWeight.SemiBold)
            }
        }"""
replacement_share = """            Spacer(modifier = Modifier.height(20.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = { shareLocation(context, lat.toDouble(), lng.toDouble(), currentAddress, locationDetails, photoPath) },
                modifier = Modifier.align(Alignment.CenterHorizontally).height(48.dp).fillMaxWidth(0.6f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpotVaultColors.PrimaryBright.copy(alpha=0.5f)),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SpotVaultColors.PrimaryBright)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Spot", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }"""
content = content.replace(target_share, replacement_share)

# 3. Strengthen the live map preview in PinnedStateView
# "Make sure a clear, tappable map area is visible that shows the exact location with a pin."
# We should place an overlay pin on top of the StaticMapPreview or just ensure Leaflet shows a pin.
# Wait, StaticMapPreview uses Leaflet and actually shows a pin if we look at its code.
# Let's check how StaticMapPreview is implemented.
with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.write(content)
