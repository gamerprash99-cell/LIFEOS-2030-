package com.lifeos.app.ui.tasks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.TaskEntity
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.ui.components.*
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId

class TasksViewModel(private val taskRepository: TaskRepository) : ViewModel() {
    private val today = DateTimeUtils.today().toEpochDay()
    val tasksToday: StateFlow<List<TaskEntity>> = taskRepository.observeForDay(today).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val overdue: StateFlow<List<TaskEntity>> = taskRepository.observeOverdue(today).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun toggleTask(id: String, completed: Boolean) = viewModelScope.launch { taskRepository.setCompleted(id, completed) }
    fun addQuickTask(title: String, reminderTime: LocalTime?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val reminderMillis = reminderTime?.let { DateTimeUtils.epochDayToLocalDate(today).atTime(it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
            taskRepository.createTask(title = title, dueDateEpochDay = today, reminderEpochMillis = reminderMillis)
        }
    }
    fun keepForTomorrow(id: String) = viewModelScope.launch { taskRepository.keepForTomorrow(id, today) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen() {
    val locator = LocalServiceLocator.current
    val viewModel: TasksViewModel = viewModel(factory = LambdaViewModelFactory { TasksViewModel(locator.taskRepository) })
    val today by viewModel.tasksToday.collectAsState()
    val overdue by viewModel.overdue.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var newTaskText by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf<LocalTime?>(null) }

    val incomplete = today.filterNot { it.isCompleted }
    val completed = today.filter { it.isCompleted }
    val progress = if (today.isEmpty()) 0f else completed.size.toFloat() / today.size

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = LifeOSSpacing.extendedFabContentClearance),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Tasks", style = MaterialTheme.typography.headlineLarge)
                Text("Plan today. Finish what matters.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            LifeOSCard {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) } }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (today.isEmpty()) "A clean slate" else "Today's rhythm", style = MaterialTheme.typography.titleLarge)
                            Text(if (today.isEmpty()) "Add a task when you're ready." else "${completed.size} of ${today.size} complete", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    LifeOSProgress(progress)
                }
            }
        }
        if (overdue.isNotEmpty()) {
            item { SectionHeader("Overdue", MaterialTheme.colorScheme.error) }
            items(overdue, key = { "overdue-${it.id}" }) { task -> TaskRow(task, onToggle = { viewModel.toggleTask(task.id, it) }, onKeepForTomorrow = { viewModel.keepForTomorrow(task.id) }) }
        }
        item { SectionHeader("Today") }
        if (incomplete.isEmpty() && completed.isEmpty()) {
            item { LifeOSEmptyState("No tasks today", "Tap + to add your first task.", icon = Icons.Filled.CheckCircle) }
        } else if (incomplete.isEmpty()) {
            item { LifeOSCard { Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) { LifeOSIconBadge(Icons.Filled.Celebration); Spacer(Modifier.width(12.dp)); Text("Everything is done. Nice work!", style = MaterialTheme.typography.titleMedium) } } }
        }
        items(incomplete, key = { it.id }) { task -> TaskRow(task, onToggle = { viewModel.toggleTask(task.id, it) }, onKeepForTomorrow = { viewModel.keepForTomorrow(task.id) }) }
        if (completed.isNotEmpty()) {
            item { SectionHeader("Completed") }
            items(completed, key = { "completed-${it.id}" }) { task -> TaskRow(task, onToggle = { viewModel.toggleTask(task.id, it) }, onKeepForTomorrow = { viewModel.keepForTomorrow(task.id) }) }
        }
    }

    FloatingActionButton(onClick = { showAddDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp)) { Icon(Icons.Filled.Add, "Add task") }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New task") },
            text = {
                Column(Modifier.fillMaxWidth().imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(newTaskText, { newTaskText = it }, Modifier.fillMaxWidth(), placeholder = { Text("What needs to get done?") }, label = { Text("Task") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                    OutlinedButton(onClick = { showTimePicker = true }, Modifier.fillMaxWidth()) { Icon(Icons.Filled.Schedule, null); Spacer(Modifier.width(8.dp)); Text(reminderTime?.let { "Reminder · ${DateTimeUtils.formatMinutes(it.hour * 60 + it.minute)}" } ?: "Add reminder") }
                }
            },
            confirmButton = { TextButton(enabled = newTaskText.isNotBlank(), onClick = { viewModel.addQuickTask(newTaskText, reminderTime); newTaskText = ""; reminderTime = null; showAddDialog = false }) { Text("Add task") } },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
    if (showTimePicker) {
        ReminderTimePickerDialog(onDismiss = { showTimePicker = false }, onConfirm = { time -> reminderTime = time; showTimePicker = false })
    }
}

@Composable
private fun SectionHeader(text: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = color)
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(Modifier.weight(1f), color = color.copy(alpha = .16f))
    }
}

@Composable
private fun TaskRow(task: TaskEntity, onToggle: (Boolean) -> Unit, onKeepForTomorrow: () -> Unit) {
    val checkScale by animateFloatAsState(if (task.isCompleted) 1.08f else 1f, label = "task_check_${task.id}")
    val titleColor by animateColorAsState(if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, label = "task_color_${task.id}")

    LifeOSCard(modifier = Modifier.animateContentSize(), onClick = { onToggle(!task.isCompleted) }) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = { onToggle(!task.isCompleted) }, shape = CircleShape, color = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = .62f), modifier = Modifier.size(42.dp).graphicsLayer { scaleX = checkScale; scaleY = checkScale }) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(if (task.isCompleted) Icons.Filled.Check else Icons.Filled.Circle, if (task.isCompleted) "Completed" else "Incomplete", tint = if (task.isCompleted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, modifier = Modifier.size(if (task.isCompleted) 22.dp else 10.dp)) }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium, color = titleColor, textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None)
                    dueDateLabel(task)?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                task.description?.takeIf { it.isNotBlank() }?.let { Text("…", style = MaterialTheme.typography.titleLarge) }
            }
            AnimatedContent(targetState = task.isCompleted, transitionSpec = { fadeIn() + scaleIn() togetherWith fadeOut() }, label = "task_status_${task.id}") { done ->
                if (!done) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        AssistChip(onClick = onKeepForTomorrow, label = { Text("Move to tomorrow") }, leadingIcon = { Icon(Icons.Filled.ArrowForward, null, Modifier.size(16.dp)) })
                    }
                } else {
                    Text("Completed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private fun dueDateLabel(task: TaskEntity): String? {
    val dueDay = task.dueDateEpochDay ?: return null
    val todayEpochDay = DateTimeUtils.today().toEpochDay()
    val dayLabel = when (dueDay) {
        todayEpochDay -> "Today"
        todayEpochDay + 1 -> "Tomorrow"
        else -> DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(dueDay))
    }
    val timeLabel = task.dueTimeMinutes?.let(DateTimeUtils::formatMinutes)
    return if (timeLabel != null) "$dayLabel · $timeLabel" else dayLabel
}
