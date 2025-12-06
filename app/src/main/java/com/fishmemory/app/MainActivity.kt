package com.fishmemory.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fishmemory.app.xml.activity.XmlMainActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 开发阶段：直接启动XML版本（跳过选择器）
        //val intent = Intent(this, XmlMainActivity::class.java)
       // startActivity(intent)
       // finish()

        // 或者如果你想用选择器：
        val intent = Intent(this, LauncherActivity::class.java)
         startActivity(intent)
        finish()
    }
}