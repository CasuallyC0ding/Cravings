package com.example.cravings.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.cravings.R
import com.example.cravings.baseActivities.ShopProductsActivity
import com.example.cravings.models.Shop

class ShopAdapter(private val shopList: List<Shop>) :
    RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {

    class ShopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val shopImage: ImageView = itemView.findViewById(R.id.shopImage)
        val shopName: TextView = itemView.findViewById(R.id.shopName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop_card, parent, false)
        return ShopViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val shop = shopList[position]
        holder.shopName.text = shop.shopName

        // Load shop image
        if (!shop.profileImage.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(shop.profileImage)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(holder.shopImage)
        } else {
            holder.shopImage.setImageResource(R.drawable.ic_profile_placeholder)
        }

        // Send shop name and ID when opened
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ShopProductsActivity::class.java)
            intent.putExtra("shopId", shop.uid)
            intent.putExtra("shopName", shop.shopName)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = shopList.size
}