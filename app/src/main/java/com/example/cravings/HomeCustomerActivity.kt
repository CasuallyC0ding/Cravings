package com.example.cravings

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class HomeCustomerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_customer)

        // Get the role passed through Intent
        val userRole = intent.getStringExtra("userRole") ?: "Customer"

        // Set the top label
        val roleTextView = findViewById<TextView>(R.id.roleText)
        roleTextView.text = userRole.uppercase()

        // Profile button
        val profileButton = findViewById<ImageView>(R.id.profileButton)

        // Placeholder for AWS image loading
        val imageUrl = "https://via.placeholder.com/150" // Replace later with AWS S3 URL
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .into(profileButton)

        // Open ProfileActivity on click
        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("userRole", userRole)
            startActivity(intent)
        }
    }
}