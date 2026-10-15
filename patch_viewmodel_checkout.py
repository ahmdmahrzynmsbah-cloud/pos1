import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
        content = f.read()

    # Pass isDraft to repository.checkout
    old_call = """                val checkedOutInvoice = repository.checkout(
                    items = _cart.value,
                    customerId = finalCustomerId,
                    discount = _discount.value,
                    taxPercent = taxPercent.value,
                    paymentMethod = paymentMethod,
                    paidAmount = paidAmount,
                    changeAmount = changeAmount,
                    cashierName = currentUser.value,
                    branchName = currentBranch.value,
                    checkNumber = checkNumber,
                    checkBankName = checkBankName,
                    checkDate = checkDate,
                    isReturn = isReturnMode
                )"""
    new_call = """                val checkedOutInvoice = repository.checkout(
                    items = _cart.value,
                    customerId = finalCustomerId,
                    discount = _discount.value,
                    taxPercent = taxPercent.value,
                    paymentMethod = paymentMethod,
                    paidAmount = paidAmount,
                    changeAmount = changeAmount,
                    cashierName = currentUser.value,
                    branchName = currentBranch.value,
                    checkNumber = checkNumber,
                    checkBankName = checkBankName,
                    checkDate = checkDate,
                    isReturn = isReturnMode,
                    isDraft = isDraft
                )"""
    content = content.replace(old_call, new_call)

    # Pass isDraft to repository.checkoutPurchase
    old_call_purchase = """                val checkedOutInvoice = repository.checkoutPurchase(
                    items = _cart.value,
                    supplierId = finalSupplierId,
                    supplierName = manualNameStr,
                    discount = _discount.value,
                    taxPercent = taxPercent.value,
                    paymentMethod = paymentMethod,
                    paidAmount = paidAmount,
                    cashierName = currentUser.value,
                    branchName = currentBranch.value,
                    checkNumber = checkNumber,
                    checkBankName = checkBankName,
                    checkDate = checkDate,
                    isReturn = isReturnMode
                )"""
    new_call_purchase = """                val checkedOutInvoice = repository.checkoutPurchase(
                    items = _cart.value,
                    supplierId = finalSupplierId,
                    supplierName = manualNameStr,
                    discount = _discount.value,
                    taxPercent = taxPercent.value,
                    paymentMethod = paymentMethod,
                    paidAmount = paidAmount,
                    cashierName = currentUser.value,
                    branchName = currentBranch.value,
                    checkNumber = checkNumber,
                    checkBankName = checkBankName,
                    checkDate = checkDate,
                    isReturn = isReturnMode,
                    isDraft = isDraft
                )"""
    content = content.replace(old_call_purchase, new_call_purchase)

    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
        f.write(content)

patch()
