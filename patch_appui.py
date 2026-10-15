import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# 1. We need allInvoiceItemsState in AppUI.kt, specifically in CashierPOSUI.
# Wait, CashierPOSUI has signature `fun CashierPOSUI(...)`
# We can add `val allInvoiceItemsState by viewModel.allInvoiceItems.collectAsState(emptyList())`
# Or we can just add it inside the `showStatementForCust?.let { cust ->` block.
# Wait, collectAsState must be called from a composable scope directly, not inside a let block if it's conditional?
# Actually `showStatementForCust?.let` is inside `if (showSalesSetupDialog)`.
# `if (showSalesSetupDialog)` is a composable block. So we can add it there.

# 2. Add allInvoiceItemsState to showSalesSetupDialog block
old_block_1 = """    var showStatementForCust by remember { mutableStateOf<Customer?>(null) }
    var custSearchQuery by remember { mutableStateOf("") }"""

new_block_1 = """    var showStatementForCust by remember { mutableStateOf<Customer?>(null) }
    var custSearchQuery by remember { mutableStateOf("") }
    val allInvoiceItemsState by viewModel.allInvoiceItems.collectAsState(emptyList())
    var showPrintTypeDialogForCust by remember { mutableStateOf(false) }
    var pendingPrintActionForCust by remember { mutableStateOf("") }"""

content = content.replace(old_block_1, new_block_1)

# 3. Replace the ConfirmButton of showStatementForCust
old_confirm_button = """        confirmButton = {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = { showStatementForCust = null }) { Text("إغلاق") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = {
                  val cleanPhone = cust.phone.replace("+", "").replace(" ", "").filter { it.isDigit() }
                  val mappedRows = combinedTxList.map { 
                    com.example.ui.StatementRow(it.date, it.description, it.debit, it.credit)
                  }
                  val summaryText = "إجمالي المبيعات: ${formatMoney(totalDebit)} | إجمالي السداد: ${formatMoney(totalCredit)} | المتبقي المستحق: ${formatMoney(outstandingBalance)}"
                  val entityDetails = "رقم الجوال: ${cust.phone} | التصنيف: ${cust.type}"
                  
                  val pdfFile = com.example.ui.PdfGenerator.generateStatementPdfFile(
                    context = context,
                    storeName = viewModel.storeName.value,
                    storePhone = viewModel.storePhone.value,
                    title = "كشف حساب عميل: ${cust.name}",
                    bodyRows = mappedRows,
                    summaryText = summaryText,
                    entityDetails = entityDetails
                  )
                  if (pdfFile != null) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                      type = "application/pdf"
                      putExtra(android.content.Intent.EXTRA_STREAM, uri)
                      putExtra(android.content.Intent.EXTRA_TEXT, "مرفق كشف الحساب من ${viewModel.storeName.value}")
                      if (cleanPhone.isNotEmpty()) {
                        putExtra("jid", "$cleanPhone@s.whatsapp.net")
                      }
                      addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    // Try personal WhatsApp first
                    shareIntent.setPackage("com.whatsapp")
                    try {
                      context.startActivity(shareIntent)
                      viewModel.showToast("تم فتح واتساب للمشاركة ")
                    } catch (e: Exception) {
                      // Try Business WhatsApp
                      shareIntent.setPackage("com.whatsapp.w4b")
                      try {
                        context.startActivity(shareIntent)
                        viewModel.showToast("تم فتح واتساب للأعمال للمشاركة ")
                      } catch (e2: Exception) {
                        // Fallback to chooser
                        shareIntent.setPackage(null)
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة كشف الحساب"))
                      }
                    }
                  } else {
                    viewModel.showToast("حدث خطأ أثناء إنشاء ملف PDF ")
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
              ) {
                Text("واتساب", color = MaterialTheme.colorScheme.onPrimary)
              }
              Button(
                onClick = {
                  val mappedRows = combinedTxList.map { 
                    com.example.ui.StatementRow(it.date, it.description, it.debit, it.credit)
                  }
                  val summaryText = "إجمالي المبيعات: ${formatMoney(totalDebit)} | إجمالي السداد: ${formatMoney(totalCredit)} | المتبقي المستحق: ${formatMoney(outstandingBalance)}"
                  val entityDetails = "رقم الجوال: ${cust.phone} | التصنيف: ${cust.type}"
                  viewModel.exportStatementToPdf(context, "كشف حساب عميل: ${cust.name}", mappedRows, summaryText, entityDetails)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
              ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("PDF", fontSize = 12.sp)
              }
            }
          }
        }"""

