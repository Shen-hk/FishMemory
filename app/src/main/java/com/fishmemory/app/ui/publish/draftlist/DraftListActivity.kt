package com.fishmemory.app.ui.publish.draftlist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fishmemory.app.data.local.rooms.AppDatabase
import com.fishmemory.app.data.local.rooms.entity.DraftStatus
import com.fishmemory.app.databinding.ActivityDraftListBinding
import com.fishmemory.app.ui.publish.draft.DraftManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DraftListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SELECTED_DRAFT_ID = "extra_selected_draft_id"

        fun createIntent(activity: Activity): Intent {
            return Intent(activity, DraftListActivity::class.java)
        }
    }

    private lateinit var binding: ActivityDraftListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDraftListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.draftDao()

        val adapter = DraftListAdapter(
            onClick = { entity ->
                setResult(
                    RESULT_OK,
                    Intent().putExtra(EXTRA_SELECTED_DRAFT_ID, entity.draftId)
                )
                finish()
            },
            onDelete = { entity ->
                lifecycleScope.launch {
                    dao.updateStatus(entity.draftId, DraftStatus.DELETED, System.currentTimeMillis())
                }
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnNewDraft.setOnClickListener {
            lifecycleScope.launch {
                val manager = DraftManager(dao)
                val snapshot = manager.createNewEmptyDraft()
                setResult(
                    RESULT_OK,
                    Intent().putExtra(EXTRA_SELECTED_DRAFT_ID, snapshot.draftId)
                )
                finish()
            }
        }

        lifecycleScope.launch {
            dao.observeRecent(
                visibleStatuses = listOf(DraftStatus.ACTIVE, DraftStatus.SAVED),
                limit = 100
            ).collectLatest { list ->
                adapter.submitList(list)
                binding.emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}

