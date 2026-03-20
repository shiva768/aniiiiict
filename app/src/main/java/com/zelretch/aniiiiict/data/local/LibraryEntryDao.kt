package com.zelretch.aniiiiict.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LibraryEntryDao {
    @Query("SELECT * FROM library_entries WHERE fetchParamsHash = :hash")
    suspend fun getByHash(hash: String): List<LibraryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LibraryEntryEntity>)

    @Query("DELETE FROM library_entries WHERE fetchParamsHash = :hash")
    suspend fun deleteByHash(hash: String)
}
