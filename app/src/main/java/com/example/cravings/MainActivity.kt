package com.example.cravings

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Start the RoleSelectionActivity by default
        val intent = Intent(this, RoleSelectionActivity::class.java)
        startActivity(intent)
        finish() // Prevent returning to MainActivity when pressing back
    }
}
