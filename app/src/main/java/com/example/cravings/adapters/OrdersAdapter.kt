package com.example.cravings.adapters

import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.cravings.R
import com.example.cravings.models.Order

class OrdersAdapter(private val orders: List<Order>) :
    RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val shopText: TextView = itemView.findViewById(R.id.txtShopUid)
        val statusText: TextView = itemView.findViewById(R.id.txtStatus)
        val totalText: TextView = itemView.findViewById(R.id.txtTotal)
        val itemsContainer: LinearLayout = itemView.findViewById(R.id.itemsContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.shopText.text = order.shopName ?: "Shop"

        // Set status text
        val statusText = order.status ?: "Pending"
        holder.statusText.text = statusText.capitalize()

        // Set status badge background color based on status
        val backgroundColor = when (statusText.lowercase()) {
            "waiting for seller approval", "pending" -> android.R.color.holo_orange_dark
            "preparing", "accepted" -> android.R.color.holo_blue_dark
            "rejected", "cancelled" -> android.R.color.holo_red_dark
            "completed", "delivered" -> android.R.color.holo_green_dark
            else -> android.R.color.darker_gray
        }

        // Create rounded background drawable
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = 20f // Rounded corners
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
            Log.d("OrdersAdapter", "Adding product: ${item.name} Qty:${item.quantity} Price:${item.price}")
        }
    }

    override fun getItemCount(): Int = orders.size
}