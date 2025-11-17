package com.example.cravings.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cravings.R

class DeliveryShopsAdapter(
    private val shopsList: List<Pair<String, String>>,
    private val onShopClick: (String, String) -> Unit
) : RecyclerView.Adapter<DeliveryShopsAdapter.ShopViewHolder>() {

    inner class ShopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val shopNameText: TextView = itemView.findViewById(R.id.shopNameText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivery_shop, parent, false)
        return ShopViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val (shopId, shopName) = shopsList[position]
        holder.shopNameText.text = shopName

        holder.itemView.setOnClickListener {
            onShopClick(shopId, shopName)
        }
    }

    override fun getItemCount(): Int = shopsList.size
}