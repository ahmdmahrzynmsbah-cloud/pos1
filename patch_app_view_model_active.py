import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("isActive = false, // Always require admin activation first", "isActive = true, // Auto-activate for testing")
content = content.replace("isActive = false, // Must be approved by Admin first!", "isActive = true, // Auto-activate for testing")

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
    f.write(content)

print("Patched isActive")
