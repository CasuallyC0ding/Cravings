package com.example.cravings.models

data class Order(
    val orderId: String? = null,
    val shopUid: String? = null,
    var shopName: String? = null,
    val customerUid: String? = null,
    val items: List<OrderItem>? = emptyList(),
    val itemsTotal: Double? = 0.0,
    val deliveryFee: Double? = 0.0,
    val orderTotal: Double? = 0.0,
    val pickupMethod: String? = null,
    val deliveryLat: Double? = null,
    val deliveryLng: Double? = null,
    val status: String? = null,
    val timestamp: Long? = 0
)