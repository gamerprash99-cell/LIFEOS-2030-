package com.lifeos.app.ui.timeline

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.components.LifeOSEmptyState
import com.lifeos.app.ui.components.LifeOSIconBadge
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
    val timelineItems by vm.items.collectAsState()
    val visibleItems = remember(timelineItems, selectedType) { selectedType?.let { type -> timelineItems.filter { it.type == type } } ?: timelineItems }
    LaunchedEffect(selectedDate) { vm.loadFor(selectedDate.toEpochDay()) }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding=PaddingValues(start=18.dp,top=14.dp,end=18.dp,bottom=34.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { TimelineHero(selectedDate, visibleItems.size) { selectedDate = DateTimeUtils.today() } }
        item { TimelineDateStrip(selectedDate,{selectedDate=selectedDate.minusDays(1)},{selectedDate=selectedDate.plusDays(1)},{selectedDate=it}) }
        item { TimelineFilters(selectedType) { selectedType = it } }
        if (visibleItems.isEmpty()) item { LifeOSEmptyState(if(selectedType==null) "A quiet day" else "Nothing here yet", "Your notes, tasks, spending, diary and moments will appear here as one simple memory trail.", Modifier.fillMaxWidth(), Icons.Filled.AutoAwesome) }
        else {
            item { Row(Modifier.fillMaxWidth().padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically){Text("YOUR DAY",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);Spacer(Modifier.width(8.dp));HorizontalDivider(Modifier.weight(1f),color=MaterialTheme.colorScheme.primary.copy(alpha=.16f));Spacer(Modifier.width(8.dp));Text("${visibleItems.size} memories",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)} }
            itemsIndexed(visibleItems,key={_,item->item.id}){index,item->TimelineAnimatedRow(index,item,locator.captureRepository,index==visibleItems.lastIndex){if(item.type==TimelineItemType.CAPTURE)onOpenCapture(item.sourceId)}}
        }
    }
}

