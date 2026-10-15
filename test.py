import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()
    
print("File lines:", len(content.split('\n')))