new_confirm_button = """        confirmButton = {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = { showStatementForCust = null }) { Text("إغلاق") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = {
                    pendingPrintActionForCust = "whatsapp"
                    showPrintTypeDialogForCust = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
              ) {
                Text("واتساب", color = MaterialTheme.colorScheme.onPrimary)
              }
              Button(
                onClick = {
                    pendingPrintActionForCust = "pdf"
                    showPrintTypeDialogForCust = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
              ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("PDF", fontSize = 12.sp)
              }
            }
          }
        }"""

content = content.replace(old_confirm_button, new_confirm_button)

if old_confirm_button not in content and new_confirm_button not in content:
    print("Warning: old_confirm_button not found in AppUI.kt")

# 4. Insert the PrintTypeDialog inside showStatementForCust block
# Let's find the end of `showStatementForCust?.let { cust -> ... }`
# The end of the AlertDialog is `confirmButton = { ... } )`
end_of_dialog = """          }
        }
      )
"""

dialog_code = """
      if (showPrintTypeDialogForCust) {
        val executePrintAction = { isDetailed: Boolean ->
            val mappedRows = combinedTxList.map { tx ->
                var desc = tx.description
                if (isDetailed && tx.refId > 0 && desc.startsWith("فاتورة")) {
                    val items = allInvoiceItemsState.filter { it.invoiceId == tx.refId }
                    if (items.isNotEmpty()) {
                        desc += "\\n" + items.joinToString("\\n") { "- ${it.productNameAr} (${it.quantity} x ${formatMoney(it.price)})" }
                    }
                }
                com.example.ui.StatementRow(tx.date, desc, tx.debit, tx.credit)
            }
            val summaryText = "إجمالي المبيعات: ${formatMoney(totalDebit)} | إجمالي السداد: ${formatMoney(totalCredit)} | المتبقي المستحق: ${formatMoney(outstandingBalance)}"
            val entityDetails = "رقم الجوال: ${cust.phone} | التصنيف: ${cust.type}"
            val finalTitle = "كشف حساب عميل: ${cust.name}" + if (isDetailed) " (تفصيلي)" else " (مجمل)"
            
            if (pendingPrintActionForCust == "pdf") {
                viewModel.exportStatementToPdf(context, finalTitle, mappedRows, summaryText, entityDetails)
            } else if (pendingPrintActionForCust == "whatsapp") {
                val cleanPhone = cust.phone.replace("+", "").replace(" ", "").filter { it.isDigit() }
                val pdfFile = com.example.ui.PdfGenerator.generateStatementPdfFile(
                    context = context,
                    storeName = viewModel.storeName.value,
                    storePhone = viewModel.storePhone.value,
                    title = finalTitle,
                    bodyRows = mappedRows,
                    summaryText = summaryText,
                    entityDetails = entityDetails
                )
                if (pdfFile != null) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        putExtra(android.content.Intent.EXTRA_TEXT, "مرفق كشف الحساب من ${viewModel.storeName.value}")
                        if (cleanPhone.isNotEmpty()) {
                            putExtra("jid", "$cleanPhone@s.whatsapp.net")
                        }
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        shareIntent.setPackage("com.whatsapp")
                        context.startActivity(shareIntent)
                    } catch (e: Exception) {
                        try {
                            shareIntent.setPackage("com.whatsapp.w4b")
                            context.startActivity(shareIntent)
                        } catch (e2: Exception) {
                            shareIntent.setPackage(null)
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة كشف الحساب"))
                        }
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showPrintTypeDialogForCust = false },
            title = { Text("نوع كشف الحساب", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد إنشاء كشف حساب مجمل أم تفصيلي؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showPrintTypeDialogForCust = false
                        executePrintAction(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) { Text("تفصيلي") }
            },
            dismissButton = {
                Button(onClick = {
                    showPrintTypeDialogForCust = false
                    executePrintAction(false)
                }) { Text("مجمل") }
            }
        )
      }
"""

content = content.replace(end_of_dialog, end_of_dialog + dialog_code, 1)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

