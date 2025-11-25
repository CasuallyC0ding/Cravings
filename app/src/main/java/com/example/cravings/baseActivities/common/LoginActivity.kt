package com.example.cravings.baseActivities.common

import android.graphics.Paint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cravings.R
import com.example.cravings.baseActivities.customer.HomeCustomerActivity
import com.example.cravings.baseActivities.merchant.HomeMerchantActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        val userRole = intent.getStringExtra("userRole") ?: return

        val loginRoleText = findViewById<TextView>(R.id.loginRoleText)
        loginRoleText.text = userRole.uppercase()

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val registerLink = findViewById<TextView>(R.id.registerRedirect)

        registerLink.paintFlags = registerLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: return@addOnSuccessListener

                    database.reference.child("users").child(userRole).child(userId).get()
                        .addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {

                                // ================================
                                // SAVE DEVICE FCM TOKEN BY ROLE
                                // ================================
                                FirebaseMessaging.getInstance().token
                                    .addOnSuccessListener { token ->
                                        database.reference
                                            .child("users")
                                            .child(userRole)  // "Merchant" or "Customer"
                                            .child(userId)
                                            .child("fcmToken")
                                            .setValue(token)
                                    }

                                Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()

                                val intent = when (userRole.lowercase()) {
                                    "merchant" -> Intent(this, HomeMerchantActivity::class.java)
                                    "customer" -> Intent(this, HomeCustomerActivity::class.java)
                                    else -> Intent(this, ProfileActivity::class.java)
                                }

                                intent.putExtra("userRole", userRole)
                                startActivity(intent)
                                finish()
                            } else {
                                auth.signOut()
                                Toast.makeText(this, "Logged in using wrong role!", Toast.LENGTH_LONG).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error checking role: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Login failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        registerLink.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            intent.putExtra("userRole", userRole)
            startActivity(intent)
        }
    }
}
