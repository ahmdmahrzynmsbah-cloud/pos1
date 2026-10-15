import sys

def patch():
    with open('app/src/main/java/com/example/data/AppRepository.kt', 'r') as f:
        content = f.read()

    # Patch checkout signature
    old_checkout = """    suspend fun checkout(
        items: List<CartItem>,
        customerId: Int?,
        discount: Double,
        taxPercent: Double,
        paymentMethod: String,
        paidAmount: Double,
        changeAmount: Double,
        cashierName: String,
        branchName: String,
        checkNumber: String? = null,
        checkBankName: String? = null,
        checkDate: Long? = null,
        isReturn: Boolean = false
    ): Invoice {"""
    
    new_checkout = """    suspend fun checkout(
        items: List<CartItem>,
        customerId: Int?,
        discount: Double,
        taxPercent: Double,
        paymentMethod: String,
        paidAmount: Double,
        changeAmount: Double,
        cashierName: String,
        branchName: String,
        checkNumber: String? = null,
        checkBankName: String? = null,
        checkDate: Long? = null,
        isReturn: Boolean = false,
        isDraft: Boolean = false
    ): Invoice {"""
    
    content = content.replace(old_checkout, new_checkout)

    # Patch checkoutPurchase signature
    old_purchase = """    suspend fun checkoutPurchase(
        items: List<CartItem>,
        supplierId: Int?,
        supplierName: String,
        discount: Double,
        taxPercent: Double,
        paymentMethod: String,
        paidAmount: Double,
        cashierName: String,
        branchName: String,
        checkNumber: String? = null,
        checkBankName: String? = null,
        checkDate: Long? = null,
        isReturn: Boolean = false
    ): Invoice {"""

    new_purchase = """    suspend fun checkoutPurchase(
        items: List<CartItem>,
        supplierId: Int?,
        supplierName: String,
        discount: Double,
        taxPercent: Double,
        paymentMethod: String,
        paidAmount: Double,
        cashierName: String,
        branchName: String,
        checkNumber: String? = null,
        checkBankName: String? = null,
        checkDate: Long? = null,
        isReturn: Boolean = false,
        isDraft: Boolean = false
    ): Invoice {"""
    
    content = content.replace(old_purchase, new_purchase)
    
    with open('app/src/main/java/com/example/data/AppRepository.kt', 'w') as f:
        f.write(content)

patch()
