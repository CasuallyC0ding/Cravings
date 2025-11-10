package com.example.cravings.baseActivities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cravings.adapters.ProductAdapter
import com.example.cravings.models.Product
import com.google.firebase.database.*
import com.example.cravings.R

class ShopProductsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter
    private val productList = mutableListOf<Product>()
    private lateinit var database: FirebaseDatabase

    private lateinit var cartLayout: LinearLayout
    private lateinit var cartItemCount: TextView
    private lateinit var cartTotalPrice: TextView

    private val cartMap = mutableMapOf<Int, Product>() // productId -> Product

    // Activity result launcher for CartActivity
    private val cartLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val updatedCart = result.data?.getParcelableArrayListExtra<Product>("updatedCart")
            if (updatedCart != null) {
                // Update productList quantities
                for (product in productList) {
                    val updated = updatedCart.find { it.productId == product.productId }
                    product.selectedQuantity = updated?.selectedQuantity ?: 0
                }

                // Update cartMap
                cartMap.clear()
                updatedCart.forEach { if (it.selectedQuantity > 0) cartMap[it.productId!!] = it }

                adapter.notifyDataSetChanged()
                updateCartUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop_products)

        recyclerView = findViewById(R.id.recyclerViewProducts)
        recyclerView.layoutManager = LinearLayoutManager(this)

        cartLayout = findViewById(R.id.cartLayout)
        cartItemCount = findViewById(R.id.cartItemCount)
        cartTotalPrice = findViewById(R.id.cartTotalPrice)

        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        // Adapter with lambda to update cart
        adapter = ProductAdapter(productList) { product ->
            updateCart(product)
        }
        recyclerView.adapter = adapter

        cartLayout.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            intent.putParcelableArrayListExtra("cart", ArrayList(cartMap.values))
            cartLauncher.launch(intent)
        }

        val shopId = intent.getStringExtra("shopId")
        if (shopId != null) loadProducts(shopId)
    }

    private fun loadProducts(shopId: String) {
        val ref = database.reference.child("users").child("Merchant").child(shopId).child("products")
        ref.get().addOnSuccessListener { snapshot ->
            productList.clear()
            for (data in snapshot.children) {
                val product = data.getValue(Product::class.java)
                if (product != null) {
                    product.productId = data.key?.toIntOrNull()
                    productList.add(product)
                }
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun updateCart(product: Product) {
        product.productId?.let { id ->
            if (product.selectedQuantity > 0) cartMap[id] = product
            else cartMap.remove(id)
        }
        updateCartUI()
    }

    private fun updateCartUI() {
        if (cartMap.isEmpty()) {
            cartLayout.visibility = View.GONE
        } else {
            cartLayout.visibility = View.VISIBLE
            val totalItems = cartMap.values.sumOf { it.selectedQuantity }
            val totalPrice = cartMap.values.sumOf { (it.price ?: 0.0) * it.selectedQuantity }
            cartItemCount.text = "$totalItems items"
            cartTotalPrice.text = "EGP %.2f".format(totalPrice)
        }
    }
}