package com.zelretch.aniiiiiict.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zelretch.aniiiiiict.data.local.entity.CustomStartDate
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomStartDateDao {
    @Query("SELECT * FROM custom_start_dates WHERE workId = :workId")
    fun getCustomStartDate(workId: Long): Flow<CustomStartDate?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCustomStartDate(customStartDate: CustomStartDate)

    @Query("DELETE FROM custom_start_dates WHERE workId = :workId")
    suspend fun deleteCustomStartDate(workId: Long)
} 