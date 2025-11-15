package com.example.cravings.customerFragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cravings.R
import com.example.cravings.adapters.OrdersAdapter
import com.example.cravings.models.Order
import com.example.cravings.models.OrderItem
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase

class OrdersFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: OrdersAdapter
    private val ordersList = mutableListOf<Order>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_orders, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        ordersRecyclerView = view.findViewById(R.id.ordersRecyclerView)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        progressBar = view.findViewById(R.id.progressBar)

        ordersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = OrdersAdapter(ordersList)
        ordersRecyclerView.adapter = adapter

        fetchCustomerOrders()

        return view
    }

    private fun fetchCustomerOrders() {
        val uid = auth.currentUser?.uid ?: return

        progressBar.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
        ordersRecyclerView.visibility = View.GONE

        val customerOrdersRef = database.reference.child("users/Customer/$uid/orders")

        customerOrdersRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists() || !snapshot.hasChildren()) {
                showEmptyState()
                return@addOnSuccessListener
            }

            ordersList.clear()

            val tasks = mutableListOf<Task<DataSnapshot>>()
            for (shopSnap in snapshot.children) {
                val shopUid = shopSnap.key ?: continue
                for (orderSnap in shopSnap.children) {
                    val orderId = orderSnap.key ?: continue
                    val merchantOrderRef =
                        database.reference.child("users/Merchant/$shopUid/orders/$uid/$orderId")
                    tasks.add(merchantOrderRef.get())
                }
            }

            if (tasks.isEmpty()) {
                showEmptyState()
                return@addOnSuccessListener
            }

            Tasks.whenAllSuccess<DataSnapshot>(tasks).addOnSuccessListener { results ->
                for (merchantSnap in results) {
                    val snap = merchantSnap as DataSnapshot
                    if (!snap.exists()) continue

                    val items = snap.child("items").children.mapNotNull { itemSnap ->
                        val productId = itemSnap.child("productId").getValue(Long::class.java)?.toInt() ?: 0
                        val quantity = itemSnap.child("quantity").getValue(Long::class.java)?.toInt() ?: 0
                        OrderItem(
                            productId = productId,
                            name = itemSnap.child("name").getValue(String::class.java),
                            price = itemSnap.child("price").getValue(Double::class.java) ?: 0.0,
                            quantity = quantity
                        )
                    }

                    val order = Order(
                        orderId = snap.key ?: "",
                        shopUid = snap.ref.parent?.parent?.key ?: "",
                        customerUid = uid,
                        items = items,
                        itemsTotal = snap.child("itemsTotal").getValue(Double::class.java) ?: 0.0,
                        deliveryFee = snap.child("deliveryFee").getValue(Double::class.java) ?: 0.0,
                        orderTotal = snap.child("orderTotal").getValue(Double::class.java) ?: 0.0,
                        pickupMethod = snap.child("pickupMethod").getValue(String::class.java),
                        deliveryLat = snap.child("deliveryLat").getValue(Double::class.java),
                        deliveryLng = snap.child("deliveryLng").getValue(Double::class.java),
                        status = snap.child("status").getValue(String::class.java),
                        timestamp = snap.child("timestamp").getValue(Long::class.java),
                        shopName = snap.child("shopName").getValue(String::class.java)
                    )

                    Log.d("OrdersFragment", "Order fetched: $order")
                    ordersList.add(order)
                }

                progressBar.visibility = View.GONE

                if (ordersList.isEmpty()) {
                    showEmptyState()
                } else {
                    emptyStateLayout.visibility = View.GONE
                    ordersRecyclerView.visibility = View.VISIBLE
                    // Sort by timestamp descending (newest first)
                    ordersList.sortByDescending { it.timestamp }
                    adapter.notifyDataSetChanged()
                }
            }.addOnFailureListener {
                progressBar.visibility = View.GONE
                showEmptyState()
            }
        }.addOnFailureListener {
            progressBar.visibility = View.GONE
            showEmptyState()
        }
    }

    private fun showEmptyState() {
        progressBar.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
        ordersRecyclerView.visibility = View.GONE
    }

    companion object {
        fun newInstance(role: String): OrdersFragment {
            val fragment = OrdersFragment()
            fragment.arguments = Bundle().apply { putString("userRole", role) }
            return fragment
        }
    }
}