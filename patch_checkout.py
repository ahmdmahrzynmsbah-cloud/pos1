import sys

def patch():
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'r') as f:
        content = f.read()

    # Patch performCheckout signature
    old_checkout = """    fun performCheckout(
        paymentMethod: String,
        paidAmount: Double,
        changeAmount: Double,
        checkNumber: String? = null,
        checkBankName: String? = null,
        checkDate: Long? = null,
        clearCartAfter: Boolean = true,
        isReturnMode: Boolean = false,
        onSuccess: () -> Unit
    ) {"""
    
    new_checkout = """    fun performCheckout(
        paymentMethod: String,
        paidAmount: Double,
        changeAmount: Double,
        checkNumber: String? = null,
        checkBankName: String? = null,
        checkDate: Long? = null,
        clearCartAfter: Boolean = true,
        isReturnMode: Boolean = false,
        isDraft: Boolean = false,
        onSuccess: () -> Unit
    ) {"""
    
    content = content.replace(old_checkout, new_checkout)

    # Patch performPurchaseCheckout signature
    old_purchase = """    fun performPurchaseCheckout(
        paymentMethod: String = "نقدي",
        paidAmount: Double = 0.0,
        checkNumber: String? = null,
        checkBankName: String? = null,
        checkDate: Long? = null,
        clearCartAfter: Boolean = false,
        isReturnMode: Boolean = false,
        onSuccess: (Invoice) -> Unit = {}
    ) {"""

    new_purchase = """    fun performPurchaseCheckout(
        paymentMethod: String = "نقدي",
        paidAmount: Double = 0.0,
        checkNumber: String? = null,
        checkBankName: String? = null,
        checkDate: Long? = null,
        clearCartAfter: Boolean = false,
        isReturnMode: Boolean = false,
        isDraft: Boolean = false,
        onSuccess: (Invoice) -> Unit = {}
    ) {"""
    
    content = content.replace(old_purchase, new_purchase)
    
    with open('app/src/main/java/com/example/ui/AppViewModel.kt', 'w') as f:
        f.write(content)

patch()
