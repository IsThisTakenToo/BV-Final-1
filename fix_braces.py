import re

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    content = f.read()

target = """    MaterialTheme(
        colorScheme = if (darkTheme) darkColors else lightColors,
        typography = SpotVaultTypography,
        shapes = shapes,
        content = content
    )
}"""

replacement = """    MaterialTheme(
        colorScheme = if (darkTheme) darkColors else lightColors,
        typography = SpotVaultTypography,
        shapes = shapes,
        content = content
    )
    }
}"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Patched Braces")
else:
    print("Target not found")
