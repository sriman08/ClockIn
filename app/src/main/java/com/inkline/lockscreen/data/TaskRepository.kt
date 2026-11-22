package com.inkline.lockscreen.data

import com.inkline.lockscreen.data.db.TaskDao
import com.inkline.lockscreen.data.db.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TaskRepository(private val dao: TaskDao) {

    fun observeAllTasks(): Flow<List<TaskEntity>> = dao.observeAll()

    suspend fun getTaskForDate(date: LocalDate): TaskEntity? =
        dao.getByDate(date.toString())

    suspend fun getUpcomingWithNotifications(fromDate: LocalDate): List<TaskEntity> =
        dao.getUpcomingWithNotifications(fromDate.toString())

    suspend fun saveTask(
        date: LocalDate,
        title: String,
        description: String,
        isImportant: Boolean,
        notificationTimeMinutes: Int?
    ): TaskEntity {
        val entity = TaskEntity(
            dateIso = date.toString(),
            title = title.trim(),
            description = description.trim(),
            isImportant = isImportant,
            notificationTimeMinutes = notificationTimeMinutes,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        dao.upsert(entity)
        return entity
    }

    suspend fun deleteTask(date: LocalDate) {
        dao.deleteByDate(date.toString())
    }
}
