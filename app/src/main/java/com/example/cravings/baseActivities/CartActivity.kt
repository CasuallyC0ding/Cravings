package com.example.cravings.baseActivities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cravings.R
import com.example.cravings.adapters.CartAdapter
import com.example.cravings.models.Product
import com.example.cravings.utils.CartManager

class CartActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CartAdapter
    private lateinit var totalPriceText: TextView
    private lateinit var proceedButton: Button
    private lateinit var clearCartButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var shopNameText: TextView

    private var cartItems = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        recyclerView = findViewById(R.id.recyclerViewCart)
        totalPriceText = findViewById(R.id.totalPriceText)
        proceedButton = findViewById(R.id.proceedButton)
        backButton = findViewById(R.id.backButton)
        clearCartButton = findViewById(R.id.clearCartButton)
        shopNameText = findViewById(R.id.shopNameText)

        // Get shop name if available
        shopNameText.text = CartManager.shopName ?: "Shop"

        // Prefer CartManager items if it exists, otherwise from Intent
        cartItems = if (CartManager.getCartItems().isNotEmpty()) {
            CartManager.getCartItems()
        } else {
            intent.getParcelableArrayListExtra<Product>("cart")?.toMutableList() ?: mutableListOf()
        }

        adapter = CartAdapter(cartItems) { updateTotalPrice() }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        updateTotalPrice()

        // Go back
        backButton.setOnClickListener { onBackPressed() }

        // Proceed to checkout
        proceedButton.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putParcelableArrayListExtra("updatedCart", ArrayList(cartItems))
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        // Clear cart
        clearCartButton.setOnClickListener {
            CartManager.clearCart()
            cartItems.clear()
            adapter.notifyDataSetChanged()
            updateTotalPrice()
        }
    }

    private fun updateTotalPrice() {
        val total = if (CartManager.getCartItems().isNotEmpty()) {
            CartManager.getTotalPrice()
        } else {
            cartItems.sumOf { (it.price ?: 0.0) * it.selectedQuantity }
        }
        totalPriceText.text = "EGP %.2f".format(total)
    }

    override fun onBackPressed() {
        val resultIntent = Intent()
        resultIntent.putParcelableArrayListExtra("updatedCart", ArrayList(cartItems))
        setResult(RESULT_OK, resultIntent)
        super.onBackPressed()
    }
}
