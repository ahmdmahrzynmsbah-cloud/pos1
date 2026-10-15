import re
with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Just use regex to fix this specific mess
mess_pattern = r'desc \+= "\n" \+ items\.joinToString\("\n"\)'
content = re.sub(mess_pattern, 'desc += "\\\\n" + items.joinToString("\\\\n")', content)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

