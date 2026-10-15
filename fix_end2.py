import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Fix the screens to have onBackClick
content = content.replace("fun AdminSettingsScreen(viewModel: AppViewModel)", "fun AdminSettingsScreen(viewModel: AppViewModel, onBackClick: () -> Unit = {})")
content = content.replace("fun FeatureRequestsScreen(viewModel: AppViewModel)", "fun FeatureRequestsScreen(viewModel: AppViewModel, onBackClick: () -> Unit = {})")

# Add FinancialStatementsTab back
tab = """
@Composable
fun FinancialStatementsTab(balances: Map<String, Double>) {
    val cash = balances["الصندوق"] ?: 0.0
    val bank = balances["البنك"] ?: 0.0
    val inventory = balances["مخزون مستودع 1"] ?: 0.0
    val capital = balances["رأس المال"] ?: 0.0
    val vat = balances["ضريبة القيمة المضافة"] ?: 0.0
    val revenue = balances["إيرادات المبيعات"] ?: 0.0
    val cogs = balances["تكلفة البضاعة المباعة"] ?: 0.0
    val expenses = balances["المصاريف التشغيلية"] ?: 0.0

    val netProfit = revenue - cogs - expenses
    val totalAssets = cash + bank + inventory
    val totalLiabilitiesAndEquity = capital + vat + netProfit

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Income Statement
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("قائمة الدخل التقديرية (Income Statement)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                HorizontalDivider()
                FinancialRow("إيرادات المبيعات والخدمات (النشاط):", revenue)
                FinancialRow("تكلفة البضاعة المباعة (COGS):", -cogs)
                FinancialRow("إجمالي المصاريف التشغيلية (Expenses):", -expenses)
                HorizontalDivider()
                FinancialRow("صافي الأرباح التشغيلية:", netProfit, isTotal = true)
            }
        }

        // Balance Sheet
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.02f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الميزانية العمومية المبسطة (Balance Sheet)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                HorizontalDivider()
                Text("الأصول (Assets):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                FinancialRow("النقدية بالصندوق (الدرج):", cash)
                FinancialRow("النقدية بالحساب البنكي:", bank)
                FinancialRow("مخزون مستودع البضاعة المتاحة:", inventory)
                FinancialRow("إجمالي الأصول المتداولة:", totalAssets, isTotal = true)

                HorizontalDivider()
                Text("الخصوم وحقوق الملكية (Liabilities & Equity):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                FinancialRow("رأس المال التأسيسي المستثمر:", capital)
                FinancialRow("أمانات ضريبة القيمة المضافة (15%):", vat)
                FinancialRow("الأرباح المرحلة الحالية للشركة:", netProfit)
                FinancialRow("إجمالي الخصوم وحقوق الملكية:", totalLiabilitiesAndEquity, isTotal = true)
            }
        }
    }
}
"""
content += tab

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
