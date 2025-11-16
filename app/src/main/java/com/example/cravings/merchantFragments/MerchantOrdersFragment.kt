package com.example.cravings.merchantFragments

import android.os.Bundle
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

class MerchantOrdersFragment : Fragment() {

    private lateinit var ordersContainer: LinearLayout
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(
        "https://dbcravings-default-rtdb.europe-west1-firebasedatabase.app/"
    )

    private val userRole = "Merchant"

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
        val merchantId = auth.currentUser?.uid ?: return

        val ordersRef = database.reference
            .child("users").child(userRole).child(merchantId).child("orders")

        ordersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                ordersContainer.removeAllViews()

                if (!snapshot.exists()) {
                    addEmpty("No orders yet")
                    return
                }

                snapshot.children.forEach { customerNode ->

                    val customerId = customerNode.key ?: return@forEach

                    customerNode.children.forEach { orderNode ->

                        val orderId = orderNode.key ?: return@forEach
                        val status = orderNode.child("status").value?.toString() ?: "pending"

                        val itemsList = orderNode.child("items").children.mapNotNull {
                            val pid = it.child("productId").value?.toString() ?: return@mapNotNull null
                            val qty = it.child("quantity").getValue(Int::class.java) ?: 1
                            pid to qty

                        }

                        addOrderCard(customerId, orderId, status, itemsList)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun addOrderCard(
        customerId: String,
        orderId: String,
        status: String,
        items: List<Pair<String, Int>>
    ) {
        val card = layoutInflater.inflate(R.layout.item_order_card, ordersContainer, false)

        card.findViewById<TextView>(R.id.orderIdText).text = "Order: $orderId"
        card.findViewById<TextView>(R.id.customerIdText).text = "Customer: $customerId"
        card.findViewById<TextView>(R.id.orderStatusText).text = "Status: $status"

        val itemsString = items.joinToString("\n") {
            "• ${it.first} x${it.second}"
        }
        card.findViewById<TextView>(R.id.itemsText).text = itemsString

        card.findViewById<Button>(R.id.acceptBtn).setOnClickListener {
            acceptOrder(customerId, orderId, items)
        }

        card.findViewById<Button>(R.id.rejectBtn).setOnClickListener {
            rejectOrder(customerId, orderId)
        }

        ordersContainer.addView(card)
    }

    private fun rejectOrder(customerId: String, orderId: String) {
        val merchantId = auth.currentUser?.uid ?: return

        database.reference.child("users").child(userRole)
            .child(merchantId).child("orders")
            .child(customerId).child(orderId).child("status")
            .setValue("rejected")

        Toast.makeText(requireContext(), "Order Rejected", Toast.LENGTH_SHORT).show()
    }

    private fun acceptOrder(
        customerId: String,
        orderId: String,
        items: List<Pair<String, Int>>
    ) {
        val merchantId = auth.currentUser?.uid ?: return

        val productsRef = database.reference
            .child("users").child(userRole).child(merchantId).child("products")

        productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                // Validate stock
                items.forEach { (productId, qty) ->
                    val stock = snapshot.child(productId).child("stock")
                        .getValue(Int::class.java) ?: 0

                    if (stock < qty) {
                        Toast.makeText(
                            requireContext(),
                            "Not enough stock for product $productId",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }
                }

                // Deduct stock
                items.forEach { (productId, qty) ->
                    val stock = snapshot.child(productId).child("stock")
                        .getValue(Int::class.java) ?: 0

                    productsRef.child(productId)
                        .child("stock")
                        .setValue(stock - qty)
                }

                // Update status
                database.reference.child("users").child(userRole)
                    .child(merchantId).child("orders")
                    .child(customerId).child(orderId).child("status")
                    .setValue("preparing")

                Toast.makeText(requireContext(), "Order Accepted", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun addEmpty(text: String) {
        val tv = TextView(requireContext())
        tv.text = text
        tv.textSize = 18f
        tv.setPadding(30, 50, 30, 20)
        ordersContainer.addView(tv)
    }
}
