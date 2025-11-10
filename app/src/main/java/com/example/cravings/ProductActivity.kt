package com.example.cravings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProductActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var addItemBtn: FloatingActionButton
    private lateinit var adapter: ProductAdapter
    private val productMerchantList = mutableListOf<Product_Merchant>() // ✅ Changed from ProductActivity to Product

    private lateinit var dbRef: DatabaseReference
    private val sellerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_products)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recyclerViewProducts)
        addItemBtn = findViewById(R.id.btnAddItem)

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = ProductAdapter(productMerchantList) { product ->
            val intent = Intent(this, EditProductActivity::class.java)
            intent.putExtra("productId", product.productId)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        addItemBtn.setOnClickListener {
            startActivity(Intent(this, AddProductActivity::class.java))
        }

        dbRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child("Merchants")
            .child(sellerId)
            .child("products")

        loadProducts()
    }

    private fun loadProducts() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productMerchantList.clear()
                for (child in snapshot.children) {
                    val productMerchant = child.getValue(Product_Merchant::class.java) // ✅ Correct class
                    if (productMerchant != null) productMerchantList.add(productMerchant)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProductActivity, "Failed to load products", Toast.LENGTH_SHORT).show()
            }
        })
    }
}