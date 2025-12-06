package com.fishmemory.app.xml.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.fishmemory.app.databinding.ItemArticleBinding
import com.fishmemory.app.shared.model.ArticleData

class ArticleAdapter : RecyclerView.Adapter<ArticleAdapter.ViewHolder>() {

    var dataList: List<ArticleData> = emptyList()
    var onItemClick: ((Int, ArticleData) -> Unit)? = null
    var onLikeClick: ((Int, ArticleData) -> Unit)? = null
    var onCollectClick: ((Int, ArticleData) -> Unit)? = null
//数据更新
    fun setData(newData: List<ArticleData>) {
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

        fun bind(article: ArticleData, position: Int) {
            // 绑定数据
            binding.run {
                tvTitle.text = article.title
                tvSummary.text = article.body
                tvAuthor.text = "作者: ${article.author}"
                tvDate.text=article.createdAt
                Glide.with(ivCover.context)
                    .load(article.coverImage)
                    .into(ivCover)

                // 点赞状态
                ivLike.isSelected = article.isFavorite
                ivLike.setColorFilter(
                    if (article.isFavorite) Color.RED else Color.GRAY
                )

                // 收藏状态
                ivCollect.isSelected = article.isCollect
                ivCollect.setColorFilter(
                    if (article.isCollect) Color.DKGRAY else Color.GRAY
                )
            }

            // 设置点击事件（使用 itemView 和容器）
            binding.root.setOnClickListener {
                onItemClick?.invoke(position, article)
            }

            binding.llLike.setOnClickListener {
                onLikeClick?.invoke(position, article)
            }

            binding.llCollect.setOnClickListener {
                onCollectClick?.invoke(position, article)
            }
        }
    }
}