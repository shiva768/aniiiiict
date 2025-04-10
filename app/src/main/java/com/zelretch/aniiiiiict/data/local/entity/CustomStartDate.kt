package com.zelretch.aniiiiiict.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "custom_start_dates")
data class CustomStartDate(
    @PrimaryKey
    @ColumnInfo(name = "work_id")
    val workId: Long,
    val startDate: LocalDate
) 