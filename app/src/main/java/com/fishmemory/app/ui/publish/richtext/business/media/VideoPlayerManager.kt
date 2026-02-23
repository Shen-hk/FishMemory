package com.fishmemory.app.ui.publish.richtext.business.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.util.LinkedHashMap

/**
 * 视频播放器管理器：管理少量 ExoPlayer 实例（LRU 池，最多 3 个），按 blockId 绑定。
 * 避免在滚动时创建过多播放器，控制内存占用。
 */
class VideoPlayerManager(private val context: Context) {

    companion object {
        private const val MAX_PLAYERS = 3
    }

    private val players = object : LinkedHashMap<String, ExoPlayer>(MAX_PLAYERS + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ExoPlayer>?): Boolean {
            if (size > MAX_PLAYERS && eldest != null) {
                eldest.value.release()
                return true
            }
            return false
        }
    }

    private var currentPlayingBlockId: String? = null

    /**
     * 绑定播放器到指定 block 的 PlayerView，准备播放（不自动播放）。
     */
    fun attachPlayer(blockId: String, playerView: PlayerView, urlOrUri: String) {
        release(blockId)
        val player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(urlOrUri)))
            prepare()
            playWhenReady = false
        }
        playerView.player = player
        synchronized(players) {
            players[blockId] = player
        }
    }

    fun play(blockId: String) {
        val player = players[blockId] ?: return
        currentPlayingBlockId?.let { if (it != blockId) pause(it) }
        currentPlayingBlockId = blockId
        player.playWhenReady = true
        player.play()
    }

    fun pause(blockId: String) {
        players[blockId]?.playWhenReady = false
        players[blockId]?.pause()
        if (currentPlayingBlockId == blockId) currentPlayingBlockId = null
    }

    fun stop(blockId: String) {
        pause(blockId)
        players[blockId]?.stop()
    }

    fun release(blockId: String) {
        players.remove(blockId)?.release()
        if (currentPlayingBlockId == blockId) currentPlayingBlockId = null
    }

    /** 暂停当前正在播放的视频，供 Activity onStop 时调用。 */
    fun pauseAll() {
        currentPlayingBlockId?.let { pause(it) }
    }

    fun releaseAll() {
        synchronized(players) {
            players.values.forEach { it.release() }
            players.clear()
        }
        currentPlayingBlockId = null
    }

    fun getPlayer(blockId: String): ExoPlayer? = players[blockId]

    fun addListener(blockId: String, listener: Player.Listener) {
        players[blockId]?.addListener(listener)
    }

    fun removeListener(blockId: String, listener: Player.Listener) {
        players[blockId]?.removeListener(listener)
    }
}