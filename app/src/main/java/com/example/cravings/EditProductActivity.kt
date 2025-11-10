package com.example.cravings

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EditProductActivity : AppCompatActivity() {

    private lateinit var nameField: EditText
    private lateinit var priceField: EditText
    private lateinit var quantityField: EditText
    private lateinit var descField: EditText
    private lateinit var updateBtn: Button

    private lateinit var dbRef: DatabaseReference
    private val sellerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var productId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        nameField = findViewById(R.id.editProductName)
        priceField = findViewById(R.id.editProductPrice)
        quantityField = findViewById(R.id.editProductQuantity)
        descField = findViewById(R.id.editProductDescription)
        updateBtn = findViewById(R.id.btnUpdateProduct)

        productId = intent.getStringExtra("productId")

        dbRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child("Merchants")
            .child(sellerId)
            .child("products")

        loadProductDetails()
        updateBtn.setOnClickListener { updateProduct() }
    }

    private fun loadProductDetails() {
        val id = productId ?: return
        dbRef.child(id).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val product = snapshot.getValue(Product::class.java)
                if (product != null) {
                    nameField.setText(product.name)
                    priceField.setText(product.price.toString())
                    // quantityField.setText(product.quantity.toString()) // optional if added
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateProduct() {
        val id = productId ?: return
        val name = nameField.text.toString().trim()
        val price = priceField.text.toString().toDoubleOrNull() ?: 0.0

        if (name.isEmpty() || price <= 0) {
            Toast.makeText(this, "Please enter valid details", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mapOf(
            "name" to name,
            "price" to price
        )

        dbRef.child(id).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
