package com.example.cravings.baseActivities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cravings.R
import com.example.cravings.adapters.ProductAdapter
import com.example.cravings.models.Product
import com.example.cravings.utils.CartManager
import com.google.firebase.database.FirebaseDatabase

class ShopProductsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter
    private val productList = mutableListOf<Product>()
    private lateinit var database: FirebaseDatabase
    private lateinit var cartLayout: LinearLayout
    private lateinit var cartItemCount: TextView
    private lateinit var cartTotalPrice: TextView
    private lateinit var shopNameText: TextView
    private lateinit var backButton: ImageButton

    private val cartLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            adapter.notifyDataSetChanged()
            updateCartUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop_products)

        backButton = findViewById(R.id.backButton)
        recyclerView = findViewById(R.id.recyclerViewProducts)
        cartLayout = findViewById(R.id.cartLayout)
        cartItemCount = findViewById(R.id.cartItemCount)
        cartTotalPrice = findViewById(R.id.cartTotalPrice)
        shopNameText = findViewById(R.id.shopNameText)

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        val shopId = intent.getStringExtra("shopId") ?: return
        val shopName = intent.getStringExtra("shopName") ?: "Shop"
        shopNameText.text = shopName

        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        backButton.setOnClickListener { onBackPressed() }

        adapter = ProductAdapter(productList) { product ->
            handleCartInteraction(product, shopName, shopId)
        }
        recyclerView.adapter = adapter

        cartLayout.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            intent.putExtra("shopName", CartManager.shopName ?: shopName)
            cartLauncher.launch(intent)
        }

        loadProducts(shopId)
    }

    override fun onResume() {
        super.onResume()
        if (CartManager.getCartItems().isNotEmpty()) {
            productList.forEach { product ->
                val cartProduct = if (CartManager.shopId == intent.getStringExtra("shopId")) {
                    CartManager.getCartItems().find { it.productId == product.productId }
                } else null
                product.selectedQuantity = cartProduct?.selectedQuantity ?: 0
            }
        }
        adapter.notifyDataSetChanged()
        updateCartUI()
    }

    private fun handleCartInteraction(product: Product, shopName: String, shopId: String) {
        if (CartManager.getCartItems().isNotEmpty() && CartManager.shopId != shopId) {
            product.selectedQuantity = (product.selectedQuantity - 1).coerceAtLeast(0)
            adapter.notifyDataSetChanged()
            showClearCartDialog {
                CartManager.clearCart()
                CartManager.shopId = shopId
                CartManager.shopName = shopName
                product.selectedQuantity += 1
                CartManager.addOrUpdateProduct(product, shopName, shopId)
                adapter.notifyDataSetChanged()
                updateCartUI()
            }
        } else {
            CartManager.shopId = shopId
            CartManager.shopName = shopName
            CartManager.addOrUpdateProduct(product, shopName, shopId)
            updateCartUI()
        }
    }

    private fun loadProducts(shopId: String) {
        val ref = database.reference.child("users").child("Merchant").child(shopId).child("products")
        ref.get().addOnSuccessListener { snapshot ->
            productList.clear()
            for (data in snapshot.children) {
                val product = data.getValue(Product::class.java)
                if (product != null) {
                    product.productId = data.key?.toIntOrNull()
                    val existing = if (CartManager.shopId == shopId) {
                        CartManager.getCartItems().find { it.productId == product.productId }
                    } else null
                    if (existing != null) product.selectedQuantity = existing.selectedQuantity
                    productList.add(product)
                }
            }
            adapter.notifyDataSetChanged()
            updateCartUI()
        }
    }

    private fun updateCartUI() {
        val totalItems = CartManager.getTotalItems()
        val totalPrice = CartManager.getTotalPrice()
        if (totalItems == 0) {
            cartLayout.visibility = View.GONE
        } else {
            cartLayout.visibility = View.VISIBLE
            cartItemCount.text = "$totalItems items"
            cartTotalPrice.text = "EGP %.2f".format(totalPrice)
        }
    }

    private fun showClearCartDialog(onStart: () -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Start New Order?")
        builder.setMessage("Starting a new order will clear your current cart with ${CartManager.shopName}. Continue?")
        builder.setPositiveButton("Start") { _, _ -> onStart() }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
}