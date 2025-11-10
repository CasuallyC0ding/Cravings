package com.example.cravings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AddProductActivity : AppCompatActivity() {

    private lateinit var nameField: EditText
    private lateinit var priceField: EditText
    private lateinit var quantityField: EditText
    private lateinit var descField: EditText
    private lateinit var saveBtn: Button

    private val sellerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val dbRef =  FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")
    private val TAG = "AddProductActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        nameField = findViewById(R.id.editProductName)
        priceField = findViewById(R.id.editProductPrice)
        quantityField = findViewById(R.id.editProductQuantity)
        descField = findViewById(R.id.editProductDescription)
        saveBtn = findViewById(R.id.btnSaveProduct)

        saveBtn.setOnClickListener { saveProduct() }
    }

    private fun saveProduct() {
        if (sellerId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "User not logged in – sellerId is empty.")
            return
        }

        val name = nameField.text.toString().trim()
        val price = priceField.text.toString().toDoubleOrNull() ?: 0.0
        val quantity = quantityField.text.toString().toIntOrNull() ?: 0
        val desc = descField.text.toString().trim()

        if (name.isEmpty() || price <= 0) {
            Toast.makeText(this, "Please enter valid product details", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Invalid product details entered: name='$name', price=$price")
            return
        }

        ensureMerchantPathExists {
            getNextProductId { nextId ->
                val productMerchant = Product_Merchant(
                    productId = nextId,
                    name = name,
                    price = price,
                    stock = quantity,
                    description = desc,
                    imageUrl = ""
                )

                val productRef = dbRef.reference
                    .child("users")
                    .child("Merchant")
                    .child(sellerId)
                    .child("products")
                    .child(nextId.toString())

                productRef.setValue(productMerchant)
                    .addOnSuccessListener {
                        Log.i(TAG, "Product added successfully with ID $nextId")
                        updateLastProductId(nextId)
                        Toast.makeText(this, "Product added successfully!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, ProductActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { ex ->
                        Log.e(TAG, "Failed to save product: ${ex.message}", ex)
                        Toast.makeText(this, "Failed to save: ${ex.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    /**
     * Ensures the merchant node and "products" subnode exist before writing.
     */
    private fun ensureMerchantPathExists(onReady: () -> Unit) {
        val merchantRef = dbRef.reference.child("users").child("Merchant").child(sellerId)
        merchantRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.w(TAG, "Merchant node not found. Creating structure.")
                    val defaultData = mapOf(
                        "lastProductId" to 0,
                        "products" to mapOf<String, Any>()
                    )
                    merchantRef.setValue(defaultData)
                        .addOnSuccessListener {
                            Log.i(TAG, "Merchant structure created.")
                            onReady()
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "Failed to create merchant structure: ${it.message}", it)
                            Toast.makeText(this@AddProductActivity, "Error initializing merchant node", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // If products child missing, ensure it exists
                    if (!snapshot.hasChild("products")) {
                        Log.w(TAG, "Merchant found but missing 'products' node. Creating it.")
                        merchantRef.child("products").setValue(mapOf<String, Any>())
                            .addOnSuccessListener {
                                Log.i(TAG, "'products' node created successfully.")
                                onReady()
                            }
                            .addOnFailureListener {
                                Log.e(TAG, "Failed to create 'products' node: ${it.message}", it)
                                Toast.makeText(this@AddProductActivity, "Error creating products node", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Log.d(TAG, "Merchant and products path verified.")
                        onReady()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error while ensuring path: ${error.message}")
                Toast.makeText(this@AddProductActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getNextProductId(callback: (Int) -> Unit) {
        val lastIdRef = dbRef.reference.child("users").child("Merchant").child(sellerId).child("lastProductId")

        lastIdRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lastId = snapshot.getValue(Int::class.java) ?: 0
                val nextId = lastId + 1
                Log.d(TAG, "Next product ID calculated: $nextId (last=$lastId)")
                callback(nextId)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching lastProductId: ${error.message}")
                callback(1)
            }
        })
    }

    private fun updateLastProductId(newId: Int) {
        val lastIdRef = dbRef.reference
            .child("users")
            .child("Merchant")
            .child(sellerId)
            .child("lastProductId")

        lastIdRef.setValue(newId)
            .addOnSuccessListener {
                Log.d(TAG, "lastProductId updated to $newId")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show()
            }
    }
}
