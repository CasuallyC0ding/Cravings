package com.example.cravings.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

object FCMTokenManager {

    private const val TAG = "FCMTokenManager"
    private val database = FirebaseDatabase.getInstance(
        "https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/"
    )

    /**
     * Call this after user logs in or when activity starts
     * @param userRole "Customer" or "Merchant"
     */
    fun saveTokenForUser(userRole: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.w(TAG, "No user logged in, cannot save token")
            return
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "FCM Token obtained: ${token.take(20)}...")

            // Save token to the user's role path
            database.reference
                .child("users")
                .child(userRole)
                .child(uid)
                .child("fcmToken")
                .setValue(token)
                .addOnSuccessListener {
                    Log.d(TAG, "✅ FCM token saved for $userRole/$uid")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to save FCM token: ${e.message}")
                }
        }
    }

    /**
     * Remove token when user logs out
     */
    fun clearToken(userRole: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        database.reference
            .child("users")
            .child(userRole)
            .child(uid)
            .child("fcmToken")
            .removeValue()
            .addOnSuccessListener {
                Log.d(TAG, "✅ FCM token cleared for $userRole/$uid")
            }
    }
}