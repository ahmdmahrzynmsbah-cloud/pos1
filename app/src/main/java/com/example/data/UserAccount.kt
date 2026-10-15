package com.example.data

data class UserAccount(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val phone: String = "",
    val role: String = "CUSTOMER", // "ADMIN", "CUSTOMER"
    val businessName: String = "",
)
