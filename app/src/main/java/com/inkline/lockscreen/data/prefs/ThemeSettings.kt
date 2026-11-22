package com.inkline.lockscreen.data.prefs

/**
 * Color palette extracted from Reference Image 1:
 * Pure Black (#000000), Off-White (#FAF9F6), plus 6 selectable accent themes:
 * Yellow, Pink, Green, Lime, Purple, and Grey.
 */
enum class LockScreenTheme(
    val label: String,
    val accentHex: Long,
    val lineHex: Long
) {
    CLASSIC("Black & White", 0xFFFAF9F6L, 0xFFFAF9F6L),
    YELLOW("Yellow", 0xFFFFD426L, 0xFFFFD426L),
    PINK("Pink", 0xFFFF4F87L, 0xFFFF4F87L),
    GREEN("Green", 0xFF23C552L, 0xFF23C552L),
    LIME("Lime", 0xFFA3E635L, 0xFFA3E635L),
    PURPLE("Purple", 0xFF8B5CF6L, 0xFF8B5CF6L),
    GREY("Grey", 0xFF9CA3AFL, 0xFF9CA3AFL)
}

enum class InkFontStyle(val label: String) {
    HAND_DRAWN("Hand-Drawn"),
    SERIF("Editorial Serif"),
    MONO("Minimal Mono")
}

data class ThemeSettings(
    val theme: LockScreenTheme = LockScreenTheme.CLASSIC,
    val backgroundColor: Long = 0xFF000000L,
    val textColor: Long = 0xFFFAF9F6L,
    val fontStyle: InkFontStyle = InkFontStyle.HAND_DRAWN,
    val taskFontSizeSp: Float = 22f,
    /**
     * Vertical shift relative to Reference Image 2's native placement (-0.06f..+0.34f).
     * 0.0f keeps DAY and TASK OF THE DAY in the exact relative position of Reference Image 2.
     */
    val verticalShiftPercent: Float = 0.0f,
    val showShortNote: Boolean = true,
    val showDateNumber: Boolean = true,
    val autoApplyLockScreen: Boolean = true
)
