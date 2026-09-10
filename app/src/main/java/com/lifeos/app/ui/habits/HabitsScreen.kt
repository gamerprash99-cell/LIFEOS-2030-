package com.lifeos.app.ui.habits

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.repository.HabitRepository
import com.lifeos.app.domain.model.HabitAnalytics
import com.lifeos.app.ui.components.*
import com.lifeos.app.ui.theme.LifeOSSpacing
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId

private val PRESET_HABIT_ICONS = listOf("🔥", "💧", "🏃", "📖", "🧘", "😴", "🥗", "✅")

class HabitsViewModel(private val habitRepository: HabitRepository) : ViewModel() {
    val habits: StateFlow<List<HabitEntity>> = habitRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _analytics = MutableStateFlow<Map<String, HabitAnalytics>>(emptyMap())
    val analytics: StateFlow<Map<String, HabitAnalytics>> = _analytics
    init { viewModelScope.launch { habits.collectLatest { current -> _analytics.value = current.associate { it.id to habitRepository.computeAnalytics(it) } } } }
    fun addHabit(name: String, icon: String, reminderTime: LocalTime?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val today = DateTimeUtils.today()
            val millis = reminderTime?.let { today.atTime(it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
            habitRepository.createHabit(name = name, icon = icon.ifBlank { "✅" }, reminderEpochMillis = millis)
        }
    }
    fun logToday(habitId: String, currentProgress: Int, goal: Int) = viewModelScope.launch {
        habitRepository.logProgress(habitId, DateTimeUtils.today().toEpochDay(), (currentProgress + 1).coerceAtMost(goal + 3))
    }
}

@Composable
fun HabitsScreen(onOpenHabit: (String) -> Unit) {
    val locator = LocalServiceLocator.current
    val vm: HabitsViewModel = viewModel(factory = LambdaViewModelFactory { HabitsViewModel(locator.habitRepository) })
    val habits by vm.habits.collectAsState()
    val analytics by vm.analytics.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🔥") }
    var reminder by remember { mutableStateOf<LocalTime?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("Habits") }) },
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }, containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Filled.Add, "New habit") } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(LifeOSSpacing.screenPadding, 0.dp, LifeOSSpacing.screenPadding, LifeOSSpacing.fabContentClearance),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LifeOSCard {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LifeOSIconBadge(Icons.Filled.LocalFireDepartment)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Your rhythm", style = MaterialTheme.typography.titleLarge)
                                Text("Today, this week and this month at a glance.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            if (habits.isEmpty()) item { LifeOSEmptyState("No habits yet", "Tap + to create your first daily habit.", icon = Icons.Filled.LocalFireDepartment) }
            items(habits, key = { it.id }) { habit ->
                HabitRow(habit, locator.habitRepository, analytics[habit.id], { onOpenHabit(habit.id) }) { current -> vm.logToday(habit.id, current, habit.goalCount) }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Create a habit") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose an icon", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PRESET_HABIT_ICONS) { preset ->
                            FilterChip(selected = preset == icon, onClick = { icon = preset }, label = { Text(preset, style = MaterialTheme.typography.titleLarge) })
                        }
                    }
                    OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Habit name") }, singleLine = true)
                    OutlinedButton(onClick = { showTime = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Notifications, null); Spacer(Modifier.width(8.dp)); Text(reminder?.let { "Reminder · $it" } ?: "Add a reminder (optional)") }
                }
            },
            confirmButton = { Button(enabled = name.isNotBlank(), onClick = { vm.addHabit(name, icon, reminder); name=""; reminder=null; showAdd=false }) { Text("Create habit") } },
            dismissButton = { TextButton(onClick = { showAdd=false }) { Text("Cancel") } }
        )
    }
    if (showTime) ReminderTimePickerDialog(onDismiss = { showTime=false }, onConfirm = { reminder=it; showTime=false })
}

@Composable
private fun HabitRow(habit: HabitEntity, habitRepository: HabitRepository, analytics: HabitAnalytics?, onOpenDetail: () -> Unit, onLogToday: (Int) -> Unit) {
    val today = remember { DateTimeUtils.today().toEpochDay() }
    val completion by habitRepository.observeCompletion(habit.id, today).collectAsState(initial = null)
    val progress = completion?.progressCount ?: 0
    val done = progress >= habit.goalCount
    val percentToday = if (habit.goalCount == 0) 0 else ((progress * 100f) / habit.goalCount).toInt().coerceAtMost(100)
    LifeOSCard(modifier = Modifier.animateContentSize(), onClick = onOpenDetail) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(50.dp)) { Box(contentAlignment=Alignment.Center, modifier=Modifier.fillMaxSize()) { Text(habit.icon, style=MaterialTheme.typography.headlineSmall) } }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(habit.name, style = MaterialTheme.typography.titleMedium)
                    Text(habit.frequency.name.lowercase().replaceFirstChar { it.uppercase() } + (habit.category?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onLogToday(progress) }) { Icon(if(done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, if(done) "Completed today" else "Log today", tint=if(done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            LifeOSProgress(percentToday / 100f, label = "$progress/${habit.goalCount} today · $percentToday%")
            analytics?.let { stats ->
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LifeOSStatChip("Current streak", "${stats.currentStreak}d", Modifier.weight(1f))
                        LifeOSStatChip("Best streak", "${stats.longestStreak}d", Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LifeOSStatChip("7 days", "${stats.completionPercentThisWeek}%", Modifier.weight(1f))
                        LifeOSStatChip("30 days", "${stats.completionPercentThisMonth}%", Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
