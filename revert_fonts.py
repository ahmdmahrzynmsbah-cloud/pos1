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
        
        # Replace Typeface.DEFAULT_BOLD with androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_bold) ?: Typeface.DEFAULT_BOLD
        content = re.sub(r'(?<!\?\:\s)Typeface\.DEFAULT_BOLD', r'androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_bold) ?: Typeface.DEFAULT_BOLD', content)
        
        # Replace Typeface.DEFAULT with androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_regular) ?: Typeface.DEFAULT
        content = re.sub(r'(?<!\?\:\s)Typeface\.DEFAULT', r'androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_regular) ?: Typeface.DEFAULT', content)
        
        with open(file_path, 'w') as f:
            f.write(content)
        print(f"Restored {file_path}")

