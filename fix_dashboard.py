import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

idx = content.find("fun DashboardScreen(")
idx_brace = content.find("{", idx) + 1

insert_str = """
  var showAddPurchaseDialog by remember { mutableStateOf(false) }
"""
content = content[:idx_brace] + insert_str + content[idx_brace:]

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

print("Added showAddPurchaseDialog")
