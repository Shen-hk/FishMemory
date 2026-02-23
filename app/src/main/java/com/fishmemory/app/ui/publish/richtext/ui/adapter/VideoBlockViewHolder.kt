package com.fishmemory.app.ui.publish.richtext.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.app.R
import com.fishmemory.app.ui.publish.richtext.business.media.VideoPlayerManager
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockDisplay
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.business.media.VideoUploadManager
import com.fishmemory.app.ui.publish.richtext.ui.actions.VideoActions

class VideoBlockViewHolder(
    private val root: View,
    private val videoContainer: View,
    private val ivCover: ImageView,
    private val playerView: PlayerView,
    private val ivPlay: ImageView,
    private val tvDuration: TextView,
    private val progressBar: ProgressBar,
    private val failedOverlay: View,
    private val btnRetry: Button,
    private val btnDelete: ImageButton,
    private val viewSelectedBorder: View,
    private val actions: VideoActions = VideoActions()
) : RecyclerView.ViewHolder(root) {

    private var block: EditorBlock.VideoBlock? = null
    private var uploadManager: VideoUploadManager? = null
    private var onDeleteRequested: ((String) -> Unit)? = null
    private var onRetryRequested: ((String) -> Unit)? = null
    private var onBlockChanged: ((String) -> Unit)? = null
    private var onPlayStateChanged: ((String, EditorBlock.PlayState) -> Unit)? = null
    private var onVideoClicked: ((String) -> Unit)? = null
    private var exoPlayer: ExoPlayer? = null

    fun bind(
        block: EditorBlock.VideoBlock,
        playerManager: VideoPlayerManager?, // 已不再使用，仅为兼容签名
        uploadManager: VideoUploadManager?,
        onDeleteRequested: ((String) -> Unit)?,
        onRetryRequested: ((String) -> Unit)?,
        onBlockChanged: ((String) -> Unit)?,
        onPlayStateChanged: ((String, EditorBlock.PlayState) -> Unit)?,
        onVideoClicked: ((String) -> Unit)?
    ) {
        // 使用 Actions 类处理视频块逻辑
        exoPlayer = actions.bindVideo(
            root = root,
            videoContainer = videoContainer,
            ivCover = ivCover,
            playerView = playerView,
            ivPlay = ivPlay,
            tvDuration = tvDuration,
            progressBar = progressBar,
            failedOverlay = failedOverlay,
            btnRetry = btnRetry,
            btnDelete = btnDelete,
            viewSelectedBorder = viewSelectedBorder,
            block = block,
            uploadManager = uploadManager,
            exoPlayer = exoPlayer,
            onDeleteRequested = onDeleteRequested,
            onRetryRequested = onRetryRequested,
            onBlockChanged = onBlockChanged,
            onPlayStateChanged = onPlayStateChanged,
            onVideoClicked = onVideoClicked
        )
    }


    fun clear() {
        actions.clear(playerView, exoPlayer, ivCover)
        exoPlayer = null
    }

    /** 暂停播放，供 Activity onStop 调用 */
    fun pausePlayback() {
        actions.pausePlayback(exoPlayer, playerView, ivPlay, ivCover)
    }

    /** 只读态：支持原地播放或点击全屏预览。 */
    fun bindReadOnly(display: EditorBlockDisplay.Video) {
        // 使用 Actions 类处理只读模式逻辑
        exoPlayer = actions.bindReadOnly(
            root = root,
            videoContainer = videoContainer,
            ivCover = ivCover,
            playerView = playerView,
            ivPlay = ivPlay,
            tvDuration = tvDuration,
            progressBar = progressBar,
            failedOverlay = failedOverlay,
            btnRetry = btnRetry,
            btnDelete = btnDelete,
            viewSelectedBorder = viewSelectedBorder,
            display = display,
            exoPlayer = exoPlayer
        )
    }

    companion object {
        fun create(parent: ViewGroup): VideoBlockViewHolder {
            val root = LayoutInflater.from(parent.context).inflate(R.layout.item_block_video, parent, false)
            return VideoBlockViewHolder(
                root,
                root.findViewById(R.id.videoContainer),
                root.findViewById(R.id.ivCover),
                root.findViewById(R.id.playerView),
                root.findViewById(R.id.ivPlay),
                root.findViewById(R.id.tvDuration),
                root.findViewById(R.id.progressBar),
                root.findViewById(R.id.failedOverlay),
                root.findViewById(R.id.btnRetry),
                root.findViewById(R.id.btnDelete),
                root.findViewById(R.id.viewSelectedBorder)
            )
        }
    }
}
