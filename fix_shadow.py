with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Add import if missing
if 'import androidx.compose.ui.draw.shadow' not in content:
    content = content.replace('import androidx.compose.ui.draw.clip', 'import androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.shadow')

# Or fallback if clip isn't explicitly imported
if 'import androidx.compose.ui.draw.shadow' not in content:
    content = content.replace('import androidx.compose.foundation.layout.*', 'import androidx.compose.foundation.layout.*\nimport androidx.compose.ui.draw.shadow')

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
print("Import added")
