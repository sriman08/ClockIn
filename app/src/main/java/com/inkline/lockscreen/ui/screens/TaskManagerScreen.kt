package com.inkline.lockscreen.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.inkline.lockscreen.data.db.TaskEntity
import com.inkline.lockscreen.ui.viewmodel.InkLineUiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ROW_DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())
private val NOTIFY_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

@Composable
fun TaskManagerScreen(
    uiState: InkLineUiState,
    initialEditDate: LocalDate?,
    onClearInitialEditDate: () -> Unit,
    onSaveTask: (
        date: LocalDate,
        title: String,
        description: String,
        isImportant: Boolean,
        notificationTimeMinutes: Int?
    ) -> Unit,
    onDeleteTask: (LocalDate) -> Unit
) {
    var editingDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(initialEditDate) {
        if (initialEditDate != null) {
            editingDate = initialEditDate
            onClearInitialEditDate()
        }
    }

    val upcoming = uiState.allTasks.filter { !it.localDate().isBefore(uiState.today) }
    val past = uiState.allTasks
        .filter { it.localDate().isBefore(uiState.today) }
        .sortedByDescending { it.dateIso }
    val accentColor = Color(uiState.settings.theme.accentHex)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tasks",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Schedule one Task of the Day per date. Past tasks remain archived.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }

            if (upcoming.isEmpty() && past.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No tasks scheduled",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Tap + to add your Task of the Day for today or any upcoming date.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (upcoming.isNotEmpty()) {
                item {
                    Text(
                        text = "TODAY & UPCOMING",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.4.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(upcoming, key = { it.dateIso }) { item ->
                    TaskRowCard(
                        task = item,
                        isToday = item.localDate().isEqual(uiState.today),
                        isPast = false,
                        accentColor = accentColor,
                        onEdit = { editingDate = item.localDate() },
                        onDelete = { onDeleteTask(item.localDate()) }
                    )
                }
            }

            if (past.isNotEmpty()) {
                item {
                    Text(
                        text = "PAST ARCHIVE",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.4.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                items(past, key = { it.dateIso }) { item ->
                    TaskRowCard(
                        task = item,
                        isToday = false,
                        isPast = true,
                        accentColor = accentColor,
                        onEdit = { editingDate = item.localDate() },
                        onDelete = { onDeleteTask(item.localDate()) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(88.dp)) }
        }

        FloatingActionButton(
            onClick = { editingDate = uiState.today },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add task")
        }
    }

    val activeDate = editingDate
    if (activeDate != null) {
        val existingByDate = uiState.allTasks.associateBy { it.dateIso }
        TaskEditorDialog(
            initialDate = activeDate,
            existingTasksByDate = existingByDate,
            onDismiss = { editingDate = null },
            onSave = { date, title, description, isImportant, notifyMinutes ->
                onSaveTask(date, title, description, isImportant, notifyMinutes)
                editingDate = null
            },
            onDelete = { date ->
                onDeleteTask(date)
                editingDate = null
            }
        )
    }
}

@Composable
private fun TaskRowCard(
    task: TaskEntity,
    isToday: Boolean,
    isPast: Boolean,
    accentColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isToday) accentColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(34.dp)
                    .background(
                        when {
                            task.isImportant -> accentColor
                            isToday -> Color(0xFFFAF9F6)
                            else -> Color(0xFF2B2B30)
                        }
                    )
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isToday) "TODAY · ${task.localDate().format(ROW_DATE_FORMAT)}"
                        else task.localDate().format(ROW_DATE_FORMAT),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.isImportant) {
                        Text(
                            text = "PRIORITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                task.notificationLocalTime()?.let { time ->
                    Text(
                        text = "Notify at ${time.format(NOTIFY_TIME_FORMAT)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete task",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorDialog(
    initialDate: LocalDate,
    existingTasksByDate: Map<String, TaskEntity>,
    onDismiss: () -> Unit,
    onSave: (LocalDate, String, String, Boolean, Int?) -> Unit,
    onDelete: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(initialDate) }
    val existingForSelectedDate = existingTasksByDate[selectedDate.toString()]

    var title by remember(selectedDate) { mutableStateOf(existingForSelectedDate?.title ?: "") }
    var description by remember(selectedDate) { mutableStateOf(existingForSelectedDate?.description ?: "") }
    var isImportant by remember(selectedDate) { mutableStateOf(existingForSelectedDate?.isImportant ?: false) }
    var notifyMinutes by remember(selectedDate) { mutableStateOf(existingForSelectedDate?.notificationTimeMinutes) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        showTimePicker = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingForSelectedDate != null) "Edit Task of the Day" else "New Task of the Day",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(selectedDate.format(ROW_DATE_FORMAT))
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 80) title = it },
                    label = { Text("Task of the day") },
                    placeholder = { Text("e.g. Finish design review") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 140) description = it },
                    label = { Text("Short note (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Priority task", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Highlights the Task of the Day label in your theme color",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isImportant,
                        onCheckedChange = { isImportant = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeLabel = notifyMinutes?.let {
                        LocalTime.of(it / 60, it % 60).format(NOTIFY_TIME_FORMAT)
                    } ?: "Off"

                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!granted) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    showTimePicker = true
                                }
                            } else {
                                showTimePicker = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (notifyMinutes != null) Icons.Outlined.NotificationsActive
                            else Icons.Outlined.NotificationsOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Notify: $timeLabel")
                    }

                    if (notifyMinutes != null) {
                        TextButton(onClick = { notifyMinutes = null }) {
                            Text("Clear")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedDate, title, description, isImportant, notifyMinutes)
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (existingForSelectedDate != null) {
                    TextButton(onClick = { onDelete(selectedDate) }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )

    if (showDatePicker) {
        val initialMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val currentHour = (notifyMinutes ?: (9 * 60)) / 60
        val currentMinute = (notifyMinutes ?: (9 * 60)) % 60
        val timePickerState = rememberTimePickerState(
            initialHour = currentHour,
            initialMinute = currentMinute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Notification Time") },
            text = {
                Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        notifyMinutes = timePickerState.hour * 60 + timePickerState.minute
                        showTimePicker = false
                    }
                ) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}
