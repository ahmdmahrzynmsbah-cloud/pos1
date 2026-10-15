import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Fix mock roles block (more robustly matching just the block to remove it)
# We will use regex to find and remove the block from if (userAccount?.role != "ADMIN") to just before val currentSymbol by viewModel.currencySymbol.collectAsState()
old_roles = r'            if \(userAccount\?\.role \!= "ADMIN"\) \{.*?\}\s*val currentSymbol by viewModel\.currencySymbol\.collectAsState\(\)'
new_roles = '            val currentSymbol by viewModel.currencySymbol.collectAsState()'

content = re.sub(old_roles, new_roles, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

print("AppUI mock roles regex removed.")
