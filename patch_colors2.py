import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    lines = f.readlines()

for i in range(2000, 2500):
    if "OutlinedTextField(" in lines[i]:
        # check if we already added it
        already_added = False
        for j in range(i, i+15):
            if "colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors" in lines[j]:
                already_added = True
                break
        
        if not already_added:
            for j in range(i+1, i+15):
                if "shape = " in lines[j] or "singleLine =" in lines[j]:
                    indent = lines[j][:len(lines[j]) - len(lines[j].lstrip())]
                    lines.insert(j, indent + "colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(unfocusedTextColor = SpotVaultColors.OnSurface, unfocusedLabelColor = SpotVaultColors.Muted, unfocusedBorderColor = SpotVaultColors.Outline, unfocusedLeadingIconColor = SpotVaultColors.Teal, focusedLabelColor = SpotVaultColors.Teal, focusedBorderColor = SpotVaultColors.Teal),\n")
                    break
                

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.writelines(lines)
print("done")
