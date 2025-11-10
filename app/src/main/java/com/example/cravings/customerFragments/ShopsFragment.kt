package com.example.cravings.customerFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cravings.R
import com.example.cravings.adapters.ShopAdapter
import com.example.cravings.models.Shop
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ShopsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShopAdapter
    private lateinit var shopList: ArrayList<Shop>
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        database = FirebaseDatabase.getInstance("https://dbcravings-default-rtdb.europe-west1.firebasedatabase.app/")

        val view = inflater.inflate(R.layout.fragment_shops, container, false)

        recyclerView = view.findViewById(R.id.shopsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        shopList = arrayListOf()

        adapter = ShopAdapter(shopList)
        recyclerView.adapter = adapter
        fetchShopsFromFirebase()
        return view
    }

    private fun fetchShopsFromFirebase() {

        database.reference.child("users").child("Merchant").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                shopList.clear()

                for (shopSnapshot in snapshot.children) {
                    val shop = shopSnapshot.getValue(Shop::class.java)
                    if (shop != null) {
                        // Assuming your Shop model has a 'uid' field
                        val shopWithUid = shop.copy(uid = shopSnapshot.key ?: "")
                        shopList.add(shopWithUid)
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