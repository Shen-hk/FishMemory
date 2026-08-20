package com.fishmemory.app.ui.publish.richtext.business.media

import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.fishmemory.richeditor.R
import com.fishmemory.app.ui.publish.richtext.api.EditorVideoUploader
import com.fishmemory.app.ui.publish.richtext.ui.container.BlockEditorRecyclerView
import com.fishmemory.app.ui.publish.richtext.core.model.BlockIdGenerator
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlockEntity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * 发布页图片/视频选择、URI 拷贝、插入与替换、uCrop、剪贴板 URL、预览。
 * Activity 仅注册 [ActivityResultLauncher] 并把结果转发到本类。
 */
class PublishMediaCoordinator(
    private val activity: AppCompatActivity,
    private val editor: BlockEditorRecyclerView,
    private val videoScope: CoroutineScope,
    private val getVideoUploader: () -> EditorVideoUploader,
    private val permissionHandler: VidepPermissionHandler,
    private val requestImageReadPermissionLauncher: ActivityResultLauncher<String>,
    private val requestCameraPermissionLauncher: ActivityResultLauncher<String>,
    private val requestVideoReadPermissionLauncher: ActivityResultLauncher<String>,
    private val launchPickImages: (Intent) -> Unit,
    private val launchTakePicture: (Uri) -> Unit,
    private val launchPickVideo: (Intent) -> Unit,
    private val launchUCrop: (Intent) -> Unit,
) {

    private var pendingCameraPhotoUri: Uri? = null
    private var pendingReplaceBlockId: String? = null

    /** 在打开相册/拍照/粘贴 URL 前保存的光标插入位置，避免返回后焦点丢失导致图片被追加到文末。 */
    private var pendingInsertCursorInfo: Pair<String, Int>? = null

    private var cropTargetBlockId: String? = null

    /**
     * 统一解析「在正文文本块中插入」的锚点：优先消费 [pendingInsertCursorInfo]（异步返回前快照），
     * 否则用当前焦点块光标；均无则返回 null，调用方应走文末 [EditorBlock] 追加路径。
     */
    private fun resolveInsertCursorForNewMedia(): Pair<String, Int>? {
        return pendingInsertCursorInfo?.also { pendingInsertCursorInfo = null }
            ?: editor.getFocusedTextCursorInfo()
    }

    fun startReplaceImageFlow(blockId: String) {
        pendingReplaceBlockId = blockId
        showImageSourceDialog()
    }

    fun onTakePictureResult(success: Boolean) {
        pendingCameraPhotoUri?.let { uri ->
            if (success) {
                val replaceId = pendingReplaceBlockId
                pendingReplaceBlockId = null
                if (replaceId != null) {
                    editor.updateImageBlock(replaceId, uri.toString(), null)
                } else {
                    insertImageBlockFromUri(uri)
                }
            }
            pendingCameraPhotoUri = null
        }
    }

    fun onImageReadPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            activity.runOnUiThread {
                launchMultipleImagePicker()
            }
        } else {
            Toast.makeText(
                activity,
                "需要存储权限才能选择图片",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun onVideoReadPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            launchVideoPicker()
        } else {
            Toast.makeText(activity, "需要存储权限才能选择视频", Toast.LENGTH_SHORT).show()
        }
    }

    fun onCameraPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(activity, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    fun onUCropActivityResult(isResultOk: Boolean, data: Intent?) {
        if (isResultOk) {
            val outputUri = UCrop.getOutput(data ?: return) ?: return
            val blockId = cropTargetBlockId
            cropTargetBlockId = null
            if (blockId != null) {
                editor.updateImageBlock(blockId, outputUri.toString(), null)
            }
        } else {
            cropTargetBlockId = null
        }
    }

    fun checkAndRequestVideoPermission() {
        permissionHandler.checkAndRequestVideoReadPermission(
            onGranted = { launchVideoPicker() },
            requestLauncher = requestVideoReadPermissionLauncher
        )
    }

    private fun checkAndRequestImageReadPermission() {
        permissionHandler.checkAndRequestImageReadPermission(
            onGranted = { launchMultipleImagePicker() },
            requestLauncher = requestImageReadPermissionLauncher
        )
    }

    private fun checkCameraAndLaunch() {
        permissionHandler.checkCameraAndLaunch(
            onGranted = { launchCamera() },
            requestLauncher = requestCameraPermissionLauncher
        )
    }

    private fun startCropForImageBlock(blockId: String) {
        try {
            val sourceUri = editor.getImageBlockSourceUri(blockId)
            if (sourceUri == null) {
                Toast.makeText(activity, "无法获取图片源，请重新选择图片", Toast.LENGTH_SHORT).show()
                return
            }

            if (!isUriAccessible(sourceUri)) {
                Toast.makeText(activity, "图片文件无法访问，请重新选择", Toast.LENGTH_SHORT).show()
                return
            }

            val destinationUri = try {
                Uri.fromFile(File(activity.cacheDir, "crop_${System.currentTimeMillis()}.jpg"))
            } catch (e: Exception) {
                Toast.makeText(activity, "创建临时文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
                return
            }

            val options = UCrop.Options().apply {
                setFreeStyleCropEnabled(true)
                setHideBottomControls(false)
                setCompressionQuality(90)
                try {
                    setToolbarColor(ContextCompat.getColor(activity, R.color.holo_purple))
                    setStatusBarColor(ContextCompat.getColor(activity, R.color.black))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set uCrop colors", e)
                }
            }

            cropTargetBlockId = blockId

            val intent = try {
                UCrop.of(sourceUri, destinationUri)
                    .withOptions(options)
                    .getIntent(activity)
            } catch (e: Exception) {
                Toast.makeText(activity, "初始化图片裁剪失败: ${e.message}", Toast.LENGTH_SHORT).show()
                return
            }

            launchUCrop(intent)
        } catch (e: Exception) {
            Log.e(TAG, "启动图片裁剪失败", e)
            Toast.makeText(activity, "图片裁剪功能暂时不可用: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** 检查 URI 是否可读；content 仅探测能否打开流，file 检查存在与读权限。 */
    private fun isUriAccessible(uri: Uri): Boolean {
        return try {
            when {
                uri.scheme == "content" -> {
                    activity.contentResolver.openInputStream(uri)?.use { it.close() }
                    true
                }
                uri.scheme == "file" -> {
                    val file = File(uri.path ?: "")
                    file.exists() && file.canRead()
                }
                else -> true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun showImageBlockMenu(anchorView: View, blockId: String) {
        val popup = PopupMenu(activity, anchorView)
        popup.menu.add(0, 1, 0, "编辑")
        popup.menu.add(0, 2, 1, "注释")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    startCropForImageBlock(blockId)
                    true
                }
                2 -> {
                    editor.showCaptionForImageBlock(blockId)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun launchVideoPicker() {
        pendingInsertCursorInfo = editor.getFocusedTextCursorInfo()
        launchPickVideo(VideoPicker.createPickIntent())
    }

    fun handleSelectedVideo(uri: Uri) {
        videoScope.launch {
            val internalUri = InternalMediaCopier.copyToInternalFilesDir(
                activity,
                uri,
                InternalMediaCopier.MediaKind.VIDEO,
                TAG,
            )
            if (internalUri == null) {
                Toast.makeText(activity, "无法读取视频文件", Toast.LENGTH_SHORT).show()
                return@launch
            }

            Log.d(TAG, "✅ 视频复制成功：$internalUri")

            val internalUriObj = Uri.parse(internalUri)
            val meta = VideoPicker.parseVideoMeta(activity, internalUriObj)
            if (meta == null) {
                Toast.makeText(activity, "无法解析视频信息", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val blockId = BlockIdGenerator.nextId()
            val videoBlock = EditorBlock.VideoBlock(
                id = blockId,
                localUri = internalUri.toString(),
                remoteUrl = null,
                coverUrl = meta.coverPath,
                durationMs = meta.durationMs,
                uploadState = EditorBlockEntity.UploadState.PENDING,
                uploadProgress = 0
            )
            val (blockIdForInsert, cursorPos) = resolveInsertCursorForNewMedia()
                ?: run {
                    val anchorId = editor.getAnchorBlockIdForInsert()
                    editor.addVideoBlocksAfter(anchorId, listOf(videoBlock))
                    getVideoUploader().enqueueUpload(blockId, internalUri.toString(), createVideoUploadCallback())
                    return@launch
                }
            val newTextBlockId = editor.insertVideoAtCursor(videoBlock, blockIdForInsert, cursorPos)
            if (newTextBlockId != null) {
                getVideoUploader().enqueueUpload(blockId, internalUri.toString(), createVideoUploadCallback())
            }
        }
    }

    private fun createVideoUploadCallback(): EditorVideoUploader.Callback {
        return object : EditorVideoUploader.Callback {
            override fun onProgress(blockId: String, progress: Int) {
                editor.updateVideoBlockProgress(blockId, progress)
            }

            override fun onSuccess(blockId: String, remoteUrl: String) {
                editor.updateVideoBlockRemoteUrl(blockId, remoteUrl)
            }

            override fun onError(blockId: String, throwable: Throwable) {
                editor.updateVideoBlockFailed(blockId)
                Toast.makeText(activity, "视频上传失败: ${throwable.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showImageSourceDialog() {
        MaterialAlertDialogBuilder(activity)
            .setTitle("添加图片")
            .setItems(arrayOf("相册", "拍照", "粘贴 URL")) { _, which ->
                when (which) {
                    0 -> {
                        pendingInsertCursorInfo = editor.getFocusedTextCursorInfo()
                        checkAndRequestImageReadPermission()
                    }
                    1 -> {
                        pendingInsertCursorInfo = editor.getFocusedTextCursorInfo()
                        checkCameraAndLaunch()
                    }
                    2 -> handlePasteImageUrl()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun handlePasteImageUrl() {
        val clip = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = clip?.primaryClip?.getItemAt(0)?.coerceToText(activity)?.toString()?.trim()
        if (isImageUrl(text)) {
            insertImageBlockFromUrl(text!!)
        } else {
            showPasteUrlInputDialog()
        }
    }

    private fun isImageUrl(s: String?): Boolean {
        if (s.isNullOrBlank()) return false
        return Regex("^https?://.*\\.(jpg|jpeg|png|gif|webp)(\\?.*)?$", RegexOption.IGNORE_CASE).matches(s)
    }

    private fun showPasteUrlInputDialog() {
        pendingInsertCursorInfo = editor.getFocusedTextCursorInfo()
        val input = EditText(activity).apply {
            hint = "输入图片 URL"
            setPadding(48, 32, 48, 32)
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle("粘贴图片 URL")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) insertImageBlockFromUrl(url)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun insertImageBlockFromUrl(url: String) {
        val replaceId = pendingReplaceBlockId
        pendingReplaceBlockId = null
        if (replaceId != null) {
            editor.updateImageBlock(replaceId, null, url)
        } else {
            val (blockId, cursorPos) = resolveInsertCursorForNewMedia()
                ?: run {
                    val block = EditorBlock.ImageBlock(
                        id = BlockIdGenerator.nextId(),
                        localUri = null,
                        remoteUrl = url,
                        caption = "",
                        uploadState = EditorBlockEntity.UploadState.SUCCESS
                    )
                    val anchorId = editor.getAnchorBlockIdForInsert()
                    editor.addImageBlocksAfter(anchorId, listOf(block))
                    return
                }
            val uri = Uri.parse(url)
            editor.insertImageAtCursor(uri, blockId, cursorPos)
        }
    }

    private fun launchCamera() {
        val photoFile = File(activity.cacheDir, "publish_${System.currentTimeMillis()}.jpg")
        pendingCameraPhotoUri = FileProvider.getUriForFile(
            activity, "${activity.packageName}.fileprovider", photoFile
        )
        launchTakePicture(pendingCameraPhotoUri!!)
    }

    private fun insertImageBlockFromUri(uri: Uri) {
        val (blockId, cursorPos) = resolveInsertCursorForNewMedia()
            ?: run {
                val block = EditorBlock.ImageBlock(
                    id = BlockIdGenerator.nextId(),
                    localUri = uri.toString(),
                    remoteUrl = null,
                    caption = "",
                    uploadState = EditorBlockEntity.UploadState.SUCCESS
                )
                val anchorId = editor.getAnchorBlockIdForInsert()
                editor.addImageBlocksAfter(anchorId, listOf(block))
                return
            }
        editor.insertImageAtCursor(uri, blockId, cursorPos)
    }

    private fun copyImageToInternalStorage(uri: Uri): String? =
        InternalMediaCopier.copyToInternalFilesDir(
            activity,
            uri,
            InternalMediaCopier.MediaKind.IMAGE,
            TAG,
        )

    private fun launchMultipleImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            setType("image/*")
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        launchPickImages(intent)
    }

    fun handleSelectedImages(data: Intent) {
        Log.d("TitleDebug", "[handleSelectedImages] start, uris count=")
        val uris = mutableListOf<Uri>()
        data.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                clip.getItemAt(i).uri?.let { uris.add(it) }
            }
        } ?: run {
            data.data?.let { uris.add(it) }
        }
        if (uris.isEmpty()) return

        Log.d("TitleDebug", "[handleSelectedImages] before insert, title=[${editor.getTitleText()}]")
        val replaceId = pendingReplaceBlockId
        pendingReplaceBlockId = null

        if (replaceId != null) {
            val internalUri = copyImageToInternalStorage(uris.first())
            if (internalUri != null) {
                editor.updateImageBlock(replaceId, internalUri, null)
            }
            uris.drop(1).forEach { uri ->
                val internalUriCopy = copyImageToInternalStorage(uri)
                if (internalUriCopy != null) {
                    insertImageBlockFromUri(Uri.parse(internalUriCopy))
                }
            }
        } else {
            val savedCursor = pendingInsertCursorInfo?.also { pendingInsertCursorInfo = null }
            if (savedCursor != null && uris.isNotEmpty()) {
                val (blockId, cursorPos) = savedCursor
                var currentBlockId = blockId
                var currentCursorPos = cursorPos

                for (i in uris.indices) {
                    val internalUri = copyImageToInternalStorage(uris[i])
                    if (internalUri != null) {
                        val newTextBlockId = editor.insertImageAtCursor(Uri.parse(internalUri), currentBlockId, currentCursorPos)
                        if (newTextBlockId != null) {
                            currentBlockId = newTextBlockId
                            currentCursorPos = 0
                        }
                    }
                }
            } else {
                val anchorId = editor.getAnchorBlockIdForInsert()
                var currentAnchor = anchorId

                val anchorBlock = editor.blockList.findBlock(currentAnchor) as? EditorBlock.TextBlock
                var cursorPos = anchorBlock?.text?.length ?: 0

                for (uri in uris) {
                    val internalUri = copyImageToInternalStorage(uri)
                    if (internalUri != null) {
                        val currentTitleBefore = editor.getTitleText()
                        Log.d("TitleDebug", "[insertImage loop] before title=[$currentTitleBefore]")
                        val newTextBlockId = editor.insertImageAtCursor(Uri.parse(internalUri), currentAnchor, cursorPos)
                        val currentTitleAfter = editor.getTitleText()
                        Log.d("TitleDebug", "[insertImage loop] after title=[$currentTitleAfter], newBlockId=$newTextBlockId")
                        if (newTextBlockId != null) {
                            currentAnchor = newTextBlockId
                            cursorPos = 0
                        }
                    }
                }
            }
        }
    }

    fun showImagePreview(imageUrl: String) {
        val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(activity)

        Glide.with(activity)
            .load(Uri.parse(imageUrl))
            .into(imageView)

        imageView.setOnClickListener { dialog.dismiss() }
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        dialog.setContentView(imageView)
        dialog.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        dialog.show()
    }

    private companion object {
        private const val TAG = "PublishMediaCoordinator"
    }
}
