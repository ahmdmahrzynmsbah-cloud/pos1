import re

with open("app/src/main/java/com/example/ui/AppViewModel.kt", "r") as f:
    content = f.read()

content = content.replace(
"""    fun performCheckout(
        paymentMethod: String,
        paidAmount: Double,
        changeAmount: Double,
        checkNumber: String? = null,
        checkBankName: String? = null,
        checkDate: Long? = null,
        clearCartAfter: Boolean = true,
        onSuccess: () -> Unit
    ) {""",
"""    fun performCheckout(
        paymentMethod: String,
        paidAmount: Double,
        changeAmount: Double,
        checkNumber: String? = null,
        checkBankName: String? = null,
        checkDate: Long? = null,
        clearCartAfter: Boolean = true,
        isReturnMode: Boolean = false,
        onSuccess: () -> Unit
    ) {""")

content = content.replace(
"""                val checkedOutInvoice = repository.checkout(
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
                    checkDate = checkDate
                )""",
"""                val checkedOutInvoice = repository.checkout(
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
                )""")

content = content.replace(
"""    fun performPurchaseCheckout(
        paymentMethod: String = "نقدي",
        paidAmount: Double = 0.0,
        checkNumber: String? = null,
        checkBankName: String? = null,
        checkDate: Long? = null,
        clearCartAfter: Boolean = true,
        onSuccess: (com.example.data.Invoice) -> Unit
    ) {""",
"""    fun performPurchaseCheckout(
        paymentMethod: String = "نقدي",
        paidAmount: Double = 0.0,
        checkNumber: String? = null,
        checkBankName: String? = null,
        checkDate: Long? = null,
        clearCartAfter: Boolean = true,
        isReturnMode: Boolean = false,
        onSuccess: (com.example.data.Invoice) -> Unit
    ) {""")

content = content.replace(
"""                val checkedOutInvoice = repository.checkoutPurchase(
                    items = _cart.value,
                    supplierId = finalSupplierId,
                    supplierName = finalSupplierName,
                    discount = _discount.value,
                    taxPercent = taxPercent.value,
                    paymentMethod = paymentMethod,
                    paidAmount = paidAmount,
                    cashierName = currentUser.value,
                    branchName = currentBranch.value,
                    checkNumber = checkNumber,
                    checkBankName = checkBankName,
                    checkDate = checkDate
                )""",
"""                val checkedOutInvoice = repository.checkoutPurchase(
                    items = _cart.value,
                    supplierId = finalSupplierId,
                    supplierName = finalSupplierName,
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
                )""")


with open("app/src/main/java/com/example/ui/AppViewModel.kt", "w") as f:
    f.write(content)
