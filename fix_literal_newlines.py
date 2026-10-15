with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Replace all occurrences of literal newlines inside the `desc += ` assignment
fixed_content = content.replace('desc += "\\n" + items.joinToString("\\n") {', 'desc += "\\\\n" + items.joinToString("\\\\n") {')
with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(fixed_content)

