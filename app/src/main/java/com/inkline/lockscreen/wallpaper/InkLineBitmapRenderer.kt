package com.inkline.lockscreen.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.inkline.lockscreen.data.db.TaskEntity
import com.inkline.lockscreen.data.prefs.InkFontStyle
import com.inkline.lockscreen.data.prefs.LockScreenTheme
import com.inkline.lockscreen.data.prefs.ThemeSettings
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Renders the InkLine (ClockIn) lock-screen wallpaper inspired strictly by
 * Reference Image 2:
 *  - Pure black (#000000) background with off-white (#FAF9F6) hand-drawn typography.
 *  - Thin decorative architectural lines colored by the selected theme (Default Off-White,
 *    Yellow, Pink, Green, Lime, Purple, or Grey).
 *  - ONLY two primary elements in their exact relative positions:
 *      1. DAY (upper-right with thin horizontal callout pointer line extending left).
 *      2. TASK OF THE DAY (replacing WEATHER, centered below the thin open-top bracket tray).
 *  - Strictly excludes dots, circles, weather, battery, network, unlock text, and extra clocks.
 */
object InkLineBitmapRenderer {

    private val DAY_NAME_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

    private val DATE_SHORT_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    fun render(
        widthPx: Int,
        heightPx: Int,
        density: Float,
        today: LocalDate,
        todayTask: TaskEntity?,
        settings: ThemeSettings
    ): Bitmap {
        val w = widthPx.coerceAtLeast(320)
        val h = heightPx.coerceAtLeast(560)
        val d = density.coerceAtLeast(0.75f)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Pure black minimalist canvas
        canvas.drawColor(settings.backgroundColor.toInt())

        val textColor = settings.textColor.toInt()
        val themeAccentColor = settings.theme.accentHex.toInt()
        val lineColor = settings.theme.lineHex.toInt()
        val mutedTextColor = applyAlpha(textColor, 0.68f)

        val handTypeface = when (settings.fontStyle) {
            InkFontStyle.HAND_DRAWN -> Typeface.create("casual", Typeface.NORMAL)
            InkFontStyle.SERIF -> Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            InkFontStyle.MONO -> Typeface.MONOSPACE
        }
        val handBoldTypeface = Typeface.create(handTypeface, Typeface.BOLD)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = (1.6f * d).coerceAtLeast(1.8f)
            strokeCap = Paint.Cap.SQUARE
            strokeJoin = Paint.Join.MITER
        }

        // Shift both elements together as a locked unit so relative geometry never breaks
        val shiftY = h * settings.verticalShiftPercent.coerceIn(-0.06f, 0.34f)

        // -------------------------------------------------------------------------
        // A. DAY (Upper-right, matching Reference Image 2: y ≈ 23.8%, x ≈ 67%..94%)
        // -------------------------------------------------------------------------
        val dayLineY = (h * 0.238f) + shiftY
        val dayPointerStartX = w * 0.675f
        val dayPointerEndX = w * 0.805f

        // Thin horizontal pointer line extending left from DAY
        canvas.drawLine(dayPointerStartX, dayLineY, dayPointerEndX, dayLineY, linePaint)

        val dayLabel = today.format(DAY_NAME_FORMATTER).uppercase(Locale.getDefault())
        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (settings.theme == LockScreenTheme.CLASSIC) textColor else themeAccentColor
            textSize = 21f * d
            typeface = handBoldTypeface
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.06f
        }
        val dayTextX = dayPointerEndX + (9f * d)
        val dayBaselineY = dayLineY + (7f * d)
        canvas.drawText(dayLabel, dayTextX, dayBaselineY, dayPaint)

        if (settings.showDateNumber) {
            val dateSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedTextColor
                textSize = 11.5f * d
                typeface = handTypeface
                textAlign = Paint.Align.LEFT
                letterSpacing = 0.08f
            }
            val dateText = today.format(DATE_SHORT_FORMATTER).uppercase(Locale.getDefault())
            canvas.drawText(dateText, dayTextX, dayBaselineY + (16f * d), dateSubPaint)
        }

        // -------------------------------------------------------------------------
        // B. TASK OF THE DAY (Replacing WEATHER in Reference Image 2)
        // Thin open-top horizontal bracket tray: y ≈ 25.4%..28.6%, x ≈ 26.5%..72.0%
        // -------------------------------------------------------------------------
        val trayLeftX = w * 0.265f
        val trayRightX = w * 0.720f
        val trayTopLeftY = (h * 0.256f) + shiftY
        val trayTopRightY = (h * 0.251f) + shiftY
        val trayBottomY = (h * 0.286f) + shiftY
        val leftLipEndX = trayLeftX + (w * 0.032f)
        val rightLipEndX = trayRightX - (w * 0.072f)

        val bracketPath = Path().apply {
            moveTo(leftLipEndX, trayTopLeftY)
            lineTo(trayLeftX, trayTopLeftY)
            lineTo(trayLeftX, trayBottomY)
            lineTo(trayRightX, trayBottomY)
            lineTo(trayRightX, trayTopRightY)
            lineTo(rightLipEndX, trayTopRightY)
        }
        canvas.drawPath(bracketPath, linePaint)

        // Centered content region directly below the bracket tray
        val taskMarginX = w * 0.14f
        val taskContentWidth = (w - taskMarginX * 2f).toInt().coerceAtLeast(180)
        var cursorY = trayBottomY + (14f * d)

        // Hand-drawn "TASK OF THE DAY" heading (directly in WEATHER's spot)
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (todayTask?.isImportant == true && settings.theme != LockScreenTheme.CLASSIC) {
                themeAccentColor
            } else {
                mutedTextColor
            }
            textSize = 12.5f * d
            typeface = handTypeface
            letterSpacing = 0.12f
        }
        val headerText = if (todayTask?.isImportant == true) {
            "TASK OF THE DAY · PRIORITY"
        } else {
            "TASK OF THE DAY"
        }
        val headerLayout = StaticLayout.Builder
            .obtain(headerText, 0, headerText.length, headerPaint, taskContentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(1)
            .build()

        canvas.save()
        canvas.translate(taskMarginX, cursorY)
        headerLayout.draw(canvas)
        canvas.restore()
        cursorY += headerLayout.height + (10f * d)

        // Real task title from Room database (never hardcoded demo tasks)
        val hasTask = todayTask != null && todayTask.title.isNotBlank()
        val displayTitle = if (hasTask) todayTask!!.title else "No task today"
        val taskSizeSp = settings.taskFontSizeSp.coerceIn(15f, 34f)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (hasTask) textColor else applyAlpha(textColor, 0.48f)
            textSize = taskSizeSp * d
            typeface = if (hasTask) handBoldTypeface else handTypeface
        }
        val titleLayout = StaticLayout.Builder
            .obtain(displayTitle, 0, displayTitle.length, titlePaint, taskContentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(4f * d, 1.0f)
            .setMaxLines(3)
            .build()

        canvas.save()
        canvas.translate(taskMarginX, cursorY)
        titleLayout.draw(canvas)
        canvas.restore()
        cursorY += titleLayout.height

        // Optional short note below task title
        val shortNote = todayTask?.description?.takeIf { it.isNotBlank() }
        if (settings.showShortNote && shortNote != null) {
            cursorY += 8f * d
            val notePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedTextColor
                textSize = (taskSizeSp * 0.62f).coerceAtLeast(11.5f) * d
                typeface = handTypeface
            }
            val noteLayout = StaticLayout.Builder
                .obtain(shortNote, 0, shortNote.length, notePaint, taskContentWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(3f * d, 1.0f)
                .setMaxLines(2)
                .build()

            canvas.save()
            canvas.translate(taskMarginX, cursorY)
            noteLayout.draw(canvas)
            canvas.restore()
        }

        return bitmap
    }

    private fun applyAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
