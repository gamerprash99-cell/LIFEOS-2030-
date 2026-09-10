package com.lifeos.app.ui.timeline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.domain.model.TimelineItem
import com.lifeos.app.domain.model.TimelineItemType
import com.lifeos.app.domain.usecase.BuildTimelineUseCase
import com.lifeos.app.ui.capture.rememberVideoThumbnail
import com.lifeos.app.ui.components.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TimelineViewModel(private val buildTimeline:BuildTimelineUseCase):ViewModel(){private val _items=MutableStateFlow<List<TimelineItem>>(emptyList());val items:StateFlow<List<TimelineItem>> = _items;fun loadFor(day:Long){viewModelScope.launch{_items.value=buildTimeline(day)}}}

@Composable
fun TimelineScreen(onOpenCapture:(String)->Unit={}){
    val locator=LocalServiceLocator.current;val vm:TimelineViewModel=viewModel(factory=LambdaViewModelFactory{TimelineViewModel(locator.buildTimelineUseCase)});var date by remember{mutableStateOf(DateTimeUtils.today())};val items by vm.items.collectAsState();LaunchedEffect(date){vm.loadFor(date.toEpochDay())}
    Scaffold(containerColor=MaterialTheme.colorScheme.background,topBar={TopAppBar(title={Text("Timeline")})}){padding->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp,12.dp,20.dp,24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            item{LifeOSCard{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={date=date.minusDays(1)}){Icon(Icons.Filled.ChevronLeft,"Previous day")};Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Text("LIFEOS MEMORY",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary);Text(DateTimeUtils.formatFullDate(date),style=MaterialTheme.typography.titleLarge)};IconButton(onClick={date=date.plusDays(1)}){Icon(Icons.Filled.ChevronRight,"Next day")}}}}
            if(items.isEmpty())item{LifeOSEmptyState("Nothing recorded", "There are no memories for this day yet. Capture something or add a note.",Modifier.fillMaxWidth(),Icons.Filled.EventBusy)}
            items(items,key={it.id}){item->TimelineRow(item,locator.captureRepository){if(item.type==TimelineItemType.CAPTURE)onOpenCapture(item.sourceId)}}
        }
    }
}

@Composable private fun TimelineRow(item:TimelineItem,captureRepository:CaptureRepository,onClick:()->Unit){val capture=item.type==TimelineItemType.CAPTURE;LifeOSCard(modifier=Modifier.fillMaxWidth(),onClick=if(capture)onClick else null){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){if(capture)CaptureThumbnail(item.sourceId,item.icon,captureRepository)else LifeOSIconBadge(Icons.Filled.Event);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(item.title,style=MaterialTheme.typography.titleMedium);item.subtitle?.let{Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}};Text(DateTimeUtils.formatMinutes(item.timeMinutes),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)}}}

@Composable private fun CaptureThumbnail(captureId:String,fallbackIcon:String,captureRepository:CaptureRepository){val capture by produceState<com.lifeos.app.data.db.entities.CaptureEntity?>(null,captureId){value=captureRepository.getById(captureId)};val path=capture?.filePath;Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){when{capture?.type==CaptureType.PHOTO&&path!=null->AsyncImage(path,"Photo thumbnail",Modifier.fillMaxSize(),contentScale=androidx.compose.ui.layout.ContentScale.Crop);capture?.type==CaptureType.VIDEO&&path!=null->{val thumb=rememberVideoThumbnail(path);if(thumb!=null)androidx.compose.foundation.Image(thumb,"Video thumbnail",Modifier.fillMaxSize(),contentScale=androidx.compose.ui.layout.ContentScale.Crop)else Text("▶",style=MaterialTheme.typography.titleLarge)};else->Text(fallbackIcon,style=MaterialTheme.typography.headlineSmall)}}}
