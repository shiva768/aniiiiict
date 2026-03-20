package com.zelretch.aniiiiict.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LibraryEntryEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryEntryDao(): LibraryEntryDao
}
