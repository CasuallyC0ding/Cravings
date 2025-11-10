package com.example.cravings.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cravings.R
import com.example.cravings.models.Shop

class ShopAdapter(private val shopList: List<Shop>) :
    RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {

    class ShopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val shopImage: ImageView = itemView.findViewById(R.id.shopImage)
        val shopName: TextView = itemView.findViewById(R.id.shopName)
       // val shopDescription: TextView = itemView.findViewById(R.id.shopDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop_card, parent, false)
        return ShopViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val shop = shopList[position]

        holder.shopName.text = shop.shopName
      //  holder.shopDescription.text = shop.description ?: ""

        val imageUrl = shop.profileImage

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .error(R.drawable.ic_profile_placeholder)
            .into(holder.shopImage)
    }

    override fun getItemCount(): Int = shopList.size
}
