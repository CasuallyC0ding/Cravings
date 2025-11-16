package com.example.cravings.merchantFragments

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
import com.example.cravings.R
import com.example.cravings.baseActivities.ProductActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MerchantProductsFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var merchantNameText: TextView
    private lateinit var shopNameText: TextView
    private lateinit var phoneText: TextView
    private lateinit var manageProductsBtn: Button

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(
        "https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/"
    )

    private val userRole = "Merchant"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_merchant_products, container, false)

        profileImage = view.findViewById(R.id.profileImage)
        merchantNameText = view.findViewById(R.id.merchantNameText)
        shopNameText = view.findViewById(R.id.shopNameText)
        phoneText = view.findViewById(R.id.phoneText)
        manageProductsBtn = view.findViewById(R.id.manageProductsBtn)

        loadMerchantData()

        manageProductsBtn.setOnClickListener {
            startActivity(Intent(requireContext(), ProductActivity::class.java))
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadMerchantData()
    }

    private fun loadMerchantData() {
        val user = auth.currentUser ?: return

        val ref = database.reference.child("users").child(userRole).child(user.uid)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                merchantNameText.text = snapshot.child("name").value?.toString() ?: "Merchant"
                shopNameText.text = snapshot.child("shopName").value?.toString() ?: "Shop"
                phoneText.text = snapshot.child("phone").value?.toString() ?: ""

                val img = snapshot.child("profileImage").value?.toString()

                if (!img.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(img)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .into(profileImage)
                } else {
                    profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load info", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
