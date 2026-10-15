import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Locate CRMScreen
crm_start = r"  @Composable\s*fun CRMSystemScreen\(viewModel: AppViewModel, onBack: \(\) -> Unit\) \{"

crm_state = """  @Composable
  fun CRMSystemScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val allInvoiceItemsState by viewModel.allInvoiceItems.collectAsState(emptyList())
    var showPrintTypeDialogForCust by remember { mutableStateOf(false) }
    var pendingPrintActionForCust by remember { mutableStateOf("") }
    var showPrintTypeDialogForSup by remember { mutableStateOf(false) }
    var pendingPrintActionForSup by remember { mutableStateOf("") }"""

content = re.sub(crm_start, crm_state, content)

# 1. Update Customer Confirm Button
cust_confirm = r"        confirmButton = \{\s*Row\(\s*modifier = Modifier\.fillMaxWidth\(\),\s*horizontalArrangement = Arrangement\.SpaceBetween,\s*verticalAlignment = Alignment\.CenterVertically\s*\)\s*\{\s*TextButton\(onClick = \{ selectedCustomerForStatement = null \}\) \{ Text\(\"إغلاق\"\) \}\s*Row\(horizontalArrangement = Arrangement\.spacedBy\(8\.dp\)\) \{.*?Text\(\"PDF\", fontSize = 12\.sp\)\s*\}\s*\}\s*\}\s*\}"

new_cust_confirm = """        confirmButton = {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = { selectedCustomerForStatement = null }) { Text("إغلاق") }
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

content = re.sub(cust_confirm, new_cust_confirm, content, flags=re.DOTALL)


# 2. Add Customer PrintTypeDialog
cust_dialog_code = """
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

end_of_cust_dialog = r"          \}\s*\}\s*\)\s*\}\s*// Supplier Statement of Account Dialog"
content = re.sub(end_of_cust_dialog, "          }\n        }\n      )\n" + cust_dialog_code + "    }\n\n      // Supplier Statement of Account Dialog", content)


# 3. Update Supplier Confirm Button
sup_confirm = r"        confirmButton = \{\s*Row\(\s*modifier = Modifier\.fillMaxWidth\(\),\s*horizontalArrangement = Arrangement\.SpaceBetween,\s*verticalAlignment = Alignment\.CenterVertically\s*\)\s*\{\s*TextButton\(onClick = \{ selectedSupplierForStatement = null \}\) \{ Text\(\"إغلاق\"\) \}\s*Row\(horizontalArrangement = Arrangement\.spacedBy\(8\.dp\)\) \{.*?Text\(\"PDF\", fontSize = 12\.sp\)\s*\}\s*\}\s*\}\s*\}"

new_sup_confirm = """        confirmButton = {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(onClick = { selectedSupplierForStatement = null }) { Text("إغلاق") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = {
                    pendingPrintActionForSup = "whatsapp"
                    showPrintTypeDialogForSup = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
              ) {
                Text("واتساب", color = MaterialTheme.colorScheme.onPrimary)
              }
              Button(
                onClick = {
                    pendingPrintActionForSup = "pdf"
                    showPrintTypeDialogForSup = true
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

# Actually, the supplier dialog doesn't currently have the WhatsApp/PDF buttons!
# Let's check what it has currently.

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
