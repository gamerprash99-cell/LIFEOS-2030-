package com.lifeos.app.ui.timeline

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.CaptureEntity
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.domain.model.TimelineItemType
import com.lifeos.app.domain.usecase.BuildTimelineUseCase
import com.lifeos.app.ui.capture.rememberVideoThumbnail
import com.lifeos.app.ui.components.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class TimelineViewModel(private val buildTimeline: BuildTimelineUseCase) : ViewModel() {
    private val _items = MutableStateFlow<List<TimelineItem>>(emptyList())
    val items: StateFlow<List<TimelineItem>> = _items

    fun loadFor(day: Long) {
        viewModelScope.launch { _items.value = buildTimeline(day) }
    }
}

@Composable
fun TimelineScreen(onOpenCapture: (String) -> Unit = {}) {
    val locator = LocalServiceLocator.current
    val vm: TimelineViewModel = viewModel(factory = LambdaViewModelFactory { TimelineViewModel(locator.buildTimelineUseCase) })
    var selectedDate by remember { mutableStateOf(DateTimeUtils.today()) }
    var selectedType by remember { mutableStateOf<TimelineItemType?>(null) }
    val timelineItems by vm.items.collectAsState()
    val visibleItems = remember(timelineItems, selectedType) {
        selectedType?.let { type -> timelineItems.filter { it.type == type } } ?: timelineItems
    }

    LaunchedEffect(selectedDate) { vm.loadFor(selectedDate.toEpochDay()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TimelineHeader(
                date = selectedDate,
                onPrevious = { selectedDate = selectedDate.minusDays(1) },
                onNext = { selectedDate = selectedDate.plusDays(1) },
                onToday = { selectedDate = DateTimeUtils.today() }
            )
        }
        item { TimelineDateStrip(selectedDate = selectedDate, onDateSelected = { selectedDate = it }) }
        item { TimelineFilters(selectedType = selectedType, onTypeSelected = { selectedType = it }) }

        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("YOUR DAY", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.primary.copy(alpha = .18f))
                Spacer(Modifier.width(8.dp))
                Text("${visibleItems.size} memories", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (visibleItems.isEmpty()) {
            item {
                LifeOSEmptyState(
                    title = if (selectedType == null) "A quiet day" else "Nothing in this filter",
                    message = "No memories are recorded for ${DateTimeUtils.formatFullDate(selectedDate)} yet.",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Filled.AutoAwesome
                )
            }
        } else {
            itemsIndexed(visibleItems, key = { _, item -> item.id }) { index, item ->
                TimelineAnimatedRow(
                    index = index,
                    item = item,
                    captureRepository = locator.captureRepository,
                    onOpenCapture = { if (item.type == TimelineItemType.CAPTURE) onOpenCapture(item.sourceId) }
                )
            }
        }
    }
}

@Composable
private fun TimelineHeader(date: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Timeline, null, tint = MaterialTheme.colorScheme.primary) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("LifeOS Timeline", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
                Text("• Offline  •  Local-first", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onToday) { Icon(Icons.Filled.Today, "Today", tint = MaterialTheme.colorScheme.primary) }
        }
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .18f)),
            tonalElevation = 1.dp
        ) {
            Row(Modifier.fillMaxWidth().height(74.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) { Icon(Icons.Filled.ChevronLeft, "Previous day") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("YOUR LIFE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    AnimatedContent(targetState = DateTimeUtils.formatFullDate(date), label = "timeline_date") { value ->
                        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = onNext) { Icon(Icons.Filled.ChevronRight, "Next day") }
            }
        }
    }
}

