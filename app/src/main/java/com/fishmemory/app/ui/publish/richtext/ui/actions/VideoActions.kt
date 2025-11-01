package com.fishmemory.app.ui.publish.richtext.ui.actions

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.fishmemory.app.R
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockDisplay
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockEntity
import com.fishmemory.app.ui.publish.richtext.business.media.VideoPreviewActivity
import com.fishmemory.app.ui.publish.richtext.business.media.VideoUploadManager

/**
 * 视频块的行为逻辑封装
 */
class VideoActions {
    
    /**
     * 为视频块绑定显示和交互逻辑
     * @param root 根视图
     * @param videoContainer 视频容器视图
     * @param ivCover 封面 ImageView
     * @param playerView 播放器视图
     * @param ivPlay 播放按钮 ImageView
     * @param tvDuration 时长 TextView
     * @param progressBar 进度条
     * @param failedOverlay 失败遮罩
     * @param btnRetry 重试按钮
     * @param btnDelete 删除按钮
     * @param viewSelectedBorder 选中边框
     * @param block 视频块数据
     * @param uploadManager 上传管理器
     * @param exoPlayer 当前 ExoPlayer 实例（可空）
     * @param onDeleteRequested 删除请求回调
     * @param onRetryRequested 重试请求回调
     * @param onBlockChanged 块变化回调
     * @param onPlayStateChanged 播放状态变化回调
     * @param onVideoClicked 视频点击回调
     * @return 更新后的 ExoPlayer 实例
     */
    fun bindVideo(
        root: View,
        videoContainer: View,
        ivCover: ImageView,
        playerView: PlayerView,
        ivPlay: ImageView,
        tvDuration: TextView,
        progressBar: ProgressBar,
        failedOverlay: View,
        btnRetry: Button,
        btnDelete: ImageButton,
        viewSelectedBorder: View,
        block: EditorBlock.VideoBlock,
        uploadManager: VideoUploadManager?,
        exoPlayer: ExoPlayer?,
        onDeleteRequested: ((String) -> Unit)?,
        onRetryRequested: ((String) -> Unit)?,
        onBlockChanged: ((String) -> Unit)?,
        onPlayStateChanged: ((String, EditorBlock.PlayState) -> Unit)?,
        onVideoClicked: ((String) -> Unit)?
    ): ExoPlayer? {
        var updatedExoPlayer = exoPlayer
        
        // 显示封面图
        setupCoverImage(ivCover, block.coverUrl ?: block.localUri ?: block.remoteUrl)
        
        // 更新 UI 状态（上传进度、失败、选中等）
        updateUiState(
            progressBar = progressBar,
            failedOverlay = failedOverlay,
            viewSelectedBorder = viewSelectedBorder,
            btnDelete = btnDelete,
            tvDuration = tvDuration,
            block = block
        )
        
        // 设置并更新播放器
        updatedExoPlayer = setupPlayer(
            root = root,
            playerView = playerView,
            block = block,
            currentExoPlayer = updatedExoPlayer
        )
        
        // 根据播放状态控制播放/暂停
        controlPlayback(
            exoPlayer = updatedExoPlayer,
            playState = block.playState,
            playableUrl = block.remoteUrl ?: block.localUri
        )
        
        // 绑定点击事件
        bindClickListeners(
            videoContainer = videoContainer,
            btnRetry = btnRetry,
            btnDelete = btnDelete,
            block = block,
            exoPlayer = updatedExoPlayer,
            playerView = playerView,
            ivPlay = ivPlay,
            ivCover = ivCover,
            onVideoClicked = onVideoClicked,
            onPlayStateChanged = onPlayStateChanged,
            onBlockChanged = onBlockChanged,
            onRetryRequested = onRetryRequested,
            onDeleteRequested = onDeleteRequested,
            root = root
        )
        
        return updatedExoPlayer
    }
    
    /**
     * 只读模式下的视频绑定
     */
    fun bindReadOnly(
        root: View,
        videoContainer: View,
        ivCover: ImageView,
        playerView: PlayerView,
        ivPlay: ImageView,
        tvDuration: TextView,
        progressBar: ProgressBar,
        failedOverlay: View,
        btnRetry: Button,
        btnDelete: ImageButton,
        viewSelectedBorder: View,
        display: EditorBlockDisplay.Video,
        exoPlayer: ExoPlayer?
    ): ExoPlayer? {
        var updatedExoPlayer = exoPlayer
        
        // 隐藏编辑态控件
        progressBar.visibility = View.GONE
        failedOverlay.visibility = View.GONE
        btnRetry.visibility = View.GONE
        btnDelete.visibility = View.GONE
        viewSelectedBorder.visibility = View.GONE
        
        // 设置初始状态
        playerView.visibility = View.GONE
        ivPlay.visibility = View.VISIBLE
        
        // 显示封面图
        setupCoverImage(ivCover, display.coverUrl ?: display.url, isDisplay = true)
        
        // 设置时长
        tvDuration.text = formatDuration(display.durationMs)
        tvDuration.visibility = if (display.durationMs > 0) View.VISIBLE else View.GONE
        
        // 初始化播放器
        val playableUrl = display.url
        if (!playableUrl.isNullOrBlank()) {
            if (updatedExoPlayer == null) {
                updatedExoPlayer = ExoPlayer.Builder(root.context).build().also { player ->
                    player.setMediaItem(MediaItem.fromUri(Uri.parse(playableUrl)))
                    player.prepare()
                    player.playWhenReady = false
                }
                playerView.player = updatedExoPlayer
            }
        } else {
            playerView.player = null
        }
        
        // 绑定点击事件（原地播放/暂停）
        bindReadOnlyClickListeners(
            videoContainer = videoContainer,
            playableUrl = playableUrl,
            exoPlayer = updatedExoPlayer,
            playerView = playerView,
            ivPlay = ivPlay,
            ivCover = ivCover,
            root = root
        )
        
        return updatedExoPlayer
    }
    
