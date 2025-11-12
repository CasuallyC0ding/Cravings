package com.example.cravings

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeMerchantActivity : AppCompatActivity() {

    private lateinit var profileButton: ImageView
    private lateinit var profileImage: ImageView
    private lateinit var roleTextView: TextView
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    private lateinit var manageProductsButton: Button

    private var userRole: String = "Merchant"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_merchant)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        // Get the role passed through Intent
        userRole = intent.getStringExtra("userRole") ?: "Merchant"

        // Initialize UI
        roleTextView = findViewById(R.id.roleText)
        profileButton = findViewById(R.id.profileButton)
        profileImage = findViewById(R.id.profileImage)
        manageProductsButton = findViewById(R.id.manageProductsBtn)

        // Set top label
        roleTextView.text = userRole.uppercase()

        // Setup ViewPager and TabLayout
        setupViewPager()

        // Open ProfileActivity on click
        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("userRole", userRole)
            startActivity(intent)
        }
        manageProductsButton.setOnClickListener {
            val intent = Intent(this, ProductActivity::class.java)
            startActivity(intent)
        }

    }

    private fun setupViewPager() {
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        val adapter = MerchantPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Products"
                1 -> "Orders"
                else -> "Products"
            }
        }.attach()
    }

    // 🔁 Refresh profile image each time user returns
    override fun onResume() {
        super.onResume()
        // The profile image loading will be handled in the ProductsFragment now
    }
}