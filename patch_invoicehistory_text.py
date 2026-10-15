import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
        content = f.read()

    old_text = """                                                    Text(
                                                        text = "فاتورة #${inv.id}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        color = Color(0xFF1E293B)
                                                    )"""
                                                    
    new_text = """                                                    Text(
                                                        text = if (inv.isDraft) "مسودة #${inv.id} 📝" else "فاتورة #${inv.id}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        color = Color(0xFF1E293B)
                                                    )"""
                                                    
    content = content.replace(old_text, new_text)
    
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)

patch()
