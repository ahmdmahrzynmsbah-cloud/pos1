import re

with open('app/src/main/java/com/example/ui/AuthScreen.kt', 'r') as f:
    content = f.read()

# We want to remove the block from "Spacer(modifier = Modifier.height(24.dp))" 
# followed by "val context = androidx.compose.ui.platform.LocalContext.current"
# to the end of the method.

start_marker = "        Spacer(modifier = Modifier.height(24.dp))\n        val context = androidx.compose.ui.platform.LocalContext.current"
if start_marker in content:
    parts = content.split(start_marker)
    # The end should be just "    }\n}\n"
    new_content = parts[0] + "    }\n}\n"
    with open('app/src/main/java/com/example/ui/AuthScreen.kt', 'w') as f:
        f.write(new_content)
    print("Replaced!")
else:
    print("Marker not found")
