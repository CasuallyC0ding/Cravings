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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.text.get

// this receives the FCM from the buyer and then creates a notification
class OrderNotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        val title = remoteMessage.notification?.title ?: "New Order"
        val body = remoteMessage.notification?.body ?: "A customer just placed an order"
        val orderId = remoteMessage.data["orderId"]

        showNotification(title, body, orderId)
    }

    private fun showNotification(title: String, body: String, orderId: String?) {

        val channelId = "orders_channel"

        // Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Orders Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        // Open HomeMerchantActivity and force open Orders tab
        val intent = Intent(this, HomeMerchantActivity::class.java)
        intent.putExtra("open_orders", true)
        intent.putExtra("orderId", orderId)
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

        // Android 13+ requires checking notification permission
        if (NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            NotificationManagerCompat.from(applicationContext)
                .notify(System.currentTimeMillis().toInt(), notification)
        }

    }
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // Save updated token for the current user if logged in
        val uid = FirebaseAuth.getInstance().uid ?: return

        FirebaseDatabase.getInstance(
            "https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/"
        ).reference.child("users").child("Merchant").child(uid).child("fcmToken")
            .setValue(token)
    }

}