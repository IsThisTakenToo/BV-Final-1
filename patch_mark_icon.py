import re

with open('app/src/main/res/drawable/ic_spotvault_mark.xml', 'r') as f:
    content = f.read()

target = """    <!-- Outer Drop Shadow for Pin -->"""
replacement = """    <group android:pivotX="54" android:pivotY="54" android:scaleX="0.75" android:scaleY="0.75">
    <!-- Outer Drop Shadow for Pin -->"""
content = content.replace(target, replacement)

target2 = """    <!-- Keyhole -->
    <path android:fillColor="#151022" android:pathData="M54,38 A2,2 0 1,0 54,42 A2,2 0 1,0 54,38 Z M52.5,41.5 L55.5,41.5 L56.5,46 L51.5,46 Z"/>
</vector>"""
replacement2 = """    <!-- Keyhole -->
    <path android:fillColor="#151022" android:pathData="M54,38 A2,2 0 1,0 54,42 A2,2 0 1,0 54,38 Z M52.5,41.5 L55.5,41.5 L56.5,46 L51.5,46 Z"/>
    </group>
</vector>"""
content = content.replace(target2, replacement2)

with open('app/src/main/res/drawable/ic_spotvault_mark.xml', 'w') as f:
    f.write(content)
