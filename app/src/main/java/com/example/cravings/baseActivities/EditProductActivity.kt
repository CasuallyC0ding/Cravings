package com.example.cravings.baseActivities

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cravings.R
import com.example.cravings.adapters.ProductsMerchantAdapter
import com.example.cravings.models.Product_Merchant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EditProductActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var nameField: EditText
    private lateinit var priceField: EditText
    private lateinit var quantityField: EditText
    private lateinit var descField: EditText
    private lateinit var updateBtn: Button
    private lateinit var deleteBtn: Button
    private lateinit var backButton: ImageButton
    private lateinit var dbRef: DatabaseReference
    private lateinit var adapter: ProductsMerchantAdapter
    private val productsList = mutableListOf<Product_Merchant>()
    private val TAG = "EditProductActivity"
    private val sellerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var selectedProductId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        recycler = findViewById(R.id.recyclerProducts)
        nameField = findViewById(R.id.editProductName)
        priceField = findViewById(R.id.editProductPrice)
        quantityField = findViewById(R.id.editProductQuantity)
        descField = findViewById(R.id.editProductDescription)
        updateBtn = findViewById(R.id.btnUpdateProduct)
        deleteBtn = findViewById(R.id.btnDeleteProduct)
        backButton = findViewById(R.id.backButton)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ProductsMerchantAdapter(productsList) { product ->
            populateFields(product)
        }
        recycler.adapter = adapter

        dbRef = FirebaseDatabase
            .getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("users")
            .child("Merchant")
            .child(sellerId)
            .child("products")

        selectedProductId = intent.getStringExtra("productId")
        if (selectedProductId != null) {
            Log.d(TAG, "Opening directly for productId=$selectedProductId")
            loadSingleProduct(selectedProductId!!)
        } else {
            Log.d(TAG, "No productId passed; loading all products normally")
            loadAllProducts()
        }

        backButton.setOnClickListener {
            finish()
        }

        updateBtn.setOnClickListener { updateSelectedProduct() }
        deleteBtn.setOnClickListener { deleteSelectedProduct() }
    }

    private fun loadSingleProduct(id: String) {
        dbRef.child(id).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val product = snapshot.getValue(Product_Merchant::class.java)
                if (product != null) {
                    Log.d(TAG, "Loaded product: $product")
                    selectedProductId = id
                    populateFields(product)
                } else {
                    Toast.makeText(
                        this@EditProductActivity,
                        "Product not found",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load product: ${error.message}")
                Toast.makeText(
                    this@EditProductActivity,
                    "Failed to load product",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun loadAllProducts() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productsList.clear()
                Log.d(
                    TAG,
                    "Snapshot exists: ${snapshot.exists()} | Children count: ${snapshot.childrenCount}"
                )
                for (child in snapshot.children) {
                    val product = child.getValue(Product_Merchant::class.java)
                    if (product != null) {
                        if (child.key != null) {
                            product.productId = try {
                                child.key!!.toInt()
                            } catch (_: Exception) {
                                0
                            }
                        }
                        productsList.add(product)
                    }
                }
                adapter.notifyDataSetChanged()
                if (productsList.isEmpty()) {
                    Toast.makeText(
                        this@EditProductActivity,
                        "No products found",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Log.d(TAG, "Loaded ${productsList.size} products")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                Toast.makeText(
                    this@EditProductActivity,
                    "Failed to load products",
                    Toast.LENGTH_SHORT
                ).show()
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
                Toast.makeText(this, "Product updated successfully ✅", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteSelectedProduct() {
        val id = selectedProductId ?: run {
            Toast.makeText(this, "Select a product first", Toast.LENGTH_SHORT).show()
            return
        }

        dbRef.child(id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Product deleted successfully ✅", Toast.LENGTH_SHORT).show()
                clearFields()
                selectedProductId = null
            }
            .addOnFailureListener {
                Toast.makeText(this, "Delete failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearFields() {
        nameField.setText("")
        priceField.setText("")
        quantityField.setText("")
        descField.setText("")
    }
}
