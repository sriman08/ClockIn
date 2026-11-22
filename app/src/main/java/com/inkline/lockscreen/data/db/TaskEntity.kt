package com.inkline.lockscreen.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

/**
 * Stores one primary daily task/reminder per calendar date ("YYYY-MM-DD").
 * Preserves DayDot's clean local-date persistence across restarts and week rollovers.
 */
@Entity(tableName = "daily_tasks")
data class TaskEntity(
    @PrimaryKey
    val dateIso: String,
    val title: String,
    val description: String = "",
    val isImportant: Boolean = false,
    val notificationTimeMinutes: Int? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun localDate(): LocalDate = LocalDate.parse(dateIso)

    fun notificationLocalTime(): LocalTime? =
        notificationTimeMinutes?.let { LocalTime.of(it / 60, it % 60) }
}
