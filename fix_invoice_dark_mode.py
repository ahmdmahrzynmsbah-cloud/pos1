import re

with open('app/src/main/java/com/example/ui/InvoiceFormScreen.kt', 'r') as f:
    content = f.read()

# Fix PosBgLight (line 57 and usage) -> we can just keep PosBgLight but we don't need to change its definition if we replace its usage.
content = content.replace('.background(PosBgLight)', '.background(MaterialTheme.colorScheme.background)')

# Card 1 container (Scanner input bar)
content = content.replace('colors = CardDefaults.cardColors(containerColor = Color.White),', 'colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),')

# BasicTextField background
content = content.replace('.background(Color.White, RoundedCornerShape(6.dp))', '.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))')
content = content.replace('.border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))', '.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))')
content = content.replace('color = Color(0xFF0F172A)', 'color = MaterialTheme.colorScheme.onSurface')
content = content.replace('color = Color(0xFF475569)', 'color = MaterialTheme.colorScheme.onSurfaceVariant')

# Dropdown / Popups
content = content.replace('containerColor = Color.White', 'containerColor = MaterialTheme.colorScheme.surface')
content = content.replace('focusedContainerColor = Color.White', 'focusedContainerColor = MaterialTheme.colorScheme.surface')
content = content.replace('unfocusedContainerColor = Color.White', 'unfocusedContainerColor = MaterialTheme.colorScheme.surface')

# Fix divider color in dropdown
content = content.replace('HorizontalDivider(color = Color(0xFFF1F5F9))', 'HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)')

# Textfield in bottom sheet for discount
content = content.replace('focusedContainerColor = Color.White', 'focusedContainerColor = MaterialTheme.colorScheme.surface')
content = content.replace('unfocusedContainerColor = Color.White', 'unfocusedContainerColor = MaterialTheme.colorScheme.surface')

# Main layout box/backgrounds
content = content.replace('.background(Color.White)', '.background(MaterialTheme.colorScheme.surface)')
content = content.replace('containerColor = if (isSelected) Color(0xFFF0FDF4) else Color.White', 'containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface')
content = content.replace('containerColor = if (inCartQty > 0) Color(0xFFF0FDF4) else Color.White', 'containerColor = if (inCartQty > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface')

with open('app/src/main/java/com/example/ui/InvoiceFormScreen.kt', 'w') as f:
    f.write(content)
print("Finished replacements.")
