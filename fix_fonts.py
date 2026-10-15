import re
import os

files_to_fix = [
    'app/src/main/java/com/example/ui/TransactionsHistoryScreen.kt',
    'app/src/main/java/com/example/ui/AppViewModel.kt',
    'app/src/main/java/com/example/ui/PdfGenerator.kt',
    'app/src/main/java/com/example/ui/InvoiceDetailsDialog.kt'
]

for file_path in files_to_fix:
    if os.path.exists(file_path):
        with open(file_path, 'r') as f:
            content = f.read()
        
        # Replace ResourcesCompat.getFont with Typeface.DEFAULT etc.
        content = re.sub(r'androidx\.core\.content\.res\.ResourcesCompat\.getFont\(context,\s*com\.example\.R\.font\.[a-zA-Z0-9_]+\)\s*\?:\s*(Typeface\.DEFAULT_BOLD|fontBold|Typeface\.DEFAULT)', r'\1', content)
        
        with open(file_path, 'w') as f:
            f.write(content)
        print(f"Fixed {file_path}")

