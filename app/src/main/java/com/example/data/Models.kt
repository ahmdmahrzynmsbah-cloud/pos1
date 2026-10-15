package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nameAr: String = "",
    val nameEn: String = "",
    val barcode: String = "",
    val pricePurchase: Double = 0.0,
    val priceSale: Double = 0.0,
    val quantity: Int = 0,
    val unit: String = "", // e.g., "قطعة", "كرتونة"
    val minQuantityAlert: Int = 0,
    val category: String = "",
    val imageResName: String = "" // Optional image reference
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nameAr: String = "",
    val nameEn: String = ""
)

data class ProductSaleHistory(
    val invoiceId: Int,
    val invoiceDate: Long,
    val quantity: Int,
    val price: Double,
    val total: Double
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int? = null,
    val invoiceDate: Long = System.currentTimeMillis(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val total: Double = 0.0,
    val paymentMethod: String = "", // "نقدي", "بطاقة", "محفظة", "مقسم", "شيك"
    val paidAmount: Double = 0.0,
    val changeAmount: Double = 0.0,
    val cashierName: String = "",
    val branchName: String = "",
    val checkNumber: String? = null,
    val checkBankName: String? = null,
    val checkDate: Long? = null,
    val checkStatus: String? = null, // "قيد الانتظار", "تم التحصيل", "مرفوض"
    val dueDate: Long? = null,
    val isPurchase: Boolean = false,
    val isDraft: Boolean = false
)

@Entity(tableName = "banks")
data class Bank(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val code: String = "",
    val name: String = ""
)

@Entity(tableName = "invoice_items")
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceId: Int = 0,
    val productId: Int = 0,
    val productNameAr: String = "",
    val productNameEn: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val total: Double = 0.0
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val points: Int = 0,
    val type: String = "عادي" // "VIP", "عادي", "جملة"
)

@Entity(tableName = "ledger_entries")
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val description: String = "",
    val debitAccount: String = "",
    val creditAccount: String = "",
    val amount: Double = 0.0,
    val employeeId: Int? = null
)

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeName: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val startingCash: Double = 0.0,
    val endingCash: Double = 0.0,
    val salesAmount: Double = 0.0,
    val isClosed: Boolean = false
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val employeeName: String = "",
    val action: String = "",
    val details: String = ""
)


@Entity(tableName = "admin_alerts")
data class AdminAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "WARNING", // WARNING, ERROR, INFO
    val title: String = "",
    val message: String = "",
    val isRead: Boolean = false
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val contactName: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val companyName: String = "",
    val balance: Double = 0.0 // positive: we owe them, negative: we prepaid
)

@Entity(tableName = "supplier_transactions")
data class SupplierTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val supplierId: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val transactionType: String = "شراء", // "شراء" (Purchase), "دفعة" (Payment)
    val amount: Double = 0.0,
    val note: String = ""
)

@Entity(tableName = "customer_transactions")
data class CustomerTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val transactionType: String = "بيع", // "بيع" (Sale Invoice), "دفعة" (Payment / Receipt)
    val invoiceId: Int? = null,
    val amount: Double = 0.0,
    val note: String = "",
    val dueDate: Long? = null
)


data class MovementRecord(
    val id: String,
    val date: Long,
    val type: String,
    val isIncoming: Boolean,
    val quantity: Int,
    val price: Double,
    val total: Double,
    val partyName: String,
    val reference: String,
    val invoiceId: Int? = null
)

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val phone: String = "",
    val role: String = "",
    val salary: Double = 0.0,
    val balance: Double = 0.0
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String = "أخرى", // "رواتب وأجور", "إيجار", "كهرباء", "مياه", "أخرى"
    val amount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val employeeId: Int? = null,
    val notes: String = "",
    val paymentMethod: String = "الصندوق", // "الصندوق", "شيك"
    val checkNumber: String? = null,
    val checkBankName: String? = null,
    val checkInvoiceId: Int? = null,
    val partyType: String = "مصروف عام", // "عميل", "مورد", "موظف", "مصروف عام"
    val partyName: String? = null,
    val customerId: Int? = null,
    val supplierId: Int? = null
)

@Entity(tableName = "app_feedbacks")
data class AppFeedback(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val senderName: String = "",
    val storeName: String = "",
    val phone: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "اقتراح ميزة جديدة", // "اقتراح ميزة جديدة", "تطوير قسم موجود", "ملاحظة أو فكرة", "طلب مخصص"
    val imageUri: String = "",
    val status: String = "جديد", // "جديد", "قيد المراجعة", "تم اعتماده", "تم التنفيذ"
    val adminReply: String = ""
)

