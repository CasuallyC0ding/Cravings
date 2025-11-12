package com.example.cravings.customerFragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.cravings.R
import com.example.cravings.baseActivities.ProfileActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class OrdersFragment : Fragment() {
    private lateinit var roleTextView: TextView
    private lateinit var profileButton: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var userRole = "Customer"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_orders, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        roleTextView = view.findViewById(R.id.roleText)
        profileButton = view.findViewById(R.id.profileButton)

        userRole = arguments?.getString("userRole") ?: "Customer"

        loadUserName()
        loadProfileImage()

        profileButton.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java)
                .putExtra("userRole", userRole))
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadUserName()
        loadProfileImage()
    }

    private fun loadUserName() {
        val uid = auth.currentUser?.uid ?: return
        database.reference.child("users").child(userRole).child(uid)
            .get().addOnSuccessListener {
                val name = it.child("name").getValue(String::class.java)
                roleTextView.text = "Welcome, ${name ?: "User"}!"
            }
    }

    private fun loadProfileImage() {
        val uid = auth.currentUser?.uid ?: return
        database.reference.child("users").child(userRole).child(uid)
            .get().addOnSuccessListener {
                Glide.with(requireContext())
                    .load(it.child("profileImage").value)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(profileButton)
            }
    }

    companion object {
        fun newInstance(role: String): OrdersFragment {
            val fragment = OrdersFragment()
            fragment.arguments = Bundle().apply { putString("userRole", role) }
            return fragment
        }
    }
}