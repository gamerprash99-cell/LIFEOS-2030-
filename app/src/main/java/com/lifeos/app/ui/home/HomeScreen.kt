package com.lifeos.app.ui.home

import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.item
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.reminders.AlarmScheduler
import com.lifeos.app.core.util.DailyAlarm
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.core.util.rememberPermissionState
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.domain.usecase.HabitSummaryRow
import com.lifeos.app.ui.components.*
import com.lifeos.app.ui.theme.LifeOSDarkHeroGradient
import com.lifeos.app.ui.theme.LifeOSPrimaryGradient
import com.lifeos.app.ui.theme.LifeOSSoftGradient
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Responsive Home dashboard. One column on phones, adaptive cards on larger
 * screens/tablets. Data still comes from the existing use case/repositories.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    val viewModel: HomeViewModel = viewModel(
        factory = LambdaViewModelFactory {
            HomeViewModel(locator.getHomeSummaryUseCase, locator.taskRepository, locator.habitRepository)
        }
    )
    val summary by viewModel.summary.collectAsState()
    val todayCaptures by locator.captureRepository
        .observeForDay(DateTimeUtils.today().toEpochDay())
        .collectAsState(initial = emptyList())
    val morningPhotoDone = todayCaptures.any { it.type == CaptureType.PHOTO && it.caption == "Morning check-in" }
    val latestCapture = todayCaptures.maxByOrNull { it.timeMinutes }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = LifeOSSpacing.screenPadding,
                end = LifeOSSpacing.screenPadding,
                top = LifeOSSpacing.sm,
                bottom = 118.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.cardSpacing),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.cardSpacing)
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                HomeHeader(summary?.greeting ?: "Welcome", summary?.dateLabel.orEmpty(), onOpenSearch)
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                TodayOverviewCard(
                    total = summary?.tasksTotalToday ?: 0,
                    done = summary?.tasksCompletedToday ?: 0,
                    habits = summary?.habitsToday.orEmpty(),
                    spending = summary?.todaySpend ?: 0.0,
                    onClick = onOpenTasks
                )
            }

            if (!morningPhotoDone) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    MorningCheckInCard(onOpenMorningPhoto)
                }
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                HomeAlarmCard()
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                SectionHeading("Today's habits", "View all", onOpenHabits)
            }
            if (summary?.habitsToday.isNullOrEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    LifeOSEmptyState(
                        "Your rhythm starts here",
                        "Create a habit and LifeOS will keep the streak visible.",
                        icon = Icons.Filled.LocalFireDepartment
                    )
                }
            } else {
                items(summary?.habitsToday.orEmpty().take(6), key = { it.habit.id }) { row ->
                    HabitHomeCard(row)
                }
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                SectionHeading("Today's tasks", "View all", onOpenTasks)
            }
            if (summary?.tasksToday.isNullOrEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    LifeOSEmptyState(
                        "Nothing urgent",
                        "You're clear for now. Add a task when something needs your attention.",
                        icon = Icons.Filled.CheckCircle
                    )
                }
            } else {
                items(summary?.tasksToday?.take(6).orEmpty(), key = { it.id }) { task ->
                    var checked by remember(task.id, task.isCompleted) { mutableStateOf(task.isCompleted) }
                    TaskHomeRow(task.title, checked) { checked = it; viewModel.toggleTask(task.id, it) }
                }
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                QuickActionsCard(
                    onCapture = onOpenCapture,
                    onNotes = onOpenNotes,
                    onDiary = onOpenDiary,
                    onTask = onOpenTasks,
                    onHabit = onOpenHabits
                )
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                SpendingHomeCard(summary?.todaySpend ?: 0.0, onOpenExpenses)
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                SectionHeading("Latest memory", "Open Timeline", onOpenTimeline)
            }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                LatestMemoryCard(latestCapture, onOpenTimeline)
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                LifeOSAiHomeCard(onOpenAiAssistant)
            }
        }

        HomeFloatingActions(
            onCapture = onOpenCapture,
            onAi = onOpenAiAssistant
        )
    }
}

