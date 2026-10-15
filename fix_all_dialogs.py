import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# 1. Fix CashierPOSUI Customer statement (showStatementForCust)
# Find the end of `showStatementForCust?.let { cust -> ... }` which is followed by `// --- Bluetooth Thermal Printer Dialog ---`
# It might be `} // End of showStatementForCust Let`

cust_dialog_cashier = """      if (showPrintTypeDialogForCust) {
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

if 'if (showPrintTypeDialogForCust)' not in content[:5000]:  # Only insert if missing
    content = content.replace("    } // End of showStatementForCust Let", cust_dialog_cashier + "    } // End of showStatementForCust Let")


# 2. Fix CrmScreen Customer statement (selectedCustomerForStatement)
# It uses `customer` instead of `cust`, and `showPrintTypeDialogForCust`
cust_dialog_crm = """      if (showPrintTypeDialogForCust) {
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
            val entityDetails = "رقم الجوال: ${customer.phone} | التصنيف: ${customer.type}"
            val finalTitle = "كشف حساب عميل: ${customer.name}" + if (isDetailed) " (تفصيلي)" else " (مجمل)"
            
            if (pendingPrintActionForCust == "pdf") {
                viewModel.exportStatementToPdf(context, finalTitle, mappedRows, summaryText, entityDetails)
            } else if (pendingPrintActionForCust == "whatsapp") {
                val cleanPhone = customer.phone.replace("+", "").replace(" ", "").filter { it.isDigit() }
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
if 'if (showPrintTypeDialogForCust)' not in content[8000:]: # insert only if missing
    content = content.replace("      // Supplier Statement of Account Dialog", cust_dialog_crm + "      // Supplier Statement of Account Dialog")


with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
