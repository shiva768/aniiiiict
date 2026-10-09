package com.zelretch.aniiiiict.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v2（library_entries のみ）の DB から v3 へ、ライブラリを消さずに移行できることを確認する。
 * スキーマは export していないので、v2 のテーブルは Room が生成していた DDL で直接作る。
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate2To3_ライブラリが残り優先度テーブルが使える() {
        // Arrange: v2 の DB を作ってエントリーを 1 件入れておく
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(dbName), null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `library_entries` (`id` TEXT NOT NULL, `workId` TEXT NOT NULL, " +
                    "`workTitle` TEXT NOT NULL, `workMedia` TEXT, `workSeasonName` TEXT, `workSeasonYear` INTEGER, " +
                    "`workViewerStatusState` TEXT NOT NULL, `workMalAnimeId` TEXT, " +
                    "`workNoEpisodes` INTEGER NOT NULL, " +
                    "`workImageUrl` TEXT, `nextEpisodeId` TEXT, `nextEpisodeNumber` INTEGER, " +
                    "`nextEpisodeNumberText` TEXT, `nextEpisodeTitle` TEXT, `statusState` TEXT, " +
                    "`fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)"
            )
            db.execSQL(
                "INSERT INTO library_entries (id, workId, workTitle, workViewerStatusState, workNoEpisodes, " +
                    "fetchedAt) VALUES ('entry1', 'work1', 'テスト', 'WATCHING', 0, 0)"
            )
            db.version = 2
        }

        // Act: 本番と同じ設定で開く
        val database = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .build()

        // Assert
        runBlocking {
            assertEquals(listOf("entry1"), database.libraryEntryDao().getAll().map { it.id })
            database.workPriorityDao().upsert(WorkPriorityEntity("work1", "TIER1"))
            assertEquals("TIER1", database.workPriorityDao().get("work1")?.priority)
        }
        database.close()
    }
}
