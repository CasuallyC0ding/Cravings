package com.example.cravings.baseActivities.common

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.cravings.baseActivities.customer.HomeCustomerActivity
import com.example.cravings.baseActivities.merchant.HomeMerchantActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        val currentUser = auth.currentUser

        if (currentUser != null) {
            //check if the user came from a notification
            checkNotificationIntent()
            // User is logged in, check their role and redirect to appropriate home
            checkUserRoleAndRedirect(currentUser.uid)
        } else {
            // No user logged in, go to role selection
            redirectToRoleSelection()
        }
    }

    // Check if launched from notification
    private fun checkNotificationIntent() {
        val extras = intent.extras
        if (extras != null) {
            val type = extras.getString("type") ?: ""
            val fromNotification = extras.getBoolean("fromNotification", false)

            // Only handle if explicitly from notification
            if (fromNotification || type.isNotEmpty()) {
                when (type) {
                    // Customer notifications
                    "order_status" -> {
                        val targetIntent = Intent(this, HomeCustomerActivity::class.java).apply {
                            putExtra("open_orders", true)
                            putExtra("orderId", extras.getString("orderId"))
                            putExtra("status", extras.getString("status"))
                            putExtra("fromNotification", true)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(targetIntent)
                        finish()
                        return
                    }

                    // Merchant notifications
                    "new_order", "merchant_status" -> {
                        val targetIntent = Intent(this, HomeMerchantActivity::class.java).apply {
                            putExtra("open_orders", true)
                            putExtra("orderId", extras.getString("orderId"))
                            putExtra("fromNotification", true)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(targetIntent)
                        finish()
                        return
                    }

                    // Stock alert - Use NotificationHandlerActivity instead of direct EditProductActivity
                    "stock_alert" -> {
                        val targetIntent = Intent(this, NotificationHandlerActivity::class.java).apply {
                            putExtras(extras)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(targetIntent)
                        finish()
                        return
                    }
                }
            }
        }
    }

    private fun checkUserRoleAndRedirect(uid: String) {
        // Check if user exists in Merchant collection
        database.reference.child("users").child("Merchant").child(uid).get()
            .addOnSuccessListener { merchantSnapshot ->
                if (merchantSnapshot.exists()) {
                    // User is a Merchant, redirect to merchant home
                    val intent = Intent(this, HomeMerchantActivity::class.java)
                    intent.putExtra("userRole", "Merchant")
                    startActivity(intent)
                    finish()
                } else {
                    // Check if user exists in Customer collection
                    database.reference.child("users").child("Customer").child(uid).get()
                        .addOnSuccessListener { customerSnapshot ->
                            if (customerSnapshot.exists()) {
                                // User is a Customer, redirect to customer home
                                val intent = Intent(this, HomeCustomerActivity::class.java)
                                intent.putExtra("userRole", "Customer")
                                startActivity(intent)
                                finish()
                            } else {
                                // User doesn't exist in either role (shouldn't happen normally)
                                // Sign them out and redirect to role selection
                                auth.signOut()
                                redirectToRoleSelection()
                            }
                        }
                        .addOnFailureListener {
                            // Error checking customer role, sign out and redirect
                            auth.signOut()
                            redirectToRoleSelection()
                        }
                }
            }
            .addOnFailureListener {
                // Error checking merchant role, try customer as fallback
                database.reference.child("users").child("Customer").child(uid).get()
                    .addOnSuccessListener { customerSnapshot ->
                        if (customerSnapshot.exists()) {
                            val intent = Intent(this, HomeCustomerActivity::class.java)
                            intent.putExtra("userRole", "Customer")
                            startActivity(intent)
                            finish()
                        } else {
                            auth.signOut()
                            redirectToRoleSelection()
                        }
                    }
                    .addOnFailureListener {
                        auth.signOut()
                        redirectToRoleSelection()
                    }
            }
    }

    private fun redirectToRoleSelection() {
        val intent = Intent(this, RoleSelectionActivity::class.java)
        startActivity(intent)
        finish()
    }
}