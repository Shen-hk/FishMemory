package com.fishmemory.app.ui.publish.richtext.business.media

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.fishmemory.richeditor.R

/**
 * 全屏视频预览页：用于放大当前视频块并提供完整播放控制。
 */
class VideoPreviewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL: String = "extra_url"
    }

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_preview)

        // 全屏沉浸：隐藏状态栏和导航栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        playerView = findViewById(R.id.fullscreenPlayerView)

        val url = intent.getStringExtra(EXTRA_URL)
        if (url.isNullOrEmpty()) {
            finish()
            return
        }

        val exoPlayer = ExoPlayer.Builder(this).build().also { p ->
            p.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            p.prepare()
            p.playWhenReady = true
        }
        player = exoPlayer
        playerView.player = exoPlayer
    }

    override fun onStop() {
        super.onStop()
        player?.playWhenReady = false
        player?.pause()
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
