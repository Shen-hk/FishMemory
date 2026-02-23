package com.fishmemory.app.ui.articledetail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fishmemory.app.data.local.rooms.AppDatabase
import com.fishmemory.app.databinding.ActivityLocalArticleDetailBinding
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockDisplay
import com.fishmemory.app.ui.publish.richtext.ui.adapter.EditorAdapter
import com.fishmemory.app.ui.publish.richtext.core.converter.StandardBlockToDisplay
import com.fishmemory.app.ui.publish.richtext.core.converter.StandardJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 本地原创文章详情页：根据 localId 从 Room 拉取并展示。
 * 解析 blocksJson 并只读渲染。
 */
class LocalArticleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocalArticleDetailBinding
    private val blockAdapter = EditorAdapter(readOnlyBlocks = emptyList())
    
    // 缓存当前展示的图片块 URL，用于预览
    private val imageBlockUrls = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val localId = intent.getStringExtra(EXTRA_LOCAL_ID)
        if (localId.isNullOrBlank()) {
            Toast.makeText(this, "无效文章", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        setupScrollAnimation()
        setupBottomBarClicks()

        binding.recyclerBlocks.layoutManager = LinearLayoutManager(this).apply { isAutoMeasureEnabled = true }
        binding.recyclerBlocks.adapter = blockAdapter
        binding.recyclerBlocks.setHasFixedSize(false)
        binding.recyclerBlocks.isFocusable = false
        
        // 设置图片预览回调
        blockAdapter.onImageBlockPreviewRequested = { blockId ->
            val imageUrl = imageBlockUrls[blockId]
            if (!imageUrl.isNullOrBlank()) {
                showImagePreview(imageUrl)
            }
        }

        binding.scrollView.scrollTo(0, 0)
        lifecycleScope.launch {
            val entity = withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(applicationContext)
                db.localArticleDao().incrementReadCount(localId)
                db.localArticleDao().getById(localId)
            }
            if (entity == null) {
                Toast.makeText(this@LocalArticleDetailActivity, "文章不存在", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            binding.tvTitle.text = entity.title
            binding.tvMeta.text = "${entity.authorName} · ${formatTime(entity.publishTimeMs)}"
            binding.tvAuthorName.text = entity.authorName
            val doc = StandardJsonParser.parse(entity.blocksJson)
            val displayList = if (doc != null && doc.blocks.isNotEmpty()) {
                StandardBlockToDisplay.toDisplayList(doc.blocks)
            } else {
                emptyList()
            }
            blockAdapter.setReadOnlyBlocks(displayList)
            
            // 缓存所有图片块的 URL
            imageBlockUrls.clear()
            displayList.filterIsInstance<EditorBlockDisplay.Image>()
                .forEach { 
                    if (!it.url.isNullOrBlank()) {
                        imageBlockUrls[it.id] = it.url
                    }
                }

            // 先抢焦点，避免子 View 获得焦点后系统自动滚到底部
            binding.scrollView.requestFocus()

            scrollToTopOnceReady()
        }
    }

    /** 在 layout/draw 就绪后滚到顶部；并用短延迟再滚一次，覆盖后续被系统或焦点带到底部的情况 */
    private fun scrollToTopOnceReady() {
        binding.recyclerBlocks.scrollToPosition(0)
        binding.scrollView.scrollTo(0, 0)

        val scrollView = binding.scrollView
        val recycler = binding.recyclerBlocks
        val runScroll = Runnable {
            recycler.scrollToPosition(0)
            scrollView.scrollTo(0, 0)
            scrollView.requestFocus()
        }

        // PreDraw 在 layout 之后、draw 之前执行，此时内容高度通常已确定
        scrollView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                scrollView.viewTreeObserver.removeOnPreDrawListener(this)
                runScroll.run()
                return true
            }
        })

        // 延迟再滚一次，防止 RecyclerView 二次 layout 或焦点导致又滚到底部
        scrollView.postDelayed(runScroll, 120)
        scrollView.postDelayed(runScroll, 300)
    }

    private fun formatTime(publishTimeMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(publishTimeMs))
    }

    /** 作者信息滚动动画：随 ScrollView 滚动渐显并上移 */
    private fun setupScrollAnimation() {
        val triggerHeight = 120f
        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val progress = (scrollY / triggerHeight).coerceIn(0f, 1f)
            binding.authorContainer.alpha = progress
            binding.authorContainer.translationY = (1 - progress) * dp(8f)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    /** 全屏图片预览 */
    private fun showImagePreview(imageUrl: String) {
        // 创建全屏对话框
        val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = android.widget.ImageView(this)

        // 使用 Glide 加载图片
        com.bumptech.glide.Glide.with(this)
            .load(
                if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://")) {
                    android.net.Uri.parse(imageUrl)
                } else {
                    imageUrl
                }
            )
            .into(imageView)

        imageView.setOnClickListener { dialog.dismiss() }
        imageView.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        imageView.layoutParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        )

        dialog.setContentView(imageView)
        dialog.window?.apply {
            setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        dialog.show()
    }

    override fun onStop() {
        super.onStop()
        (binding.recyclerBlocks.adapter as? EditorAdapter)?.let { adapter ->
            val recyclerView = binding.recyclerBlocks
            for (i in 0 until recyclerView.childCount) {
                val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i))
                if (viewHolder is com.fishmemory.app.ui.publish.richtext.ui.adapter.VideoBlockViewHolder) {
                    viewHolder.pausePlayback()
                }
            }
        }
    }

    private fun setupBottomBarClicks() {
        binding.etComment.setOnClickListener {
            Toast.makeText(this, "评论功能开发中...", Toast.LENGTH_SHORT).show()
        }
        binding.llCommentCount.setOnClickListener {
            Toast.makeText(this, "查看评论开发中...", Toast.LENGTH_SHORT).show()
        }
        binding.llLike.setOnClickListener {
            Toast.makeText(this, "点赞功能开发中...", Toast.LENGTH_SHORT).show()
        }
        binding.llCollect.setOnClickListener {
            Toast.makeText(this, "收藏功能开发中...", Toast.LENGTH_SHORT).show()
        }
        binding.llShare.setOnClickListener {
            val shareText = buildString {
                append(binding.tvTitle.text)
                append("\n\n")
                append("查看本地原创文章")
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, binding.tvTitle.text.toString())
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(shareIntent, "分享到"))
        }
    }

    companion object {
        private const val EXTRA_LOCAL_ID = "local_id"

        fun createIntent(context: Context, localId: String): Intent {
            return Intent(context, LocalArticleDetailActivity::class.java).apply {
                putExtra(EXTRA_LOCAL_ID, localId)
            }
        }
    }
}
