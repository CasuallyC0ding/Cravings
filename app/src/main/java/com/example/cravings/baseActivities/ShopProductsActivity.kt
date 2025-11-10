package com.example.cravings.baseActivities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cravings.adapters.ProductAdapter
import com.example.cravings.models.Product
import com.google.firebase.database.*

class ShopProductsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter
    private val productList = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop_products)

        recyclerView = findViewById(R.id.recyclerViewProducts)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ProductAdapter(productList)
        recyclerView.adapter = adapter

        // Get shopId passed from the previous screen
        val shopId = intent.getStringExtra("shopId")
        if (shopId != null) {
            loadProducts(shopId)
        }
    }

    private fun loadProducts(shopId: String) {
        val databaseRef = FirebaseDatabase.getInstance()
            .getReference("Products")
            .child(shopId)

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productList.clear()
                for (data in snapshot.children) {
                    val product = data.getValue(Product::class.java)
                    if (product != null) productList.add(product)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
