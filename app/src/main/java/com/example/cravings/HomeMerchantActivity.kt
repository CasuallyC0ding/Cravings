package com.example.cravings

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    private lateinit var viewPager: androidx.viewpager2.widget.ViewPager2
    private lateinit var tabLayout: TabLayout

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
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        // Set top label
        roleTextView.text = userRole.uppercase()

        // Load profile image
        loadProfileImage()

        // Open ProfileActivity on click
        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("userRole", userRole)
            startActivity(intent)
        }

        // ✅ Attach ViewPager to adapter
        val pagerAdapter = MerchantPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        // ✅ Connect TabLayout + ViewPager with labels
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Products"
                1 -> tab.text = "Orders"
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
