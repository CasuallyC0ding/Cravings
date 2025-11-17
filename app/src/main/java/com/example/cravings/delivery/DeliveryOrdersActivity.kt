package com.example.cravings.delivery

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cravings.R
import com.example.cravings.adapters.DeliveryOrdersAdapter
import com.example.cravings.models.Order
import com.example.cravings.models.OrderItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DeliveryOrdersActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var shopNameText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var adapter: DeliveryOrdersAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    private val ordersList = mutableListOf<Order>()
    private var shopId: String = ""
    private var shopName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_orders)

        shopId = intent.getStringExtra("shopId") ?: ""
        shopName = intent.getStringExtra("shopName") ?: "Shop"

        backButton = findViewById(R.id.backButton)
        shopNameText = findViewById(R.id.shopNameText)
        recyclerView = findViewById(R.id.recyclerViewOrders)
        progressBar = findViewById(R.id.progressBar)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)

        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")
        auth = FirebaseAuth.getInstance()

        shopNameText.text = shopName

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DeliveryOrdersAdapter(ordersList) { order ->
            applyForDelivery(order)
        }
        recyclerView.adapter = adapter

        backButton.setOnClickListener { finish() }

        fetchDeliveryOrders()
    }

    private fun fetchDeliveryOrders() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE

        val ordersRef = database.reference.child("users").child("Merchant").child(shopId).child("orders")

        ordersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ordersList.clear()

                for (customerSnap in snapshot.children) {
                    val customerId = customerSnap.key ?: continue

                    for (orderSnap in customerSnap.children) {
                        val orderId = orderSnap.key ?: continue
                        val status = orderSnap.child("status").getValue(String::class.java)
                        val pickupMethod = orderSnap.child("pickupMethod").getValue(String::class.java)

                        if (status == "Waiting for Delivery" && pickupMethod == "delivery") {
                            val items = orderSnap.child("items").children.mapNotNull { itemSnap ->
                                OrderItem(
                                    productId = itemSnap.child("productId").getValue(Long::class.java)?.toInt() ?: 0,
                                    name = itemSnap.child("name").getValue(String::class.java),
                                    price = itemSnap.child("price").getValue(Double::class.java) ?: 0.0,
                                    quantity = itemSnap.child("quantity").getValue(Long::class.java)?.toInt() ?: 0
                                )
                            }

                            val order = Order(
                                orderId = orderId,
                                shopUid = shopId,
                                customerUid = customerId,
                                items = items,
                                itemsTotal = orderSnap.child("itemsTotal").getValue(Double::class.java) ?: 0.0,
                                deliveryFee = orderSnap.child("deliveryFee").getValue(Double::class.java) ?: 0.0,
                                orderTotal = orderSnap.child("orderTotal").getValue(Double::class.java) ?: 0.0,
                                pickupMethod = pickupMethod,
                                deliveryLat = orderSnap.child("deliveryLat").getValue(Double::class.java),
                                deliveryLng = orderSnap.child("deliveryLng").getValue(Double::class.java),
                                status = status,
                                timestamp = orderSnap.child("timestamp").getValue(Long::class.java),
                                shopName = shopName
                            )

                            ordersList.add(order)
                        }
                    }
                }

                progressBar.visibility = View.GONE

                if (ordersList.isEmpty()) {
                    emptyStateLayout.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyStateLayout.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    ordersList.sortByDescending { it.timestamp }
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                emptyStateLayout.visibility = View.VISIBLE
            }
        })
    }

    private fun applyForDelivery(order: Order) {
        val deliveryDriverId = auth.currentUser?.uid ?: return

        // Add delivery driver info to the order
        val orderRef = database.reference
            .child("users")
            .child("Merchant")
            .child(order.shopUid ?: "")
            .child("orders")
            .child(order.customerUid ?: "")
            .child(order.orderId ?: "")

        val updates = hashMapOf<String, Any>(
            "deliveryDriverId" to deliveryDriverId,
            "deliveryApplicationStatus" to "Pending Merchant Approval"
        )

        orderRef.updateChildren(updates)
            .addOnSuccessListener {
                android.widget.Toast.makeText(this, "Application sent! Waiting for merchant approval", android.widget.Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                android.widget.Toast.makeText(this, "Failed to apply: ${it.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
    }
}