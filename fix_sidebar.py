import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    lines = f.readlines()

# Strip out ALL expenses and journal_vouchers and the dangling SidebarItem(
new_lines = []
skip = False
skip_count = 0

for line in lines:
    if skip:
        skip_count -= 1
        if skip_count <= 0:
            skip = False
        continue
    
    if 'id = "expenses"' in line or 'id = "journal_vouchers"' in line:
        if len(new_lines) > 0 and 'SidebarItem(' in new_lines[-1]:
            new_lines.pop()
        skip = True
        skip_count = 5
        continue
        
    new_lines.append(line)

# We want to insert exactly after the `id = "employees"` that is inside `if (canAccessAccounting) {`
final_lines = []
in_sidebar_accounting = False

for i, line in enumerate(new_lines):
    final_lines.append(line)
    
    # We know the block starts with `if (canAccessAccounting) {` 
    if 'if (canAccessAccounting) {' in line and 'SidebarItem' not in line and 'list.add' not in line:
        in_sidebar_accounting = True
        
    if in_sidebar_accounting and 'onTabSelected("employees");' in line:
        # We know the next line is `                        )` 
        # so we will insert after the next line
        pass
        
    if in_sidebar_accounting and line.strip() == ')' and 'onTabSelected("employees")' in final_lines[-2]:
        insertion = """                        SidebarItem(
                            id = "expenses",
                            label = "سندات الصرف",
                            icon = Icons.Default.AttachMoney,
                            isSelected = currentTab == "expenses",
                            onClick = { onTabSelected("expenses"); onCloseDrawer?.invoke() }
                        )
                        SidebarItem(
                            id = "journal_vouchers",
                            label = "سند القيد المحاسبي",
                            icon = Icons.Default.ReceiptLong,
                            isSelected = currentTab == "journal_vouchers",
                            onClick = { onTabSelected("journal_vouchers"); onCloseDrawer?.invoke() }
                        )
"""
        final_lines.append(insertion)
        in_sidebar_accounting = False

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.writelines(final_lines)

print("Fixed again!")
