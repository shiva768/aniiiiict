package com.zelretch.aniiiiict.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class WorkPriorityDao {
    @Query("SELECT * FROM work_priorities")
    abstract suspend fun getAll(): List<WorkPriorityEntity>

    @Query("SELECT * FROM work_priorities WHERE workId = :workId")
    abstract suspend fun get(workId: String): WorkPriorityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(entity: WorkPriorityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(entities: List<WorkPriorityEntity>)

    @Query("DELETE FROM work_priorities WHERE workId = :workId")
    abstract suspend fun delete(workId: String)

    @Query("DELETE FROM work_priorities")
    abstract suspend fun deleteAll()

    /** 別端末から受け取った優先度で丸ごと置き換える */
    @Transaction
    open suspend fun replaceAll(entities: List<WorkPriorityEntity>) {
        deleteAll()
        insertAll(entities)
    }
}
