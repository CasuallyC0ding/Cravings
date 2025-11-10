package com.example.cravings.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.cravings.customerFragments.ShopsFragment
import com.example.cravings.customerFragments.OrdersFragment
import com.example.cravings.customerFragments.AccountFragment

class ViewPagerAdapter(
    activity: FragmentActivity,
    private val userRole: String
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
            0 -> ShopsFragment()
            1 -> OrdersFragment()
            2 -> AccountFragment()
            else -> ShopsFragment()
        }

        // ✅ Pass userRole to fragment
        fragment.arguments = Bundle().apply {
            putString("userRole", userRole)
        }

        return fragment
    }
}
