package com.swanky.stoktakip.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.swanky.stoktakip.fragments.CompletedFragment
import com.swanky.stoktakip.fragments.UnCompletedFragment

class FragmentPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return if (position == 0)
            UnCompletedFragment()
        else
            CompletedFragment()
    }
}