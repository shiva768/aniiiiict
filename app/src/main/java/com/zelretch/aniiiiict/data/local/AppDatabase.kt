package com.zelretch.aniiiiict.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * スキーマを変えるときは version を上げ、必ず Migration を書くこと。
 * work_priorities は端末内にしか無いデータなので、破壊的マイグレーションで消してはいけない。
 */
@Database(entities = [LibraryEntryEntity::class, WorkPriorityEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryEntryDao(): LibraryEntryDao
    abstract fun workPriorityDao(): WorkPriorityDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `work_priorities` " +
                        "(`workId` TEXT NOT NULL, `priority` TEXT NOT NULL, PRIMARY KEY(`workId`))"
                )
            }
        }
    }
}
