package com.inkline.lockscreen.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = false
)
abstract class InkLineDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: InkLineDatabase? = null

        fun getInstance(context: Context): InkLineDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    InkLineDatabase::class.java,
                    "inkline_tasks.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
