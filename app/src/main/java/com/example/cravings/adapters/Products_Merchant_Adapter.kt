package com.example.cravings.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cravings.R
import com.example.cravings.models.Product_Merchant

class ProductsMerchantAdapter(
    private val productList: List<Product_Merchant>,
    private val onItemClick: ((Product_Merchant) -> Unit)? = null
) : RecyclerView.Adapter<ProductsMerchantAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.imgProductPhoto)
        val name: TextView = itemView.findViewById(R.id.txtProductName)
        val price: TextView = itemView.findViewById(R.id.txtProductPrice)
        val stock: TextView = itemView.findViewById(R.id.txtProductStock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        holder.name.text = product.name
        holder.price.text = "EGP ${product.price}"
        holder.stock.text = "Stock: ${product.stock}"

        // 🖼 Load image from URL (if available)
        if (!product.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(product.imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.ic_image_placeholder)
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(product)
        }
    }

    override fun getItemCount(): Int = productList.size
}
