package com.inkline.lockscreen.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.inkline.lockscreen.InkLineApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val handledActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
        if (action !in handledActions) return

        val app = context.applicationContext as? InkLineApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = LocalDate.now()
                val settings = app.themeRepository.getCurrentSettings()
                if (settings.autoApplyLockScreen) {
                    app.wallpaperManager.renderAndApplyLockScreen(today)
                }
                AlarmScheduler.scheduleMidnightRefresh(context)
                val upcoming = app.taskRepository.getUpcomingWithNotifications(today)
                upcoming.forEach { task ->
                    AlarmScheduler.scheduleTaskNotification(context, task)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
