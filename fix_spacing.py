import re

files_to_fix = [
    'app/src/main/java/com/example/ui/AppUI.kt',
    'app/src/main/java/com/example/ui/AdminUsersScreen.kt',
    'app/src/main/java/com/example/ui/BalancesReportScreen.kt',
    'app/src/main/java/com/example/ui/InvoiceFormScreen.kt'
]

for file_path in files_to_fix:
    try:
        with open(file_path, 'r') as f:
            content = f.read()
            
        # 1. Reduce main padding from 16.dp to 8.dp for screens
        # This regex looks for .fillMaxSize() followed by .padding(16.dp) or .padding(20.dp)
        content = re.sub(
            r'\.fillMaxSize\(\)\s*\.padding\((16\.dp|20\.dp|24\.dp)\)',
            r'.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)',
            content
        )
        content = re.sub(
            r'\.fillMaxSize\(\)\s*\.padding\(horizontal = \d+\.dp,\s*vertical = \d+\.dp\)',
            r'.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)',
            content
        )
        
        # 2. Also for AdminUsersScreen line 115-116:
        content = content.replace(
            ".background(MaterialTheme.colorScheme.background)\n                .padding(16.dp)",
            ".background(MaterialTheme.colorScheme.background)\n                .padding(8.dp)"
        )
        
        # 3. Reduce Arrangement.spacedBy(16.dp) to Arrangement.spacedBy(8.dp) in major columns
        # To avoid changing everything, we replace globally:
        content = content.replace(
            "Arrangement.spacedBy(16.dp)",
            "Arrangement.spacedBy(8.dp)"
        )
        content = content.replace(
            "Arrangement.spacedBy(12.dp)",
            "Arrangement.spacedBy(8.dp)"
        )
        
        # 4. Same for padding(16.dp) on Cards, let's make it 12.dp or 8.dp if it's top-level, but 
        # it's safer to leave inner card paddings mostly alone, or change them from 16.dp to 12.dp.
        content = content.replace(
            "padding(16.dp)",
            "padding(12.dp)"
        )
        
        # 5. Make sure Cards and buttons are fillMaxWidth
        # Actually, Jetpack Compose Cards default to wrap_content unless specified. 
        # So many cards might not have fillMaxWidth. 
        # I'll check how many Cards are there.
        
        with open(file_path, 'w') as f:
            f.write(content)
            
        print(f"Fixed {file_path}")
    except Exception as e:
        print(f"Error on {file_path}: {e}")
