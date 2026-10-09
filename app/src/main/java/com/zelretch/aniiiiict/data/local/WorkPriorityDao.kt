package com.zelretch.aniiiiict.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkPriorityDao {
    @Query("SELECT * FROM work_priorities")
    suspend fun getAll(): List<WorkPriorityEntity>

    @Query("SELECT * FROM work_priorities WHERE workId = :workId")
    suspend fun get(workId: String): WorkPriorityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WorkPriorityEntity)

    @Query("DELETE FROM work_priorities WHERE workId = :workId")
    suspend fun delete(workId: String)
}
