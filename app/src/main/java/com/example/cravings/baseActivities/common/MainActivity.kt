package com.example.cravings.baseActivities.common

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.cravings.baseActivities.customer.HomeCustomerActivity
import com.example.cravings.baseActivities.merchant.HomeMerchantActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "========== MAIN ACTIVITY STARTED ==========")
        Log.d(TAG, "Intent extras: ${intent.extras?.keySet()?.joinToString()}")

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        val currentUser = auth.currentUser

        if (currentUser != null) {
            // ✅ CRITICAL FIX: Check notification FIRST and return early
            if (checkNotificationIntent()) {
                Log.d(TAG, "Handled notification intent, MainActivity finishing")
                return // ✅ Don't call checkUserRoleAndRedirect!
            }

            // Only check role if NOT from notification
            Log.d(TAG, "No notification, checking user role")
            checkUserRoleAndRedirect(currentUser.uid)
        } else {
            // No user logged in, go to role selection
            Log.d(TAG, "No user logged in, redirecting to role selection")
            redirectToRoleSelection()
        }
    }

    /**
     * Check if launched from notification
     * @return true if handled, false otherwise
     */
    private fun checkNotificationIntent(): Boolean {
        val extras = intent.extras
        if (extras == null) {
            Log.d(TAG, "No extras found")
            return false
        }

        // Log all extras for debugging
        for (key in extras.keySet()) {
            Log.d(TAG, "Extra - $key: ${extras.get(key)}")
        }

        val type = extras.getString("type") ?: ""
        val fromNotification = extras.getBoolean("fromNotification", false)
        val openOrders = extras.getBoolean("open_orders", false)
        val openOrdersTab = extras.getBoolean("openOrdersTab", false)

        Log.d(TAG, "type: $type, fromNotification: $fromNotification, openOrders: $openOrders")

        // Handle if from notification OR has specific flags
        if (fromNotification || type.isNotEmpty() || openOrders || openOrdersTab) {
            Log.d(TAG, "Notification detected with type: $type")

            when (type) {
                // ============================
                // CUSTOMER NOTIFICATIONS
                // ============================
                "order_status" -> {
                    Log.d(TAG, "Opening HomeCustomerActivity for order_status")
                    val targetIntent = Intent(this, HomeCustomerActivity::class.java).apply {
                        putExtra("userRole", "Customer")
                        putExtra("open_orders", true)
                        putExtra("openOrdersTab", true)
                        putExtra("orderId", extras.getString("orderId"))
                        putExtra("customerId", extras.getString("customerId"))
                        putExtra("status", extras.getString("status"))
                        putExtra("fromNotification", true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(targetIntent)
                    finish()
                    return true
                }

                // ============================
                // MERCHANT NOTIFICATIONS
                // ============================
                "new_order", "merchant_status" -> {
                    Log.d(TAG, "Opening HomeMerchantActivity for $type")
                    val targetIntent = Intent(this, HomeMerchantActivity::class.java).apply {
                        putExtra("userRole", "Merchant")
                        putExtra("open_orders", true)
                        putExtra("orderId", extras.getString("orderId"))
                        putExtra("customerId", extras.getString("customerId"))
                        putExtra("status", extras.getString("status"))
                        putExtra("fromNotification", true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(targetIntent)
                    finish()
                    return true
                }

                // ============================
                // STOCK ALERT
                // ============================
                "stock_alert" -> {
                    Log.d(TAG, "Opening HomeMerchantActivity for stock_alert")
                    val targetIntent = Intent(this, HomeMerchantActivity::class.java).apply {
                        putExtra("userRole", "Merchant")
                        putExtra("open_products", true)
                        putExtra("productId", extras.getString("productId"))
                        putExtra("productName", extras.getString("productName"))
                        putExtra("fromNotification", true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(targetIntent)
                    finish()
                    return true
                }

                // ============================
                // FALLBACK: If type is empty but has notification flags
                // ============================
                else -> {
                    if (openOrders || openOrdersTab) {
                        Log.d(TAG, "No type but has openOrders flag, checking user role")
                        // Determine user type and open appropriate activity
                        checkUserRoleAndOpenOrders()
                        return true
                    }
                }
            }
        }

        Log.d(TAG, "Not a notification intent")
        return false
    }

    private fun checkUserRoleAndOpenOrders() {
        val uid = auth.currentUser?.uid ?: return

        database.reference.child("users").child("Customer").child(uid).get()
            .addOnSuccessListener { customerSnapshot ->
                if (customerSnapshot.exists()) {
                    Log.d(TAG, "User is Customer, opening customer home with orders tab")
                    val targetIntent = Intent(this, HomeCustomerActivity::class.java).apply {
                        putExtra("userRole", "Customer")
                        putExtra("open_orders", true)
                        putExtra("openOrdersTab", true)
                        putExtra("fromNotification", true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(targetIntent)
                    finish()
                } else {
                    // Must be merchant
                    Log.d(TAG, "User is Merchant, opening merchant home with orders tab")
                    val targetIntent = Intent(this, HomeMerchantActivity::class.java).apply {
                        putExtra("userRole", "Merchant")
                        putExtra("open_orders", true)
                        putExtra("fromNotification", true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(targetIntent)
                    finish()
                }
            }
    }

    private fun checkUserRoleAndRedirect(uid: String) {
        Log.d(TAG, "Checking user role for uid: ${uid.take(10)}...")

        // Check if user exists in Merchant collection
        database.reference.child("users").child("Merchant").child(uid).get()
            .addOnSuccessListener { merchantSnapshot ->
                if (merchantSnapshot.exists()) {
                    Log.d(TAG, "User is Merchant")
                    val intent = Intent(this, HomeMerchantActivity::class.java)
                    intent.putExtra("userRole", "Merchant")
                    startActivity(intent)
                    finish()
                } else {
                    Log.d(TAG, "Not a Merchant, checking Customer")
                    // Check if user exists in Customer collection
                    database.reference.child("users").child("Customer").child(uid).get()
                        .addOnSuccessListener { customerSnapshot ->
                            if (customerSnapshot.exists()) {
                                Log.d(TAG, "User is Customer")
                                val intent = Intent(this, HomeCustomerActivity::class.java)
                                intent.putExtra("userRole", "Customer")
                                startActivity(intent)
                                finish()
                            } else {
                                Log.d(TAG, "User not found in either role")
                                // User doesn't exist in either role
                                auth.signOut()
                                redirectToRoleSelection()
                            }
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "Error checking customer role: ${it.message}")
                            auth.signOut()
                            redirectToRoleSelection()
                        }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Error checking merchant role: ${it.message}")
                // Try customer as fallback
                database.reference.child("users").child("Customer").child(uid).get()
                    .addOnSuccessListener { customerSnapshot ->
                        if (customerSnapshot.exists()) {
                            Log.d(TAG, "User is Customer (fallback check)")
                            val intent = Intent(this, HomeCustomerActivity::class.java)
                            intent.putExtra("userRole", "Customer")
                            startActivity(intent)
                            finish()
                        } else {
                            Log.d(TAG, "User not found (fallback)")
                            auth.signOut()
                            redirectToRoleSelection()
                        }
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "Error in fallback check: ${it.message}")
                        auth.signOut()
                        redirectToRoleSelection()
                    }
            }
    }

    private fun redirectToRoleSelection() {
        Log.d(TAG, "Redirecting to RoleSelectionActivity")
        val intent = Intent(this, RoleSelectionActivity::class.java)
        startActivity(intent)
        finish()
    }
}