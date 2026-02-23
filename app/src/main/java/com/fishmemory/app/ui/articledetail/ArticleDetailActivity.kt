package com.fishmemory.app.ui.articledetail

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.ViewModelProvider
import com.fishmemory.app.App
import com.fishmemory.app.databinding.ActivityArticleDetailBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 文章详情页：展示标题、作者、WebView 正文，以及点赞/收藏/分享。
 * 职责：WebView 配置与加载、滚动动画、结果回传；收藏/点赞状态由 ArticleDetailViewModel + Repository 驱动。
 */
class ArticleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArticleDetailBinding
    private lateinit var viewModel: ArticleDetailViewModel

    private var articleId = -1
    private var title = ""
    private var originalUrl = ""
    private var content = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = (application as App).articleRepository
        viewModel = ViewModelProvider(
            this,
            ArticleDetailViewModelFactory(repository)
        )[ArticleDetailViewModel::class.java]

        readIntent()
        viewModel.init(articleId, title, originalUrl, intent.getBooleanExtra("article_is_favorite", false))

        binding.tvDetailTitle.text = title
        binding.tvDetailAuthor.text = "作者：${intent.getStringExtra("article_author") ?: "无作者"} · ${intent.getStringExtra("article_created_at") ?: "无时间"}"
        binding.tvAuthorName.text = intent.getStringExtra("article_author") ?: "无作者"
        content = intent.getStringExtra("article_content") ?: "无内容"

        setupWebView(binding.webView)
        binding.webView.loadUrl(originalUrl)

        initScrollAnimation()
        observeLikeAndCollect()
        setupLikeClick()
        setupCollectClick()
        setupShareClick()
    }

    private fun readIntent() {
        title = intent.getStringExtra("article_title") ?: "无标题"
        originalUrl = intent.getStringExtra("article_original_url")?.takeIf { it.startsWith("http") }
            ?: "https://www.zhihu.com"
        articleId = intent.getIntExtra("article_id", -1)
    }

    private fun setupWebView(webView: WebView) {
        webView.webViewClient = WebViewClient()
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            // 启用媒体播放支持
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(true)
            builtInZoomControls = false
            displayZoomControls = false
        }
        // 允许混合内容（HTTP 视频在 HTTPS 页面中）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
    }

    private fun initScrollAnimation() {
        val triggerHeight = 120f
        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val progress = (scrollY / triggerHeight).coerceIn(0f, 1f)
            binding.authorContainer.alpha = progress
            binding.authorContainer.translationY = (1 - progress) * dp(8f)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun observeLikeAndCollect() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.isFavorite.collectLatest { updateLikeButton(it) }
                }
                launch {
                    viewModel.isCollected.collectLatest { updateCollectButton(it) }
                }
            }
        }
    }

    private fun updateLikeButton(isFavorite: Boolean) {
        binding.ivLike.setColorFilter(if (isFavorite) Color.RED else Color.GRAY)
    }

    private fun updateCollectButton(isCollected: Boolean) {
        binding.ivCollect.setColorFilter(if (isCollected) Color.DKGRAY else Color.GRAY)
    }

    private fun setupLikeClick() {
        binding.llLike.setOnClickListener { viewModel.toggleLike() }
    }

    private fun setupCollectClick() {
        binding.llCollect.setOnClickListener { viewModel.toggleCollect() }
    }

    private fun setupShareClick() {
        binding.llShare.setOnClickListener {
            val summary = content.take(80).replace("\n", "")
            val shareText = buildString {
                append(title)
                append("\n\n")
                if (summary.isNotEmpty()) {
                    append(summary)
                    if (content.length > 80) append("...")
                    append("\n\n")
                }
                append(originalUrl)
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(shareIntent, "分享到"))
        }
    }

    override fun finish() {
        val resultIntent = Intent().apply { putExtra("updated_article_id", articleId) }
        setResult(RESULT_OK, resultIntent)
        super.finish()
    }
}
