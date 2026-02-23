package com.fishmemory.app.ui.home.category

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class CategoryPagerAdapter(
    fragment: Fragment,
    private val categories: List<String>
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        return CategoryPageFragment.newInstance(categories[position])
    }
}
