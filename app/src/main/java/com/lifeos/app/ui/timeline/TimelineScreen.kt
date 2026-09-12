package com.lifeos.app.ui.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
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
import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.domain.model.TimelineItemType
import com.lifeos.app.domain.usecase.BuildTimelineUseCase
import com.lifeos.app.ui.components.LifeOSCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class TimelineViewModel(private val buildTimeline: BuildTimelineUseCase) : ViewModel() {
    private val _items = MutableStateFlow<List<TimelineItem>>(emptyList())
    val items: StateFlow<List<TimelineItem>> = _items
    fun loadFor(day: Long) { viewModelScope.launch { _items.value = buildTimeline(day) } }
}

@Composable
fun TimelineScreen(onOpenCapture: (String) -> Unit = {}) {
    val locator = LocalServiceLocator.current
    val vm: TimelineViewModel = viewModel(factory = LambdaViewModelFactory { TimelineViewModel(locator.buildTimelineUseCase) })
    var selectedDate by remember { mutableStateOf(DateTimeUtils.today()) }
    var selectedType by remember { mutableStateOf<TimelineItemType?>(null) }
    val items by vm.items.collectAsState()

    LaunchedEffect(selectedDate) { vm.loadFor(selectedDate.toEpochDay()) }
    val visible = remember(items, selectedType) { if (selectedType == null) items else items.filter { it.type == selectedType } }

    Scaffold(topBar = { TopAppBar(title = { Text("Timeline") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedDate = selectedDate.minusDays(1) }) { Icon(Icons.Filled.ArrowBack, "Previous day") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary)
                    Text(selectedDate.toString(), style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = { selectedDate = selectedDate.plusDays(1) }) { Icon(Icons.Filled.ArrowForward, "Next day") }
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(visible, key = { it.id }) { item ->
                    LifeOSCard(modifier = Modifier.fillMaxWidth(), onClick = { if (item.type == TimelineItemType.CAPTURE) onOpenCapture(item.sourceId) }) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.icon, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                item.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                Text(DateTimeUtils.formatMinutes(item.timeMinutes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                if (visible.isEmpty()) item { Text("Nothing recorded for this day.", modifier = Modifier.fillMaxWidth().padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
