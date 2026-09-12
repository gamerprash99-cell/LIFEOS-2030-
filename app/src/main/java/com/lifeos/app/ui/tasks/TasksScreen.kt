package com.lifeos.app.ui.tasks

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
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
import com.lifeos.app.ui.theme.*
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
    val vm: TasksViewModel = viewModel(factory = LambdaViewModelFactory { TasksViewModel(locator.taskRepository) })
    val today by vm.tasksToday.collectAsState()
    val overdue by vm.overdue.collectAsState()
    val incomplete = today.filterNot { it.isCompleted }
    val completed = today.filter { it.isCompleted }
    val progress = if (today.isEmpty()) 0f else completed.size.toFloat() / today.size
    var showAdd by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var reminder by remember { mutableStateOf<LocalTime?>(null) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Pill("🌸 DAILY FLOW & FOCUS")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tasks", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Avatar("🐰")
                    }
                    Text("Plan today. Finish what matters.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                LifeOSCard {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(LifeOSPrimaryGradient)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (today.isEmpty()) "A clean slate 🌱" else "Today's rhythm ✨", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(if (today.isEmpty()) "Add a task when you're ready." else "${completed.size} done · ${incomplete.size} pending", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(shape = RoundedCornerShape(14.dp), color = LifeOSLavender.copy(alpha = .65f)) {
                                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = LifeOSPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp))
                            }
                        }
                        LifeOSProgress(progress)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatPill("● ${completed.size} done")
                            StatPill("● ${incomplete.size} pending")
                            Spacer(Modifier.weight(1f))
                            Text(if (today.isEmpty()) "Ready for magic ✨" else "Keep going 🌿", color = LifeOSPrimary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            if (overdue.isNotEmpty()) {
                item { Text("Overdue", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp)) }
                items(overdue, key = { "overdue-${it.id}" }) { task -> TaskRow(task, vm) }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Today", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Surface(shape = CircleShape, color = LifeOSLavender.copy(alpha = .65f)) { Text("${today.size}", color = LifeOSPrimary, modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp), fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, LifeOSLavender)) {
                        Text("▣ ${java.time.LocalDate.now().let { DateTimeUtils.formatFullDate(it) }}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
                    }
                }
            }
            if (today.isEmpty()) {
                item {
                    LifeOSCard {
                        Column(Modifier.fillMaxWidth().padding(34.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(92.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(LifeOSPrimaryGradient)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(52.dp)) }
                            Text("No tasks today ✨", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Tap + to add your first task and conquer the day with joy!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            HorizontalDivider(color = LifeOSLavender.copy(alpha = .55f))
                            Text("QUICK SUGGESTIONS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Suggestion("🍵 Morning matcha")
                            Suggestion("🎯 Review weekly goal")
                        }
                    }
                }
            } else {
                items(incomplete, key = { it.id }) { task -> TaskRow(task, vm) }
                if (completed.isNotEmpty()) {
                    item { Text("Completed", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                    items(completed, key = { "completed-${it.id}" }) { task -> TaskRow(task, vm) }
                }
            }
        }
        FloatingActionButton(onClick = { showAdd = true }, containerColor = LifeOSPrimary, contentColor = Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 22.dp, bottom = 18.dp)) { Icon(Icons.Filled.Add, "Add task", modifier = Modifier.size(30.dp)) }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("New task") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Task") }, placeholder = { Text("What matters today?") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                OutlinedButton(onClick = { showTime = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Schedule, null); Spacer(Modifier.width(8.dp)); Text(reminder?.let { "Reminder · ${DateTimeUtils.formatMinutes(it.hour * 60 + it.minute)}" } ?: "Add reminder") }
            } },
            confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { vm.addQuickTask(title, reminder); title = ""; reminder = null; showAdd = false }) { Text("Add task") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
    if (showTime) ReminderTimePickerDialog(onDismiss = { showTime = false }, onConfirm = { reminder = it; showTime = false })
}

@Composable private fun Pill(text: String) = Surface(shape = RoundedCornerShape(999.dp), color = LifeOSLavender.copy(alpha = .42f), border = androidx.compose.foundation.BorderStroke(1.dp, LifeOSLavender)) { Text(text, color = LifeOSPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) }
@Composable private fun Avatar(text: String) = Surface(shape = CircleShape, color = Color.White, border = androidx.compose.foundation.BorderStroke(2.dp, LifeOSLavender), modifier = Modifier.size(54.dp)) { Box(contentAlignment = Alignment.Center) { Text(text, style = MaterialTheme.typography.headlineSmall) } }
@Composable private fun StatPill(text: String) = Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)) { Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) }
@Composable private fun Suggestion(text: String) = Surface(shape = RoundedCornerShape(999.dp), color = LifeOSVioletSoft, border = androidx.compose.foundation.BorderStroke(1.dp, LifeOSLavender)) { Text(text, color = LifeOSPrimary, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), fontWeight = FontWeight.SemiBold) }

@Composable private fun TaskRow(task: TaskEntity, vm: TasksViewModel) {
    val scale by animateFloatAsState(if (task.isCompleted) 1.04f else 1f, label = "task_${task.id}")
    LifeOSCard(modifier = Modifier.animateContentSize(), onClick = { vm.toggleTask(task.id, !task.isCompleted) }) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = { vm.toggleTask(task.id, !task.isCompleted) }, shape = RoundedCornerShape(16.dp), color = if (task.isCompleted) LifeOSPrimary else LifeOSVioletSoft, modifier = Modifier.size(50.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(if (task.isCompleted) Icons.Filled.Check else Icons.Filled.Circle, null, tint = if (task.isCompleted) Color.White else LifeOSPrimary, modifier = Modifier.size(if (task.isCompleted) 28.dp else 11.dp)) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None, color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                dueDateLabel(task)?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (!task.isCompleted) Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun dueDateLabel(task: TaskEntity): String? {
    val dueDay = task.dueDateEpochDay ?: return null
    val today = DateTimeUtils.today().toEpochDay()
    val day = when (dueDay) { today -> "Today"; today + 1 -> "Tomorrow"; else -> DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(dueDay)) }
    val time = task.dueTimeMinutes?.let(DateTimeUtils::formatMinutes)
    return if (time != null) "$day · $time" else day
}
