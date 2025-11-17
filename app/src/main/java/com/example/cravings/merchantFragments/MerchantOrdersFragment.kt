package com.example.cravings.merchantFragments

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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

        val ordersRef = database.reference
            .child("users").child(userRole).child(merchantId).child("orders")

        ordersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ordersContainer.removeAllViews()

                if (!snapshot.exists()) {
                    addEmpty("No orders yet")
                    return
                }

                val ordersList = mutableListOf<OrderData>()
                val customerIds = mutableSetOf<String>()

                snapshot.children.forEach { customerNode ->
                    val customerId = customerNode.key ?: return@forEach
                    customerIds.add(customerId)

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

                            val itemsList = mutableListOf<OrderItem>()
                            orderNode.child("items").children.forEach { itemNode ->
                                val name = itemNode.child("name").value?.toString() ?: "Unknown"
                                val price = itemNode.child("price").getValue(Double::class.java) ?: 0.0
                                val quantity = itemNode.child("quantity").getValue(Int::class.java) ?: 1
                                val productId = itemNode.child("productId").getValue(Int::class.java) ?: 0
                                itemsList.add(OrderItem(productId, name, price, quantity))
                            }

                            ordersList.add(
                                OrderData(
                                    customerId, orderId, status, shopName, pickupMethod,
                                    orderTotal, itemsTotal, deliveryFee, timestamp, itemsList
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("FIREBASE", "Error parsing order $orderId: ${e.message}")
                        }
                    }
                }

                ordersList.sortByDescending { it.timestamp }

                if (ordersList.isEmpty()) {
                    addEmpty("No orders yet")
                } else {
                    fetchAllCustomerNames(customerIds.toList()) {
                        ordersList.forEach { order -> addOrderCard(order) }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load orders", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun fetchAllCustomerNames(customerIds: List<String>, callback: () -> Unit) {
        var fetchedCount = 0
        val totalToFetch = customerIds.size
        if (totalToFetch == 0) { callback(); return }

        customerIds.forEach { customerId ->
            if (customerNamesCache.containsKey(customerId)) {
                fetchedCount++
                if (fetchedCount == totalToFetch) callback()
                return@forEach
            }

            database.reference.child("users").child("Customer").child(customerId).child("name")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        customerNamesCache[customerId] =
                            snapshot.getValue(String::class.java) ?: "Unknown Customer"
                        fetchedCount++
                        if (fetchedCount == totalToFetch) callback()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        customerNamesCache[customerId] = "Customer"
                        fetchedCount++
                        if (fetchedCount == totalToFetch) callback()
                    }
                })
        }
    }

    private fun addOrderCard(order: OrderData) {
        val card = layoutInflater.inflate(R.layout.item_order_card, ordersContainer, false)

        val customerName = customerNamesCache[order.customerId] ?: "Unknown Customer"
        card.findViewById<TextView>(R.id.customerIdText)?.text = "Customer: $customerName"
        card.findViewById<TextView>(R.id.orderIdText)?.text = "Order #${order.orderId.takeLast(8)}"

        val statusText = card.findViewById<TextView>(R.id.orderStatusText)
        statusText?.text = order.status.capitalize(Locale.ROOT)

        val backgroundColor = when (order.status.lowercase()) {
            "waiting for seller approval", "pending" -> android.R.color.holo_orange_dark
            "preparing", "accepted" -> android.R.color.holo_blue_dark
            "rejected", "cancelled" -> android.R.color.holo_red_dark
            "waiting for delivery" -> android.R.color.holo_orange_light
            "order ready" -> android.R.color.holo_green_light
            "out for delivery" -> android.R.color.holo_blue_light
            "delivered", "completed" -> android.R.color.holo_green_dark
            else -> android.R.color.darker_gray
        }

        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = 20f
        drawable.setColor(ContextCompat.getColor(requireContext(), backgroundColor))
        statusText?.background = drawable
        statusText?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

        val timeText = if (order.timestamp > 0) {
            dateFormat.format(Date(order.timestamp))
        } else "Unknown time"
        card.findViewById<TextView>(R.id.orderTimeText)?.text = timeText

        val itemsString = order.items.joinToString("\n") { item ->
            "• ${item.name} x${item.quantity} - ${String.format("%.2f", item.price * item.quantity)} EGP"
        }
        card.findViewById<TextView>(R.id.itemsText)?.text = itemsString
        card.findViewById<TextView>(R.id.orderTotalText)?.text = "${String.format("%.2f", order.orderTotal)} EGP"
        card.findViewById<TextView>(R.id.pickupMethodText)?.text =
            order.pickupMethod.capitalize(Locale.ROOT)

        val acceptBtn = card.findViewById<Button>(R.id.acceptBtn)
        val rejectBtn = card.findViewById<Button>(R.id.rejectBtn)

        when (order.status.lowercase()) {
            "waiting for seller approval", "pending" -> {
                acceptBtn?.isEnabled = true
                rejectBtn?.isEnabled = true
                acceptBtn?.setOnClickListener { acceptOrder(order) }
                rejectBtn?.setOnClickListener {
                    rejectOrder(order.customerId, order.orderId)
                }
            }
            "preparing", "accepted", "waiting for delivery", "order ready" -> {
                acceptBtn?.text = order.status.uppercase()
                acceptBtn?.isEnabled = false
                acceptBtn?.alpha = 0.6f
                rejectBtn?.visibility = View.GONE
            }
            "out for delivery" -> {
                acceptBtn?.text = "OUT FOR DELIVERY"
                acceptBtn?.isEnabled = false
                acceptBtn?.alpha = 0.6f
                rejectBtn?.visibility = View.GONE
            }
            "delivered", "completed" -> {
                acceptBtn?.text = "COMPLETED"
                acceptBtn?.isEnabled = false
                acceptBtn?.alpha = 0.6f
                rejectBtn?.visibility = View.GONE
            }
            "rejected", "cancelled" -> {
                acceptBtn?.visibility = View.GONE
                rejectBtn?.text = "REJECTED"
                rejectBtn?.isEnabled = false
                rejectBtn?.alpha = 0.6f
            }
        }

        ordersContainer.addView(card)
    }

    private fun rejectOrder(customerId: String, orderId: String) {
        val merchantId = auth.currentUser?.uid ?: return
        database.reference.child("users").child(userRole)
            .child(merchantId).child("orders")
            .child(customerId).child(orderId).child("status")
            .setValue("rejected")
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Order Rejected", Toast.LENGTH_SHORT).show()
            }
    }

    // -------------------------------------------------------------
    // 🔥🔥🔥 THIS IS THE ONLY PART THAT WAS MODIFIED (acceptOrder) 🔥🔥🔥
    // -------------------------------------------------------------
    private fun acceptOrder(order: OrderData) {
        val merchantId = auth.currentUser?.uid ?: return
        val productsRef = database.reference.child("users").child(userRole).child(merchantId).child("products")

        productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val insufficientStock = mutableListOf<String>()

                order.items.forEach { item ->
                    val productNode = snapshot.child(item.productId.toString())
                    val stock = productNode.child("stock")
                        .getValue(Int::class.java) ?: 0
                    val productName = productNode.child("name")
                        .getValue(String::class.java) ?: "Product ${item.productId}"

                    if (stock < item.quantity) {
                        insufficientStock.add("$productName (need ${item.quantity}, have $stock)")
                    }
                }

                if (insufficientStock.isNotEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Insufficient stock:\n${insufficientStock.joinToString("\n")}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                // Deduct stock
                order.items.forEach { item ->
                    val currentStock = snapshot.child(item.productId.toString())
                        .child("stock").getValue(Int::class.java) ?: 0
                    val newStock = currentStock - item.quantity
                    productsRef.child(item.productId.toString())
                        .child("stock").setValue(newStock)
                }

                // 🔥 SHOW DELIVERY SELECTION DIALOG AFTER STOCK IS UPDATED
                val dialog = android.app.AlertDialog.Builder(requireContext())
                dialog.setTitle("Choose Delivery Method")
                dialog.setMessage("How do you want to deliver this order?")

                dialog.setPositiveButton("Shop Delivery") { _, _ ->
                    updateOrderStatus(order, "out for delivery")
                }

                dialog.setNegativeButton("Freelancer Delivery") { _, _ ->
                    updateOrderStatus(order, "Waiting for Delivery")
                }

                dialog.setNeutralButton("Cancel", null)
                if (order.pickupMethod!="shop") {

                    dialog.show()
                }
                else{
                    updateOrderStatus(order, "order ready")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to check stock", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun updateOrderStatus(order: OrderData, newStatus: String) {
        val merchantId = auth.currentUser?.uid ?: return

        database.reference.child("users").child(userRole)
            .child(merchantId).child("orders")
            .child(order.customerId).child(order.orderId)
            .child("status").setValue(newStatus)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Order updated: $newStatus",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun addEmpty(text: String) {
        val tv = TextView(requireContext())
        tv.text = text
        tv.textSize = 18f
        tv.setPadding(30, 50, 30, 20)
        tv.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        ordersContainer.addView(tv)
    }

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