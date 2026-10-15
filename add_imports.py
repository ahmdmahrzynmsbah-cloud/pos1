with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    lines = f.readlines()

new_imports = """import android.content.Intent
import android.speech.RecognizerIntent
import android.app.Activity
"""

lines.insert(50, new_imports)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.writelines(lines)
