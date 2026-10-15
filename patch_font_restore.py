import re

with open('app/src/main/java/com/example/ui/theme/Type.kt', 'r') as f:
    content = f.read()

content = content.replace("val AppFontFamily = FontFamily.Default", "val AppFontFamily = FontFamily(\n    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),\n    Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),\n    Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold),\n    Font(R.font.ibm_plex_sans_arabic_bold, FontWeight.Bold)\n)")

with open('app/src/main/java/com/example/ui/theme/Type.kt', 'w') as f:
    f.write(content)
print("Patched Type.kt successfully")
