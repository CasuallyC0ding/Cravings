package com.example.cravings

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class HomeMerchantActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_merchant)

        // Get the role passed through Intent
        val userRole = intent.getStringExtra("userRole") ?: "Merchant"

        // Display role text at the top
        val roleTextView = findViewById<TextView>(R.id.roleText)
        roleTextView.text = userRole.uppercase()

        // Profile button setup
        val profileButton = findViewById<ImageView>(R.id.profileButton)

        // Placeholder image (replace with AWS S3 link later)
        val imageUrl = "https://via.placeholder.com/150"
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .into(profileButton)

        // When profile is clicked → go to ProfileActivity
        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("userRole", userRole)
            startActivity(intent)
        }
    }
}