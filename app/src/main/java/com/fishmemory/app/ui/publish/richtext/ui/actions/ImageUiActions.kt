package com.fishmemory.app.ui.publish.richtext.ui.actions

import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.fishmemory.app.R
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockEntity

class ImageBlockActions(
    private val activity: FragmentActivity
) {

    /**
     * 加载图片到 ImageView
     */
    fun loadImage(imageView: ImageView, url: String?) {
        if (!url.isNullOrEmpty()) {
            val toLoad = if (url.startsWith("content://") || url.startsWith("file://")) {
                Uri.parse(url)
            } else {
                url
            }
            Glide.with(imageView)
                .load(toLoad)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
        } else {
            Glide.with(imageView).clear(imageView)
            imageView.setImageResource(R.drawable.ic_image_placeholder)
        }
    }

    /**
     * 根据上传状态更新 UI
     */
    fun updateUploadStateUI(
        block: EditorBlock.ImageBlock,
        progressBar: View,
        failedOverlay: View
    ) {
        progressBar.visibility = if (block.uploadState == EditorBlockEntity.UploadState.UPLOADING) {
            View.VISIBLE
        } else {
            View.GONE
        }
        failedOverlay.visibility = if (block.uploadState == EditorBlockEntity.UploadState.FAILED) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    /**
     * 处理图片预览
     */
    fun onPreviewImage(blockId: String, imageUrl: String?, onPreview: (String) -> Unit) {
        if (!imageUrl.isNullOrBlank()) {
            onPreview.invoke(blockId)
        }
    }

    /**
     * 处理图片删除
     */
    fun onDeleteImage(blockId: String, onDelete: (String) -> Unit) {
        // 可以添加确认对话框
        onDelete.invoke(blockId)
    }

    /**
     * 处理图片替换
     */
    fun onReplaceImage(blockId: String, onReplace: (String) -> Unit) {
        // 打开图片选择器
        // 可以复用现有的图片选择逻辑
        onReplace.invoke(blockId)
    }

    /**
     * 显示图片菜单
     */
    fun showImageMenu(
        anchorView: View,
        blockId: String,
        onMenuAction: (action: ImageMenuAction, blockId: String) -> Unit
    ) {
        // 可以显示 PopupMenu
        // 包含：预览、替换、删除、添加注释等选项
        // 暂时简化，直接触发菜单请求
        onMenuAction(ImageMenuAction.SHOW_MENU, blockId)
    }

    /**
     * 清理 Glide 资源
     */
    fun clearImage(imageView: ImageView) {
        Glide.with(imageView).clear(imageView)
    }

    /**
     * 图片菜单操作类型
     */
    enum class ImageMenuAction {
        SHOW_MENU,
        PREVIEW,
        REPLACE,
        DELETE,
        EDIT_CAPTION
    }
}