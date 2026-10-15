import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Fix labels in Add/Edit Product dialogs (there might be multiple, so I'll replace globally in the file where these strings appear exactly)

# 1. English Name
content = content.replace('label = { Text("اسم المنتج (بالإنجليزية - اختياري)") }', 'label = { Text("الاسم بالإنجليزي (اختياري)", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }')

# 2. Barcode
content = content.replace('label = { Text("الباركود العالمي أو المحلي") }', 'label = { Text("الباركود") }')

# 3. Purchase price
content = content.replace('label = { Text("سعر الشراء *") }', 'label = { Text("سعر الشراء", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }')

# 4. Sale price
content = content.replace('label = { Text("سعر البيع *") }', 'label = { Text("سعر البيع", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }')

# 5. Quantity
content = content.replace('label = { Text("الكمية المتوفرة *") }', 'label = { Text("الكمية", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }')

# 6. Alert Level
content = content.replace('label = { Text("حد التنبيه") }', 'label = { Text("حد التنبيه", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }')

# 7. Arabic Name
content = content.replace('label = { Text("اسم المنتج (بالعربية) *") }', 'label = { Text("اسم المنتج *", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }')

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

print("Labels updated successfully.")