@Composable
private fun TimelineDateStrip(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val dates = (-2..2).map { selectedDate.plusDays(it.toLong()) }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
        itemsIndexed(dates) { _, date ->
            val selected = date == selectedDate
            Surface(
                onClick = { onDateSelected(date) },
                shape = RoundedCornerShape(22.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f),
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(72.dp).height(82.dp)
            ) {
                Column(Modifier.fillMaxSize().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(date.dayOfWeek.name.take(3), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (selected) Text("TODAY", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun TimelineFilters(selectedType: TimelineItemType?, onTypeSelected: (TimelineItemType?) -> Unit) {
    val filters = listOf(
        null to "All" ,
        TimelineItemType.CAPTURE to "Moments",
        TimelineItemType.TASK_COMPLETED to "Tasks",
        TimelineItemType.EXPENSE to "Money"
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
        itemsIndexed(filters) { _, (type, label) ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(label) },
                leadingIcon = {
                    Icon(
                        when (type) {
                            null -> Icons.Filled.GridView
                            TimelineItemType.CAPTURE -> Icons.Filled.PhotoCamera
                            TimelineItemType.TASK_COMPLETED -> Icons.Filled.CheckCircle
                            TimelineItemType.EXPENSE -> Icons.Filled.AccountBalanceWallet
                            else -> Icons.Filled.Circle
                        }, null, Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun TimelineAnimatedRow(index: Int, item: TimelineItem, captureRepository: CaptureRepository, onOpenCapture: () -> Unit) {
    var visible by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.id) { visible = true }
    AnimatedContent(targetState = visible, transitionSpec = { fadeIn(tween(220)) + slideInVertically(tween(280), initialOffsetY = { it / 5 }) }, label = "timeline_item_$index") { shown ->
        if (shown) TimelineMemoryCard(item, captureRepository, onOpenCapture)
    }
}

@Composable
private fun TimelineMemoryCard(item: TimelineItem, captureRepository: CaptureRepository, onOpenCapture: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.width(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(22.dp))
            Box(Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Box(Modifier.width(2.dp).height(210.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .18f)))
        }
        LifeOSCard(modifier = Modifier.weight(1f), onClick = if (item.type == TimelineItemType.CAPTURE) onOpenCapture else null) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .65f)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(typeIcon(item.type), null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(typeLabel(item.type), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(DateTimeUtils.formatMinutes(item.timeMinutes), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(item.dateEpochDay)) + " · " + DateTimeUtils.formatMinutes(item.timeMinutes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                item.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis) }
                if (item.type == TimelineItemType.CAPTURE) CaptureTimelineMedia(item, captureRepository)
            }
        }
    }
}

private fun typeLabel(type: TimelineItemType) = when (type) {
    TimelineItemType.NOTE -> "Note"
    TimelineItemType.TASK_COMPLETED -> "Task"
    TimelineItemType.HABIT_COMPLETED -> "Habit"
    TimelineItemType.EXPENSE -> "Money"
    TimelineItemType.DIARY -> "Diary"
    TimelineItemType.CAPTURE -> "Moment"
}

private fun typeIcon(type: TimelineItemType) = when (type) {
    TimelineItemType.NOTE -> Icons.Filled.EditNote
    TimelineItemType.TASK_COMPLETED -> Icons.Filled.CheckCircle
    TimelineItemType.HABIT_COMPLETED -> Icons.Filled.LocalFireDepartment
    TimelineItemType.EXPENSE -> Icons.Filled.Payments
    TimelineItemType.DIARY -> Icons.Filled.Book
    TimelineItemType.CAPTURE -> Icons.Filled.PhotoCamera
}

@Composable
private fun CaptureTimelineMedia(item: TimelineItem, captureRepository: CaptureRepository) {
    val capture by produceState<CaptureEntity?>(null, item.sourceId) { value = captureRepository.getById(item.sourceId) }
    val path = capture?.filePath
    when (capture?.type) {
        CaptureType.PHOTO -> if (path != null) AsyncImage(path, "Captured photo", Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
        CaptureType.VIDEO -> if (path != null) {
            val thumbnail = rememberVideoThumbnail(path)
            if (thumbnail != null) {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(18.dp))) {
                    androidx.compose.foundation.Image(thumbnail, "Video thumbnail", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .92f), modifier = Modifier.size(58.dp)) { Icon(Icons.Filled.PlayArrow, "Play video", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(15.dp)) } }
                }
            }
        }
        CaptureType.AUDIO -> Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f)) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { LifeOSIconBadge(Icons.Filled.GraphicEq); Spacer(Modifier.width(12.dp)); Text("Audio memory", style = MaterialTheme.typography.titleMedium) }
        }
        else -> Unit
    }
}
