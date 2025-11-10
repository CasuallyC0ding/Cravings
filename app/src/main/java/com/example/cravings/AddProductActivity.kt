package com.example.cravings

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID

class AddProductActivity : AppCompatActivity() {

    private lateinit var nameField: EditText
    private lateinit var priceField: EditText
    private lateinit var quantityField: EditText
    private lateinit var descField: EditText
    private lateinit var saveBtn: Button

    private val sellerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

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
        val name = nameField.text.toString().trim()
        val price = priceField.text.toString().toDoubleOrNull() ?: 0.0
        val quantity = quantityField.text.toString().toIntOrNull() ?: 0
        val desc = descField.text.toString().trim()

        if (name.isEmpty() || price <= 0) {
            Toast.makeText(this, "Please enter valid product details", Toast.LENGTH_SHORT).show()
            return
        }

        val productId = UUID.randomUUID().toString()
        val product = Product(
            productId = productId,
            sellerId = sellerId,
            name = name,
            price = price,
            imageUrl = "", // reserved for AWS S3 later
        )

        val dbRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child("Merchants")
            .child(sellerId)
            .child("products")
            .child(productId)

        dbRef.setValue(product)
            .addOnSuccessListener {
                Toast.makeText(this, "Product added!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
