package com.example.cravings.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.cravings.merchantFragments.MerchantOrdersFragment
import com.example.cravings.merchantFragments.MerchantProductsFragment

class MerchantPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MerchantProductsFragment()
            1 -> MerchantOrdersFragment()
            else -> MerchantProductsFragment()
        }
    }
}
