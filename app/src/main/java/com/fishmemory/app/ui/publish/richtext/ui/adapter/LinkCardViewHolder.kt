package com.fishmemory.app.ui.publish.richtext.ui.adapter

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fishmemory.app.databinding.ItemBlockLinkCardBinding
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockDisplay
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.ui.actions.LinkUiActions

class LinkCardViewHolder(
    private val binding: ItemBlockLinkCardBinding
) : RecyclerView.ViewHolder(binding.root) {

    private var block: EditorBlock.LinkCard? = null
    private var onClicked: ((Int) -> Unit)? = null
    private var onDelete: ((String) -> Unit)? = null
    private var linkUiActions: LinkUiActions? = null

    fun bind(
        block: EditorBlock.LinkCard,
        isSelected: Boolean,
        onClicked: (Int) -> Unit,
        onDelete: (String) -> Unit,
        linkUiActions: LinkUiActions? = null  // 新增：可选注入 LinkUiActions
    ) {
        this.block = block
        this.onClicked = onClicked
        this.onDelete = onDelete
        this.linkUiActions = linkUiActions

        // 更新选中态 UI：删除按钮和边框
        binding.ivDelete.visibility = if (isSelected) android.view.View.VISIBLE else android.view.View.GONE
        binding.viewSelectedBorder.visibility = if (isSelected) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvLoading.visibility = if (block.isLoading) android.view.View.VISIBLE else android.view.View.GONE

        binding.tvTitle.text = block.title.ifBlank { "链接" }
        binding.tvDesc.text = block.description
        binding.tvUrl.text = block.url

        val cover = block.imageUrl
        if (!cover.isNullOrBlank()) {
            binding.ivCover.visibility = android.view.View.VISIBLE
            Glide.with(binding.ivCover)
                .load(cover)
                .into(binding.ivCover)
        } else {
            binding.ivCover.visibility = android.view.View.GONE
            Glide.with(binding.ivCover).clear(binding.ivCover)
        }

        binding.cardRoot.isFocusable = true
        binding.cardRoot.isFocusableInTouchMode = true

        binding.cardRoot.setOnClickListener {
            binding.cardRoot.requestFocus()
            onClicked(bindingAdapterPosition)
        }

        // Backspace：卡片为原子块。未选中时按退格仅选中；已选中时消费但不删除。
        binding.cardRoot.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onClicked(pos)
                }
                true
            } else {
                false
            }
        }

        binding.ivDelete.setOnClickListener {
            val id = this.block?.id ?: return@setOnClickListener
            onDelete(id)
        }

        // 长按弹出链接菜单 - 委托给 LinkUiActions 处理
        binding.cardRoot.setOnLongClickListener {
            val currentBlock = this.block ?: return@setOnLongClickListener false
            val actions = this.linkUiActions
                ?: return@setOnLongClickListener false  // 没有 LinkUiActions 时不处理长按

            actions.showLinkCardMenu(
                context = binding.root.context,
                anchor = binding.cardRoot,
                block = currentBlock
            )
            true
        }
    }

    /** 只读态：展示标题/描述/图，有 LinkUiActions 时支持点击跳转和长按复制。 */
    fun bindReadOnly(
        display: EditorBlockDisplay.LinkCard,
        linkUiActions: LinkUiActions? = null
    ) {
        block = null
        this.linkUiActions = linkUiActions

        binding.ivDelete.visibility = android.view.View.GONE
        binding.viewSelectedBorder.visibility = android.view.View.GONE
        binding.tvLoading.visibility = android.view.View.GONE
        binding.tvTitle.text = display.title.ifBlank { "链接" }
        binding.tvDesc.text = display.description
        binding.tvUrl.text = display.url

        val cover = display.imageUrl
        if (!cover.isNullOrBlank()) {
            binding.ivCover.visibility = android.view.View.VISIBLE
            Glide.with(binding.ivCover).load(cover).into(binding.ivCover)
        } else {
            binding.ivCover.visibility = android.view.View.GONE
            Glide.with(binding.ivCover).clear(binding.ivCover)
        }

        // 根据 LinkUiActions 是否存在设置交互
        if (linkUiActions != null) {
            binding.cardRoot.setOnClickListener {
                linkUiActions.openLinkCardUrl(binding.root.context, display.url)
            }
            binding.cardRoot.setOnLongClickListener {
                linkUiActions.copyLinkToClipboard(
                    context = binding.root.context,
                    url = display.url,
                    label = "链接"
                )
                true
            }
        } else {
            binding.cardRoot.setOnClickListener(null)
            binding.cardRoot.setOnLongClickListener(null)
        }
        
        binding.ivDelete.setOnClickListener(null)
    }

    fun clear() {
        block = null
        onClicked = null
        onDelete = null
        linkUiActions = null
        Glide.with(binding.ivCover).clear(binding.ivCover)
    }

    companion object {
        fun create(parent: ViewGroup): LinkCardViewHolder {
            val binding = ItemBlockLinkCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return LinkCardViewHolder(binding)
        }
    }
}