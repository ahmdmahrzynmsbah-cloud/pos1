import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

idx = content.find("if (showAddExpenseDialog)")
sub = content[:idx]
print(f"Open: {sub.count('{')}, Close: {sub.count('}')}")
