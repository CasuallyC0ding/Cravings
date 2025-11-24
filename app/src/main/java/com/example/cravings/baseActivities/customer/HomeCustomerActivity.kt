package com.example.cravings.baseActivities.customer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.cravings.R
import com.example.cravings.adapters.customer.ViewPagerAdapter
import com.example.cravings.utils.FCMTokenManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HomeCustomerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HomeCustomerActivity"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var userRole: String = "Customer"
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_customer)

        Log.d(TAG, "onCreate called")

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        userRole = intent.getStringExtra("userRole") ?: "Customer"

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        viewPager.adapter = ViewPagerAdapter(this, userRole)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "Home"
                    tab.setIcon(R.drawable.ic_home)
                }
                1 -> {
                    tab.text = "Orders"
                    tab.setIcon(R.drawable.ic_orders)
                }
                2 -> {
                    tab.text = "Account"
                    tab.setIcon(R.drawable.ic_profile_placeholder)
                }
            }
        }.attach()

        // Save FCM token for this customer
        FCMTokenManager.saveTokenForUser("Customer")

        // Handle notification click - check intent
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent called")
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent) {
        // Log all extras for debugging
        Log.d(TAG, "handleNotificationIntent - extras: ${intent.extras?.keySet()?.joinToString()}")

        val openOrdersTab = intent.getBooleanExtra("openOrdersTab", false)
        val openOrders = intent.getBooleanExtra("open_orders", false)
        val orderId = intent.getStringExtra("orderId")
        val status = intent.getStringExtra("status")

        Log.d(TAG, "openOrdersTab: $openOrdersTab, openOrders: $openOrders, orderId: $orderId, status: $status")

        if (openOrdersTab || openOrders) {
            Log.d(TAG, "Opening Orders tab")
            viewPager.post {
                viewPager.currentItem = 1  // Orders tab
            }
        }
    }
}