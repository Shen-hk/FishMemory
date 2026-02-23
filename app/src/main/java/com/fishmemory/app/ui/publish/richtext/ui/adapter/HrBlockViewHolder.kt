package com.fishmemory.app.ui.publish.richtext.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.fishmemory.app.R
import com.fishmemory.app.ui.publish.richtext.core.model.EditorBlock

class HrBlockViewHolder(
    private val root: ConstraintLayout,
    private val ivDelete: ImageView
) : RecyclerView.ViewHolder(root) {

    fun bind(
        block: EditorBlock.HrBlock,
        isSelected: Boolean,
        onClick: (position: Int) -> Unit,
        onDelete: (position: Int) -> Unit
    ) {
        root.isSelected = isSelected
        ivDelete.visibility = if (isSelected) View.VISIBLE else View.GONE

        root.setOnClickListener {
            onClick(bindingAdapterPosition)
        }
        ivDelete.setOnClickListener {
            onDelete(bindingAdapterPosition)
        }
    }

    /** 只读态：仅展示分割线，无点击/删除。 */
    fun bindReadOnly() {
        root.isSelected = false
        ivDelete.visibility = View.GONE
        root.setOnClickListener(null)
        ivDelete.setOnClickListener(null)
    }

    companion object {
        fun create(parent: ViewGroup): HrBlockViewHolder {
            val root = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_block_hr, parent, false) as ConstraintLayout
            val ivDelete: ImageView = root.findViewById(R.id.ivDelete)
            return HrBlockViewHolder(root, ivDelete)
        }
    }
}

