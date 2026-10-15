import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

target = """                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B5998.toInt())),"""
replacement = """                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B5998)),"""

target2 = """.background(Color(0xFF3B5998.toInt()).copy(alpha = 0.1f)),"""
replacement2 = """.background(Color(0xFF3B5998).copy(alpha = 0.1f)),"""

target3 = """.background(Color(0xFF3B5998.toInt())),"""
replacement3 = """.background(Color(0xFF3B5998)),"""


if target in content:
    content = content.replace(target, replacement)
if target2 in content:
    content = content.replace(target2, replacement2)
if target3 in content:
    content = content.replace(target3, replacement3)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
print("Patched exceptions successfully")
