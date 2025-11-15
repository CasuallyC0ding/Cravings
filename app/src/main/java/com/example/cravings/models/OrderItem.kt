package com.example.cravings.models

data class OrderItem(
    val productId: Int? = 0,
    val name: String? = null,
    val price: Double? = 0.0,
    val quantity: Int = 0
)