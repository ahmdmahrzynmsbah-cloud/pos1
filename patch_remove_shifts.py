import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

content = re.sub(r'\n\s*baseList\.add\(TabItem\("shifts"[^\)]+\)\)', '', content)
content = re.sub(r'\n\s*val shifts by viewModel\.allShifts\.collectAsState\(emptyList\(\)\)', '', content)
content = content.replace('"shifts", ', '')
content = content.replace(' "shifts"', '')

# Remove from MoreScreen
content = re.sub(r'\n\s*Triple\("shifts"[^\n]+,', '', content)

# Remove Shift SidebarItem
content = re.sub(r'\n\s*SidebarItem\(\s*id = "shifts"[\s\S]*?\n\s*\)', '', content)

# Remove quick actions from POS
content = re.sub(r'\n\s*PosQuickAction\(\s*"الورديات"[\s\S]*?\n\s*\)', '', content)

# Remove ShiftsScreen usage
content = re.sub(r'\n\s*"shifts" -> ShiftsScreen[^\n]+', '', content)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
