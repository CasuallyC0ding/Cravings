package com.example.cravings.customerFragments

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

    // Track last status for notifications
    private val lastStatusMap = mutableMapOf<String, String>()

    private val orderListeners = mutableMapOf<String, ValueEventListener>()

    private val CHANNEL_ID = "order_status_channel"

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

        createNotificationChannel()
        fetchCustomerOrdersRealtime()

        return view
    }

    private fun fetchCustomerOrdersRealtime() {
        val uid = auth.currentUser?.uid ?: return

        progressBar.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
        ordersRecyclerView.visibility = View.GONE

        val customerOrdersRef = database.reference.child("users/Customer/$uid/orders")

        customerOrdersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
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
                showEmptyState()
            }
        })
    }

    private fun setupOrderListener(customerUid: String, shopUid: String, orderId: String) {
        val merchantOrderRef = database.reference
            .child("users/Merchant/$shopUid/orders/$customerUid/$orderId")

        val listener = object : ValueEventListener {
            @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
            override fun onDataChange(snapshot: DataSnapshot) {
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

                // Send notification if status changed
                val key = "${shopUid}_$orderId"
                val lastStatus = lastStatusMap[key]
                if (lastStatus != null && lastStatus != status) {
                    sendStatusNotification(order)
                }
                lastStatusMap[key] = status ?: ""

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
        val existingIndex = ordersList.indexOfFirst { it.orderId == order.orderId }

        if (existingIndex != -1) {
            ordersList[existingIndex] = order
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
        val removed = ordersList.removeAll { it.orderId == orderId }
        if (removed) {
            if (ordersList.isEmpty()) showEmptyState() else adapter.notifyDataSetChanged()
        }
    }

    private fun removeAllOrderListeners() {
        orderListeners.forEach { (key, listener) ->
            val parts = key.split("-")
            if (parts.size == 2) {
                val shopUid = parts[0]
                val orderId = parts[1]
                val customerUid = auth.currentUser?.uid ?: return
                database.reference
                    .child("users/Merchant/$shopUid/orders/$customerUid/$orderId")
                    .removeEventListener(listener)
            }
        }
        orderListeners.clear()
    }

    private fun showEmptyState() {
        progressBar.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
        ordersRecyclerView.visibility = View.GONE
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun sendStatusNotification(order: Order) {
        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Order Status Updated")
            .setContentText("Order #${order.orderId?.take(8)} is now '${order.status}'")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(requireContext()).notify(order.orderId.hashCode(), builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Order Status"
            val descriptionText = "Notifications for order status changes"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeAllOrderListeners()
    }

    companion object {
        fun newInstance(role: String): OrdersFragment {
            val fragment = OrdersFragment()
            fragment.arguments = Bundle().apply { putString("userRole", role) }
            return fragment
        }
    }
}
