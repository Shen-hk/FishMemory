package com.fishmemory.app.ui

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
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.fishmemory.app.NetworkClient
import com.fishmemory.app.model.ArticleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArticleViewModel : ViewModel() {
    private val _articles = MutableStateFlow<List<ArticleData>>(emptyList())
    val articles: StateFlow<List<ArticleData>> = _articles

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // 新增：搜索关键词
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // 新增：当前分类
    private val _currentCategory = MutableStateFlow("全部")
    val currentCategory: StateFlow<String> = _currentCategory

    // 计算属性：过滤后的文章列表
    val filteredArticles: StateFlow<List<ArticleData>> =
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
        }.combine(_currentCategory) { articles, category ->
            if (category == "全部") {
                articles
            } else {
                articles.filter { it.category == category }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    init {
        loadArticles()
    }

    fun loadArticles() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = NetworkClient.postApi.getPosts()
                val articleList = response.mapIndexed { index, apiResponse ->
                    ArticleData(
                        id = apiResponse.id,
                        title = apiResponse.title,
                        body = apiResponse.body,
                        userId = apiResponse.userId,
                        category = when (index % 4) {
                            0 -> "技术"
                            1 -> "前端"
                            2 -> "后端"
                            else -> "移动端"
                        },
                        tags = when (index % 4) {
                            0 -> listOf("Android", "Kotlin")
                            1 -> listOf("JavaScript", "Vue")
                            2 -> listOf("Java", "Spring")
                            else -> listOf("iOS", "Swift")
                        },
                        coverImage = "https://picsum.photos/400/200?random=${apiResponse.id}"
                    )
                }
                _articles.value = articleList
            } catch (e: Exception) {
                // 网络失败时使用模拟数据
                val mockArticles = listOf(
                    ArticleData(
                        id = 1,
                        title = "欢迎使用 FishMemory",
                        body = "这是一个仿掘金风格的技术文章阅读应用，采用最新的 Jetpack Compose 和 MVVM 架构开发。",
                        userId = 1,
                        category = "技术",
                        tags = listOf("Android", "Kotlin"),
                        coverImage = "https://picsum.photos/400/200?random=1"
                    ),
                    ArticleData(
                        id = 2,
                        title = "Compose 开发指南",
                        body = "学习如何使用 Jetpack Compose 构建现代化的 Android 应用界面。了解声明式UI的优势和最佳实践。",
                        userId = 1,
                        category = "移动端",
                        tags = listOf("Compose", "UI"),
                        coverImage = "https://picsum.photos/400/200?random=2"
                    ),
                    ArticleData(
                        id = 3,
                        title = "MVVM 架构最佳实践",
                        body = "了解如何在 Android 应用中正确实现 MVVM 架构模式，包括数据绑定、LiveData 和 ViewModel 的使用。",
                        userId = 1,
                        category = "架构",
                        tags = listOf("MVVM", "架构"),
                        coverImage = "https://picsum.photos/400/200?random=3"
                    )
                )
                _articles.value = mockArticles
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 新增：设置搜索关键词
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // 新增：设置当前分类
    fun setCurrentCategory(category: String) {
        _currentCategory.value = category
    }

    // 新增：切换收藏状态
    fun toggleFavorite(articleId: Int) {
        val updatedArticles = _articles.value.map { article ->
            if (article.id == articleId) {
                article.copy(isFavorite = !article.isFavorite)
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
                            // 这里可以打开搜索对话框或跳转到搜索页面
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleItem(
    article: ArticleData,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit = {}  // 添加点击参数
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
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