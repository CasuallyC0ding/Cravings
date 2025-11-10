package com.example.cravings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class ProductAdapter(
    private val productList: MutableList<Product>,
    private val onEditClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProduct: ImageView = view.findViewById(R.id.productImage)
        val txtName: TextView = view.findViewById(R.id.productName)
        val txtPrice: TextView = view.findViewById(R.id.productPrice)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        holder.txtName.text = product.name
        holder.txtPrice.text = "EGP %.2f".format(product.price)

        if (product.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(product.imageUrl)
                .placeholder(R.drawable.ic_edit)
                .into(holder.imgProduct)
        } else {
            holder.imgProduct.setImageResource(R.drawable.ic_edit)
        }

        holder.btnEdit.setOnClickListener { onEditClick(product) }
    }

    override fun getItemCount(): Int = productList.size
}
