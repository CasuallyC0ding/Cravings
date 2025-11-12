package com.example.cravings.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cravings.R
import com.example.cravings.models.Product_Merchant

class ProductsMerchantAdapter(
    private val productList: List<Product_Merchant>,
    private val onItemClick: ((Product_Merchant) -> Unit)? = null
) : RecyclerView.Adapter<ProductsMerchantAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(product)
        }
    }

    override fun getItemCount(): Int = productList.size
}
