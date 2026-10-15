import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Remove the broken KpiCard completely
idx = content.find("@Composable\n  fun KpiCard(")
if idx != -1:
    content = content[:idx]

# Now let's add the missing screens at the end!
missing_code = """

@Composable
fun AdminSettingsScreen(viewModel: AppViewModel) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("إعدادات المشرف")
    }
}

@Composable
fun FeatureRequestsScreen(viewModel: AppViewModel) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("طلبات الميزات")
    }
}

fun isPurchaseInvoice(
  inv: com.example.data.Invoice,
  supplierTransactions: List<com.example.data.SupplierTransaction> = emptyList(),
  ledgerEntries: List<com.example.data.LedgerEntry> = emptyList(),
  auditLogs: List<com.example.data.AuditLog> = emptyList(),
  customers: List<com.example.data.Customer> = emptyList(),
  suppliers: List<com.example.data.Supplier> = emptyList()
): Boolean {
  return suppliers.any { it.id == inv.customerId }
}
"""

content += missing_code

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

print("Fixed end of AppUI.kt")
