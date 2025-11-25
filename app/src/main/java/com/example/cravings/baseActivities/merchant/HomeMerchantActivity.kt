package com.example.cravings.baseActivities.merchant

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.cravings.R
import com.example.cravings.adapters.merchant.MerchantPagerAdapter
import com.example.cravings.baseActivities.common.ProfileActivity
import com.example.cravings.utils.FCMTokenManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeMerchantActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HomeMerchantActivity"
    }

    private lateinit var profileButton: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private var userRole = "Merchant"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_merchant)

        Log.d(TAG, "onCreate called")

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(
            "https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/"
        )

        profileButton = findViewById(R.id.profileButton)
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        userRole = intent.getStringExtra("userRole") ?: "Merchant"

        loadProfileImage()

        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("userRole", userRole)
            startActivity(intent)
        }

        viewPager.adapter = MerchantPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "Products"
                    tab.setIcon(R.drawable.ic_home)
                }

                1 -> {
                    tab.text = "Orders"
                    tab.setIcon(R.drawable.ic_orders)
                }

                2 -> {
                    tab.text = "VOIP"
                    tab.setIcon(R.drawable.ic_call)
                }
            }
        }.attach()

        // Save FCM token for this merchant
        FCMTokenManager.saveTokenForUser("Merchant")

        // Handle notification click
        handleNotificationIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        loadProfileImage()
    }

    private fun loadProfileImage() {
        val currentUser = auth.currentUser ?: return

        val userRef = database.reference
            .child("users").child(userRole).child(currentUser.uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val url = snapshot.child("profileImage").value?.toString()

                if (!url.isNullOrEmpty()) {
                    Glide.with(this@HomeMerchantActivity)
                        .load(url)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(profileButton)
                } else {
                    profileButton.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@HomeMerchantActivity,
                    "Failed to load profile image",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent called")
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent) {
        // Log all extras for debugging
        val extras = intent.extras
        Log.d(TAG, "handleNotificationIntent - extras: ${extras?.keySet()?.joinToString()}")

        val openOrders = intent.getBooleanExtra("open_orders", false)
        val openProducts = intent.getBooleanExtra("open_products", false)
        val fromNotification = intent.getBooleanExtra("fromNotification", false)
        val type = intent.getStringExtra("type") ?: ""
        val orderId = intent.getStringExtra("orderId")
        val productId = intent.getStringExtra("productId")
        val productName = intent.getStringExtra("productName")

        Log.d(TAG, "openOrders: $openOrders, openProducts: $openProducts, type: $type, orderId: $orderId, productId: $productId")

        when {
            openOrders -> {
                Log.d(TAG, "Opening Orders tab")
                viewPager.post {
                    viewPager.currentItem = 1  // Orders tab
                }
            }
            openProducts || type == "stock_alert" -> {
                Log.d(TAG, "Opening Products tab for stock alert")
                viewPager.post {
                    viewPager.currentItem = 0  // Products tab
                }

                // If from stock alert, show toast and navigate to the specific product
                if (fromNotification && type == "stock_alert" && !productId.isNullOrEmpty()) {
                    Log.d(TAG, "Stock alert - productId: $productId, productName: $productName")

                    viewPager.postDelayed({
                        Toast.makeText(
                            this,
                            "⚠️ '$productName' is out of stock!",
                            Toast.LENGTH_LONG
                        ).show()

                        // Navigate to EditProductActivity with the product details
                        val editIntent = Intent(this, EditProductActivity::class.java).apply {
                            putExtra("productId", productId)
                            putExtra("productName", productName)
                            putExtra("fromStockAlert", true)
                            putExtra("fromNotification", true)
                        }
                        startActivity(editIntent)
                    }, 500) // Delay to allow Products tab to load and show toast
                }
            }
        }
    }
}