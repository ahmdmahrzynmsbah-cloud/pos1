import sys

def patch():
    with open('app/src/main/java/com/example/ui/InvoiceDetailsDialog.kt', 'r') as f:
        content = f.read()

    old_import = "import androidx.compose.material.icons.filled.PictureAsPdf"
    new_import = "import androidx.compose.material.icons.filled.PictureAsPdf\nimport androidx.compose.material.icons.filled.Visibility"
    
    content = content.replace(old_import, new_import)
    
    with open('app/src/main/java/com/example/ui/InvoiceDetailsDialog.kt', 'w') as f:
        f.write(content)

patch()
