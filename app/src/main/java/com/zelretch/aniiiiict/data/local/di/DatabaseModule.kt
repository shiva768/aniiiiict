package com.zelretch.aniiiiict.data.local.di

import android.content.Context
import androidx.room.Room
import com.zelretch.aniiiiict.data.local.AppDatabase
import com.zelretch.aniiiiict.data.local.LibraryEntryDao
import com.zelretch.aniiiiict.data.local.WorkPriorityDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "aniiiiict_db")
            .addMigrations(AppDatabase.MIGRATION_2_3)
            // Migration を用意していない古い版（v1）だけは作り直す（中身は Annict から再同期できるキャッシュのみ）
            .fallbackToDestructiveMigrationFrom(true, 1)
            .build()

    @Provides
    @Singleton
    fun provideLibraryEntryDao(db: AppDatabase): LibraryEntryDao = db.libraryEntryDao()

    @Provides
    @Singleton
    fun provideWorkPriorityDao(db: AppDatabase): WorkPriorityDao = db.workPriorityDao()
}
