import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Replace tint = MaterialTheme.colorScheme.surface -> onPrimary
content = content.replace("tint = MaterialTheme.colorScheme.surface", "tint = MaterialTheme.colorScheme.onPrimary")

# Replace color = MaterialTheme.colorScheme.surface -> onPrimary when used in Text
# There are not many, let's just replace all "color = MaterialTheme.colorScheme.surface" that are preceded by ", " or "    " and followed by ")" or ","
# Wait, Surface(..., color = MaterialTheme.colorScheme.surface, ...) is valid.
# How about we just replace "color = MaterialTheme.colorScheme.surface" with "color = MaterialTheme.colorScheme.onPrimary" 
# only if "Text(" or "Icon(" or "IconToggleButton(" is found in the preceding characters on the same line.
new_lines = []
for line in content.split('\n'):
    if "color = MaterialTheme.colorScheme.surface" in line:
        if "Text(" in line or "Icon(" in line or "IconToggleButton(" in line or "Text = " in line or "Text" in line:
            line = line.replace("color = MaterialTheme.colorScheme.surface", "color = MaterialTheme.colorScheme.onPrimary")
    new_lines.append(line)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write('\n'.join(new_lines))

print("Fixed text and icon colors.")
