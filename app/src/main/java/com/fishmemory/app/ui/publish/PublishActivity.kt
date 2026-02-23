package com.fishmemory.app.ui.publish

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fishmemory.app.databinding.ActivityPublishBinding
import com.fishmemory.app.ui.publish.draft.PublishDraftCoordinator
import com.fishmemory.app.ui.publish.draftlist.DraftListActivity
import com.fishmemory.app.ui.publish.richtext.business.media.PublishMediaCoordinator
import com.fishmemory.app.ui.publish.richtext.business.media.VideoPlayerManager
import com.fishmemory.app.ui.publish.richtext.business.media.VideoUploadManager
import com.fishmemory.app.ui.publish.richtext.business.media.VidepPermissionHandler
import com.fishmemory.app.ui.publish.richtext.core.engine.span.EditorSpanApplier
import com.fishmemory.app.ui.publish.richtext.core.model.Document
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.usecase.PublishArticleUseCase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

import com.fishmemory.app.ui.publish.richtext.ui.container.BlockEditorRecyclerView

class PublishActivity : AppCompatActivity() {

    private companion object {
        private const val TAG = "PublishActivity"
    }

    private lateinit var binding: ActivityPublishBinding
    private lateinit var permissionHandler: VidepPermissionHandler
    private lateinit var mediaCoordinator: PublishMediaCoordinator
    private lateinit var draftCoordinator: PublishDraftCoordinator
    private lateinit var publishArticleUseCase: PublishArticleUseCase

