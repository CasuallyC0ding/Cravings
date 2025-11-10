package com.example.cravings

data class Product(
    val productId: String = "",
    val sellerId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = ""
)
