import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Fix ShiftsScreen in when block
content = re.sub(r'\s*-> ShiftsScreen\(viewModel, shifts, activeShift, invoices\)', '', content)

# Fix SidebarItem
bad_sidebar_item = r'\s*SidebarItem\(\s*id =,\s*label = "ورديات العمل والصندوق",\s*icon = Icons\.Default\.HistoryToggleOff,\s*isSelected = currentTab ==,\s*onClick = \{ onTabSelected\("shifts"\); onCloseDrawer\?\.invoke\(\) \}\s*\)'
content = re.sub(bad_sidebar_item, '', content)

# Look for Triple(,"shifts", 
bad_triple = r'\s*Triple\(,"الورديات ومراقبة الصندوق اليومي 🕒", Icons\.Default\.HistoryToggleOff\),'
content = re.sub(bad_triple, '', content)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