    /**
     * 清除资源
     */
    fun clear(
        playerView: PlayerView,
        exoPlayer: ExoPlayer?,
        ivCover: ImageView
    ) {
        playerView.player = null
        exoPlayer?.release()
        Glide.with(ivCover).clear(ivCover)
    }
    
    /**
     * 暂停播放
     */
    fun pausePlayback(
        exoPlayer: ExoPlayer?,
        playerView: View,
        ivPlay: View,
        ivCover: View
    ) {
        exoPlayer?.pause()
        exoPlayer?.playWhenReady = false
        
        if (playerView.visibility == View.VISIBLE) {
            playerView.visibility = View.GONE
            ivPlay.visibility = View.VISIBLE
            ivCover.visibility = View.VISIBLE
        }
    }
    
    /**
     * 设置封面图片
     */
    private fun setupCoverImage(ivCover: ImageView, coverUrl: String?, isDisplay: Boolean = false) {
        if (!coverUrl.isNullOrEmpty()) {
            Glide.with(ivCover)
                .load(
                    if (coverUrl.startsWith("content://") || coverUrl.startsWith("file://")) {
                        Uri.parse(coverUrl)
                    } else {
                        coverUrl
                    }
                )
                .placeholder(R.drawable.bg_video_placeholder)
                .error(R.drawable.bg_video_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade())
                .skipMemoryCache(false)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .into(ivCover)
        } else {
            Glide.with(ivCover).clear(ivCover)
            ivCover.setImageResource(0)
            ivCover.setBackgroundResource(R.drawable.bg_video_placeholder)
        }
    }
    
    /**
     * 更新 UI 状态
     */
    private fun updateUiState(
        progressBar: ProgressBar,
        failedOverlay: View,
        viewSelectedBorder: View,
        btnDelete: ImageButton,
        tvDuration: TextView,
        block: EditorBlock.VideoBlock
    ) {
        progressBar.visibility = 
            if (block.uploadState == EditorBlockEntity.UploadState.UPLOADING) View.VISIBLE else View.GONE
        failedOverlay.visibility = 
            if (block.uploadState == EditorBlockEntity.UploadState.FAILED) View.VISIBLE else View.GONE
        viewSelectedBorder.visibility = if (block.isSelected) View.VISIBLE else View.GONE
        btnDelete.visibility = if (block.isSelected) View.VISIBLE else View.GONE
        
        tvDuration.text = formatDuration(block.durationMs)
        tvDuration.visibility = if (block.durationMs > 0) View.VISIBLE else View.GONE
    }
    
    /**
     * 设置播放器
     */
    private fun setupPlayer(
        root: View,
        playerView: PlayerView,
        block: EditorBlock.VideoBlock,
        currentExoPlayer: ExoPlayer?
    ): ExoPlayer? {
        var exoPlayer = currentExoPlayer
        val playableUrl = block.remoteUrl ?: block.localUri
        
        if (playableUrl != null) {
            val currentUri = exoPlayer?.currentMediaItem?.localConfiguration?.uri?.toString()
            if (exoPlayer == null || currentUri != playableUrl) {
                exoPlayer?.release()
                exoPlayer = ExoPlayer.Builder(root.context).build().also { player ->
                    player.setMediaItem(MediaItem.fromUri(Uri.parse(playableUrl)))
                    player.prepare()
                    playerView.player = player
                }
            } else {
                playerView.player = exoPlayer
            }
        } else {
            playerView.player = null
        }
        
        return exoPlayer
    }
    
    /**
     * 控制播放/暂停
     */
    private fun controlPlayback(
        exoPlayer: ExoPlayer?,
        playState: EditorBlock.PlayState,
        playableUrl: String?
    ) {
        val isPlaying = playState == EditorBlock.PlayState.PLAYING
        
        if (isPlaying && playableUrl != null) {
            exoPlayer?.playWhenReady = true
            exoPlayer?.play()
        } else {
            exoPlayer?.playWhenReady = false
            exoPlayer?.pause()
        }
    }
    
