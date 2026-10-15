import re

with open('app/src/main/java/com/example/ui/theme/Type.kt', 'r') as f:
    content = f.read()

new_font = """val AppFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.SemiBold),
    Font(R.font.cairo, FontWeight.Bold)
)"""

content = re.sub(r'val AppFontFamily = FontFamily\([^)]+\)', new_font, content)

with open('app/src/main/java/com/example/ui/theme/Type.kt', 'w') as f:
    f.write(content)
print("Updated Type.kt to use cairo")
