package com.zelretch.aniiiiiict.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "custom_start_dates")
data class CustomStartDate(
    @PrimaryKey
    val workId: Long,
    val startDate: LocalDateTime
) 