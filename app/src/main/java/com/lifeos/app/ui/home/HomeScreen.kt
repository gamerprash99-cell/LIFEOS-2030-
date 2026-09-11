package com.lifeos.app.ui.home

import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.reminders.AlarmScheduler
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.core.util.rememberPermissionState
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.ui.components.*
import com.lifeos.app.ui.theme.LifeOSDarkHeroGradient
import com.lifeos.app.ui.theme.LifeOSPrimaryGradient
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val latestCapture = todayCaptures.maxByOrNull { it.timeMinutes }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = LifeOSSpacing.screenPadding, vertical = LifeOSSpacing.lg, bottom = LifeOSSpacing.extendedFabContentClearance),
        verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sm)
    ) {
        item { HomeHeader(summary?.greeting ?: "Welcome", summary?.dateLabel.orEmpty(), onOpenSearch) }
        item { TodayProgressCard(summary?.tasksTotalToday ?: 0, summary?.tasksCompletedToday ?: 0, summary?.overdueTaskCount ?: 0) }

        if (!morningPhotoDone) item { MorningCheckInCard(onOpenMorningPhoto) }
        item { HomeAlarmCard() }

        stickyHeader { HomeStickyHeader("Today's Habits", "View all", onOpenHabits) }
        item {
            val habits = summary?.habitsToday.orEmpty()
            if (habits.isEmpty()) {
                LifeOSEmptyState("Your rhythm starts here", "Create a habit and LifeOS will keep the streak visible.", icon = Icons.Filled.LocalFireDepartment)
            } else {
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(habits.take(4), key = { it.habit.id }) { row ->
                        HabitHomeCard(row.habit.name, row.habit.icon, row.progressCount, row.goalCount, row.isDone)
                    }
                }
            }
        }

        stickyHeader { HomeStickyHeader("Today's Tasks", "View all", onOpenTasks) }
        if (summary?.tasksToday.isNullOrEmpty()) {
            item { LifeOSEmptyState("Nothing urgent", "You're clear for now. Add a task when something needs your attention.", icon = Icons.Filled.CheckCircle) }
        } else {
            items(summary?.tasksToday?.take(5).orEmpty(), key = { it.id }) { task ->
                var checked by remember(task.id, task.isCompleted) { mutableStateOf(task.isCompleted) }
                TaskHomeRow(task.title, checked) { checked = it; viewModel.toggleTask(task.id, it) }
            }
        }

        stickyHeader { HomeStickyHeader("Spending", "Open expenses", onOpenExpenses) }
        item { SpendingHomeCard(summary?.todaySpend ?: 0.0, onOpenExpenses) }

        stickyHeader { HomeStickyHeader("Latest Memory", "Open Timeline", onOpenTimeline) }
        item {
            if (latestCapture == null) {
                LifeOSEmptyState("Your story starts here", "Capture a thought, photo, video or audio moment.", icon = Icons.Filled.AutoAwesome)
            } else {
                LifeOSCard(onClick = onOpenTimeline) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LifeOSIconBadge(if (latestCapture.type == CaptureType.PHOTO) Icons.Filled.PhotoCamera else Icons.Filled.AutoAwesome, Modifier.size(40.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(captureLabel(latestCapture.type), style = MaterialTheme.typography.titleMedium)
                                Text(DateTimeUtils.formatMinutes(latestCapture.timeMinutes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        latestCapture.filePath?.let { path ->
                            if (latestCapture.type == CaptureType.PHOTO) {
                                coil.compose.AsyncImage(path, "Latest memory", Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(20.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                            }
                        }
                        latestCapture.caption?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                    }
                }
            }
        }

        stickyHeader { HomeStickyHeader("Intelligence", "Ask LifeOS", onOpenAiAssistant) }
        item {
            GradientCard(modifier = Modifier.fillMaxWidth(), dark = true, onClick = onOpenAiAssistant) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LifeOSAIOrb(size = 54.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("LifeOS AI", color = Color.White, style = MaterialTheme.typography.titleLarge)
                            Text("Private local insights from your own data.", color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = Color.White)
                    }
                    Text("Ask about habits, tasks, spending, diary patterns or your week.", color = Color.White.copy(alpha = .86f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    ExtendedFloatingActionButton(
        onClick = onOpenCapture,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        icon = { Icon(Icons.Filled.Add, null) },
        text = { Text("Capture", fontWeight = FontWeight.SemiBold) },
        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = 16.dp)
    )
    }
}

@Composable
private fun HomeHeader(greeting: String, date: String, onSearch: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(greeting, style = MaterialTheme.typography.displaySmall)
            Text(date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LifeOSOfflinePill(Modifier.padding(top = 4.dp))
        }
        IconButton(onClick = onSearch, modifier = Modifier.size(48.dp)) { Icon(Icons.Filled.Search, "Search your life") }
    }
}

@Composable
private fun TodayProgressCard(total: Int, done: Int, overdue: Int) {
    val progress = if (total == 0) 0f else done.toFloat() / total
    GradientCard(modifier = Modifier.fillMaxWidth(), dark = true) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TODAY'S PROGRESS", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .72f))
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.displaySmall, color = Color.White)
                Text("$done of $total tasks completed", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .78f))
                if (overdue > 0) Text("$overdue overdue", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFB4AB))
            }
            LifeOSProgressRing(progress, size = 88.dp, label = "${(progress * 100).toInt()}%")
        }
    }
}

