package com.fishmemory.app.ui.articledetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fishmemory.app.data.repository.ArticleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArticleDetailViewModelFactory(
    private val articleRepository: ArticleRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ArticleDetailViewModel(articleRepository) as T
    }
}

/**
 * 文章详情页状态与收藏/点赞业务。
 * 职责：从 Repository 加载收藏状态、切换收藏/点赞状态；不持有 Context/Activity/View。
 */
class ArticleDetailViewModel(
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private var articleId: Int = -1
    private var title: String = ""
    private var originalUrl: String = ""

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _isCollected = MutableStateFlow(false)
    val isCollected: StateFlow<Boolean> = _isCollected.asStateFlow()

    /** 初始化文章信息并异步加载收藏状态 */
    fun init(articleId: Int, title: String, originalUrl: String, initialFavorite: Boolean) {
        this.articleId = articleId
        this.title = title
        this.originalUrl = originalUrl
        _isFavorite.value = initialFavorite
        viewModelScope.launch {
            _isCollected.value = articleRepository.isCollected(articleId)
        }
    }

    fun toggleLike() {
        _isFavorite.value = !_isFavorite.value
    }

    fun toggleCollect() {
        viewModelScope.launch {
            val newValue = articleRepository.toggleCollect(articleId, title, originalUrl)
            _isCollected.value = newValue
        }
    }
}
