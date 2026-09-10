package com.lifeos.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.ui.components.*
import com.lifeos.app.ui.theme.LifeOSSpacing

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
    onOpenTimeline: () -> Unit = {}
) {
    val locator = LocalServiceLocator.current
    val viewModel: HomeViewModel = viewModel(factory = LambdaViewModelFactory { HomeViewModel(locator.getHomeSummaryUseCase, locator.taskRepository, locator.habitRepository) })
    val summary by viewModel.summary.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenCapture,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Capture") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
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
    }
}
