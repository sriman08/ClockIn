package com.inkline.lockscreen.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inkline.lockscreen.InkLineApplication
import com.inkline.lockscreen.data.db.TaskEntity
import com.inkline.lockscreen.data.prefs.ThemeSettings
import com.inkline.lockscreen.scheduling.AlarmScheduler
import com.inkline.lockscreen.wallpaper.WallpaperApplyResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class WeekTaskItem(
    val date: LocalDate,
    val task: TaskEntity?,
    val isToday: Boolean
)

data class InkLineUiState(
    val today: LocalDate = LocalDate.now(),
    val allTasks: List<TaskEntity> = emptyList(),
    val weekItems: List<WeekTaskItem> = emptyList(),
    val todayTask: TaskEntity? = null,
    val settings: ThemeSettings = ThemeSettings(),
    val isSettingsLoaded: Boolean = false,
    val isApplyingWallpaper: Boolean = false,
    val fallbackReason: String? = null
)

class InkLineViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as InkLineApplication
    private val taskRepo = app.taskRepository
    private val themeRepo = app.themeRepository
    private val wallpaperManager = app.wallpaperManager

    private val _today = MutableStateFlow(LocalDate.now())
    private val _isApplying = MutableStateFlow(false)
    private val _fallbackReason = MutableStateFlow<String?>(null)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _pendingEditDate = MutableStateFlow<LocalDate?>(null)
    val pendingEditDate: StateFlow<LocalDate?> = _pendingEditDate.asStateFlow()

    private val saveMutex = Mutex()
    private var applyJob: Job? = null
    private var debouncedAutoApplyJob: Job? = null

    private val loadedSettingsFlow = themeRepo.settingsFlow
        .map { true to it }
        .onStart { emit(false to ThemeSettings()) }

    val uiState: StateFlow<InkLineUiState> = combine(
        _today,
        taskRepo.observeAllTasks(),
        loadedSettingsFlow,
        _isApplying,
        _fallbackReason
    ) { today, allTasks, (isLoaded, settings), isApplying, fallbackReason ->
        val byDate = allTasks.associateBy { it.dateIso }
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekItems = (0L..6L).map { offset ->
            val date = monday.plusDays(offset)
            WeekTaskItem(
                date = date,
                task = byDate[date.toString()],
                isToday = date.isEqual(today)
            )
        }
        val todayTask = byDate[today.toString()]

        InkLineUiState(
            today = today,
            allTasks = allTasks,
            weekItems = weekItems,
            todayTask = todayTask,
            settings = settings,
            isSettingsLoaded = isLoaded,
            isApplyingWallpaper = isApplying,
            fallbackReason = fallbackReason
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = InkLineUiState()
    )

    fun openEditorForDate(date: LocalDate) {
        _pendingEditDate.value = date
    }

    fun consumePendingEditDate() {
        _pendingEditDate.value = null
    }

    fun refreshDateIfChanged() {
        val now = LocalDate.now()
        if (!now.isEqual(_today.value)) {
            _today.value = now
            viewModelScope.launch {
                val settings = themeRepo.getCurrentSettings()
                if (settings.autoApplyLockScreen) {
                    applyToLockScreen(silent = true)
                }
            }
        }
    }

    fun saveTask(
        date: LocalDate,
        title: String,
        description: String,
        isImportant: Boolean,
        notificationTimeMinutes: Int?
    ) {
        if (title.isBlank()) {
            _snackbarMessage.value = "Please enter a task title."
            return
        }
        viewModelScope.launch {
            val saved = taskRepo.saveTask(
                date = date,
                title = title,
                description = description,
                isImportant = isImportant,
                notificationTimeMinutes = notificationTimeMinutes
            )
            AlarmScheduler.scheduleTaskNotification(app, saved)

            val settings = themeRepo.getCurrentSettings()
            if (settings.autoApplyLockScreen) {
                applyToLockScreen(
                    silent = false,
                    customSuccess = "Task saved and lock screen updated."
                )
            } else {
                _snackbarMessage.value = "Task saved."
            }
        }
    }

    fun deleteTask(date: LocalDate) {
        viewModelScope.launch {
            taskRepo.deleteTask(date)
            AlarmScheduler.cancelTaskNotification(app, date.toString())

            val settings = themeRepo.getCurrentSettings()
            if (settings.autoApplyLockScreen) {
                applyToLockScreen(
                    silent = false,
                    customSuccess = "Task deleted and lock screen updated."
                )
            } else {
                _snackbarMessage.value = "Task deleted."
            }
        }
    }

    fun updateSettings(settings: ThemeSettings, applyImmediately: Boolean = false) {
        if (!uiState.value.isSettingsLoaded) return

        viewModelScope.launch {
            saveMutex.withLock {
                themeRepo.saveSettings(settings)
            }

            if (applyImmediately) {
                debouncedAutoApplyJob?.cancel()
                applyToLockScreen(
                    silent = false,
                    customSuccess = "Theme applied to lock screen."
                )
            } else if (settings.autoApplyLockScreen) {
                debouncedAutoApplyJob?.cancel()
                debouncedAutoApplyJob = viewModelScope.launch {
                    delay(450L)
                    applyToLockScreen(silent = true)
                }
            }
        }
    }

    fun applyToLockScreen(
        silent: Boolean = false,
        customSuccess: String = "Lock screen wallpaper updated."
    ) {
        if (applyJob?.isActive == true) return
        applyJob = viewModelScope.launch {
            _isApplying.value = true
            try {
                saveMutex.withLock { }
                val result = wallpaperManager.renderAndApplyLockScreen(_today.value)
                if (!silent) {
                    handleApplyResult(result, customSuccess = customSuccess)
                } else if (result is WallpaperApplyResult.RestrictedFallbackNeeded) {
                    _fallbackReason.value = result.reason
                }
            } finally {
                _isApplying.value = false
            }
        }
    }

    fun exportAndLaunchFallbackPicker() {
        if (applyJob?.isActive == true) return
        applyJob = viewModelScope.launch {
            _isApplying.value = true
            try {
                saveMutex.withLock { }
                val exportResult = wallpaperManager.exportLockScreenImageToGallery(_today.value)
                exportResult.onSuccess { uri ->
                    val launched = wallpaperManager.launchSystemWallpaperPicker(uri)
                    _snackbarMessage.value = if (launched) {
                        "Saved to Pictures/clock.in. Choose 'Lock screen' in the picker."
                    } else {
                        "Saved PNG to Pictures/clock.in. Open your Gallery to set it as your lock screen."
                    }
                }.onFailure { err ->
                    _snackbarMessage.value = "Could not save image: ${err.message ?: "Unknown error"}"
                }
            } finally {
                _isApplying.value = false
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun handleApplyResult(result: WallpaperApplyResult, customSuccess: String) {
        when (result) {
            WallpaperApplyResult.Success -> {
                _fallbackReason.value = null
                _snackbarMessage.value = customSuccess
            }
            is WallpaperApplyResult.RestrictedFallbackNeeded -> {
                _fallbackReason.value = result.reason
                _snackbarMessage.value = "Automatic lock-screen update is restricted on this device. Use manual apply."
            }
            is WallpaperApplyResult.Error -> {
                _snackbarMessage.value = result.message
            }
        }
    }
}
