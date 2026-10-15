import re

with open('app/src/main/java/com/example/ui/AccountStatementsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('placeholder = { Text("بحث باسم العميل أو الجوال...", fontSize = 12.sp) }', 
                          'placeholder = { Text("بحث باسم العميل أو الجوال...", fontSize = 11.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }')

with open('app/src/main/java/com/example/ui/AccountStatementsScreen.kt', 'w') as f:
    f.write(content)

print("AccountStatementsScreen fixed.")
