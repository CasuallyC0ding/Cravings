package com.example.cravings.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cravings.R
import com.example.cravings.baseActivities.EditProductActivity
import com.example.cravings.baseActivities.HomeCustomerActivity
import com.example.cravings.baseActivities.HomeMerchantActivity
import com.example.cravings.baseActivities.MainActivity
import com.example.cravings.baseActivities.NotificationHandlerActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class OrderNotificationService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "orders_channel"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "========== FCM MESSAGE RECEIVED ==========")
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Data: ${remoteMessage.data}")
        Log.d(TAG, "Notification: ${remoteMessage.notification?.title} - ${remoteMessage.notification?.body}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Notification"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: ""

        val data = remoteMessage.data
        val type = data["type"] ?: ""
        val orderId = data["orderId"] ?: ""
        val customerId = data["customerId"] ?: ""
        val merchantId = data["merchantId"] ?: ""
        val productIndex = data["productIndex"] ?: ""
        val productName = data["productName"] ?: ""
        val status = data["status"] ?: ""

        Log.d(TAG, "Parsed - Type: $type, Status: $status, OrderId: $orderId")

        showNotification(title, body, type, orderId, customerId, merchantId, productIndex, productName, status)
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String,
        orderId: String,
        customerId: String,
        merchantId: String,
        productIndex: String,
        productName: String,
        status: String
    ) {
        createNotificationChannel()

        // Determine the target activity based on notification type
        val intent: Intent = when (type) {
            // ============================
            // CUSTOMER NOTIFICATIONS
            // ============================
            "order_status" -> {
                Log.d(TAG, "Creating CUSTOMER intent")
                Intent(this, HomeCustomerActivity::class.java).apply {
                    putExtra("open_orders", true)
                    putExtra("openOrdersTab", true)
                    putExtra("orderId", orderId)
                    putExtra("customerId", customerId)
                    putExtra("status", status)
                    putExtra("type", type)
                }
            }

            // ============================
            // MERCHANT NOTIFICATIONS
            // ============================
            "new_order", "merchant_status" -> {
                Log.d(TAG, "Creating MERCHANT intent for $type")
                Intent(this, HomeMerchantActivity::class.java).apply {
                    putExtra("open_orders", true)
                    putExtra("orderId", orderId)
                    putExtra("customerId", customerId)
                    putExtra("status", status)
                    putExtra("type", type)
                }
            }

            // ============================
            // STOCK ALERT
            // ============================
            "stock_alert" -> {
                // Route through HomeMerchantActivity to build proper activity stack
                Intent(this, HomeMerchantActivity::class.java).apply {
                    putExtra("fromNotification", true)
                    putExtra("open_products", true)
                    putExtra("productId", productIndex)
                    putExtra("productName", productName)
                    putExtra("type", type)
                    // CRITICAL: These flags ensure proper activity stack
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
            }

            // Default fallback
            else -> {
                Log.d(TAG, "Unknown type '$type', using MainActivity fallback")
                Intent(this, MainActivity::class.java).apply {
                    putExtra("type", type)
                }
            }
        }

        // CRITICAL: These flags ensure the app opens correctly when killed
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        )

        // Unique request code for each notification
        val requestCode = System.currentTimeMillis().toInt()

        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            // Full-screen intent for higher priority (optional)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        if (NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            NotificationManagerCompat.from(applicationContext).notify(requestCode, notification)
            Log.d(TAG, "✅ Notification displayed with ID: $requestCode")
        } else {
            Log.w(TAG, "❌ Notifications are disabled")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Orders Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for orders and stock alerts"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: ${token.take(20)}...")

        val uid = FirebaseAuth.getInstance().uid ?: return

        val db = FirebaseDatabase.getInstance(
            "https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/"
        ).reference

        // Save to both paths
        db.child("users").child("Merchant").child(uid).child("fcmToken").setValue(token)
        db.child("users").child("Customer").child(uid).child("fcmToken").setValue(token)
    }
}