package com.example.cravings

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Start the LoginActivity by default
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Prevent returning to MainActivity when pressing back
    }
}
