import re

def fix_file(path):
    with open(path, 'r') as f:
        content = f.read()
    
    # We already added the opening group tag, let's remove it first to be safe
    content = content.replace('<group android:pivotX="54" android:pivotY="54" android:scaleX="0.75" android:scaleY="0.75">\n    <!-- Outer Drop Shadow for Pin -->', '    <!-- Outer Drop Shadow for Pin -->')
    
    # Now replace the start and end correctly
    content = content.replace('    <!-- Outer Drop Shadow for Pin -->', '    <group android:pivotX="54" android:pivotY="54" android:scaleX="0.75" android:scaleY="0.75">\n    <!-- Outer Drop Shadow for Pin -->')
    
    content = content.replace('</vector>', '    </group>\n</vector>')
    
    with open(path, 'w') as f:
        f.write(content)

fix_file('app/src/main/res/drawable/ic_launcher_foreground.xml')
fix_file('app/src/main/res/drawable/ic_spotvault_mark.xml')

