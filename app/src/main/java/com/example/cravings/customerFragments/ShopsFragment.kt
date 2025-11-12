package com.example.cravings.customerFragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cravings.R
import com.example.cravings.adapters.ShopAdapter
import com.example.cravings.baseActivities.ProfileActivity
import com.example.cravings.baseActivities.CartActivity
import com.example.cravings.models.Shop
import com.example.cravings.utils.CartManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ShopsFragment : Fragment() {

    private lateinit var roleTextView: TextView
    private lateinit var profileButton: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShopAdapter
    private lateinit var shopList: ArrayList<Shop>

    private lateinit var cartIcon: ImageView
    private lateinit var cartBadge: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var userRole = "Customer"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_shops, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        roleTextView = view.findViewById(R.id.roleText)
        profileButton = view.findViewById(R.id.profileButton)
        cartIcon = view.findViewById(R.id.cartIcon)
        cartBadge = view.findViewById(R.id.cartBadge)

        userRole = arguments?.getString("userRole") ?: "Customer"

        // Load user data
        loadUserName()
        loadProfileImage()

        // Navigate to profile
        profileButton.setOnClickListener {
            startActivity(
                Intent(requireContext(), ProfileActivity::class.java)
                    .putExtra("userRole", userRole)
            )
        }

        // Navigate to cart
        cartIcon.setOnClickListener {
            startActivity(Intent(requireContext(), CartActivity::class.java))
        }

        // RecyclerView with 2-column grid layout
        recyclerView = view.findViewById(R.id.shopsRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        shopList = arrayListOf()
        adapter = ShopAdapter(shopList)
        recyclerView.adapter = adapter

        // Fetch shops from Firebase
        fetchShops()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadUserName()
        loadProfileImage()
        updateCartBadge(CartManager.getTotalItems())
    }

    private fun loadUserName() {
        val uid = auth.currentUser?.uid ?: return
        database.reference.child("users").child(userRole).child(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java)
                    roleTextView.text = "Welcome, ${name ?: "User"}!"
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadProfileImage() {
        val uid = auth.currentUser?.uid ?: return
        database.reference.child("users").child(userRole).child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Glide.with(requireContext())
                        .load(snapshot.child("profileImage").value)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(profileButton)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchShops() {
        database.reference.child("users").child("Merchant")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    shopList.clear()
                    for (shopSnap in snapshot.children) {
                        shopSnap.getValue(Shop::class.java)?.let {
                            shopList.add(it.copy(uid = shopSnap.key ?: ""))
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateCartBadge(count: Int) {
        if (count > 0) {
            cartBadge.text = count.toString()
            cartBadge.visibility = View.VISIBLE
            cartIcon.visibility=View.VISIBLE
        } else {
            cartBadge.visibility = View.GONE
            cartIcon.visibility=View.GONE
        }
    }

    companion object {
        fun newInstance(role: String): ShopsFragment {
            val fragment = ShopsFragment()
            fragment.arguments = Bundle().apply { putString("userRole", role) }
            return fragment
        }
    }
}
