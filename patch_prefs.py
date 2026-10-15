import re

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace('if (proj.isBlank() || key.isBlank() || url.isBlank()) {', 'if (proj.isBlank() || key.isBlank() || url.isBlank() || proj == "fox-tech-pos-cloud") {')

with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
    f.write(content)
