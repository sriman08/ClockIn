package com.inkline.lockscreen.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.inkline.lockscreen.InkLineApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? InkLineApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    AlarmScheduler.ACTION_MIDNIGHT_REFRESH -> {
                        val settings = app.themeRepository.getCurrentSettings()
                        if (settings.autoApplyLockScreen) {
                            app.wallpaperManager.renderAndApplyLockScreen(LocalDate.now())
                        }
                        AlarmScheduler.scheduleMidnightRefresh(context)
                    }
                    AlarmScheduler.ACTION_TASK_ALARM -> {
                        val dateIso = intent.getStringExtra(AlarmScheduler.EXTRA_DATE_ISO)
                        if (!dateIso.isNullOrBlank()) {
                            val task = app.taskRepository.getTaskForDate(LocalDate.parse(dateIso))
                            if (task != null) {
                                TaskNotificationHelper.showTaskNotification(context, task)
                            }
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
