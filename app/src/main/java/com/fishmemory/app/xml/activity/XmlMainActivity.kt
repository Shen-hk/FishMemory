package com.fishmemory.app.xml.activity

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.app.R
import com.fishmemory.app.databinding.ActivityArticleListBinding
import com.fishmemory.app.shared.model.ArticleData
import com.fishmemory.app.xml.adapter.ArticleAdapter
import kotlin.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import com.fishmemory.app.shared.api.PostApi
import com.fishmemory.app.shared.model.ArticleResponse
import com.fishmemory.app.shared.model.DmItArticle
import com.fishmemory.app.shared.network.NetworkClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class XmlMainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArticleAdapter
    private lateinit var binding: ActivityArticleListBinding
    private  var currentSelectedTab=0
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    //API引入
    private val postApi: PostApi by lazy { NetworkClient.postApi }
    //颜色资源引入
    private val colorPrimary by lazy { ContextCompat.getColor(this, R.color.app_primary) }
    private val colorSecondary by lazy { ContextCompat.getColor(this, R.color.app_secondary) }
    private val colorIconSelected by lazy { ContextCompat.getColor(this, R.color.icon_color_selected) }
    private val colorIconNormal by lazy { ContextCompat.getColor(this, R.color.icon_color) }

    private val articleDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { intent ->
                val updatedId = intent.getIntExtra("updated_article_id", -1)
                val updatedIsFavorite = intent.getBooleanExtra("updated_is_favorite", false)
                val updatedIsCollect = intent.getBooleanExtra("updated_is_collect", false)

                val position = adapter.dataList.indexOfFirst { it.id == updatedId }
                if (position != -1) {
                    val article = adapter.dataList[position]
                    article.isFavorite = updatedIsFavorite
                    article.isCollect = updatedIsCollect
                    adapter.notifyItemChanged(position)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       binding= ActivityArticleListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeUI()

    }
    private  fun initializeUI(){
        setupSearchView()
        setupBottomNavigation()
        setupRecyclerView()
        swipeRefreshLayout = binding.swipeRefreshLayout
        // 设置下拉刷新监听
        swipeRefreshLayout.setOnRefreshListener {
            loadData(isRefresh = true)
        }
        swipeRefreshLayout.setColorSchemeResources(
            R.color.app_primary,
            R.color.app_secondary
        )
        setupCategories()
        loadData(isRefresh=false)
    }


    //搜索功能
    private fun setupSearchView(){
        binding.searchButton.setOnClickListener {
            performSearch()
        }
        //软键盘搜索键
        binding.searchEditText.setOnEditorActionListener { _,actionId, _->
            if(actionId== EditorInfo.IME_ACTION_SEARCH){
                performSearch()
                true
            }else{false
        }
     }
    }
    private fun performSearch() {
        val query=binding.searchEditText.text.toString().trim()
        if (query.isNotEmpty()){
            searchArticles(query)
            hideKeyboard()//隐藏软键盘
        }else{
            Toast.makeText(this,"请输入搜索内容",Toast.LENGTH_SHORT).show()
        }
    }
    //隐藏软键盘
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }
    private fun searchArticles(query: String){
        Toast.makeText(this, "搜索: $query", Toast.LENGTH_SHORT).show()
        // 这里实现具体的搜索逻辑
        // adapter.filter.filter(query)

    }




    //标签分类
    private  fun setupCategories(){
        val categories=listOf("全部","工作","学习","生活","收藏")
        setupCategoryClickListeners(categories)
    }
    private fun setupCategoryClickListeners(categories: List<String>) {
        // 由于布局中已经有静态分类，我们直接设置点击事件
        val categoryViews = listOf(
            binding.categoryContainer.getChildAt(0) as TextView, // 全部
            binding.categoryContainer.getChildAt(1) as TextView, // 工作
            binding.categoryContainer.getChildAt(2) as TextView, // 学习
            binding.categoryContainer.getChildAt(3) as TextView, // 生活
            binding.categoryContainer.getChildAt(4) as TextView  // 收藏
        )

        categoryViews.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                // 更新分类选中状态
                updateCategorySelection(categoryViews, index)
                // 执行分类筛选
                filterByCategory(categories[index])
            }
        }
    }
    private  fun updateCategorySelection(views: List<TextView>,selectedIndex: Int){
        views.forEachIndexed { index, textView ->
            if (index == selectedIndex) {
                // 选中状态
                textView.setBackgroundResource(R.drawable.category_selected)
                textView.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                // 未选中状态
                textView.setBackgroundResource(R.drawable.category_normal)
                textView.setTextColor(ContextCompat.getColor(this, R.color.black))
            }
        }
    }
    private fun filterByCategory(category: String) {
        Toast.makeText(this, "筛选: $category", Toast.LENGTH_SHORT).show()
        // 这里实现分类筛选逻辑
        // when(category) {
        //     "全部" -> adapter.showAll()
        //     "工作" -> adapter.filterByCategory("work")
        //     // ... 其他分类
        // }
    }
    private fun setupBottomNavigation(){
        updateNavigationState(0)
        binding.navHome.setOnClickListener {
            if(currentSelectedTab!=0){
                switchTab(0)
                updateNavigationState(0)
            }
        }
        binding.navHot.setOnClickListener {
            if(currentSelectedTab!=1){
                switchTab(1)
                updateNavigationState(1)
            }
        }
        binding.navProfile.setOnClickListener {
            if(currentSelectedTab!=2){
                switchTab(2)
                updateNavigationState(2)
            }
        }
    }
    private fun switchTab(tabPosition:Int){
        currentSelectedTab=tabPosition
        when(tabPosition){
            0->showHomeContent()
            1->showHotContent()
            2->showProfileContent()
        }
    }
    private  fun updateNavigationState(selectedTab:Int){
        //重置标签状态
        resetNavigationStste()
        //设置选中状态
        when(selectedTab){
            0->setNavHomeSelected(true)
            1->setNavHotSelected(true)
            2->setNavProfileSelected(true)
        }
    }
    private fun resetNavigationStste(){
        setNavHomeSelected(false)
        setNavHotSelected(false)
        setNavProfileSelected(false)
    }
    private fun setNavHomeSelected(selected: Boolean){
        val iconColor=if(selected)colorIconSelected else colorIconNormal
        val textColor=if (selected)colorIconSelected else colorIconNormal
        binding.navHomeIcon.setColorFilter(iconColor)
        binding.navHomeText.setTextColor(textColor)
    }
    private fun setNavHotSelected(selected: Boolean){
        val iconColor=if(selected)colorIconSelected else colorIconNormal
        val textColor=if (selected)colorIconSelected else colorIconNormal
        binding.navHotIcon.setColorFilter(iconColor)
        binding.navHotText.setTextColor(textColor)
    }
    private fun setNavProfileSelected(selected: Boolean){
        val iconColor=if(selected)colorIconSelected else colorIconNormal
        val textColor=if (selected)colorIconSelected else colorIconNormal
        binding.navProfileIcon.setColorFilter(iconColor)
        binding.navProfileText.setTextColor(textColor)
    }
    private fun showHomeContent(){
        loadData()
        Toast.makeText(this, "首页", Toast.LENGTH_SHORT).show()
    }
    private fun showHotContent() {
        // 双重保险：防止在销毁后启动新 Activity
        if (isFinishing || isDestroyed) return

        try {
            val intent = Intent(this, HotRankingActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            // 可选：记录日志，便于调试
            e.printStackTrace()
        }
    }
    private fun showProfileContent(){
        Toast.makeText(this, "个人", Toast.LENGTH_SHORT).show()
    }
    private fun setupRecyclerView() {
        adapter = ArticleAdapter()

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@XmlMainActivity)
            adapter = this@XmlMainActivity.adapter

        }

        // 设置item点击事件
        adapter.onItemClick = { position, article ->
            openArticleDetail(article)
        }

        // 设置点赞点击事件
        adapter.onLikeClick = { position, article ->
            toggleArticleLike(position, article)
        }
        //设置收藏点击事件
        adapter.onCollectClick={position, article ->
            toggleArticleCollect(position,article)
        }
    }
