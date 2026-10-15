import sys

def patch():
    with open('app/src/main/java/com/example/data/AppRepository.kt', 'r') as f:
        content = f.read()

    # Find the problematic block in checkoutPurchase
    old_block = """            // Update product stock quantity. If return, we subtract from stock.
            val newQuantity = (product.quantity + finalQty).coerceAtLeast(0)
            dao.updateProductQuantity(product.id, newQuantity)
                val updatedProduct = product.copy(quantity = newQuantity)
                saveFirestoreDocumentAsync("products", product.id.toString(), updatedProduct.toMap())
            }
        }

        // 3. Record Supplier Transaction
        if (supplierId != null) {
            val tx = SupplierTransaction(
                supplierId = supplierId,
                amount = total,
                transactionType = "شراء",
                note = "فاتورة شراء بضاعة وتوريد للمخزن رقم #$invoiceId"
            )
            val txId = dao.insertSupplierTransaction(tx)
            saveFirestoreDocumentAsync("supplier_transactions", txId.toString(), tx.copy(id = txId.toInt()).toMap())
            dao.updateSupplierBalance(supplierId, total)
        }

        // 4. Ledger Entry
        val entry = LedgerEntry(
            description = "فاتورة شراء بضاعة وتوريد للمخزن رقم #$invoiceId للمورد: $supplierName",
            debitAccount = "المخزون",
            creditAccount = if (paymentMethod == "نقدي") "الصندوق" else "ذمم الموردين",
            amount = total
        )
        dao.insertLedgerEntry(entry)
        logAudit(cashierName, if (isReturn) "مرتجع شراء" else "فاتورة شراء", if (isReturn) "تم حفظ فاتورة مرتجع مشتريات #$invoiceId وخصم المنتجات من المخزن بإجمالي $total للمورد $supplierName" else "تم حفظ فاتورة شراء #$invoiceId وتوريد المنتجات للمخزن بإجمالي $total للمورد $supplierName")
        }
        return finalInvoice"""

    new_block = """            if (!isDraft) {
                // Update product stock quantity. If return, we subtract from stock.
                val newQuantity = (product.quantity + finalQty).coerceAtLeast(0)
                dao.updateProductQuantity(product.id, newQuantity)
                val updatedProduct = product.copy(quantity = newQuantity)
                saveFirestoreDocumentAsync("products", product.id.toString(), updatedProduct.toMap())
            }
        }

        if (!isDraft) {
            // 3. Record Supplier Transaction
            if (supplierId != null) {
                val tx = SupplierTransaction(
                    supplierId = supplierId,
                    amount = total,
                    transactionType = "شراء",
                    note = "فاتورة شراء بضاعة وتوريد للمخزن رقم #$invoiceId"
                )
                val txId = dao.insertSupplierTransaction(tx)
                saveFirestoreDocumentAsync("supplier_transactions", txId.toString(), tx.copy(id = txId.toInt()).toMap())
                dao.updateSupplierBalance(supplierId, total)
            }

            // 4. Ledger Entry
            val entry = LedgerEntry(
                description = "فاتورة شراء بضاعة وتوريد للمخزن رقم #$invoiceId للمورد: $supplierName",
                debitAccount = "المخزون",
                creditAccount = if (paymentMethod == "نقدي") "الصندوق" else "ذمم الموردين",
                amount = total
            )
            dao.insertLedgerEntry(entry)
            logAudit(cashierName, if (isReturn) "مرتجع شراء" else "فاتورة شراء", if (isReturn) "تم حفظ فاتورة مرتجع مشتريات #$invoiceId وخصم المنتجات من المخزن بإجمالي $total للمورد $supplierName" else "تم حفظ فاتورة شراء #$invoiceId وتوريد المنتجات للمخزن بإجمالي $total للمورد $supplierName")
        }
        return finalInvoice"""
        
    content = content.replace(old_block, new_block)
    
    with open('app/src/main/java/com/example/data/AppRepository.kt', 'w') as f:
        f.write(content)

patch()
