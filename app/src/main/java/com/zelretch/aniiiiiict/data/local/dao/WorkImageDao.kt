package com.zelretch.aniiiiiict.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zelretch.aniiiiiict.data.local.entity.WorkImage
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkImageDao {
    @Query("SELECT * FROM work_images WHERE work_id = :workId")
    fun getWorkImage(workId: Long): Flow<WorkImage?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkImage(workImage: WorkImage)

    @Query("DELETE FROM work_images WHERE work_id = :workId")
    suspend fun deleteWorkImage(workId: Long)
} 