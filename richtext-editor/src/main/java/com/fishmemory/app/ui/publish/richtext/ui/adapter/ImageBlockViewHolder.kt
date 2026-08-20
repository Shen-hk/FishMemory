package com.fishmemory.app.ui.publish.richtext.ui.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.richeditor.R
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockDisplay
import com.fishmemory.app.ui.publish.richtext.ui.actions.ImageBlockActions

class ImageBlockViewHolder(
    private val root: View,
    private val imageContainer: FrameLayout,
    private val imageView: ImageView,
    private val progressBar: View,
    private val failedOverlay: View,
    private val btnDelete: View,
    private val btnRetry: View,
    private val etCaption: EditText,
    private val selectedBorderView: View,
    private val actions: ImageBlockActions
) : RecyclerView.ViewHolder(root) {

    private var block: EditorBlock.ImageBlock? = null
    private var captionWatcher: TextWatcher? = null

    fun bind(
        block: EditorBlock.ImageBlock,
        onDeleteRequested: ((String) -> Unit)?,
        onReplaceRequested: ((String) -> Unit)?,
        onPreviewRequested: ((String) -> Unit)?,
        onBlockChanged: ((String) -> Unit)?,
        onMenuRequested: ((anchorView: View, blockId: String) -> Unit)?
    ) {
        this.block = block

        // 加载图片
        actions.loadImage(imageView, block.remoteUrl ?: block.localUri)

        // 更新上传状态 UI
        actions.updateUploadStateUI(block, progressBar, failedOverlay)

        // 设置注释
        setupCaption(block.caption) { newText ->
            block.caption = newText
            onBlockChanged?.invoke(block.id)
        }

        // 更新选中状态
        updateSelectionUI(block.isSelected)

        // 设置点击事件
        btnDelete.setOnClickListener {
            actions.onDeleteImage(block.id) { onDeleteRequested?.invoke(it) }
        }
        btnRetry.setOnClickListener {
            actions.onReplaceImage(block.id) { onReplaceRequested?.invoke(it) }
        }

        // 菜单和预览
        val menuClick = View.OnClickListener {
            onMenuRequested?.invoke(imageContainer, block.id)
        }
        imageContainer.setOnClickListener(menuClick)
        imageView.setOnClickListener(menuClick)

        imageView.setOnLongClickListener {
            actions.onPreviewImage(block.id, block.remoteUrl ?: block.localUri) {
                onPreviewRequested?.invoke(it)
            }
            true
        }
    }
//只读模式
    fun bindReadOnly(display: EditorBlockDisplay.Image, onPreviewRequested: ((String) -> Unit)?) {
        this.block = null

        // 隐藏编辑 UI
        hideEditUI()

        // 加载图片
        actions.loadImage(imageView, display.url)

        // 设置只读注释
        setupReadOnlyCaption(display.caption)

        // 只读态点击预览
        val previewClick = View.OnClickListener {
            actions.onPreviewImage(display.id, display.url) {
                onPreviewRequested?.invoke(it)
            }
        }

        imageContainer.setOnClickListener(previewClick)
        imageView.setOnClickListener(previewClick)
        imageView.setOnLongClickListener {
            previewClick.onClick(imageView)
            true
        }
    }

    private fun updateSelectionUI(isSelected: Boolean) {
        selectedBorderView.visibility = if (isSelected) View.VISIBLE else View.GONE
        btnDelete.visibility = if (isSelected) View.VISIBLE else View.GONE
    }

    private fun hideEditUI() {
        progressBar.visibility = View.GONE
        failedOverlay.visibility = View.GONE
        btnDelete.visibility = View.GONE
        btnRetry.visibility = View.GONE
        selectedBorderView.visibility = View.GONE
    }

    private fun setupCaption(initialText: String?, onTextChanged: (String) -> Unit) {
        etCaption.removeTextChangedListener(captionWatcher)
        etCaption.setText(initialText)
        captionWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                onTextChanged(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        etCaption.addTextChangedListener(captionWatcher)
    }

    private fun setupReadOnlyCaption(caption: String?) {
        etCaption.removeTextChangedListener(captionWatcher)
        etCaption.setText(caption)
        etCaption.isFocusable = false
        etCaption.isFocusableInTouchMode = false
        etCaption.visibility = if (!caption.isNullOrBlank()) View.VISIBLE else View.GONE
    }

    fun clearGlide() {
        actions.clearImage(imageView)
    }

    companion object {
        fun create(parent: ViewGroup, actions: ImageBlockActions): ImageBlockViewHolder {
            val root = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_block_image, parent, false)
            return ImageBlockViewHolder(
                root,
                root.findViewById(R.id.imageContainer),
                root.findViewById(R.id.ivImage),
                root.findViewById(R.id.progressBar),
                root.findViewById(R.id.failedOverlay),
                root.findViewById(R.id.btnDelete),
                root.findViewById(R.id.btnRetry),
                root.findViewById(R.id.etCaption),
                root.findViewById(R.id.viewSelectedBorder),
                actions
            )
        }
    }
}
