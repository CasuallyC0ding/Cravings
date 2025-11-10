package com.example.cravings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProductActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var addItemBtn: FloatingActionButton
    private lateinit var adapter: ProductAdapter
    private val productList = mutableListOf<Product_Merchant>()

    private lateinit var dbRef: DatabaseReference
    private val sellerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        recyclerView = findViewById(R.id.recyclerViewProducts)
        addItemBtn = findViewById(R.id.btnAddItem)

        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // ✅ Initialize the adapter properly and assign it
        adapter = ProductAdapter(productList) { selectedProduct ->
            val intent = Intent(this, EditProductActivity::class.java)
            intent.putExtra("productId", selectedProduct.productId)
            startActivity(intent)

            // ✅ Add smooth slide animation
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        recyclerView.adapter = adapter

        addItemBtn.setOnClickListener {
            startActivity(Intent(this, AddProductActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        dbRef = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("users")
            .child("Merchant")
            .child(sellerId)
            .child("products")

        loadProducts()
    }

    private fun loadProducts() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productList.clear()
                for (child in snapshot.children) {
                    val product = child.getValue(Product_Merchant::class.java)
                    if (product != null) {
                        productList.add(product)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProductActivity, "Failed to load products", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
