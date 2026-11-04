package com.example.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class AppRepository(val dao: AppDao, private val context: android.content.Context? = null) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun getFirebaseConfig(): Triple<String, String, String>? {
        if (context == null) return null
        val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val proj = prefs.getString("firebase_project_id", "") ?: ""
        val key = prefs.getString("firebase_web_api_key", "") ?: ""
        val url = prefs.getString("firebase_database_url", "") ?: ""
        if (FirebaseSyncManager.isPlaceholderConfig(proj, key)) return null
        return Triple(proj, key, url)
    }

    private fun saveFirestoreDocumentAsync(collection: String, docId: String, data: Map<String, Any?>) {
        repositoryScope.launch {
            try {
                val config = getFirebaseConfig() ?: return@launch
                val idToken = context?.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)?.getString("firebase_id_token", null)
                FirebaseSyncManager.saveDocument(config.first, config.second, collection, docId, data, idToken)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w("FirestoreSync", "Could not auto-save document $docId in collection $collection to Firestore: ${e.message}")
            }
        }
    }

    private fun deleteFirestoreDocumentAsync(collection: String, docId: String) {
        repositoryScope.launch {
            try {
                val config = getFirebaseConfig() ?: return@launch
                val idToken = context?.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)?.getString("firebase_id_token", null)
                FirebaseSyncManager.deleteDocument(config.first, config.second, collection, docId, idToken)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w("FirestoreSync", "Could not auto-delete document $docId in collection $collection from Firestore: ${e.message}")
            }
        }
    }

    
    val allProducts: Flow<List<Product>> = dao.getAllProducts()
    val allCategories: Flow<List<Category>> = dao.getAllCategories()
    val allInvoices: Flow<List<Invoice>> = dao.getAllInvoices()
    val allInvoiceItems: Flow<List<InvoiceItem>> = dao.getAllInvoiceItems()
    fun getInvoiceItemsForInvoice(invoiceId: Int): Flow<List<InvoiceItem>> = dao.getInvoiceItemsForInvoice(invoiceId)
    fun getInvoiceItemsForProduct(productId: Int): Flow<List<InvoiceItem>> = dao.getInvoiceItemsForProduct(productId)
    
    fun getProductSalesHistory(productId: Int): Flow<List<ProductSaleHistory>> = dao.getProductSalesHistory(productId)

    val allCustomers: Flow<List<Customer>> = dao.getAllCustomers()
    val allBanks: Flow<List<Bank>> = dao.getAllBanks()
    
    suspend fun insertBank(bank: Bank) = dao.insertBank(bank)
    suspend fun deleteBank(bank: Bank) = dao.deleteBank(bank)
    suspend fun getBankByCode(code: String): Bank? = dao.getBankByCode(code)

    val allLedgerEntries: Flow<List<LedgerEntry>> = dao.getAllLedgerEntries()
    val allShifts: Flow<List<Shift>> = dao.getAllShifts()
    val activeShift: Flow<Shift?> = dao.getActiveShift()
    val allAuditLogs: Flow<List<AuditLog>> = dao.getAllAuditLogs()

    fun getAuditLogsForProduct(productName: String): Flow<List<AuditLog>> = dao.getAuditLogsForProduct(productName)

    suspend fun insertProduct(product: Product, supplier: Supplier? = null, quantityAdded: Int = 0, currentUser: String = "النظام") {
        val rowId = dao.insertProduct(product)
        val finalId = if (product.id == 0) rowId.toInt() else product.id
        val finalProduct = product.copy(id = finalId)
        
        if (supplier != null && quantityAdded > 0) {
            val totalCost = finalProduct.pricePurchase * quantityAdded
            val tx = SupplierTransaction(
                supplierId = supplier.id,
                date = System.currentTimeMillis(),
                transactionType = "شراء",
                amount = totalCost,
                note = "شراء ${quantityAdded} ${finalProduct.unit} من ${finalProduct.nameAr}"
            )
            val txId = dao.insertSupplierTransaction(tx)
            dao.updateSupplierBalance(supplier.id, totalCost)
        }
        
        val details = if (quantityAdded > 0) {
            "IMPORT|${finalProduct.nameAr}|${supplier?.name ?: "غير محدد"}|${finalProduct.pricePurchase}|${finalProduct.priceSale}|${quantityAdded}"
        } else {
            "تم إضافة منتج جديد: ${finalProduct.nameAr} بسعر بيع ${finalProduct.priceSale}"
        }
        logAudit(currentUser, "إضافة منتج", details)
    }

    suspend fun updateProduct(product: Product, supplier: Supplier? = null, quantityAdded: Int = 0, currentUser: String = "النظام") {
        dao.updateProduct(product)
        
        if (supplier != null && quantityAdded > 0) {
            val totalCost = product.pricePurchase * quantityAdded
            val tx = SupplierTransaction(
                supplierId = supplier.id,
                date = System.currentTimeMillis(),
                transactionType = "شراء",
                amount = totalCost,
                note = "شراء ${quantityAdded} ${product.unit} من ${product.nameAr}"
            )
            val txId = dao.insertSupplierTransaction(tx)
            dao.updateSupplierBalance(supplier.id, totalCost)
        }
        
        val details = if (quantityAdded > 0) {
            "IMPORT|${product.nameAr}|${supplier?.name ?: "غير محدد"}|${product.pricePurchase}|${product.priceSale}|${quantityAdded}"
        } else {
            "تم تعديل المنتج: ${product.nameAr}"
        }
        logAudit(currentUser, "تعديل منتج", details)
    }

    suspend fun deleteProduct(product: Product, currentUser: String = "النظام") {
        dao.deleteProduct(product)
        logAudit(currentUser, "حذف منتج", "تم حذف المنتج: ${product.nameAr}")
    }

    suspend fun insertCategory(category: Category, currentUser: String = "النظام") {
        dao.insertCategory(category)
        logAudit(currentUser, "إضافة تصنيف", "تم إضافة تصنيف جديد: ${category.nameAr}")
    }

    suspend fun deleteCategory(category: Category, currentUser: String = "النظام") {
        dao.deleteCategory(category)
        logAudit(currentUser, "حذف تصنيف", "تم حذف التصنيف: ${category.nameAr}")
    }

    suspend fun insertCustomer(customer: Customer, currentUser: String = "النظام"): Long {
        val id = dao.insertCustomer(customer)
        
        logAudit(currentUser, "إضافة عميل", "تم تسجيل عميل جديد: ${customer.name}")
        return id
    }

    suspend fun deleteCustomer(customer: Customer, currentUser: String = "النظام") {
        dao.deleteCustomer(customer)
        logAudit(currentUser, "حذف عميل", "تم حذف العميل: ${customer.name}")
    }

    suspend fun insertInvoice(invoice: Invoice): Long {
        val id = dao.insertInvoice(invoice)
        saveFirestoreDocumentAsync("invoices", id.toString(), mapOf(
            "id" to id,
            "invoiceDate" to invoice.invoiceDate,
            "customerId" to invoice.customerId,
            "total" to invoice.total,
            "paymentMethod" to invoice.paymentMethod,
            "checkNumber" to invoice.checkNumber,
            "checkBankName" to invoice.checkBankName,
            "checkDate" to invoice.checkDate,
            "checkStatus" to invoice.checkStatus
        ))
        return id
    }

    suspend fun deleteInvoice(invoice: Invoice, currentUser: String = "النظام") {
        val items = dao.getInvoiceItemsListForInvoice(invoice.id)
        if (!invoice.isDraft) {
            items.forEach { item ->
                val product = dao.getProductById(item.productId)
                if (product != null) {
                    val newQty = if (invoice.isPurchase) {
                        (product.quantity - item.quantity).coerceAtLeast(0)
                    } else {
                        product.quantity + item.quantity
                    }
                    dao.updateProductQuantity(product.id, newQty)
                }
            }
        }
        dao.deleteInvoiceItemsForInvoice(invoice.id)
        dao.deleteInvoiceById(invoice.id)
        deleteFirestoreDocumentAsync("invoices", invoice.id.toString())
        logAudit(currentUser, "حذف فاتورة", "تم حذف الفاتورة رقم #${invoice.id}")
    }

    // --- POS Checkout & Accounting Transaction Engine ---
    suspend fun checkout(
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
        isDraft: Boolean = false,
        customInvoiceDate: Long? = null,
        isPartySupplier: Boolean = false
    ): Invoice {
        val sign = if (isReturn) -1.0 else 1.0
        val baseSubtotal = items.sumOf { it.product.priceSale * it.quantity }
        val subtotal = baseSubtotal * sign
        val discountedSubtotal = (baseSubtotal - discount) * sign
        val taxAmount = discountedSubtotal * (taxPercent / 100)
        val total = discountedSubtotal + taxAmount
        val finalPaidAmount = paidAmount * sign
        val finalChangeAmount = changeAmount * sign
        val targetDate = customInvoiceDate ?: System.currentTimeMillis()

        // 1. Save Invoice
        val invoice = Invoice(
            customerId = customerId,
            invoiceDate = targetDate,
            subtotal = subtotal,
            discount = discount,
            taxAmount = taxAmount,
            total = total,
            paymentMethod = paymentMethod,
            paidAmount = finalPaidAmount,
            changeAmount = finalChangeAmount,
            cashierName = cashierName,
            branchName = branchName,
            checkNumber = checkNumber,
            checkBankName = checkBankName,
            checkDate = checkDate,
            checkStatus = if (paymentMethod == "شيك") "قيد الانتظار" else null,
            isPurchase = false,
            isDraft = isDraft
        )
        val invoiceId = dao.insertInvoice(invoice).toInt()
        val finalInvoice = invoice.copy(id = invoiceId)

        // 2. Save Invoice Items & Update Inventory quantities
        var totalCostOfGoodsSold = 0.0
        items.forEach { cartItem ->
            val product = cartItem.product
            val signQty = if (isReturn) -1 else 1
            val finalQty = cartItem.quantity * signQty
            val itemTotal = product.priceSale * finalQty
            val invoiceItem = InvoiceItem(
                invoiceId = invoiceId,
                productId = product.id,
                productNameAr = product.nameAr,
                productNameEn = product.nameEn,
                price = product.priceSale,
                quantity = finalQty,
                total = itemTotal
            )
            dao.insertInvoiceItem(invoiceItem)

            if (!isDraft) {
                // Update product stock quantity. If return, we add back to stock.
            val newQuantity = (product.quantity - finalQty).coerceAtLeast(0)
            dao.updateProductQuantity(product.id, newQuantity)
            val updatedProduct = product.copy(quantity = newQuantity)

            totalCostOfGoodsSold += product.pricePurchase * cartItem.quantity
            }
        }

        if (!isDraft) {
        // 3. Loyalty points processing or Supplier debt recording when selling to a supplier
        if (customerId != null && !isDraft) {
            if (isPartySupplier) {
                val absTotal = kotlin.math.abs(total)
                val supTx = SupplierTransaction(
                    supplierId = customerId,
                    amount = absTotal,
                    transactionType = if (isReturn) "مرتجع_مبيعات" else "بيع",
                    date = targetDate,
                    note = if (isReturn) "فاتورة مرتجع مبيعات للمورد رقم #$invoiceId" else "فاتورة مبيعات بضاعة للمورد رقم #$invoiceId"
                )
                dao.insertSupplierTransaction(supTx)
                val balChange = if (isReturn) absTotal else -absTotal
                dao.updateSupplierBalance(customerId, balChange)
            } else if (!isReturn) {
                val pointsToAdd = (total / 10.0).toInt()
                if (pointsToAdd > 0) {
                    dao.addCustomerPoints(customerId, pointsToAdd)
                }
            }
        }

        // 4. Automatic Double-Entry Bookkeeping Ledger Recording
        val mainDebitAccount = when (paymentMethod) {
            "نقدي" -> "الصندوق"
            "شيك" -> "شيكات تحت التحصيل"
            else -> "البنك"
        }
        
        // Journal entry A: Record Sales Revenue, Tax Payable, and Cash/Bank debit
        val absDiscounted = kotlin.math.abs(discountedSubtotal)
        val absTax = kotlin.math.abs(taxAmount)
        dao.insertLedgerEntry(
            LedgerEntry(
                date = targetDate,
                description = "${if (isReturn) "مرتجع مبيعات" else "مبيعات"} فاتورة رقم #$invoiceId",
                debitAccount = if (isReturn) "مردودات ومسموحات المبيعات" else mainDebitAccount,
                creditAccount = if (isReturn) mainDebitAccount else "إيرادات المبيعات",
                amount = absDiscounted
            )
        )
        if (absTax > 0) {
            dao.insertLedgerEntry(
                LedgerEntry(
                    date = targetDate,
                    description = "ضريبة القيمة المضافة ${if (isReturn) "مرتجع مبيعات" else ""} فاتورة رقم #$invoiceId",
                    debitAccount = if (isReturn) "ضريبة القيمة المضافة المستحقة" else mainDebitAccount,
                    creditAccount = if (isReturn) mainDebitAccount else "ضريبة القيمة المضافة المستحقة",
                    amount = absTax
                )
            )
        }

        // Journal entry B: Record Cost of Goods Sold and Inventory Credit
        if (totalCostOfGoodsSold > 0) {
            dao.insertLedgerEntry(
                LedgerEntry(
                    date = targetDate,
                    description = "تكلفة بضاعة مباعة ${if (isReturn) "مرتجع" else ""} فاتورة رقم #$invoiceId",
                    debitAccount = if (isReturn) "المخزون" else "تكلفة البضاعة المباعة",
                    creditAccount = if (isReturn) "تكلفة البضاعة المباعة" else "المخزون",
                    amount = totalCostOfGoodsSold
                )
            )
        }

        // 5. Audit log
        val absTotal = kotlin.math.abs(total)
        logAudit(
            cashierName,
            if (isReturn) "مرتجع مبيعات" else "إتمام بيع",
            "تم ${if (isReturn) "إرجاع" else "بيع"} فاتورة #$invoiceId بإجمالي $absTotal شيكل بطريقة دفع $paymentMethod"
        )
        }

        return invoice.copy(id = invoiceId)
    }

    // --- POS Purchase Checkout & Inventory Stock In Engine ---
    suspend fun checkoutPurchase(
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
        isDraft: Boolean = false,
        customInvoiceDate: Long? = null,
        isPartyCustomer: Boolean = false
    ): Invoice {
        val sign = if (isReturn) -1.0 else 1.0
        val baseSubtotal = items.sumOf { item ->
            val price = if (item.product.pricePurchase > 0) item.product.pricePurchase else item.product.priceSale
            price * item.quantity
        }
        val subtotal = baseSubtotal * sign
        val discountedSubtotal = (baseSubtotal - discount).coerceAtLeast(0.0) * sign
        val taxAmount = discountedSubtotal * (taxPercent / 100)
        val total = discountedSubtotal + taxAmount
        val finalPaidAmount = paidAmount * sign
        val targetDate = customInvoiceDate ?: System.currentTimeMillis()

        // 1. Save Purchase Invoice
        val invoice = Invoice(
            customerId = supplierId,
            invoiceDate = targetDate,
            subtotal = subtotal,
            discount = discount,
            taxAmount = taxAmount,
            total = total,
            paymentMethod = paymentMethod,
            paidAmount = finalPaidAmount,
            changeAmount = 0.0,
            cashierName = cashierName,
            branchName = branchName,
            checkNumber = checkNumber,
            checkBankName = checkBankName,
            checkDate = checkDate,
            checkStatus = if (paymentMethod == "شيك") "قيد الانتظار" else null,
            isPurchase = true,
            isDraft = isDraft
        )
        val invoiceId = dao.insertInvoice(invoice).toInt()
        val finalInvoice = invoice.copy(id = invoiceId)

        // 2. Save Invoice Items & INCREASE Inventory Stock (Stock In)
        items.forEach { cartItem ->
            val product = cartItem.product
            val itemPrice = if (product.pricePurchase > 0) product.pricePurchase else product.priceSale
            val signQty = if (isReturn) -1 else 1
            val finalQty = cartItem.quantity * signQty
            val itemTotal = itemPrice * finalQty
            val invoiceItem = InvoiceItem(
                invoiceId = invoiceId,
                productId = product.id,
                productNameAr = product.nameAr,
                productNameEn = product.nameEn,
                price = itemPrice,
                quantity = finalQty,
                total = itemTotal
            )
            dao.insertInvoiceItem(invoiceItem)

            if (!isDraft) {
                // Update product stock quantity. If return, we subtract from stock.
                val newQuantity = (product.quantity + finalQty).coerceAtLeast(0)
                dao.updateProductQuantity(product.id, newQuantity)
                val updatedProduct = product.copy(quantity = newQuantity)
            }
        }

        if (!isDraft) {
            val absTotal = kotlin.math.abs(total)
            // 3. Record Party Transaction (Customer or Supplier)
            if (supplierId != null) {
                if (isPartyCustomer) {
                    val custTx = CustomerTransaction(
                        customerId = supplierId,
                        amount = absTotal,
                        transactionType = if (isReturn) "مرتجع_شراء" else "شراء",
                        invoiceId = invoiceId,
                        date = targetDate,
                        note = if (isReturn) "فاتورة مرتجع مشتريات من العميل رقم #$invoiceId" else "فاتورة شراء بضاعة وتوريد للمخزن من العميل رقم #$invoiceId"
                    )
                    dao.insertCustomerTransaction(custTx)
                } else {
                    val tx = SupplierTransaction(
                        supplierId = supplierId,
                        amount = absTotal,
                        transactionType = if (isReturn) "مرتجع" else "شراء",
                        date = targetDate,
                        note = if (isReturn) "فاتورة مرتجع مشتريات رقم #$invoiceId" else "فاتورة شراء بضاعة وتوريد للمخزن رقم #$invoiceId"
                    )
                    dao.insertSupplierTransaction(tx)
                    val balChange = if (isReturn) -absTotal else total
                    dao.updateSupplierBalance(supplierId, balChange)
                }
            }

            // 4. Ledger Entry
            val partyLabel = if (isPartyCustomer) "للعميل" else "للمورد"
            val debitPartyAcc = if (isPartyCustomer) "ذمم العملاء" else "ذمم الموردين"
            val entry = LedgerEntry(
                date = targetDate,
                description = "${if (isReturn) "فاتورة مرتجع مشتريات" else "فاتورة شراء بضاعة وتوريد للمخزن"} رقم #$invoiceId $partyLabel: $supplierName",
                debitAccount = if (isReturn) (if (paymentMethod == "نقدي") "الصندوق" else debitPartyAcc) else "المخزون",
                creditAccount = if (isReturn) "المخزون" else (if (paymentMethod == "نقدي") "الصندوق" else debitPartyAcc),
                amount = absTotal
            )
            dao.insertLedgerEntry(entry)
            logAudit(cashierName, if (isReturn) "مرتجع شراء" else "فاتورة شراء", if (isReturn) "تم حفظ فاتورة مرتجع مشتريات #$invoiceId وخصم المنتجات من المخزن بإجمالي $absTotal $partyLabel $supplierName" else "تم حفظ فاتورة شراء #$invoiceId وتوريد المنتجات للمخزن بإجمالي $total $partyLabel $supplierName")
        }

        return finalInvoice
    }

    suspend fun toggleInvoiceReturnType(invoiceId: Int, currentUser: String): Invoice? {
        val invoice = dao.getInvoiceById(invoiceId) ?: return null
        val currentlyReturn = invoice.total < 0 || invoice.subtotal < 0
        val targetSign = if (currentlyReturn) 1.0 else -1.0
        val willBeReturn = !currentlyReturn

        val newSubtotal = kotlin.math.abs(invoice.subtotal) * targetSign
        val newTaxAmount = kotlin.math.abs(invoice.taxAmount) * targetSign
        val newTotal = kotlin.math.abs(invoice.total) * targetSign
        val newPaidAmount = kotlin.math.abs(invoice.paidAmount) * targetSign

        val updatedInvoice = invoice.copy(
            subtotal = newSubtotal,
            taxAmount = newTaxAmount,
            total = newTotal,
            paidAmount = newPaidAmount
        )
        dao.updateInvoice(updatedInvoice)

        // Adjust items and product stock
        val items = dao.getInvoiceItemsListForInvoice(invoiceId)
        items.forEach { item ->
            val signQty = if (willBeReturn) -1 else 1
            val absQty = kotlin.math.abs(item.quantity)
            val updatedQty = absQty * signQty
            val updatedTotal = item.price * updatedQty
            val updatedItem = item.copy(quantity = updatedQty, total = updatedTotal)
            dao.insertInvoiceItem(updatedItem)

            val product = dao.getProductById(item.productId)
            if (product != null) {
                // When switching to return: sales return adds items back to stock; purchase return removes items from stock
                val stockDelta = if (invoice.isPurchase) -absQty else absQty
                val newStock = if (willBeReturn) {
                    (product.quantity + stockDelta).coerceAtLeast(0)
                } else {
                    (product.quantity - stockDelta).coerceAtLeast(0)
                }
                dao.updateProductQuantity(product.id, newStock)
            }
        }

        val typeStr = if (willBeReturn) "مرتجع" else "عادية"
        logAudit(
            currentUser,
            "تعديل تصنيف فاتورة",
            "تم تحويل تصنيف الفاتورة #${invoiceId} إلى فاتورة $typeStr بقيمة ${kotlin.math.abs(newTotal)} شيكل"
        )
        return updatedInvoice
    }

    suspend fun updateCheckStatus(invoiceId: Int, newStatus: String, currentUser: String) {
        val invoice = dao.getInvoiceById(invoiceId) ?: return
        val oldStatus = invoice.checkStatus
        if (oldStatus == newStatus) return
        
        val updatedInvoice = invoice.copy(checkStatus = newStatus)
        dao.updateInvoice(updatedInvoice)
        
        logAudit(currentUser, "تحديث حالة شيك", "تم تحديث حالة الشيك رقم ${invoice.checkNumber} من '$oldStatus' إلى '$newStatus'")
        
        if (newStatus == "تم التحصيل" && oldStatus != "تم التحصيل") {
            // Update treasury (Bank or Cash)
            dao.insertLedgerEntry(
                LedgerEntry(
                    description = "تحصيل شيك رقم ${invoice.checkNumber} للفاتورة #${invoice.id}",
                    debitAccount = "الصندوق", // as requested "تحديث رصيد الخزينة تلقائياً" (treasury usually means Cash, but could be Bank. We use الصندوق)
                    creditAccount = "شيكات تحت التحصيل",
                    amount = invoice.total
                )
            )
        } else if (oldStatus == "تم التحصيل" && newStatus != "تم التحصيل") {
            // Reverse the collection if changed back
            dao.insertLedgerEntry(
                LedgerEntry(
                    description = "إلغاء تحصيل شيك رقم ${invoice.checkNumber} للفاتورة #${invoice.id}",
                    debitAccount = "شيكات تحت التحصيل",
                    creditAccount = "الصندوق",
                    amount = invoice.total
                )
            )
        }
    }

    suspend fun insertLedgerEntry(entry: LedgerEntry): Long {
        val id = dao.insertLedgerEntry(entry)
        logAudit("المحاسبة", "قيد يدوي", "تم تسجيل قيد محاسبي يدوي: ${entry.description}")
        return id
    }

    // --- Shift Operations ---
    suspend fun startShift(employeeName: String, startingCash: Double) {
        val shift = Shift(
            employeeName = employeeName,
            startingCash = startingCash,
            isClosed = false
        )
        dao.insertShift(shift)
        logAudit(employeeName, "فتح وردية", "تم فتح وردية جديدة بمبلغ نقدي ابتدائي $startingCash شيكل")
    }

    suspend fun closeShift(shift: Shift, endingCash: Double, totalSales: Double) {
        val closedShift = shift.copy(
            endingCash = endingCash,
            salesAmount = totalSales,
            endTime = System.currentTimeMillis(),
            isClosed = true
        )
        dao.updateShift(closedShift)
        logAudit(shift.employeeName, "إغلاق وردية", "تم إغلاق الوردية. المبلغ الفعلي في الدرج: $endingCash شيكل. المبيعات الإجمالية: $totalSales شيكل")
    }

    suspend fun logAudit(employeeName: String, action: String, details: String) {
        val log = AuditLog(
            employeeName = employeeName,
            action = action,
            details = details
        )
        val logId = dao.insertAuditLog(log).toInt()
        val finalLog = log.copy(id = logId)
    }

    // --- System Users ---
    val allSystemUsers: Flow<List<SystemUser>> = dao.getAllSystemUsers()

    suspend fun getSystemUserById(uid: String): SystemUser? = dao.getSystemUserById(uid)
    suspend fun getSystemUserByEmail(email: String): SystemUser? = dao.getSystemUserByEmail(email)
    suspend fun getSystemUserByPhone(phone: String): SystemUser? = dao.getSystemUserByPhone(phone)
    suspend fun insertSystemUser(user: SystemUser) {
        dao.insertSystemUser(user)
        saveFirestoreDocumentAsync("system_users", user.uid, user.toMap())
    }
    suspend fun updateSystemUser(user: SystemUser) {
        dao.updateSystemUser(user)
        saveFirestoreDocumentAsync("system_users", user.uid, user.toMap())
    }
    suspend fun deleteSystemUser(user: SystemUser) {
        dao.deleteSystemUser(user)
        deleteFirestoreDocumentAsync("system_users", user.uid)
    }

    // --- Admin Alerts ---
    val allAdminAlerts: Flow<List<AdminAlert>> = dao.getAllAdminAlerts()
    suspend fun insertAdminAlert(alert: AdminAlert) = dao.insertAdminAlert(alert)
    suspend fun markAlertAsRead(alertId: Int) = dao.markAlertAsRead(alertId)
    suspend fun markAllAlertsAsRead() = dao.markAllAlertsAsRead()
    suspend fun clearAdminAlerts() = dao.clearAdminAlerts()

    // --- Suppliers ---
    val allSuppliers: Flow<List<Supplier>> = dao.getAllSuppliers()
    suspend fun insertSupplier(supplier: Supplier): Long = dao.insertSupplier(supplier)
    suspend fun updateSupplier(supplier: Supplier) = dao.updateSupplier(supplier)
    suspend fun deleteSupplier(supplier: Supplier) = dao.deleteSupplier(supplier)
    suspend fun getSupplierById(id: Int): Supplier? = dao.getSupplierById(id)
    suspend fun updateSupplierBalance(id: Int, balanceDelta: Double) = dao.updateSupplierBalance(id, balanceDelta)

    // --- Supplier Transactions ---
    val allSupplierTransactions: Flow<List<SupplierTransaction>> = dao.getAllSupplierTransactions()
    fun getTransactionsForSupplier(supplierId: Int): Flow<List<SupplierTransaction>> = dao.getTransactionsForSupplier(supplierId)
    suspend fun insertSupplierTransaction(tx: SupplierTransaction): Long {
        val id = dao.insertSupplierTransaction(tx)
        // Adjust supplier balance: purchases (شراء) increase the amount we owe them (positive delta)
        // payments (دفعة) decrease the amount we owe them (negative delta)
        val delta = if (tx.transactionType == "شراء") tx.amount else -tx.amount
        dao.updateSupplierBalance(tx.supplierId, delta)
        return id
    }
    suspend fun deleteSupplierTransaction(tx: SupplierTransaction) {
        dao.deleteSupplierTransaction(tx)
        // Reverse the balance adjustment
        val delta = if (tx.transactionType == "شراء") -tx.amount else tx.amount
        dao.updateSupplierBalance(tx.supplierId, delta)
    }

    // --- Customer Transactions ---
    val allCustomerTransactions: Flow<List<CustomerTransaction>> = dao.getAllCustomerTransactions()
    fun getTransactionsForCustomer(customerId: Int): Flow<List<CustomerTransaction>> = dao.getTransactionsForCustomer(customerId)
    suspend fun insertCustomerTransaction(tx: CustomerTransaction): Long {
        val id = dao.insertCustomerTransaction(tx)
        // Adjust customer points or other things if needed.
        // We can use CustomerTransaction to record invoices (بيع increases their outstanding debit)
        // and payments (دفعة decreases their outstanding balance)
        return id
    }
    suspend fun deleteCustomerTransaction(tx: CustomerTransaction) = dao.deleteCustomerTransaction(tx)

    suspend fun clearAllData() {
        dao.clearProducts()
        dao.clearCategories()
        dao.clearInvoices()
        dao.clearInvoiceItems()
        dao.clearLedgerEntries()
        dao.clearShifts()
        dao.clearCustomers()
        dao.clearAuditLogs()
        dao.clearAdminAlerts()
    }

    // --- Employees ---
    val allEmployees: Flow<List<Employee>> = dao.getAllEmployees()

    suspend fun insertEmployee(employee: Employee): Long {
        return dao.insertEmployee(employee)
    }

    suspend fun updateEmployee(employee: Employee) {
        dao.updateEmployee(employee)
    }

    suspend fun updateEmployeeBalance(id: Int, balanceDelta: Double) {
        dao.updateEmployeeBalance(id, balanceDelta)
    }

    // --- Expenses ---
    val allExpenses: Flow<List<Expense>> = dao.getAllExpenses()

    suspend fun insertExpense(expense: Expense): Long {
        return dao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        dao.deleteExpense(expense)
    }

    suspend fun deleteLedgerEntry(entry: LedgerEntry) {
        dao.deleteLedgerEntry(entry)
    }

    // --- Feedbacks ---
    val allFeedbacks: Flow<List<AppFeedback>> = dao.getAllFeedbacks()

    suspend fun insertFeedback(feedback: AppFeedback): Long {
        val id = dao.insertFeedback(feedback)
        saveFirestoreDocumentAsync("app_feedbacks", id.toString(), mapOf(
            "id" to id,
            "timestamp" to feedback.timestamp,
            "senderName" to feedback.senderName,
            "storeName" to feedback.storeName,
            "phone" to feedback.phone,
            "title" to feedback.title,
            "description" to feedback.description,
            "category" to feedback.category,
            "imageUri" to feedback.imageUri,
            "status" to feedback.status,
            "adminReply" to feedback.adminReply
        ))
        return id
    }

    suspend fun updateFeedback(feedback: AppFeedback) {
        dao.updateFeedback(feedback)
        saveFirestoreDocumentAsync("app_feedbacks", feedback.id.toString(), mapOf(
            "id" to feedback.id,
            "timestamp" to feedback.timestamp,
            "senderName" to feedback.senderName,
            "storeName" to feedback.storeName,
            "phone" to feedback.phone,
            "title" to feedback.title,
            "description" to feedback.description,
            "category" to feedback.category,
            "imageUri" to feedback.imageUri,
            "status" to feedback.status,
            "adminReply" to feedback.adminReply
        ))
    }

    suspend fun deleteFeedback(feedback: AppFeedback) {
        dao.deleteFeedback(feedback)
        deleteFirestoreDocumentAsync("app_feedbacks", feedback.id.toString())
    }
}

data class CartItem(
    val product: Product,
    val quantity: Int
)