@Composable
private fun HomeHeader(greeting: String, date: String, onSearch: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(top = 4.dp, bottom = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("LifeOS", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text("Your Second Brain", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSearch, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Search, "Search your life")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(greeting, style = MaterialTheme.typography.displaySmall)
        Text(date.uppercase(Locale.getDefault()), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        LifeOSOfflinePill(Modifier.padding(top = 7.dp))
    }
}

@Composable
private fun TodayOverviewCard(total: Int, done: Int, habits: List<HabitSummaryRow>, spending: Double, onClick: () -> Unit) {
    val progress = if (total == 0) 0f else done.toFloat() / total
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "home_progress")
    GradientCard(dark = true, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TODAY'S PROGRESS", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = .68f))
                    Text("${(animated * 100).toInt()}%", style = MaterialTheme.typography.displaySmall, color = Color.White)
                    Text("$done of $total tasks completed", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .78f))
                }
                LifeOSProgressRing(progress, size = 78.dp, label = "${(animated * 100).toInt()}%")
            }
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                trackColor = Color.White.copy(alpha = .16f),
                color = Color.White
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeMetric("Tasks", "$done/$total", Icons.Filled.CheckCircle)
                HomeMetric("Habits", habits.count { it.isDone }.toString(), Icons.Filled.LocalFireDepartment)
                HomeMetric("Spending", "₹${"%.0f".format(Locale.getDefault(), spending)}", Icons.Filled.AccountBalanceWallet)
            }
        }
    }
}

@Composable
private fun RowScope.HomeMetric(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = .10f)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(18.dp), tint = Color.White.copy(alpha = .9f))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .72f))
        }
    }
}

@Composable
private fun SectionHeading(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(action) }
    }
}

@Composable
private fun HabitHomeCard(row: HabitSummaryRow) {
    LifeOSCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.habit.icon, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(row.habit.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${row.progressCount}/${row.goalCount} today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LifeOSCompletionBadge(row.isDone)
            }
            LifeOSProgress(if (row.goalCount == 0) 0f else row.progressCount.toFloat() / row.goalCount)
        }
    }
}

