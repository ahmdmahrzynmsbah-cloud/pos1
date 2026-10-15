import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Just replace "color = MaterialTheme.colorScheme.surface," with "color = Color.White," for this specific canvas point
# Actually, inside Canvas, if we need surface, we can just use Color.White or extract the color.
# Let's extract surface color. But Color.White is easier.
content = content.replace("color = MaterialTheme.colorScheme.surface,\n                                radius = 3f", "color = Color.White,\n                                radius = 3f")

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
