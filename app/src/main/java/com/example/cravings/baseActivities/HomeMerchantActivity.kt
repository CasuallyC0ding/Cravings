package com.example.cravings.baseActivities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.cravings.R
import com.example.cravings.adapters.MerchantPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeMerchantActivity : AppCompatActivity() {
    private lateinit var profileButton: ImageView
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private var userRole: String = "Merchant"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_merchant)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        userRole = intent.getStringExtra("userRole") ?: "Merchant"

        profileButton = findViewById(R.id.profileButton)
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        loadProfileImage()

        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("userRole", userRole)
            startActivity(intent)
        }

        val pagerAdapter = MerchantPagerAdapter(this)
        viewPager.adapter = pagerAdapter

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
            }
        }.attach()
    }

    override fun onResume() {
        super.onResume()
        loadProfileImage()
    }

    private fun loadProfileImage() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userId = currentUser.uid
        val userRef = database.reference.child("users").child(userRole).child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val imageUrl = snapshot.child("profileImage").getValue(String::class.java)
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this@HomeMerchantActivity)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .circleCrop()
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
}
