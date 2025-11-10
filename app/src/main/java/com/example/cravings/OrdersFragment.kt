package com.example.cravings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class OrdersFragment : Fragment() {

    private lateinit var roleTextView: TextView
    private lateinit var profileButton: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var userRole: String = "Customer"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        val view = inflater.inflate(R.layout.fragment_orders, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        roleTextView = view.findViewById(R.id.roleText)
        profileButton = view.findViewById(R.id.profileButton)

        userRole = arguments?.getString("userRole") ?: "Customer"
        roleTextView.text = userRole.uppercase()

        loadProfileImage()

        profileButton.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            intent.putExtra("userRole", userRole)
            startActivity(intent)
        }

        return view
    }

    private fun loadProfileImage() {
        val currentUser = auth.currentUser ?: return
        val userRef = database.reference.child("users").child(userRole).child(currentUser.uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val imageUrl = snapshot.child("profileImage").getValue(String::class.java)
                Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(profileButton)
            }

            override fun onCancelled(error: DatabaseError) { }
        })
    }
}
