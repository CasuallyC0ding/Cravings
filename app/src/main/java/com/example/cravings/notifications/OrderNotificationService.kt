package com.example.cravings.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cravings.R
import com.example.cravings.baseActivities.HomeMerchantActivity
import com.example.cravings.baseActivities.HomeCustomerActivity
import com.example.cravings.baseActivities.EditProductActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class OrderNotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        val title = remoteMessage.notification?.title ?: "Notification"
        val body = remoteMessage.notification?.body ?: ""
        val type = remoteMessage.data["type"] ?: ""
        val orderId = remoteMessage.data["orderId"]

        showNotification(title, body, type, orderId)
    }

    private fun showNotification(title: String, body: String, type: String, orderId: String?) {

        val channelId = "orders_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Orders Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        // 🔥 Routing logic based on 'type' sent by the server
        //type = "order_status"           → customer statuses
        //type = "new_order"              → merchant new order
        //type = "merchant_status"        → delivered, get orders
        //type = "stock_alert"            → stock alert
        val intent: Intent = when (type) {

            // ============================
            // CUSTOMER NOTIFICATIONS
            // ============================
            "order_status" -> {
                // Always open the Orders tab in HomeCustomerActivity
                Intent(this, HomeCustomerActivity::class.java).apply {
                    putExtra("open_orders", true)
                    putExtra("orderId", orderId)
                }
            }

            // ============================
            // MERCHANT NOTIFICATIONS
            // ============================

            "new_order",
            "merchant_status" -> {
                Intent(this, HomeMerchantActivity::class.java).apply {
                    putExtra("open_orders", true)
                    putExtra("orderId", orderId)
                }
            }

            // ============================
            // STOCK ALERT → Products tab
            // ============================
            "stock_alert" -> {
                Intent(this, HomeMerchantActivity::class.java).apply {
                    putExtra("open_products", true)
                }
            }


            // Default fallback
            else -> {
                Intent(this, HomeMerchantActivity::class.java)
            }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            NotificationManagerCompat.from(applicationContext)
                .notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val uid = FirebaseAuth.getInstance().uid ?: return

        FirebaseDatabase.getInstance(
            "https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/"
        ).reference.child("users").child("Merchant").child(uid).child("fcmToken")
            .setValue(token)
    }
}
