package com.lifeos.app.ui.habits

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import com.lifeos.app.ui.theme.*
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
        viewModelScope.launch { val day = DateTimeUtils.today(); val millis = reminderTime?.let { day.atTime(it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }; habitRepository.createHabit(name = name, icon = icon.ifBlank { "✅" }, reminderEpochMillis = millis) }
    }
    fun logToday(id: String, current: Int, goal: Int) = viewModelScope.launch { habitRepository.logProgress(id, DateTimeUtils.today().toEpochDay(), (current + 1).coerceAtMost(goal + 3)) }
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

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 112.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Surface(shape = RoundedCornerShape(999.dp), color = LifeOSLavender.copy(alpha = .42f), border = androidx.compose.foundation.BorderStroke(1.dp, LifeOSLavender)) { Text("✨ HABITS & RHYTHM", color = LifeOSPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Habits 🌸", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Surface(shape = CircleShape, color = Color.White, border = androidx.compose.foundation.BorderStroke(2.dp, LifeOSLavender), modifier = Modifier.size(54.dp)) { Box(contentAlignment = Alignment.Center) { Text("🐰", style = MaterialTheme.typography.headlineSmall) } }
                    }
                }
            }
            item {
                LifeOSCard {
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(LifeOSPrimaryGradient)), contentAlignment = Alignment.Center) { Text("🔥", style = MaterialTheme.typography.headlineMedium) }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) { Text("Your rhythm ✨", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Today, this week and this month at a glance.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Surface(shape = RoundedCornerShape(999.dp), color = LifeOSVioletSoft) { Text("Week ${java.time.LocalDate.now().get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())}", color = LifeOSPrimary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) }
                        }
                        HorizontalDivider(color = LifeOSLavender.copy(alpha = .5f))
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            val labels = listOf("MON","TUE","WED","THU","FRI","SAT","SUN")
                            labels.forEachIndexed { index, label -> DayDot(label, index) }
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("TODAY'S HABITS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("View All (${habits.size})", color = LifeOSPrimary, fontWeight = FontWeight.Bold) }
            }
            if (habits.isEmpty()) item { LifeOSEmptyState("No habits yet", "Tap + to create your first daily habit.", icon = Icons.Filled.LocalFireDepartment) }
            items(habits, key = { it.id }) { habit -> HabitRow(habit, locator.habitRepository, analytics[habit.id], { onOpenHabit(habit.id) }) { current -> vm.logToday(habit.id, current, habit.goalCount) } }
        }
        FloatingActionButton(onClick = { showAdd = true }, containerColor = LifeOSPrimary, contentColor = Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 22.dp, bottom = 18.dp)) { Icon(Icons.Filled.Add, "New habit", modifier = Modifier.size(30.dp)) }
    }

    if (showAdd) AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("Create a habit") }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Choose an icon", style = MaterialTheme.typography.labelLarge); LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(PRESET_HABIT_ICONS) { preset -> FilterChip(selected = preset == icon, onClick = { icon = preset }, label = { Text(preset, style = MaterialTheme.typography.titleLarge) }) } }
        OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Habit name") }, singleLine = true)
        OutlinedButton(onClick = { showTime = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Notifications, null); Spacer(Modifier.width(8.dp)); Text(reminder?.let { "Reminder · $it" } ?: "Add a reminder (optional)") }
    } }, confirmButton = { Button(enabled = name.isNotBlank(), onClick = { vm.addHabit(name, icon, reminder); name=""; reminder=null; showAdd=false }) { Text("Create habit") } }, dismissButton = { TextButton(onClick = { showAdd=false }) { Text("Cancel") } })
    if (showTime) ReminderTimePickerDialog(onDismiss = { showTime=false }, onConfirm = { reminder=it; showTime=false })
}

@Composable private fun DayDot(label: String, index: Int) {
    val active = index < 4
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (index == 3) LifeOSPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Text("${15 + index}", style = MaterialTheme.typography.labelSmall, color = if (index == 3) LifeOSPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = RoundedCornerShape(14.dp), color = if (index == 3) LifeOSPrimary else if (active) LifeOSVioletSoft else MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, if (index == 3) LifeOSPrimary else LifeOSLavender), modifier = Modifier.size(38.dp)) { Box(contentAlignment = Alignment.Center) { Text(if (active) if (index == 3) "★" else "✓" else "·", color = if (index == 3) Color.White else LifeOSPrimary, fontWeight = FontWeight.Bold) } }
    }
}

@Composable private fun HabitRow(habit: HabitEntity, repo: HabitRepository, analytics: HabitAnalytics?, onOpen: () -> Unit, onLog: (Int) -> Unit) {
    val today = remember { DateTimeUtils.today().toEpochDay() }
    val completion by repo.observeCompletion(habit.id, today).collectAsState(initial = null)
    val progress = completion?.progressCount ?: 0
    val done = progress >= habit.goalCount
    val percent = if (habit.goalCount == 0) 0 else ((progress * 100f) / habit.goalCount).toInt().coerceAtMost(100)
    LifeOSCard(modifier = Modifier.animateContentSize(), onClick = onOpen) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(18.dp), color = LifeOSVioletSoft, modifier = Modifier.size(58.dp)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(habit.icon, style = MaterialTheme.typography.headlineMedium) } }
                Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(habit.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Spacer(Modifier.width(8.dp)); Surface(shape=RoundedCornerShape(999.dp), color=LifeOSVioletSoft) { Text("Daily", color=LifeOSPrimary, modifier=Modifier.padding(horizontal=9.dp, vertical=5.dp), style=MaterialTheme.typography.labelSmall) } }; Text("Goal: ${habit.goalCount} · ${habit.category ?: "Mind & Body"} 🌿", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = { onLog(progress) }) { Surface(shape = RoundedCornerShape(15.dp), color = if(done) LifeOSPrimary else LifeOSVioletSoft, modifier = Modifier.size(52.dp)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(if(done) Icons.Filled.Check else Icons.Filled.Check, null, tint = if(done) Color.White else LifeOSPrimary, modifier=Modifier.size(28.dp)) } } }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("$progress/${habit.goalCount} today · $percent%", style=MaterialTheme.typography.titleMedium, color=MaterialTheme.colorScheme.onSurface, fontWeight=FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(if(done) "Complete! 🎉" else "Remaining today", color=if(done) LifeOSPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight=FontWeight.SemiBold) }
            LifeOSProgress(percent/100f)
            analytics?.let { stats -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { LifeOSStatChip("Current streak", "${stats.currentStreak}d 🔥", Modifier.weight(1f)); LifeOSStatChip("Best streak", "${stats.longestStreak}d 🏆", Modifier.weight(1f)) }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { LifeOSStatChip("7 days", "${stats.completionPercentThisWeek}% 📊", Modifier.weight(1f)); LifeOSStatChip("30 days", "${stats.completionPercentThisMonth}% 📈", Modifier.weight(1f)) } } }
        }
    }
}
