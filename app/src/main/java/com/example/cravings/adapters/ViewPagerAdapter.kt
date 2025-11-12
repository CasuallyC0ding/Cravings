package com.example.cravings.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.cravings.customerFragments.AccountFragment
import com.example.cravings.customerFragments.OrdersFragment
import com.example.cravings.customerFragments.ShopsFragment

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

        fragment.arguments = Bundle().apply {
            putString("userRole", userRole)
        }

        return fragment
    }
}