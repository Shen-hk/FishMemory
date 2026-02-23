package com.fishmemory.app.ui.community

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.app.R
import com.fishmemory.app.databinding.ItemRankingBinding
import com.fishmemory.app.data.model.HotRankItem

class HotRankAdapter : RecyclerView.Adapter<HotRankAdapter.ViewHolder>() {
    var dataList = listOf<HotRankItem>()
        get() = field // getter 直接返回当前字段值（通常可省略，但显式写出更清晰）
        set(value) {
            field = value               // 更新内部数据
            notifyDataSetChanged()    // 通知 RecyclerView 全量刷新
        }


    inner class ViewHolder(
        val binding: ItemRankingBinding
    ) : RecyclerView.ViewHolder(binding.root) // itemView = binding.root


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRankingBinding.inflate(
            LayoutInflater.from(parent.context), // 安全获取 Context
            parent,
            false // 关键：必须为 false！否则会抛异常（parent 已有 LayoutParams）
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataList[position] // 获取当前位置的数据项

        // 使用 with 作用域简化对 binding 的多次访问
        with(holder.binding) {
            // 🥇 排名显示：使用 position + 1（第1名=0+1）
            // 注意：如果 HotRankItem 本身包含 rank 字段，也可用 item.rank
            tvRank.text = (position + 1).toString()

            // 📰 标题
            tvTitle.text = item.title

            // 🔥 热度值（假设是 Long 或 Int）
            tvHotValue.text = item.hotValue.toString()

            // 🎨 为排名 1/2/3 设置特殊背景色（金/银/铜），其余为普通色
            val context: Context = root.context // 从根 View 获取 Context（安全）

            // 根据 position 选择颜色资源 ID
            val colorRes = when (position) {
                0 -> R.color.rank_gold    // 第1名：金色
                1 -> R.color.rank_silver  // 第2名：银色
                2 -> R.color.rank_bronze  // 第3名：铜色
                else -> R.color.rank_normal // 其他：灰色或白色
            }

            // 将颜色资源 ID 转为实际颜色值（兼容旧 API）
            tvRank.setBackgroundColor(ContextCompat.getColor(context, colorRes))
        }
    }


    override fun getItemCount(): Int = dataList.size.coerceAtMost(10)
}