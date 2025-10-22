package com.example.cravings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val userRole = intent.getStringExtra("userRole")

        auth = FirebaseAuth.getInstance()

        // ✅ Explicitly connect to your database URL
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")


        val greetingText = findViewById<TextView>(R.id.greetingText)
        val nameInput = findViewById<EditText>(R.id.nameInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val saveBtn = findViewById<Button>(R.id.saveBtn)
        val signOutBtn = findViewById<Button>(R.id.signOutBtn)

        val uid = auth.currentUser?.uid
        if (uid == null|| userRole == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // ✅ Load user data from Firebase
        database.reference.child("users").child(userRole).child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val name = snapshot.child("name").value?.toString() ?: "User"
                        val phone = snapshot.child("phone").value?.toString() ?: ""
                        val firstName = name.split(" ").firstOrNull() ?: name
                        greetingText.text = "Hello, $firstName 👋"

                        nameInput.setText(name)
                        phoneInput.setText(phone)
                    } else {
                        Toast.makeText(this@HomeActivity, "No data found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HomeActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })

        // ✅ Save updates
        saveBtn.setOnClickListener {
            val updatedName = nameInput.text.toString().trim()
            val updatedPhone = phoneInput.text.toString().trim()

            if (updatedName.isEmpty() || updatedPhone.isEmpty()) {
                Toast.makeText(this, "Please fill out both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = mapOf("name" to updatedName, "phone" to updatedPhone)
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

        // ✅ Sign out confirmation
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