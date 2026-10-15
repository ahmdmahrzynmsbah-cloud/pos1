import sys

def patch():
    with open('app/src/main/java/com/example/data/AppRepository.kt', 'r') as f:
        content = f.read()

    # Find start of checkoutPurchase
    start_idx = content.find('suspend fun checkoutPurchase(')
    end_idx = content.find('return finalInvoice', start_idx) + len('return finalInvoice')
    
    old_block = content[start_idx:end_idx]
    
    new_block = old_block.replace('isPurchase = true\n        )', 'isPurchase = true,\n            isDraft = isDraft\n        )')
    
    new_block = new_block.replace('// Update product stock quantity. Purchase adds to stock. Return subtracts.', 'if (!isDraft) {\n                // Update product stock quantity. Purchase adds to stock. Return subtracts.')
    new_block = new_block.replace('dao.updateProductQuantity(product.id, newQuantity)\n            val updatedProduct = product.copy(quantity = newQuantity)\n            saveFirestoreDocumentAsync("products", product.id.toString(), updatedProduct.toMap())\n        }', 'dao.updateProductQuantity(product.id, newQuantity)\n                val updatedProduct = product.copy(quantity = newQuantity)\n                saveFirestoreDocumentAsync("products", product.id.toString(), updatedProduct.toMap())\n            }\n        }')
    
    new_block = new_block.replace('// 4. Automatic Double-Entry Bookkeeping Ledger Recording', 'if (!isDraft) {\n        // 4. Automatic Double-Entry Bookkeeping Ledger Recording')
    new_block = new_block.replace('logAudit(cashierName, if (isReturn) "مرتجع شراء" else "فاتورة شراء", if (isReturn) "تم حفظ فاتورة مرتجع مشتريات #$invoiceId وخصم المنتجات من المخزن بإجمالي $total للمورد $supplierName" else "تم حفظ فاتورة شراء #$invoiceId وتوريد المنتجات للمخزن بإجمالي $total للمورد $supplierName")', 'logAudit(cashierName, if (isReturn) "مرتجع شراء" else "فاتورة شراء", if (isReturn) "تم حفظ فاتورة مرتجع مشتريات #$invoiceId وخصم المنتجات من المخزن بإجمالي $total للمورد $supplierName" else "تم حفظ فاتورة شراء #$invoiceId وتوريد المنتجات للمخزن بإجمالي $total للمورد $supplierName")\n        }')
    
    content = content.replace(old_block, new_block)
    
    with open('app/src/main/java/com/example/data/AppRepository.kt', 'w') as f:
        f.write(content)

patch()
