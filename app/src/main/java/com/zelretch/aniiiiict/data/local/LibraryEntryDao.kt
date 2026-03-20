package com.zelretch.aniiiiict.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class LibraryEntryDao {
    @Query("SELECT * FROM library_entries")
    abstract suspend fun getAll(): List<LibraryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(entries: List<LibraryEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(entry: LibraryEntryEntity)

    @Query("DELETE FROM library_entries")
    abstract suspend fun deleteAll()

    @Query("DELETE FROM library_entries WHERE id = :id")
    abstract suspend fun deleteById(id: String)

    @Transaction
    open suspend fun replaceAll(entries: List<LibraryEntryEntity>) {
        deleteAll()
        insertAll(entries)
    }
}