private  var isLoading=false
    private fun loadData(isRefresh: Boolean=false) {
        // 显示加载状态
        if (isLoading) return
        isLoading=true
        if (!isRefresh)showLoading(true)

        // 使用协程进行网络请求
   lifecycleScope.launch {
            try {
                val apiKey="344f51df856bad0953057069d7de41fb"
                // 在 IO 线程执行网络请求
                val articles = withContext(Dispatchers.IO) {
                    postApi.getItNews(10,apiKey)
                }

                // 转换 API 响应为应用需要的 ArticleData 格式
                val articleDataList = articles.data.newslist.map{ response ->
                    convertToArticleData(response)
                }

                // 更新 UI（在主线程）
                adapter.setData(articleDataList)

                showLoading(false)
                swipeRefreshLayout.isRefreshing = false // 👈 关键：停止刷新动画
                if (isRefresh) {
                    Toast.makeText(this@XmlMainActivity, "刷新成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@XmlMainActivity, "加载成功，共 ${articleDataList.size} 篇文章", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                // 处理错误
                showLoading(false)
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(this@XmlMainActivity, "加载失败: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()

                // 可选：显示错误页面或重试按钮
                showErrorState()
            }
       finally {
           isLoading=false
           swipeRefreshLayout.isRefreshing=false
       }
        }
    }

    /**
     * 将 API 返回的 ArticleResponse 转换为应用需要的 ArticleData
     */
    private fun convertToArticleData(itArticle: DmItArticle): ArticleData {
        return ArticleData(
            id = itArticle.id.hashCode(), // 或考虑将 ArticleData.id 改为 String
            title = itArticle.title,
            body = itArticle.description,
            userId = 0,
            author = itArticle.source,
            category = "IT资讯",
            tags = listOf("科技", "新闻"),
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

    private fun showLoading(show: Boolean) {
        // 这里可以显示/隐藏加载进度条
        if (show) {
            Toast.makeText(this, "加载中...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showErrorState() {
        // 显示错误状态，可以添加重试按钮等
        Toast.makeText(this, "网络请求失败，请检查网络连接", Toast.LENGTH_LONG).show()

    }

    private fun toggleArticleLike(position: Int, article: ArticleData) {
        // 简单直接：切换点赞状态
        article.isFavorite = !article.isFavorite

        // 立即更新这一项的显示
        adapter.notifyItemChanged(position)

        // 显示提示

    }
    private  fun toggleArticleCollect(position: Int,article: ArticleData){
        article.isCollect=!article.isCollect
        adapter.notifyItemChanged(position)

    }
    companion object {
        private const val REQUEST_ARTICLE_DETAIL = 1001  // ← 添加这一行
    }

    private fun openArticleDetail(article: ArticleData) {
        val intent = Intent(this, ArticleDetailActivity::class.java).apply {
            putExtra("article_id", article.id)
            putExtra("article_title", article.title)
            putExtra("article_content", article.body)
            putExtra("article_author", article.author)
            putExtra("article_category", article.category)
            putExtra("article_created_at", article.createdAt)
            putExtra("article_read_time", article.readTime)
            putExtra("article_cover_image",article.coverImage)
            putExtra("article_like_count", article.likeCount)
            putExtra("article_comment_count", article.commentCount)
            putExtra("article_is_favorite", article.isFavorite)
            putExtra("article_is_collect", article.isCollect)
            putExtra("article_tags", article.tags.joinToString(","))
            putExtra("article_original_url",article.originalUrl)
        }
        articleDetailLauncher.launch(intent)
    }


}