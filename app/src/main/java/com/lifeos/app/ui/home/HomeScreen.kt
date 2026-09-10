package com.lifeos.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.ui.components.GlassCard
import com.lifeos.app.ui.components.LifeOSMetricCard
import com.lifeos.app.ui.components.LifeOSSectionHeader
import com.lifeos.app.ui.components.LifeOSStatusPill
import com.lifeos.app.ui.theme.LifeOSSpacing

/**
 * Home follows the LifeOS dashboard priority:
 * Today → Tasks → Habits → Spending → Recent Activity → Intelligence.
 *
 * Navigation remains the existing Compose NavHost; this screen only changes
 * presentation/order and reuses the live HomeSummary data.
 */
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
    val viewModel: HomeViewModel = viewModel(
        factory = LambdaViewModelFactory {
            HomeViewModel(locator.getHomeSummaryUseCase, locator.taskRepository, locator.habitRepository)
        }
    )
    val summary by viewModel.summary.collectAsState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenCapture,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Capture") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = PaddingValues(
                start = LifeOSSpacing.screenPadding,
                end = LifeOSSpacing.screenPadding,
                top = LifeOSSpacing.screenPadding,
                bottom = LifeOSSpacing.extendedFabContentClearance
            ),
            verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.sectionSpacing)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(summary?.greeting ?: "Welcome", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        summary?.dateLabel.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.cardSpacing)) {
                    LifeOSSectionHeader("Today")
                    LifeOSMetricCard(
                        label = "Tasks completed",
                        value = "${summary?.tasksCompletedToday ?: 0}/${summary?.tasksTotalToday ?: 0}",
                        supportingText = if ((summary?.overdueTaskCount ?: 0) > 0)
                            "${summary?.overdueTaskCount} overdue"
                        else "You're on track today",
                        icon = Icons.Filled.CheckCircle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.cardSpacing)) {
                    LifeOSSectionHeader(
                        title = "Tasks",
                        action = {
                            LifeOSStatusPill(
                                text = "View all",
                                modifier = Modifier.clickable { onOpenTasks() }
                            )
                        }
                    )
                    summary?.tasksToday?.take(5)?.let { tasks ->
                        tasks.forEach { task ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { viewModel.toggleTask(task.id, it) }
                                )
                                Text(task.title, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        if (tasks.isEmpty()) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "No tasks scheduled for today.",
                                    modifier = Modifier.padding(LifeOSSpacing.cardPadding),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.cardSpacing)) {
                    LifeOSSectionHeader(
                        title = "Habits",
                        action = {
                            LifeOSStatusPill(
                                text = "View all",
                                modifier = Modifier.clickable { onOpenHabits() }
                            )
                        }
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(LifeOSSpacing.cardSpacing)) {
                        items(summary?.habitsToday ?: emptyList()) { row ->
                            GlassCard(modifier = Modifier.height(112.dp)) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(row.habit.icon, style = MaterialTheme.typography.headlineMedium)
                                    Text(row.habit.name, style = MaterialTheme.typography.labelMedium)
                                    Icon(
                                        if (row.isDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (row.isDone) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${row.progressCount}/${row.goalCount}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.cardSpacing)) {
                    LifeOSSectionHeader("Spending")
                    LifeOSMetricCard(
                        label = "Today's spend",
                        value = "₹${"%.0f".format(summary?.todaySpend ?: 0.0)}",
                        supportingText = "Tap to open Expenses",
                        modifier = Modifier.fillMaxWidth().clickable { onOpenExpenses() }
                    )
                }
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenTimeline() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(LifeOSSpacing.cardPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Timeline, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Recent Activity", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Open your chronological LifeOS memory.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(LifeOSSpacing.cardSpacing)) {
                    LifeOSSectionHeader("Intelligence")
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenAiAssistant() }
                    ) {
                        Column(modifier = Modifier.padding(LifeOSSpacing.cardPadding)) {
                            Text("Ask LifeOS", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Get offline summaries, trends, patterns and answers from your local data.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
