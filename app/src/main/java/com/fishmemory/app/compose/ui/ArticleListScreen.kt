package com.fishmemory.app.compose.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// 正确的图标导入
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fishmemory.app.shared.network.NetworkClient
import com.fishmemory.app.shared.model.ArticleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArticleViewModel : ViewModel() {
//待优化：搜索按钮功能

    /*PullToRefreshBox (下拉刷新)
     └── Scaffold (整体骨架)
          ├── TopAppBar (顶部栏)
          └── Content (内容区)
                └── Surface (背景 surface)
                    ├── 加载状态 (旋转动画)
                    ├── 错误状态 (错误信息 + 重试按钮)
                    └── 正常状态
                        └── LazyColumn (滚动列表)
                            ├── CategoryFilter (分类筛选)
                            ├── SearchBar (搜索框)
                            ├── 空状态提示 (可选)
                            └── 文章列表 (多个ArticleItem)*/


    //状态定义-------->状态公告板
    private val _articles = MutableStateFlow<List<ArticleData>>(emptyList())
    val articles: StateFlow<List<ArticleData>> = _articles

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _searchQuery = MutableStateFlow("")          // 新增：搜索关键词
    val searchQuery: StateFlow<String> = _searchQuery

    private val _currentCategory = MutableStateFlow("全部")           // 新增：当前分类
    val currentCategory: StateFlow<String> = _currentCategory

    // 计算属性：过滤后的文章列表    流水线->搜索过滤->分类过滤
    val filteredArticles: StateFlow<List<ArticleData>> =

        //搜索过滤器
        _articles.combine(_searchQuery) { articles, query ->
            if (query.isBlank()) {
                articles
            } else {
                articles.filter { article ->
                    article.title.contains(query, ignoreCase = true) ||
                            article.body.contains(query, ignoreCase = true) ||
                            article.tags.any { tag -> tag.contains(query, ignoreCase = true) }
                }
            }
        }.combine(_currentCategory) { articles, category ->     //分类过滤器
            if (category == "全部") {
                articles
            } else {
                articles.filter { it.category == category }
            }
        }.stateIn(
            viewModelScope,//生命周期
            SharingStarted.WhileSubscribed(5000),//规律
            emptyList()//初始
        )

    init {
        loadArticles()
    }

    // 在 ArticleViewModel.kt 中替换 loadArticles() 函数

    fun loadArticles() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val apiKey = "344f51df856bad0953057069d7de41fb"
                val response = NetworkClient.postApi.getItNews(num = 20, apikey = apiKey)

                if (response.code == 200) {
                    val articleList = response.data.newslist.mapIndexed { index, item ->
                        // 自动分配分类和标签（根据来源或关键词）
                        val (category, tags) = categorizeBySource(item.source)

                        ArticleData(
                            id = item.id.hashCode(), // 转为 Int
                            title = item.title,
                            body = item.description,
                            userId = 0,
                            author = item.source,
                            category = category,
                            tags = tags,
                            coverImage = item.picUrl.takeIf { it.isNotBlank() },
                            createdAt = item.ctime,
                            readTime = 3,
                            likeCount = (100..999).random(),
                            commentCount = (10..99).random(),
                            isFavorite = false,
                            originalUrl = item.url
                        )
                    }
                    _articles.value = articleList
                } else {
                    throw Exception(response.msg ?: "API 返回错误")
                }
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "加载失败", e)
                // 失败时仍可使用 mock 数据（可选）
                _articles.value = generateMockArticles()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 辅助函数：根据来源自动打标签
    private fun categorizeBySource(source: String): Pair<String, List<String>> {
        return when {
            source.contains("前端|Vue|React|JavaScript", ignoreCase = true) -> "前端" to listOf("JavaScript", "Web")
            source.contains("Android|Kotlin|移动端", ignoreCase = true) -> "移动端" to listOf("Android", "Kotlin")
            source.contains("后端|Java|Spring|Python|Go", ignoreCase = true) -> "后端" to listOf("后端", "服务端")
            source.contains("AI|机器学习|大模型", ignoreCase = true) -> "AI" to listOf("AI", "LLM")
            else -> "技术" to listOf("IT", "资讯")
        }
    }

    // 保留 mock 数据（网络失败时兜底）
    private fun generateMockArticles(): List<ArticleData> = listOf(
        ArticleData(
            id = 1,
            title = "大米 API 加载成功！",
            body = "你现在看到的是来自真实 IT 媒体的新闻，如网易、腾讯科技等。",
            author = "FishMemory",
            category = "技术",
            tags = listOf("API", "Compose"),
            coverImage = "https://picsum.photos/400/200?random=999",
            createdAt = "2025-11-29",
            originalUrl = "https://api.qqsuu.cn"
        )
    )

    // 设置搜索关键词
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // 设置当前分类
    fun setCurrentCategory(category: String) {
        _currentCategory.value = category
    }

    // 切换收藏状态
    fun toggleFavorite(articleId: Int) {
        val updatedArticles = _articles.value.map { article ->
            if (article.id == articleId) {
                article.copy(isFavorite = !article.isFavorite)//不可变数据的更新利用copy 保持了不可变性又符合响应式
            } else {
                article
            }
        }
        _articles.value = updatedArticles
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    onArticleClick: (Int) -> Unit = {}  // 添加导航回调参数
) {
    val viewModel: ArticleViewModel = viewModel()
    val articles by viewModel.filteredArticles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentCategory by viewModel.currentCategory.collectAsState()

    // 使用正确的 PullToRefreshBox API
    PullToRefreshBox(
        isRefreshing = isLoading, // 传入刷新状态
        onRefresh = { viewModel.loadArticles() }, // 传入刷新回调
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "FishMemory",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        // 搜索按钮
                        IconButton(onClick = {
                            // 这里可以打开搜索对话框或跳转到搜索页面还没设置呢
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                        IconButton(onClick = { /* 更多 */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

                //error的不同状态
                if (isLoading && articles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (error != null && articles.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = error ?: "未知错误")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadArticles() }) {
                            Text("重试")
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 分类筛选栏
                        item {
                            CategoryFilter(
                                currentCategory = currentCategory,
                                onCategorySelected = { category ->
                                    viewModel.setCurrentCategory(category)
                                }
                            )
                        }

                        // 搜索框
                        item {
                            SearchBar(
                                searchQuery = searchQuery,
                                onSearchQueryChanged = { query ->
                                    viewModel.setSearchQuery(query)
                                }
                            )
                        }

                        // 如果没有搜索结果
                        if (articles.isEmpty() && searchQuery.isNotBlank()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "没有找到相关文章",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        items(articles) { article ->
                            ArticleItem(
                                article = article,
                                onFavoriteClick = {
                                    viewModel.toggleFavorite(article.id)
                                },
                                onClick = { onArticleClick(article.id) }  // 添加点击回调
                            )
                        }
                    }
                }
            }
        }
    }
}
//卡片设置
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleItem(
    article: ArticleData,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit = {}  // 添加点击参数
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick  // 设置点击事件
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 作者信息和分类
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = article.author,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${article.createdAt} · ${article.readTime}分钟阅读",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                // 分类标签
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = article.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // 文章内容
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = article.body.take(100) + if (article.body.length > 100) "..." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 封面图片
                if (article.coverImage != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    AsyncImage(
                        model = article.coverImage,
                        contentDescription = "文章封面",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // 标签和互动信息
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 标签
                Row {
                    article.tags.take(2).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                // 互动数据
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 收藏按钮
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (article.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (article.isFavorite) Color.Red else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "${if (article.isFavorite) article.likeCount + 1 else article.likeCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "${article.commentCount} 评论",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilter(
    currentCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("全部", "技术", "前端", "后端", "移动端", "架构")

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = currentCategory == category,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }

    if (isSearchActive) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("搜索文章...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "清除")
                        }
                    }
                }
            )
            TextButton(onClick = { isSearchActive = false }) {
                Text("取消")
            }
        }
    } else {
        Card(
            onClick = { isSearchActive = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "搜索文章...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}