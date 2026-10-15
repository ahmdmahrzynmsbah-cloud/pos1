import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
        content = f.read()

    # 1. Update exportInvoiceItemsToPdf
    old_items_sig = "fun exportInvoiceItemsToPdf(context: android.content.Context, invoice: Invoice, items: List<com.example.data.InvoiceItem>, customerName: String?) {"
    new_items_sig = "fun exportInvoiceItemsToPdf(context: android.content.Context, invoice: Invoice, items: List<com.example.data.InvoiceItem>, customerName: String?, isPreview: Boolean = false) {"
    content = content.replace(old_items_sig, new_items_sig)
    
    old_items_call = "exportInvoiceToPdf(context, invoice, cartItems, customerName)"
    new_items_call = "exportInvoiceToPdf(context, invoice, cartItems, customerName, isPreview)"
    content = content.replace(old_items_call, new_items_call)
    
    # 2. Update exportInvoiceToPdf
    old_sig = "fun exportInvoiceToPdf(context: android.content.Context, invoice: Invoice, items: List<CartItem>, customerName: String?) {"
    new_sig = "fun exportInvoiceToPdf(context: android.content.Context, invoice: Invoice, items: List<CartItem>, customerName: String?, isPreview: Boolean = false) {"
    content = content.replace(old_sig, new_sig)
    
    # 3. Update the saving logic in exportInvoiceToPdf
    old_save = """                    }
                }
                
                pdfDocument.close()"""
                
    new_save = """                    }
                }
                
                if (isPreview) {
                    val tempFile = java.io.File(context.cacheDir, "Invoice_Preview.pdf")
                    tempFile.outputStream().use { pdfDocument.writeTo(it) }
                    pdfDocument.close()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        pdfPreviewFile.value = tempFile
                    }
                } else {
                    pdfDocument.close()
                }"""
                
    # We must be careful because old_save might match multiple things. 
    # Let's use a regex or specific replacement for the save block.
    
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
        f.write(content)

patch()
