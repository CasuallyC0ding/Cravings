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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class OrdersFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: OrdersAdapter
    private val ordersList = mutableListOf<Order>()

    private val orderListeners = mutableMapOf<String, ValueEventListener>()

    private var customerOrdersListener: ValueEventListener? = null
    private var customerOrdersRef: DatabaseReference? = null

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

        ordersRecyclerView.layoutManager = LinearLayoutManager(context)
        adapter = OrdersAdapter(ordersList)
        ordersRecyclerView.adapter = adapter

        fetchCustomerOrdersRealtime()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        customerOrdersListener?.let { listener ->
            customerOrdersRef?.removeEventListener(listener)
        }
        customerOrdersListener = null
        customerOrdersRef = null
        removeAllOrderListeners()
    }

    private fun fetchCustomerOrdersRealtime() {
        val uid = auth.currentUser?.uid ?: return

        progressBar.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
        ordersRecyclerView.visibility = View.GONE

        customerOrdersRef = database.reference.child("users/Customer/$uid/orders")

        customerOrdersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || view == null || context == null) return

                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    showEmptyState()
                    return
                }

                removeAllOrderListeners()
                ordersList.clear()

                for (shopSnap in snapshot.children) {
                    val shopUid = shopSnap.key ?: continue
                    for (orderSnap in shopSnap.children) {
                        val orderId = orderSnap.key ?: continue
                        setupOrderListener(uid, shopUid, orderId)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("OrdersFragment", "Error: ${error.message}")
                if (isAdded && view != null) {
                    showEmptyState()
                }
            }
        }

        customerOrdersRef?.addValueEventListener(customerOrdersListener!!)
    }

    private fun setupOrderListener(customerUid: String, shopUid: String, orderId: String) {
        val merchantOrderRef = database.reference
            .child("users/Merchant/$shopUid/orders/$customerUid/$orderId")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || view == null || context == null) return

                if (!snapshot.exists()) {
                    removeOrderFromList(orderId)
                    return
                }

                val items = snapshot.child("items").children.mapNotNull { itemSnap ->
                    val productId = itemSnap.child("productId").getValue(Long::class.java)?.toInt() ?: 0
                    val quantity = itemSnap.child("quantity").getValue(Long::class.java)?.toInt() ?: 0
                    OrderItem(
                        productId = productId,
                        name = itemSnap.child("name").getValue(String::class.java),
                        price = itemSnap.child("price").getValue(Double::class.java) ?: 0.0,
                        quantity = quantity
                    )
                }

                val status = snapshot.child("status").getValue(String::class.java)

                val order = Order(
                    orderId = orderId,
                    shopUid = shopUid,
                    customerUid = customerUid,
                    items = items,
                    itemsTotal = snapshot.child("itemsTotal").getValue(Double::class.java) ?: 0.0,
                    deliveryFee = snapshot.child("deliveryFee").getValue(Double::class.java) ?: 0.0,
                    orderTotal = snapshot.child("orderTotal").getValue(Double::class.java) ?: 0.0,
                    pickupMethod = snapshot.child("pickupMethod").getValue(String::class.java),
                    deliveryLat = snapshot.child("deliveryLat").getValue(Double::class.java),
                    deliveryLng = snapshot.child("deliveryLng").getValue(Double::class.java),
                    status = status,
                    timestamp = snapshot.child("timestamp").getValue(Long::class.java),
                    shopName = snapshot.child("shopName").getValue(String::class.java)
                )

                // REMOVED: Local notification - FCM handles this now
                updateOrderInList(order)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("OrdersFragment", "Order listener error: ${error.message}")
            }
        }

        merchantOrderRef.addValueEventListener(listener)
        orderListeners["$shopUid-$orderId"] = listener
    }

    private fun updateOrderInList(order: Order) {
        if (!isAdded || view == null) return

        val index = ordersList.indexOfFirst { it.orderId == order.orderId }

        if (index != -1) {
            ordersList[index] = order
        } else {
            ordersList.add(order)
        }

        ordersList.sortByDescending { it.timestamp }

        progressBar.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE
        ordersRecyclerView.visibility = View.VISIBLE
        adapter.notifyDataSetChanged()
    }

    private fun removeOrderFromList(orderId: String) {
        if (!isAdded || view == null) return

        val removed = ordersList.removeAll { it.orderId == orderId }
        if (removed) {
            if (ordersList.isEmpty()) showEmptyState() else adapter.notifyDataSetChanged()
        }
    }

    private fun removeAllOrderListeners() {
        val uid = auth.currentUser?.uid ?: return

        orderListeners.forEach { (key, listener) ->
            val parts = key.split("-")
            if (parts.size == 2) {
                val shopUid = parts[0]
                val orderId = parts[1]

                database.reference
                    .child("users/Merchant/$shopUid/orders/$uid/$orderId")
                    .removeEventListener(listener)
            }
        }
        orderListeners.clear()
    }

    private fun showEmptyState() {
        if (!isAdded || view == null) return

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