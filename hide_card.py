import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Make the card completely invisible if not ADMIN
old_card = """            if (userAccount?.role != "ADMIN") {
        Card(
        modifier = Modifier.fillMaxWidth(),"""

new_card = """            if (false) { // HIDDEN: userAccount?.role != "ADMIN"
        Card(
        modifier = Modifier.fillMaxWidth(),"""

content = content.replace(old_card, new_card)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

print("AppUI mock roles card hidden.")
