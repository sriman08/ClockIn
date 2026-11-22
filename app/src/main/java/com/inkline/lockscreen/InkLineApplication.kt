package com.inkline.lockscreen

import android.app.Application
import com.inkline.lockscreen.data.TaskRepository
import com.inkline.lockscreen.data.db.InkLineDatabase
import com.inkline.lockscreen.data.prefs.ThemeRepository
import com.inkline.lockscreen.scheduling.AlarmScheduler
import com.inkline.lockscreen.scheduling.TaskNotificationHelper
import com.inkline.lockscreen.wallpaper.InkLineWallpaperManager

class InkLineApplication : Application() {

    lateinit var database: InkLineDatabase
        private set

    lateinit var taskRepository: TaskRepository
        private set

    lateinit var themeRepository: ThemeRepository
        private set

    lateinit var wallpaperManager: InkLineWallpaperManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = InkLineDatabase.getInstance(this)
        taskRepository = TaskRepository(database.taskDao())
        themeRepository = ThemeRepository(this)
        wallpaperManager = InkLineWallpaperManager(
            context = this,
            taskRepository = taskRepository,
            themeRepository = themeRepository
        )
        TaskNotificationHelper.ensureChannel(this)
        AlarmScheduler.scheduleMidnightRefresh(this)
    }
}
