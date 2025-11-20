package com.example.cravings.baseActivities

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.cravings.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        val userRole = intent.getStringExtra("userRole")
        val nameInput = findViewById<EditText>(R.id.nameInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val signupBtn = findViewById<Button>(R.id.signupBtn)
        val loginRedirect = findViewById<TextView>(R.id.loginRedirect)
        loginRedirect.paintFlags = loginRedirect.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        signupBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            when {
                name.isEmpty() -> {
                    nameInput.error = "Please enter your name"
                    return@setOnClickListener
                }
                phone.isEmpty() -> {
                    phoneInput.error = "Please enter your phone number"
                    return@setOnClickListener
                }
                email.isEmpty() -> {
                    emailInput.error = "Please enter your email"
                    return@setOnClickListener
                }
                password.length < 6 -> {
                    passwordInput.error = "Password must be at least 6 characters"
                    return@setOnClickListener
                }
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: return@addOnCompleteListener

                        val userMap = mapOf(
                            "name" to name,
                            "phone" to phone,
                            "email" to email
                        )

                        // Save under users → Merchant or Customer → UID
                        if (userRole != null) {
                            database.reference.child("users").child(userRole).child(uid).setValue(userMap)
                                .addOnSuccessListener {

                                    // ====================================================
                                    // ONLY ADDITION: SAVE FCM TOKEN AFTER SIGNUP
                                    // ====================================================
                                    FirebaseMessaging.getInstance().token
                                        .addOnSuccessListener { token ->
                                            database.reference
                                                .child("users")
                                                .child(userRole)   // Merchant or Customer
                                                .child(uid)
                                                .child("fcmToken")
                                                .setValue(token)
                                        }
                                    // ====================================================

                                    Toast.makeText(this, "Account created successfully! Please log in.", Toast.LENGTH_SHORT).show()
                                    auth.signOut()

                                    val intent = Intent(this, LoginActivity::class.java)
                                    intent.putExtra("userRole", userRole)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Failed to save user: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    } else {
                        val error = task.exception?.message ?: "Signup failed"
                        Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                }
        }

        loginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
