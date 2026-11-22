package com.inkline.lockscreen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkline.lockscreen.ui.viewmodel.InkLineUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FULL_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
private val TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

@Composable
fun DashboardScreen(
    uiState: InkLineUiState,
    onQuickEditDate: (LocalDate) -> Unit,
    onOpenLockScreenPreview: () -> Unit,
    onApplyNow: () -> Unit
) {
    val todayTask = uiState.todayTask
    val accentColor = Color(uiState.settings.theme.accentHex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = uiState.today.format(FULL_DATE_FORMAT).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "clock.in",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Today's Task of the Day Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                .clickable { onQuickEditDate(uiState.today) }
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (todayTask?.isImportant == true) {
                            "TASK OF THE DAY · PRIORITY"
                        } else {
                            "TASK OF THE DAY"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.4.sp,
                        color = accentColor
                    )
                    Icon(
                        imageVector = if (todayTask == null) Icons.Outlined.Add else Icons.Outlined.Edit,
                        contentDescription = "Edit today's task",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = todayTask?.title ?: "No task today",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = if (todayTask == null) FontWeight.Normal else FontWeight.SemiBold,
                    color = if (todayTask == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                if (!todayTask?.description.isNullOrBlank()) {
                    Text(
                        text = todayTask!!.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val notifyTime = todayTask?.notificationLocalTime()
                if (notifyTime != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Notification at ${notifyTime.format(TIME_FORMAT)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Current Week Schedule (Mon–Sun list using clean minimalist bars, zero dots/circles)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "THIS WEEK",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.4.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Theme: ${uiState.settings.theme.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                }

                uiState.weekItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (item.isToday) Color.White.copy(alpha = 0.08f)
                                else Color.Transparent
                            )
                            .clickable { onQuickEditDate(item.date) }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(18.dp)
                                .background(
                                    when {
                                        item.task?.isImportant == true -> accentColor
                                        item.isToday -> Color(0xFFFAF9F6)
                                        else -> Color(0xFF2B2B30)
                                    }
                                )
                        )
                        Text(
                            text = "${item.date.dayOfWeek.name.take(3)} ${item.date.dayOfMonth}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (item.isToday) Color(0xFFFAF9F6)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(56.dp)
                        )
                        Text(
                            text = item.task?.title ?: "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.task != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onQuickEditDate(uiState.today) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = if (todayTask == null) Icons.Outlined.Add else Icons.Outlined.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(if (todayTask == null) "Set Today" else "Edit Today")
            }

            OutlinedButton(
                onClick = onApplyNow,
                enabled = !uiState.isApplyingWallpaper,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Wallpaper,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(if (uiState.isApplyingWallpaper) "Updating..." else "Sync Lock Screen")
            }
        }

        OutlinedButton(
            onClick = onOpenLockScreenPreview,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Open Full Lock Screen Preview")
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
