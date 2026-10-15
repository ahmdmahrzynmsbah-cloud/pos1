import sys

def patch():
    with open('app/src/main/java/com/example/ui/InvoiceFormScreen.kt', 'r') as f:
        content = f.read()

    # Find the place where performPurchaseCheckout is called
    old_call_purc = """            viewModel.performPurchaseCheckout(
                paymentMethod = paymentMethod,
                paidAmount = total,
                checkNumber = chkNum,
                checkBankName = chkBank,
                checkDate = chkDate,
                clearCartAfter = false,
                isReturnMode = isReturnMode,
                onSuccess = { createdInvoice ->
                    lastSavedInvoice = createdInvoice
                    lastSavedItems = cart.toList()
                    viewModel.showToast("تم حفظ فاتورة الشراء وتوريد البضاعة للمخزن بنجاح وتظهر الآن في الفواتير السابقة! 💾")
                }
            )"""
    new_call_purc = """            viewModel.performPurchaseCheckout(
                paymentMethod = paymentMethod,
                paidAmount = total,
                checkNumber = chkNum,
                checkBankName = chkBank,
                checkDate = chkDate,
                clearCartAfter = false,
                isReturnMode = isReturnMode,
                isDraft = isDraftMode,
                onSuccess = { createdInvoice ->
                    lastSavedInvoice = createdInvoice
                    lastSavedItems = cart.toList()
                    val successMsg = if (isDraftMode) "تم حفظ مسودة الشراء بنجاح 📝" else "تم حفظ فاتورة الشراء وتوريد البضاعة للمخزن بنجاح وتظهر الآن في الفواتير السابقة! 💾"
                    viewModel.showToast(successMsg)
                }
            )"""
    content = content.replace(old_call_purc, new_call_purc)

    # Find where performCheckout is called
    old_call_sale = """            viewModel.performCheckout(
                paymentMethod = paymentMethod,
                paidAmount = total,
                changeAmount = 0.0,
                checkNumber = chkNum,
                checkBankName = chkBank,
                checkDate = chkDate,
                clearCartAfter = false, // Do NOT clear cart automatically!
                isReturnMode = isReturnMode,
                onSuccess = {
                    lastSavedInvoice = createdInvoice
                    lastSavedItems = cart.toList()
                    viewModel.showToast("تم حفظ الفاتورة بنجاح وتظهر الآن في قائمة الفواتير السابقة! 💾")
                }
            )"""
    new_call_sale = """            viewModel.performCheckout(
                paymentMethod = paymentMethod,
                paidAmount = total,
                changeAmount = 0.0,
                checkNumber = chkNum,
                checkBankName = chkBank,
                checkDate = chkDate,
                clearCartAfter = false, // Do NOT clear cart automatically!
                isReturnMode = isReturnMode,
                isDraft = isDraftMode,
                onSuccess = {
                    lastSavedInvoice = createdInvoice
                    lastSavedItems = cart.toList()
                    val successMsg = if (isDraftMode) "تم حفظ مسودة المبيع بنجاح 📝" else "تم حفظ الفاتورة بنجاح وتظهر الآن في قائمة الفواتير السابقة! 💾"
                    viewModel.showToast(successMsg)
                }
            )"""
    content = content.replace(old_call_sale, new_call_sale)
    
    with open('app/src/main/java/com/example/ui/InvoiceFormScreen.kt', 'w') as f:
        f.write(content)

patch()
