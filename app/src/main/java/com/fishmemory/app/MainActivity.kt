package com.fishmemory.app

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.fishmemory.app.core.utils.view.setStatusBarIconsBlack
import com.fishmemory.app.databinding.ActivityMainBinding
import com.fishmemory.app.ui.common.BottomNavWithFab
import com.fishmemory.app.ui.publish.PublishActivity

/**
 * 单 Activity 容器：承载 NavHostFragment（Home/热榜/消息/我的）与底部导航栏、FAB。
 * 职责：Tab 点击与导航绑定、底部 Tab 选中态更新、FAB 点击跳转发表页；不负责各 Fragment 内部业务。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    /** 各 Tab 的 (ImageView, TextView) 对，顺序与 R.id.homeFragment / hotFragment / messageFragment / myFragment 一致 */
    private lateinit var tabViews: List<Pair<ImageView, TextView>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setStatusBarIconsBlack()

        navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        initTabViews()
        setupTabClickListeners()
        setupDestinationChangedListener()
        binding.fabPublish.setOnClickListener { onFabClick() }

        updateTabSelection(0)
    }

    private fun initTabViews() {
        tabViews = listOf(
            binding.ivHome to binding.tvHome,
            binding.ivHot to binding.tvHot,
            binding.ivMsg to binding.tvMsg,
            binding.ivMe to binding.tvMe
        )
    }

    private fun setupTabClickListeners() {
        binding.tabHome.setOnClickListener { navigateToTab(0, R.id.homeFragment) }
        binding.tabHot.setOnClickListener { navigateToTab(1, R.id.hotFragment) }
        binding.tabMsg.setOnClickListener { navigateToTab(2, R.id.messageFragment) }
        binding.tabMe.setOnClickListener { navigateToTab(3, R.id.myFragment) }
    }

    private fun navigateToTab(index: Int, navActionId: Int) {
        navController.navigate(navActionId)
        updateTabSelection(index)
    }

    private fun setupDestinationChangedListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> updateTabSelection(0)
                R.id.hotFragment -> updateTabSelection(1)
                R.id.messageFragment -> updateTabSelection(2)
                R.id.myFragment -> updateTabSelection(3)
                else -> { /* 其他目标（如详情）不改变底部选中态 */ }
            }
        }
    }

    /** 根据当前选中的 Tab 下标更新所有 Tab 的图标与文字选中状态 */
    private fun updateTabSelection(selectedIndex: Int) {
        tabViews.forEachIndexed { index, (iv, tv) ->
            val selected = (index == selectedIndex)
            iv.isSelected = selected
            tv.isSelected = selected
        }
    }

    private fun onFabClick() {
        val extraOffset = 80f
        val targetY = -(binding.fabPublish.height + extraOffset)

        ObjectAnimator.ofFloat(binding.fabPublish, "translationY", 0f, targetY, 0f).apply {
            duration = 600
            addUpdateListener { animator ->
                val transY = animator.animatedValue as Float
                (binding.bottomNavBg as? BottomNavWithFab)?.updateDistance(-transY)
            }
            doOnEnd {
                startActivity(Intent(this@MainActivity, PublishActivity::class.java))
            }
        }.start()
    }
}
