package com.fishmemory.app.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fishmemory.app.R
import com.fishmemory.app.databinding.ItemArticleBinding
import com.fishmemory.app.data.model.ArticleSummary
import com.fishmemory.app.core.utils.view.clickFeedback
import com.fishmemory.app.core.utils.view.vibrate

/**
 * 文章列表项 Adapter：绑定标题/摘要/作者/封面，点赞与收藏状态与点击回调。
 * 职责：仅做 UI 绑定与事件转发，不持有 Repository 或修改数据。
 */
class ArticleAdapter : RecyclerView.Adapter<ArticleAdapter.ViewHolder>() {

    var dataList: List<ArticleSummary> = emptyList()
    var onItemClick: ((Int, ArticleSummary) -> Unit)? = null
    var onLikeClick: ((Int, ArticleSummary) -> Unit)? = null
    var onCollectClick: ((Int, ArticleSummary) -> Unit)? = null

    fun setData(newData: List<ArticleSummary>) {
        dataList = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position], position)
    }

    override fun getItemCount(): Int = dataList.size

    inner class ViewHolder(private val binding: ItemArticleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(article: ArticleSummary, position: Int) {
            bindBasic(binding, article)
            // TODO: 点赞/收藏状态后续接入 ArticleSummary
            bindLikeState(binding, false)
            bindCollectState(binding, false)
            bindClicks(binding, article, position)
        }

        private fun bindBasic(binding: ItemArticleBinding, article: ArticleSummary) {
            binding.tvTitle.text = article.title
            binding.tvSummary.text = article.summary
            binding.tvAuthor.text = "作者: ${article.authorName}"
            binding.tvDate.text = ""
            val url = article.coverUrl?.takeIf { it.isNotBlank() }
            Glide.with(binding.ivCover.context)
                .load(url)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.ivCover)
        }

        private fun bindLikeState(binding: ItemArticleBinding, isLiked: Boolean) {
            binding.ivLike.isSelected = isLiked
            binding.ivLike.setColorFilter(if (isLiked) Color.RED else Color.GRAY)
        }

        private fun bindCollectState(binding: ItemArticleBinding, isCollected: Boolean) {
            binding.ivCollect.isSelected = isCollected
            binding.ivCollect.setColorFilter(if (isCollected) Color.DKGRAY else Color.GRAY)
        }

        private fun bindClicks(binding: ItemArticleBinding, article: ArticleSummary, position: Int) {
            binding.root.setOnClickListener { onItemClick?.invoke(position, article) }
            binding.llLike.setOnClickListener {
                onLikeClick?.invoke(position, article)
                binding.ivLike.clickFeedback()
                binding.root.context.vibrate(50, 45)
            }
            binding.llCollect.setOnClickListener {
                onCollectClick?.invoke(position, article)
                binding.ivCollect.clickFeedback()
                binding.root.context.vibrate(10, 2)
            }
        }
    }
}