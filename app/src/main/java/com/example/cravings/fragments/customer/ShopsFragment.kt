package com.example.cravings.fragments.customer

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cravings.R
import com.example.cravings.adapters.customer.ShopAdapter
import com.example.cravings.baseActivities.common.ProfileActivity
import com.example.cravings.baseActivities.customer.CartActivity
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

    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private var fullShopList = arrayListOf<Shop>()

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
        searchInput = view.findViewById(R.id.searchInput)
        searchButton = view.findViewById(R.id.searchButton)

        userRole = arguments?.getString("userRole") ?: "Customer"

        loadUserName()
        loadProfileImage()

        profileButton.setOnClickListener {
            startActivity(
                Intent(requireContext(), ProfileActivity::class.java)
                    .putExtra("userRole", userRole)
            )
        }

        cartIcon.setOnClickListener {
            startActivity(Intent(requireContext(), CartActivity::class.java))
        }

        recyclerView = view.findViewById(R.id.shopsRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        shopList = arrayListOf()
        adapter = ShopAdapter(shopList)
        recyclerView.adapter = adapter

        fetchShops()

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterShops(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        searchButton.setOnClickListener {
            filterShops(searchInput.text.toString())
        }

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
                    fullShopList.clear()
                    shopList.clear()
                    for (shopSnap in snapshot.children) {
                        shopSnap.getValue(Shop::class.java)?.let {
                            val shop = it.copy(uid = shopSnap.key ?: "")
                            fullShopList.add(shop)
                            shopList.add(shop)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun filterShops(query: String) {
        val filteredList = if (query.isEmpty()) {
            fullShopList
        } else {
            fullShopList.filter {
                it.shopName?.contains(query, ignoreCase = true) == true
            }
        }
        shopList.clear()
        shopList.addAll(filteredList)
        adapter.notifyDataSetChanged()
    }

    private fun updateCartBadge(count: Int) {
        if (count > 0) {
            cartBadge.text = count.toString()
            cartBadge.visibility = View.VISIBLE
            cartIcon.visibility = View.VISIBLE
        } else {
            cartBadge.visibility = View.GONE
            cartIcon.visibility = View.GONE
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