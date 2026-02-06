package com.fishmemory.app

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.fishmemory.app.core.utils.autoSetStatusBarIcons
import com.fishmemory.app.core.utils.setStatusBarIconsBlack
import com.fishmemory.app.databinding.ActivityMainBinding
import com.fishmemory.app.ui.publish.PublishActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setStatusBarIconsBlack()


        // 获取 NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // 绑定 Tab 点击事件 → 使用全局 Action
        binding.tabHome.setOnClickListener {
            navController.navigate(R.id.homeFragment)
            updateTabSelection(0)
        }
        binding.tabHot.setOnClickListener {
            navController.navigate(R.id.hotFragment)
            updateTabSelection(1)
        }
        binding.tabMsg.setOnClickListener {
            navController.navigate(R.id.messageFragment)
            updateTabSelection(2)
        }
        binding.tabMe.setOnClickListener {
            navController.navigate(R.id.myFragment)
            updateTabSelection(3)
        }

        // 监听 Navigation 变化，同步 Tab 选中状态
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> updateTabSelection(0)
                R.id.hotFragment -> updateTabSelection(1)
                R.id.messageFragment -> updateTabSelection(2)
                R.id.myFragment -> updateTabSelection(3)
            }
        }

        // FAB 点击
        binding.fabPublish.setOnClickListener { onFabClick() }

        // 初始化选中状态（可选，通常 startDestination 是 home）
        updateTabSelection(0)
    }

    private fun updateTabSelection(index: Int) {
        binding.ivHome.isSelected = (index == 0)
        binding.tvHome.isSelected=(index==0)
        binding.ivHot.isSelected = (index == 1)
        binding.tvHot.isSelected = (index == 1)
        binding.ivMsg.isSelected = (index == 2)
        binding.tvMsg.isSelected = (index == 2)
        binding.ivMe.isSelected = (index == 3)
        binding.tvMe.isSelected = (index == 3)
    }

    private fun onFabClick() {
        // 向上移动足够距离，确保 BottomNavWithFab 变成直线
        val extraOffset = 80f
        val targetY = - (binding.fabPublish.height + extraOffset)

        ObjectAnimator.ofFloat(
            binding.fabPublish,
            "translationY",
            0f,
            targetY,
            0f
        ).apply {
            duration = 600
            addUpdateListener { animator ->
                val transY = animator.animatedValue as Float
                // 传入正数距离（越大，底部越平）
                binding.bottomNavBg.updateDistance(-transY)
            }
            doOnEnd {
                startActivity(Intent(this@MainActivity, PublishActivity::class.java))
            }
        }.start()
    }
}