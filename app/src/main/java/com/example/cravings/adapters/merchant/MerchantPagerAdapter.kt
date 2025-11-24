package com.example.cravings.adapters.merchant

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.cravings.fragments.merchant.CallFragment
import com.example.cravings.fragments.merchant.MerchantOrdersFragment
import com.example.cravings.fragments.merchant.MerchantProductsFragment

class MerchantPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MerchantProductsFragment()
            1 -> MerchantOrdersFragment()
            2 -> CallFragment()
            else -> MerchantProductsFragment()
        }
    }
}