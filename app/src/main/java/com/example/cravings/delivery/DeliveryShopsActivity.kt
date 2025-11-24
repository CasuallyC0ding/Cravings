package com.example.cravings.delivery

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cravings.R
import com.example.cravings.adapters.delivery.DeliveryShopsAdapter
import com.google.firebase.database.*

class DeliveryShopsActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var adapter: DeliveryShopsAdapter
    private lateinit var database: FirebaseDatabase

    private val shopsList = mutableListOf<Pair<String, String>>() // Pair<shopId, shopName>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_shops)

        backButton = findViewById(R.id.backButton)
        recyclerView = findViewById(R.id.recyclerViewShops)
        progressBar = findViewById(R.id.progressBar)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)

        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = DeliveryShopsAdapter(shopsList) { shopId, shopName ->
            val intent = Intent(this, DeliveryOrdersActivity::class.java)
            intent.putExtra("shopId", shopId)
            intent.putExtra("shopName", shopName)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        backButton.setOnClickListener { finish() }

        fetchShopsWithDeliveryOrders()
    }

    private fun fetchShopsWithDeliveryOrders() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE

        val merchantsRef = database.reference.child("users").child("Merchant")

        merchantsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                shopsList.clear()

                for (merchantSnap in snapshot.children) {
                    val shopId = merchantSnap.key ?: continue
                    val shopName = merchantSnap.child("shopName").getValue(String::class.java) ?: "Shop"

                    // Check if this merchant has any orders with status "Waiting for Delivery"
                    val ordersSnap = merchantSnap.child("orders")
                    var hasDeliveryOrders = false

                    for (customerSnap in ordersSnap.children) {
                        for (orderSnap in customerSnap.children) {
                            val status = orderSnap.child("status").getValue(String::class.java)
                            val pickupMethod = orderSnap.child("pickupMethod").getValue(String::class.java)

                            if (status == "Waiting for Delivery" && pickupMethod == "delivery") {
                                hasDeliveryOrders = true
                                break
                            }
                        }
                        if (hasDeliveryOrders) break
                    }

                    if (hasDeliveryOrders) {
                        shopsList.add(Pair(shopId, shopName))
                    }
                }

                progressBar.visibility = View.GONE

                if (shopsList.isEmpty()) {
                    emptyStateLayout.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyStateLayout.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                emptyStateLayout.visibility = View.VISIBLE
            }
        })
    }
}