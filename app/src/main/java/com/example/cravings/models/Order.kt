package com.example.cravings.models

data class Order(
    var orderId: String? = null,
    var shopUid: String? = null,
    var shopName: String? = null,
    var customerUid: String? = null,
    var items: List<OrderItem>? = emptyList(),
    var itemsTotal: Double? = 0.0,
    var deliveryFee: Double? = 0.0,
    var orderTotal: Double? = 0.0,
    var pickupMethod: String? = null,
    var deliveryLat: Double? = null,
    var deliveryLng: Double? = null,
    var status: String? = null,
    var timestamp: Long? = 0
)