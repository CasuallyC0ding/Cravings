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

class CartAdapter(
    private val cartItems: List<Product>,
    private val onQuantityChanged: () -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.productImage)
        val productName: TextView = itemView.findViewById(R.id.productName)
        val productPrice: TextView = itemView.findViewById(R.id.productPrice)
        val productDescription: TextView = itemView.findViewById(R.id.productDescription)
        val btnMinus: Button = itemView.findViewById(R.id.btnMinus)
        val btnPlus: Button = itemView.findViewById(R.id.btnPlus)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_card, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val product = cartItems[position]

        holder.productName.text = product.name
        holder.productPrice.text = "EGP ${product.price}"
        holder.productDescription.text = product.description
        holder.tvQuantity.text = product.selectedQuantity.toString()

        Glide.with(holder.itemView.context)
            .load(product.imageUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .into(holder.productImage)

        holder.btnPlus.setOnClickListener {
            if (product.selectedQuantity < (product.stock ?: 0)) {
                product.selectedQuantity++
                holder.tvQuantity.text = product.selectedQuantity.toString()
                onQuantityChanged()
            }
        }

        holder.btnMinus.setOnClickListener {
            if (product.selectedQuantity > 0) {
                product.selectedQuantity--
                holder.tvQuantity.text = product.selectedQuantity.toString()
                onQuantityChanged()
            }
        }
    }

    override fun getItemCount(): Int = cartItems.size
}
