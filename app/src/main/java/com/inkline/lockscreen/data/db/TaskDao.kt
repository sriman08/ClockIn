package com.inkline.lockscreen.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM daily_tasks ORDER BY dateIso ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM daily_tasks WHERE dateIso = :dateIso LIMIT 1")
    suspend fun getByDate(dateIso: String): TaskEntity?

    @Query("SELECT * FROM daily_tasks WHERE dateIso >= :fromDateIso AND notificationTimeMinutes IS NOT NULL ORDER BY dateIso ASC")
    suspend fun getUpcomingWithNotifications(fromDateIso: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Query("DELETE FROM daily_tasks WHERE dateIso = :dateIso")
    suspend fun deleteByDate(dateIso: String)
}
