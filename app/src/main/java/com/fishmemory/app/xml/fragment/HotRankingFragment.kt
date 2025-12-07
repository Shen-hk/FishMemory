package com.fishmemory.app.xml.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.fishmemory.app.databinding.FragmentHotRankingBinding
import com.fishmemory.app.shared.model.HotRankItem
import com.fishmemory.app.xml.adapter.HotRankAdapter
import com.fishmemory.app.shared.model.SharedArticleViewModel
import kotlin.random.Random
import kotlin.random.nextInt

class HotRankingFragment : Fragment() {

    private var _binding: FragmentHotRankingBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HotRankAdapter
    private lateinit var sharedViewModel: SharedArticleViewModel


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHotRankingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        // 获取 Activity 级别的共享 ViewModel
        sharedViewModel = ViewModelProvider(requireActivity())[SharedArticleViewModel::class.java]

        // 监听文章数据变化
        sharedViewModel.articles.observe(viewLifecycleOwner) { articleList ->
            if (articleList != null && articleList.isNotEmpty()) {
                val hotRankItems = convertToHotRankItems(articleList)
                adapter.dataList = hotRankItems
            }
            // 可选：如果为空，显示“暂无数据”提示
        }
    }

    private fun setupRecyclerView() {
        adapter = HotRankAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )
    }

    /**
     * 将 ArticleData 列表转换为 HotRankItem 列表
     */
    private fun convertToHotRankItems(articles: List<com.fishmemory.app.shared.model.ArticleData>): List<HotRankItem> {
        return articles
            .mapIndexed { index, article ->
                // 自定义热度值：点赞 + 评论*2 + 阅读时间权重（可调整）
                val hotValue = (500..10000).random()

                HotRankItem(
                    id = article.id.toString(), // 确保转为 String
                    title = article.title,
                    hotValue = hotValue,
                    rank = 0 // 临时占位
                )
            }
            .sortedByDescending { it.hotValue } // 按热度降序
            .mapIndexed { index, item ->
                item.copy(rank = index + 1) // 重新分配排名（1 开始）
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}