    private lateinit var videoPlayerManager: VideoPlayerManager
    private lateinit var videoUploadManager: VideoUploadManager
    private val videoScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { mediaCoordinator.handleSelectedImages(it) }
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        mediaCoordinator.onTakePictureResult(success)
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        mediaCoordinator.onCameraPermissionResult(isGranted)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        mediaCoordinator.onImageReadPermissionResult(isGranted)
    }

    private val pickDraftLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val draftId = result.data?.getStringExtra(DraftListActivity.EXTRA_SELECTED_DRAFT_ID)
            ?: return@registerForActivityResult
        draftCoordinator.onDraftSelectedFromPicker(draftId)
    }

    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri -> mediaCoordinator.handleSelectedVideo(uri) }
        }
    }

    private val requestVideoPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        mediaCoordinator.onVideoReadPermissionResult(isGranted)
    }

    private val uCropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        mediaCoordinator.onUCropActivityResult(
            isResultOk = result.resultCode == RESULT_OK,
            data = result.data
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionHandler = VidepPermissionHandler(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.activitypublishroot) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val realImeHeight = max(0, imeHeight - navHeight)
            binding.blockEditorView.setImeBottomPadding(realImeHeight)
            binding.bottomBarScroll.translationY = -realImeHeight.toFloat()
            insets
        }

        videoPlayerManager = VideoPlayerManager(this)
        videoUploadManager = VideoUploadManager(this)
        binding.blockEditorView.setVideoManagers(videoPlayerManager, videoUploadManager)

        mediaCoordinator = PublishMediaCoordinator(
            activity = this,
            editor = binding.blockEditorView,
            videoScope = videoScope,
            getVideoUploadManager = { videoUploadManager },
            permissionHandler = permissionHandler,
            requestImageReadPermissionLauncher = requestPermissionLauncher,
            requestCameraPermissionLauncher = requestCameraPermissionLauncher,
            requestVideoReadPermissionLauncher = requestVideoPermissionLauncher,
            launchPickImages = { intent -> pickImageLauncher.launch(intent) },
            launchTakePicture = { uri -> takePictureLauncher.launch(uri) },
            launchPickVideo = { intent -> pickVideoLauncher.launch(intent) },
            launchUCrop = { intent -> uCropLauncher.launch(intent) }
        )

        draftCoordinator = PublishDraftCoordinator(
            activity = this,
            binding = binding,
            buildDocument = { buildCurrentDocument() },
            renderDocument = { renderDocument(it) }
        )
        publishArticleUseCase =
            PublishArticleUseCase(applicationContext, draftCoordinator.draftScope)

        binding.btnBold.setOnClickListener {
            binding.blockEditorView.getFocusedEditText()?.let {
                EditorSpanApplier.toggleBold(it)
            }
        }
        binding.btnUnderline.setOnClickListener {
            binding.blockEditorView.getFocusedEditText()?.let {
                EditorSpanApplier.toggleUnderline(it)
            }
        }

        binding.btnHeading.setOnClickListener {
            binding.blockEditorView.toggleHeadingForFocusedBlock()
        }

        binding.btnBulletList.setOnClickListener {
            binding.blockEditorView.toggleListForFocusedBlock(EditorBlock.ListType.BULLET_LIST)
        }
        binding.btnNumberList.setOnClickListener {
            binding.blockEditorView.toggleListForFocusedBlock(EditorBlock.ListType.NUMBER_LIST)
        }

        binding.btnQuote.setOnClickListener {
            binding.blockEditorView.toggleQuoteForFocusedBlock()
        }

        binding.btnHr.setOnClickListener {
            binding.blockEditorView.insertHorizontalRule()
        }

        binding.btnCode.setOnClickListener {
            showCodeLanguageDialog()
        }

        binding.btnImage.setOnClickListener { mediaCoordinator.showImageSourceDialog() }

        binding.btnVideo.setOnClickListener { mediaCoordinator.checkAndRequestVideoPermission() }

        binding.btnPublish.setOnClickListener {
            val titleBefore = binding.blockEditorView.getTitleText()
            Log.d("TitleDebug", "[导出前] title=[$titleBefore]")
            val document = buildCurrentDocument()
            Log.d("TitleDebug", "[导出后] title=[${document.title}], blocks=${document.blocks.size}")
            when (val outcome = publishArticleUseCase.execute(document)) {
                is PublishArticleUseCase.Outcome.Success -> shareExportedJsonFile(outcome.jsonFile)
                is PublishArticleUseCase.Outcome.FailureCreateFile ->
                    Toast.makeText(this, "导出失败，无法创建文件", Toast.LENGTH_SHORT).show()
                is PublishArticleUseCase.Outcome.FailureExport ->
                    Toast.makeText(this, "导出失败，请稍后重试", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDrafts.setOnClickListener {
            pickDraftLauncher.launch(DraftListActivity.createIntent(this))
        }

        binding.blockEditorView.setOnImageBlockPreviewRequested { url -> mediaCoordinator.showImagePreview(url) }
        binding.blockEditorView.setOnImageBlockReplaceRequested { blockId ->
            mediaCoordinator.startReplaceImageFlow(blockId)
        }
        binding.blockEditorView.setOnImageBlockMenuRequested { anchorView, blockId ->
            mediaCoordinator.showImageBlockMenu(anchorView, blockId)
        }

        draftCoordinator.attachAndStart()
    }

    private fun buildCurrentDocument(): Document {
        val title = binding.blockEditorView.getTitleText()
        Log.d("TitleDebug", "[buildCurrentDocument] title=[$title]")
        val blocks = binding.blockEditorView.getBlocks()
        Log.d("TitleDebug", "[buildCurrentDocument] blocks count=${blocks.size}")
        return Document(title = title, blocks = blocks)
    }

    private fun renderDocument(document: Document) {
        binding.blockEditorView.setTitleText(document.title)
        binding.blockEditorView.setBlocks(document.blocks)
    }

    /** 系统分享导出 JSON；需 Activity 以启动 Chooser 与授权读 URI。 */
    private fun shareExportedJsonFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "分享文章 JSON"))
        } catch (e: Exception) {
            Log.e(TAG, "shareExportedJsonFile failed", e)
            Toast.makeText(this, "分享失败，请稍后重试", Toast.LENGTH_SHORT).show()
        }
    }

    /** 在当前光标位置插入代码块，并让代码块获取焦点。 */
    fun insertCodeBlock(language: String) {
        binding.blockEditorView.insertCodeBlock(language)
    }

    private fun showCodeLanguageDialog() {
        val languages = arrayOf("Java", "Kotlin", "Python", "JavaScript", "HTML", "CSS", "SQL", "其他")
        MaterialAlertDialogBuilder(this)
            .setTitle("选择代码语言")
            .setItems(languages) { _, which ->
                val language = when (which) {
                    0 -> "java"
                    1 -> "kotlin"
                    2 -> "python"
                    3 -> "javascript"
                    4 -> "html"
                    5 -> "css"
                    6 -> "sql"
                    else -> "text"
                }
                insertCodeBlock(language)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onStop() {
        super.onStop()
        if (::videoPlayerManager.isInitialized) {
            videoPlayerManager.pauseAll()
        }
        draftCoordinator.onStop()
    }

    override fun onDestroy() {
        if (::videoPlayerManager.isInitialized) {
            videoPlayerManager.releaseAll()
        }
        draftCoordinator.onDestroy()
        videoScope.cancel()
        super.onDestroy()
    }
}
