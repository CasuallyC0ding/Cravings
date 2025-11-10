package com.example.cravings

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.RecyclerView

class ProductAdapter(
    private val productList: List<Product_Merchant>,
    private val onItemClick: ((Product_Merchant) -> Unit)? = null
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.txtProductName)
        val price: TextView = itemView.findViewById(R.id.txtProductPrice)
//        val image: ImageView = itemView.findViewById(R.id.imageProduct)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        holder.name.text = product.name
        holder.price.text = "${product.price} EGP"

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, EditProductActivity::class.java)
            intent.putExtra("productId", product.productId.toString())

            // ✅ Modern approach using ActivityOptions
            val options = ActivityOptionsCompat.makeCustomAnimation(
                context,
                R.anim.fade_in,  // enter animation
                R.anim.fade_out  // exit animation
            )

            context.startActivity(intent, options.toBundle())
        }
    }

    override fun getItemCount(): Int = productList.size
}