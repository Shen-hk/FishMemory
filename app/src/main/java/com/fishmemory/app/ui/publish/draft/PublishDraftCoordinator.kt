package com.fishmemory.app.ui.publish.draft

import androidx.appcompat.app.AppCompatActivity
import com.fishmemory.app.data.local.rooms.AppDatabase
import com.fishmemory.app.data.local.rooms.entity.DraftStatus
import com.fishmemory.app.databinding.ActivityPublishBinding
import com.fishmemory.app.ui.publish.richtext.core.model.Document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 草稿：加载/切换、[DraftAutoSaver] 与状态文案；正文读写通过 [buildDocument]、[renderDocument] 注入，不直接依赖编辑器实现细节。
 */
class PublishDraftCoordinator(
    private val activity: AppCompatActivity,
    private val binding: ActivityPublishBinding,
    private val buildDocument: () -> Document,
    private val renderDocument: (Document) -> Unit,
) {

    /** 与自动保存、发布落库共用的作用域（在子协程中切 IO）。 */
    val draftScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var draftManager: DraftManager
    private var currentDraftId: String? = null
    private var autoSaver: DraftAutoSaver? = null

    fun attachAndStart() {
        val db = AppDatabase.Companion.getInstance(activity.applicationContext)
        draftManager = DraftManager(db.draftDao())

        binding.blockEditorView.setOnContentChangedListener {
            autoSaver?.markDirty()
        }

        autoSaver = DraftAutoSaver(
            scope = draftScope,
            intervalMs = 30_000L,
            onAutoSave = {
                val draftId = currentDraftId ?: return@DraftAutoSaver
                val doc = buildDocument()
                draftManager.save(draftId, doc, DraftStatus.ACTIVE)
            },
            onStateChanged = { state ->
                when (state) {
                    is DraftAutoSaver.State.Dirty -> binding.tvDraftStatus.text = "未保存"
                    is DraftAutoSaver.State.Saving -> binding.tvDraftStatus.text = "保存中…"
                    is DraftAutoSaver.State.Saved -> binding.tvDraftStatus.text = "已保存"
                    is DraftAutoSaver.State.Error -> binding.tvDraftStatus.text = "保存失败"
                }
            }
        ).also { it.start() }

        draftScope.launch {
            val snapshot = draftManager.loadLatestActiveOrNull() ?: draftManager.createNewEmptyDraft()
            currentDraftId = snapshot.draftId
            draftManager.markOpened(snapshot.draftId)
            renderDocument(snapshot.document)
            binding.tvDraftStatus.text = "已保存"
        }
    }

    /**
     * 从草稿列表返回后加载指定草稿；切换前先 [DraftAutoSaver.saveNow]，避免丢未落盘编辑。
     */
    fun onDraftSelectedFromPicker(draftId: String) {
        draftScope.launch {
            runCatching { autoSaver?.saveNow() }

            val snapshot = draftManager.loadByIdOrNull(draftId) ?: return@launch
            currentDraftId = snapshot.draftId
            draftManager.markOpened(snapshot.draftId)
            renderDocument(snapshot.document)
            binding.tvDraftStatus.text = "已保存"
        }
    }

    fun onStop() {
        draftScope.launch {
            runCatching {
                autoSaver?.saveNow()
            }
        }
    }

    fun onDestroy() {
        autoSaver?.stop()
        draftScope.cancel()
    }
}