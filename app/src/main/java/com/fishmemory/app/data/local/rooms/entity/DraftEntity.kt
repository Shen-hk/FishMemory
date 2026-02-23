package com.fishmemory.app.data.local.rooms.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "drafts",
    indices = [
        Index(value = ["status"]),
        Index(value = ["updated_at"]),
        Index(value = ["last_opened_at"])
    ]
)
data class DraftEntity(
    @PrimaryKey
    @ColumnInfo(name = "draft_id")
    val draftId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content_json")
    val contentJson: String,

    @ColumnInfo(name = "status")
    val status: DraftStatus,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "last_opened_at")
    val lastOpenedAt: Long,

    @ColumnInfo(name = "word_count")
    val wordCount: Int,

    @ColumnInfo(name = "preview")
    val preview: String
)

enum class DraftStatus {
    ACTIVE,
    SAVED,
    ARCHIVED,
    DELETED
}

