package com.example.cravings

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    // Total number of pages (tabs)
    override fun getItemCount(): Int = 3

    // Return the correct fragment for each position
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ShopsFragment()      // 🏠 Home tab
            1 -> OrdersFragment()    // 📦 Orders tab
            2 -> AccountFragment()   // 👤 Account tab
            else -> ShopsFragment()
        }
    }
}
