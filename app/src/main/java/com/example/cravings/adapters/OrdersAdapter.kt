package com.example.cravings.adapters

import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.cravings.R
import com.example.cravings.models.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class OrdersAdapter(private val orders: List<Order>) :
    RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    private val database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")
    private val auth = FirebaseAuth.getInstance()

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val shopText: TextView = itemView.findViewById(R.id.txtShopUid)
        val statusText: TextView = itemView.findViewById(R.id.txtStatus)
        val totalText: TextView = itemView.findViewById(R.id.txtTotal)
        val itemsContainer: LinearLayout = itemView.findViewById(R.id.itemsContainer)
        val btnDelivered: Button? = itemView.findViewById(R.id.btnDelivered)
        val btnNotDelivered: Button? = itemView.findViewById(R.id.btnNotDelivered)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.shopText.text = order.shopName ?: "Shop"

        val statusText = order.status ?: "Pending"
        holder.statusText.text = statusText.capitalize()

        // Set status badge background color
        val backgroundColor = when (statusText.lowercase()) {
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
        drawable.setColor(ContextCompat.getColor(holder.itemView.context, backgroundColor))
        holder.statusText.background = drawable
        holder.statusText.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))

        holder.totalText.text = "EGP %.2f".format(order.orderTotal ?: 0.0)

        holder.itemsContainer.removeAllViews()
        order.items?.forEach { item ->
            val itemView = LayoutInflater.from(holder.itemsContainer.context)
                .inflate(R.layout.item_order_product, holder.itemsContainer, false)

            val txtName = itemView.findViewById<TextView>(R.id.txtProductName)
            val txtQuantity = itemView.findViewById<TextView>(R.id.txtProductQuantity)
            val txtPrice = itemView.findViewById<TextView>(R.id.txtProductPrice)

            txtName.text = item.name
            txtQuantity.text = "${item.quantity}"
            txtPrice.text = "EGP %.2f".format(item.price ?: 0.0)

            holder.itemsContainer.addView(itemView)
        }

        // Show delivery confirmation buttons if status is "Out for Delivery"
        if (statusText.lowercase() == "out for delivery") {
            holder.btnDelivered?.visibility = View.VISIBLE
            holder.btnNotDelivered?.visibility = View.VISIBLE

            holder.btnDelivered?.setOnClickListener {
                confirmDelivery(order, holder.itemView.context, true)
            }

            holder.btnNotDelivered?.setOnClickListener {
                confirmDelivery(order, holder.itemView.context, false)
            }
        } else {
            holder.btnDelivered?.visibility = View.GONE
            holder.btnNotDelivered?.visibility = View.GONE
        }
    }

    private fun confirmDelivery(order: Order, context: android.content.Context, wasDelivered: Boolean) {
        val orderRef = database.reference
            .child("users")
            .child("Merchant")
            .child(order.shopUid ?: "")
            .child("orders")
            .child(order.customerUid ?: "")
            .child(order.orderId ?: "")

        if (wasDelivered) {
            // Mark as delivered and credit delivery person
            orderRef.child("status").setValue("Delivered")
                .addOnSuccessListener {
                    // Get delivery driver ID
                    orderRef.child("deliveryDriverId").get()
                        .addOnSuccessListener { snapshot ->
                            val driverId = snapshot.getValue(String::class.java)
                            if (driverId != null) {
                                creditDeliveryDriver(driverId, order.deliveryFee ?: 0.0, context)
                            }
                        }
                    Toast.makeText(context, "✅ Order marked as delivered!", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Mark as not delivered (keep status or revert)
            orderRef.child("status").setValue("Waiting for Delivery")
                .addOnSuccessListener {
                    Toast.makeText(context, "❌ Order marked as not delivered", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun creditDeliveryDriver(driverId: String, deliveryFee: Double, context: android.content.Context) {
        val driverRef = database.reference
            .child("users")
            .child("Customer")
            .child(driverId)
            .child("deliveryPoints")

        driverRef.get().addOnSuccessListener { snapshot ->
            val currentPoints = snapshot.getValue(Double::class.java) ?: 0.0
            val newPoints = currentPoints + deliveryFee

            driverRef.setValue(newPoints)
                .addOnSuccessListener {
                    Log.d("DELIVERY", "Credited $deliveryFee EGP to driver $driverId")
                }
        }
    }

    override fun getItemCount(): Int = orders.size
}