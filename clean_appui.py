import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# 1. formatMoney
content = re.sub(r'fun formatMoney\(amount: Double\): String \{.*?\n\}', '', content, flags=re.DOTALL)

# 2. FinancialRow
content = re.sub(r'@Composable\s*fun FinancialRow\(label: String, value: Double, isTotal: Boolean = false\) \{.*?Row\(.*?\n\}\n\}', '', content, flags=re.DOTALL)

# 3. KpiCard
content = re.sub(r'@Composable\s*fun KpiCard\(.*?\n\}\n\}', '', content, flags=re.DOTALL)

# 4. CombinedCustomerTx
content = re.sub(r'data class CombinedCustomerTx\(.*?\)', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

print("Cleaned AppUI.kt")
