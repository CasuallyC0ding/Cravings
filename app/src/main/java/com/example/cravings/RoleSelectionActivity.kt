package com.example.cravings

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity

class RoleSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        val merchantBtn = findViewById<Button>(R.id.merchantBtn)
        val customerBtn = findViewById<Button>(R.id.customerBtn)

        merchantBtn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("userRole", "Merchant")
            startActivity(intent)
        }

        customerBtn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("userRole", "Customer")
            startActivity(intent)
        }
    }
}
