package com.zelretch.aniiiiiict.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_images")
data class WorkImage(
    @PrimaryKey
    @ColumnInfo(name = "work_id")
    val workId: Long,
    val imageUrl: String,
    val localPath: String
) 