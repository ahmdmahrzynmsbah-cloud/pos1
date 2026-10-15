import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Remove the wrongly injected block
wrong_injection_pattern = r"      if \(showPrintTypeDialogForCust\).*?\} // Customer Account Statement Dialog"
content = re.sub(wrong_injection_pattern, "", content, flags=re.DOTALL)

# Now, we need to inject the dialog code properly. 
# Inside `showStatementForCust?.let { cust ->`, right after the `AlertDialog( ... confirmButton = { ... } )`
# Let's find the end of that AlertDialog.
# The `confirmButton` of this AlertDialog is the one we replaced!

pattern_confirm_button = r"        confirmButton = \{\s*Row\(\s*modifier = Modifier\.fillMaxWidth\(\),\s*horizontalArrangement = Arrangement\.SpaceBetween,\s*verticalAlignment = Alignment\.CenterVertically\s*\)\s*\{\s*TextButton\(onClick = \{ showStatementForCust = null \}\) \{ Text\(\"إغلاق\"\) \}\s*Row\(horizontalArrangement = Arrangement\.spacedBy\(8\.dp\)\) \{.*?Text\(\"PDF\", fontSize = 12\.sp\)\s*\}\s*\}\s*\}\s*\}\s*\)\s*\}"

replacement = """        confirmButton = {
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
        }
      )

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
    } // End of showStatementForCust Let
"""

content = re.sub(pattern_confirm_button, replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)

