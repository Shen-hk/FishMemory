package com.fishmemory.app.data.local.rooms

import androidx.room.TypeConverter
import com.fishmemory.app.data.local.rooms.entity.DraftStatus

class RoomConverters {
    @TypeConverter
    fun draftStatusToString(status: DraftStatus): String = status.name

    @TypeConverter
    fun draftStatusFromString(value: String): DraftStatus = runCatching { DraftStatus.valueOf(value) }
        .getOrDefault(DraftStatus.ACTIVE)
}

