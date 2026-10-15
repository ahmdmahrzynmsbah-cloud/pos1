import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Let's find patterns like:
# modifier = Modifier
#     .fillMaxSize()
#     .padding(16.dp)
# Or:
# modifier = Modifier.fillMaxSize().padding(16.dp)

# We want to change the outermost paddings of screens from 16.dp or 24.dp to 8.dp or 12.dp, and spacedBy(16.dp) to spacedBy(8.dp)

# Actually, doing this via regex might be dangerous without knowing context.
# Let's first collect all occurrences of padding and Arrangement in main containers.
import sys

lines = content.split('\n')
for i, line in enumerate(lines):
    if "fun " in line and "Screen(" in line:
        print(f"\n--- Screen: {line.strip()} ---")
    if ".fillMaxSize()" in line and ".padding(" in line:
        print(f"{i+1}: {line.strip()}")
    elif ".padding(" in line and "fillMaxSize" in lines[i-1] if i>0 else False:
        print(f"{i}: {lines[i-1].strip()} {line.strip()}")
