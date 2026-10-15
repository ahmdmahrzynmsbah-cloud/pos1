import re

def patch_account_statements():
    with open('app/src/main/java/com/example/ui/AccountStatementsScreen.kt', 'r') as f:
        content = f.read()
    
    # 1. Add states
    content = content.replace("val supplierTransactions by viewModel.allSupplierTransactions.collectAsState(emptyList())",
        "val supplierTransactions by viewModel.allSupplierTransactions.collectAsState(emptyList())\n  val allInvoiceItemsState by viewModel.allInvoiceItems.collectAsState(emptyList())\n  var showStatementTypeDialog by remember { mutableStateOf(false) }")
    
    # 2. Extract the click block
    old_block = """          IconButton(
            onClick = {
              if (selectedCustomer != null) {
                val cust = selectedCustomer!!
                val relatedInvoices = invoices.filter { it.customerId == cust.id }
                val relatedTxs = customerTransactions.filter { it.customerId == cust.id }
                
                val combinedList = mutableListOf<StatementRow>()
                relatedInvoices.forEach { inv ->
                  combinedList.add(StatementRow(inv.invoiceDate, "فاتورة مبيعات رقم #${inv.id}", inv.total, inv.paidAmount, inv.dueDate))
                }
                relatedTxs.forEach { tx ->
                  combinedList.add(StatementRow(tx.date, tx.note.ifEmpty { if (tx.transactionType == "بيع") "فاتورة آجلة" else "سند قبض مسدد" }, if (tx.transactionType == "بيع") tx.amount else 0.0, if (tx.transactionType == "دفعة") tx.amount else 0.0, tx.dueDate))
                }
                combinedList.sortBy { it.date }
                
                val totalDeb = combinedList.sumOf { it.debit }
                val totalCred = combinedList.sumOf { it.credit }
                
                printContentTitle = "كشف حساب عميل: ${cust.name}"
                printContentBody = combinedList
                printSummaryText = "إجمالي المبيعات: ${formatLocalMoney(totalDeb)} شيكل | إجمالي السداد: ${formatLocalMoney(totalCred)} شيكل | المتبقي المستحق: ${formatLocalMoney(totalDeb - totalCred)} شيكل"
                entityDetails = "رقم الجوال: ${cust.phone} | التصنيف: ${cust.type}"
                showPrintSimulation = true
              } else if (selectedSupplier != null) {
                val sup = selectedSupplier!!
                val relatedTxs = supplierTransactions.filter { it.supplierId == sup.id }
                val combinedList = relatedTxs.sortedBy { it.date }.map { tx ->
                  StatementRow(
                    date = tx.date,
                    description = tx.note.ifEmpty { if (tx.transactionType == "شراء") "فاتورة توريد بضاعة" else "سند صرف دفعة مالية" },
                    debit = if (tx.transactionType != "شراء") tx.amount else 0.0, // payment reduces what we owe
                    credit = if (tx.transactionType == "شراء") tx.amount else 0.0 // purchase increases what we owe
                  )
                }
                
                val totalDeb = combinedList.sumOf { it.debit } // payments
                val totalCred = combinedList.sumOf { it.credit } // purchases
                
                printContentTitle = "كشف حساب مورد: ${sup.name}"
                printContentBody = combinedList
                printSummaryText = "إجمالي التوريد (دائن): ${formatLocalMoney(totalCred)} شيكل | إجمالي المسدد (مدين): ${formatLocalMoney(totalDeb)} شيكل | الرصيد المتبقي: ${formatLocalMoney(totalCred - totalDeb)} شيكل"
                entityDetails = "الشركة: ${sup.companyName} | جوال مسؤول الاتصال: ${sup.phone}"
                showPrintSimulation = true
              }
            }
          )"""
          
    new_block = """          IconButton(
            onClick = {
              if (selectedCustomer != null || selectedSupplier != null) {
                  showStatementTypeDialog = true
              }
            }
          )"""
          
    content = content.replace(old_block, new_block)
    
    # insert the dialog right after TopAppBar
    dialog_code = """
    if (showStatementTypeDialog) {
      AlertDialog(
        onDismissRequest = { showStatementTypeDialog = false },
        title = { Text("نوع كشف الحساب", fontWeight = FontWeight.Bold) },
        text = { Text("هل تريد كشف حساب مجمل أم تفصيلي؟") },
        confirmButton = {
          Button(
            onClick = {
              showStatementTypeDialog = false
              generateStatementPreview(true)
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
          ) { Text("تفصيلي") }
        },
        dismissButton = {
          Button(
            onClick = {
              showStatementTypeDialog = false
              generateStatementPreview(false)
            }
          ) { Text("مجمل") }
        }
      )
    }
"""

    generate_func = """
    val generateStatementPreview = { isDetailed: Boolean ->
        if (selectedCustomer != null) {
                val cust = selectedCustomer!!
                val relatedInvoices = invoices.filter { it.customerId == cust.id }
                val relatedTxs = customerTransactions.filter { it.customerId == cust.id }
                
                val combinedList = mutableListOf<StatementRow>()
                relatedInvoices.forEach { inv ->
                  var desc = "فاتورة مبيعات رقم #${inv.id}"
                  if (isDetailed) {
                      val items = allInvoiceItemsState.filter { it.invoiceId == inv.id }
                      if (items.isNotEmpty()) {
                          desc += "\\n" + items.joinToString("\\n") { "- ${it.productNameAr} (${it.quantity} x ${formatLocalMoney(it.price)})" }
                      }
                  }
                  combinedList.add(StatementRow(inv.invoiceDate, desc, inv.total, inv.paidAmount, inv.dueDate))
                }
                relatedTxs.forEach { tx ->
                  combinedList.add(StatementRow(tx.date, tx.note.ifEmpty { if (tx.transactionType == "بيع") "فاتورة آجلة" else "سند قبض مسدد" }, if (tx.transactionType == "بيع") tx.amount else 0.0, if (tx.transactionType == "دفعة") tx.amount else 0.0, tx.dueDate))
                }
                combinedList.sortBy { it.date }
                
                val totalDeb = combinedList.sumOf { it.debit }
                val totalCred = combinedList.sumOf { it.credit }
                
                printContentTitle = "كشف حساب عميل: ${cust.name}" + if (isDetailed) " (تفصيلي)" else " (مجمل)"
                printContentBody = combinedList
                printSummaryText = "إجمالي المبيعات: ${formatLocalMoney(totalDeb)} | إجمالي السداد: ${formatLocalMoney(totalCred)} | المتبقي المستحق: ${formatLocalMoney(totalDeb - totalCred)}"
                entityDetails = "رقم الجوال: ${cust.phone} | التصنيف: ${cust.type}"
                showPrintSimulation = true
        } else if (selectedSupplier != null) {
                val sup = selectedSupplier!!
                val relatedTxs = supplierTransactions.filter { it.supplierId == sup.id }
                val combinedList = relatedTxs.sortedBy { it.date }.map { tx ->
                  StatementRow(
                    date = tx.date,
                    description = tx.note.ifEmpty { if (tx.transactionType == "شراء") "فاتورة توريد بضاعة" else "سند صرف دفعة مالية" },
                    debit = if (tx.transactionType != "شراء") tx.amount else 0.0,
                    credit = if (tx.transactionType == "شراء") tx.amount else 0.0
                  )
                }
                
                val totalDeb = combinedList.sumOf { it.debit } // payments
                val totalCred = combinedList.sumOf { it.credit } // purchases
                
                printContentTitle = "كشف حساب مورد: ${sup.name}" + if (isDetailed) " (تفصيلي)" else " (مجمل)"
                printContentBody = combinedList
                printSummaryText = "إجمالي التوريد (دائن): ${formatLocalMoney(totalCred)} | إجمالي المسدد (مدين): ${formatLocalMoney(totalDeb)} | الرصيد المتبقي: ${formatLocalMoney(totalCred - totalDeb)}"
                entityDetails = "الشركة: ${sup.companyName} | جوال مسؤول الاتصال: ${sup.phone}"
                showPrintSimulation = true
        }
    }
"""

    content = content.replace("    CenterAlignedTopAppBar(", generate_func + "\n    CenterAlignedTopAppBar(")
    content = content.replace("    if (activeSubTab == \"customers\" || activeSubTab == \"suppliers\") {", dialog_code + "\n    if (activeSubTab == \"customers\" || activeSubTab == \"suppliers\") {")
    
    with open('app/src/main/java/com/example/ui/AccountStatementsScreen.kt', 'w') as f:
        f.write(content)

patch_account_statements()
