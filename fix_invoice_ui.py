import re

with open('app/src/main/java/com/example/ui/InvoiceFormScreen.kt', 'r') as f:
    content = f.read()

# Fix the white background in search barcode scanner
barcode_old = """                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, PosTealHeader),"""
barcode_new = """                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, PosTealHeader.copy(alpha=0.5f)),"""
content = content.replace(barcode_old, barcode_new)

with open('app/src/main/java/com/example/ui/InvoiceFormScreen.kt', 'w') as f:
    f.write(content)
print("Barcode background fixed")
