package com.example.cravings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductAdapter(
    private val products: List<Product_Merchant>,
    private val onItemClick: (Product_Merchant) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.txtProductName)
        val priceText: TextView = itemView.findViewById(R.id.txtProductPrice)
        val stockText: TextView = itemView.findViewById(R.id.txtProductStock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount() = products.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.nameText.text = product.name
        holder.priceText.text = "Price: ${product.price}"
        holder.stockText.text = "Stock: ${product.stock}"
        holder.itemView.setOnClickListener { onItemClick(product) }
    }
}
