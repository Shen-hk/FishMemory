package com.fishmemory.app.shared.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fishmemory.app.data.model.ArticleData

class SharedArticleViewModel : ViewModel() {

    private val _articles = MutableLiveData<List<ArticleData>>(emptyList())
    val articles: LiveData<List<ArticleData>> = _articles

    fun setArticles(data: List<ArticleData>) {
        _articles.value = data
    }

    fun hasData(): Boolean = _articles.value?.isNotEmpty() == true
}