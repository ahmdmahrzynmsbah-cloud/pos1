import re

with open('app/src/main/java/com/example/ui/InvoiceFormScreen.kt', 'r') as f:
    content = f.read()

# Fix borders and text colors in PosInvoiceItemRow and other places
content = content.replace('Color(0xFFE2E8F0)', 'MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)')
content = content.replace('Color(0xFF1E293B)', 'MaterialTheme.colorScheme.onSurface')
content = content.replace('Color(0xFF64748B)', 'MaterialTheme.colorScheme.onSurfaceVariant')
content = content.replace('Color(0xFF334155)', 'MaterialTheme.colorScheme.onSurface')
content = content.replace('Color(0xFF475569)', 'MaterialTheme.colorScheme.onSurfaceVariant')

# Fix other hardcoded colors from earlier
content = content.replace('Color(0xFFF1F5F9)', 'MaterialTheme.colorScheme.surfaceVariant')

with open('app/src/main/java/com/example/ui/InvoiceFormScreen.kt', 'w') as f:
    f.write(content)
print("Finished row replacements.")
