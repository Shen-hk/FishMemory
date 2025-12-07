package com.fishmemory.app.xml.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fishmemory.app.R
import com.fishmemory.app.databinding.FragmentHomeBinding
import com.fishmemory.app.shared.model.ArticleData
import com.fishmemory.app.shared.model.DmItArticle
import com.fishmemory.app.shared.network.NetworkClient
import com.fishmemory.app.xml.adapter.ArticleAdapter
import com.fishmemory.app.xml.activity.ArticleDetailActivity
import com.fishmemory.app.shared.model.SharedArticleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ArticleAdapter
    private var allArticles = listOf<ArticleData>()
    private var isLoading = false
    private var currentCategory = "全部"

    // API 引入
    private val postApi by lazy { NetworkClient.postApi }

    // 👇 新增：共享 ViewModel（Activity 级别）
    private lateinit var sharedViewModel: SharedArticleViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化 Shared ViewModel
        sharedViewModel = ViewModelProvider(requireActivity())[SharedArticleViewModel::class.java]

        setupRecyclerView()
        setupSearchView()

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadData(isRefresh = true)
        }

        loadData(isRefresh = false)
    }

    private fun setupRecyclerView() {
        adapter = ArticleAdapter()
        adapter.onItemClick = { _, article ->
            val intent = Intent(requireContext(), ArticleDetailActivity::class.java).apply {
                putExtra("article_id", article.id)
                putExtra("article_title", article.title)
                putExtra("article_content", article.body)
                putExtra("article_author", article.author)
                putExtra("article_category", article.category)
                putExtra("article_created_at", article.createdAt)
                putExtra("article_read_time", article.readTime)
                putExtra("article_cover_image", article.coverImage)
                putExtra("article_like_count", article.likeCount)
                putExtra("article_comment_count", article.commentCount)
                putExtra("article_is_favorite", article.isFavorite)
                putExtra("article_is_collect", article.isCollect)
                putExtra("article_tags", article.tags.joinToString(","))
                putExtra("article_original_url", article.originalUrl ?: "")
            }
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupSearchView() {
        val searchEditText = binding.searchEditText
        val searchButton = binding.searchButton

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchEditText.text.toString().trim())
                hideKeyboard()
                true
            } else {
                false
            }
        }

        searchButton.setOnClickListener {
            performSearch(searchEditText.text.toString().trim())
            hideKeyboard()
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    filterByCategory(currentCategory)
                }
            }
        })
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            filterByCategory(currentCategory)
            return
        }
        val filtered = allArticles.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.author.contains(query, ignoreCase = true) ||
                    it.body.contains(query, ignoreCase = true)
        }
        adapter.setData(filtered)
    }

    private fun setupCategories(categories: Set<String>) {
        val container = binding.categoryContainer
        container.removeAllViews()

        // 添加“全部”按钮
        addCategoryButton("全部", isSelected = currentCategory == "全部") { selectedCategory ->
            currentCategory = selectedCategory
            filterByCategory(selectedCategory)
            // 注意：不再递归调用 setupCategories，避免无限循环
            // 而是重新获取当前所有 tags 并重建按钮（但这里我们只重建一次）
            // 实际上，categories 是固定的，所以可以直接传入
            rebuildCategoryButtons(categories)
        }

        // 添加其他分类
        for (category in categories.sorted()) {
            addCategoryButton(category, isSelected = currentCategory == category) { selectedCategory ->
                currentCategory = selectedCategory
                filterByCategory(selectedCategory)
                rebuildCategoryButtons(categories)
            }
        }
    }

    private fun rebuildCategoryButtons(categories: Set<String>) {
        setupCategories(categories)
    }

    private fun addCategoryButton(text: String, isSelected: Boolean, onClick: (String) -> Unit) {
        val textView = TextView(requireContext()).apply {
            this.text = text
            setPadding(32, 16, 32, 16) // ≈ 16dp horizontal padding
            textSize = 14f
            setBackgroundResource(
                if (isSelected) R.drawable.category_selected
                else R.drawable.category_normal
            )
            setTextColor(
                if (isSelected) ContextCompat.getColor(context, R.color.white)
                else ContextCompat.getColor(context, R.color.black)
            )
            setOnClickListener { onClick(text) }
        }
        if (text != "全部") {
            val lp = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 16 // ≈ 8dp
            }
            textView.layoutParams = lp
        }
        binding.categoryContainer.addView(textView)
    }

    private fun filterByCategory(category: String) {
        currentCategory = category
        val filtered = when (category) {
            "全部" -> allArticles
            else -> allArticles.filter { it.tags.contains(category) }
        }
        adapter.setData(filtered)
    }

    private fun loadData(isRefresh: Boolean) {
        if (isLoading) return
        isLoading = true
        if (!isRefresh) {
            Toast.makeText(requireContext(), "加载中...", Toast.LENGTH_SHORT).show()
        }
        binding.swipeRefreshLayout.isRefreshing = isRefresh

        lifecycleScope.launch {
            try {
                val apiKey = "344f51df856bad0953057069d7de41fb"
                val response = withContext(Dispatchers.IO) {
                    postApi.getItNews(10, apiKey)
                }

                if (response.code == 200) {
                    val articleDataList = response.data.newslist.map { dmArticle ->
                        convertToArticleData(dmArticle)
                    }

                    val allTags = mutableSetOf<String>()
                    articleDataList.forEach { article ->
                        allTags.addAll(article.tags)
                    }

                    withContext(Dispatchers.Main) {
                        allArticles = articleDataList
                        adapter.setData(articleDataList)

                        // 👇 关键：保存到 Shared ViewModel，供 HotRankingFragment 使用
                        sharedViewModel.setArticles(articleDataList)

                        setupCategories(allTags)

                        if (isRefresh) {
                            Toast.makeText(requireContext(), "刷新成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "加载成功，共 ${articleDataList.size} 篇文章",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    throw Exception("API 返回错误: ${response.msg}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "加载失败: ${e.message ?: "未知错误"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.swipeRefreshLayout.isRefreshing = false
                    isLoading = false
                }
            }
        }
    }

    /** 将 DmItArticle 转换为 ArticleData */
    private fun convertToArticleData(itArticle: DmItArticle): ArticleData {
        return ArticleData(
            id = itArticle.id.hashCode(), // Int 类型
            title = itArticle.title,
            body = itArticle.description,
            userId = 0,
            author = itArticle.source,
            category = "IT资讯",
            tags = listOf("科技", "新闻"), // 大米 API 无 tags，可后续扩展
            createdAt = itArticle.ctime.split(" ").firstOrNull() ?: itArticle.ctime,
            readTime = (itArticle.description.length / 200) + 1,
            likeCount = 0,
            commentCount = 0,
            coverImage = itArticle.picUrl.ifEmpty { null },
            isFavorite = false,
            isCollect = false,
            originalUrl = itArticle.url
        )
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}