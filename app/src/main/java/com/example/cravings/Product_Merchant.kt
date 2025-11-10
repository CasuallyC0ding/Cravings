package com.example.cravings

data class Product_Merchant(
    var name: String? = null,
    var description: String? = null,
    var price: Double? = null,
    var imageUrl: String? = null,
    var stock: Int? = null,
    var productId: Int? = null
)