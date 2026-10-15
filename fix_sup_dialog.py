import re

with open('app/src/main/java/com/example/ui/AppUI.kt', 'r') as f:
    content = f.read()

# Replace empty confirmButton for Supplier Dialog
sup_dialog_confirm = r"          \}\s*,\s*confirmButton = \{\}\s*\)\s*\}\s*\}\s*\}"

new_sup_confirm = """          },
          confirmButton = {
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
          }
        )
      
        if (showPrintTypeDialogForSup) {
            val executePrintAction = { isDetailed: Boolean ->
                val mappedRows = sortedTxList.map { tx ->
                    var desc = tx.note.ifEmpty { if (tx.transactionType == "شراء") "فاتورة توريد بضاعة" else "سند صرف دفعة مالية" }
                    if (isDetailed && tx.transactionType == "شراء") {
                        val relatedInv = allInvoicesState.find { it.customerId == supplier.id && it.isPurchase && Math.abs(it.total - tx.amount) < 0.1 }
                        if (relatedInv != null) {
                            val items = allInvoiceItemsState.filter { it.invoiceId == relatedInv.id }
                            if (items.isNotEmpty()) {
                                desc += "\\n" + items.joinToString("\\n") { "- ${it.productNameAr} (${it.quantity} x ${formatMoney(it.price)})" }
                            }
                        }
                    }
                    com.example.ui.StatementRow(
                        date = tx.date,
                        description = desc,
                        debit = if (tx.transactionType != "شراء") tx.amount else 0.0,
                        credit = if (tx.transactionType == "شراء") tx.amount else 0.0
                    )
                }
                val summaryText = "إجمالي التوريد (دائن): ${formatMoney(totalPurchases)} | إجمالي المسدد (مدين): ${formatMoney(totalPayments)} | الرصيد المتبقي: ${formatMoney(supplierBalance)}"
                val entityDetails = "الشركة: ${supplier.companyName} | جوال مسؤول الاتصال: ${supplier.phone}"
                val finalTitle = "كشف حساب مورد: ${supplier.name}" + if (isDetailed) " (تفصيلي)" else " (مجمل)"
                
                if (pendingPrintActionForSup == "pdf") {
                    viewModel.exportStatementToPdf(context, finalTitle, mappedRows, summaryText, entityDetails)
                } else if (pendingPrintActionForSup == "whatsapp") {
                    val cleanPhone = supplier.phone.replace("+", "").replace(" ", "").filter { it.isDigit() }
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
                onDismissRequest = { showPrintTypeDialogForSup = false },
                title = { Text("نوع كشف الحساب", fontWeight = FontWeight.Bold) },
                text = { Text("هل تريد إنشاء كشف حساب مجمل أم تفصيلي؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            showPrintTypeDialogForSup = false
                            executePrintAction(true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) { Text("تفصيلي") }
                },
                dismissButton = {
                    Button(onClick = {
                        showPrintTypeDialogForSup = false
                        executePrintAction(false)
                    }) { Text("مجمل") }
                }
            )
        }
      }
    }
  }
"""

content = re.sub(sup_dialog_confirm, new_sup_confirm, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/AppUI.kt', 'w') as f:
    f.write(content)
