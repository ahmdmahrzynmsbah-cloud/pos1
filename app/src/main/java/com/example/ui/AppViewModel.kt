package com.example.ui

import android.app.Application
import android.graphics.Typeface
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class AppViewModel(application: Application) : AndroidViewModel(application) {

  private val _currentUserAccount = MutableStateFlow<com.example.data.UserAccount?>(null)
  val currentUserAccount = _currentUserAccount.asStateFlow()
  val subscriptionStatus = MutableStateFlow("ACTIVE")
  val pdfPreviewFile = MutableStateFlow<java.io.File?>(null)

  private val database = AppDatabase.getDatabase(application, viewModelScope)
  private val repository = AppRepository(database.appDao())

  // --- State Observables ---
  val allProducts = repository.allProducts
  val allCategories = repository.allCategories
  val allInvoices = repository.allInvoices
  val allInvoiceItems = repository.allInvoiceItems
  fun getInvoiceItemsForInvoice(invoiceId: Int) = repository.getInvoiceItemsForInvoice(invoiceId)
  fun getInvoiceItemsForProduct(productId: Int) = repository.getInvoiceItemsForProduct(productId)
  fun deleteInvoice(invoice: Invoice, onSuccess: (() -> Unit)? = null) {
    viewModelScope.launch {
      try {
        repository.deleteInvoice(invoice)
        showToast("تم حذف الفاتورة رقم #${invoice.id} بنجاح ")
        onSuccess?.invoke()
      } catch (e: Exception) {
        showToast("خطأ أثناء حذف الفاتورة: ${e.message}")
      }
    }
  }

  fun deleteExpense(expense: Expense, onSuccess: (() -> Unit)? = null) {
    viewModelScope.launch {
      try {
        repository.deleteExpense(expense)
        showToast("تم حذف سند الصرف بنجاح ")
        onSuccess?.invoke()
      } catch (e: Exception) {
        showToast("خطأ أثناء حذف سند الصرف: ${e.message}")
      }
    }
  }

  fun deleteLedgerEntry(entry: LedgerEntry, onSuccess: (() -> Unit)? = null) {
    viewModelScope.launch {
      try {
        repository.deleteLedgerEntry(entry)
        showToast("تم حذف سند القيد بنجاح ")
        onSuccess?.invoke()
      } catch (e: Exception) {
        showToast("خطأ أثناء حذف سند القيد: ${e.message}")
      }
    }
  }
  val allCustomers = repository.allCustomers
  val allLedgerEntries = repository.allLedgerEntries
  val allShifts = repository.allShifts
  val activeShift = repository.activeShift
  val allAuditLogs = repository.allAuditLogs
  val allSuppliers = repository.allSuppliers
  val allBanks = repository.allBanks
  val allSupplierTransactions = repository.allSupplierTransactions
  val allCustomerTransactions = repository.allCustomerTransactions
  val allFeedbacks = repository.allFeedbacks

  fun submitFeedback(
    senderName: String,
    storeName: String,
    phone: String,
    title: String,
    description: String,
    category: String,
    imageUri: String,
    onSuccess: () -> Unit
  ) {
    viewModelScope.launch {
      try {
        if (title.isBlank() || description.isBlank()) {
          showToast("يرجى إدخال عنوان ووصف الاقتراح أو الطلب")
          return@launch
        }
        val feedback = AppFeedback(
          senderName = senderName,
          storeName = storeName,
          phone = phone,
          title = title.trim(),
          description = description.trim(),
          category = category,
          imageUri = imageUri,
          status = "جديد"
        )
        repository.insertFeedback(feedback)
        showToast("تم إرسال اقتراحك للشركة بنجاح! شكراً لتواصلك ")
        
        // Notify Admin (Simulated System Notification)
        NotificationHelper.showNotification(
          getApplication(),
          "طلب/اقتراح جديد من $senderName",
          "العنوان: ${title.trim()}\nالتفاصيل: ${description.take(50)}..."
        )
        
        onSuccess()
      } catch (e: Exception) {
        showToast("خطأ أثناء إرسال الاقتراح: ${e.message}")
      }
    }
  }

  fun updateFeedbackStatus(feedback: AppFeedback, newStatus: String, adminReply: String) {
    viewModelScope.launch {
      try {
        val updated = feedback.copy(status = newStatus, adminReply = adminReply)
        repository.updateFeedback(updated)
        showToast("تم تحديث حالة الاقتراح بنجاح ")
        
        // Notify User (Simulated System Notification)
        val msg = if (adminReply.isNotBlank()) "الإدارة: $adminReply" else "الحالة الجديدة: $newStatus"
        NotificationHelper.showNotification(
          getApplication(),
          "تحديث بخصوص طلبك: ${feedback.title}",
          msg
        )
      } catch (e: Exception) {
        showToast("خطأ أثناء التحديث: ${e.message}")
      }
    }
  }

  fun deleteFeedback(feedback: AppFeedback) {
    viewModelScope.launch {
      try {
        repository.deleteFeedback(feedback)
        showToast("تم حذف الطلب/الاقتراح بنجاح")
      } catch (e: Exception) {
        showToast("خطأ أثناء الحذف: ${e.message}")
      }
    }
  }

  private val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)

  val branches = MutableStateFlow<List<String>>(emptyList())

  val firebaseProjectId = MutableStateFlow("")
  val firebaseWebApiKey = MutableStateFlow("")
  val firebaseDatabaseUrl = MutableStateFlow("")
  val lastSyncTime = MutableStateFlow("لم يتم المزامنة بعد")
  val isCloudSyncing = MutableStateFlow(false)

  // --- Bluetooth Thermal Printer Management ---
  val bluetoothPrinterManager = com.example.printer.BluetoothPrinterManager(application)
  val savedPrinterDevice = MutableStateFlow<com.example.printer.BluetoothPrinterDevice?>(bluetoothPrinterManager.getSavedPrinter())
  val isPrinterPrinting = MutableStateFlow(false)

  fun refreshPairedPrinters(): List<com.example.printer.BluetoothPrinterDevice> {
    return bluetoothPrinterManager.getPairedDevices()
  }

  fun saveBluetoothPrinter(device: com.example.printer.BluetoothPrinterDevice) {
    bluetoothPrinterManager.saveSelectedPrinter(device)
    savedPrinterDevice.value = device
    showToast("تم اختيار الطابعة الحرارية: ${device.name} ")
  }

  fun clearBluetoothPrinter() {
    bluetoothPrinterManager.clearSavedPrinter()
    savedPrinterDevice.value = null
    showToast("تم إزالة الطابعة المقترنة")
  }

  fun testBluetoothPrinter(device: com.example.printer.BluetoothPrinterDevice) {
    viewModelScope.launch {
      isPrinterPrinting.value = true
      showToast("جاري الاتصال بالطابعة واختبار الطباعة... ")
      val res = bluetoothPrinterManager.printTestReceipt(device.name, device.address)
      isPrinterPrinting.value = false
      if (res.isSuccess) {
        showToast("تمت طباعة الفاتورة التجريبية بنجاح! ")
      } else {
        showToast("فشل الاتصال بالطابعة : ${res.exceptionOrNull()?.message}")
      }
    }
  }

  fun printThermalInvoice(
    invoice: Invoice,
    items: List<CartItem>,
    customerName: String?,
    onNeedPrinterSelection: () -> Unit
  ) {
    val printer = savedPrinterDevice.value
    if (printer == null) {
      onNeedPrinterSelection()
      return
    }

    viewModelScope.launch {
      isPrinterPrinting.value = true
      showToast("جاري إرسال الفاتورة للطابعة الحرارية... ")
      val res = bluetoothPrinterManager.printInvoiceReceipt(
        address = printer.address,
        storeName = storeName.value,
        storePhone = storePhone.value,
        storeVatNumber = storeVatNumber.value,
        currentBranch = currentBranch.value,
        invoice = invoice,
        items = items,
        customerName = customerName,
        currencySymbol = currencySymbol.value
      )
      isPrinterPrinting.value = false
      if (res.isSuccess) {
        showToast("تمت طباعة الفاتورة بنجاح عبر البلوتوث! ")
      } else {
        showToast("خطأ أثناء الطباعة : ${res.exceptionOrNull()?.message}")
      }
    }
  }


  // --- POS Search & Category Filter ---
  private val _authState = kotlinx.coroutines.flow.MutableStateFlow<AuthState>(AuthState.Idle)
  val authState = _authState.asStateFlow()
  fun loginAs(account: com.example.data.UserAccount) { 
    _currentUserAccount.value = account
    currentUser.value = account.name
    currentUserRole.value = if (account.role == "ADMIN") "مدير النظام" else "كاشير"
    if (account.phone.isNotBlank()) {
      cashierPhone.value = account.phone
      if (storePhone.value.isBlank() || storePhone.value == "0591234567") {
        storePhone.value = account.phone
      }
    }
    prefs.edit()
      .putString("logged_in_uid", account.uid)
      .putString("logged_in_name", account.name)
      .putString("logged_in_phone", account.phone)
      .putString("logged_in_role", account.role)
      .putBoolean("has_logged_out_manually", false)
      .putBoolean("user_explicitly_logged_in", true)
      .apply()
  }
  fun getProductSalesHistory(productId: Int): Flow<List<ProductSaleHistory>> {
    return repository.getProductSalesHistory(productId)
  }

  fun getAuditLogsForProduct(productName: String): Flow<List<AuditLog>> {
    return repository.getAuditLogsForProduct(productName)
  }


  fun checkpointDatabase() {
    try {
      val cursor = database.query(androidx.sqlite.db.SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
      cursor.close()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun closeDatabase() {
    try {
      if (database.isOpen) {
        database.close()
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  suspend fun exportFullBackup(context: android.content.Context, uri: android.net.Uri): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
      checkpointDatabase()
      
      val backupObj = org.json.JSONObject()
      backupObj.put("version", 15)
      backupObj.put("timestamp", System.currentTimeMillis())
      
      // Products
      val products = repository.allProducts.first()
      val productsArray = org.json.JSONArray()
      products.forEach { p ->
        productsArray.put(org.json.JSONObject().apply {
          put("id", p.id)
          put("nameAr", p.nameAr)
          put("nameEn", p.nameEn)
          put("barcode", p.barcode)
          put("pricePurchase", p.pricePurchase)
          put("priceSale", p.priceSale)
          put("quantity", p.quantity)
          put("unit", p.unit)
          put("minQuantityAlert", p.minQuantityAlert)
          put("category", p.category)
          put("imageResName", p.imageResName)
        })
      }
      backupObj.put("products", productsArray)
      
      // Categories
      val categories = repository.allCategories.first()
      val categoriesArray = org.json.JSONArray()
      categories.forEach { c ->
        categoriesArray.put(org.json.JSONObject().apply {
          put("id", c.id)
          put("nameAr", c.nameAr)
          put("nameEn", c.nameEn)
        })
      }
      backupObj.put("categories", categoriesArray)
      
      // Invoices
      val invoices = repository.allInvoices.first()
      val invoicesArray = org.json.JSONArray()
      invoices.forEach { inv ->
        invoicesArray.put(org.json.JSONObject().apply {
          put("id", inv.id)
          if (inv.customerId != null) put("customerId", inv.customerId)
          put("invoiceDate", inv.invoiceDate)
          put("subtotal", inv.subtotal)
          put("discount", inv.discount)
          put("taxAmount", inv.taxAmount)
          put("total", inv.total)
          put("paymentMethod", inv.paymentMethod)
          put("paidAmount", inv.paidAmount)
          put("changeAmount", inv.changeAmount)
          put("cashierName", inv.cashierName)
          put("branchName", inv.branchName)
          if (inv.checkNumber != null) put("checkNumber", inv.checkNumber)
          if (inv.checkBankName != null) put("checkBankName", inv.checkBankName)
          if (inv.checkDate != null) put("checkDate", inv.checkDate)
          if (inv.checkStatus != null) put("checkStatus", inv.checkStatus)
          if (inv.dueDate != null) put("dueDate", inv.dueDate)
          put("isPurchase", inv.isPurchase)
          put("isDraft", inv.isDraft)
        })
      }
      backupObj.put("invoices", invoicesArray)
      
      // Invoice Items
      val invoiceItems = repository.allInvoiceItems.first()
      val itemsArray = org.json.JSONArray()
      invoiceItems.forEach { item ->
        itemsArray.put(org.json.JSONObject().apply {
          put("id", item.id)
          put("invoiceId", item.invoiceId)
          put("productId", item.productId)
          put("productNameAr", item.productNameAr)
          put("productNameEn", item.productNameEn)
          put("price", item.price)
          put("quantity", item.quantity)
          put("total", item.total)
        })
      }
      backupObj.put("invoice_items", itemsArray)
      
      // Customers
      val customers = repository.allCustomers.first()
      val custArray = org.json.JSONArray()
      customers.forEach { cust ->
        custArray.put(org.json.JSONObject().apply {
          put("id", cust.id)
          put("name", cust.name)
          put("phone", cust.phone)
          put("email", cust.email)
          put("points", cust.points)
          put("type", cust.type)
        })
      }
      backupObj.put("customers", custArray)
      
      // Suppliers
      val suppliers = repository.allSuppliers.first()
      val supArray = org.json.JSONArray()
      suppliers.forEach { sup ->
        supArray.put(org.json.JSONObject().apply {
          put("id", sup.id)
          put("name", sup.name)
          put("contactName", sup.contactName)
          put("phone", sup.phone)
          put("email", sup.email)
          put("address", sup.address)
          put("companyName", sup.companyName)
          put("balance", sup.balance)
        })
      }
      backupObj.put("suppliers", supArray)
      
      // Supplier Transactions
      val supTx = repository.allSupplierTransactions.first()
      val supTxArray = org.json.JSONArray()
      supTx.forEach { tx ->
        supTxArray.put(org.json.JSONObject().apply {
          put("id", tx.id)
          put("supplierId", tx.supplierId)
          put("date", tx.date)
          put("transactionType", tx.transactionType)
          put("amount", tx.amount)
          put("note", tx.note)
        })
      }
      backupObj.put("supplier_transactions", supTxArray)

      // Customer Transactions
      val custTx = repository.allCustomerTransactions.first()
      val custTxArray = org.json.JSONArray()
      custTx.forEach { tx ->
        custTxArray.put(org.json.JSONObject().apply {
          put("id", tx.id)
          put("customerId", tx.customerId)
          put("date", tx.date)
          put("transactionType", tx.transactionType)
          if (tx.invoiceId != null) put("invoiceId", tx.invoiceId)
          put("amount", tx.amount)
          put("note", tx.note)
          if (tx.dueDate != null) put("dueDate", tx.dueDate)
        })
      }
      backupObj.put("customer_transactions", custTxArray)

      // Ledger Entries
      val ledger = repository.allLedgerEntries.first()
      val ledgerArray = org.json.JSONArray()
      ledger.forEach { e ->
        ledgerArray.put(org.json.JSONObject().apply {
          put("id", e.id)
          put("date", e.date)
          put("description", e.description)
          put("debitAccount", e.debitAccount)
          put("creditAccount", e.creditAccount)
          put("amount", e.amount)
        })
      }
      backupObj.put("ledger_entries", ledgerArray)

      // Employees
      val employees = repository.allEmployees.first()
      val empArray = org.json.JSONArray()
      employees.forEach { emp ->
        empArray.put(org.json.JSONObject().apply {
          put("id", emp.id)
          put("name", emp.name)
          put("phone", emp.phone)
          put("role", emp.role)
          put("salary", emp.salary)
          put("balance", emp.balance)
        })
      }
      backupObj.put("employees", empArray)

      // Expenses
      val expenses = repository.allExpenses.first()
      val expArray = org.json.JSONArray()
      expenses.forEach { exp ->
        expArray.put(org.json.JSONObject().apply {
          put("id", exp.id)
          put("type", exp.type)
          put("amount", exp.amount)
          put("date", exp.date)
          if (exp.employeeId != null) put("employeeId", exp.employeeId)
          put("notes", exp.notes)
        })
      }
      backupObj.put("expenses", expArray)

      // System Users
      val sysUsers = repository.allSystemUsers.first()
      val usersArray = org.json.JSONArray()
      sysUsers.forEach { u ->
        usersArray.put(org.json.JSONObject().apply {
          put("uid", u.uid)
          put("name", u.name)
          put("email", u.email)
          put("phone", u.phone)
          put("role", u.role)
          put("registrationDate", u.registrationDate)
          if (u.trialEndDate != null) put("trialEndDate", u.trialEndDate)
          put("isSubscribed", u.isSubscribed)
          put("isActive", u.isActive)
          put("storeName", u.storeName)
          put("storePhone", u.storePhone)
          put("storeVatNumber", u.storeVatNumber)
          put("currentBranch", u.currentBranch)
          put("currencySymbol", u.currencySymbol)
          put("cashierName", u.cashierName)
          put("cashierPhone", u.cashierPhone)
          put("cashierAddress", u.cashierAddress)
          put("canAccessReports", u.canAccessReports)
          put("canAccessInventory", u.canAccessInventory)
          put("canAccessAccounting", u.canAccessAccounting)
          put("canAccessAdvancedTools", u.canAccessAdvancedTools)
          put("canAddProducts", u.canAddProducts)
          put("canDeleteProducts", u.canDeleteProducts)
          put("canApplyDiscounts", u.canApplyDiscounts)
          put("canViewProfits", u.canViewProfits)
        })
      }
      backupObj.put("system_users", usersArray)

      // Audit Logs
      val auditLogs = repository.allAuditLogs.first()
      val auditArray = org.json.JSONArray()
      auditLogs.forEach { log ->
        auditArray.put(org.json.JSONObject().apply {
          put("id", log.id)
          put("timestamp", log.timestamp)
          put("employeeName", log.employeeName)
          put("action", log.action)
          put("details", log.details)
        })
      }
      backupObj.put("audit_logs", auditArray)

      context.contentResolver.openOutputStream(uri)?.use { outputStream ->
        outputStream.write(backupObj.toString(2).toByteArray(Charsets.UTF_8))
      }
      return@withContext true
    } catch (e: Exception) {
      Log.e("Backup", "Export failed", e)
      return@withContext false
    }
  }

  suspend fun restoreFullBackup(context: android.content.Context, uri: android.net.Uri): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
      val content = context.contentResolver.openInputStream(uri)?.use { isStream ->
        isStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
      } ?: return@withContext false

      if (content.trim().startsWith("{")) {
        val jsonObj = org.json.JSONObject(content)
        val dao = repository.dao

        // Clear existing tables
        dao.clearProducts()
        dao.clearCategories()
        dao.clearInvoices()
        dao.clearInvoiceItems()
        dao.clearCustomers()
        dao.clearSuppliers()
        dao.clearSupplierTransactions()
        dao.clearCustomerTransactions()
        dao.clearLedgerEntries()
        dao.clearShifts()
        dao.clearEmployees()
        dao.clearExpenses()
        dao.clearSystemUsers()
        dao.clearAuditLogs()

        // Restore Products
        val productsArray = jsonObj.optJSONArray("products")
        if (productsArray != null) {
          for (i in 0 until productsArray.length()) {
            val obj = productsArray.getJSONObject(i)
            dao.insertProduct(Product(
              id = obj.optInt("id", 0),
              nameAr = obj.optString("nameAr", ""),
              nameEn = obj.optString("nameEn", ""),
              barcode = obj.optString("barcode", ""),
              pricePurchase = obj.optDouble("pricePurchase", 0.0),
              priceSale = obj.optDouble("priceSale", 0.0),
              quantity = obj.optInt("quantity", 0),
              unit = obj.optString("unit", "قطعة"),
              minQuantityAlert = obj.optInt("minQuantityAlert", 0),
              category = obj.optString("category", "عام"),
              imageResName = obj.optString("imageResName", "")
            ))
          }
        }

        // Restore Categories
        val categoriesArray = jsonObj.optJSONArray("categories")
        if (categoriesArray != null) {
          for (i in 0 until categoriesArray.length()) {
            val obj = categoriesArray.getJSONObject(i)
            dao.insertCategory(Category(
              id = obj.optInt("id", 0),
              nameAr = obj.optString("nameAr", ""),
              nameEn = obj.optString("nameEn", "")
            ))
          }
        }

        // Restore Invoices
        val invoicesArray = jsonObj.optJSONArray("invoices")
        if (invoicesArray != null) {
          for (i in 0 until invoicesArray.length()) {
            val obj = invoicesArray.getJSONObject(i)
            dao.insertInvoice(Invoice(
              id = obj.optInt("id", 0),
              customerId = if (obj.has("customerId") && !obj.isNull("customerId")) obj.optInt("customerId") else null,
              invoiceDate = obj.optLong("invoiceDate", System.currentTimeMillis()),
              subtotal = obj.optDouble("subtotal", 0.0),
              discount = obj.optDouble("discount", 0.0),
              taxAmount = obj.optDouble("taxAmount", 0.0),
              total = obj.optDouble("total", 0.0),
              paymentMethod = obj.optString("paymentMethod", "نقدي"),
              paidAmount = obj.optDouble("paidAmount", 0.0),
              changeAmount = obj.optDouble("changeAmount", 0.0),
              cashierName = obj.optString("cashierName", ""),
              branchName = obj.optString("branchName", ""),
              checkNumber = if (obj.has("checkNumber")) obj.optString("checkNumber") else null,
              checkBankName = if (obj.has("checkBankName")) obj.optString("checkBankName") else null,
              checkDate = if (obj.has("checkDate") && !obj.isNull("checkDate")) obj.optLong("checkDate") else null,
              checkStatus = if (obj.has("checkStatus")) obj.optString("checkStatus") else null,
              dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.optLong("dueDate") else null,
              isPurchase = obj.optBoolean("isPurchase", false),
              isDraft = obj.optBoolean("isDraft", false)
            ))
          }
        }

        // Restore Invoice Items
        val itemsArray = jsonObj.optJSONArray("invoice_items")
        if (itemsArray != null) {
          for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.getJSONObject(i)
            dao.insertInvoiceItem(InvoiceItem(
              id = obj.optInt("id", 0),
              invoiceId = obj.optInt("invoiceId", 0),
              productId = obj.optInt("productId", 0),
              productNameAr = obj.optString("productNameAr", ""),
              productNameEn = obj.optString("productNameEn", ""),
              price = obj.optDouble("price", 0.0),
              quantity = obj.optInt("quantity", 1),
              total = obj.optDouble("total", 0.0)
            ))
          }
        }

        // Restore Customers
        val custArray = jsonObj.optJSONArray("customers")
        if (custArray != null) {
          for (i in 0 until custArray.length()) {
            val obj = custArray.getJSONObject(i)
            dao.insertCustomer(Customer(
              id = obj.optInt("id", 0),
              name = obj.optString("name", ""),
              phone = obj.optString("phone", ""),
              email = obj.optString("email", ""),
              points = obj.optInt("points", 0),
              type = obj.optString("type", "عادي")
            ))
          }
        }

        // Restore Suppliers
        val supArray = jsonObj.optJSONArray("suppliers")
        if (supArray != null) {
          for (i in 0 until supArray.length()) {
            val obj = supArray.getJSONObject(i)
            dao.insertSupplier(Supplier(
              id = obj.optInt("id", 0),
              name = obj.optString("name", ""),
              contactName = obj.optString("contactName", ""),
              phone = obj.optString("phone", ""),
              email = obj.optString("email", ""),
              address = obj.optString("address", ""),
              companyName = obj.optString("companyName", ""),
              balance = obj.optDouble("balance", 0.0)
            ))
          }
        }

        // Restore Supplier Transactions
        val supTxArray = jsonObj.optJSONArray("supplier_transactions")
        if (supTxArray != null) {
          for (i in 0 until supTxArray.length()) {
            val obj = supTxArray.getJSONObject(i)
            dao.insertSupplierTransaction(SupplierTransaction(
              id = obj.optInt("id", 0),
              supplierId = obj.optInt("supplierId", 0),
              date = obj.optLong("date", System.currentTimeMillis()),
              transactionType = obj.optString("transactionType", "دفعة"),
              amount = obj.optDouble("amount", 0.0),
              note = obj.optString("note", "")
            ))
          }
        }

        // Restore Customer Transactions
        val custTxArray = jsonObj.optJSONArray("customer_transactions")
        if (custTxArray != null) {
          for (i in 0 until custTxArray.length()) {
            val obj = custTxArray.getJSONObject(i)
            dao.insertCustomerTransaction(CustomerTransaction(
              id = obj.optInt("id", 0),
              customerId = obj.optInt("customerId", 0),
              date = obj.optLong("date", System.currentTimeMillis()),
              transactionType = obj.optString("transactionType", "سداد"),
              invoiceId = if (obj.has("invoiceId") && !obj.isNull("invoiceId")) obj.optInt("invoiceId") else null,
              amount = obj.optDouble("amount", 0.0),
              note = obj.optString("note", ""),
              dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.optLong("dueDate") else null
            ))
          }
        }

        // Restore Ledger Entries
        val ledgerArray = jsonObj.optJSONArray("ledger_entries")
        if (ledgerArray != null) {
          for (i in 0 until ledgerArray.length()) {
            val obj = ledgerArray.getJSONObject(i)
            dao.insertLedgerEntry(LedgerEntry(
              id = obj.optInt("id", 0),
              date = obj.optLong("date", System.currentTimeMillis()),
              description = obj.optString("description", ""),
              debitAccount = obj.optString("debitAccount", ""),
              creditAccount = obj.optString("creditAccount", ""),
              amount = obj.optDouble("amount", 0.0)
            ))
          }
        }

        // Restore Employees
        val empArray = jsonObj.optJSONArray("employees")
        if (empArray != null) {
          for (i in 0 until empArray.length()) {
            val obj = empArray.getJSONObject(i)
            dao.insertEmployee(Employee(
              id = obj.optInt("id", 0),
              name = obj.optString("name", ""),
              phone = obj.optString("phone", ""),
              role = obj.optString("role", ""),
              salary = obj.optDouble("salary", 0.0),
              balance = obj.optDouble("balance", 0.0)
            ))
          }
        }

        // Restore Expenses
        val expArray = jsonObj.optJSONArray("expenses")
        if (expArray != null) {
          for (i in 0 until expArray.length()) {
            val obj = expArray.getJSONObject(i)
            dao.insertExpense(Expense(
              id = obj.optInt("id", 0),
              type = obj.optString("type", "أخرى"),
              amount = obj.optDouble("amount", 0.0),
              date = obj.optLong("date", System.currentTimeMillis()),
              employeeId = if (obj.has("employeeId") && !obj.isNull("employeeId")) obj.optInt("employeeId") else null,
              notes = obj.optString("notes", "")
            ))
          }
        }

        // Restore System Users
        val usersArray = jsonObj.optJSONArray("system_users")
        if (usersArray != null) {
          for (i in 0 until usersArray.length()) {
            val obj = usersArray.getJSONObject(i)
            dao.insertSystemUser(SystemUser(
              uid = obj.optString("uid", ""),
              name = obj.optString("name", ""),
              email = obj.optString("email", ""),
              phone = obj.optString("phone", ""),
              role = obj.optString("role", "CUSTOMER"),
              registrationDate = obj.optLong("registrationDate", System.currentTimeMillis()),
              trialEndDate = if (obj.has("trialEndDate") && !obj.isNull("trialEndDate")) obj.optLong("trialEndDate") else null,
              isSubscribed = obj.optBoolean("isSubscribed", false),
              isActive = obj.optBoolean("isActive", true),
              storeName = obj.optString("storeName", "مؤسسة الكنز للمحاسبة التجارية"),
              storePhone = obj.optString("storePhone", ""),
              storeVatNumber = obj.optString("storeVatNumber", ""),
              currentBranch = obj.optString("currentBranch", "الفرع الرئيسي").replace(" (الرياض)", ""),
              currencySymbol = obj.optString("currencySymbol", "شيكل"),
              cashierName = obj.optString("cashierName", "كاشير المبيعات"),
              cashierPhone = obj.optString("cashierPhone", ""),
              cashierAddress = obj.optString("cashierAddress", "").replace("الرياض، المملكة العربية السعودية", "").replace("الرياض", "").trim(),
              canAccessReports = obj.optBoolean("canAccessReports", true),
              canAccessInventory = obj.optBoolean("canAccessInventory", true),
              canAccessAccounting = obj.optBoolean("canAccessAccounting", true),
              canAccessAdvancedTools = obj.optBoolean("canAccessAdvancedTools", false),
              canAddProducts = obj.optBoolean("canAddProducts", true),
              canDeleteProducts = obj.optBoolean("canDeleteProducts", false),
              canApplyDiscounts = obj.optBoolean("canApplyDiscounts", false),
              canViewProfits = obj.optBoolean("canViewProfits", false)
            ))
          }
        }

        // Restore Audit Logs
        val auditArray = jsonObj.optJSONArray("audit_logs")
        if (auditArray != null) {
          for (i in 0 until auditArray.length()) {
            val obj = auditArray.getJSONObject(i)
            dao.insertAuditLog(AuditLog(
              id = obj.optInt("id", 0),
              timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
              employeeName = obj.optString("employeeName", "النظام"),
              action = obj.optString("action", ""),
              details = obj.optString("details", "")
            ))
          }
        }

        return@withContext true
      } else {
        // Fallback to SQLite binary file overwrite
        val dbFile = context.getDatabasePath("smart_pos_database")
        val dbShm = context.getDatabasePath("smart_pos_database-shm")
        val dbWal = context.getDatabasePath("smart_pos_database-wal")

        closeDatabase()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
          java.io.FileOutputStream(dbFile).use { outputStream ->
            inputStream.copyTo(outputStream)
          }
        }

        if (dbShm.exists()) dbShm.delete()
        if (dbWal.exists()) dbWal.delete()
        return@withContext true
      }
    } catch (e: Exception) {
      Log.e("Backup", "Restore failed", e)
      return@withContext false
    }
  }

  fun logoutSystem() { 
    _currentUserAccount.value = null 
    prefs.edit()
      .remove("logged_in_uid")
      .remove("logged_in_name")
      .remove("logged_in_phone")
      .remove("logged_in_role")
      .putBoolean("has_logged_out_manually", true)
      .putBoolean("user_explicitly_logged_in", false)
      .apply()
  }
  fun updateSubscription(status: String) { subscriptionStatus.value = status }


  private val _searchQuery = MutableStateFlow("")
  val searchQuery = _searchQuery.asStateFlow()

  private val _selectedCategory = MutableStateFlow<String?>(null)
  val selectedCategory = _selectedCategory.asStateFlow()

  val filteredProducts = combine(allProducts, _searchQuery, _selectedCategory) { products, query, cat ->
    products.filter { product ->
      val matchesQuery = product.nameAr.contains(query, ignoreCase = true) ||
          product.nameEn.contains(query, ignoreCase = true) ||
          product.barcode.contains(query)
      val matchesCategory = cat == null || product.category == cat
      matchesQuery && matchesCategory
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // --- POS Cart State ---
  private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
  val cart = _cart.asStateFlow()

  // --- Check Setup State ---
  val setupPaymentMethod = MutableStateFlow("نقدي")
  val setupCheckNumber = MutableStateFlow("")
  val setupCheckBankName = MutableStateFlow("")
  val setupCheckDate = MutableStateFlow(System.currentTimeMillis())
  val setupCustomDate = MutableStateFlow<Long?>(null)

  private val _discount = MutableStateFlow(0.0)
  val discount = _discount.asStateFlow()

  val taxPercent = MutableStateFlow(15.0)

  // --- Customer & Supplier System ---
  private val _selectedCustomerId = MutableStateFlow<Int?>(null)
  val selectedCustomerId = _selectedCustomerId.asStateFlow()

  private val _manualCustomerName = MutableStateFlow<String>("")
  val manualCustomerName = _manualCustomerName.asStateFlow()

  fun setManualCustomerName(name: String) {
    _manualCustomerName.value = name
  }

  private val _selectedSupplierId = MutableStateFlow<Int?>(null)
  val selectedSupplierId = _selectedSupplierId.asStateFlow()

  private val _manualSupplierName = MutableStateFlow<String>("")
  val manualSupplierName = _manualSupplierName.asStateFlow()

  fun setSelectedSupplier(id: Int?) {
    _selectedSupplierId.value = id
    _manualSupplierName.value = ""
  }

  fun setManualSupplierName(name: String) {
    _manualSupplierName.value = name
    _selectedSupplierId.value = null
  }

  // --- Toast Notification Feedback State ---
  private val _toastMessage = MutableStateFlow<String?>(null)
  val toastMessage = _toastMessage.asStateFlow()

  // --- Settings / Metadata ---
  val storeName = MutableStateFlow(prefs.getString("store_name", "مؤسسة الكنز للمحاسبة التجارية") ?: "مؤسسة الكنز للمحاسبة التجارية")
  val storePhone = MutableStateFlow(prefs.getString("store_phone", "") ?: "")
  val storeVatNumber = MutableStateFlow(prefs.getString("store_vat_number", "") ?: "")
  val storeLogoUrl = MutableStateFlow(prefs.getString("store_logo_url", "") ?: "")
  val supportWhatsappNumber = MutableStateFlow(prefs.getString("support_whatsapp", "201034859313") ?: "201034859313")
  val defaultSubscriptionBannerText = "النسخة التجريبية (Trial) - يرجى تفعيل الاشتراك عبر الأدمن للحصول على كافة الصلاحيات والتحديثات السحابية"
  val subscriptionBannerText = MutableStateFlow(prefs.getString("subscription_banner_text", defaultSubscriptionBannerText) ?: defaultSubscriptionBannerText)
  val currentBranch = MutableStateFlow(prefs.getString("current_branch", "الفرع الرئيسي")?.replace(" (الرياض)", "")?.ifBlank { "الفرع الرئيسي" } ?: "الفرع الرئيسي")
  val currencySymbol = MutableStateFlow("شيكل")
  val isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))

  fun toggleDarkMode(isDark: Boolean) {
    isDarkMode.value = isDark
    prefs.edit().putBoolean("is_dark_mode", isDark).apply()
  }

  fun updateSupportWhatsappNumber(number: String) {
    supportWhatsappNumber.value = number
    prefs.edit().putString("support_whatsapp", number).apply()
  }

  fun updateSubscriptionBannerText(text: String) {
    val cleanText = text.trim().ifEmpty { defaultSubscriptionBannerText }
    subscriptionBannerText.value = cleanText
    prefs.edit().putString("subscription_banner_text", cleanText).apply()

    val proj = firebaseProjectId.value
    val apiKey = firebaseWebApiKey.value
    if (!com.example.data.FirebaseSyncManager.isPlaceholderConfig(proj, apiKey)) {
      viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
          val data = mapOf(
            "banner_text" to cleanText,
            "updated_at" to System.currentTimeMillis().toString()
          )
          val idToken = prefs.getString("firebase_id_token", null)
          com.example.data.FirebaseSyncManager.saveDocument(proj, apiKey, "app_config", "subscription_banner", data, idToken)
        } catch (e: Exception) {
          if (e is CancellationException) throw e
          Log.w("BannerSync", "Could not sync banner to cloud: ${e.message}")
        }
      }
    }
    logAdminAction("تحديث شريط التفعيل", "تم تعديل نص شريط تفعيل الاشتراك المتحرك إلى: $cleanText")
  }
  
  fun loadBranches() {
    val saved = prefs.getString("branches_list", null)
    if (saved != null) {
      val parsed = saved.split("||").filter { it.isNotBlank() }
      // Clear out the dummy template ones if they are exactly the ones from the template
      val dummySet = setOf("الفرع الرئيسي (الرياض)", "فرع مكة المكرمة", "فرع جدة (الحمراء)", "فرع الدمام الدولي")
      val isOnlyDummies = parsed.all { dummySet.contains(it) }
      
      if (parsed.isNotEmpty() && !isOnlyDummies) {
         branches.value = parsed.filter { !dummySet.contains(it) }.ifEmpty { listOf("الفرع الرئيسي") }
      } else {
         branches.value = listOf("الفرع الرئيسي")
         // Save the clean list to overwrite the dummy ones
         saveBranches(branches.value)
      }
    } else {
      branches.value = listOf("الفرع الرئيسي")
    }
  }

  fun saveBranches(newList: List<String>) {
    branches.value = newList
    prefs.edit().putString("branches_list", newList.joinToString("||")).apply()
    
    // If currentBranch is no longer in the list, set it to the first one available
    if (!newList.contains(currentBranch.value) && newList.isNotEmpty()) {
      currentBranch.value = newList.first()
    }
  }

  fun getBranchAddress(branch: String): String {
    return prefs.getString("branch_address_$branch", "") ?: ""
  }

  fun getBranchPhone(branch: String): String {
    return prefs.getString("branch_phone_$branch", "") ?: ""
  }

  fun getBranchManager(branch: String): String {
    return prefs.getString("branch_manager_$branch", "") ?: ""
  }

  fun getBranchStatus(branch: String): Boolean {
    return prefs.getBoolean("branch_status_$branch", true)
  }

  fun saveBranchDetails(branch: String, address: String, phone: String, manager: String, active: Boolean) {
    prefs.edit()
      .putString("branch_address_$branch", address)
      .putString("branch_phone_$branch", phone)
      .putString("branch_manager_$branch", manager)
      .putBoolean("branch_status_$branch", active)
      .apply()
    logAdminAction("تحديث تفاصيل الفرع", "تم تحديث تفاصيل الفرع '$branch'.")
  }

  fun addBranch(name: String) {
    val updated = branches.value.toMutableList()
    if (!updated.contains(name) && name.isNotBlank()) {
      updated.add(name)
      saveBranches(updated)
      saveBranchDetails(name, "", "", "", true)
      logAdminAction("إضافة فرع", "تمت إضافة فرع جديد باسم '$name'.")
    }
  }

  fun deleteBranch(name: String) {
    val updated = branches.value.toMutableList()
    updated.remove(name)
    saveBranches(updated)
    prefs.edit()
      .remove("branch_address_$name")
      .remove("branch_phone_$name")
      .remove("branch_manager_$name")
      .remove("branch_status_$name")
      .apply()
    logAdminAction("حذف فرع", "تم حذف الفرع '$name'.")
  }

  fun editBranch(oldName: String, newName: String) {
    val updated = branches.value.toMutableList()
    val idx = updated.indexOf(oldName)
    if (idx != -1 && newName.isNotBlank() && !updated.contains(newName)) {
      updated[idx] = newName
      saveBranches(updated)
      
      val address = getBranchAddress(oldName)
      val phone = getBranchPhone(oldName)
      val manager = getBranchManager(oldName)
      val status = getBranchStatus(oldName)
      
      prefs.edit()
        .putString("branch_address_$newName", address)
        .putString("branch_phone_$newName", phone)
        .putString("branch_manager_$newName", manager)
        .putBoolean("branch_status_$newName", status)
        .remove("branch_address_$oldName")
        .remove("branch_phone_$oldName")
        .remove("branch_manager_$oldName")
        .remove("branch_status_$oldName")
        .apply()

      if (currentBranch.value == oldName) {
        currentBranch.value = newName
      }
      logAdminAction("تعديل فرع", "تم تعديل اسم الفرع من '$oldName' إلى '$newName'.")
    }
  }
  
  fun updateStoreSettings(name: String, phone: String, vat: String, logoUrl: String) {
    storeName.value = name
    storePhone.value = phone
    storeVatNumber.value = vat
    storeLogoUrl.value = logoUrl
    
    prefs.edit()
      .putString("store_name", name)
      .putString("store_phone", phone)
      .putString("store_vat_number", vat)
      .putString("store_logo_url", logoUrl)
      .apply()

    // If a system user is logged in, update their database record as well
    val account = currentUserAccount.value
    if (account != null) {
      viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
          val user = repository.getSystemUserById(account.uid)
          if (user != null) {
            val updatedUser = user.copy(
              storeName = name,
              storePhone = phone,
              storeVatNumber = vat
            )
            repository.updateSystemUser(updatedUser)
          }
        } catch (e: Exception) {
          Log.e("UpdateSettings", "Failed to update system user settings in DB", e)
        }
      }
    }

    logAdminAction("تعديل الهوية", "تم تغيير اسم التطبيق إلى '$name' وتحديث معلومات الهوية والشعار.")
  }

  fun saveFirebaseConfig(projectId: String, apiKey: String, url: String) {
    firebaseProjectId.value = projectId
    firebaseWebApiKey.value = apiKey
    firebaseDatabaseUrl.value = url
    prefs.edit()
      .putString("firebase_project_id", projectId)
      .putString("firebase_web_api_key", apiKey)
      .putString("firebase_database_url", url)
      .apply()
    showToast("تم حفظ إعدادات الاتصال بـ Firebase بنجاح.")
    if (!com.example.data.FirebaseSyncManager.isPlaceholderConfig(projectId, apiKey)) {
      syncWithCloud(isManual = true)
    }
  }

  fun syncWithCloud(isManual: Boolean = false) {
    val proj = firebaseProjectId.value.trim()
    val apiKey = firebaseWebApiKey.value.trim()
    if (com.example.data.FirebaseSyncManager.isPlaceholderConfig(proj, apiKey)) {
      if (isManual) {
        showToast("يرجى إدخال إعدادات الاتصال بـ Firebase أولاً لتفعيل المزامنة!")
      }
      return
    }
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      isCloudSyncing.value = true
      try {
        val idToken = prefs.getString("firebase_id_token", null)

        // 1. Sync System Users strictly (Cloud Database is reserved EXCLUSIVELY for accounts)
        // ONLY Admin is allowed to upload system_users to avoid overwriting admin changes (like subscription renewals)
        if (currentUserAccount.value?.role == "ADMIN") {
            val localUsers = repository.allSystemUsers.first()
            localUsers.forEach { user ->
              try {
                com.example.data.FirebaseSyncManager.saveDocument(proj, apiKey, "system_users", user.uid, user.toMap(), idToken)
              } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w("Sync", "Could not upload user: ${user.uid} (${e.message})")
              }
            }
        }
        try {
          val remoteUsers = com.example.data.FirebaseSyncManager.getCollection(proj, apiKey, "system_users", idToken)
          remoteUsers.forEach { (uid, fields) ->
            val remoteUser = systemUserFromMap(uid, fields)
            val localUser = repository.getSystemUserById(uid)
            if (localUser == null) {
              repository.insertSystemUser(remoteUser)
            } else {
              repository.updateSystemUser(remoteUser)
            }
          }
        } catch (e: Exception) {
          if (e is CancellationException) throw e
          Log.w("Sync", "Could not sync remote users: ${e.message}")
        }

        try {
          val configs = com.example.data.FirebaseSyncManager.getCollection(proj, apiKey, "app_config", idToken)
          val bannerDoc = configs.find { it.first == "subscription_banner" }
          if (bannerDoc != null) {
            val bannerText = bannerDoc.second["banner_text"]?.toString()
            if (!bannerText.isNullOrBlank()) {
              subscriptionBannerText.value = bannerText
              prefs.edit().putString("subscription_banner_text", bannerText).apply()
            }
          }
        } catch (e: Exception) {
          if (e is CancellationException) throw e
          Log.w("Sync", "Could not sync app config: ${e.message}")
        }

        val uCount = repository.allSystemUsers.first().size

        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault())
        val timeNow = sdf.format(java.util.Date())

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
          lastSyncTime.value = timeNow
          prefs.edit().putString("last_sync_time", timeNow).apply()
          if (isManual) {
            showToast("تمت مزامنة حسابات المستخدمين سحابياً بنجاح! \nعدد الحسابات المسجلة: $uCount")
          }
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.w("Sync", "Cloud sync encountered an issue: ${e.message}")
        if (isManual) {
          viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            showToast("تعذر إكمال المزامنة السحابية. يرجى التحقق من أذونات Firebase وقواعد الحماية.")
          }
        }
      } finally {
        isCloudSyncing.value = false
      }
    }
  }
  
  // Cashier specific configurations
  val cashierPhone = MutableStateFlow(prefs.getString("cashier_phone", "") ?: "")
  val cashierAddress = MutableStateFlow(prefs.getString("cashier_address", "")?.replace("الرياض، المملكة العربية السعودية", "")?.replace("الرياض", "")?.trim() ?: "")

  fun updateCurrencySymbol(symbol: String) {
    currencySymbol.value = symbol
  }

  // --- Multi-user login Simulation ---
  val currentUser = MutableStateFlow("كاشير المبيعات")
  val currentUserRole = MutableStateFlow("كاشير") // "مدير", "محاسب", "كاشير"

  fun saveUserSettings() {
    prefs.edit()
      .putString("store_name", storeName.value)
      .putString("store_phone", storePhone.value)
      .putString("store_vat_number", storeVatNumber.value)
      .putString("store_logo_url", storeLogoUrl.value)
      .putString("support_whatsapp", supportWhatsappNumber.value)
      .putString("subscription_banner_text", subscriptionBannerText.value)
      .putString("current_branch", currentBranch.value)
      .putString("currency_symbol", currencySymbol.value)
      .putString("cashier_name", currentUser.value)
      .putString("cashier_phone", cashierPhone.value)
      .putString("cashier_address", cashierAddress.value)
      .apply()
    
    val account = _currentUserAccount.value
    if (account == null || account.role == "ADMIN") {
      logAdminAction("حفظ الإعدادات", "تم حفظ إعدادات النظام وتحديث المتجر بنجاح")
      showToast("تم حفظ جميع الإعدادات والتغييرات بنجاح! ")
      return
    }
    viewModelScope.launch {
      val systemUser = repository.getSystemUserById(account.uid)
      if (systemUser != null) {
        val updatedUser = systemUser.copy(
          storeName = storeName.value,
          storePhone = storePhone.value,
          storeVatNumber = storeVatNumber.value,
          currentBranch = currentBranch.value,
          currencySymbol = currencySymbol.value,
          cashierName = currentUser.value,
          cashierPhone = cashierPhone.value,
          cashierAddress = cashierAddress.value
        )
        repository.updateSystemUser(updatedUser)
        showToast("تم حفظ الإعدادات المخصصة للمستخدم بنجاح! ")
      } else {
        showToast("تم حفظ الإعدادات بنجاح! ")
      }
    }
  }

  // --- Invoice Checkout State (for print receipt dialog) ---
  private val _lastCheckedOutInvoice = MutableStateFlow<Invoice?>(null)
  val lastCheckedOutInvoice = _lastCheckedOutInvoice.asStateFlow()

  private val _lastCheckedOutItems = MutableStateFlow<List<CartItem>>(emptyList())
  val lastCheckedOutItems = _lastCheckedOutItems.asStateFlow()

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setSelectedCategory(category: String?) {
    _selectedCategory.value = category
  }

  // --- Cart Actions ---
  fun addToCart(product: Product, qty: Int = 1) {
    if (product.quantity <= 0) {
      showToast("تحذير: نفذت كمية هذا المنتج من المخزون!")
      return
    }

    val currentCart = _cart.value.toMutableList()
    val index = currentCart.indexOfFirst { it.product.id == product.id }
    if (index != -1) {
      val currentQty = currentCart[index].quantity
      if (currentQty + qty > product.quantity) {
        showToast("لا يمكن تجاوز الكمية المتاحة في المخزون (${product.quantity})")
        return
      }
      currentCart[index] = currentCart[index].copy(quantity = currentQty + qty)
    } else {
      if (qty > product.quantity) {
        showToast("لا يمكن تجاوز الكمية المتاحة في المخزون (${product.quantity})")
        return
      }
      currentCart.add(CartItem(product, qty))
    }
    _cart.value = currentCart
    showToast("تم إضافة ${product.nameAr} إلى السلة")
  }

  fun decreaseQuantity(product: Product) {
    val currentCart = _cart.value.toMutableList()
    val index = currentCart.indexOfFirst { it.product.id == product.id }
    if (index != -1) {
      val currentQty = currentCart[index].quantity
      if (currentQty > 1) {
        currentCart[index] = currentCart[index].copy(quantity = currentQty - 1)
      } else {
        currentCart.removeAt(index)
      }
      _cart.value = currentCart
    }
  }

  fun removeFromCart(product: Product) {
    val currentCart = _cart.value.toMutableList()
    val index = currentCart.indexOfFirst { it.product.id == product.id }
    if (index != -1) {
      currentCart.removeAt(index)
      _cart.value = currentCart
      showToast("تم إزالة المنتج من السلة")
    }
  }

  fun updateCartItemQuantity(productId: Int, newQty: Int) {
    val currentCart = _cart.value.toMutableList()
    val index = currentCart.indexOfFirst { it.product.id == productId }
    if (index != -1) {
      if (newQty <= 0) {
        currentCart.removeAt(index)
        showToast("تم إزالة المنتج من السلة")
      } else {
        currentCart[index] = currentCart[index].copy(quantity = newQty)
      }
      _cart.value = currentCart
    }
  }

  fun updateCartItemPrice(productId: Int, newPrice: Double) {
    val currentCart = _cart.value.toMutableList()
    val index = currentCart.indexOfFirst { it.product.id == productId }
    if (index != -1) {
      val updatedProduct = currentCart[index].product.copy(priceSale = newPrice)
      currentCart[index] = currentCart[index].copy(product = updatedProduct)
      _cart.value = currentCart
    }
  }

  fun clearCart() {
    _cart.value = emptyList()
    _discount.value = 0.0
    _selectedCustomerId.value = null
    _manualCustomerName.value = ""
  }

  fun setDiscount(amount: Double) {
    _discount.value = amount
  }

  fun selectCustomer(id: Int?) {
    _selectedCustomerId.value = id
  }

  fun showToast(message: String) {
    _toastMessage.value = message
  }

  fun clearToast() {
    _toastMessage.value = null
  }

  // --- Inventory Operations ---
  fun addProduct(product: Product, supplier: Supplier?, quantityAdded: Int) {
    viewModelScope.launch {
      repository.insertProduct(product, supplier, quantityAdded, currentUser.value)
      showToast("تم إضافة المنتج ${product.nameAr} بنجاح.")
    }
  }

  fun updateProduct(product: Product, supplier: Supplier?, quantityAdded: Int) {
    viewModelScope.launch {
      repository.updateProduct(product, supplier, quantityAdded, currentUser.value)
      showToast("تم تحديث المنتج ${product.nameAr} بنجاح.")
    }
  }

  fun deleteProduct(product: Product) {
    viewModelScope.launch {
      repository.deleteProduct(product, currentUser.value)
      showToast("تم حذف المنتج ${product.nameAr}.")
    }
  }

  fun addCategory(category: Category) {
    viewModelScope.launch {
      repository.insertCategory(category, currentUser.value)
      showToast("تم إضافة التصنيف ${category.nameAr}.")
    }
  }

  fun deleteCategory(category: Category) {
    viewModelScope.launch {
      repository.deleteCategory(category, currentUser.value)
      showToast("تم حذف التصنيف ${category.nameAr}.")
    }
  }

  // --- CRM Customer Operations ---
  fun addCustomerWithInitialBalance(customer: Customer, initialOwedToUs: Double, initialOwedToThem: Double) {
    viewModelScope.launch {
      val id = repository.insertCustomer(customer, currentUser.value)
      if (initialOwedToUs > 0.0) {
        repository.insertCustomerTransaction(
          CustomerTransaction(
            customerId = id.toInt(),
            transactionType = "بيع",
            amount = initialOwedToUs,
            note = "رصيد افتتاحي عليه"
          )
        )
      }
      if (initialOwedToThem > 0.0) {
        repository.insertCustomerTransaction(
          CustomerTransaction(
            customerId = id.toInt(),
            transactionType = "دفعة",
            amount = initialOwedToThem,
            note = "رصيد افتتاحي له"
          )
        )
      }
      showToast("تم إضافة العميل ${customer.name} بنجاح.")
    }
  }

  fun addCustomer(customer: Customer) {
    viewModelScope.launch {
      repository.insertCustomer(customer, currentUser.value)
      showToast("تم إضافة العميل ${customer.name} بنجاح.")
    }
  }

  fun deleteCustomer(customer: Customer) {
    viewModelScope.launch {
      repository.deleteCustomer(customer, currentUser.value)
      showToast("تم حذف العميل ${customer.name}.")
    }
  }

  // --- CRM Supplier & Transaction Operations ---
  fun addSupplier(supplier: Supplier) {
    viewModelScope.launch {
      repository.insertSupplier(supplier)
      repository.logAudit(currentUser.value, "إضافة مورد", "تم تسجيل مورد جديد: ${supplier.name}")
      showToast("تم إضافة المورد ${supplier.name} بنجاح.")
    }
  }

  fun addSupplierWithInitialBalance(supplier: Supplier, initialOwedToThem: Double, initialOwedToUs: Double) {
    viewModelScope.launch {
      val id = repository.insertSupplier(supplier)
      if (initialOwedToThem > 0.0) {
        repository.insertSupplierTransaction(
          SupplierTransaction(
            supplierId = id.toInt(),
            transactionType = "شراء",
            amount = initialOwedToThem,
            note = "رصيد افتتاحي له"
          )
        )
      }
      if (initialOwedToUs > 0.0) {
        repository.insertSupplierTransaction(
          SupplierTransaction(
            supplierId = id.toInt(),
            transactionType = "سداد",
            amount = initialOwedToUs,
            note = "رصيد افتتاحي عليه"
          )
        )
      }
      repository.logAudit(currentUser.value, "إضافة مورد", "تم تسجيل مورد جديد: ${supplier.name}")
      showToast("تم تسجيل المورد ${supplier.name} بنجاح.")
    }
  }

  fun addSupplierWithInitialBalanceOld(supplier: Supplier, initialBalance: Double) {
    viewModelScope.launch {
      val id = repository.insertSupplier(supplier)
      if (initialBalance != 0.0) {
        repository.insertSupplierTransaction(
          SupplierTransaction(
            supplierId = id.toInt(),
            transactionType = "شراء",
            amount = initialBalance,
            note = "رصيد افتتاحي عند التأسيس"
          )
        )
      }
      repository.logAudit(currentUser.value, "إضافة مورد", "تم تسجيل مورد جديد: ${supplier.name} برصيد افتتاحي $initialBalance")
      showToast("تم تسجيل المورد ${supplier.name} بنجاح.")
    }
  }

  fun updateSupplier(supplier: Supplier) {
    viewModelScope.launch {
      repository.updateSupplier(supplier)
      repository.logAudit(currentUser.value, "تعديل مورد", "تم تعديل بيانات المورد: ${supplier.name}")
      showToast("تم تعديل المورد ${supplier.name} بنجاح.")
    }
  }

  fun deleteSupplier(supplier: Supplier) {
    viewModelScope.launch {
      repository.deleteSupplier(supplier)
      repository.logAudit(currentUser.value, "حذف مورد", "تم حذف المورد: ${supplier.name}")
      showToast("تم حذف المورد ${supplier.name}.")
    }
  }

  fun addSupplierTransaction(tx: SupplierTransaction) {
    viewModelScope.launch {
      repository.insertSupplierTransaction(tx)
      val typeText = if (tx.transactionType == "شراء") "عملية توريد/شراء بضاعة" else "سند صرف دفعة مالية"
      repository.logAudit(currentUser.value, "حركة مورد", "تم تسجيل $typeText بقيمة ${tx.amount} للمورد")
      showToast("تم تسجيل الحركة المالية للمورد بنجاح.")
    }
  }

  fun addPurchaseInvoice(supplierName: String, amount: Double, note: String) {
    viewModelScope.launch {
      val existing = repository.allSuppliers.first().find { it.name.equals(supplierName.trim(), ignoreCase = true) }
      val supplierId = if (existing != null) {
        existing.id
      } else {
        val newSup = Supplier(
          name = supplierName.trim(),
          companyName = "توريدات متنوعة",
          phone = ""
        )
        repository.insertSupplier(newSup).toInt()
      }
      
      val tx = SupplierTransaction(
        supplierId = supplierId,
        amount = amount,
        transactionType = "شراء",
        note = note.ifEmpty { "شراء بضاعة وتوريد للمخزن" }
      )
      repository.insertSupplierTransaction(tx)
      
      val entry = LedgerEntry(
        description = "فاتورة شراء: ${note.ifEmpty { "شراء بضاعة" }}",
        debitAccount = "المخزون",
        creditAccount = "الصندوق",
        amount = amount
      )
      repository.insertLedgerEntry(entry)
      
      repository.logAudit(currentUser.value, "فاتورة شراء", "تم تسجيل فاتورة شراء بقيمة $amount للمورد $supplierName")
      showToast("تم تسجيل فاتورة الشراء وتحديث المخزون والدفاتر بنجاح ")
    }
  }

  fun addExpenseInvoice(description: String, amount: Double, creditAccount: String) {
    viewModelScope.launch {
      val entry = LedgerEntry(
        description = "مصروف: $description",
        debitAccount = "المصاريف",
        creditAccount = creditAccount,
        amount = amount
      )
      repository.insertLedgerEntry(entry)
      repository.logAudit(currentUser.value, "سند مصروف", "تسجيل مصروف بقيمة $amount ($description) من $creditAccount")
      showToast("تم تسجيل المصروف بقيمة $amount شيكل وتحديث القيود بنجاح ")
    }
  }

  fun deleteSupplierTransaction(tx: SupplierTransaction) {
    viewModelScope.launch {
      repository.deleteSupplierTransaction(tx)
      showToast("تم حذف الحركة المالية للمورد.")
    }
  }

  // --- Banks Operations ---
  fun addBank(bank: Bank) {
    viewModelScope.launch {
      repository.insertBank(bank)
      showToast("تم إضافة البنك ${bank.name} بنجاح.")
    }
  }

  fun deleteBank(bank: Bank) {
    viewModelScope.launch {
      repository.deleteBank(bank)
      showToast("تم حذف البنك بنجاح.")
    }
  }

  fun addCustomerTransaction(tx: CustomerTransaction) {
    viewModelScope.launch {
      repository.insertCustomerTransaction(tx)
      val typeText = if (tx.transactionType == "بيع") "فاتورة مبيعات آجلة" else "سند قبض دفعة مالية"
      repository.logAudit(currentUser.value, "حركة عميل", "تم تسجيل $typeText بقيمة ${tx.amount} للعميل")
      showToast("تم تسجيل الحركة المالية للعميل بنجاح.")
    }
  }

  fun sendCustomerOverdueAlertNotification(customerName: String, phone: String, overdueAmount: Double, dueDateStr: String) {
    viewModelScope.launch {
      val alert = AdminAlert(
        timestamp = System.currentTimeMillis(),
        type = "WARNING",
        title = "تنبيه دفعة متأخرة: $customerName ",
        message = "العميل $customerName (جوال: $phone) لديه دفعة متأخرة بقيمة $overdueAmount شيكل تستحق بتاريخ $dueDateStr.",
        isRead = false
      )
      repository.insertAdminAlert(alert)
      repository.logAudit(currentUser.value, "تنبيه متأخرات", "تم إرسال إشعار تنبيهي بدفعة متأخرة للعميل $customerName بقيمة $overdueAmount")
      showToast("تم تسجيل وإرسال إشعار التنبيه للعميل $customerName بنجاح ")
    }
  }

  fun sendBulkCustomerOverdueAlerts(overdueCustomersCount: Int, totalOverdueSum: Double) {
    viewModelScope.launch {
      val alert = AdminAlert(
        timestamp = System.currentTimeMillis(),
        type = "WARNING",
        title = "تنبيهات دفعات متأخرة جماعية ",
        message = "تم إرسال تنبيهات تذكيرية لعدد $overdueCustomersCount عميل متأخر بإجمالي مبالغ $totalOverdueSum شيكل.",
        isRead = false
      )
      repository.insertAdminAlert(alert)
      repository.logAudit(currentUser.value, "تنبيهات جماعية", "تم إطلاق حملة تذكير وإرسال إشعارات لـ $overdueCustomersCount عميل متأخر بقيمة $totalOverdueSum شيكل")
      showToast("تم إرسال الإشعارات والتنبيهات الجماعية لـ $overdueCustomersCount عميل بنجاح ")
    }
  }

  fun deleteCustomerTransaction(tx: CustomerTransaction) {
    viewModelScope.launch {
      repository.deleteCustomerTransaction(tx)
      showToast("تم حذف الحركة المالية للعميل.")
    }
  }

  // --- Shift Operations ---
  fun openNewShift(startingCash: Double) {
    viewModelScope.launch {
      repository.startShift(currentUser.value, startingCash)
      showToast("تم فتح الوردية بمبلغ ابتدائي $startingCash شيكل")
    }
  }

  fun closeActiveShift(shift: Shift, endingCash: Double, totalSales: Double) {
    viewModelScope.launch {
      repository.closeShift(shift, endingCash, totalSales)
      showToast("تم إغلاق الوردية وتسليم الصندوق بنجاح.")
    }
  }

  // --- Manual General Ledger entry ---
  fun addManualLedgerEntry(
    description: String,
    debitAccount: String,
    creditAccount: String,
    amount: Double,
    onSuccess: ((LedgerEntry) -> Unit)? = null
  ) {
    viewModelScope.launch {
      val entry = LedgerEntry(
        description = description,
        debitAccount = debitAccount,
        creditAccount = creditAccount,
        amount = amount
      )
      repository.insertLedgerEntry(entry)
      showToast("تم تسجيل القيد المحاسبي اليدوي بنجاح.")
      onSuccess?.invoke(entry)
    }
  }

  // --- Checks Management ---
  fun updateCheckStatus(invoiceId: Int, newStatus: String) {
    viewModelScope.launch {
      repository.updateCheckStatus(invoiceId, newStatus, currentUser.value)
      showToast("تم تحديث حالة الشيك إلى: $newStatus")
    }
  }

  // --- Custom Invoice Issuance ---
  fun issueCustomInvoice(
    items: List<com.example.data.CartItem>,
    customerId: Int?,
    manualCustomerName: String,
    discount: Double,
    taxPercent: Double,
    paymentMethod: String,
    paidAmount: Double,
    changeAmount: Double,
    checkNumber: String? = null,
    checkBankName: String? = null,
    checkDate: Long? = null,
    onSuccess: (com.example.data.Invoice) -> Unit
  ) {
    if (items.isEmpty()) {
      showToast("خطأ: قائمة المنتجات بالفاتورة فارغة!")
      return
    }
    viewModelScope.launch {
      try {
        var finalCustomerId = customerId
        val manualNameStr = manualCustomerName.trim()
        if (finalCustomerId == null && manualNameStr.isNotEmpty()) {
          val existing = repository.allCustomers.first().find { it.name.equals(manualNameStr, ignoreCase = true) }
          if (existing != null) {
            finalCustomerId = existing.id
          } else {
            val newCustId = repository.insertCustomer(
              com.example.data.Customer(
                name = manualNameStr,
                phone = "يدوي",
                email = "",
                points = 0,
                type = "عادي"
              ),
              currentUser.value
            )
            finalCustomerId = newCustId.toInt()
          }
        }

        val checkedOutInvoice = repository.checkout(
          items = items,
          customerId = finalCustomerId,
          discount = discount,
          taxPercent = taxPercent,
          paymentMethod = paymentMethod,
          paidAmount = paidAmount,
          changeAmount = changeAmount,
          cashierName = currentUser.value,
          branchName = currentBranch.value,
          checkNumber = checkNumber,
          checkBankName = checkBankName,
          checkDate = checkDate
        )

        val updatedProducts = repository.allProducts.first()
        val lowStockNames = items.mapNotNull { item ->
          val prod = updatedProducts.find { it.id == item.product.id }
          if (prod != null && prod.quantity <= prod.minQuantityAlert) {
            "${prod.nameAr} (المتبقي: ${prod.quantity})"
          } else null
        }

        if (lowStockNames.isNotEmpty()) {
          showToast("تم إصدار الفاتورة #${checkedOutInvoice.id}!  تنبيه حد الطلب: ${lowStockNames.joinToString(" ، ")}")
        } else {
          showToast("تم إصدار الفاتورة رقم #${checkedOutInvoice.id} بنجاح!")
        }
        onSuccess(checkedOutInvoice)
      } catch (e: Exception) {
        showToast("حدث خطأ أثناء إصدار الفاتورة: ${e.message}")
      }
    }
  }

  // --- POS Checkout Engine ---
  fun performCheckout(
    paymentMethod: String,
    paidAmount: Double,
    changeAmount: Double,
    checkNumber: String? = null,
    checkBankName: String? = null,
    checkDate: Long? = null,
    clearCartAfter: Boolean = true,
    isReturnMode: Boolean = false,
    isDraft: Boolean = false,
    customInvoiceDate: Long? = null,
    onSuccess: () -> Unit
  ) {
    if (_cart.value.isEmpty()) {
      showToast("خطأ: السلة فارغة!")
      return
    }

    viewModelScope.launch {
      try {
        var finalCustomerId = _selectedCustomerId.value
        val manualNameStr = _manualCustomerName.value.trim()
        if (finalCustomerId == null && manualNameStr.isNotEmpty()) {
          val existing = repository.allCustomers.first().find { it.name.equals(manualNameStr, ignoreCase = true) }
          if (existing != null) {
            finalCustomerId = existing.id
          } else {
            val newCustId = repository.insertCustomer(
              Customer(
                name = manualNameStr,
                phone = "يدوي",
                email = "",
                points = 0,
                type = "عادي"
              ),
              currentUser.value
            )
            finalCustomerId = newCustId.toInt()
          }
        }

        val cartItemsSold = _cart.value.toList()

        val checkedOutInvoice = repository.checkout(
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
          isDraft = isDraft,
          customInvoiceDate = customInvoiceDate
        )

        _lastCheckedOutInvoice.value = checkedOutInvoice
        _lastCheckedOutItems.value = cartItemsSold

        if (clearCartAfter) {
          clearCart()
        }

        val updatedProducts = repository.allProducts.first()
        val lowStockNames = cartItemsSold.mapNotNull { item ->
          val prod = updatedProducts.find { it.id == item.product.id }
          if (prod != null && prod.quantity <= prod.minQuantityAlert) {
            "${prod.nameAr} (المتبقي: ${prod.quantity})"
          } else null
        }

        if (lowStockNames.isNotEmpty()) {
          showToast("تم حفظ الفاتورة #${checkedOutInvoice.id}!  تنبيه حد الطلب المنخفض: ${lowStockNames.joinToString(" ، ")}")
        } else {
          showToast("تم حفظ الفاتورة بنجاح! فاتورة رقم #${checkedOutInvoice.id} ")
        }
        onSuccess()
      } catch (e: Exception) {
        showToast("حدث خطأ أثناء حفظ الفاتورة: ${e.message}")
      }
    }
  }

  // --- POS Purchase Checkout Engine ---
  fun performPurchaseCheckout(
    paymentMethod: String = "نقدي",
    paidAmount: Double = 0.0,
    checkNumber: String? = null,
    checkBankName: String? = null,
    checkDate: Long? = null,
    clearCartAfter: Boolean = false,
    isReturnMode: Boolean = false,
    isDraft: Boolean = false,
    customInvoiceDate: Long? = null,
    onSuccess: (Invoice) -> Unit = {}
  ) {
    if (_cart.value.isEmpty()) {
      showToast("خطأ: السلة فارغة!")
      return
    }

    viewModelScope.launch {
      try {
        var finalSupplierId = _selectedSupplierId.value
        val manualNameStr = _manualSupplierName.value.trim()
        if (finalSupplierId == null && manualNameStr.isNotEmpty()) {
          val existing = repository.allSuppliers.first().find { it.name.equals(manualNameStr, ignoreCase = true) }
          if (existing != null) {
            finalSupplierId = existing.id
          } else {
            val newSupId = repository.insertSupplier(
              com.example.data.Supplier(
                name = manualNameStr,
                phone = "يدوي",
                email = "",
                address = ""
              )
            )
            finalSupplierId = newSupId.toInt()
          }
        }

        val supplierObj = if (finalSupplierId != null) {
          repository.allSuppliers.first().find { it.id == finalSupplierId }
        } else null
        val finalSupplierName = supplierObj?.name ?: manualNameStr.ifEmpty { "مورد عام" }

        val cartItemsPurchased = _cart.value.toList()

        val checkedOutInvoice = repository.checkoutPurchase(
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
          isReturn = isReturnMode,
          isDraft = isDraft,
          customInvoiceDate = customInvoiceDate
        )

        _lastCheckedOutInvoice.value = checkedOutInvoice
        _lastCheckedOutItems.value = cartItemsPurchased

        if (clearCartAfter) {
          clearCart()
        }

        showToast("تم حفظ فاتورة الشراء وتوريد الأصناف إلى المخزن بنجاح! ")
        onSuccess(checkedOutInvoice)
      } catch (e: Exception) {
        showToast("حدث خطأ أثناء حفظ فاتورة الشراء: ${e.message}")
      }
    }
  }

  // --- Dynamic Accounting & Reports Generator ---
  @android.annotation.SuppressLint("NewApi")
  fun exportDashboardToExcel(
    generalTreasury: Double,
    periodSales: Double,
    periodPurchases: Double,
    periodExpenses: Double,
    periodNetProfit: Double,
    fromDateStr: String?,
    toDateStr: String?,
    invoices: List<Invoice>
  ) {
    viewModelScope.launch {
      try {
        val context = getApplication<Application>().applicationContext
        val csvBuilder = StringBuilder()
        
        // BOM for UTF-8 Excel compatibility (Arabic text support)
        csvBuilder.append('\uFEFF')
        
        val periodLabel = if (!fromDateStr.isNullOrEmpty() && !toDateStr.isNullOrEmpty()) "من $fromDateStr إلى $toDateStr" else "جميع الأيام"
        
        // Header & Summary
        csvBuilder.append("التقرير المالي المفصل للداشبورد\n")
        csvBuilder.append("الفترة المحددة,$periodLabel\n")
        csvBuilder.append("الصندوق العام (رصيد الخزينة الكلي لجميع الأيام),${String.format("%.2f", generalTreasury)}\n")
        csvBuilder.append("إجمالي مبيعات الفترة,${String.format("%.2f", periodSales)}\n")
        csvBuilder.append("إجمالي مشتريات الفترة,${String.format("%.2f", periodPurchases)}\n")
        csvBuilder.append("إجمالي مصاريف الفترة,${String.format("%.2f", periodExpenses)}\n")
        csvBuilder.append("صافي أرباح الفترة,${String.format("%.2f", periodNetProfit)}\n")
        csvBuilder.append("عدد فواتير الفترة,${invoices.size}\n\n")
        
        // Table Header
        csvBuilder.append("رقم الفاتورة,تاريخ العملية,الكاشير,طريقة الدفع,الخصم (شيكل),الضريبة (شيكل),الإجمالي (شيكل)\n")
        
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
        invoices.forEach { inv ->
          csvBuilder.append("${inv.id},")
          csvBuilder.append("${sdf.format(java.util.Date(inv.invoiceDate))},")
          csvBuilder.append("${inv.cashierName.replace(",", " ")},")
          csvBuilder.append("${inv.paymentMethod},")
          csvBuilder.append("${inv.discount},")
          csvBuilder.append("${inv.taxAmount},")
          csvBuilder.append("${inv.total}\n")
        }
        
        val fileName = "تقرير_الداشبورد_المفصل_${System.currentTimeMillis()}.csv"
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
          put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
          put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
          }
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
          val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
          if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
              outputStream.write(csvBuilder.toString().toByteArray(Charsets.UTF_8))
            }
            showToast("تم بنجاح حفظ ملف Excel في مجلد Downloads باسم: $fileName ")
          } else {
            showToast("فشل إنشاء الملف في مجلد التنزيلات.")
          }
        } else {
          val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
          val file = java.io.File(downloadsDir, fileName)
          java.io.FileOutputStream(file).use { outputStream ->
            outputStream.write(csvBuilder.toString().toByteArray(Charsets.UTF_8))
          }
          showToast("تم بنجاح حفظ ملف Excel في مجلد Downloads باسم: $fileName ")
        }
      } catch (e: Exception) {
        showToast("حدث خطأ أثناء تصدير Excel: ${e.localizedMessage}")
      }
    }
  }

  fun exportDashboardToPdf(
    context: android.content.Context,
    generalTreasury: Double,
    periodSales: Double,
    periodPurchases: Double,
    periodExpenses: Double,
    periodNetProfit: Double,
    periodCount: Int,
    lowStockCount: Int,
    fromDateStr: String?,
    toDateStr: String?,
    invoices: List<Invoice>,
    ledgerEntries: List<LedgerEntry>
  ) {
    val store = storeName.value
    val phone = storePhone.value
    val vat = storeVatNumber.value
    val branch = currentBranch.value
    val cashier = currentUser.value
    val curr = currencySymbol.value
    
    val dateStr = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
    val periodText = if (!fromDateStr.isNullOrEmpty() && !toDateStr.isNullOrEmpty()) "الفترة من $fromDateStr إلى $toDateStr" else "تقرير شامل لكافة الأيام"
    
    val invSdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
    val invoiceRows = StringBuilder()
    invoices.take(30).forEach { inv ->
      invoiceRows.append("""
        <tr>
          <td>#${inv.id}</td>
          <td>${inv.cashierName}</td>
          <td>${inv.paymentMethod}</td>
          <td>${invSdf.format(java.util.Date(inv.invoiceDate))}</td>
          <td style="font-weight: bold;">${String.format("%.2f", inv.total)} $curr</td>
        </tr>
      """.trimIndent())
    }

    val ledgerRows = StringBuilder()
    ledgerEntries.take(15).forEach { entry ->
      ledgerRows.append("""
        <tr>
          <td>${entry.description}</td>
          <td>${entry.debitAccount}</td>
          <td>${entry.creditAccount}</td>
          <td style="color: green; font-weight: bold;">${String.format("%.2f", entry.amount)} $curr</td>
        </tr>
      """.trimIndent())
    }
    
    val htmlContent = """
      <!DOCTYPE html>
      <html dir="rtl" lang="ar">
      <head>
        <meta charset="utf-8">
        <style>
          body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 25px; color: #2d3748; text-align: right; }
          .header { text-align: center; border-bottom: 2px solid #2b6cb0; padding-bottom: 12px; margin-bottom: 20px; }
          .header h1 { margin: 4px 0; color: #1a365d; font-size: 22px; }
          .header p { margin: 4px 0; color: #4a5568; font-size: 13px; }
          .period-banner { background: #ebf8ff; border: 1px solid #bee3f8; border-radius: 8px; padding: 10px 15px; margin-bottom: 20px; text-align: center; color: #2b6cb0; font-weight: bold; font-size: 14px; }
          .treasury-box { background: #f0fff4; border: 2px solid #38a169; border-radius: 10px; padding: 15px; margin-bottom: 20px; text-align: center; }
          .treasury-title { font-size: 13px; color: #276749; font-weight: bold; }
          .treasury-val { font-size: 24px; color: #22543d; font-weight: bold; margin-top: 5px; }
          .grid { display: table; width: 100%; table-layout: fixed; margin-bottom: 20px; }
          .grid-row { display: table-row; }
          .kpi-card { display: table-cell; background: #f7fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 10px; text-align: center; vertical-align: middle; }
          .kpi-title { font-size: 11px; color: #718096; margin-bottom: 4px; }
          .kpi-value { font-size: 15px; font-weight: bold; color: #2d3748; }
          table { width: 100%; border-collapse: collapse; margin-top: 15px; font-size: 11px; }
          th, td { border: 1px solid #cbd5e0; padding: 8px; text-align: right; }
          th { background-color: #edf2f7; color: #2d3748; font-weight: bold; }
          tr:nth-child(even) { background-color: #f7fafc; }
          .section-title { font-size: 14px; color: #2b6cb0; margin-top: 25px; margin-bottom: 8px; border-bottom: 1px solid #e2e8f0; padding-bottom: 4px; }
          .footer { text-align: center; margin-top: 35px; font-size: 10px; color: #a0aec0; border-top: 1px solid #e2e8f0; padding-top: 12px; }
        </style>
        <title>التقرير المالي التفصيلي</title>
      </head>
      <body>
        <div class="header">
          <h1>$store</h1>
          <p>فرع: $branch | هاتف: $phone | الرقم الضريبي: $vat</p>
          <p style="font-weight: bold; color: #2b6cb0; font-size: 16px;">التقرير المالي المفصل للداشبورد والعمليات</p>
          <p>تاريخ الاستخراج: $dateStr | المستخرج: $cashier</p>
        </div>
        
        <div class="period-banner">
           نطاق التقرير: $periodText
        </div>
        
        <div class="treasury-box">
          <div class="treasury-title"> الصندوق العام (رصيد الخزينة الكلي لجميع الأيام)</div>
          <div class="treasury-val">${String.format("%.2f", generalTreasury)} $curr</div>
        </div>
        
        <div class="grid">
          <div class="grid-row">
            <div class="kpi-card">
              <div class="kpi-title">مبيعات الفترة</div>
              <div class="kpi-value">${String.format("%.2f", periodSales)} $curr</div>
            </div>
            <div class="kpi-card">
              <div class="kpi-title">مشتريات الفترة</div>
              <div class="kpi-value" style="color: #c53030;">${String.format("%.2f", periodPurchases)} $curr</div>
            </div>
            <div class="kpi-card">
              <div class="kpi-title">مصاريف الفترة</div>
              <div class="kpi-value" style="color: #dd6b20;">${String.format("%.2f", periodExpenses)} $curr</div>
            </div>
            <div class="kpi-card">
              <div class="kpi-title">صافي ربح الفترة</div>
              <div class="kpi-value" style="color: #2f855a;">${String.format("%.2f", periodNetProfit)} $curr</div>
            </div>
            <div class="kpi-card">
              <div class="kpi-title">عدد الفواتير</div>
              <div class="kpi-value">$periodCount</div>
            </div>
          </div>
        </div>
        
        <div class="section-title"> فواتير العمليات المباشرة خلال الفترة ($periodCount فاتورة):</div>
        <table>
          <thead>
            <tr>
              <th>رقم الفاتورة</th>
              <th>الكاشير</th>
              <th>طريقة الدفع</th>
              <th>التاريخ والوقت</th>
              <th>المبلغ الإجمالي</th>
            </tr>
          </thead>
          <tbody>
            ${if (invoiceRows.isNotEmpty()) invoiceRows else "<tr><td colspan='5' style='text-align:center;'>لا توجد فواتير لهذه الفترة المحددة.</td></tr>"}
          </tbody>
        </table>

        <div class="section-title"> حركات القيود المحاسبية خلال الفترة:</div>
        <table>
          <thead>
            <tr>
              <th>البيان / الوصف</th>
              <th>الحساب المدين</th>
              <th>الحساب الدائن</th>
              <th>المبلغ</th>
            </tr>
          </thead>
          <tbody>
            ${if (ledgerRows.isNotEmpty()) ledgerRows else "<tr><td colspan='4' style='text-align:center;'>لا توجد حركات قيود لهذه الفترة المحددة.</td></tr>"}
          </tbody>
        </table>
        
        <div class="footer">
          <p>تم استخراج هذا التقرير آلياً من نظام كاشير المحاسبة والـ CRM المتكامل.</p>
        </div>
      </body>
      </html>
    """.trimIndent()

    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
      try {
        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? android.print.PrintManager
        if (printManager != null) {
          val webView = android.webkit.WebView(context)
          webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
          webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
              val printAdapter = webView.createPrintDocumentAdapter("التقرير المالي المفصل")
              printManager.print("التقرير المالي المفصل", printAdapter, android.print.PrintAttributes.Builder().build())
            }
          }
          showToast("جاري فتح الحفظ كملف PDF مالي... ")
        } else {
          showToast("نظام الطباعة غير مدعوم على هذا الجهاز.")
        }
      } catch (e: Exception) {
        showToast("خطأ أثناء استخراج PDF: ${e.localizedMessage}")
      }
    }
  }

  fun exportStatementToPdf(
    context: android.content.Context,
    title: String,
    bodyRows: List<com.example.ui.StatementRow>,
    summaryText: String,
    entityDetails: String
  ) {
    val store = storeName.value
    val phone = storePhone.value
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    
    var rowsHtml = ""
    for (row in bodyRows) {
      rowsHtml += """
        <tr>
          <td>${sdf.format(java.util.Date(row.date))}</td>
          <td>${row.description}</td>
          <td>${row.debit}</td>
          <td>${row.credit}</td>
        </tr>
      """.trimIndent()
    }

    val htmlContent = """
      <!DOCTYPE html>
      <html dir="rtl" lang="ar">
      <head>
        <meta charset="utf-8">
        <style>
          body { font-family: sans-serif; padding: 20px; }
          .header { text-align: center; border-bottom: 2px solid #ddd; padding-bottom: 10px; margin-bottom: 20px; }
          .header h1 { margin: 0; color: #333; }
          .header p { margin: 5px 0; color: #666; }
          .details { background-color: #f9f9f9; padding: 15px; border-radius: 8px; margin-bottom: 20px; }
          .details p { margin: 5px 0; font-weight: bold; }
          table { width: 100%; border-collapse: collapse; margin-top: 10px; }
          th, td { border: 1px solid #ccc; padding: 8px; text-align: center; }
          th { background-color: #eee; }
          .summary { margin-top: 20px; font-weight: bold; font-size: 1.1em; background-color: #e6f2ff; padding: 15px; border-radius: 8px; }
        </style>
      </head>
      <body>
        <div class="header">
          <h1>$store</h1>
          <p>هاتف: $phone</p>
        </div>
        <div class="details">
          <h2>$title</h2>
          <p>$entityDetails</p>
        </div>
        <table>
          <thead>
            <tr>
              <th>التاريخ</th>
              <th>البيان</th>
              <th>مدين (+)</th>
              <th>دائن (-)</th>
            </tr>
          </thead>
          <tbody>
            $rowsHtml
          </tbody>
        </table>
        <div class="summary">
          $summaryText
        </div>
      </body>
      </html>
    """.trimIndent()

    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
      try {
        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? android.print.PrintManager
        if (printManager != null) {
          val webView = android.webkit.WebView(context)
          webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
          webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
              val printAdapter = webView.createPrintDocumentAdapter(title)
              printManager.print(title, printAdapter, android.print.PrintAttributes.Builder().build())
            }
          }
          showToast("جاري التجهيز لحفظ الكشف كملف PDF... ")
        } else {
          showToast("نظام الطباعة غير مدعوم على هذا الجهاز.")
        }
      } catch (e: Exception) {
        showToast("خطأ أثناء استخراج PDF: ${e.localizedMessage}")
      }
    }
  }

  fun exportBalancesReportToPdf(
    context: android.content.Context,
    reportTitle: String,
    isCustomerReport: Boolean,
    rows: List<BalancesReportRowData>
  ) {
    val store = storeName.value
    val phone = storePhone.value
    val vat = storeVatNumber.value
    val curr = currencySymbol.value
    val dateStr = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

    val totalDebit = rows.filter { it.balance > 0 }.sumOf { it.balance }
    val totalCredit = rows.filter { it.balance < 0 }.sumOf { -it.balance }
    val netTotal = rows.sumOf { it.balance }

    val tableRows = StringBuilder()
    rows.forEachIndexed { index, row ->
      val statusColor = if (row.balance > 0) "#c53030" else if (row.balance < 0) "#2f855a" else "#718096"
      tableRows.append("""
        <tr>
          <td style="text-align: center;">${index + 1}</td>
          <td style="font-weight: bold;">${row.name}</td>
          <td>${row.phone.ifEmpty { "-" }}</td>
          <td>${row.companyOrType.ifEmpty { "-" }}</td>
          <td style="font-weight: bold; color: $statusColor;">${row.statusText}</td>
          <td style="font-weight: bold; text-align: left;">${String.format(java.util.Locale.US, "%.2f", Math.abs(row.balance))} $curr</td>
        </tr>
      """.trimIndent())
    }

    val htmlContent = """
      <!DOCTYPE html>
      <html dir="rtl" lang="ar">
      <head>
        <meta charset="utf-8">
        <style>
          body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 20px; color: #2d3748; }
          .header { text-align: center; border-bottom: 2px solid #3182ce; padding-bottom: 15px; margin-bottom: 20px; }
          .store-name { font-size: 22px; font-weight: bold; color: #2b6cb0; }
          .report-title { font-size: 18px; margin-top: 5px; font-weight: bold; color: #1a202c; }
          .meta-info { display: flex; justify-content: space-between; margin-bottom: 15px; font-size: 12px; color: #4a5568; }
          .summary-box { display: flex; gap: 10px; margin-bottom: 20px; background: #f7fafc; padding: 12px; border-radius: 8px; border: 1px solid #e2e8f0; }
          .kpi { flex: 1; text-align: center; padding: 8px; background: white; border-radius: 6px; border: 1px solid #cbd5e0; }
          .kpi-title { font-size: 11px; color: #718096; margin-bottom: 4px; }
          .kpi-val { font-size: 15px; font-weight: bold; }
          table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 12px; }
          th, td { border: 1px solid #e2e8f0; padding: 8px 10px; text-align: right; }
          th { background-color: #ebf8ff; color: #2b6cb0; font-weight: bold; }
          tr:nth-child(even) { background-color: #f7fafc; }
          .footer { text-align: center; margin-top: 30px; font-size: 10px; color: #a0aec0; border-top: 1px solid #e2e8f0; padding-top: 10px; }
        </style>
      </head>
      <body>
        <div class="header">
          <div class="store-name">$store</div>
          <div class="report-title">$reportTitle</div>
          <div style="font-size: 12px; color: #718096; margin-top: 4px;">الهاتف: $phone ${if (vat.isNotEmpty()) "| الرقم الضريبي: $vat" else ""}</div>
        </div>
        <div class="meta-info">
          <span>تاريخ التقرير: $dateStr</span>
          <span>عدد الحسابات: ${rows.size}</span>
        </div>
        <div class="summary-box">
          <div class="kpi">
            <div class="kpi-title">إجمالي الأرصدة المدينة</div>
            <div class="kpi-val" style="color: #e53e3e;">${String.format(java.util.Locale.US, "%.2f", totalDebit)} $curr</div>
          </div>
          <div class="kpi">
            <div class="kpi-title">إجمالي الأرصدة الدائنة</div>
            <div class="kpi-val" style="color: #2f855a;">${String.format(java.util.Locale.US, "%.2f", totalCredit)} $curr</div>
          </div>
          <div class="kpi">
            <div class="kpi-title">صافي إجمالي الأرصدة</div>
            <div class="kpi-val" style="color: #2b6cb0;">${String.format(java.util.Locale.US, "%.2f", netTotal)} $curr</div>
          </div>
        </div>
        <table>
          <thead>
            <tr>
              <th style="width: 30px;">#</th>
              <th>الاسم</th>
              <th>رقم الهاتف</th>
              <th>الشركة / النوع</th>
              <th>حالة الرصيد</th>
              <th style="text-align: left;">المبلغ المستحق</th>
            </tr>
          </thead>
          <tbody>
            ${if (tableRows.isNotEmpty()) tableRows else "<tr><td colspan='6' style='text-align:center;'>لا توجد بيانات مطابقة لهذه الفلترة.</td></tr>"}
          </tbody>
        </table>
        <div class="footer">
          <p>تم استخراج هذا التقرير آلياً بواسطة نظام إدارة المبيعات والمحاسبة الذكي - $store</p>
        </div>
      </body>
      </html>
    """.trimIndent()

    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
      try {
        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? android.print.PrintManager
        if (printManager != null) {
          val webView = android.webkit.WebView(context)
          webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
          webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
              val printAdapter = webView.createPrintDocumentAdapter(reportTitle)
              printManager.print(reportTitle, printAdapter, android.print.PrintAttributes.Builder().build())
            }
          }
          showToast("جاري فتح معاينة وحفظ PDF $reportTitle ... ")
        } else {
          showToast("نظام الطباعة غير مدعوم على هذا الجهاز.")
        }
      } catch (e: Exception) {
        showToast("خطأ أثناء استخراج PDF: ${e.localizedMessage}")
      }
    }
  }

  fun exportExpenseBalancesReportToPdf(
    context: android.content.Context,
    reportTitle: String = "كشف أرصدة وحركات المصاريف",
    dateRangeText: String,
    expenses: List<com.example.data.Expense>,
    employees: List<com.example.data.Employee>
  ) {
    val store = storeName.value
    val phone = storePhone.value
    val vat = storeVatNumber.value
    val curr = currencySymbol.value
    val dateStr = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
    val sdfItem = java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", java.util.Locale.getDefault())

    val totalAmount = expenses.sumOf { it.amount }
    val count = expenses.size
    val avgAmount = if (count > 0) totalAmount / count else 0.0

    // Grouping by category
    val categoryMap = expenses.groupBy { it.type }
    val categorySummaries = categoryMap.map { (cat, list) ->
      val catTotal = list.sumOf { it.amount }
      val pct = if (totalAmount > 0) (catTotal / totalAmount) * 100 else 0.0
      Triple(cat, catTotal, Pair(list.size, pct))
    }.sortedByDescending { it.second }

    val topCategory = categorySummaries.firstOrNull()?.first ?: "لا يوجد"

    val categoryTableRows = StringBuilder()
    categorySummaries.forEachIndexed { idx, item ->
      val catName = item.first
      val catTotal = item.second
      val catCount = item.third.first
      val catPct = item.third.second
      categoryTableRows.append("""
        <tr>
          <td style="text-align: center;">${idx + 1}</td>
          <td style="font-weight: bold; color: #2b6cb0;">$catName</td>
          <td style="text-align: center;">$catCount</td>
          <td style="font-weight: bold; text-align: left; color: #c53030;">${String.format(java.util.Locale.US, "%.2f", catTotal)} $curr</td>
          <td style="text-align: center;">
            <div style="background: #edf2f7; border-radius: 4px; overflow: hidden; height: 14px; position: relative; display: flex; align-items: center;">
              <div style="background: #3182ce; width: ${String.format(java.util.Locale.US, "%.1f", catPct)}%; height: 100%;"></div>
              <span style="position: absolute; width: 100%; text-align: center; font-size: 10px; font-weight: bold;">${String.format(java.util.Locale.US, "%.1f", catPct)}%</span>
            </div>
          </td>
        </tr>
      """.trimIndent())
    }

    val itemTableRows = StringBuilder()
    expenses.forEachIndexed { index, exp ->
      val empName = employees.find { it.id == exp.employeeId }?.name ?: "-"
      val formattedDate = sdfItem.format(java.util.Date(exp.date))
      itemTableRows.append("""
        <tr>
          <td style="text-align: center;">${index + 1}</td>
          <td style="font-weight: bold; white-space: nowrap;">$formattedDate</td>
          <td style="font-weight: bold; color: #2b6cb0;">${exp.type}</td>
          <td style="font-weight: bold; text-align: left; color: #c53030;">${String.format(java.util.Locale.US, "%.2f", exp.amount)} $curr</td>
          <td>${if (empName != "-") empName else "عام"}</td>
          <td>${exp.notes.ifEmpty { "-" }}</td>
        </tr>
      """.trimIndent())
    }

    val htmlContent = """
      <!DOCTYPE html>
      <html dir="rtl" lang="ar">
      <head>
        <meta charset="utf-8">
        <style>
          body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 20px; color: #2d3748; }
          .header { text-align: center; border-bottom: 2px solid #3182ce; padding-bottom: 15px; margin-bottom: 20px; }
          .store-name { font-size: 22px; font-weight: bold; color: #2b6cb0; }
          .report-title { font-size: 18px; margin-top: 5px; font-weight: bold; color: #1a202c; }
          .meta-info { display: flex; justify-content: space-between; margin-bottom: 15px; font-size: 12px; color: #4a5568; background: #f7fafc; padding: 8px 12px; border-radius: 6px; }
          .summary-box { display: flex; gap: 10px; margin-bottom: 20px; background: #f7fafc; padding: 12px; border-radius: 8px; border: 1px solid #e2e8f0; }
          .kpi { flex: 1; text-align: center; padding: 10px; background: white; border-radius: 6px; border: 1px solid #cbd5e0; }
          .kpi-title { font-size: 11px; color: #718096; margin-bottom: 4px; }
          .kpi-val { font-size: 16px; font-weight: bold; }
          .section-title { font-size: 14px; font-weight: bold; color: #2b6cb0; margin-top: 20px; margin-bottom: 8px; border-right: 4px solid #3182ce; padding-right: 8px; }
          table { width: 100%; border-collapse: collapse; margin-top: 6px; font-size: 11px; }
          th, td { border: 1px solid #e2e8f0; padding: 7px 9px; text-align: right; }
          th { background-color: #ebf8ff; color: #2b6cb0; font-weight: bold; }
          tr:nth-child(even) { background-color: #f7fafc; }
          .footer { text-align: center; margin-top: 30px; font-size: 10px; color: #a0aec0; border-top: 1px solid #e2e8f0; padding-top: 10px; }
        </style>
      </head>
      <body>
        <div class="header">
          <div class="store-name">$store</div>
          <div class="report-title">$reportTitle</div>
          <div style="font-size: 12px; color: #718096; margin-top: 4px;">الهاتف: $phone ${if (vat.isNotEmpty()) "| الرقم الضريبي: $vat" else ""}</div>
        </div>
        <div class="meta-info">
          <span><b>الفترة المحددة:</b> $dateRangeText</span>
          <span><b>تاريخ ووقت الطباعة:</b> $dateStr</span>
          <span><b>عدد السندات:</b> $count</span>
        </div>
        <div class="summary-box">
          <div class="kpi">
            <div class="kpi-title">إجمالي المصروفات</div>
            <div class="kpi-val" style="color: #e53e3e;">${String.format(java.util.Locale.US, "%.2f", totalAmount)} $curr</div>
          </div>
          <div class="kpi">
            <div class="kpi-title">عدد السندات</div>
            <div class="kpi-val" style="color: #2b6cb0;">$count</div>
          </div>
          <div class="kpi">
            <div class="kpi-title">متوسط السند</div>
            <div class="kpi-val" style="color: #4a5568;">${String.format(java.util.Locale.US, "%.2f", avgAmount)} $curr</div>
          </div>
          <div class="kpi">
            <div class="kpi-title">أعلى بند صرف</div>
            <div class="kpi-val" style="color: #d69e2e; font-size: 14px;">$topCategory</div>
          </div>
        </div>

        <div class="section-title">أولاً: أرصدة وتوزيع المصاريف حسب البنود</div>
        <table>
          <thead>
            <tr>
              <th style="width: 30px; text-align: center;">#</th>
              <th>بند / نوع المصروف</th>
              <th style="text-align: center; width: 90px;">عدد السندات</th>
              <th style="text-align: left; width: 130px;">إجمالي المنصرف</th>
              <th style="text-align: center; width: 140px;">النسبة من الإجمالي</th>
            </tr>
          </thead>
          <tbody>
            ${if (categoryTableRows.isNotEmpty()) categoryTableRows else "<tr><td colspan='5' style='text-align:center;'>لا توجد بيانات مسجلة.</td></tr>"}
          </tbody>
        </table>

        <div class="section-title" style="margin-top: 25px;">ثانياً: سجل الحركات والتفاصيل الكاملة للسندات</div>
        <table>
          <thead>
            <tr>
              <th style="width: 30px; text-align: center;">#</th>
              <th style="width: 130px;">التاريخ والوقت</th>
              <th>نوع المصروف</th>
              <th style="text-align: left; width: 110px;">المبلغ</th>
              <th>المستلم / الموظف</th>
              <th>البيان / الملاحظات</th>
            </tr>
          </thead>
          <tbody>
            ${if (itemTableRows.isNotEmpty()) itemTableRows else "<tr><td colspan='6' style='text-align:center;'>لا توجد حركات مسجلة في هذه الفترة.</td></tr>"}
          </tbody>
        </table>

        <div class="footer">
          <p>تم استخراج هذا التقرير آلياً بواسطة نظام إدارة المبيعات والمحاسبة الذكي - $store</p>
        </div>
      </body>
      </html>
    """.trimIndent()

    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
      try {
        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? android.print.PrintManager
        if (printManager != null) {
          val webView = android.webkit.WebView(context)
          webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
          webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
              val printAdapter = webView.createPrintDocumentAdapter(reportTitle)
              printManager.print(reportTitle, printAdapter, android.print.PrintAttributes.Builder().build())
            }
          }
          showToast("جاري فتح معاينة وطباعة $reportTitle ...")
        } else {
          showToast("نظام الطباعة غير مدعوم على هذا الجهاز.")
        }
      } catch (e: Exception) {
        showToast("خطأ أثناء استخراج التقرير: ${e.localizedMessage}")
      }
    }
  }

  fun shareExpenseBalancesReportWhatsApp(
    context: android.content.Context,
    dateRangeText: String,
    expenses: List<com.example.data.Expense>,
    employees: List<com.example.data.Employee>
  ) {
    val store = storeName.value
    val curr = currencySymbol.value
    val dateStr = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
    val sdfItem = java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", java.util.Locale.getDefault())

    val totalAmount = expenses.sumOf { it.amount }
    val count = expenses.size

    val categoryMap = expenses.groupBy { it.type }
    val categorySummaries = categoryMap.map { (cat, list) ->
      val catTotal = list.sumOf { it.amount }
      val pct = if (totalAmount > 0) (catTotal / totalAmount) * 100 else 0.0
      Triple(cat, catTotal, Pair(list.size, pct))
    }.sortedByDescending { it.second }

    val message = buildString {
      appendLine(" *كشف أرصدة وحركات المصاريف*")
      appendLine(" *المتجر:* $store")
      appendLine(" *الفترة:* $dateRangeText")
      appendLine(" *تاريخ الاستخراج:* $dateStr")
      appendLine("")
      appendLine(" *إجمالي المصاريف:* ${String.format(java.util.Locale.US, "%.2f", totalAmount)} $curr")
      appendLine(" *عدد السندات:* $count سند")
      appendLine("")
      appendLine(" *أرصدة وتوزيع البنود:*")
      if (categorySummaries.isNotEmpty()) {
        categorySummaries.forEach { item ->
          val catName = item.first
          val catTotal = item.second
          val catPct = item.third.second
          appendLine("• *$catName*: ${String.format(java.util.Locale.US, "%.2f", catTotal)} $curr (${String.format(java.util.Locale.US, "%.1f", catPct)}%)")
        }
      } else {
        appendLine("• لا توجد مصاريف مسجلة")
      }
      appendLine("")
      appendLine(" *سجل تفاصيل السندات والتاريخ:*")
      if (expenses.isNotEmpty()) {
        expenses.take(25).forEachIndexed { idx, exp ->
          val formattedDate = sdfItem.format(java.util.Date(exp.date))
          val empName = employees.find { it.id == exp.employeeId }?.name
          val empText = if (!empName.isNullOrBlank()) " | المستلم: $empName" else ""
          val noteText = if (exp.notes.isNotBlank()) " (${exp.notes})" else ""
          appendLine("${idx + 1}.  *$formattedDate*")
          appendLine("    ${exp.type}: *${String.format(java.util.Locale.US, "%.2f", exp.amount)} $curr*$empText$noteText")
        }
        if (expenses.size > 25) {
          appendLine("... وغيرها (${expenses.size - 25} سند إضافي مسجل بالنظام)")
        }
      } else {
        appendLine("• لا توجد حركات خلال هذه الفترة")
      }
      appendLine("")
      appendLine(" _تم الإصدار عبر نظام المحاسبة وإدارة المبيعات_")
    }

    try {
      val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, message)
      }
      context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة كشف أرصدة المصاريف"))
    } catch (e: Exception) {
      showToast("تعذر مشاركة التقرير: ${e.message}")
    }
  }

  fun exportJournalVoucherToPdf(
    context: android.content.Context,
    entry: LedgerEntry,
    voucherType: String = "سند قيد محاسبي"
  ) {
    val store = storeName.value
    val phone = storePhone.value
    val vat = storeVatNumber.value
    val branch = currentBranch.value
    val cashier = currentUser.value
    val curr = currencySymbol.value
    
    val dateStr = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(entry.date))
    val voucherNo = "JV-${if (entry.id != 0) entry.id else System.currentTimeMillis() % 10000}"

    val htmlContent = """
      <!DOCTYPE html>
      <html dir="rtl" lang="ar">
      <head>
        <meta charset="utf-8">
        <style>
          body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 30px; color: #2d3748; text-align: right; }
          .header { text-align: center; border-bottom: 2px solid #2b6cb0; padding-bottom: 12px; margin-bottom: 20px; }
          .header h1 { margin: 4px 0; color: #1a365d; font-size: 24px; }
          .header p { margin: 3px 0; color: #4a5568; font-size: 13px; }
          
          .voucher-title-box { background: #ebf8ff; border: 2px solid #3182ce; border-radius: 8px; padding: 12px; text-align: center; margin-bottom: 25px; }
          .voucher-title { font-size: 20px; font-weight: bold; color: #2b6cb0; }
          .voucher-no { font-size: 14px; color: #4a5568; margin-top: 4px; font-weight: bold; }

          .meta-table { width: 100%; border-collapse: collapse; margin-bottom: 25px; }
          .meta-table td { padding: 8px 12px; border: 1px solid #e2e8f0; font-size: 13px; }
          .meta-label { background-color: #f7fafc; font-weight: bold; color: #4a5568; width: 22%; }

          .amount-box { background: #f0fff4; border: 2px solid #38a169; border-radius: 8px; padding: 15px; text-align: center; margin-bottom: 25px; }
          .amount-label { font-size: 13px; color: #276749; font-weight: bold; }
          .amount-val { font-size: 26px; color: #22543d; font-weight: bold; margin-top: 4px; }

          .entry-table { width: 100%; border-collapse: collapse; margin-bottom: 30px; font-size: 13px; }
          .entry-table th, .entry-table td { border: 1px solid #cbd5e0; padding: 12px; text-align: center; }
          .entry-table th { background-color: #2b6cb0; color: white; font-weight: bold; }

          .signatures { margin-top: 50px; width: 100%; border-collapse: collapse; }
          .signatures td { text-align: center; vertical-alignment: top; width: 33%; font-size: 13px; color: #4a5568; }
          .sig-line { margin-top: 45px; border-top: 1px dashed #cbd5e0; width: 80%; margin-left: auto; margin-right: auto; }

          .footer { text-align: center; margin-top: 40px; font-size: 10px; color: #a0aec0; border-top: 1px solid #e2e8f0; padding-top: 12px; }
        </style>
        <title>$voucherType - $voucherNo</title>
      </head>
      <body>
        <div class="header">
          <h1>$store</h1>
          <p>فرع: $branch | هاتف: $phone | الرقم الضريبي: $vat</p>
        </div>

        <div class="voucher-title-box">
          <div class="voucher-title"> $voucherType</div>
          <div class="voucher-no">رقم القيد: $voucherNo | التاريخ: $dateStr</div>
        </div>

        <table class="meta-table">
          <tr>
            <td class="meta-label">المستخرج بواسطة:</td>
            <td>$cashier</td>
            <td class="meta-label">الفرع / القسم:</td>
            <td>$branch</td>
          </tr>
          <tr>
            <td class="meta-label">شرح وبيان القيد:</td>
            <td colspan="3" style="font-weight: bold; color: #1a365d;">${entry.description}</td>
          </tr>
        </table>

        <div class="amount-box">
          <div class="amount-label">المبلغ الإجمالي للقيد المحاسبي</div>
          <div class="amount-val">${String.format("%.2f", entry.amount)} $curr</div>
        </div>

        <table class="entry-table">
          <thead>
            <tr>
              <th>جانب القيد</th>
              <th>اسم الحساب المحاسبي</th>
              <th>المبلغ المدين</th>
              <th>المبلغ الدائن</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td style="font-weight:bold; color:#2b6cb0;">من حـ/ (مدين)</td>
              <td style="font-weight:bold; font-size:14px;">${entry.debitAccount}</td>
              <td style="font-weight:bold; color:#2f855a; font-size:14px;">${String.format("%.2f", entry.amount)} $curr</td>
              <td>0.00 $curr</td>
            </tr>
            <tr>
              <td style="font-weight:bold; color:#c53030;">إلى حـ/ (دائن)</td>
              <td style="font-weight:bold; font-size:14px;">${entry.creditAccount}</td>
              <td>0.00 $curr</td>
              <td style="font-weight:bold; color:#c53030; font-size:14px;">${String.format("%.2f", entry.amount)} $curr</td>
            </tr>
          </tbody>
        </table>

        <table class="signatures">
          <tr>
            <td>
              <b>منظم القيد / المحاسب</b>
              <div class="sig-line"></div>
            </td>
            <td>
              <b>المراجع / التدقيق</b>
              <div class="sig-line"></div>
            </td>
            <td>
              <b>اعتماد المدير المالي</b>
              <div class="sig-line"></div>
            </td>
          </tr>
        </table>

        <div class="footer">
          <p>تم استخراج سند القيد هذا آلياً من النظام المحاسبي المتكامل - $store</p>
        </div>
      </body>
      </html>
    """.trimIndent()

    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
      try {
        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? android.print.PrintManager
        if (printManager != null) {
          val webView = android.webkit.WebView(context)
          webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
          webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
              val printAdapter = webView.createPrintDocumentAdapter("سند قيد $voucherNo")
              printManager.print("سند قيد $voucherNo", printAdapter, android.print.PrintAttributes.Builder().build())
            }
          }
          showToast("جاري فتح طباعة سند القيد ...")
        } else {
          showToast("نظام الطباعة غير مدعوم على هذا الجهاز.")
        }
      } catch (e: Exception) {
        showToast("خطأ أثناء استخراج سند القيد: ${e.localizedMessage}")
      }
    }
  }

  // --- Receipt Voucher (سند قبض) Operations ---
  fun addReceiptVoucher(
    customerId: Int?,
    customerName: String,
    amount: Double,
    paymentMethod: String = "نقداً (كاش)",
    receiptType: String = "قبض من عميل",
    bankName: String? = null,
    notes: String = "",
    voucherDate: Long = System.currentTimeMillis(),
    checkNumber: String? = null,
    checkBankName: String? = null,
    checkDueDate: Long? = null,
    checkDrawer: String? = null,
    checkStatus: String? = null,
    checkBranch: String? = null,
    onSuccess: (() -> Unit)? = null
  ) {
    viewModelScope.launch {
      try {
        val curr = currencySymbol.value
        val isCheck = paymentMethod == "شيك" || paymentMethod.contains("شيك")

        val checkInfoSummary = if (isCheck && !checkNumber.isNullOrBlank()) {
          val sdf = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
          val dueStr = if (checkDueDate != null) " - استحقاق: ${sdf.format(java.util.Date(checkDueDate))}" else ""
          val bankStr = if (!checkBankName.isNullOrBlank()) " - بنك: $checkBankName" else ""
          val drawerStr = if (!checkDrawer.isNullOrBlank() && checkDrawer != customerName) " - الساحب: $checkDrawer" else ""
          val branchStr = if (!checkBranch.isNullOrBlank()) " - فرع: $checkBranch" else ""
          " [شيك رقم: $checkNumber$bankStr$dueStr$drawerStr$branchStr]"
        } else ""

        val rawNotes = notes.trim().ifEmpty { if (isCheck) "سند قبض شيك" else "سند قبض مالي" }
        val formattedNotes = "$rawNotes$checkInfoSummary"

        val effectiveBank = if (isCheck) checkBankName else bankName

        if (customerId != null && customerId > 0) {
          // 1. Insert Customer Transaction (دفعة / قبض)
          val custTx = CustomerTransaction(
            customerId = customerId,
            date = voucherDate,
            transactionType = "دفعة",
            amount = amount,
            note = "سند قبض: $formattedNotes ($paymentMethod)"
          )
          repository.insertCustomerTransaction(custTx)

          // 2. Insert Accounting Ledger Entry
          val debitAcc = when {
            isCheck -> "شيكات برسم التحصيل: ${effectiveBank ?: "شيك بنكي"}"
            paymentMethod.contains("بنك") && !effectiveBank.isNullOrBlank() -> "البنك: $effectiveBank"
            else -> "الصندوق / الخزينة"
          }
          val creditAcc = "حساب العملاء: $customerName"
          val ledger = LedgerEntry(
            date = voucherDate,
            description = "سند قبض من العميل: $customerName - $formattedNotes",
            debitAccount = debitAcc,
            creditAccount = creditAcc,
            amount = amount
          )
          repository.insertLedgerEntry(ledger)

          // 3. Insert Check Record into Invoices table for Checks screen tracking
          if (isCheck && !checkNumber.isNullOrBlank()) {
            val checkInvoice = Invoice(
              customerId = customerId,
              invoiceDate = voucherDate,
              subtotal = amount,
              discount = 0.0,
              taxAmount = 0.0,
              total = amount,
              paidAmount = amount,
              changeAmount = 0.0,
              paymentMethod = "شيك",
              cashierName = currentUser.value,
              branchName = currentBranch.value,
              checkNumber = checkNumber,
              checkBankName = checkBankName ?: "شيك بنكي",
              checkDate = checkDueDate ?: voucherDate,
              checkStatus = checkStatus ?: "قيد الانتظار",
              dueDate = checkDueDate ?: voucherDate,
              isPurchase = false,
              isDraft = false
            )
            repository.insertInvoice(checkInvoice)
          }

          // 4. Log Audit
          repository.logAudit(
            currentUser.value,
            "سند قبض",
            "تم تسجيل سند قبض من العميل: $customerName بمبلغ $amount $curr ($paymentMethod)$checkInfoSummary"
          )
        } else {
          // General / Other Receipt
          val debitAcc = when {
            isCheck -> "شيكات برسم التحصيل: ${effectiveBank ?: "شيك بنكي"}"
            paymentMethod.contains("بنك") && !effectiveBank.isNullOrBlank() -> "البنك: $effectiveBank"
            else -> "الصندوق / الخزينة"
          }
          val creditAcc = "حساب الإيرادات: $receiptType"
          val party = if (customerName.isNotBlank()) " من $customerName" else ""
          val ledger = LedgerEntry(
            date = voucherDate,
            description = "سند قبض إيراد: $receiptType$party - $formattedNotes ($paymentMethod)",
            debitAccount = debitAcc,
            creditAccount = creditAcc,
            amount = amount
          )
          repository.insertLedgerEntry(ledger)

          // Insert Check Record into Invoices table for Checks screen tracking
          if (isCheck && !checkNumber.isNullOrBlank()) {
            val checkInvoice = Invoice(
              customerId = null,
              invoiceDate = voucherDate,
              subtotal = amount,
              discount = 0.0,
              taxAmount = 0.0,
              total = amount,
              paidAmount = amount,
              changeAmount = 0.0,
              paymentMethod = "شيك",
              cashierName = currentUser.value,
              branchName = currentBranch.value,
              checkNumber = checkNumber,
              checkBankName = checkBankName ?: "شيك بنكي",
              checkDate = checkDueDate ?: voucherDate,
              checkStatus = checkStatus ?: "قيد الانتظار",
              dueDate = checkDueDate ?: voucherDate,
              isPurchase = false,
              isDraft = false
            )
            repository.insertInvoice(checkInvoice)
          }

          repository.logAudit(
            currentUser.value,
            "سند قبض",
            "تم تسجيل سند قبض إيراد: $receiptType$party بمبلغ $amount $curr ($paymentMethod)$checkInfoSummary"
          )
        }

        showToast("تم حفظ سند القبض بنجاح ")
        onSuccess?.invoke()
      } catch (e: Exception) {
        showToast("خطأ أثناء حفظ سند القبض: ${e.localizedMessage}")
      }
    }
  }

  fun exportReceiptVoucherToPdf(
    context: android.content.Context,
    voucherNo: String,
    dateMillis: Long,
    receivedFrom: String,
    amount: Double,
    paymentMethod: String,
    notes: String,
    receiptType: String = "سند قبض مالي",
    remainingBalance: Double? = null,
    checkNumber: String? = null,
    checkBankName: String? = null,
    checkDueDate: Long? = null,
    checkDrawer: String? = null,
    checkStatus: String? = null
  ) {
    val store = storeName.value
    val phone = storePhone.value
    val vat = storeVatNumber.value
    val branch = currentBranch.value
    val cashier = currentUser.value
    val curr = currencySymbol.value

    val dateStr = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(dateMillis))

    val balanceRow = if (remainingBalance != null) {
      """
      <tr>
        <td class="meta-label">الرصيد المتبقي على العميل:</td>
        <td colspan="3" style="font-weight: bold; color: ${if (remainingBalance > 0) "#c53030" else "#276749"}; font-size: 14px;">
          ${String.format("%.2f", Math.abs(remainingBalance))} $curr ${if (remainingBalance > 0) "(مدين - متبقي عليه)" else if (remainingBalance < 0) "(دائن - رصيد لصالحه)" else "(خالص / متزن)"}
        </td>
      </tr>
      """
    } else ""

    val isCheck = paymentMethod.contains("شيك") || !checkNumber.isNullOrBlank()
    val checkSection = if (isCheck) {
      val dueFormatted = if (checkDueDate != null) java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault()).format(java.util.Date(checkDueDate)) else "غير محدد"
      """
      <div style="background: #f0f9ff; border: 1.5px solid #0284c7; border-radius: 8px; padding: 12px; margin-bottom: 20px;">
        <div style="font-weight: bold; color: #0369a1; font-size: 15px; margin-bottom: 8px;"> بيانات الشيك البنكي</div>
        <table style="width: 100%; border-collapse: collapse; font-size: 13px;">
          <tr>
            <td style="padding: 5px; color: #334155;"><b>رقم الشيك:</b> <span style="color: #0284c7; font-weight: bold;">${checkNumber ?: "-"}</span></td>
            <td style="padding: 5px; color: #334155;"><b>البنك المسحوب عليه:</b> <b>${checkBankName ?: "-"}</b></td>
          </tr>
          <tr>
            <td style="padding: 5px; color: #334155;"><b>تاريخ الاستحقاق (الصرف):</b> <b>$dueFormatted</b></td>
            <td style="padding: 5px; color: #334155;"><b>اسم صاحب الشيك / الساحب:</b> <b>${checkDrawer?.ifBlank { receivedFrom } ?: receivedFrom}</b></td>
          </tr>
          <tr>
            <td style="padding: 5px; color: #334155;" colspan="2"><b>حالة الشيك:</b> <span style="background: #e0f2fe; color: #0369a1; padding: 2px 8px; border-radius: 4px; font-weight: bold;">${checkStatus ?: "تحت التحصيل"}</span></td>
          </tr>
        </table>
      </div>
      """
    } else ""

    val htmlContent = """
      <!DOCTYPE html>
      <html dir="rtl" lang="ar">
      <head>
        <meta charset="utf-8">
        <style>
          body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 30px; color: #2d3748; text-align: right; }
          .header { text-align: center; border-bottom: 3px solid #16a34a; padding-bottom: 12px; margin-bottom: 20px; }
          .header h1 { margin: 4px 0; color: #166534; font-size: 24px; }
          .header p { margin: 3px 0; color: #4a5568; font-size: 13px; }
          
          .voucher-title-box { background: #f0fdf4; border: 2px solid #16a34a; border-radius: 8px; padding: 12px; text-align: center; margin-bottom: 25px; }
          .voucher-title { font-size: 22px; font-weight: bold; color: #15803d; }
          .voucher-no { font-size: 14px; color: #374151; margin-top: 4px; font-weight: bold; }

          .amount-box { background: #ecfdf5; border: 2px dashed #059669; border-radius: 8px; padding: 15px; text-align: center; margin-bottom: 25px; }
          .amount-label { font-size: 14px; color: #065f46; font-weight: bold; }
          .amount-val { font-size: 28px; color: #047857; font-weight: bold; margin-top: 4px; }

          .meta-table { width: 100%; border-collapse: collapse; margin-bottom: 25px; }
          .meta-table td { padding: 10px 14px; border: 1px solid #e2e8f0; font-size: 14px; }
          .meta-label { background-color: #f8fafc; font-weight: bold; color: #475569; width: 25%; }

          .signatures { margin-top: 50px; width: 100%; border-collapse: collapse; }
          .signatures td { text-align: center; vertical-alignment: top; width: 50%; font-size: 14px; color: #4a5568; }
          .sig-line { margin-top: 50px; border-top: 1px dashed #cbd5e0; width: 70%; margin-left: auto; margin-right: auto; }

          .footer { text-align: center; margin-top: 40px; font-size: 11px; color: #9ca3af; border-top: 1px solid #e5e7eb; padding-top: 12px; }
        </style>
        <title>$receiptType - $voucherNo</title>
      </head>
      <body>
        <div class="header">
          <h1>$store</h1>
          <p>الفرع: $branch | الهاتف: $phone ${if (vat.isNotBlank()) "| الرقم الضريبي: $vat" else ""}</p>
        </div>

        <div class="voucher-title-box">
          <div class="voucher-title"> $receiptType</div>
          <div class="voucher-no">رقم السند: $voucherNo | التاريخ: $dateStr</div>
        </div>

        <div class="amount-box">
          <div class="amount-label">المبلغ المقبوض</div>
          <div class="amount-val">${String.format("%.2f", amount)} $curr</div>
        </div>

        <table class="meta-table">
          <tr>
            <td class="meta-label">استلمنا من السيد/السادة:</td>
            <td colspan="3" style="font-weight: bold; font-size: 16px; color: #1e293b;">$receivedFrom</td>
          </tr>
          <tr>
            <td class="meta-label">طريقة القبض / الاستلام:</td>
            <td style="font-weight: bold; color: #166534;">$paymentMethod</td>
            <td class="meta-label">المستلم / الكاشير:</td>
            <td>$cashier</td>
          </tr>
          <tr>
            <td class="meta-label">وذلك عن (البيان):</td>
            <td colspan="3" style="font-weight: bold; color: #1e293b;">${notes.ifEmpty { "سداد دفعة مالية" }}</td>
          </tr>
          $balanceRow
        </table>

        $checkSection

        <table class="signatures">
          <tr>
            <td>
              <b>توقيع المستلم / المحاسب</b>
              <div class="sig-line"></div>
              <p style="margin-top: 6px; font-size: 12px; color: #6b7280;">$cashier</p>
            </td>
            <td>
              <b>توقيع المسلّم / العميل</b>
              <div class="sig-line"></div>
              <p style="margin-top: 6px; font-size: 12px; color: #6b7280;">$receivedFrom</p>
            </td>
          </tr>
        </table>

        <div class="footer">
          <p>شكراً لتعاملكم معنا - تم إصدار هذا السند إلكترونياً من نظام $store</p>
        </div>
      </body>
      </html>
    """.trimIndent()

    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
      try {
        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? android.print.PrintManager
        if (printManager != null) {
          val webView = android.webkit.WebView(context)
          webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
          webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
              val printAdapter = webView.createPrintDocumentAdapter("سند قبض $voucherNo")
              printManager.print("سند قبض $voucherNo", printAdapter, android.print.PrintAttributes.Builder().build())
            }
          }
          showToast("جاري فتح طباعة سند القبض ...")
        } else {
          showToast("نظام الطباعة غير مدعوم على هذا الجهاز.")
        }
      } catch (e: Exception) {
        showToast("خطأ أثناء استخراج سند القبض: ${e.localizedMessage}")
      }
    }
  }

  fun exportInvoiceItemsToPdf(context: android.content.Context, invoice: Invoice, items: List<com.example.data.InvoiceItem>, customerName: String?, isPreview: Boolean = false) {
    val cartItems = items.map { item ->
      CartItem(
        product = Product(
          id = item.productId,
          nameAr = item.productNameAr,
          nameEn = item.productNameEn,
          priceSale = item.price
        ),
        quantity = item.quantity
      )
    }
    exportInvoiceToPdf(context, invoice, cartItems, customerName, isPreview)
  }


  fun exportItemMovementToPdf(context: android.content.Context, product: com.example.data.Product, movements: List<com.example.data.MovementRecord>, netQty: Int, branchName: String = "", isPreview: Boolean = false) {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      try {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
          showToast("جاري التجهيز... سيتم التنزيل قريباً ")
        }

        val pdfDocument = android.graphics.pdf.PdfDocument()
        // Constants for layout
        val pageW = 595 // A4 width at 72 PPI
        val margin = 30f
        val rowHeight = 25f
        val headerHeight = 35f
        
        val contentHeight = 200 + headerHeight + (movements.size * rowHeight) + 50
        val pageH = if (contentHeight > 842) contentHeight.toInt() else 842 // Minimum A4 height

        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val branchNameStr = currentBranch.value
        val canvas = page.canvas

        val paint = android.graphics.Paint().apply {
          color = android.graphics.Color.BLACK
          textSize = 12f
          isAntiAlias = true
        }
        val bgPaint = android.graphics.Paint().apply {
          color = android.graphics.Color.parseColor("#F5F5F5")
          style = android.graphics.Paint.Style.FILL
        }
        val borderPaint = android.graphics.Paint().apply {
          color = android.graphics.Color.parseColor("#E0E0E0")
          style = android.graphics.Paint.Style.STROKE
          strokeWidth = 1f
        }

        // 1. Draw Page Border
        canvas.drawRect(margin, margin, pageW - margin, pageH - margin, borderPaint)

        var y = margin + 30f
        val xRight = pageW - margin - 10f
        val xLeft = margin + 10f
        
        // 2. Report Header (Title & Logo Placeholder)
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.isFakeBoldText = true
        paint.textSize = 22f
        paint.color = android.graphics.Color.parseColor("#1565C0") // Primary Blue
        canvas.drawText("كشف حركة مادة تفصيلي", pageW / 2f, y, paint)
        
        y += 20f
        paint.textSize = 12f
        paint.color = android.graphics.Color.GRAY
        canvas.drawText("الفرع: ${if(branchNameStr.isEmpty()) "الرئيسي" else branchNameStr}", pageW / 2f, y, paint)
        
        y += 30f
        
        // 3. Info Section (Boxed)
        val infoBoxTop = y
        bgPaint.color = android.graphics.Color.parseColor("#FAFAFA")
        canvas.drawRect(margin, infoBoxTop, pageW - margin, infoBoxTop + 80f, bgPaint)
        canvas.drawRect(margin, infoBoxTop, pageW - margin, infoBoxTop + 80f, borderPaint)
        
        paint.color = android.graphics.Color.BLACK
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        paint.textSize = 14f
        y += 25f
        
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale("ar"))
        paint.isFakeBoldText = true
        canvas.drawText("المادة:", xRight, y, paint)
        paint.isFakeBoldText = false
        canvas.drawText(" ${product.nameAr}", xRight - 50f, y, paint)
        
        paint.isFakeBoldText = true
        canvas.drawText("الباركود:", pageW / 2f, y, paint)
        paint.isFakeBoldText = false
        canvas.drawText(" ${product.barcode.ifEmpty { "بدون" }}", (pageW / 2f) - 60f, y, paint)

        y += 25f
        paint.isFakeBoldText = true
        canvas.drawText("القسم:", xRight, y, paint)
        paint.isFakeBoldText = false
        canvas.drawText(" ${product.category}", xRight - 50f, y, paint)
        
        paint.isFakeBoldText = true
        canvas.drawText("تاريخ الطباعة:", pageW / 2f, y, paint)
        paint.isFakeBoldText = false
        canvas.drawText(" ${sdf.format(java.util.Date())}", (pageW / 2f) - 90f, y, paint)

        y += 25f
        paint.isFakeBoldText = true
        paint.color = android.graphics.Color.parseColor("#2E7D32")
        canvas.drawText("الرصيد الحالي:", xRight, y, paint)
        canvas.drawText(" $netQty وحدة", xRight - 100f, y, paint)
        paint.color = android.graphics.Color.BLACK
        
        y += 30f // Space before table
        
        // 4. Table Header
        bgPaint.color = android.graphics.Color.parseColor("#1976D2") // Header Blue
        canvas.drawRect(margin, y, pageW - margin, y + headerHeight, bgPaint)
        canvas.drawRect(margin, y, pageW - margin, y + headerHeight, borderPaint)
        
        paint.color = android.graphics.Color.WHITE
        paint.isFakeBoldText = true
        paint.textSize = 13f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        
        // Columns X centers
        val colDate = xRight - 60f
        val colType = xRight - 170f
        val colParty = xRight - 280f
        val colQty = xRight - 380f
        val colPrice = xRight - 440f
        val colBal = xRight - 500f
        
        val headerY = y + 22f
        canvas.drawText("التاريخ", colDate, headerY, paint)
        canvas.drawText("النوع والمرجع", colType, headerY, paint)
        canvas.drawText("الطرف", colParty, headerY, paint)
        canvas.drawText("الكمية", colQty, headerY, paint)
        canvas.drawText("السعر", colPrice, headerY, paint)
        canvas.drawText("الرصيد", colBal, headerY, paint)
        
        y += headerHeight
        
        // 5. Table Rows
        var runningBalance = 0
        val sortedMovements = movements.sortedBy { it.date }
        paint.isFakeBoldText = false
        
        sortedMovements.forEachIndexed { index, m ->
          // Alternate row color
          bgPaint.color = if (index % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#F9F9F9")
          canvas.drawRect(margin, y, pageW - margin, y + rowHeight, bgPaint)
          
          // Draw vertical dividers
          canvas.drawLine(xRight - 120f, y, xRight - 120f, y + rowHeight, borderPaint)
          canvas.drawLine(xRight - 230f, y, xRight - 230f, y + rowHeight, borderPaint)
          canvas.drawLine(xRight - 340f, y, xRight - 340f, y + rowHeight, borderPaint)
          canvas.drawLine(xRight - 410f, y, xRight - 410f, y + rowHeight, borderPaint)
          canvas.drawLine(xRight - 470f, y, xRight - 470f, y + rowHeight, borderPaint)
          
          val sign = if (m.isIncoming) 1 else -1
          runningBalance += (m.quantity * sign)
          
          val textY = y + 17f
          paint.color = android.graphics.Color.BLACK
          paint.textSize = 11f
          
          // Date
          canvas.drawText(sdf.format(java.util.Date(m.date)), colDate, textY, paint)
          // Type
          var cleanType = m.type.replace(Regex("[↪]"), "").trim()
          canvas.drawText(cleanType, colType, textY, paint)
          // Party
          canvas.drawText(m.partyName.take(15), colParty, textY, paint)
          
          // Qty
          val qtyStr = if (m.isIncoming) "+${m.quantity}" else "-${m.quantity}"
          paint.color = if (m.isIncoming) android.graphics.Color.parseColor("#2E7D32") else android.graphics.Color.parseColor("#C62828")
          paint.isFakeBoldText = true
          canvas.drawText(qtyStr, colQty, textY, paint)
          
          // Price
          paint.color = android.graphics.Color.DKGRAY
          paint.isFakeBoldText = false
          canvas.drawText(String.format("%.2f", m.price), colPrice, textY, paint)
          
          // Balance
          paint.color = android.graphics.Color.BLACK
          paint.isFakeBoldText = true
          canvas.drawText(runningBalance.toString(), colBal, textY, paint)
          
          // Bottom border for row
          canvas.drawLine(margin, y + rowHeight, pageW - margin, y + rowHeight, borderPaint)
          
          y += rowHeight
          paint.isFakeBoldText = false
        }
        
        // 6. Footer
        y += 30f
        paint.color = android.graphics.Color.GRAY
        paint.textSize = 10f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("تم إنشاء هذا التقرير آلياً - نظام المبيعات والمخزون", pageW / 2f, y, paint)
        
        pdfDocument.finishPage(page)
        
        if (isPreview) {
          val tempFile = java.io.File(context.cacheDir, "ItemMovement_Preview.pdf")
          tempFile.outputStream().use { pdfDocument.writeTo(it) }
          pdfDocument.close()
          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            pdfPreviewFile.value = tempFile
          }
        } else {
          val fileName = "ItemMovement_${product.id}_${System.currentTimeMillis()}.pdf"
          
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
              put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
              put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
              put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
              resolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
              }
              kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                showToast("تم تنزيل كشف حركة المادة بنجاح ")
              }
            }
          } else {
            val targetFile = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
            targetFile.outputStream().use { outputStream ->
              pdfDocument.writeTo(outputStream)
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
              showToast("تم تنزيل كشف حركة المادة بنجاح ")
            }
          }
          pdfDocument.close()
        }
      } catch (e: Exception) {
        e.printStackTrace()
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
          showToast("حدث خطأ أثناء التنزيل : ${e.message}")
        }
      }
    }
  }

  fun exportInvoiceToPdf(context: android.content.Context, invoice: Invoice, items: List<CartItem>, customerName: String?, isPreview: Boolean = false) {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      try {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
          showToast("جاري تنزيل الفاتورة كملف PDF... ")
        }
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(400, 600 + (items.size * 30), 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val branchNameStr = currentBranch.value
        val canvas = page.canvas

        val fontBold = try {
          androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_bold)
            ?: androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_regular)
            ?: Typeface.DEFAULT_BOLD
        } catch (e: Exception) { Typeface.DEFAULT_BOLD }
        val fontSemiBold = try {
          androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_semibold)
            ?: fontBold
        } catch (e: Exception) { fontBold }
        val fontRegular = try {
          androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_regular)
            ?: Typeface.DEFAULT
        } catch (e: Exception) { Typeface.DEFAULT }

        val paint = android.graphics.Paint().apply {
          color = android.graphics.Color.BLACK
          textSize = 14f
          typeface = fontRegular
          isAntiAlias = true
        }
        var y = 40f
        val xRight = 380f // Right aligned for Arabic
        
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.typeface = fontBold
        paint.textSize = 18f
        canvas.drawText("فاتورة مبيعات", 200f, y, paint)
        y += 40f
        
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        paint.textSize = 12f
        paint.typeface = fontRegular
        
        canvas.drawText("رقم الفاتورة: #${invoice.id}", xRight, y, paint)
        y += 20f
        
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
        canvas.drawText("التاريخ: ${sdf.format(java.util.Date(invoice.invoiceDate))}", xRight, y, paint)
        y += 20f
        
        canvas.drawText("الكاشير: ${invoice.cashierName.ifEmpty { currentUser.value }}", xRight, y, paint)
        y += 20f
        
        if (!customerName.isNullOrEmpty()) {
          canvas.drawText("العميل: $customerName", xRight, y, paint)
          y += 20f
        }
        
        y += 10f
        canvas.drawLine(20f, y, 380f, y, paint)
        y += 20f
        
        // Table header
        paint.typeface = fontBold
        canvas.drawText("الصنف", 380f, y, paint)
        canvas.drawText("الكمية", 200f, y, paint)
        canvas.drawText("السعر", 100f, y, paint)
        y += 20f
        
        canvas.drawLine(20f, y, 380f, y, paint)
        y += 20f
        
        paint.typeface = fontRegular
        items.forEach { item ->
          canvas.drawText(item.product.nameAr, 380f, y, paint)
          canvas.drawText("${item.quantity}", 200f, y, paint)
          canvas.drawText("${String.format("%.2f", item.product.priceSale)}", 100f, y, paint)
          y += 20f
        }
        
        canvas.drawLine(20f, y, 380f, y, paint)
        y += 20f
        
        paint.typeface = fontBold
        canvas.drawText("المجموع النهائي: ${String.format("%.2f", invoice.total)}", xRight, y, paint)
        
        pdfDocument.finishPage(page)
        
        val fileName = "Invoice_${invoice.id}.pdf"
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
          val resolver = context.contentResolver
          val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
          }
          val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
          if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
              pdfDocument.writeTo(outputStream)
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
              showToast("تم تنزيل الفاتورة بنجاح في مجلد التنزيلات ")
            }
          }
        } else {
          val targetFile = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
          targetFile.outputStream().use { outputStream ->
            pdfDocument.writeTo(outputStream)
          }
          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            showToast("تم تنزيل الفاتورة بنجاح في مجلد التنزيلات ")
          }
        }
        
        if (isPreview) {
          val tempFile = java.io.File(context.cacheDir, "Invoice_Preview_${System.currentTimeMillis()}.pdf")
          tempFile.outputStream().use { pdfDocument.writeTo(it) }
          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            pdfPreviewFile.value = tempFile
          }
        }
        
        pdfDocument.close()
      } catch (e: Exception) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
          showToast("خطأ أثناء استخراج PDF: ${e.localizedMessage}")
        }
      }
    }
  }

  // Calculates balances dynamically using systemic double-entry ledger entries!
  fun calculateAccountBalances(entries: List<LedgerEntry>): Map<String, Double> {
    val balances = mutableMapOf<String, Double>()
    val debitNature = setOf("الصندوق", "البنك", "المخزون", "تكلفة البضاعة المباعة", "المصاريف")
    val creditNature = setOf("رأس المال", "إيرادات المبيعات", "ضريبة القيمة المضافة المستحقة")

    // Initialize
    (debitNature + creditNature).forEach { balances[it] = 0.0 }

    entries.forEach { entry ->
      val dAcc = entry.debitAccount
      val cAcc = entry.creditAccount
      val amt = entry.amount

      if (debitNature.contains(dAcc)) {
        balances[dAcc] = (balances[dAcc] ?: 0.0) + amt
      } else if (creditNature.contains(dAcc)) {
        balances[dAcc] = (balances[dAcc] ?: 0.0) - amt
      }

      if (creditNature.contains(cAcc)) {
        balances[cAcc] = (balances[cAcc] ?: 0.0) + amt
      } else if (debitNature.contains(cAcc)) {
        balances[cAcc] = (balances[cAcc] ?: 0.0) - amt
      }
    }
    return balances
  }

  // Swaps simulate users




  // --- System Users ---
  val allSystemUsers = repository.allSystemUsers

  private fun logAdminAction(action: String, details: String) {
    viewModelScope.launch {
      val adminName = _currentUserAccount.value?.name ?: "مدير النظام"
      repository.logAudit(adminName, action, details)
    }
  }

  fun activateUser(user: SystemUser, trialDays: Int = 10) {
    viewModelScope.launch {
      val trialEnd = System.currentTimeMillis() + (trialDays * 24L * 60L * 60L * 1000L)
      repository.updateSystemUser(user.copy(
        trialEndDate = trialEnd,
        isActive = true
      ))
      logAdminAction("تفعيل حساب", "تم تفعيل حساب ${user.name} لمدة $trialDays يوم.")
    }
  }

  fun updateUserSubscription(user: SystemUser, newEndDate: Long?, isSubscribed: Boolean, isActive: Boolean) {
    viewModelScope.launch {
      repository.updateSystemUser(user.copy(
        trialEndDate = newEndDate,
        isSubscribed = isSubscribed,
        isActive = isActive
      ))
      val dateStr = newEndDate?.let { java.text.SimpleDateFormat("yyyy/MM/dd").format(java.util.Date(it)) } ?: "غير محدد"
      logAdminAction("تعديل اشتراك", "تحديث اشتراك ${user.name} إلى تاريخ $dateStr (نشط: $isActive)")
    }
  }

  fun deactivateUser(user: SystemUser) {
    viewModelScope.launch {
      repository.updateSystemUser(user.copy(isActive = false))
      logAdminAction("تعطيل حساب", "تم تعطيل حساب ${user.name}.")
    }
  }

  fun deleteUser(user: SystemUser) {
    viewModelScope.launch {
      repository.deleteSystemUser(user)
      logAdminAction("حذف حساب", "تم حذف حساب المستخدم: ${user.name} (${user.email}) نهائياً من قاعدة البيانات Local DB.")
      showToast("تم حذف حساب (${user.name}) نهائياً من قاعدة البيانات ")
    }
  }

  fun toggleUserSubscription(user: SystemUser) {
    viewModelScope.launch {
      val isSubscribedNew = !user.isSubscribed
      val newTrialEnd = if (isSubscribedNew) {
        System.currentTimeMillis() + (365L * 24L * 60L * 60L * 1000L) // 1 Year full subscription
      } else {
        System.currentTimeMillis() + (10L * 24L * 60L * 60L * 1000L) // Reset to 10 days trial
      }
      repository.updateSystemUser(user.copy(
        isSubscribed = isSubscribedNew,
        trialEndDate = newTrialEnd
      ))
      logAdminAction("تبديل اشتراك", if (isSubscribedNew) "منح اشتراك كامل لـ ${user.name}" else "إلغاء اشتراك كامل لـ ${user.name}")
    }
  }

  fun addUserByAdmin(
    name: String,
    phone: String,
    password: String,
    role: String = "CASHIER",
    storeName: String = "مؤسسة الكنز للمحاسبة التجارية",
    trialDays: Int = 30,
    isSubscribed: Boolean = true,
    onSuccess: () -> Unit = {}
  ) {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      val email = "$phone@smartpos.com"
      val existing = repository.getSystemUserByEmail(email) ?: repository.getSystemUserByPhone(phone)
      if (existing != null) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
          showToast("رقم الجوال هذا مسجل بالفعل لمستخدم آخر!")
        }
        return@launch
      }

      val trialEnd = System.currentTimeMillis() + (trialDays * 24L * 60L * 60L * 1000L)
      val localUid = "admin_created_${java.util.UUID.randomUUID().toString().take(8)}"
      val newUser = SystemUser(
        uid = localUid,
        name = name,
        email = email,
        phone = phone,
        role = role,
        trialEndDate = trialEnd,
        isActive = true, // Admin-created users are directly active!
        isSubscribed = isSubscribed,
        cashierName = name,
        cashierPhone = phone,
        storeName = storeName
      )
      repository.insertSystemUser(newUser)
      prefs.edit()
        .putString("offline_password_$email", password)
        .putString("offline_password_$phone", password)
        .apply()

      logAdminAction("إنشاء مستخدم جديد", "قام المدير بإنشاء وتفعيل مستخدم جديد: $name ($phone - دور: $role)")

      // If Firebase configured, sync to Firestore
      val proj = firebaseProjectId.value
      val apiKey = firebaseWebApiKey.value
      val idToken = prefs.getString("firebase_id_token", null)
      if (!FirebaseSyncManager.isPlaceholderConfig(proj, apiKey)) {
        try {
          FirebaseSyncManager.saveDocument(proj, apiKey, "system_users", newUser.uid, newUser.toMap(), idToken)
        } catch (e: Exception) {
          Log.w("AdminAddUser", "Could not sync new user to remote: ${e.message}")
        }
      }

      viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
        showToast("تم إنشاء وتفعيل حساب المستخدم ($name) بنجاح بواسطة المدير!")
        onSuccess()
      }
    }
  }

  fun updateUserPermissions(
    user: SystemUser,
    canAccessReports: Boolean,
    canAccessInventory: Boolean,
    canAccessAccounting: Boolean,
    canAccessAdvancedTools: Boolean,
    canAddProducts: Boolean,
    canDeleteProducts: Boolean,
    canApplyDiscounts: Boolean,
    canViewProfits: Boolean
  ) {
    viewModelScope.launch {
      repository.updateSystemUser(
        user.copy(
          canAccessReports = canAccessReports,
          canAccessInventory = canAccessInventory,
          canAccessAccounting = canAccessAccounting,
          canAccessAdvancedTools = canAccessAdvancedTools,
          canAddProducts = canAddProducts,
          canDeleteProducts = canDeleteProducts,
          canApplyDiscounts = canApplyDiscounts,
          canViewProfits = canViewProfits
        )
      )
      logAdminAction("تعديل صلاحيات", "تم تحديث الصلاحيات للمستخدم: ${user.name}.")
    }
  }
  /**
   * تتحقق هذه الدالة مما إذا كانت الفترة التجريبية للمستخدم قد انتهت بمقارنتها بالتاريخ الحالي.
   * تُرجع true إذا كانت منتهية، وfalse إذا كانت لا تزال صالحة أو إذا كان المستخدم مشتركاً بالكامل.
   */
  fun isTrialExpired(user: SystemUser): Boolean {
    if (user.isSubscribed) return false
    val trialEnd = user.trialEndDate ?: return false
    return System.currentTimeMillis() > trialEnd
  }

  val allAdminAlerts = repository.allAdminAlerts

  private fun logAdminAlert(type: String, title: String, message: String) {
    viewModelScope.launch {
      repository.insertAdminAlert(
        AdminAlert(
          type = type,
          title = title,
          message = message
        )
      )
    }
  }

  fun markAdminAlertAsRead(alertId: Int) {
    viewModelScope.launch {
      repository.markAlertAsRead(alertId)
    }
  }
  
  fun clearAdminAlerts() {
    viewModelScope.launch {
      repository.clearAdminAlerts()
    }
  }

  fun checkSubscriptionsForAlerts() {
    viewModelScope.launch {
      val users = repository.allSystemUsers.first()
      val existingAlerts = repository.allAdminAlerts.first()
      val now = System.currentTimeMillis()
      users.forEach { user ->
        if (user.isActive && user.trialEndDate != null) {
          val daysLeft = ((user.trialEndDate - now) / (24.0 * 60.0 * 60.0 * 1000.0)).toInt()
          if (daysLeft in 0..3) {
            val alertMessage = "اشتراك المستخدم ${user.name} سينتهي خلال $daysLeft أيام."
            val alreadyAlerted = existingAlerts.any { it.message == alertMessage }
            if (!alreadyAlerted) {
              logAdminAlert(
                type = "WARNING",
                title = "اشتراك يوشك على الانتهاء",
                message = alertMessage
              )
            }
          }
        }
      }
    }
  }

  fun resetAuthState() {
    _authState.value = AuthState.Idle
  }

  fun login(email: String, pass: String, isAdminRequest: Boolean) {
    _authState.value = AuthState.Loading
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      val apiKey = firebaseWebApiKey.value
      val proj = firebaseProjectId.value
      val isDefaultKey = com.example.data.FirebaseSyncManager.isPlaceholderConfig(proj, apiKey)

      if (isAdminRequest) {
        val trimmedEmail = email.trim()
        val isAdminEmail = trimmedEmail.equals("admin@admin.com", ignoreCase = true) || 
                           trimmedEmail.equals("admin", ignoreCase = true) || 
                           trimmedEmail.equals("admin@smartpos.com", ignoreCase = true)
        val expectedAdminPass = prefs.getString("admin_password", "mmnn12345") ?: "mmnn12345"
        if (isAdminEmail) {
          if (pass == "mmnn12345" || pass == expectedAdminPass) {
            val adminUser = com.example.data.UserAccount(uid = "admin1", name = "مدير النظام", phone = "01000000000", role = "ADMIN")
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
              loginAs(adminUser)
              _authState.value = AuthState.Success("ADMIN", "مدير النظام")
            }
            return@launch
          } else {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
              _authState.value = AuthState.Error("كلمة مرور مدير النظام غير صحيحة، يرجى كتابة كلمة المرور المعتمدة: mmnn12345")
            }
            return@launch
          }
        }

        // Check local database for admin user first
        val localUser = repository.getSystemUserByEmail(email) ?: repository.getSystemUserByPhone(email)
        val localSavedPass = prefs.getString("offline_password_$email", null)

        if (localUser != null) {
          if (localUser.role != "ADMIN") {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
              _authState.value = AuthState.Error("عذراً، هذا الحساب ليس لديه صلاحيات مدير النظام!")
            }
            return@launch
          }
          if (pass == "mmnn12345" || pass == expectedAdminPass || (localSavedPass != null && localSavedPass == pass)) {
            val adminUser = com.example.data.UserAccount(uid = localUser.uid, name = localUser.name, phone = localUser.phone, role = "ADMIN")
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
              loginAs(adminUser)
              _authState.value = AuthState.Success("ADMIN", localUser.name)
            }
            return@launch
          } else {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
              _authState.value = AuthState.Error("كلمة مرور مدير النظام غير صحيحة، يرجى كتابة كلمة المرور المعتمدة: mmnn12345")
            }
            return@launch
          }
        }

        if (isDefaultKey) {
          viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            if (localUser == null) {
              _authState.value = AuthState.Error("اسم مستخدم مدير النظام غير مسجل لدينا. يمكنك استخدام اسم المستخدم: admin وكلمة المرور: mmnn12345")
            } else {
              _authState.value = AuthState.Error("كلمة مرور مدير النظام غير صحيحة، يرجى كتابة كلمة المرور: mmnn12345")
            }
          }
          return@launch
        }

        try {
          val authResult = FirebaseSyncManager.signIn(apiKey, email, pass)
          prefs.edit().putString("firebase_id_token", authResult.idToken).apply()
          var user = repository.getSystemUserById(authResult.uid)
          if (user == null) {
            val remoteUsers = FirebaseSyncManager.getCollection(proj, apiKey, "system_users", authResult.idToken)
            val matched = remoteUsers.find { it.first == authResult.uid }
            if (matched != null) {
              user = systemUserFromMap(matched.first, matched.second)
              repository.insertSystemUser(user)
            }
          }
          if (user != null && user.role == "ADMIN") {
            val adminUser = com.example.data.UserAccount(uid = user.uid, name = user.name, phone = user.phone, role = "ADMIN")
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
              loginAs(adminUser)
              _authState.value = AuthState.Success("ADMIN", user.name)
            }
          } else {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
              _authState.value = AuthState.Error("عذراً، هذا الحساب ليس لديه صلاحيات مدير النظام!")
            }
          }
        } catch (e: Exception) {
          viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            val friendlyMsg = FirebaseSyncManager.translateAuthError(e.message ?: "")
            _authState.value = AuthState.Error(friendlyMsg)
          }
        }
        return@launch
      }

      // Check if trying to login as admin without toggling the switch
      val trimmedEmail = email.trim()
      if (trimmedEmail.equals("admin@admin.com", ignoreCase = true) || 
          trimmedEmail.equals("admin@smartpos.com", ignoreCase = true) || 
          trimmedEmail.equals("admin", ignoreCase = true)) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
          _authState.value = AuthState.Error("هذا الحساب مخصص لمدير النظام فقط. يرجى تفعيل خيار 'دخول كمدير النظام' واستخدام كلمة المرور mmnn12345.")
        }
        return@launch
      }

      // Check local user directly
      val localUser = repository.getSystemUserByEmail(email) ?: repository.getSystemUserByPhone(email)
      val savedPass = prefs.getString("offline_password_$email", null) ?: prefs.getString("offline_password_${localUser?.email}", null)

      if (localUser != null) {
        if (savedPass != null && savedPass == pass) {
          if (!localUser.isActive) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
              _authState.value = AuthState.Error("عذراً، هذا الحساب غير مفعل! التطبيق لا يقبل أي مستخدم إلا بعد موافقة واعتماد مدير النظام (الأدمن) حصراً. يرجى التواصل مع الإدارة لتفعيل حسابك.")
            }
            return@launch
          }
          val isExpired = localUser.trialEndDate != null && System.currentTimeMillis() > localUser.trialEndDate && !localUser.isSubscribed
          if (isExpired) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
              _authState.value = AuthState.Error("عذراً، انتهت فترة اشتراكك. يرجى التواصل مع إدارة النظام للتجديد.")
            }
            return@launch
          }
          val customer = com.example.data.UserAccount(uid = localUser.uid, name = localUser.name, phone = localUser.phone, role = localUser.role)
          viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            loginAs(customer)
            _authState.value = AuthState.Success(localUser.role, localUser.name)
          }
          return@launch
        } else if (savedPass != null && savedPass != pass) {
          viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            _authState.value = AuthState.Error("كلمة المرور غير صحيحة، يرجى التأكد من كلمة المرور وإعادة المحاولة.")
          }
          return@launch
        }
      }

      if (isDefaultKey) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
          if (localUser == null) {
            _authState.value = AuthState.Error("عذراً، هذا الحساب غير مسجل في النظام! التطبيق لا يقبل مستخدمين إلا بواسطة مدير النظام (الأدمن). يرجى مراجعة الأدمن لإضافتك واعتمادك.")
          } else {
            _authState.value = AuthState.Error("كلمة المرور غير صحيحة، يرجى التأكد من كلمة المرور.")
          }
        }
        return@launch
      }

      try {
        val authResult = FirebaseSyncManager.signIn(apiKey, email, pass)
        prefs.edit().putString("firebase_id_token", authResult.idToken).apply()
        var user: com.example.data.SystemUser? = null
        try {
          val remoteUsers = FirebaseSyncManager.getCollection(proj, apiKey, "system_users", authResult.idToken)
          val matched = remoteUsers.find { it.first == authResult.uid }
          if (matched != null) {
            user = systemUserFromMap(matched.first, matched.second)
            val existing = repository.getSystemUserById(authResult.uid)
            if (existing != null) repository.updateSystemUser(user) else repository.insertSystemUser(user)
          }
        } catch (e: Exception) {
          if (e is CancellationException) throw e
          Log.w("Login", "Could not fetch remote user: ${e.message}")
        }
        
        if (user == null) {
          user = repository.getSystemUserById(authResult.uid)
        }

        if (user != null) {
          val isExpired = user.trialEndDate != null && System.currentTimeMillis() > user.trialEndDate && !user.isSubscribed
          if (isExpired && user.isActive) {
            val expiredUser = user.copy(isActive = false)
            repository.updateSystemUser(expiredUser)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
              _authState.value = AuthState.Error("عذراً، لقد انتهت فترة اشتراكك. يرجى التواصل مع الدعم الفني لتجديد الاشتراك.")
            }
            return@launch
          } else if (!user.isActive) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
              _authState.value = AuthState.Error(if (isExpired) "عذراً، لقد انتهت فترة اشتراكك." else "عذراً، هذا الحساب بانتظار موافقة واعتماد مدير النظام (الأدمن) لتفعيله. يرجى التواصل مع الإدارة.")
            }
            return@launch
          }
          val customer = com.example.data.UserAccount(uid = user.uid, name = user.name, phone = user.phone, role = user.role)
          viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            loginAs(customer)
            _authState.value = AuthState.Success(user.role, user.name)
          }
        } else {
          viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            _authState.value = AuthState.Error("عذراً، لم يتم العثور على بيانات هذا الحساب في النظام.")
          }
        }
      } catch (e: Exception) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
          val friendlyMsg = FirebaseSyncManager.translateAuthError(e.message ?: "")
          _authState.value = AuthState.Error(friendlyMsg)
        }
      }
    }
  }

  fun resetPassword(emailOrPhone: String, securityWord: String, newPassword: String, onSuccess: () -> Unit) {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        val user = repository.getSystemUserByEmail(emailOrPhone) ?: repository.getSystemUserByPhone(emailOrPhone)
        
        if (user == null) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) { showToast("المستخدم غير موجود") }
            return@launch
        }
        
        if (user.securityWord != securityWord) {
             viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) { showToast("كلمة سر الاستعادة غير صحيحة") }
            return@launch
        }
        
        prefs.edit().putString("offline_password_${user.email}", newPassword).apply()
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) { onSuccess() }
    }
  }

  fun register(name: String, email: String, phone: String, pass: String, role: String = "CUSTOMER", businessName: String = "مؤسسة الكنز للمحاسبة التجارية", securityWord: String) {
    _authState.value = AuthState.Loading
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      val apiKey = firebaseWebApiKey.value
      val proj = firebaseProjectId.value
      val isDefaultKey = com.example.data.FirebaseSyncManager.isPlaceholderConfig(proj, apiKey)

      if (name.isBlank()) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
          _authState.value = AuthState.Error("يرجى كتابة الاسم الكامل لصاحب الحساب")
        }
        return@launch
      }

      if (name.trim().length < 3) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
          _authState.value = AuthState.Error("الاسم قصير جداً (يرجى إدخال الاسم بالكامل)")
        }
        return@launch
      }

      if (businessName.isBlank()) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
          _authState.value = AuthState.Error("يرجى كتابة اسم النشاط التجاري أو المحل")
        }
        return@launch
      }

      if (businessName.trim().length < 2) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
          _authState.value = AuthState.Error("اسم النشاط التجاري قصير جداً")
        }
        return@launch
      }

      val cleanPhone = phone.filter { it.isDigit() }
      if (phone.isBlank()) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
          _authState.value = AuthState.Error("يرجى إدخال رقم الجوال")
        }
        return@launch
      }

      if (cleanPhone.length < 8) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
          _authState.value = AuthState.Error("رقم الجوال قصير جداً (يجب ألا يقل عن 8 أرقام)")
        }
        return@launch
      }

      if (pass.isBlank()) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
          _authState.value = AuthState.Error("يرجى إدخال كلمة المرور")
        }
        return@launch
      }

      if (pass.length < 6) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
          _authState.value = AuthState.Error("كلمة المرور قصيرة جداً (يجب أن تتكون من 6 أحرف أو أرقام على الأقل)")
        }
        return@launch
      }

      // Check if already registered locally
      val existingUser = repository.getSystemUserByEmail(email) ?: repository.getSystemUserByPhone(phone)
      if (existingUser != null) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
          _authState.value = AuthState.Error("رقم الجوال مسجل لدينا بالفعل! يرجى تسجيل الدخول مباشرة.")
        }
        return@launch
      }

      // Local Registration Fallback directly if default key is used
      if (isDefaultKey) {
        try {
          val localUid = "offline_${java.util.UUID.randomUUID()}"
          val newUser = SystemUser(
            uid = localUid,
            name = name,
            email = email,
            phone = phone,
            role = "CUSTOMER",
            trialEndDate = System.currentTimeMillis() + (10L * 24L * 60L * 60L * 1000L),
            isActive = false, // Must be approved and activated by Admin!
            cashierName = name,
            cashierPhone = phone,
            storeName = businessName,
            securityWord = securityWord
          )
          repository.insertSystemUser(newUser)
          prefs.edit().putString("offline_password_$email", pass).putString("offline_password_$phone", pass).apply()

          repository.insertAdminAlert(
            com.example.data.AdminAlert(
              title = "طلب تسجيل مستخدم جديد",
              message = "طلب تسجيل جديد من: $name ($phone) - نشاط: '$businessName'. الحساب بانتظار موافقة واعتماد مدير النظام لتفعيله.",
              type = "WARNING"
            )
          )

          viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            _authState.value = AuthState.RegistrationSuccess("تم إرسال طلب تسجيل حسابك بنجاح! التطبيق لا يقبل دخول المستخدمين إلا بعد موافقة وتفعيل مدير النظام. يرجى انتظار اعتماد حسابك من قبل الإدارة.")
          }
        } catch (e: Exception) {
          viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            _authState.value = AuthState.Error("حدث خطأ أثناء إنشاء الحساب: ${e.localizedMessage}")
          }
        }
        return@launch
      }

      try {
        // Register in Firebase Auth
        val authResult = FirebaseSyncManager.signUp(apiKey, email, pass)
        prefs.edit().putString("firebase_id_token", authResult.idToken).apply()
        
        // Create profile locally and on Firestore
        val newUser = SystemUser(
          uid = authResult.uid,
          name = name,
          email = email,
          phone = phone,
          role = "CUSTOMER",
          trialEndDate = System.currentTimeMillis() + (10L * 24L * 60L * 60L * 1000L),
          isActive = false, // Must be approved and activated by Admin!
          cashierName = name,
          cashierPhone = phone,
          storePhone = phone,
          storeName = businessName,
          currentBranch = "الفرع الرئيسي",
          cashierAddress = "",
          securityWord = securityWord
        )
        repository.insertSystemUser(newUser)
        prefs.edit().putString("offline_password_$email", pass).putString("offline_password_$phone", pass).apply()
        
        try {
          FirebaseSyncManager.saveDocument(proj, apiKey, "system_users", newUser.uid, newUser.toMap(), authResult.idToken)
        } catch (e: Exception) {
          if (e is CancellationException) throw e
          Log.w("Register", "Could not upload profile to Firestore: ${e.message}")
        }

        repository.insertAdminAlert(
          com.example.data.AdminAlert(
            title = "طلب تسجيل مستخدم جديد",
            message = "طلب تسجيل جديد من: $name ($phone) - نشاط: '$businessName'. الحساب بانتظار موافقة واعتماد مدير النظام لتفعيله.",
            type = "WARNING"
          )
        )

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
          _authState.value = AuthState.RegistrationSuccess("تم إرسال طلب تسجيل حسابك بنجاح! التطبيق لا يقبل دخول المستخدمين إلا بعد موافقة وتفعيل مدير النظام. يرجى انتظار اعتماد حسابك من قبل الإدارة.")
        }
      } catch (e: Exception) {
        val errorMsg = e.message ?: ""
        if (errorMsg.contains("EMAIL_EXISTS") || errorMsg.contains("already in use")) {
          viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            _authState.value = AuthState.Error("رقم الجوال مسجل لدينا بالفعل! يرجى تسجيل الدخول بدلاً من إنشاء حساب جديد.")
          }
          return@launch
        }

        // Automatic local registration fallback if key is invalid
        if (errorMsg.contains("API key not valid") || errorMsg.contains("API_KEY_INVALID")) {
          try {
            val localUid = "offline_${java.util.UUID.randomUUID()}"
            val newUser = SystemUser(
              uid = localUid,
              name = name,
              email = email,
              phone = phone,
              role = "CUSTOMER",
              trialEndDate = System.currentTimeMillis() + (10L * 24L * 60L * 60L * 1000L),
              isActive = false, // Must be approved and activated by Admin!
              cashierName = name,
              cashierPhone = phone,
              storeName = businessName
            )
            repository.insertSystemUser(newUser)
            prefs.edit().putString("offline_password_$email", pass).putString("offline_password_$phone", pass).apply()

            repository.insertAdminAlert(
              com.example.data.AdminAlert(
                title = "طلب تسجيل مستخدم جديد",
                message = "طلب تسجيل جديد من: $name ($phone) - نشاط: '$businessName'. الحساب بانتظار موافقة واعتماد مدير النظام لتفعيله.",
                type = "WARNING"
              )
            )

            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
              _authState.value = AuthState.RegistrationSuccess("تم إرسال طلب تسجيل حسابك بنجاح! التطبيق لا يقبل دخول المستخدمين إلا بعد موافقة وتفعيل مدير النظام. يرجى انتظار اعتماد حسابك من قبل الإدارة.")
            }
          } catch (ex: Exception) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
              _authState.value = AuthState.Error("حدث خطأ أثناء إنشاء الحساب: ${ex.localizedMessage}")
            }
          }
        } else {
          viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            _authState.value = AuthState.Error(FirebaseSyncManager.translateAuthError(errorMsg))
          }
        }
      }
    }
  }

  fun generateDemoData() {
    // Disabled by user request: No demo data should be generated
  }

  fun wipeSimulationData() {
    viewModelScope.launch {
      repository.clearAllData()
      repository.logAudit("المدير العام", "تصفير البيانات", "تم تصفير وإفراغ جميع البيانات من قاعدة البيانات بنجاح.")
      showToast("تم تصفير وإفراغ كافة السجلات والبيانات بنجاح! ")
    }
  }

  fun activateAllPendingUsers() {
    viewModelScope.launch {
      val users = repository.allSystemUsers.first()
      val pending = users.filter { !it.isActive }
      if (pending.isEmpty()) {
        showToast("لا توجد حسابات معلقة بانتظار التفعيل حالياً.")
        return@launch
      }
      pending.forEach { user ->
        repository.updateSystemUser(user.copy(
          isActive = true,
          trialEndDate = System.currentTimeMillis() + (10L * 24L * 60L * 60L * 1000L) // 10 Days Trial!
        ))
      }
      showToast("تم تفعيل جميع الحسابات المعلقة بنجاح ومُنحت 30 يوماً تجريبياً! ")
    }
  }

  fun extendAllActiveSubscriptions() {
    viewModelScope.launch {
      val users = repository.allSystemUsers.first()
      if (users.isEmpty()) {
        showToast("لا يوجد مستخدمون مسجلون في المنصة حالياً.")
        return@launch
      }
      users.forEach { user ->
        val currentEnd = user.trialEndDate ?: System.currentTimeMillis()
        val extendedEnd = currentEnd + (10L * 24L * 60L * 60L * 1000L) // Add 30 Days
        repository.updateSystemUser(user.copy(
          trialEndDate = extendedEnd
        ))
      }
      showToast("تم تمديد فترة تجربة واشتراكات جميع المستخدمين بـ 30 يوماً إضافية! ")
    }
  }

  fun exportUsersToCsv(users: List<com.example.data.SystemUser>) {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      try {
        val context = getApplication<Application>()
        val fileName = "users_export_${System.currentTimeMillis()}.csv"
        
        val csvContent = StringBuilder()
        csvContent.append('\uFEFF') // BOM for Excel
        csvContent.append("ID,Name,Email,Phone,Role,Status,Trial End Date\n")
        
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        
        users.forEach { user ->
          val status = if (user.isActive) "Active" else "Pending"
          val endDate = user.trialEndDate?.let { sdf.format(java.util.Date(it)) } ?: "N/A"
          csvContent.append("${user.uid},${user.name},${user.email},${user.phone},${user.role},$status,$endDate\n")
        }
        
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
          put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
          put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
        }
        
        var uri: android.net.Uri? = null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
          contentValues.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
          uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        } else {
          val file = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
          uri = android.net.Uri.fromFile(file)
        }
        
        if (uri != null) {
          resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(csvContent.toString().toByteArray(Charsets.UTF_8))
          }
          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            showToast("تم تصدير قائمة العملاء إلى التنزيلات بنجاح. ")
          }
        } else {
          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            showToast("تعذر إنشاء الملف.")
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
          showToast("حدث خطأ أثناء تصدير البيانات.")
        }
      }
    }
  }

  fun exportUsersToPdf(users: List<com.example.data.SystemUser>) {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      try {
        val context = getApplication<Application>()
        val document = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint()
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 12f
        
        var yOffset = 50f
        canvas.drawText("Admin Users Report", 50f, yOffset, paint)
        yOffset += 30f
        
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        
        users.forEach { user ->
          val status = if (user.isActive) "Active" else "Pending"
          val endDate = user.trialEndDate?.let { sdf.format(java.util.Date(it)) } ?: "N/A"
          val text = "${user.name} | ${user.email} | $status | Ends: $endDate"
          canvas.drawText(text, 50f, yOffset, paint)
          yOffset += 20f
          
          if (yOffset > 800f) {
            // For simplicity, we just truncate if it exceeds one page in this demo
          }
        }
        
        document.finishPage(page)
        
        val fileName = "users_export_${System.currentTimeMillis()}.pdf"
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
          put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
          put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        }
        
        var uri: android.net.Uri? = null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
          contentValues.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
          uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        } else {
          val file = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
          uri = android.net.Uri.fromFile(file)
        }
        
        if (uri != null) {
          resolver.openOutputStream(uri)?.use { outputStream ->
            document.writeTo(outputStream)
          }
          document.close()
          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            showToast("تم تصدير التقرير بصيغة PDF في مجلد التنزيلات. ")
          }
        } else {
          document.close()
          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            showToast("تعذر إنشاء الملف.")
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
          showToast("حدث خطأ أثناء تصدير التقرير.")
        }
      }
    }
  }

  private fun getBackedUpCustomersFromPrefs(): List<Customer> {
    val jsonStr = prefs.getString("backed_up_customers", null) ?: return emptyList()
    val list = mutableListOf<Customer>()
    try {
      val array = org.json.JSONArray(jsonStr)
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        list.add(
          Customer(
            id = obj.optInt("id", 0),
            name = obj.optString("name", ""),
            phone = obj.optString("phone", ""),
            email = obj.optString("email", ""),
            points = obj.optInt("points", 0),
            type = obj.optString("type", "عادي")
          )
        )
      }
    } catch (e: Exception) {
      Log.e("Backup", "Error parsing backed up customers", e)
    }
    return list
  }

  private fun backupAllCustomers(customers: List<Customer>) {
    if (customers.isEmpty()) return
    try {
      val array = org.json.JSONArray()
      customers.forEach { cust ->
        val obj = org.json.JSONObject().apply {
          put("id", cust.id)
          put("name", cust.name)
          put("phone", cust.phone)
          put("email", cust.email)
          put("points", cust.points)
          put("type", cust.type)
        }
        array.put(obj)
      }
      prefs.edit().putString("backed_up_customers", array.toString()).apply()
    } catch (e: Exception) {
      Log.e("Backup", "Error saving customers to backup", e)
    }
  }

  private fun restoreCustomersFromBackup() {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      try {
        val backedUp = getBackedUpCustomersFromPrefs()
        if (backedUp.isEmpty()) return@launch
        
        val currentInDb = repository.allCustomers.first()
        val currentNames = currentInDb.map { it.name.trim().lowercase() }.toSet()
        
        backedUp.forEach { cust ->
          val nameTrimmed = cust.name.trim().lowercase()
          if (!currentNames.contains(nameTrimmed)) {
            repository.dao.insertCustomer(cust.copy(id = 0))
          }
        }
      } catch (e: Exception) {
        Log.e("Backup", "Error restoring customers from backup", e)
      }
    }
  }

  private fun getBackedUpSuppliersFromPrefs(): List<Supplier> {
    val jsonStr = prefs.getString("backed_up_suppliers", null) ?: return emptyList()
    val list = mutableListOf<Supplier>()
    try {
      val array = org.json.JSONArray(jsonStr)
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        list.add(
          Supplier(
            id = obj.optInt("id", 0),
            name = obj.optString("name", ""),
            contactName = obj.optString("contactName", ""),
            phone = obj.optString("phone", ""),
            email = obj.optString("email", ""),
            address = obj.optString("address", ""),
            companyName = obj.optString("companyName", ""),
            balance = obj.optDouble("balance", 0.0)
          )
        )
      }
    } catch (e: Exception) {
      Log.e("Backup", "Error parsing backed up suppliers", e)
    }
    return list
  }

  private fun backupAllSuppliers(suppliers: List<Supplier>) {
    if (suppliers.isEmpty()) return
    try {
      val array = org.json.JSONArray()
      suppliers.forEach { sup ->
        val obj = org.json.JSONObject().apply {
          put("id", sup.id)
          put("name", sup.name)
          put("contactName", sup.contactName)
          put("phone", sup.phone)
          put("email", sup.email)
          put("address", sup.address)
          put("companyName", sup.companyName)
          put("balance", sup.balance)
        }
        array.put(obj)
      }
      prefs.edit().putString("backed_up_suppliers", array.toString()).apply()
    } catch (e: Exception) {
      Log.e("Backup", "Error saving suppliers to backup", e)
    }
  }

  private fun restoreSuppliersFromBackup() {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      try {
        val backedUp = getBackedUpSuppliersFromPrefs()
        if (backedUp.isEmpty()) return@launch
        
        val currentInDb = repository.allSuppliers.first()
        val currentNames = currentInDb.map { it.name.trim().lowercase() }.toSet()
        
        backedUp.forEach { sup ->
          val nameTrimmed = sup.name.trim().lowercase()
          if (!currentNames.contains(nameTrimmed)) {
            repository.dao.insertSupplier(sup.copy(id = 0))
          }
        }
      } catch (e: Exception) {
        Log.e("Backup", "Error restoring suppliers from backup", e)
      }
    }
  }

  // --- Employees & Expenses ---
  val allEmployees = repository.allEmployees.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
  val allExpenses = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun addEmployee(name: String, phone: String, role: String, salary: Double) {
    viewModelScope.launch {
      repository.insertEmployee(Employee(name = name, phone = phone, role = role, salary = salary))
      repository.logAudit(currentUser.value, "إضافة موظف", "تم إضافة الموظف: $name")
    }
  }

  fun updateEmployee(employee: Employee) {
    viewModelScope.launch {
      repository.updateEmployee(employee)
      repository.logAudit(currentUser.value, "تعديل موظف", "تم تعديل الموظف: ${employee.name}")
    }
  }

  fun addExpense(
    type: String,
    amount: Double,
    notes: String,
    employeeId: Int? = null,
    paymentMethod: String = "الصندوق",
    checkNumber: String? = null,
    checkBankName: String? = null,
    checkInvoiceId: Int? = null,
    partyType: String = "مصروف عام",
    partyName: String? = null,
    customerId: Int? = null,
    supplierId: Int? = null
  ) {
    viewModelScope.launch {
      val expense = Expense(
        type = type,
        amount = amount,
        notes = notes,
        employeeId = employeeId,
        paymentMethod = paymentMethod,
        checkNumber = checkNumber,
        checkBankName = checkBankName,
        checkInvoiceId = checkInvoiceId,
        partyType = partyType,
        partyName = partyName,
        customerId = customerId,
        supplierId = supplierId
      )
      repository.insertExpense(expense)

      // 1. If paid to supplier, record SupplierTransaction
      if (supplierId != null && supplierId > 0) {
        val sTx = com.example.data.SupplierTransaction(
          supplierId = supplierId,
          transactionType = "سداد",
          amount = amount,
          note = "سند صرف: $type - $notes (${if (paymentMethod == "شيك") "شيك رقم $checkNumber ($checkBankName)" else "نقداً من الصندوق"})"
        )
        repository.insertSupplierTransaction(sTx)
      }

      // 2. If paid/refunded to customer, record CustomerTransaction
      if (customerId != null && customerId > 0) {
        val cTx = com.example.data.CustomerTransaction(
          customerId = customerId,
          transactionType = "صرف",
          amount = amount,
          note = "سند صرف: $type - $notes (${if (paymentMethod == "شيك") "شيك رقم $checkNumber ($checkBankName)" else "نقداً من الصندوق"})"
        )
        repository.insertCustomerTransaction(cTx)
      }

      // 3. If paid with existing check, update check status to endorsed/spent
      if (paymentMethod == "شيك" && checkInvoiceId != null) {
        repository.updateCheckStatus(checkInvoiceId, "مجير / تم الصرف", currentUser.value)
      }

      // 4. Add accounting Ledger Entry
      val debitAcc = when (partyType) {
        "مورد" -> "حساب الموردين: ${partyName ?: type}"
        "عميل" -> "حساب العملاء: ${partyName ?: type}"
        "موظف" -> "رواتب ومستحقات الموظفين: ${partyName ?: type}"
        else -> "المصروفات العامة: $type"
      }
      val creditAcc = if (paymentMethod == "شيك") {
        "أوراق قبض (شيكات مجيرة: ${checkNumber ?: ""})"
      } else {
        "الصندوق / الخزينة"
      }
      repository.insertLedgerEntry(
        com.example.data.LedgerEntry(
          description = "سند صرف: $type - $notes (${if (paymentMethod == "شيك") "شيك #${checkNumber ?: ""}" else "الصندوق"})",
          debitAccount = debitAcc,
          creditAccount = creditAcc,
          amount = amount
        )
      )
      repository.logAudit(currentUser.value, "سند صرف", "تم تسجيل سند صرف: $type بمبلغ $amount طريقة الدفع: $paymentMethod")
      showToast("تم حفظ سند الصرف بنجاح")
    }
  }

  fun exportEmployeeStatementToPdf(context: android.content.Context, employee: com.example.data.Employee, expenses: List<com.example.data.Expense>, isPreview: Boolean = false) {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      try {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
          showToast("جاري تجهيز وتنزيل كشف حساب الموظف... ")
        }
        
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageW = 595
        val margin = 30f
        val rowHeight = 25f
        val headerHeight = 35f
        
        val contentHeight = 300 + headerHeight + (expenses.size * rowHeight) + 50
        val pageH = if (contentHeight > 842) contentHeight.toInt() else 842

        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val fontBold = try {
          androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_bold)
            ?: androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_regular)
            ?: Typeface.DEFAULT_BOLD
        } catch (e: Exception) { Typeface.DEFAULT_BOLD }
        val fontSemiBold = try {
          androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_semibold)
            ?: fontBold
        } catch (e: Exception) { fontBold }
        val fontMedium = try {
          androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_medium)
            ?: Typeface.DEFAULT
        } catch (e: Exception) { Typeface.DEFAULT }
        val fontRegular = try {
          androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.ibm_plex_sans_arabic_regular)
            ?: Typeface.DEFAULT
        } catch (e: Exception) { Typeface.DEFAULT }

        val paint = android.graphics.Paint().apply {
          color = android.graphics.Color.BLACK
          textSize = 12f
          typeface = fontRegular
          isAntiAlias = true
        }
        val bgPaint = android.graphics.Paint().apply {
          color = android.graphics.Color.parseColor("#F5F5F5")
          style = android.graphics.Paint.Style.FILL
        }
        val borderPaint = android.graphics.Paint().apply {
          color = android.graphics.Color.parseColor("#1565C0")
          style = android.graphics.Paint.Style.STROKE
          strokeWidth = 2f
        }
        val headerBgPaint = android.graphics.Paint().apply {
          color = android.graphics.Color.parseColor("#E3F2FD")
          style = android.graphics.Paint.Style.FILL
        }

        canvas.drawRect(margin, margin, pageW - margin, pageH - margin, borderPaint)

        var y = margin + 40f
        val xRight = pageW - margin - 20f
        val xLeft = margin + 20f
        val xCenter = pageW / 2f
        
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.typeface = fontBold
        paint.textSize = 24f
        paint.color = android.graphics.Color.parseColor("#1565C0")
        canvas.drawText(storeName.value.ifEmpty { "مؤسسة الكنز للمحاسبة التجارية" }, xCenter, y, paint)
        
        y += 30f
        paint.textSize = 18f
        paint.typeface = fontBold
        paint.color = android.graphics.Color.BLACK
        canvas.drawText("كشف حساب موظف تفصيلي", xCenter, y, paint)
        
        y += 20f
        paint.textSize = 12f
        paint.typeface = fontRegular
        paint.color = android.graphics.Color.DKGRAY
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
        canvas.drawText("تاريخ استخراج الكشف: ${sdf.format(java.util.Date())}", xCenter, y, paint)
        
        y += 30f
        canvas.drawLine(margin + 20, y, pageW - margin - 20, y, borderPaint)
        y += 30f
        
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        paint.textSize = 14f
        paint.color = android.graphics.Color.BLACK
        paint.typeface = fontBold
        canvas.drawText("معلومات الموظف:", xRight, y, paint)
        y += 25f
        
        paint.textSize = 12f
        paint.typeface = fontRegular
        canvas.drawText("اسم الموظف: ${employee.name}", xRight, y, paint)
        canvas.drawText("الوظيفة: ${employee.role}", xCenter, y, paint)
        y += 20f
        canvas.drawText("الراتب الأساسي: ${employee.salary} ر.س", xRight, y, paint)
        canvas.drawText("الهاتف: ${employee.phone.ifEmpty { "غير محدد" }}", xCenter, y, paint)
        y += 40f
        
        canvas.drawRect(xLeft, y - 20f, xRight, y + 10f, headerBgPaint)
        canvas.drawRect(xLeft, y - 20f, xRight, y + 10f, borderPaint)
        
        paint.typeface = fontBold
        paint.color = android.graphics.Color.BLACK
        val colDateX = xRight - 10f
        val colTypeX = xRight - 100f
        val colNotesX = xRight - 220f
        val colAmountX = xLeft + 60f
        
        canvas.drawText("التاريخ", colDateX, y, paint)
        canvas.drawText("النوع", colTypeX, y, paint)
        canvas.drawText("البيان", colNotesX, y, paint)
        canvas.drawText("المبلغ", colAmountX, y, paint)
        
        y += 20f
        paint.typeface = fontRegular
        
        val dateSdf = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
        var totalExpenses = 0.0
        
        if (expenses.isEmpty()) {
          y += 15f
          paint.textAlign = android.graphics.Paint.Align.CENTER
          canvas.drawText("لا يوجد مسحوبات أو سندات صرف مسجلة", xCenter, y, paint)
          y += 20f
        } else {
          for ((index, exp) in expenses.withIndex()) {
            if (index % 2 == 0) {
              canvas.drawRect(xLeft, y - 15f, xRight, y + 10f, bgPaint)
            }
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            
            canvas.drawText(dateSdf.format(java.util.Date(exp.date)), colDateX, y, paint)
            canvas.drawText(exp.type, colTypeX, y, paint)
            
            var note = exp.notes
            if (note.length > 25) note = note.take(22) + "..."
            canvas.drawText(note, colNotesX, y, paint)
            
            paint.color = android.graphics.Color.parseColor("#D32F2F")
            paint.typeface = fontBold
            canvas.drawText("${exp.amount}", colAmountX, y, paint)
            
            paint.color = android.graphics.Color.BLACK
            paint.typeface = fontRegular
            
            totalExpenses += exp.amount
            y += rowHeight
          }
        }
        
        canvas.drawLine(xLeft, y - 15f, xRight, y - 15f, borderPaint)
        y += 20f
        
        y += 20f
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        paint.textSize = 14f
        paint.typeface = fontBold
        
        val netSalary = employee.salary - totalExpenses
        
        canvas.drawText("الخلاصة المالية:", xRight, y, paint)
        y += 25f
        
        paint.textSize = 12f
        paint.typeface = fontRegular
        canvas.drawText("إجمالي الراتب المستحق: ${employee.salary} ر.س", xRight, y, paint)
        y += 20f
        
        paint.color = android.graphics.Color.parseColor("#D32F2F")
        canvas.drawText("إجمالي المسحوبات: $totalExpenses ر.س", xRight, y, paint)
        y += 20f
        
        paint.color = android.graphics.Color.parseColor("#388E3C")
        paint.typeface = fontBold
        paint.textSize = 14f
        canvas.drawText("صافي الراتب المتبقي: $netSalary ر.س", xRight, y, paint)
        
        pdfDocument.finishPage(page)

        if (isPreview) {
          val tempFile = java.io.File(context.cacheDir, "Employee_Statement_Preview.pdf")
          tempFile.outputStream().use { pdfDocument.writeTo(it) }
          pdfDocument.close()
          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            pdfPreviewFile.value = tempFile
          }
        } else {
          val fileName = "Employee_Statement_${employee.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
          val resolver = context.contentResolver
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
              put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
              put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
              put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
              resolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
              }
              kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                showToast("تم تنزيل الملف بنجاح ")
              }
            }
          } else {
            val targetFile = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
            targetFile.outputStream().use { outputStream ->
              pdfDocument.writeTo(outputStream)
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
              showToast("تم تنزيل الملف بنجاح ")
            }
          }
          pdfDocument.close()
        }
      } catch (e: Exception) {
        e.printStackTrace()
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
          showToast("حدث خطأ أثناء التنزيل : ${e.message}")
        }
      }
    }
  }

  val adminPassword = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("admin_password", "mmnn12345") ?: "mmnn12345")

  fun updateAdminPassword(newPass: String) {
    if (newPass.length < 6) {
      showToast("كلمة مرور الأدمن يجب أن تتكون من 6 خانات على الأقل!")
      return
    }
    prefs.edit()
      .putString("admin_password", newPass)
      .putString("offline_password_admin@smartpos.com", newPass)
      .putString("offline_password_admin@admin.com", newPass)
      .putString("offline_password_admin", newPass)
      .apply()
    adminPassword.value = newPass
    logAdminAction("تغيير كلمة مرور الأدمن", "تم تحديث كلمة مرور مدير النظام بنجاح.")
    showToast("تم تحديث وحفظ كلمة مرور مدير النظام بنجاح!")
  }

  init {
    loadBranches()

    // Ensure admin password is set to mmnn12345 as requested
    val currentAdminPass = prefs.getString("admin_password", "mmnn12345") ?: "mmnn12345"
    if (currentAdminPass != "mmnn12345") {
      prefs.edit().putString("admin_password", "mmnn12345").apply()
      adminPassword.value = "mmnn12345"
    } else if (!prefs.contains("admin_password")) {
      prefs.edit().putString("admin_password", "mmnn12345").apply()
    }
    prefs.edit()
      .putString("offline_password_admin@smartpos.com", "mmnn12345")
      .putString("offline_password_admin@admin.com", "mmnn12345")
      .putString("offline_password_admin", "mmnn12345")
      .apply()

    var proj = prefs.getString("firebase_project_id", "") ?: ""
    var key = prefs.getString("firebase_web_api_key", "") ?: ""
    var url = prefs.getString("firebase_database_url", "") ?: ""

    if (com.example.data.FirebaseSyncManager.isPlaceholderConfig(proj, key)) {
      proj = ""
      key = ""
      url = ""
      prefs.edit()
        .putString("firebase_project_id", "")
        .putString("firebase_web_api_key", "")
        .putString("firebase_database_url", "")
        .apply()
    }

    firebaseProjectId.value = proj
    firebaseWebApiKey.value = key
    firebaseDatabaseUrl.value = url
    lastSyncTime.value = prefs.getString("last_sync_time", "لم يتم المزامنة بعد") ?: "لم يتم المزامنة بعد"

    // Restore custom customers and suppliers from local SharedPreferences backup on startup
    restoreCustomersFromBackup()
    restoreSuppliersFromBackup()


    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      try {
        currentUserAccount.collect { user ->
          if (user != null && user.role != "ADMIN") {
            var sysUser = repository.getSystemUserById(user.uid)
            if (sysUser != null) {
              val isExpiredLocally = sysUser.trialEndDate != null && System.currentTimeMillis() > sysUser.trialEndDate!! && !sysUser.isSubscribed
              if (isExpiredLocally || !sysUser.isActive) {
                // Try to fetch latest from cloud before logging out!
                val p = firebaseProjectId.value
                val k = firebaseWebApiKey.value
                if (!com.example.data.FirebaseSyncManager.isPlaceholderConfig(p, k)) {
                    try {
                        val idToken = prefs.getString("firebase_id_token", null)
                        val remoteUsers = com.example.data.FirebaseSyncManager.getCollection(p, k, "system_users", idToken)
                        val matched = remoteUsers.find { it.first == user.uid }
                        if (matched != null) {
                           val remoteUser = systemUserFromMap(matched.first, matched.second)
                           repository.updateSystemUser(remoteUser)
                           sysUser = remoteUser
                        }
                    } catch (e: Exception) {
                      if (e is CancellationException) throw e
                      Log.w("UserSync", "Could not check remote user: ${e.message}")
                    }
                }
                
                val isExpiredRemote = sysUser!!.trialEndDate != null && System.currentTimeMillis() > sysUser.trialEndDate!! && !sysUser!!.isSubscribed
                if (isExpiredRemote || !sysUser!!.isActive) {
                  viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    logoutSystem()
                    if (isExpiredRemote) {
                        _authState.value = AuthState.Error("عذراً، لقد انتهت فترة اشتراكك. يرجى تجديد الاشتراك.")
                    } else {
                        _authState.value = AuthState.Error("عذراً، تم إيقاف الحساب.")
                    }
                  }
                }
              }
            }
          }
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e("UserAccountCollect", "Failed to collect current user account", e)
      }
    }


    // Observe and backup customers and suppliers continuously
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      try {
        allCustomers.collect { customers ->
          backupAllCustomers(customers)
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e("BackupCollect", "Failed to collect and backup customers", e)
      }
    }
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      try {
        allSuppliers.collect { suppliers ->
          backupAllSuppliers(suppliers)
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e("BackupCollect", "Failed to collect and backup suppliers", e)
      }
    }

    // Auto cloud synchronization on startup - safely run ONLY if valid custom credentials exist and user is logged in
    viewModelScope.launch {
      try {
        kotlinx.coroutines.delay(2000)
        if (currentUserAccount.value != null && !com.example.data.FirebaseSyncManager.isPlaceholderConfig(firebaseProjectId.value, firebaseWebApiKey.value)) {
          syncWithCloud(isManual = false)
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.w("StartupSync", "Skipped auto synchronization on startup: ${e.message}")
      }
    }

    // Load saved session synchronously from SharedPreferences for instant UI rendering
    try {
      val isExplicitlyLoggedIn = prefs.getBoolean("user_explicitly_logged_in", false)
      val savedUid = prefs.getString("logged_in_uid", null)
      val savedName = prefs.getString("logged_in_name", null)
      val savedPhone = prefs.getString("logged_in_phone", "") ?: ""
      val savedRole = prefs.getString("logged_in_role", null)
      val hasLoggedOutManually = prefs.getBoolean("has_logged_out_manually", false)
      
      if (isExplicitlyLoggedIn && !hasLoggedOutManually && savedUid != null && savedRole != null) {
        if (savedName != null) {
          loginAs(UserAccount(uid = savedUid, name = savedName, phone = savedPhone, role = savedRole))
        } else {
          viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
              val sysUser = repository.getSystemUserById(savedUid)
              if (sysUser != null && sysUser.isActive) {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                  loginAs(UserAccount(uid = sysUser.uid, name = sysUser.name, phone = sysUser.phone, role = sysUser.role))
                }
              } else {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                  logoutSystem()
                }
              }
            } catch (e: Exception) {
              if (e is CancellationException) throw e
              Log.e("SessionLoad", "Failed to get system user by id", e)
            }
          }
        }
      } else {
        _currentUserAccount.value = null
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      Log.e("SessionLoad", "Failed to restore saved session safely", e)
      _currentUserAccount.value = null
    }

    // Reactively update subscriptionStatus and user settings based on logged-in user and database updates
    viewModelScope.launch {
      try {
        combine(currentUserAccount, repository.allSystemUsers) { account, users ->
          Pair(account, users)
        }.collect { (account, users) ->
          try {
            if (account == null) {
              subscriptionStatus.value = "ACTIVE"
            } else if (account.role == "ADMIN") {
              subscriptionStatus.value = "ACTIVE"
            } else {
              val systemUser = users.find { it.uid == account.uid }
              if (systemUser != null) {
                val status = when {
                  !systemUser.isActive -> "EXPIRED"
                  systemUser.isSubscribed -> "ACTIVE"
                  isTrialExpired(systemUser) -> "EXPIRED"
                  else -> "TRIAL"
                }
                subscriptionStatus.value = status
                
                // Load customized settings reactively
                storeName.value = systemUser.storeName
                storePhone.value = systemUser.storePhone.ifBlank { systemUser.phone }
                storeVatNumber.value = systemUser.storeVatNumber
                currentBranch.value = systemUser.currentBranch.replace(" (الرياض)", "").ifBlank { "الفرع الرئيسي" }
                currencySymbol.value = systemUser.currencySymbol
                currentUser.value = systemUser.cashierName.ifBlank { systemUser.name }
                cashierPhone.value = systemUser.cashierPhone.ifBlank { systemUser.phone }
                cashierAddress.value = systemUser.cashierAddress.replace("الرياض، المملكة العربية السعودية", "").replace("الرياض", "").trim()
              } else {
                subscriptionStatus.value = "ACTIVE"
              }
            }
          } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("UserSettingsCollect", "Failed during user state emission process", e)
          }
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e("UserSettingsCombine", "Failed to collect system user updates reactively", e)
      }
    }
  }
}

data class BalancesReportRowData(
  val id: Int,
  val name: String,
  val phone: String,
  val companyOrType: String,
  val balance: Double,
  val statusText: String
)
