import re

with open('app/src/main/java/com/example/ui/theme/Type.kt', 'r') as f:
    content = f.read()

content = content.replace('val AppFontFamily = FontFamily(', 'val AppFontFamily = FontFamily.Default // FontFamily(')

with open('app/src/main/java/com/example/ui/theme/Type.kt', 'w') as f:
    f.write(content)
print("Patched Type.kt")
