package com.lifeos.app.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    val vm: TimelineViewModel = viewModel(
        factory = LambdaViewModelFactory { TimelineViewModel(locator.buildTimelineUseCase) }
    )
    var selectedDate by remember { mutableStateOf(DateTimeUtils.today()) }
    var selectedType by remember { mutableStateOf<TimelineItemType?>(null) }
    val timelineItems by vm.items.collectAsState()
    val visibleItems = remember(timelineItems, selectedType) {
        selectedType?.let { type -> timelineItems.filter { it.type == type } } ?: timelineItems
    }

    LaunchedEffect(selectedDate) { vm.loadFor(selectedDate.toEpochDay()) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 28.dp),
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
            item {
                TimelineDateStrip(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it }
                )
            }
            item {
                TimelineFilters(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it }
                )
            }

            if (visibleItems.isEmpty()) {
                item {
                    LifeOSEmptyState(
                        title = if (selectedType == null) "A quiet day" else "Nothing in this filter",
                        message = if (selectedType == null)
                            "No memories are recorded for ${DateTimeUtils.formatFullDate(selectedDate)} yet."
                        else
                            "There are no ${selectedType.name.lowercase()} memories on this day.",
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Filled.AutoAwesome
                    )
                }
            } else {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "YOUR DAY",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.primary.copy(alpha = .18f))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${visibleItems.size} memories",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
}

@Composable
private fun TimelineHeader(
    date: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Timeline, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(10.dp))
                Text("Life Timeline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "• Offline  •  Local-first",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onToday) {
            Icon(Icons.Filled.Today, "Today", tint = MaterialTheme.colorScheme.primary)
        }
    }

    Spacer(Modifier.height(8.dp))
    LifeOSCard {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) { Icon(Icons.Filled.ChevronLeft, "Previous day") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("YOUR LIFE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(DateTimeUtils.formatFullDate(date), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onNext) { Icon(Icons.Filled.ChevronRight, "Next day") }
        }
    }
}

@Composable
private fun TimelineDateStrip(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val dates = (-2..2).map { selectedDate.plusDays(it.toLong()) }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dates.forEach { date ->
            val selected = date == selectedDate
            Surface(
                modifier = Modifier.weight(1f).height(62.dp).clickable { onDateSelected(date) },
                shape = RoundedCornerShape(18.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .68f),
                tonalElevation = if (selected) 5.dp else 0.dp
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        date.dayOfWeek.name.take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (date == DateTimeUtils.today()) {
                        Text("TODAY", style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineFilters(selectedType: TimelineItemType?, onTypeSelected: (TimelineItemType?) -> Unit) {
    val filters = listOf(
        null to "All",
        TimelineItemType.CAPTURE to "Moments",
        TimelineItemType.TASK_COMPLETED to "Tasks",
        TimelineItemType.EXPENSE to "Money"
    )
    LazyRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters.size) { index ->
            val (type, label) = filters[index]
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = {
                    Icon(
                        when (type) {
                            null -> Icons.Filled.SelectAll
                            TimelineItemType.CAPTURE -> Icons.Filled.PhotoCamera
                            TimelineItemType.TASK_COMPLETED -> Icons.Filled.CheckCircle
                            TimelineItemType.EXPENSE -> Icons.Filled.AccountBalanceWallet
                            else -> Icons.Filled.AutoAwesome
                        },
                        null,
                        Modifier.size(15.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun TimelineAnimatedRow(
    index: Int,
    item: TimelineItem,
    captureRepository: CaptureRepository,
    onOpenCapture: () -> Unit
) {
    var visible by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 45L).coerceAtMost(360L))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + slideInVertically(tween(320), initialOffsetY = { it / 5 })
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            TimelineRail()
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                TimelineRow(item, captureRepository, onOpenCapture)
            }
        }
    }
}

@Composable
private fun TimelineRail() {
    Box(
        Modifier.width(18.dp).height(44.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            Modifier.width(2.dp).fillMaxHeight().padding(top = 14.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                Modifier.size(12.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(3.dp)
            ) {
                Box(
                    Modifier.fillMaxSize().clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    item: TimelineItem,
    captureRepository: CaptureRepository,
    onOpenCapture: () -> Unit
) {
    val isCapture = item.type == TimelineItemType.CAPTURE
    val cardModifier = Modifier.fillMaxWidth().animateContentSize()
    val content = @Composable {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimelineTypePill(item)
                Spacer(Modifier.weight(1f))
                Text(
                    DateTimeUtils.formatMinutes(item.timeMinutes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isCapture) {
                CaptureTimelineMedia(item, captureRepository)
            }

            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.icon, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    item.subtitle?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (isCapture) 3 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    item.moodOrCategory?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                if (isCapture) {
                    Icon(Icons.Filled.ChevronRight, "Open capture", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (isCapture) {
        LifeOSCard(modifier = cardModifier, onClick = onOpenCapture, content = content)
    } else {
        LifeOSCard(modifier = cardModifier, content = content)
    }
}

@Composable
private fun TimelineTypePill(item: TimelineItem) {
    val label = when (item.type) {
        TimelineItemType.NOTE -> "Note"
        TimelineItemType.TASK_COMPLETED -> "Task"
        TimelineItemType.HABIT_COMPLETED -> "Habit"
        TimelineItemType.EXPENSE -> "Expense"
        TimelineItemType.DIARY -> "Diary"
        TimelineItemType.CAPTURE -> "Moment"
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .62f)
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (item.type) {
                    TimelineItemType.NOTE -> Icons.Filled.EditNote
                    TimelineItemType.TASK_COMPLETED -> Icons.Filled.CheckCircle
                    TimelineItemType.HABIT_COMPLETED -> Icons.Filled.LocalFireDepartment
                    TimelineItemType.EXPENSE -> Icons.Filled.Payments
                    TimelineItemType.DIARY -> Icons.Filled.Book
                    TimelineItemType.CAPTURE -> Icons.Filled.PhotoCamera
                },
                null,
                Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(5.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CaptureTimelineMedia(item: TimelineItem, captureRepository: CaptureRepository) {
    val capture by produceState<CaptureEntity?>(null, item.sourceId) {
        value = captureRepository.getById(item.sourceId)
    }
    val path = capture?.filePath
    when (capture?.type) {
        CaptureType.PHOTO -> if (path != null) {
            AsyncImage(
                model = path,
                contentDescription = "Captured photo",
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 205.dp).clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )
        }
        CaptureType.VIDEO -> if (path != null) {
            val thumbnail = rememberVideoThumbnail(path)
            if (thumbnail != null) {
                Box(Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 205.dp).clip(RoundedCornerShape(18.dp))) {
                    androidx.compose.foundation.Image(thumbnail, "Video thumbnail", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .9f), modifier = Modifier.size(58.dp)) {
                            Icon(Icons.Filled.PlayArrow, "Play video", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(15.dp))
                        }
                    }
                }
            }
        }
        CaptureType.AUDIO -> Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f)
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                LifeOSIconBadge(Icons.Filled.GraphicEq)
                Spacer(Modifier.width(12.dp))
                Text("Audio memory", style = MaterialTheme.typography.titleMedium)
            }
        }
        else -> Unit
    }
}
