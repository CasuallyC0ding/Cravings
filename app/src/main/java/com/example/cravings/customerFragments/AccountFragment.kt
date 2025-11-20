package com.example.cravings.customerFragments

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
import com.example.cravings.R
import com.example.cravings.baseActivities.ProfileActivity
import com.example.cravings.baseActivities.VoipActivity
import com.example.cravings.delivery.DeliveryShopsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AccountFragment : Fragment() {

    private lateinit var roleTextView: TextView
    private lateinit var profileButton: ImageView
    private lateinit var btnEditProfile: Button
    private lateinit var btnDeliveryVolunteer: Button
    private lateinit var txtDeliveryPoints: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var userRole = "Customer"
    private lateinit var voip: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        roleTextView = view.findViewById(R.id.roleText)
        profileButton = view.findViewById(R.id.profileButton)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnDeliveryVolunteer = view.findViewById(R.id.btnDeliveryVolunteer)
        txtDeliveryPoints = view.findViewById(R.id.txtDeliveryPoints)
        voip=view.findViewById(R.id.btnvoip)
        userRole = arguments?.getString("userRole") ?: "Customer"

        loadUserName()
        loadProfileImage()
        loadDeliveryPoints()

        profileButton.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java)
                .putExtra("userRole", userRole))
        }

        btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java)
                .putExtra("userRole", userRole))
        }

        btnDeliveryVolunteer.setOnClickListener {
            applyForDelivery()
        }
        voip.setOnClickListener {
            startActivity(Intent(requireContext(), VoipActivity::class.java)
                .putExtra("userRole", userRole))

        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadUserName()
        loadProfileImage()
        loadDeliveryPoints()
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

                view?.findViewById<ImageView>(R.id.profileImageLarge)?.let { imgView ->
                    Glide.with(requireContext())
                        .load(it.child("profileImage").value)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(imgView)
                }
            }
    }

    private fun loadDeliveryPoints() {
        val uid = auth.currentUser?.uid ?: return
        database.reference.child("users").child("Customer").child(uid).child("deliveryPoints")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val points = snapshot.getValue(Double::class.java) ?: 0.0
                    txtDeliveryPoints.text = "Delivery Earnings: EGP %.2f".format(points)
                    txtDeliveryPoints.visibility = if (points > 0) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun applyForDelivery() {
        val uid = auth.currentUser?.uid ?: return

        // Check if already a volunteer
        database.reference.child("users").child("Customer").child(uid).child("isDeliveryVolunteer")
            .get().addOnSuccessListener { snapshot ->
                val isVolunteer = snapshot.getValue(Boolean::class.java) ?: false

                if (isVolunteer) {
                    // Already a volunteer, go to delivery shops
                    startActivity(Intent(requireContext(), DeliveryShopsActivity::class.java))
                } else {
                    // First time, register as volunteer
                    database.reference.child("users").child("Customer").child(uid)
                        .child("isDeliveryVolunteer").setValue(true)
                        .addOnSuccessListener {
                            // Initialize delivery points if not exists
                            database.reference.child("users").child("Customer").child(uid)
                                .child("deliveryPoints").setValue(0.0)

                            Toast.makeText(requireContext(), "You're now a delivery volunteer!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(requireContext(), DeliveryShopsActivity::class.java))
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to register: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    companion object {
        fun newInstance(role: String): AccountFragment {
            val fragment = AccountFragment()
            fragment.arguments = Bundle().apply { putString("userRole", role) }
            return fragment
        }
    }
}