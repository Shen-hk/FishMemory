package com.fishmemory.app.ui.publish

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputFilter
import android.text.Spanned
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fishmemory.app.databinding.ActivityPublishBinding
import com.fishmemory.app.ui.publish.richtext.model.RichDocument
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.max



class PublishActivity : AppCompatActivity() {

    companion object {
        // ✅ 标题最大长度限制
        private const val MAX_TITLE_LENGTH = 40
    }

    private lateinit var binding: ActivityPublishBinding

    // 图片选择启动器（单张和多张共用）
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { handleSelectedImages(it) }
        }
    }

    // 权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限获取成功后，重新触发图片选择对话框
            runOnUiThread {
            launchMultipleImagePicker()
            }
        } else {
            android.widget.Toast.makeText(
                this,
                "需要存储权限才能选择图片",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 绑定 ScrollView
        binding.editorView.bindScrollView(binding.contentScroll)

        // 标题输入/粘贴行为约束
        setupTitleInputConstraints()

        // ===== 关键：IME 高度监听 =====
        ViewCompat.setOnApplyWindowInsetsListener(binding.activitypublishroot) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            val realImeHeight = max(0, imeHeight - navHeight)

            // 👉 告诉编辑器真实被遮挡的高度
            binding.editorView.updateImeHeight(realImeHeight)

            // 底部工具栏只做位移
            binding.bottomBar.translationY = -realImeHeight.toFloat()

            insets
        }

        // 设置图片监听器
        binding.editorView.setOnImageBlockListener(object : com.fishmemory.app.ui.publish.richtext.editor.RichEditorView.OnImageBlockListener {
            override fun onImageAdded(imageBlock: com.fishmemory.app.ui.publish.richtext.model.RichBlock.Image) {
                // 图片添加成功
                android.util.Log.d("PublishActivity", "图片添加: ${imageBlock.url}")
            }

            override fun onImageClicked(imageBlock: com.fishmemory.app.ui.publish.richtext.model.RichBlock.Image) {
                // 点击图片预览
                showImagePreview(imageBlock.url)
            }

            override fun onImageRemoved(imageBlock: com.fishmemory.app.ui.publish.richtext.model.RichBlock.Image) {
                // 图片被删除
                android.util.Log.d("PublishActivity", "图片删除: ${imageBlock.url}")
            }
        })

        // 工具栏绑定
        binding.btnBold.setOnClickListener {
            binding.editorView.toggleBold()
        }
        binding.btnUnderline.setOnClickListener {
            binding.editorView.toggleUnderline()
        }

        // 图片按钮点击
        binding.btnImage.setOnClickListener {
            checkAndRequestPermission()
        }
    }

    /**
     * 构建当前页面对应的完整文档模型：
     * - 标题：来自标题 EditText
     * - blocks：来自 RichEditorView 的正文 Block 列表
     */
    private fun buildCurrentDocument(): RichDocument {
        val title = binding.etTitle.text?.toString().orEmpty()
        val blocks = binding.editorView.getBlocks()
        return RichDocument(title = title, blocks = blocks)
    }

    /**
     * 根据给定文档模型渲染页面内容。
     * - 标题直接写入标题 EditText
     * - 正文 Block 列表交由 RichEditorView 重建
     */
    private fun renderDocument(document: RichDocument) {
        binding.etTitle.setText(document.title)
        binding.editorView.setBlocks(document.blocks)
    }

    /**
     * 标题输入规范化约束：
     * 1. 禁止换行（输入/粘贴阶段直接丢弃换行符）
     * 2. 限制最大长度
     * 3. 回车不插入换行，而是显式跳到正文第一个 Block
     */
    private fun setupTitleInputConstraints() {
        val titleEt = binding.etTitle

        // 1️⃣ 使用自定义 InputFilter，在「输入阶段」就过滤掉换行并限制长度
        titleEt.filters = arrayOf(
            SingleLineNoNewLineFilter(MAX_TITLE_LENGTH)
        )

        // 2️⃣ 配置 IME 动作为“下一步”，让回车走 EditorAction 流程
        titleEt.imeOptions = EditorInfo.IME_ACTION_NEXT

        // 3️⃣ 统一处理回车：不插入换行，直接跳到正文第一个 Block
        titleEt.setOnEditorActionListener { _, actionId, event ->
            val isEnterKey = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_DOWN
            val isNextAction = actionId == EditorInfo.IME_ACTION_NEXT

            if (isEnterKey || isNextAction) {
                // 🔥 不让标题产生换行，而是显式把焦点交给正文
                binding.editorView.focusFirstTextBlock()
                true // 消费事件，阻止系统继续插入换行
            } else {
                false
            }
        }
    }

    // 修改 checkAndRequestPermission 函数
    private fun checkAndRequestPermission() {
        // 检查权限（Android 13+ 使用新权限，13以下用旧权限）
        val permissionToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(
                this,
                permissionToRequest
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
         //有权限
            launchMultipleImagePicker()
        } else {
            // 无权限->请求权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionToRequest)) {
                // 向用户解释为什么需要权限
                showPermissionRationaleDialog(permissionToRequest)
            } else {
                // 直接请求权限
                requestPermissionLauncher.launch(permissionToRequest)
            }
        }
    }

    // 添加权限解释对话框
    private fun showPermissionRationaleDialog(permission: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("需要存储权限")
            .setMessage("需要存储权限才能访问您的相册，选择图片添加到文章中")
            .setPositiveButton("确定") { _, _ ->
                requestPermissionLauncher.launch(permission)
            }
            .setNegativeButton("取消", null)
            .show()
    }


    private fun launchMultipleImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickImageLauncher.launch(intent)
    }

    private fun handleSelectedImages(data: Intent) {
        val clipData = data.clipData

        if (clipData != null) {
            // 多选图片
            for (i in 0 until clipData.itemCount) {
                val uri = clipData.getItemAt(i).uri
                binding.editorView.addLocalImage(uri)
            }
        } else {
            // 单选图片
            val uri = data.data
            uri?.let { binding.editorView.addLocalImage(it) }
        }
    }

    private fun showImagePreview(imageUrl: String) {
        // 创建预览对话框
        val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = android.widget.ImageView(this)

        // 使用 Glide 加载图片
        com.bumptech.glide.Glide.with(this)
            .load(Uri.parse(imageUrl))
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

    /**
     * 标题专用 InputFilter：
     * - 在「输入 / 粘贴阶段」就丢弃所有换行符
     * - 且根据剩余可用长度做截断，保证标题始终为单行，长度受控
     */
    private class SingleLineNoNewLineFilter(
        private val maxLength: Int
    ) : InputFilter {

        override fun filter(
            source: CharSequence?, // 本次尝试插入的内容（输入法/粘贴都走这里）
            start: Int,
            end: Int,
            dest: Spanned?,        // 目标文本当前内容
            dstart: Int,
            dend: Int
        ): CharSequence? {
            if (source.isNullOrEmpty()) return null // 返回 null 表示不改动，让系统按原逻辑处理

            // 1️⃣ 先去掉所有换行符，保证标题物理上只有一行
            val builder = StringBuilder()
            for (i in start until end) {
                val ch = source[i]
                if (ch == '\n' || ch == '\r') {
                    // 丢弃换行：通过「不插入」来禁止，而不是事后再删
                    continue
                }
                builder.append(ch)
            }
            val sanitized = builder.toString()
            if (sanitized.isEmpty()) {
                // 全部是换行，被过滤掉，则直接返回空字符串
                return ""
            }

            // 2️⃣ 计算当前可插入的最大长度（考虑本次替换范围）
            val destLength = dest?.length ?: 0
            val keep = maxLength - (destLength - (dend - dstart))
            if (keep <= 0) {
                // 标题已达或超过最大长度，本次输入/粘贴全部丢弃
                return ""
            }

            // 3️⃣ 如有必要，对本次内容做截断，保证不会越界
            return if (sanitized.length <= keep) {
                sanitized
            } else {
                sanitized.substring(0, keep)
            }
        }
    }
}