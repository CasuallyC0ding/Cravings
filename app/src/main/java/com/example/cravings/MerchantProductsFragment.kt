package com.example.cravings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MerchantProductsFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var merchantNameText: TextView
    private lateinit var shopNameText: TextView
    private lateinit var phoneText: TextView
    private lateinit var manageProductsBtn: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var userRole: String = "Merchant"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_merchant_products, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        // Initialize views
        profileImage = view.findViewById(R.id.profileImage)
        merchantNameText = view.findViewById(R.id.merchantNameText)
        shopNameText = view.findViewById(R.id.shopNameText)
        phoneText = view.findViewById(R.id.phoneText)
        manageProductsBtn = view.findViewById(R.id.manageProductsBtn)

        // Load merchant data
        loadMerchantData()

        // Set up button click
        manageProductsBtn.setOnClickListener {
            // You can navigate to product management activity here
            Toast.makeText(requireContext(), "Manage Products Clicked", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadMerchantData()
    }

    private fun loadMerchantData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "No user logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val userRef = database.reference.child("users").child(userRole).child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java) ?: "Merchant Name"
                val shop = snapshot.child("shopName").getValue(String::class.java) ?: "Shop Name"
                val phone = snapshot.child("phone").getValue(String::class.java) ?: "Phone Number"
                val imageUrl = snapshot.child("profileImage").getValue(String::class.java)

                merchantNameText.text = name
                shopNameText.text = shop
                phoneText.text = phone

                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(profileImage)
                } else {
                    profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Failed to load merchant data",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}