package com.example.cravings.baseActivities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * This activity handles notification clicks when the app is killed.
 * It routes to the correct activity based on the notification type.
 */
class NotificationHandlerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "NotificationHandler"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "========== NOTIFICATION HANDLER ==========")

        // Log all extras
        intent.extras?.let { extras ->
            for (key in extras.keySet()) {
                Log.d(TAG, "Extra: $key = ${extras.get(key)}")
            }
        }

        val type = intent.getStringExtra("type") ?: ""
        val orderId = intent.getStringExtra("orderId") ?: ""
        val customerId = intent.getStringExtra("customerId") ?: ""
        val productIndex = intent.getStringExtra("productIndex") ?: intent.getStringExtra("productId") ?: ""
        val productName = intent.getStringExtra("productName") ?: ""
        val status = intent.getStringExtra("status") ?: ""

        Log.d(TAG, "Type: $type, OrderId: $orderId, ProductIndex: $productIndex")

        // Check if user is logged in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.d(TAG, "User not logged in, going to MainActivity")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Route based on notification type
        val targetIntent: Intent = when (type) {
            "order_status" -> {
                Log.d(TAG, "Routing to HomeCustomerActivity")
                Intent(this, HomeCustomerActivity::class.java).apply {
                    putExtra("open_orders", true)
                    putExtra("openOrdersTab", true)
                    putExtra("orderId", orderId)
                    putExtra("status", status)
                }
            }

            "new_order", "merchant_status" -> {
                Log.d(TAG, "Routing to HomeMerchantActivity")
                Intent(this, HomeMerchantActivity::class.java).apply {
                    putExtra("open_orders", true)
                    putExtra("orderId", orderId)
                    putExtra("customerId", customerId)
                    putExtra("status", status)
                }
            }

            "stock_alert" -> {
                Log.d(TAG, "Routing to HomeMerchantActivity first, then EditProductActivity")
                Intent(this, HomeMerchantActivity::class.java).apply {
                    putExtra("fromNotification", true)
                    putExtra("open_products", true)
                    putExtra("productId", productIndex)
                    putExtra("productName", productName)
                    putExtra("type", "stock_alert")
                }
            }

            else -> {
                Log.d(TAG, "Unknown type, routing to MainActivity")
                Intent(this, MainActivity::class.java)
            }
        }

        targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(targetIntent)
        finish()
    }
}