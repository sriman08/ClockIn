package com.inkline.lockscreen.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.inkLineDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "inkline_appearance_prefs"
)

class ThemeRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("lockscreen_theme")
        val FONT_STYLE = stringPreferencesKey("ink_font_style")
        val TASK_FONT_SIZE = floatPreferencesKey("task_font_size_sp")
        val VERTICAL_SHIFT = floatPreferencesKey("vertical_shift_percent")
        val SHOW_SHORT_NOTE = booleanPreferencesKey("show_short_note")
        val SHOW_DATE_NUMBER = booleanPreferencesKey("show_date_number")
        val AUTO_APPLY = booleanPreferencesKey("auto_apply_lock_screen")
    }

    val settingsFlow: Flow<ThemeSettings> = context.inkLineDataStore.data.map(::fromPreferences)

    suspend fun getCurrentSettings(): ThemeSettings = settingsFlow.first()

    suspend fun saveSettings(settings: ThemeSettings) {
        context.inkLineDataStore.edit { prefs ->
            writePreferences(prefs, settings)
        }
    }

    private fun fromPreferences(prefs: Preferences): ThemeSettings {
        val defaults = ThemeSettings()
        return ThemeSettings(
            theme = prefs[Keys.THEME]?.let {
                runCatching { LockScreenTheme.valueOf(it) }.getOrNull()
            } ?: defaults.theme,
            fontStyle = prefs[Keys.FONT_STYLE]?.let {
                runCatching { InkFontStyle.valueOf(it) }.getOrNull()
            } ?: defaults.fontStyle,
            taskFontSizeSp = (prefs[Keys.TASK_FONT_SIZE] ?: defaults.taskFontSizeSp).coerceIn(15f, 34f),
            verticalShiftPercent = (prefs[Keys.VERTICAL_SHIFT] ?: defaults.verticalShiftPercent).coerceIn(-0.06f, 0.34f),
            showShortNote = prefs[Keys.SHOW_SHORT_NOTE] ?: defaults.showShortNote,
            showDateNumber = prefs[Keys.SHOW_DATE_NUMBER] ?: defaults.showDateNumber,
            autoApplyLockScreen = prefs[Keys.AUTO_APPLY] ?: defaults.autoApplyLockScreen
        )
    }

    private fun writePreferences(prefs: MutablePreferences, settings: ThemeSettings) {
        prefs[Keys.THEME] = settings.theme.name
        prefs[Keys.FONT_STYLE] = settings.fontStyle.name
        prefs[Keys.TASK_FONT_SIZE] = settings.taskFontSizeSp.coerceIn(15f, 34f)
        prefs[Keys.VERTICAL_SHIFT] = settings.verticalShiftPercent.coerceIn(-0.06f, 0.34f)
        prefs[Keys.SHOW_SHORT_NOTE] = settings.showShortNote
        prefs[Keys.SHOW_DATE_NUMBER] = settings.showDateNumber
        prefs[Keys.AUTO_APPLY] = settings.autoApplyLockScreen
    }
}
