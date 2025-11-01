package com.fishmemory.app.ui.publish.richtext.ui.container

import android.graphics.Rect
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.app.R
import com.fishmemory.app.ui.publish.richtext.business.selection.BlockEditorUiSideEffects
import com.fishmemory.app.ui.publish.richtext.ui.adapter.CodeBlockViewHolder
import com.fishmemory.app.ui.publish.richtext.ui.adapter.EditorAdapter
import com.fishmemory.app.ui.publish.richtext.ui.adapter.ImageBlockViewHolder
import com.fishmemory.app.ui.publish.richtext.ui.adapter.TextBlockViewHolder

/**
 * `BlockEditorUiSideEffects` 的默认实现，封装 RecyclerView + Adapter 的具体操作。
 *
 * 说明：
 * - 行为从 `BlockEditorRecyclerView` 中原有的 anonymous object 迁移而来；
 * - 不改变任何 focus/scroll/notify 时序，仅做位置与职责收敛。
 */
class BlockEditorUiSideEffectsImpl(
    private val recyclerView: RecyclerView,
    private val adapter: EditorAdapter,
    private val notifyContentChangedDelegate: () -> Unit,
) : BlockEditorUiSideEffects {

    override fun notifyItemChanged(adapterPos: Int) {
        adapter.notifyItemChanged(adapterPos)
    }

    override fun notifyItemInserted(adapterPos: Int) {
        adapter.notifyItemInserted(adapterPos)
    }

    override fun notifyItemRemoved(adapterPos: Int) {
        adapter.notifyItemRemoved(adapterPos)
    }

    override fun notifyItemRangeInserted(startAdapterPos: Int, itemCount: Int) {
        adapter.notifyItemRangeInserted(startAdapterPos, itemCount)
    }

    override fun notifyItemRangeChanged(startAdapterPos: Int, itemCount: Int) {
        adapter.notifyItemRangeChanged(startAdapterPos, itemCount)
    }

    override fun notifyDataSetChanged() {
        adapter.notifyDataSetChanged()
    }

    override fun scrollToAdapterPos(adapterPos: Int) {
        recyclerView.scrollToPosition(adapterPos)
    }

    override fun ensureBlockVisibleByAdapterPos(adapterPos: Int) {
        fun requestVisible() {
            val vh = recyclerView.findViewHolderForAdapterPosition(adapterPos) ?: return
            val item: View = vh.itemView ?: return
            val rect = Rect(0, 0, item.width, item.height)
            item.requestRectangleOnScreen(rect, true)
        }

        recyclerView.post {
            val vh = recyclerView.findViewHolderForAdapterPosition(adapterPos)
            if (vh != null) {
                requestVisible()
            } else {
                recyclerView.scrollToPosition(adapterPos)
                recyclerView.post { requestVisible() }
            }
        }
    }

    override fun post(action: () -> Unit) {
        recyclerView.post(action)
    }

    override fun postDelayed(delayMs: Long, action: () -> Unit) {
        recyclerView.postDelayed({ action() }, delayMs)
    }

    override fun tryFocusTextBlockByAdapterPos(adapterPos: Int, selection: Int): Boolean {
        val vh = recyclerView.findViewHolderForAdapterPosition(adapterPos) ?: return false
        val editText = (vh as? TextBlockViewHolder)?.blockView?.editText ?: return false
        val safeSelection = selection
            .coerceAtLeast(0)
            .coerceAtMost(editText.text?.length ?: 0)
        editText.requestFocus()
        editText.setSelection(safeSelection)
        return true
    }

    override fun tryFocusCodeBlockByAdapterPos(adapterPos: Int): Boolean {
        val vh = recyclerView.findViewHolderForAdapterPosition(adapterPos) ?: return false
        val codeVh = vh as? CodeBlockViewHolder ?: return false
        codeVh.requestFocusForEdit()
        return true
    }

    override fun requestFocusTextBlockByAdapterPos(adapterPos: Int): Boolean {
        val vh = recyclerView.findViewHolderForAdapterPosition(adapterPos) ?: return false
        val editText = (vh as? TextBlockViewHolder)?.blockView?.editText ?: return false
        editText.requestFocus()
        return true
    }

    override fun setSelectedHrPosition(position: Int) {
        adapter.selectedHrPosition = position
    }

    override fun setSelectedCodePosition(position: Int) {
        adapter.selectedCodePosition = position
    }

    override fun setSelectedLinkCardPosition(position: Int) {
        adapter.selectedLinkCardPosition = position
    }

    override fun setSelectedVideoBlockId(blockId: String?) {
        adapter.selectedVideoBlockId = blockId
    }

    override fun requestFocusImageCaptionByAdapterPos(adapterPos: Int): Boolean {
        val vh = recyclerView.findViewHolderForAdapterPosition(adapterPos) ?: return false
        val imageVh = vh as? ImageBlockViewHolder ?: return false
        val captionEt = imageVh.itemView.findViewById<EditText>(R.id.etCaption)
        captionEt.requestFocus()
        return true
    }

    override fun requestFocusImageCaptionEndByAdapterPos(adapterPos: Int): Boolean {
        val vh = recyclerView.findViewHolderForAdapterPosition(adapterPos) ?: return false
        val imageVh = vh as? ImageBlockViewHolder ?: return false
        val captionEt = imageVh.itemView.findViewById<EditText>(R.id.etCaption)
        captionEt.isEnabled = true
        captionEt.isFocusableInTouchMode = true
        captionEt.isFocusable = true
        captionEt.isCursorVisible = true
        captionEt.requestFocus()
        val end = captionEt.text?.length ?: 0
        captionEt.setSelection(end)
        return true
    }

    override fun notifyContentChanged() {
        notifyContentChangedDelegate.invoke()
    }
}

