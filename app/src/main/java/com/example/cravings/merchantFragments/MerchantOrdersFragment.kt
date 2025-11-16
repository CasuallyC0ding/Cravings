package com.example.cravings.merchantFragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cravings.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class MerchantOrdersFragment : Fragment() {

    private lateinit var ordersContainer: LinearLayout
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(
        "https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/"
    )

    private val userRole = "Merchant"
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    // Cache customer names to avoid multiple fetches
    private val customerNamesCache = mutableMapOf<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_merchant_orders, container, false)
        ordersContainer = view.findViewById(R.id.ordersContainer)
        loadOrders()
        return view
    }

    private fun loadOrders() {
        val merchantId = auth.currentUser?.uid ?: run {
            Log.e("FIREBASE", "No current user UID found!")
            Toast.makeText(requireContext(), "Authentication error", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d("FIREBASE", "Current merchantId: $merchantId")

        val ordersRef = database.reference
            .child("users").child(userRole).child(merchantId).child("orders")
        Log.d("FIREBASE", "Reading orders from path: ${ordersRef.path}")

        ordersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ordersContainer.removeAllViews()

                if (!snapshot.exists()) {
                    Log.d("FIREBASE", "No orders found at this path")
                    addEmpty("No orders yet")
                    return
                }

                Log.d("FIREBASE", "Orders snapshot exists with ${snapshot.childrenCount} customers")

                val ordersList = mutableListOf<OrderData>()
                val customerIds = mutableSetOf<String>()

                // Iterate through each customer
                snapshot.children.forEach { customerNode ->
                    val customerId = customerNode.key ?: return@forEach
                    customerIds.add(customerId)
                    Log.d("FIREBASE", "Processing customerId: $customerId")

                    // Iterate through each order for this customer
                    customerNode.children.forEach { orderNode ->
                        val orderId = orderNode.key ?: return@forEach

                        try {
                            val status = orderNode.child("status").value?.toString() ?: "pending"
                            val shopName = orderNode.child("shopName").value?.toString() ?: "N/A"
                            val pickupMethod = orderNode.child("pickupMethod").value?.toString() ?: "N/A"
                            val orderTotal = orderNode.child("orderTotal").getValue(Double::class.java) ?: 0.0
                            val itemsTotal = orderNode.child("itemsTotal").getValue(Double::class.java) ?: 0.0
                            val deliveryFee = orderNode.child("deliveryFee").getValue(Double::class.java) ?: 0.0
                            val timestamp = orderNode.child("timestamp").getValue(Long::class.java) ?: 0L

                            // Parse items
                            val itemsList = mutableListOf<OrderItem>()
                            orderNode.child("items").children.forEach { itemNode ->
                                val name = itemNode.child("name").value?.toString() ?: "Unknown"
                                val price = itemNode.child("price").getValue(Double::class.java) ?: 0.0
                                val quantity = itemNode.child("quantity").getValue(Int::class.java) ?: 1
                                val productId = itemNode.child("productId").getValue(Int::class.java) ?: 0

                                itemsList.add(OrderItem(productId, name, price, quantity))
                            }

                            val orderData = OrderData(
                                customerId = customerId,
                                orderId = orderId,
                                status = status,
                                shopName = shopName,
                                pickupMethod = pickupMethod,
                                orderTotal = orderTotal,
                                itemsTotal = itemsTotal,
                                deliveryFee = deliveryFee,
                                timestamp = timestamp,
                                items = itemsList
                            )

                            ordersList.add(orderData)
                            Log.d("FIREBASE", "Order parsed successfully: $orderId")

                        } catch (e: Exception) {
                            Log.e("FIREBASE", "Error parsing order $orderId: ${e.message}")
                        }
                    }
                }

                // Sort orders by timestamp (newest first)
                ordersList.sortByDescending { it.timestamp }

                if (ordersList.isEmpty()) {
                    Log.d("FIREBASE", "No valid orders found")
                    addEmpty("No orders yet")
                } else {
                    Log.d("FIREBASE", "Displaying ${ordersList.size} orders")
                    // Fetch all customer names at once, then display orders
                    fetchAllCustomerNames(customerIds.toList()) {
                        ordersList.forEach { order ->
                            addOrderCard(order)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Failed to read orders: ${error.message}")
                Toast.makeText(
                    requireContext(),
                    "Failed to load orders: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun fetchAllCustomerNames(customerIds: List<String>, callback: () -> Unit) {
        var fetchedCount = 0
        val totalToFetch = customerIds.size

        if (totalToFetch == 0) {
            callback()
            return
        }

        customerIds.forEach { customerId ->
            // Check cache first
            if (customerNamesCache.containsKey(customerId)) {
                fetchedCount++
                if (fetchedCount == totalToFetch) {
                    callback()
                }
                return@forEach
            }

            // Fetch from database
            database.reference
                .child("users").child("Customer").child(customerId).child("name")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = snapshot.getValue(String::class.java) ?: "Unknown Customer"
                        customerNamesCache[customerId] = name
                        fetchedCount++
                        if (fetchedCount == totalToFetch) {
                            callback()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FIREBASE", "Failed to fetch customer name: ${error.message}")
                        customerNamesCache[customerId] = "Customer"
                        fetchedCount++
                        if (fetchedCount == totalToFetch) {
                            callback()
                        }
                    }
                })
        }
    }

    private fun addOrderCard(order: OrderData) {
        val card = layoutInflater.inflate(R.layout.item_order_card, ordersContainer, false)

        // Get customer name from cache
        val customerName = customerNamesCache[order.customerId] ?: "Unknown Customer"
        card.findViewById<TextView>(R.id.customerIdText)?.text = "Customer: $customerName"

        card.findViewById<TextView>(R.id.orderIdText)?.text = "Order #${order.orderId.takeLast(8)}"
        card.findViewById<TextView>(R.id.orderStatusText)?.text = order.status.capitalize(Locale.ROOT)

        // Format timestamp
        val timeText = if (order.timestamp > 0) {
            dateFormat.format(Date(order.timestamp))
        } else {
            "Unknown time"
        }
        card.findViewById<TextView>(R.id.orderTimeText)?.text = timeText

        // Display items
        val itemsString = order.items.joinToString("\n") { item ->
            "• ${item.name} x${item.quantity} - ${String.format("%.2f", item.price * item.quantity)} EGP"
        }
        card.findViewById<TextView>(R.id.itemsText)?.text = itemsString

        // Display total
        card.findViewById<TextView>(R.id.orderTotalText)?.text =
            "${String.format("%.2f", order.orderTotal)} EGP"

        // Display pickup method
        card.findViewById<TextView>(R.id.pickupMethodText)?.text =
            order.pickupMethod.capitalize(Locale.ROOT)

        // Update status text color based on status
        val statusText = card.findViewById<TextView>(R.id.orderStatusText)
        when (order.status.lowercase()) {
            "waiting for seller approval", "pending" -> {
                statusText?.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
            }
            "preparing", "accepted" -> {
                statusText?.setTextColor(resources.getColor(android.R.color.holo_blue_dark, null))
            }
            "rejected", "cancelled" -> {
                statusText?.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            }
            else -> {
                statusText?.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            }
        }

        // Setup buttons BEFORE adding to container
        val acceptBtn = card.findViewById<Button>(R.id.acceptBtn)
        val rejectBtn = card.findViewById<Button>(R.id.rejectBtn)

        if (acceptBtn == null || rejectBtn == null) {
            Log.e("FIREBASE", "Buttons not found in card!")
        } else {
            Log.d("FIREBASE", "Setting up buttons for order ${order.orderId} with status ${order.status}")

            when (order.status.lowercase()) {
                "waiting for seller approval", "pending" -> {
                    acceptBtn.isEnabled = true
                    rejectBtn.isEnabled = true
                    acceptBtn.isClickable = true
                    rejectBtn.isClickable = true

                    // Set click listeners with proper logging
                    acceptBtn.setOnClickListener {
                        Log.d("FIREBASE", "Accept button clicked for order ${order.orderId}")
                        acceptOrder(order)
                    }

                    rejectBtn.setOnClickListener {
                        Log.d("FIREBASE", "Reject button clicked for order ${order.orderId}")
                        rejectOrder(order.customerId, order.orderId)
                    }

                    Log.d("FIREBASE", "Buttons enabled and click listeners set")
                }
                "preparing", "accepted" -> {
                    acceptBtn.text = "PREPARING"
                    acceptBtn.isEnabled = false
                    acceptBtn.alpha = 0.6f
                    rejectBtn.visibility = View.GONE
                }
                "rejected", "cancelled" -> {
                    acceptBtn.visibility = View.GONE
                    rejectBtn.text = "REJECTED"
                    rejectBtn.isEnabled = false
                    rejectBtn.alpha = 0.6f
                }
                else -> {
                    acceptBtn.isEnabled = false
                    acceptBtn.alpha = 0.6f
                    rejectBtn.isEnabled = false
                    rejectBtn.alpha = 0.6f
                }
            }
        }

        // Add card to container LAST
        ordersContainer.addView(card)
    }

    private fun rejectOrder(customerId: String, orderId: String) {
        val merchantId = auth.currentUser?.uid ?: return

        Log.d("FIREBASE", "Rejecting order: $orderId for customer: $customerId")

        database.reference.child("users").child(userRole)
            .child(merchantId).child("orders")
            .child(customerId).child(orderId).child("status")
            .setValue("rejected")
            .addOnSuccessListener {
                Log.d("FIREBASE", "Order rejected successfully")
                Toast.makeText(requireContext(), "Order Rejected", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE", "Failed to reject order: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Failed to reject order: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun acceptOrder(order: OrderData) {
        val merchantId = auth.currentUser?.uid ?: return

        Log.d("FIREBASE", "Accepting order: ${order.orderId}")

        val productsRef = database.reference
            .child("users").child(userRole).child(merchantId).child("products")

        productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Validate stock for all items
                val insufficientStock = mutableListOf<String>()

                order.items.forEach { item ->
                    val productNode = snapshot.child(item.productId.toString())
                    val stock = productNode.child("stock").getValue(Int::class.java) ?: 0
                    val productName = productNode.child("name").getValue(String::class.java) ?: "Product ${item.productId}"

                    Log.d("FIREBASE", "Checking stock for ${item.name}: need ${item.quantity}, have $stock")

                    if (stock < item.quantity) {
                        insufficientStock.add("$productName (need ${item.quantity}, have $stock)")
                    }
                }

                if (insufficientStock.isNotEmpty()) {
                    Log.d("FIREBASE", "Insufficient stock detected")
                    Toast.makeText(
                        requireContext(),
                        "Insufficient stock:\n${insufficientStock.joinToString("\n")}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                Log.d("FIREBASE", "Stock validated, proceeding with deduction")

                // All items have sufficient stock - proceed with deduction
                order.items.forEach { item ->
                    val productNode = snapshot.child(item.productId.toString())
                    val currentStock = productNode.child("stock").getValue(Int::class.java) ?: 0
                    val newStock = currentStock - item.quantity

                    Log.d("FIREBASE", "Updating stock for product ${item.productId}: $currentStock -> $newStock")

                    productsRef.child(item.productId.toString())
                        .child("stock")
                        .setValue(newStock)
                        .addOnSuccessListener {
                            Log.d("FIREBASE", "Stock updated for product ${item.productId}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FIREBASE", "Failed to update stock for product ${item.productId}: ${e.message}")
                        }
                }

                // Update order status
                database.reference.child("users").child(userRole)
                    .child(merchantId).child("orders")
                    .child(order.customerId).child(order.orderId).child("status")
                    .setValue("preparing")
                    .addOnSuccessListener {
                        Log.d("FIREBASE", "Order status updated to preparing")
                        Toast.makeText(
                            requireContext(),
                            "Order Accepted - Stock Updated",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("FIREBASE", "Failed to update order status: ${e.message}")
                        Toast.makeText(
                            requireContext(),
                            "Failed to accept order: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE", "Failed to check stock: ${error.message}")
                Toast.makeText(
                    requireContext(),
                    "Failed to check stock: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun addEmpty(text: String) {
        val tv = TextView(requireContext())
        tv.text = text
        tv.textSize = 18f
        tv.setPadding(30, 50, 30, 20)
        tv.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        ordersContainer.addView(tv)
    }

    // Data classes
    data class OrderData(
        val customerId: String,
        val orderId: String,
        val status: String,
        val shopName: String,
        val pickupMethod: String,
        val orderTotal: Double,
        val itemsTotal: Double,
        val deliveryFee: Double,
        val timestamp: Long,
        val items: List<OrderItem>
    )

    data class OrderItem(
        val productId: Int,
        val name: String,
        val price: Double,
        val quantity: Int
    )
}