@Composable
private fun HomeStickyHeader(title: String, action: String, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = .97f), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onClick) { Text(action) }
        }
    }
}

@Composable
private fun HabitHomeCard(name: String, icon: String, progress: Int, goal: Int, done: Boolean) {
    LifeOSCard(Modifier.width(158.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                LifeOSCompletionBadge(done)
            }
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$progress / $goal today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LifeOSProgress(if (goal == 0) 0f else progress.toFloat() / goal)
        }
    }
}

@Composable
private fun TaskHomeRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    LifeOSCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = onChecked)
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (checked) Text("DONE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SpendingHomeCard(amount: Double, onClick: () -> Unit) {
    LifeOSCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(LifeOSPrimaryGradient)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AccountBalanceWallet, null, tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Today's spending", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("₹${"%.0f".format(Locale.getDefault(), amount)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Tap to open Expenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun captureLabel(type: CaptureType) = when (type) {
    CaptureType.PHOTO -> "Photo memory"
    CaptureType.VIDEO -> "Video memory"
    CaptureType.AUDIO -> "Audio memory"
    CaptureType.THOUGHT -> "Quick thought"
}

@Composable
private fun MorningCheckInCard(onClick: () -> Unit) {
    LifeOSCard(onClick = onClick, modifier = Modifier.animateContentSize()) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(LifeOSSoftGradient)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.WbSunny, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Morning check-in", style = MaterialTheme.typography.titleMedium)
                Text("Take a quick face photo and place it in today's Timeline.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
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
    val iconScale by animateFloatAsState(if (enabled) 1.05f else 1f, label = "alarm_scale")

    LifeOSCard {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LifeOSIconBadge(Icons.Filled.Alarm, Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale })
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Math alarm", style = MaterialTheme.typography.titleMedium)
                    Text("Solve a fresh + / − problem to stop it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(enabled, onCheckedChange = { checked ->
                    if (checked && notificationPermission != null && !notificationPermission.isGranted) notificationPermission.request()
                    scope.launch {
                        locator.settingsStore.setAlarm(checked, hour, minute)
                        if (checked) AlarmScheduler.scheduleDaily(context, hour, minute) else AlarmScheduler.cancel(context)
                    }
                })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(String.format(Locale.getDefault(), "%02d:%02d", hour, minute), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Text("Every day", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = { showTimePicker = true }) { Text("Change") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("+ / − under 99") }, leadingIcon = { Icon(Icons.Filled.Calculate, null, Modifier.size(16.dp)) })
                AssistChip(onClick = {}, label = { Text("New every alarm") }, leadingIcon = { Icon(Icons.Filled.Shuffle, null, Modifier.size(16.dp)) })
            }
            AnimatedVisibility(enabled) { Text("The alarm stays active until the answer is correct.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAllowed) {
                TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = android.net.Uri.parse("package:${context.packageName}") }) }) { Text("Allow exact alarm timing") }
            }
        }
    }
    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Set daily math alarm") },
            text = { TimePicker(state = state) },
            confirmButton = { TextButton(onClick = { showTimePicker = false; scope.launch { locator.settingsStore.setAlarm(true, state.hour, state.minute); AlarmScheduler.scheduleDaily(context, state.hour, state.minute) } }) { Text("Set alarm") } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }
}