@Composable
private fun TaskHomeRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    LifeOSCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = onChecked)
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (checked) Text("DONE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun QuickActionsCard(
    onCapture: () -> Unit,
    onNotes: () -> Unit,
    onDiary: () -> Unit,
    onTask: () -> Unit,
    onHabit: () -> Unit
) {
    LifeOSCard {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LifeOSSectionHeader("Quick actions", supportingText = "Capture • Write • Track • Grow")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                item { QuickAction(Icons.Filled.CameraAlt, "Capture", "Photo / Video", onCapture) }
                item { QuickAction(Icons.Filled.NoteAlt, "Add Note", "Write something", onNotes) }
                item { QuickAction(Icons.Filled.EditNote, "Diary", "How are you?", onDiary) }
                item { QuickAction(Icons.Filled.AddTask, "Add Task", "Stay on track", onTask) }
                item { QuickAction(Icons.Filled.LocalFireDepartment, "Add Habit", "Build routine", onHabit) }
            }
        }
    }
}

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f), modifier = Modifier.width(118.dp)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LifeOSIconBadge(icon, Modifier.size(44.dp))
            Spacer(Modifier.height(7.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SpendingHomeCard(amount: Double, onClick: () -> Unit) {
    LifeOSCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp).clip(CircleShape).background(Brush.linearGradient(LifeOSPrimaryGradient)), contentAlignment = Alignment.Center) {
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

@Composable
private fun LatestMemoryCard(capture: com.lifeos.app.data.db.entities.CaptureEntity?, onClick: () -> Unit) {
    if (capture == null) {
        LifeOSEmptyState("Your story starts here", "Capture a thought, photo, video or audio moment.", icon = Icons.Filled.AutoAwesome)
        return
    }
    LifeOSCard(onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LifeOSIconBadge(if (capture.type == CaptureType.PHOTO) Icons.Filled.PhotoCamera else Icons.Filled.AutoAwesome, Modifier.size(42.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(captureLabel(capture.type), style = MaterialTheme.typography.titleMedium)
                    Text(DateTimeUtils.formatMinutes(capture.timeMinutes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
            }
            capture.filePath?.let { path ->
                if (capture.type == CaptureType.PHOTO) {
                    coil.compose.AsyncImage(
                        path,
                        "Latest memory",
                        Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 220.dp).clip(RoundedCornerShape(20.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }
            capture.caption?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun LifeOSAiHomeCard(onClick: () -> Unit) {
    GradientCard(modifier = Modifier.fillMaxWidth(), dark = true, onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            LifeOSAIOrb(size = 52.dp)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("LifeOS AI", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text("Offline • private • based on your local life", color = Color.White.copy(alpha = .74f), style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = Color.White)
        }
    }
}

@Composable
private fun HomeFloatingActions(onCapture: () -> Unit, onAi: () -> Unit) {
    Column(
        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SmallFloatingActionButton(
            onClick = onAi,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(50.dp)
        ) { Icon(Icons.Filled.AutoAwesome, "Ask LifeOS AI") }
        ExtendedFloatingActionButton(
            onClick = onCapture,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Capture", fontWeight = FontWeight.SemiBold) }
        )
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
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(LifeOSSoftGradient)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.WbSunny, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Morning check-in", style = MaterialTheme.typography.titleMedium)
                Text("Take a quick photo and place it in today's Timeline.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
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
    val alarms by locator.settingsStore.alarmTimes.collectAsState(initial = emptyList())
    var showTimePicker by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<DailyAlarm?>(null) }
    val scope = rememberCoroutineScope()
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS) else null
    val exactAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() else true

    fun saveAlarms(updated: List<DailyAlarm>) {
        val normalized = updated.distinctBy { it.minutesSinceMidnight }.sortedBy { it.minutesSinceMidnight }
        scope.launch {
            locator.settingsStore.setAlarmTimes(normalized)
            AlarmScheduler.replaceDaily(context, alarms, normalized)
        }
    }

    LifeOSCard {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LifeOSIconBadge(Icons.Filled.Alarm)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Math alarms", style = MaterialTheme.typography.titleMedium)
                    Text("Add as many daily alarms as you need", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = alarms.isNotEmpty(),
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (notificationPermission != null && !notificationPermission.isGranted) notificationPermission.request()
                            if (alarms.isEmpty()) {
                                val defaultAlarm = DailyAlarm(6, 0)
                                saveAlarms(listOf(defaultAlarm))
                            }
                        } else {
                            saveAlarms(emptyList())
                        }
                    }
                )
            }

            if (alarms.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .38f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AddAlarm, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("No alarms set. Turn this on or add your first alarm.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                alarms.forEach { alarm ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .30f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .12f), modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Alarm, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("Every day · math challenge", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { editingAlarm = alarm; showTimePicker = true }) { Icon(Icons.Filled.Edit, "Edit alarm") }
                            IconButton(onClick = { saveAlarms(alarms.filterNot { it.minutesSinceMidnight == alarm.minutesSinceMidnight }) }) { Icon(Icons.Filled.DeleteOutline, "Delete alarm") }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { editingAlarm = null; showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.AddAlarm, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add another alarm")
                }
            }

            if (alarms.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAllowed) {
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = android.net.Uri.parse("package:${context.packageName}") })
                }) { Text("Allow exact alarm timing") }
            }
        }
    }

    if (showTimePicker) {
        val initial = editingAlarm ?: DailyAlarm(6, 0)
        val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(if (editingAlarm == null) "Add daily math alarm" else "Edit daily math alarm") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val picked = DailyAlarm(state.hour, state.minute)
                    val updated = if (editingAlarm == null) alarms + picked else alarms.map { if (it.minutesSinceMidnight == editingAlarm!!.minutesSinceMidnight) picked else it }
                    showTimePicker = false
                    editingAlarm = null
                    if (notificationPermission != null && !notificationPermission.isGranted) notificationPermission.request()
                    saveAlarms(updated)
                }) { Text("Save alarm") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false; editingAlarm = null }) { Text("Cancel") } }
        )
    }
}
