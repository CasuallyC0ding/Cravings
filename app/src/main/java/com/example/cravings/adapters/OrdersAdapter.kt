package com.example.cravings.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cravings.R
import com.example.cravings.models.Order
import com.example.cravings.models.OrderItem

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
        holder.shopText.text = "Shop: ${order.shopName}"
        holder.statusText.text = "Status: ${order.status ?: "N/A"}"
        holder.totalText.text = "Total: EGP %.2f".format(order.orderTotal ?: 0.0)

        holder.itemsContainer.removeAllViews()
        order.items?.forEach { item ->
            val itemView = LayoutInflater.from(holder.itemsContainer.context)
                .inflate(R.layout.item_order_product, holder.itemsContainer, false)
            val txtName = itemView.findViewById<TextView>(R.id.txtProductName)
            val txtQuantity = itemView.findViewById<TextView>(R.id.txtProductQuantity)
            val txtPrice = itemView.findViewById<TextView>(R.id.txtProductPrice)

            txtName.text = item.name
            txtQuantity.text = "Qty: ${item.quantity}"
            txtPrice.text = "EGP %.2f".format(item.price ?: 0.0)

            holder.itemsContainer.addView(itemView)
            Log.d("OrdersAdapter", "Adding product: ${item.name} Qty:${item.quantity} Price:${item.price}")
        }
    }

    override fun getItemCount(): Int = orders.size
}