    /**
     * 绑定编辑态点击事件
     */
    private fun bindClickListeners(
        videoContainer: View,
        btnRetry: Button,
        btnDelete: ImageButton,
        block: EditorBlock.VideoBlock,
        exoPlayer: ExoPlayer?,
        playerView: PlayerView,
        ivPlay: ImageView,
        ivCover: ImageView,
        onVideoClicked: ((String) -> Unit)?,
        onPlayStateChanged: ((String, EditorBlock.PlayState) -> Unit)?,
        onBlockChanged: ((String) -> Unit)?,
        onRetryRequested: ((String) -> Unit)?,
        onDeleteRequested: ((String) -> Unit)?,
        root: View
    ) {
        val playableUrl = block.remoteUrl ?: block.localUri
        
        videoContainer.setOnClickListener {
            onVideoClicked?.invoke(block.id)
            if (block.uploadState == EditorBlockEntity.UploadState.FAILED) return@setOnClickListener
            if (playableUrl == null) return@setOnClickListener
            
            when (block.playState) {
                EditorBlock.PlayState.PLAYING -> {
                    exoPlayer?.playWhenReady = false
                    exoPlayer?.pause()
                    block.playState = EditorBlock.PlayState.PAUSED
                    onPlayStateChanged?.invoke(block.id, EditorBlock.PlayState.PAUSED)
                    ivPlay.visibility = View.VISIBLE
                    playerView.visibility = View.GONE
                    ivCover.visibility = View.VISIBLE
                }
                else -> {
                    var newExoPlayer = exoPlayer
                    if (newExoPlayer == null) {
                        newExoPlayer = ExoPlayer.Builder(root.context).build().also { player ->
                            player.setMediaItem(MediaItem.fromUri(Uri.parse(playableUrl)))
                            player.prepare()
                            playerView.player = player
                        }
                    } else {
                        playerView.player = newExoPlayer
                    }
                    newExoPlayer?.playWhenReady = true
                    newExoPlayer?.play()
                    block.playState = EditorBlock.PlayState.PLAYING
                    onPlayStateChanged?.invoke(block.id, EditorBlock.PlayState.PLAYING)
                    ivPlay.visibility = View.GONE
                    playerView.visibility = View.VISIBLE
                    ivCover.visibility = View.INVISIBLE
                }
            }
            onBlockChanged?.invoke(block.id)
        }
        
        // 长按进入全屏预览
        videoContainer.setOnLongClickListener {
            if (playableUrl == null) return@setOnLongClickListener false
            val context = root.context
            val intent = Intent(context, VideoPreviewActivity::class.java).apply {
                putExtra(VideoPreviewActivity.EXTRA_URL, playableUrl)
            }
            context.startActivity(intent)
            true
        }
        
        btnRetry.setOnClickListener { onRetryRequested?.invoke(block.id) }
        btnDelete.setOnClickListener { onDeleteRequested?.invoke(block.id) }
    }
    
    /**
     * 绑定只读态点击事件
     */
    private fun bindReadOnlyClickListeners(
        videoContainer: View,
        playableUrl: String?,
        exoPlayer: ExoPlayer?,
        playerView: PlayerView,
        ivPlay: View,
        ivCover: View,
        root: View
    ) {
        // 点击：原地播放/暂停
        videoContainer.setOnClickListener {
            if (playableUrl.isNullOrBlank()) return@setOnClickListener
            val isPlaying = exoPlayer?.isPlaying == true
            
            if (isPlaying) {
                // 暂停
                exoPlayer?.pause()
                exoPlayer?.playWhenReady = false
                playerView.visibility = View.GONE
                ivPlay.visibility = View.VISIBLE
                ivCover.visibility = View.VISIBLE
            } else {
                // 播放
                playerView.visibility = View.VISIBLE
                ivPlay.visibility = View.GONE
                ivCover.visibility = View.INVISIBLE
                exoPlayer?.playWhenReady = true
                exoPlayer?.play()
            }
        }
        
        // 长按：全屏预览
        videoContainer.setOnLongClickListener {
            if (playableUrl.isNullOrBlank()) return@setOnLongClickListener false
            
            // 先暂停
            exoPlayer?.pause()
            exoPlayer?.playWhenReady = false
            playerView.visibility = View.GONE
            ivPlay.visibility = View.VISIBLE
            ivCover.visibility = View.VISIBLE
            
            // 打开全屏预览
            val context = root.context
            context.startActivity(Intent(context, VideoPreviewActivity::class.java).apply {
                putExtra(VideoPreviewActivity.EXTRA_URL, playableUrl)
            })
            true
        }
    }
    
    /**
     * 格式化时长
     */
    private fun formatDuration(ms: Long): String {
        if (ms <= 0) return ""
        val sec = (ms / 1000) % 60
        val min = (ms / 60000) % 60
        val hour = ms / 3600000
        return if (hour > 0) {
            "%d:%02d:%02d".format(hour, min, sec)
        } else {
            "%d:%02d".format(min, sec)
        }
    }
}
