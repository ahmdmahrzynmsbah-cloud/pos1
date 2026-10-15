import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Replace the entire ShiftsScreen function. It starts with @Composable fun ShiftsScreen
pattern = r'@Composable\s*fun ShiftsScreen\([\s\S]*?(?=@Composable\s*fun \w|$)'
content = re.sub(pattern, '', content)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
