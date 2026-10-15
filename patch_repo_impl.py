import sys

def patch():
    with open('app/src/main/java/com/example/data/AppRepository.kt', 'r') as f:
        content = f.read()

    # 1. Patch checkout
    start_idx = content.find('suspend fun checkout(')
    end_idx = content.find('return invoice.copy(id = invoiceId)', start_idx) + len('return invoice.copy(id = invoiceId)')
    
    old_checkout = content[start_idx:end_idx]
    
    # We will replace parts of it.
    new_checkout = old_checkout.replace('isPurchase = false\n        )', 'isPurchase = false,\n            isDraft = isDraft\n        )')
    
    new_checkout = new_checkout.replace('// Update product stock quantity. If return, we add back to stock.', 'if (!isDraft) {\n                // Update product stock quantity. If return, we add back to stock.')
    new_checkout = new_checkout.replace('totalCostOfGoodsSold += product.pricePurchase * cartItem.quantity\n        }', 'totalCostOfGoodsSold += product.pricePurchase * cartItem.quantity\n            }\n        }')
    
    new_checkout = new_checkout.replace('// 3. Loyalty points processing (1 point per 10 Shekels spent)', 'if (!isDraft) {\n        // 3. Loyalty points processing (1 point per 10 Shekels spent)')
    new_checkout = new_checkout.replace('logAudit(cashierName, "إتمام بيع", "تم ${if (isReturn) "إرجاع" else "بيع"} فاتورة #$invoiceId بإجمالي $total شيكل بطريقة دفع $paymentMethod")', 'logAudit(cashierName, "إتمام بيع", "تم ${if (isReturn) "إرجاع" else "بيع"} فاتورة #$invoiceId بإجمالي $total شيكل بطريقة دفع $paymentMethod")\n        }')
    
    content = content.replace(old_checkout, new_checkout)
    
    with open('app/src/main/java/com/example/data/AppRepository.kt', 'w') as f:
        f.write(content)

patch()
