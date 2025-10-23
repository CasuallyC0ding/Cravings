package com.example.cravings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val userRole = intent.getStringExtra("userRole")
        auth = FirebaseAuth.getInstance()
        database =
            FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        val greetingText = findViewById<TextView>(R.id.greetingText)
        val nameInput = findViewById<EditText>(R.id.nameInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val shopNameInput = findViewById<EditText>(R.id.shopNameInput)
        val shopNameLabel = findViewById<TextView>(R.id.shopNameLabel)
        val saveBtn = findViewById<Button>(R.id.saveBtn)
        val signOutBtn = findViewById<Button>(R.id.signOutBtn)

        val uid = auth.currentUser?.uid
        if (uid == null || userRole == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // ✅ Show shop name only if user is Merchant
        if (userRole == "Merchant") {
            shopNameInput.visibility = android.view.View.VISIBLE
            shopNameLabel.visibility = android.view.View.VISIBLE
        }

        // ✅ Load user data
        database.reference.child("users").child(userRole).child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val name = snapshot.child("name").value?.toString() ?: "User"
                        val phone = snapshot.child("phone").value?.toString() ?: ""
                        val shopName = snapshot.child("shopName").value?.toString() ?: ""
                        val firstName = name.split(" ").firstOrNull() ?: name

                        greetingText.text = "Hello, $firstName 👋"
                        nameInput.setText(name)
                        phoneInput.setText(phone)
                        if (userRole == "Merchant") {
                            shopNameInput.setText(shopName)
                        }
                    } else {
                        Toast.makeText(this@ProfileActivity, "No data found", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })

        // ✅ Save updates
        saveBtn.setOnClickListener {
            val updatedName = nameInput.text.toString().trim()
            val updatedPhone = phoneInput.text.toString().trim()

            if (updatedName.isEmpty() || updatedPhone.isEmpty()) {
                Toast.makeText(this, "Please fill out all required fields", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val updates = mutableMapOf<String, Any>(
                "name" to updatedName,
                "phone" to updatedPhone
            )

            // ✅ Add shop name if user is Merchant
            if (userRole == "Merchant") {
                val updatedShopName = shopNameInput.text.toString().trim()
                if (updatedShopName.isEmpty()) {
                    Toast.makeText(this, "Please enter your shop name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                updates["shopName"] = updatedShopName
            }

            database.reference.child("users").child(userRole).child(uid).updateChildren(updates)
                .addOnSuccessListener {
                    val firstName = updatedName.split(" ").firstOrNull() ?: updatedName
                    greetingText.text = "Hello, $firstName 👋"
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        // ✅ Sign out
        signOutBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    auth.signOut()
                    startActivity(Intent(this, RoleSelectionActivity::class.java))
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}