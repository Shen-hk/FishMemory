package com.fishmemory.app.ui.home.category

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fishmemory.app.App
import com.fishmemory.app.databinding.FragmentCategoryBinding
import com.fishmemory.app.data.model.ArticleSummary
import com.fishmemory.app.data.model.ArticleSummary.SourceType
import com.fishmemory.app.data.model.DmItArticle
import com.fishmemory.app.shared.viewmodel.SharedArticleViewModel
import com.fishmemory.app.core.network.NetworkClient
import com.fishmemory.app.data.repository.ArticleRepository
import com.fishmemory.app.data.local.rooms.AppDatabase
import com.fishmemory.app.data.local.rooms.entity.LocalArticleEntity
import com.fishmemory.app.ui.articledetail.ArticleDetailActivity
import com.fishmemory.app.ui.articledetail.LocalArticleDetailActivity
import com.fishmemory.app.ui.home.ArticleAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoryPageFragment : Fragment() {

    companion object {
        private const val ARG_CATEGORY = "arg_category"

        fun newInstance(category: String): CategoryPageFragment {
            return CategoryPageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY, category)
                }
            }
        }
    }

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ArticleAdapter
    private lateinit var repository: ArticleRepository
    private val postApi = NetworkClient.postApi

    private var allArticles = listOf<ArticleSummary>()
    private var currentCategory = "全部"
    private var isLoading = false
    private lateinit var sharedViewModel: SharedArticleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentCategory = arguments?.getString(ARG_CATEGORY) ?: "全部"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedViewModel =
            ViewModelProvider(requireActivity())[SharedArticleViewModel::class.java]

        repository = (requireActivity().application as App).articleRepository

        setupRecyclerView()
        setupRefresh()
        loadData(false)
        observeCollectStatus()
    }

    private fun setupRecyclerView() {
        adapter = ArticleAdapter()

        adapter.onItemClick = { _, article ->
            when (article.sourceType) {
                SourceType.API -> {
                    val intent = Intent(requireContext(), ArticleDetailActivity::class.java).apply {
                        putExtra("article_id", article.id.hashCode())
                        putExtra("article_title", article.title)
                        putExtra("article_content", article.summary)
                        putExtra("article_author", article.authorName)
                        putExtra("article_created_at", formatTime(article.publishTimeMs))
                        putExtra("article_original_url", article.apiUrl)
                    }
                    startActivity(intent)
                }
                SourceType.LOCAL -> {
                    article.localId?.let { localId ->
                        startActivity(LocalArticleDetailActivity.createIntent(requireContext(), localId))
                    }
                }
            }
        }

        adapter.onCollectClick = { _, article ->
            lifecycleScope.launch {
                // 收藏逻辑目前只对 API 文章生效，本地原创后续单独处理
                if (article.sourceType == SourceType.API) {
                    repository.toggleCollect(
                        article.id.hashCode(),
                        article.title,
                        article.apiUrl
                    )
                }
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadData(true)
        }
    }

    private fun loadData(isRefresh: Boolean) {
        if (isLoading) return
        isLoading = true
        binding.swipeRefreshLayout.isRefreshing = isRefresh

        lifecycleScope.launch {
            try {
                val merged = withContext(Dispatchers.IO) {
                    if (currentCategory == "本地") {
                        val db = AppDatabase.getInstance(requireContext().applicationContext)
                        val localList = db.localArticleDao().getByStatus()
                        localList.map { localToSummary(it) }
                    } else {
                        val response = postApi.getItNews(30, "930db1ee95830c08251c793656771228")
                        val apiArticles: List<ArticleSummary> = if (response.code == 200) {
                            response.data.newslist.map { convertToSummary(it) }
                        } else emptyList()
                        val db = AppDatabase.getInstance(requireContext().applicationContext)
                        val localList = db.localArticleDao().getByStatus()
                        val localSummaries = localList.map { localToSummary(it) }
                        (apiArticles + localSummaries).sortedByDescending { it.publishTimeMs }
                    }
                }
                allArticles = merged
                applyFilter()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
                isLoading = false
            }
        }
    }

    fun onSearch(keyword: String) {
        val filtered =
            if (keyword.isBlank()) allArticles
            else allArticles.filter {
                it.title.contains(keyword, true) ||
                        it.summary.contains(keyword, true) ||
                        it.authorName.contains(keyword, true)
            }
        adapter.setData(filtered)
    }

    fun onCategoryChanged(category: String) {
        currentCategory = category
        applyFilter()
    }

    private fun applyFilter() {
        val result = when (currentCategory) {
            "全部" -> allArticles
            "本地" -> allArticles.filter { it.sourceType == SourceType.LOCAL }
            "收藏" -> allArticles
            else -> allArticles
        }
        adapter.setData(result)
    }

    private fun observeCollectStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            val ids = allArticles.filter { it.sourceType == SourceType.API }.map { it.id.hashCode() }
            if (ids.isEmpty()) return@launch

            repository.observeCollectedStateMap(ids).collect { _ ->
                // TODO: 后续在 ArticleSummary 中加入收藏态并联动 UI
                applyFilter()
            }
        }
    }


    private fun localToSummary(e: LocalArticleEntity): ArticleSummary {
        return ArticleSummary(
            id = "local_${e.localId}",
            title = e.title,
            authorName = e.authorName,
            publishTimeMs = e.publishTimeMs,
            summary = e.summary ?: "",
            coverUrl = e.coverUrl,
            readCount = e.readCount,
            likeCount = e.likeCount,
            commentCount = e.commentCount,
            sourceType = SourceType.LOCAL,
            apiUrl = null,
            localId = e.localId
        )
    }

    private fun convertToSummary(it: DmItArticle): ArticleSummary {
        val id = "api_${it.id}"
        val summary = it.description.ifBlank { it.title }
        val cover = it.picUrl.ifEmpty { null }
        val publishTimeMs = System.currentTimeMillis() // TODO: 可以根据 ctime 解析
        return ArticleSummary(
            id = id,
            title = it.title,
            authorName = it.source,
            publishTimeMs = publishTimeMs,
            summary = summary,
            coverUrl = cover,
            readCount = 0,
            likeCount = 0,
            commentCount = 0,
            sourceType = SourceType.API,
            apiUrl = it.url,
            localId = null
        )
    }

    private fun formatTime(publishTimeMs: Long): String {
        // TODO: 后续统一抽成时间格式化工具，这里先简单用日期字符串
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(publishTimeMs))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
