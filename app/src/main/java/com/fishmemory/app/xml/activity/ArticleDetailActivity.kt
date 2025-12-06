package com.fishmemory.app.xml.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fishmemory.app.databinding.ActivityArticleDetailBinding

class ArticleDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityArticleDetailBinding

    // 用于回传结果
    private var isFavorite = false
    private var isCollected = false
    private var articleId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 从 Intent 读取数据
        val title = intent.getStringExtra("article_title") ?: "无标题"
        val author = intent.getStringExtra("article_author") ?: "无作者"
        val content = intent.getStringExtra("article_content") ?: "无内容"
        val category = intent.getStringExtra("article_category") ?: ""
        val createdAt = intent.getStringExtra("article_created_at") ?: "无时间"
        val readTime = intent.getIntExtra("article_read_time", 0)
        val likeCount = intent.getIntExtra("article_like_count", 0)
        val commentCount = intent.getIntExtra("article_comment_count", 0)
        val tags = intent.getStringExtra("article_tags") ?: ""
        val covimage=intent.getStringExtra("article_cover_image")?:""
        val originalUrl=intent.getStringExtra("article_original_url")?:"\"https://yourapp.com\""

        // 👇 关键：读取状态
        isFavorite = intent.getBooleanExtra("article_is_favorite", false)
        isCollected = intent.getBooleanExtra("article_is_collect", false) // 注意拼写！
        articleId = intent.getIntExtra("article_id", -1)

        // 绑定基础内容
        binding.tvDetailTitle.text = title
        binding.tvDetailAuthor.text = "作者：$author · $createdAt"
        binding.tvDetailContent.text = content


        // 初始化按钮状态
        updateLikeButton()
        updateCollectButton()

        // 点赞点击
        binding.llLike.setOnClickListener {
            isFavorite = !isFavorite
            updateLikeButton()
        }

        // 收藏点击
        binding.llCollect.setOnClickListener {
            isCollected = !isCollected
            updateCollectButton()
        }
        //分享
        binding.llShare.setOnClickListener {
            val summary=content.take(80).replace("\n","")
            val shareText=buildString {
                append(title)
                append("\n\n")
                if (summary.isNotEmpty()) {
                append(summary)
                    if (content.length>80)append("...")
                    append("\n\n")
                }
                append(originalUrl)
                }

        val shareIntent=Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        // 启动系统分享面板
        startActivity(Intent.createChooser(shareIntent, "分享到"))
    }
    }




    private fun updateLikeButton() {
        binding.ivLike.setColorFilter(
            if (isFavorite) Color.RED else Color.GRAY
        )
    }

    private fun updateCollectButton() {
        binding.ivCollect.setColorFilter(
            if (isCollected) Color.DKGRAY else Color.GRAY
        )
    }

    override fun finish() {
        // 回传更新后的状态
        val resultIntent = Intent().apply {
            putExtra("updated_article_id", articleId)
            putExtra("updated_is_favorite", isFavorite)
            putExtra("updated_is_collect", isCollected)
        }
        setResult(RESULT_OK, resultIntent)
        super.finish()
    }
}