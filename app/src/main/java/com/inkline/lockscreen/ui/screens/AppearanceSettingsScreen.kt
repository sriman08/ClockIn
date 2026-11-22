package com.inkline.lockscreen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkline.lockscreen.data.prefs.InkFontStyle
import com.inkline.lockscreen.data.prefs.LockScreenTheme
import com.inkline.lockscreen.data.prefs.ThemeSettings
import com.inkline.lockscreen.ui.components.InkLinePreviewCard
import com.inkline.lockscreen.ui.viewmodel.InkLineUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceSettingsScreen(
    uiState: InkLineUiState,
    onUpdateSettings: (ThemeSettings, Boolean) -> Unit
) {
    if (!uiState.isSettingsLoaded) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val persisted = uiState.settings
    var dragTaskFontSize by remember { mutableStateOf<Float?>(null) }
    var dragVerticalShift by remember { mutableStateOf<Float?>(null) }

    val effective = persisted.copy(
        taskFontSizeSp = dragTaskFontSize ?: persisted.taskFontSizeSp,
        verticalShiftPercent = dragVerticalShift ?: persisted.verticalShiftPercent
    )

    val hasTodayNote = !uiState.todayTask?.description.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Live 9:19.5 preview powered directly by InkLineBitmapRenderer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(248.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InkLinePreviewCard(
                today = uiState.today,
                todayTask = uiState.todayTask,
                settings = effective,
                modifier = Modifier
                    .height(248.dp)
                    .aspectRatio(9f / 19.5f)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "LIVE PREVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.4.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Theme: ${effective.theme.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(effective.theme.accentHex),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Style: ${effective.fontStyle.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Text: ${effective.taskFontSizeSp.toInt()}sp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onUpdateSettings(effective, true) },
                    enabled = !uiState.isApplyingWallpaper,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (uiState.isApplyingWallpaper) "Applying..." else "Apply Now")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SectionLabel("LOCK-SCREEN THEMES")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LockScreenTheme.entries.forEach { themeOption ->
                    val isSelected = effective.theme == themeOption
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(themeOption.accentHex) else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onUpdateSettings(effective.copy(theme = themeOption), false)
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(themeOption.accentHex))
                        )
                        Text(
                            text = themeOption.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            SectionLabel("TYPOGRAPHY")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InkFontStyle.entries.forEach { style ->
                    FilterChip(
                        selected = effective.fontStyle == style,
                        onClick = { onUpdateSettings(effective.copy(fontStyle = style), false) },
                        label = { Text(style.label) }
                    )
                }
            }

            LabeledSlider(
                label = "Task text size (${effective.taskFontSizeSp.toInt()} sp)",
                value = effective.taskFontSizeSp,
                range = 15f..34f,
                onValueChange = { dragTaskFontSize = it },
                onValueChangeFinished = {
                    val finalVal = dragTaskFontSize ?: effective.taskFontSizeSp
                    dragTaskFontSize = null
                    onUpdateSettings(effective.copy(taskFontSizeSp = finalVal), false)
                }
            )

            LabeledSlider(
                label = "Vertical shift (${(effective.verticalShiftPercent * 100).toInt()}% from reference)",
                value = effective.verticalShiftPercent,
                range = -0.06f..0.34f,
                onValueChange = { dragVerticalShift = it },
                onValueChangeFinished = {
                    val finalVal = dragVerticalShift ?: effective.verticalShiftPercent
                    dragVerticalShift = null
                    onUpdateSettings(effective.copy(verticalShiftPercent = finalVal), false)
                }
            )

            ToggleRow(
                title = "Show short note below task",
                subtitle = if (hasTodayNote) {
                    "Displays today's saved note under TASK OF THE DAY"
                } else {
                    "No short note on today's task yet (add one in Tasks)"
                },
                checked = effective.showShortNote,
                onCheckedChange = { onUpdateSettings(effective.copy(showShortNote = it), false) }
            )

            ToggleRow(
                title = "Show date beneath DAY",
                subtitle = "Displays day & month (e.g., 22 NOV) under the weekday",
                checked = effective.showDateNumber,
                onCheckedChange = { onUpdateSettings(effective.copy(showDateNumber = it), false) }
            )

            ToggleRow(
                title = "Auto-update lock screen",
                subtitle = "Refreshes lock screen automatically after edits and at midnight",
                checked = effective.autoApplyLockScreen,
                onCheckedChange = { onUpdateSettings(effective.copy(autoApplyLockScreen = it), false) }
            )

            Button(
                onClick = { onUpdateSettings(effective, true) },
                enabled = !uiState.isApplyingWallpaper,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (uiState.isApplyingWallpaper) "Applying..." else "Apply Theme to Lock Screen")
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.4.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = range
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
