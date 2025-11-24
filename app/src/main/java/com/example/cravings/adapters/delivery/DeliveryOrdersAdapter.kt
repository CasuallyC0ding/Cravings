package com.example.cravings.adapters.delivery

import android.content.Intent
import android.net.Uri
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
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
        val btnViewLocation: ImageButton = itemView.findViewById(R.id.btnViewLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivery_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = ordersList[position]

        // ORDER ID
        holder.orderIdText.text = "Order #${order.orderId?.take(8)}"

        // STATUS BADGE
        val statusText = order.status ?: "Waiting"
        holder.statusText.text = statusText.replaceFirstChar { it.uppercase() }

        val badgeColor = when (statusText.lowercase()) {
            "waiting for delivery" -> android.R.color.holo_orange_dark
            "out for delivery" -> android.R.color.holo_blue_dark
            else -> android.R.color.darker_gray
        }

        val badgeBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20f
            setColor(ContextCompat.getColor(holder.itemView.context, badgeColor))
        }

        holder.statusText.background = badgeBackground
        holder.statusText.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))

        // TOTALS
        holder.orderTotalText.text = "EGP %.2f".format(order.orderTotal ?: 0.0)
        holder.deliveryFeeText.text = "Delivery Fee: EGP %.2f".format(order.deliveryFee ?: 0.0)

        // LOCATION TEXT
        if (order.deliveryLat != null && order.deliveryLng != null) {
            holder.customerLocationText.text =
                "📍 Lat: %.4f, Lng: %.4f".format(order.deliveryLat, order.deliveryLng)
        } else {
            holder.customerLocationText.text = "📍 Location not available"
        }

        // ITEMS
        holder.itemsContainer.removeAllViews()
        order.items?.forEach { item ->
            val itemView = LayoutInflater.from(holder.itemsContainer.context)
                .inflate(R.layout.item_order_product, holder.itemsContainer, false)

            itemView.findViewById<TextView>(R.id.txtProductName).text = item.name
            itemView.findViewById<TextView>(R.id.txtProductQuantity).text = "${item.quantity}"
            itemView.findViewById<TextView>(R.id.txtProductPrice).text =
                "EGP %.2f".format(item.price ?: 0.0)

            holder.itemsContainer.addView(itemView)
        }

        // APPLY BUTTON
        holder.btnApply.setOnClickListener {
            onApplyClick(order)
        }

        // VIEW LOCATION BUTTON → OPEN GOOGLE MAPS
        holder.btnViewLocation.setOnClickListener {
            val lat = order.deliveryLat
            val lng = order.deliveryLng

            if (lat != null && lng != null) {
                val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Customer Location)")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")

                try {
                    holder.itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    // Google Maps not installed → open browser
                    val browserIntent =
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$lat,$lng"))
                    holder.itemView.context.startActivity(browserIntent)
                }
            }
        }
    }

    override fun getItemCount(): Int = ordersList.size
}