package com.example.cravings

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EditProductActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var nameField: EditText
    private lateinit var priceField: EditText
    private lateinit var quantityField: EditText
    private lateinit var descField: EditText
    private lateinit var updateBtn: Button

    private lateinit var dbRef: DatabaseReference
    private val sellerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val TAG = "EditProductActivity"

    private var selectedProductId: String? = null
    private var productsList = mutableListOf<Product_Merchant>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        recycler = findViewById(R.id.recyclerProducts)
        nameField = findViewById(R.id.editProductName)
        priceField = findViewById(R.id.editProductPrice)
        quantityField = findViewById(R.id.editProductQuantity)
        descField = findViewById(R.id.editProductDescription)
        updateBtn = findViewById(R.id.btnUpdateProduct)

        dbRef = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/").reference
            .child("users")
            .child("Merchant") // singular
            .child(sellerId)
            .child("products")

        recycler.layoutManager = LinearLayoutManager(this)
        loadAllProducts()

        updateBtn.setOnClickListener { updateSelectedProduct() }
    }

    private fun loadAllProducts() {
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productsList.clear()
                for (child in snapshot.children) {
                    val product = child.getValue(Product_Merchant::class.java)
                    if (product != null) {
                        productsList.add(product)
                    }
                }
                if (productsList.isEmpty()) {
                    Toast.makeText(this@EditProductActivity, "No products found", Toast.LENGTH_SHORT).show()
                } else {
                    val adapter = ProductAdapter(productsList) { product ->
                        populateFields(product)
                    }
                    recycler.adapter = adapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load products: ${error.message}")
                Toast.makeText(this@EditProductActivity, "Failed to load products", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun populateFields(product: Product_Merchant) {
        selectedProductId = product.productId.toString()
        nameField.setText(product.name)
        priceField.setText(product.price.toString())
        quantityField.setText(product.stock.toString())
        descField.setText(product.description)
    }

    private fun updateSelectedProduct() {
        val id = selectedProductId ?: run {
            Toast.makeText(this, "Select a product first", Toast.LENGTH_SHORT).show()
            return
        }
        val name = nameField.text.toString().trim()
        val price = priceField.text.toString().toDoubleOrNull() ?: 0.0
        val stock = quantityField.text.toString().toIntOrNull() ?: 0
        val description = descField.text.toString().trim()

        if (name.isEmpty() || price <= 0) {
            Toast.makeText(this, "Enter valid details", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mapOf(
            "name" to name,
            "price" to price,
            "stock" to stock,
            "description" to description
        )

        dbRef.child(id).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show()
                loadAllProducts() // refresh list
            }
            .addOnFailureListener { ex ->
                Toast.makeText(this, "Update failed: ${ex.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
