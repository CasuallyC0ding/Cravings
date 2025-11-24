package com.example.cravings.baseActivities.customer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cravings.R
import com.example.cravings.adapters.customer.CartAdapter
import com.example.cravings.baseActivities.customer.CheckoutActivity
import com.example.cravings.models.Product
import com.example.cravings.utils.CartManager
import com.google.firebase.auth.FirebaseAuth

class CartActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CartAdapter
    private lateinit var proceedButton: Button
    private lateinit var clearCartButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var shopNameText: TextView
    private lateinit var txtItemsTotal: TextView
    private lateinit var emptyCartLayout: LinearLayout
    private lateinit var cartContentLayout: LinearLayout

    private lateinit var auth: FirebaseAuth
    private var cartItems = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        initViews()
        setupRecyclerView()
        setupButtons()
        updateUI()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewCart)
        proceedButton = findViewById(R.id.proceedButton)
        backButton = findViewById(R.id.backButton)
        clearCartButton = findViewById(R.id.clearCartButton)
        shopNameText = findViewById(R.id.shopNameText)
        txtItemsTotal = findViewById(R.id.txtItemsTotal)
        emptyCartLayout = findViewById(R.id.emptyCartLayout)
        cartContentLayout = findViewById(R.id.cartContentLayout)

        auth = FirebaseAuth.getInstance()

        shopNameText.text = CartManager.shopName ?: "Shop"

        cartItems = if (CartManager.getCartItems().isNotEmpty()) {
            CartManager.getCartItems()
        } else {
            intent.getParcelableArrayListExtra<Product>("cart")?.toMutableList() ?: mutableListOf()
        }
    }

    private fun setupRecyclerView() {
        adapter = CartAdapter(cartItems) { updateUI() }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        backButton.setOnClickListener { onBackPressed() }

        clearCartButton.setOnClickListener {
            CartManager.clearCart()
            cartItems.clear()
            adapter.notifyDataSetChanged()
            updateUI()
        }

        proceedButton.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Navigate to CheckoutActivity
            val intent = Intent(this, CheckoutActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateUI() {
        val itemsTotal = cartItems.sumOf { (it.price ?: 0.0) * it.selectedQuantity }

        if (cartItems.isEmpty()) {
            emptyCartLayout.visibility = View.VISIBLE
            cartContentLayout.visibility = View.GONE
        } else {
            emptyCartLayout.visibility = View.GONE
            cartContentLayout.visibility = View.VISIBLE
            txtItemsTotal.text = "Total: EGP %.2f".format(itemsTotal)
        }
    }

    override fun onBackPressed() {
        val resultIntent = Intent().apply {
            putParcelableArrayListExtra("updatedCart", ArrayList(cartItems))
        }
        setResult(RESULT_OK, resultIntent)
        super.onBackPressed()
    }
}