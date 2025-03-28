package com.zelretch.aniiiiiict.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zelretch.aniiiiiict.data.local.converter.DateConverters
import com.zelretch.aniiiiiict.data.local.dao.CustomStartDateDao
import com.zelretch.aniiiiiict.data.local.dao.WorkImageDao
import com.zelretch.aniiiiiict.data.local.entity.CustomStartDate
import com.zelretch.aniiiiiict.data.local.entity.WorkImage

@Database(
    entities = [
        CustomStartDate::class,
        WorkImage::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customStartDateDao(): CustomStartDateDao
    abstract fun workImageDao(): WorkImageDao
} 