@Composable private fun TimelineHero(date:LocalDate,count:Int,onToday:()->Unit){Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){Text("Timeline",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Your day, one memory at a time",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)};Surface(onClick=onToday,shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.primaryContainer.copy(alpha=.58f)){Row(Modifier.padding(horizontal=11.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Filled.Today,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("Today",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)}}};LifeOSCard{Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primary.copy(alpha=.12f),modifier=Modifier.size(48.dp)){Box(contentAlignment=Alignment.Center){Icon(Icons.Filled.AutoAwesome,null,tint=MaterialTheme.colorScheme.primary)}};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(DateTimeUtils.formatFullDate(date),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Text("$count ${if(count==1)"memory" else "memories"} captured",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Filled.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
@Composable private fun TimelineDateStrip(selectedDate:LocalDate,onPrevious:()->Unit,onNext:()->Unit,onDateSelected:(LocalDate)->Unit){val dates=(-2..2).map{selectedDate.plusDays(it.toLong())};Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onPrevious){Icon(Icons.Filled.ChevronLeft,"Previous day")};LazyRow(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(7.dp),contentPadding=PaddingValues(horizontal=2.dp)){itemsIndexed(dates){_,date->val selected=date==selectedDate;Surface(onClick={onDateSelected(date)},shape=RoundedCornerShape(18.dp),color=if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha=.40f),contentColor=if(selected)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,modifier=Modifier.width(62.dp).height(70.dp)){Column(Modifier.fillMaxSize().padding(vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(date.dayOfWeek.name.take(3),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.SemiBold);Text(date.dayOfMonth.toString(),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);if(selected)Text("•",style=MaterialTheme.typography.labelSmall)}}}}};IconButton(onClick=onNext){Icon(Icons.Filled.ChevronRight,"Next day")}}}
@Composable private fun TimelineFilters(selectedType:TimelineItemType?,onTypeSelected:(TimelineItemType?)->Unit){val filters=listOf(null to "All",TimelineItemType.NOTE to "Notes",TimelineItemType.DIARY to "Diary",TimelineItemType.TASK_COMPLETED to "Tasks",TimelineItemType.HABIT_COMPLETED to "Habits",TimelineItemType.EXPENSE to "Money",TimelineItemType.CAPTURE to "Moments");LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(horizontal=2.dp)){itemsIndexed(filters){_,pair->val type=pair.first;val label=pair.second;FilterChip(selected=selectedType==type,onClick={onTypeSelected(type)},label={Text(label)},leadingIcon={Icon(typeIcon(type),null,Modifier.size(17.dp))})}}}
private fun typeIcon(type:TimelineItemType?)=when(type){null->Icons.Filled.GridView;TimelineItemType.NOTE->Icons.Filled.EditNote;TimelineItemType.TASK_COMPLETED->Icons.Filled.CheckCircle;TimelineItemType.HABIT_COMPLETED->Icons.Filled.LocalFireDepartment;TimelineItemType.EXPENSE->Icons.Filled.Payments;TimelineItemType.DIARY->Icons.Filled.Book;TimelineItemType.CAPTURE->Icons.Filled.AutoAwesome}
@Composable private fun TimelineAnimatedRow(index:Int,item:TimelineItem,captureRepository:CaptureRepository,isLast:Boolean,onOpenCapture:()->Unit){var visible by remember(item.id){mutableStateOf(false)};LaunchedEffect(item.id){visible=true};AnimatedContent(targetState=visible,transitionSpec={fadeIn(tween(220)) togetherWith slideInVertically(tween(280),initialOffsetY={it/6})},label="timeline_item_$index"){shown->if(shown)TimelineMemoryCard(item,captureRepository,isLast,onOpenCapture)}}
@Composable private fun TimelineMemoryCard(item:TimelineItem,captureRepository:CaptureRepository,isLast:Boolean,onOpenCapture:()->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top){Column(Modifier.width(46.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(DateTimeUtils.formatMinutes(item.timeMinutes),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primary,modifier=Modifier.size(18.dp)){Box(contentAlignment=Alignment.Center){Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary))}};if(!isLast)Box(Modifier.width(2.dp).height(180.dp).background(MaterialTheme.colorScheme.primary.copy(alpha=.15f)))};Spacer(Modifier.width(8.dp));LifeOSCard(modifier=Modifier.weight(1f),onClick=if(item.type==TimelineItemType.CAPTURE)onOpenCapture else null){Column(Modifier.fillMaxWidth().padding(15.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){Row(verticalAlignment=Alignment.CenterVertically){Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.primaryContainer.copy(alpha=.52f)){Row(Modifier.padding(horizontal=9.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Icon(typeIcon(item.type),null,Modifier.size(16.dp),tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(6.dp));Text(typeLabel(item.type),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)}};Spacer(Modifier.weight(1f));if(item.type==TimelineItemType.CAPTURE)Icon(Icons.Filled.OpenInNew,"Open",Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)};Text(item.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis);item.subtitle?.let{Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=3,overflow=TextOverflow.Ellipsis)};if(item.type==TimelineItemType.CAPTURE)CaptureTimelineMedia(item,captureRepository)}}}}
private fun typeLabel(type:TimelineItemType)=when(type){TimelineItemType.NOTE->"Note";TimelineItemType.TASK_COMPLETED->"Task";TimelineItemType.HABIT_COMPLETED->"Habit";TimelineItemType.EXPENSE->"Money";TimelineItemType.DIARY->"Diary";TimelineItemType.CAPTURE->"Moment"}
@Composable private fun CaptureTimelineMedia(item:TimelineItem,captureRepository:CaptureRepository){val capture by produceState<CaptureEntity?>(null,item.sourceId){value=captureRepository.getById(item.sourceId)};val path=capture?.filePath;when(capture?.type){CaptureType.PHOTO->if(path!=null)AsyncImage(path,"Captured photo",Modifier.fillMaxWidth().aspectRatio(16f/10f).clip(RoundedCornerShape(18.dp)),contentScale=ContentScale.Crop);CaptureType.VIDEO->if(path!=null){val thumbnail=rememberVideoThumbnail(path);if(thumbnail!=null)Box(Modifier.fillMaxWidth().aspectRatio(16f/10f).clip(RoundedCornerShape(18.dp))){androidx.compose.foundation.Image(thumbnail,"Video thumbnail",Modifier.fillMaxSize(),contentScale=ContentScale.Crop);Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primary.copy(alpha=.92f),modifier=Modifier.size(56.dp)){Icon(Icons.Filled.PlayArrow,"Play video",tint=MaterialTheme.colorScheme.onPrimary,modifier=Modifier.padding(14.dp))}}}};CaptureType.AUDIO->Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.52f)){Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){LifeOSIconBadge(Icons.Filled.GraphicEq);Spacer(Modifier.width(10.dp));Column{Text("Audio memory",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold);Text("Tap to open and play",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}};else->Unit}}
