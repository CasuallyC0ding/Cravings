package com.example.cravings

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeCustomerActivity : AppCompatActivity() {

    private lateinit var profileButton: ImageView
    private lateinit var roleTextView: TextView
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private var userRole: String = "Customer"

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_customer)

        // 🔹 Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        // 🔹 Initialize layout components
        roleTextView = findViewById(R.id.roleText)
        profileButton = findViewById(R.id.profileButton)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        // 🔹 Get user role
        userRole = intent.getStringExtra("userRole") ?: "Customer"
        roleTextView.text = userRole.uppercase()

        // 🔹 Load profile image
        loadProfileImage()

        // 🔹 Profile button click → open ProfileActivity
        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("userRole", userRole)
            startActivity(intent)
        }

        // 🔹 Setup ViewPager2 + TabLayout
        setupTabs()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the image when the activity is resumed
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
                    Glide.with(this@HomeCustomerActivity)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(profileButton)
                } else {
                    profileButton.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomeCustomerActivity, "Failed to load profile image", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupTabs() {
        viewPager.adapter = ViewPagerAdapter(this)

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
    }
}
