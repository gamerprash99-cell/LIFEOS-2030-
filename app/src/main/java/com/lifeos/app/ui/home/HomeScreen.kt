package com.lifeos.app.ui.home

import android.app.AlarmManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.ui.components.*
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.CaptureType
import kotlinx.coroutines.launch
import com.lifeos.app.core.reminders.AlarmScheduler
import com.lifeos.app.core.util.rememberPermissionState
import android.os.Build
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenTasks: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenCapture: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenNotes: () -> Unit = {},
    onOpenExpenses: () -> Unit = {},
    onOpenDiary: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenTimeline: () -> Unit = {},
    onOpenMorningPhoto: () -> Unit = {}
) {
    val locator = LocalServiceLocator.current
    val viewModel: HomeViewModel = viewModel(factory = LambdaViewModelFactory { HomeViewModel(locator.getHomeSummaryUseCase, locator.taskRepository, locator.habitRepository) })
    val summary by viewModel.summary.collectAsState()
    val todayCaptures by locator.captureRepository.observeForDay(DateTimeUtils.today().toEpochDay()).collectAsState(initial = emptyList())
    val morningPhotoDone = todayCaptures.any { it.type == CaptureType.PHOTO && it.caption == "Morning check-in" }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = LifeOSSpacing.screenPadding, top = LifeOSSpacing.screenPadding, end = LifeOSSpacing.screenPadding, bottom = LifeOSSpacing.extendedFabContentClearance),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(summary?.greeting ?: "Welcome", style = MaterialTheme.typography.displaySmall)
                    Text(summary?.dateLabel.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                LifeOSSectionHeader("Today", supportingText = "A quick view of what matters now")
                Spacer(Modifier.height(10.dp))
                val total = summary?.tasksTotalToday ?: 0
                val done = summary?.tasksCompletedToday ?: 0
                LifeOSCard {
                    Row(Modifier.fillMaxWidth().padding(LifeOSSpacing.cardPadding), verticalAlignment = Alignment.CenterVertically) {
                        LifeOSIconBadge(Icons.Filled.CheckCircle)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Tasks completed", style = MaterialTheme.typography.titleMedium)
                            Text("$done / $total", style = MaterialTheme.typography.headlineMedium)
                            LifeOSProgress(if (total == 0) 0f else done.toFloat() / total, Modifier.padding(top = 8.dp), if ((summary?.overdueTaskCount ?: 0) > 0) "${summary?.overdueTaskCount} overdue" else "You're on track today")
                        }
                    }
                }
            }
            if (!morningPhotoDone) {
                item { MorningCheckInCard(onClick = onOpenMorningPhoto) }
            }
            item {
                HomeAlarmCard()
            }
            item {
                LifeOSSectionHeader("Tasks", action = { LifeOSStatusPill("View all", onClick = onOpenTasks) })
                Spacer(Modifier.height(10.dp))
            }
            items(summary?.tasksToday?.take(5) ?: emptyList(), key = { it.id }) { task ->
                var checked by remember(task.id, task.isCompleted) { mutableStateOf(task.isCompleted) }
                LifeOSCard(modifier = Modifier.animateContentSize()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checked, onCheckedChange = { checked = it; viewModel.toggleTask(task.id, it) })
                        Text(task.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (checked) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if ((summary?.tasksToday?.isEmpty() != false)) {
                item { LifeOSEmptyState("No tasks today", "You're clear for now. Add something when you need it.", icon = Icons.Filled.CheckCircle) }
            }
            item {
                LifeOSSectionHeader("Habits", supportingText = "Keep your daily rhythm visible", action = { LifeOSStatusPill("View all", onClick = onOpenHabits) })
                Spacer(Modifier.height(10.dp))
                val habits = summary?.habitsToday ?: emptyList()
                if (habits.isEmpty()) {
                    LifeOSEmptyState("No habits yet", "Create a habit to start building a streak.", icon = Icons.Filled.LocalFireDepartment)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        habits.take(3).forEach { row ->
                            LifeOSCard {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(row.habit.icon, style = MaterialTheme.typography.headlineSmall)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(row.habit.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${row.progressCount}/${row.goalCount} today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        LifeOSProgress(if (row.goalCount == 0) 0f else row.progressCount.toFloat()/row.goalCount, Modifier.padding(top=7.dp))
                                    }
                                    Icon(if (row.isDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, tint = if (row.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            item {
                LifeOSSectionHeader("Spending")
                Spacer(Modifier.height(10.dp))
                LifeOSCard(onClick = onOpenExpenses) {
                    Row(Modifier.fillMaxWidth().padding(LifeOSSpacing.cardPadding), verticalAlignment = Alignment.CenterVertically) {
                        LifeOSIconBadge(Icons.Filled.AccountBalanceWallet)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Today's spend", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${"%.0f".format(summary?.todaySpend ?: 0.0)}", style = MaterialTheme.typography.headlineMedium)
                            Text("Tap to open Expenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ChevronRight, null)
                    }
                }
            }
            item {
                LifeOSSectionHeader("Recent Activity")
                Spacer(Modifier.height(10.dp))
                LifeOSCard(onClick = onOpenTimeline) {
                    Row(Modifier.fillMaxWidth().padding(LifeOSSpacing.cardPadding), verticalAlignment = Alignment.CenterVertically) {
                        LifeOSIconBadge(Icons.Filled.Timeline)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Your LifeOS memory", style = MaterialTheme.typography.titleMedium)
                            Text("Notes, diary, tasks, expenses and captures in time order.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ChevronRight, null)
                    }
                }
            }
            item {
                LifeOSSectionHeader("Intelligence")
                Spacer(Modifier.height(10.dp))
                LifeOSCard(onClick = onOpenAiAssistant) {
                    Row(Modifier.fillMaxWidth().padding(LifeOSSpacing.cardPadding), verticalAlignment = Alignment.CenterVertically) {
                        LifeOSIconBadge(Icons.Filled.AutoAwesome)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Ask LifeOS", style = MaterialTheme.typography.titleMedium)
                            Text("Offline summaries, trends, patterns and answers from your local data.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ChevronRight, null)
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onOpenCapture,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Capture") },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = 16.dp)
        )
    }
}


@Composable
private fun MorningCheckInCard(onClick: () -> Unit) {
    LifeOSCard(onClick = onClick, modifier = Modifier.animateContentSize()) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            LifeOSIconBadge(Icons.Filled.WbSunny)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Morning check-in", style = MaterialTheme.typography.titleMedium)
                Text("Take a quick photo and save it straight to today's Timeline.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.CameraAlt, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeAlarmCard() {
    val locator = LocalServiceLocator.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val enabled by locator.settingsStore.alarmEnabled.collectAsState(initial = false)
    val hour by locator.settingsStore.alarmHour.collectAsState(initial = 6)
    val minute by locator.settingsStore.alarmMinute.collectAsState(initial = 0)
    var showTimePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS) else null
    val exactAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() else true
    val iconScale by animateFloatAsState(if (enabled) 1.06f else 1f, label = "home_alarm_icon")

    LifeOSCard(modifier = Modifier.animateContentSize()) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                LifeOSIconBadge(Icons.Filled.Alarm, Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale })
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Morning alarm", style = MaterialTheme.typography.titleLarge)
                    Text("Math challenge alarm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        if (checked && notificationPermission != null && !notificationPermission.isGranted) notificationPermission.request()
                        scope.launch {
                            locator.settingsStore.setAlarm(checked, hour, minute)
                            if (checked) AlarmScheduler.scheduleDaily(context, hour, minute) else AlarmScheduler.cancel(context)
                        }
                    }
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(String.format(Locale.getDefault(), "%02d:%02d", hour, minute), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("Every day", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = { showTimePicker = true }) { Icon(Icons.Filled.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Change") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                    Surface(shape = CircleShape, color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f), contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(day, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = {}, label = { Text("+ / − math") }, leadingIcon = { Icon(Icons.Filled.Calculate, null, Modifier.size(16.dp)) })
                AssistChip(onClick = {}, label = { Text("Fresh each ring") }, leadingIcon = { Icon(Icons.Filled.Shuffle, null, Modifier.size(16.dp)) })
            }
            AnimatedVisibility(enabled) {
                Text("The alarm keeps sounding until you choose the correct answer. Every ring gets a new problem under 99.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAllowed) {
                TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = android.net.Uri.parse("package:${context.packageName}") }) }) {
                    Icon(Icons.Filled.Schedule, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Allow exact alarm timing")
                }
            }
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Set morning alarm") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    scope.launch {
                        locator.settingsStore.setAlarm(true, state.hour, state.minute)
                        AlarmScheduler.scheduleDaily(context, state.hour, state.minute)
                    }
                }) { Text("Set alarm") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }
}
