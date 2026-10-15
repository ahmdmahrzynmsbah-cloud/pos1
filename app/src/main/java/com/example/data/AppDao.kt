package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM system_users ORDER BY registrationDate DESC")
    fun getAllSystemUsers(): Flow<List<SystemUser>>

    @Query("SELECT * FROM system_users WHERE uid = :uid")
    suspend fun getSystemUserById(uid: String): SystemUser?

    @Query("SELECT * FROM system_users WHERE email = :email LIMIT 1")
    suspend fun getSystemUserByEmail(email: String): SystemUser?

    @Query("SELECT * FROM system_users WHERE phone = :phone LIMIT 1")
    suspend fun getSystemUserByPhone(phone: String): SystemUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSystemUser(user: SystemUser)

    @Update
    suspend fun updateSystemUser(user: SystemUser)

    @Delete
    suspend fun deleteSystemUser(user: SystemUser)

    @Query("DELETE FROM system_users WHERE uid = :uid")
    suspend fun deleteSystemUserById(uid: String)

    @Query("DELETE FROM system_users")
    suspend fun clearSystemUsers()
    @Query("SELECT COUNT(*) FROM system_users")
    suspend fun getSystemUsersCount(): Int

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductsCount(): Int

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoriesCount(): Int

    // --- Products ---
    @Query("SELECT * FROM products ORDER BY nameAr ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("UPDATE products SET quantity = :quantity WHERE id = :id")
    suspend fun updateProductQuantity(id: Int, quantity: Int)

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY nameAr ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    // --- Invoices ---
    @Query("SELECT * FROM invoices ORDER BY invoiceDate DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Int): Invoice?

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: Int)

    // --- Invoice Items ---
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    fun getInvoiceItemsForInvoice(invoiceId: Int): Flow<List<InvoiceItem>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceItemsListForInvoice(invoiceId: Int): List<InvoiceItem>

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteInvoiceItemsForInvoice(invoiceId: Int)

    @Query("SELECT * FROM invoice_items WHERE productId = :productId")
    fun getInvoiceItemsForProduct(productId: Int): Flow<List<InvoiceItem>>

    @Query("SELECT * FROM invoice_items")
    fun getAllInvoiceItems(): Flow<List<InvoiceItem>>

    @Query("SELECT i.id AS invoiceId, i.invoiceDate AS invoiceDate, item.quantity AS quantity, item.price AS price, item.total AS total FROM invoice_items item INNER JOIN invoices i ON item.invoiceId = i.id WHERE item.productId = :productId ORDER BY i.invoiceDate DESC")
    fun getProductSalesHistory(productId: Int): Flow<List<ProductSaleHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItem(item: InvoiceItem)

    // --- Banks ---
    @Query("SELECT * FROM banks ORDER BY name ASC")
    fun getAllBanks(): Flow<List<Bank>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBank(bank: Bank)

    @Delete
    suspend fun deleteBank(bank: Bank)

    @Query("SELECT * FROM banks WHERE code = :code LIMIT 1")
    suspend fun getBankByCode(code: String): Bank?

    // --- Customers ---
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Int): Customer?

    @Query("UPDATE customers SET points = points + :pointsToAdd WHERE id = :id")
    suspend fun addCustomerPoints(id: Int, pointsToAdd: Int)

    // --- Ledger Entries ---
    @Query("SELECT * FROM ledger_entries ORDER BY date DESC")
    fun getAllLedgerEntries(): Flow<List<LedgerEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: LedgerEntry)

    @Delete
    suspend fun deleteLedgerEntry(entry: LedgerEntry)

    // --- Shifts ---
    @Query("SELECT * FROM shifts ORDER BY startTime DESC")
    fun getAllShifts(): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE isClosed = 0 LIMIT 1")
    fun getActiveShift(): Flow<Shift?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: Shift): Long

    @Update
    suspend fun updateShift(shift: Shift)

    // --- Audit Logs ---
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs WHERE details LIKE '%' || :productName || '%' ORDER BY timestamp DESC")
    fun getAuditLogsForProduct(productName: String): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog): Long

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM invoices")
    suspend fun clearInvoices()

    @Query("DELETE FROM invoice_items")
    suspend fun clearInvoiceItems()

    @Query("DELETE FROM ledger_entries")
    suspend fun clearLedgerEntries()

    @Query("DELETE FROM shifts")
    suspend fun clearShifts()

    @Query("DELETE FROM customers")
    suspend fun clearCustomers()

    @Query("DELETE FROM audit_logs")
    suspend fun clearAuditLogs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdminAlert(alert: AdminAlert)

    @Query("SELECT * FROM admin_alerts ORDER BY timestamp DESC")
    fun getAllAdminAlerts(): Flow<List<AdminAlert>>

    @Query("UPDATE admin_alerts SET isRead = 1 WHERE id = :alertId")
    suspend fun markAlertAsRead(alertId: Int)

    @Query("UPDATE admin_alerts SET isRead = 1")
    suspend fun markAllAlertsAsRead()

    @Query("DELETE FROM admin_alerts")
    suspend fun clearAdminAlerts()

    // --- Suppliers ---
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Delete
    suspend fun deleteSupplier(supplier: Supplier)

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: Int): Supplier?

    @Query("UPDATE suppliers SET balance = balance + :balanceDelta WHERE id = :id")
    suspend fun updateSupplierBalance(id: Int, balanceDelta: Double)

    // --- Supplier Transactions ---
    @Query("SELECT * FROM supplier_transactions ORDER BY date DESC")
    fun getAllSupplierTransactions(): Flow<List<SupplierTransaction>>

    @Query("SELECT * FROM supplier_transactions WHERE supplierId = :supplierId ORDER BY date DESC")
    fun getTransactionsForSupplier(supplierId: Int): Flow<List<SupplierTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplierTransaction(tx: SupplierTransaction): Long

    @Delete
    suspend fun deleteSupplierTransaction(tx: SupplierTransaction)

    @Query("DELETE FROM suppliers")
    suspend fun clearSuppliers()

    @Query("DELETE FROM supplier_transactions")
    suspend fun clearSupplierTransactions()

    // --- Customer Transactions ---
    @Query("SELECT * FROM customer_transactions ORDER BY date DESC")
    fun getAllCustomerTransactions(): Flow<List<CustomerTransaction>>

    @Query("SELECT * FROM customer_transactions WHERE customerId = :customerId ORDER BY date DESC")
    fun getTransactionsForCustomer(customerId: Int): Flow<List<CustomerTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerTransaction(tx: CustomerTransaction): Long

    @Delete
    suspend fun deleteCustomerTransaction(tx: CustomerTransaction)

    @Query("DELETE FROM customer_transactions")
    suspend fun clearCustomerTransactions()

    // --- Employees ---
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee): Long

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Query("UPDATE employees SET balance = balance + :balanceDelta WHERE id = :id")
    suspend fun updateEmployeeBalance(id: Int, balanceDelta: Double)

    @Query("DELETE FROM employees")
    suspend fun clearEmployees()

    // --- Expenses ---
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    // --- App Feedbacks ---
    @Query("SELECT * FROM app_feedbacks ORDER BY timestamp DESC")
    fun getAllFeedbacks(): Flow<List<AppFeedback>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: AppFeedback): Long

    @Update
    suspend fun updateFeedback(feedback: AppFeedback)

    @Delete
    suspend fun deleteFeedback(feedback: AppFeedback)

    @Query("DELETE FROM app_feedbacks")
    suspend fun clearFeedbacks()
}
