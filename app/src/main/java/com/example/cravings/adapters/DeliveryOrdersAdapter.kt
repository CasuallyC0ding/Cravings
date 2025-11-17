package com.example.cravings.adapters

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.cravings.R
import com.example.cravings.models.Order

class DeliveryOrdersAdapter(
    private val ordersList: List<Order>,
    private val onApplyClick: (Order) -> Unit
) : RecyclerView.Adapter<DeliveryOrdersAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderIdText: TextView = itemView.findViewById(R.id.orderIdText)
        val customerLocationText: TextView = itemView.findViewById(R.id.customerLocationText)
        val statusText: TextView = itemView.findViewById(R.id.statusText)
        val itemsContainer: LinearLayout = itemView.findViewById(R.id.itemsContainer)
        val orderTotalText: TextView = itemView.findViewById(R.id.orderTotalText)
        val deliveryFeeText: TextView = itemView.findViewById(R.id.deliveryFeeText)
        val btnApply: Button = itemView.findViewById(R.id.btnApply)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivery_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = ordersList[position]

        holder.orderIdText.text = "Order #${order.orderId?.take(8)}"

        // Set status with badge
        val statusText = order.status ?: "Waiting"
        holder.statusText.text = statusText.capitalize()

        val backgroundColor = when (statusText.lowercase()) {
            "waiting for delivery" -> android.R.color.holo_orange_dark
            "out for delivery" -> android.R.color.holo_blue_dark
            else -> android.R.color.darker_gray
        }

        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = 20f
        drawable.setColor(ContextCompat.getColor(holder.itemView.context, backgroundColor))
        holder.statusText.background = drawable
        holder.statusText.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))

        holder.orderTotalText.text = "EGP %.2f".format(order.orderTotal ?: 0.0)
        holder.deliveryFeeText.text = "Delivery Fee: EGP %.2f".format(order.deliveryFee ?: 0.0)

        if (order.deliveryLat != null && order.deliveryLng != null) {
            holder.customerLocationText.text = "📍 Lat: %.4f, Lng: %.4f".format(order.deliveryLat, order.deliveryLng)
        } else {
            holder.customerLocationText.text = "📍 Location not available"
        }

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

        holder.btnApply.setOnClickListener {
            onApplyClick(order)
        }
    }

    override fun getItemCount(): Int = ordersList.size
}