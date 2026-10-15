import sys

def patch():
    with open('app/src/main/java/com/example/ui/InvoiceDetailsDialog.kt', 'r') as f:
        content = f.read()

    old_text = """                Text(
                    text = "تفاصيل فاتورة #${inv.id} \uD83E\uDDFE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )"""
                
    new_text = """                Text(
                    text = if (inv.isDraft) "تفاصيل مسودة #${inv.id} 📝" else "تفاصيل فاتورة #${inv.id} 🧾",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )"""
                
    content = content.replace(old_text, new_text)
    
    with open('app/src/main/java/com/example/ui/InvoiceDetailsDialog.kt', 'w') as f:
        f.write(content)

patch()
