package com.inkline.lockscreen

import com.inkline.lockscreen.data.db.TaskEntity
import com.inkline.lockscreen.data.prefs.LockScreenTheme
import com.inkline.lockscreen.data.prefs.ThemeSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InkLineThemeAndDateTest {

    @Test
    fun allSixRequiredColorThemesPlusDefaultExist() {
        val names = LockScreenTheme.entries.map { it.name }.toSet()
        assertTrue(names.contains("CLASSIC"))
        assertTrue(names.contains("YELLOW"))
        assertTrue(names.contains("PINK"))
        assertTrue(names.contains("GREEN"))
        assertTrue(names.contains("LIME"))
        assertTrue(names.contains("PURPLE"))
        assertTrue(names.contains("GREY"))
    }

    @Test
    fun defaultThemeIsPureBlackWithOffWhiteTypography() {
        val defaults = ThemeSettings()
        assertEquals(LockScreenTheme.CLASSIC, defaults.theme)
        assertEquals(0xFF000000L, defaults.backgroundColor)
        assertEquals(0xFFFAF9F6L, defaults.textColor)
    }

    @Test
    fun taskEntityParsesExactLocalCalendarDate() {
        val task = TaskEntity(
            dateIso = "2026-11-22",
            title = "Prepare demo",
            description = "Check all 6 themes",
            isImportant = true,
            notificationTimeMinutes = 9 * 60 + 30
        )
        assertEquals(LocalDate.of(2026, 11, 22), task.localDate())
        assertEquals(9, task.notificationLocalTime()?.hour)
        assertEquals(30, task.notificationLocalTime()?.minute)
    }
}
