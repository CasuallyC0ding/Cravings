package com.example.cravings.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cravings.R
import com.example.cravings.models.Product

class ProductAdapter(
    private val productList: List<Product>,
    private val updateCart: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.productImage)
        val productName: TextView = itemView.findViewById(R.id.productName)
        val productPrice: TextView = itemView.findViewById(R.id.productPrice)
        val productDescription: TextView = itemView.findViewById(R.id.productDescription)
        val btnMinus: Button = itemView.findViewById(R.id.btnMinus)
        val btnPlus: Button = itemView.findViewById(R.id.btnPlus)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_card, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        holder.productName.text = product.name
        holder.productPrice.text = "EGP ${product.price}"
        holder.productDescription.text = product.description

        Glide.with(holder.itemView.context)
            .load(product.imageUrl ?: "")
            .placeholder(R.drawable.ic_profile_placeholder)
            .error(R.drawable.ic_profile_placeholder)
            .fallback(R.drawable.ic_profile_placeholder)
            .into(holder.productImage)

        holder.tvQuantity.text = product.selectedQuantity.toString()

        holder.btnPlus.setOnClickListener {
            if (product.selectedQuantity < (product.stock ?: 0)) {
                product.selectedQuantity++
                holder.tvQuantity.text = product.selectedQuantity.toString()
                updateCart(product)
            }
        }

        holder.btnMinus.setOnClickListener {
            if (product.selectedQuantity > 0) {
                product.selectedQuantity--
                holder.tvQuantity.text = product.selectedQuantity.toString()
                updateCart(product)
            }
        }
    }

    override fun getItemCount(): Int = productList.size
}