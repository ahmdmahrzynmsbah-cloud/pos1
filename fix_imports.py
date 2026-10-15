with open('app/src/main/java/com/example/ui/BalancesReportScreen.kt', 'r') as f:
    content = f.read()

if 'import androidx.compose.foundation.horizontalScroll' not in content:
    content = content.replace('import androidx.compose.foundation.layout.*', 'import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.horizontalScroll\nimport androidx.compose.foundation.rememberScrollState')

with open('app/src/main/java/com/example/ui/BalancesReportScreen.kt', 'w') as f:
    f.write(content)
