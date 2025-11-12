package com.example.cravings.utils

import com.example.cravings.models.Product

object CartManager {

    private val cartItems = mutableListOf<Product>()
    var shopName: String? = null
    var shopId: String? = null   // ✅ add shopId

    fun addOrUpdateProduct(product: Product, currentShopName: String, currentShopId: String) {
        // ✅ Only set shop if the cart is empty or same shop
        if (shopId == null || shopId == currentShopId) {
            shopId = currentShopId
            shopName = currentShopName
        }

        val existingProduct = cartItems.find { it.productId == product.productId }
        if (existingProduct != null) {
            existingProduct.selectedQuantity = product.selectedQuantity
            if (existingProduct.selectedQuantity == 0) {
                cartItems.remove(existingProduct)
            }
        } else if (product.selectedQuantity > 0) {
            cartItems.add(product)
        }
    }

    fun getCartItems(): MutableList<Product> {
        return cartItems.filter { it.selectedQuantity > 0 }.toMutableList()
    }

    fun getTotalItems(): Int = cartItems.sumOf { it.selectedQuantity }

    fun getTotalPrice(): Double = cartItems.sumOf { (it.price ?: 0.0) * it.selectedQuantity }

    fun clearCart() {
        cartItems.forEach { it.selectedQuantity = 0 }
        cartItems.clear()
        shopName = null
        shopId = null
    }
}