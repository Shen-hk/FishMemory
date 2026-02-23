package com.fishmemory.app.ui.publish.localarticlelist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fishmemory.app.data.local.rooms.AppDatabase
import com.fishmemory.app.data.local.rooms.entity.LocalArticleEntity
import com.fishmemory.app.databinding.ActivityLocalArticleListBinding
import com.fishmemory.app.ui.articledetail.LocalArticleDetailActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 本地文章管理列表页
 */
class LocalArticleListActivity : AppCompatActivity() {

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, LocalArticleListActivity::class.java)
        }
    }

    private lateinit var binding: ActivityLocalArticleListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalArticleListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.localArticleDao()

        val adapter = LocalArticleListAdapter(
            onClick = { entity ->
                // 点击跳转到文章详情页
                val intent = LocalArticleDetailActivity.createIntent(this, entity.localId)
                startActivity(intent)
            },
            onDelete = { entity ->
                // 点击删除按钮
                lifecycleScope.launch {
                    dao.deleteByLocalId(entity.localId)
                }
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }

        // 观察数据库变化，实时更新列表
        lifecycleScope.launch {
            dao.observeAll().collectLatest { list ->
                adapter.submitList(list)
                binding.emptyView.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }
}
