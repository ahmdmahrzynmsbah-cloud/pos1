import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

content = content.replace("Math.abs(it.total - tx.amount) < 0.1", "kotlin.math.abs(it.total - tx.amount) < 0.1")

# Also fix the \n issue in CrmScreen
pattern1 = r'desc \+= "" \+ items\.joinToString\(""\) \{ "- \$\{it\.productNameAr\} \(\$\{it\.quantity\} x \$\{formatMoney\(it\.price\)\}\)" \}'
rep1 = 'desc += "\\n" + items.joinToString("\\n") { "- ${it.productNameAr} (${it.quantity} x ${formatMoney(it.price)})" }'
content = re.sub(pattern1, rep1, content)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

