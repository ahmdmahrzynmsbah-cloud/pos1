import sys

def patch():
    with open('app/src/main/java/com/example/ui/EmployeesScreen.kt', 'r') as f:
        content = f.read()

    content = content.replace("import androidx.compose.material.icons.filled.Visibility\n", "")
    content = content.replace("import androidx.compose.material.icons.filled.Print", "import androidx.compose.material.icons.filled.Print\nimport androidx.compose.material.icons.filled.Visibility")
    
    with open('app/src/main/java/com/example/ui/EmployeesScreen.kt', 'w') as f:
        f.write(content)

patch()
