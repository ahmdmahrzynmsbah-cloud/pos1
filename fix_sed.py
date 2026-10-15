with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

content = content.replace('desc += "" + items.joinToString("") {', 'desc += "\\n" + items.joinToString("\\n") {')
with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

