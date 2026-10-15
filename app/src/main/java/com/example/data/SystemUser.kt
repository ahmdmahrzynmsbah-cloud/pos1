package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_users")
data class SystemUser(
    @PrimaryKey val uid: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String = "CUSTOMER",
    val registrationDate: Long = System.currentTimeMillis(),
    val trialEndDate: Long? = null,
    val isSubscribed: Boolean = false,
    val isActive: Boolean = true,
    
    // User-specific customizable store and cashier settings
    val storeName: String = "مؤسسة الكنز للمحاسبة التجارية",
    val storePhone: String = "",
    val storeVatNumber: String = "",
    val currentBranch: String = "الفرع الرئيسي",
    val currencySymbol: String = "شيكل",
    val cashierName: String = "كاشير المبيعات",
    val cashierPhone: String = "",
    val cashierAddress: String = "",

    // Admin-controlled user permissions
    val canAccessReports: Boolean = true,
    val canAccessInventory: Boolean = true,
    val canAccessAccounting: Boolean = true,
    val canAccessAdvancedTools: Boolean = false,
    
    // New specific operational permissions
    val canAddProducts: Boolean = true,
    val canDeleteProducts: Boolean = false,
    val canApplyDiscounts: Boolean = false,
    val canViewProfits: Boolean = false,
    
    // Security word for password reset
    val securityWord: String = ""
)
