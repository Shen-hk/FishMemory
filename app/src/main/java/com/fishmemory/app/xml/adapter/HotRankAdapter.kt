package com.fishmemory.app.xml.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.app.R
import com.fishmemory.app.databinding.ItemRankingBinding
import com.fishmemory.app.shared.model.HotRankItem

class HotRankAdapter : RecyclerView.Adapter<HotRankAdapter.ViewHolder>() {

    var dataList = listOf<HotRankItem>()
        get() = field
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class ViewHolder(
        val binding: ItemRankingBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRankingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataList[position]
        with(holder.binding) {
            // 使用 item.rank 或 position + 1（根据你的需求）
            tvRank.text = (position + 1).toString()

            tvTitle.text = item.title
            tvHotValue.text = item.hotValue.toString()

            // 设置背景色
            val context: Context = root.context
            val colorRes = when (position) {
                0 -> R.color.rank_gold
                1 -> R.color.rank_silver
                2 -> R.color.rank_bronze
                else -> R.color.rank_normal
            }
            tvRank.setBackgroundColor(ContextCompat.getColor(context, colorRes))
        }
    }

    override fun getItemCount(): Int = dataList.size
}