package com.fishmemory.app.xml.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.fishmemory.app.databinding.ActivityHotRankingBinding // 👈 注意包路径
import com.fishmemory.app.shared.model.HotRankItem
import com.fishmemory.app.xml.adapter.HotRankAdapter


class HotRankingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHotRankingBinding
    private lateinit var adapter: HotRankAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHotRankingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置 Toolbar
        setSupportActionBar(binding.toolbar)
       supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "热点榜"

        //初始化 RecyclerView
        setupRecyclerView()

        // 加载模拟榜单数据
        loadHotRankData()
    }

    private fun setupRecyclerView() {
        adapter = HotRankAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // 添加分割线（可选）
        val divider = DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
       binding.recyclerView.addItemDecoration(divider)
    }

    private fun loadHotRankData() {
        // 模拟排行榜数据（后续替换为真实 API 调用）
        val mockData = listOf(
            HotRankItem("1", "Android 15 新特性曝光", 9876, 1),
            HotRankItem("2", "Kotlin Multiplatform 发布正式版", 8765, 2),
            HotRankItem("3", "Jetpack Compose 性能优化指南", 7654, 3),
            HotRankItem("4", "Retrofit vs Ktor 如何选择？", 6543, 4),
            HotRankItem("5", "协程作用域管理最佳实践", 5432, 5),
            HotRankItem("6", "Flutter 与 Compose 对比分析", 4321, 6),
            HotRankItem("7", "AI 编程助手真的靠谱吗？", 3210, 7),
            HotRankItem("8", "现代 Android 架构指南更新", 2109, 8),
        )
        adapter.dataList = mockData
    }

    // 支持 Toolbar 的返回箭头点击
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}