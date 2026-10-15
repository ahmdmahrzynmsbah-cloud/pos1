import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

target = "androidx.compose.ui.window.Dialog(onDismissRequest = { /* Require explicit action */ }) {"
replacement = "androidx.compose.ui.window.Dialog(onDismissRequest = { /* Require explicit action */ }, properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {"

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
        f.write(content)
    print("Patched dialog successfully")
else:
    print("Target not found")
