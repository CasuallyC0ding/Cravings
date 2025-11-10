package com.example.cravings.customerFragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cravings.baseActivities.ProfileActivity
import com.example.cravings.R
import com.example.cravings.adapters.ShopAdapter
import com.example.cravings.models.Shop
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ShopsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShopAdapter
    private lateinit var shopList: ArrayList<Shop>
    private lateinit var database: FirebaseDatabase

    private lateinit var roleTextView: TextView
    private lateinit var profileButton: ImageView
    private lateinit var auth: FirebaseAuth
    private var userRole: String = "Customer"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_shops, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        // Top bar UI
        roleTextView = view.findViewById(R.id.roleText)
        profileButton = view.findViewById(R.id.profileButton)

        // Get role passed from HomeActivity
        userRole = arguments?.getString("userRole") ?: "Customer"
        roleTextView.text = userRole.uppercase()

        loadProfileImage()

        profileButton.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java).putExtra("userRole", userRole))
        }

        // Shops list
        recyclerView = view.findViewById(R.id.shopsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        shopList = arrayListOf()
        adapter = ShopAdapter(shopList)
        recyclerView.adapter = adapter

        fetchShopsFromFirebase()

        return view
    }

    private fun loadProfileImage() {
        val currentUser = auth.currentUser ?: return
        database.reference.child("users").child(userRole).child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val imageUrl = snapshot.child("profileImage").getValue(String::class.java)
                    Glide.with(requireContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(profileButton)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchShopsFromFirebase() {
        database.reference.child("users").child("Merchant")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    shopList.clear()
                    for (shopSnap in snapshot.children) {
                        val shop = shopSnap.getValue(Shop::class.java)
                        if (shop != null) {
                            shopList.add(shop.copy(uid = shopSnap.key ?: ""))
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load shops", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
