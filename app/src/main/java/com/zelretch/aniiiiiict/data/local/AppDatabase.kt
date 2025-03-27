package com.zelretch.aniiiiiict.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zelretch.aniiiiiict.data.local.converter.DateTimeConverters
import com.zelretch.aniiiiiict.data.local.dao.CustomStartDateDao
import com.zelretch.aniiiiiict.data.local.entity.CustomStartDate

@Database(
    entities = [
        CustomStartDate::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateTimeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customStartDateDao(): CustomStartDateDao
} 