package com.example.cravings.adapters.customer

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.cravings.fragments.customer.ShopsFragment
import com.example.cravings.fragments.customer.OrdersFragment
import com.example.cravings.fragments.customer.AccountFragment

class ViewPagerAdapter(
    activity: FragmentActivity,
    private val userRole: String
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ShopsFragment.newInstance(userRole)
            1 -> OrdersFragment.newInstance(userRole)
            2 -> AccountFragment.newInstance(userRole)
            else -> ShopsFragment.newInstance(userRole)
        }
    }
}