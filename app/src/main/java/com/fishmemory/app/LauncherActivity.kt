package com.fishmemory.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fishmemory.app.compose.ComposeMainActivity
import com.fishmemory.app.xml.activity.XmlMainActivity
import com.fishmemory.app.databinding.ActivityLauncherBinding

class LauncherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLauncherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 使用ViewBinding初始化布局
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置按钮点击事件
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnCompose.setOnClickListener {
            startActivity(Intent(this, ComposeMainActivity::class.java))
            finish()  // 关闭选择页面
        }

        binding.btnXml.setOnClickListener {
            startActivity(Intent(this, XmlMainActivity::class.java))
            finish()  // 关闭选择页面
        }
    }
}