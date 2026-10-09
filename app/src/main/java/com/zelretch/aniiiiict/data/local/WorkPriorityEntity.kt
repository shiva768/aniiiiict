package com.zelretch.aniiiiict.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 作品の優先度。library_entries はフル同期で丸ごと入れ替わるため、workId をキーに別テーブルで持つ。
 * 「無印」は行を持たない（削除する）。
 */
@Entity(tableName = "work_priorities")
data class WorkPriorityEntity(
    @PrimaryKey val workId: String,
    val priority: String
)
