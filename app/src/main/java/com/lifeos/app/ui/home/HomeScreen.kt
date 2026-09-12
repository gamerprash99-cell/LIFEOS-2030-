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
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { HomeHeader(summary?.greeting ?: "Welcome", summary?.dateLabel.orEmpty(), onOpenSearch) }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { TodayOverviewCard(summary?.tasksTotalToday ?: 0, summary?.tasksCompletedToday ?: 0, summary?.habitsToday.orEmpty(), summary?.todaySpend ?: 0.0, onOpenTasks) }
            if (!morningPhotoDone) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { MorningCheckInCard(onOpenMorningPhoto) }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { HomeAlarmCard() }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { SectionHeading("Today's habits", "View all", onOpenHabits) }
            if (summary?.habitsToday.isNullOrEmpty()) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { LifeOSEmptyState("Your rhythm starts here", "Create a habit and LifeOS will keep the streak visible.", icon = Icons.Filled.LocalFireDepartment) }
            else items(summary?.habitsToday.orEmpty().take(6), key = { it.habit.id }) { HabitHomeCard(it) }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { SectionHeading("Today's tasks", "View all", onOpenTasks) }
            if (summary?.tasksToday.isNullOrEmpty()) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { LifeOSEmptyState("Nothing urgent", "You're clear for now. Add a task when something needs your attention.", icon = Icons.Filled.CheckCircle) }
            else items(summary?.tasksToday?.take(6).orEmpty(), key = { it.id }) { task -> var checked by remember(task.id, task.isCompleted) { mutableStateOf(task.isCompleted) }; TaskHomeRow(task.title, checked) { checked = it; viewModel.toggleTask(task.id, it) } }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { QuickActionsCard(onOpenCapture, onOpenNotes, onOpenDiary, onOpenTasks, onOpenHabits) }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { SpendingHomeCard(summary?.todaySpend ?: 0.0, onOpenExpenses) }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { SectionHeading("Latest memory", "Open Timeline", onOpenTimeline) }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { LatestMemoryCard(latestCapture, onOpenTimeline) }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { LifeOSAiHomeCard(onOpenAiAssistant) }
        }
        HomeFloatingActions(onCapture = onOpenCapture, onAi = onOpenAiAssistant)
    }
}

@Composable private fun HomeHeader(greeting:String,date:String,onSearch:()->Unit){Column(Modifier.fillMaxWidth().animateContentSize().padding(top=4.dp,bottom=2.dp)){Row(verticalAlignment=Alignment.CenterVertically){Surface(shape=RoundedCornerShape(999.dp),color=Color.White,border=androidx.compose.foundation.BorderStroke(1.dp,LifeOSLavender)){Text("☀️ ${date.ifBlank{"Today"}}",color=LifeOSPrimary,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=14.dp,vertical=8.dp))};Spacer(Modifier.weight(1f));Surface(shape=CircleShape,color=Color.White,border=androidx.compose.foundation.BorderStroke(2.dp,LifeOSLavender),modifier=Modifier.size(54.dp)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("🐱")}};IconButton(onClick=onSearch){Icon(Icons.Filled.Search,"Search your life")}};Text(if(greeting.contains("morning",true))greeting else "Good morning!",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=14.dp));Text("${if(greeting.contains("morning",true))"" else greeting} ☀️",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
@Composable private fun TodayOverviewCard(total:Int,done:Int,habits:List<HabitSummaryRow>,spending:Double,onClick:()->Unit){val progress=if(total==0)0f else done.toFloat()/total;val animated by animateFloatAsState(progress.coerceIn(0f,1f),label="home_progress");GradientCard(dark=true,onClick=onClick,modifier=Modifier.fillMaxWidth()){Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("TODAY'S PROGRESS",style=MaterialTheme.typography.labelLarge,color=Color.White.copy(alpha=.68f));Text("${(animated*100).toInt()}%",style=MaterialTheme.typography.displaySmall,color=Color.White);Text("$done of $total tasks completed",style=MaterialTheme.typography.bodyMedium,color=Color.White.copy(alpha=.78f))};LifeOSProgressRing(progress,size=78.dp,label="${(animated*100).toInt()}%")};LinearProgressIndicator(progress={animated},modifier=Modifier.fillMaxWidth().height(7.dp),trackColor=Color.White.copy(alpha=.16f),color=Color.White);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){HomeMetric("Tasks","$done/$total",Icons.Filled.CheckCircle);HomeMetric("Habits",habits.count{it.isDone}.toString(),Icons.Filled.LocalFireDepartment);HomeMetric("Spending","₹${"%.0f".format(Locale.getDefault(),spending)}",Icons.Filled.AccountBalanceWallet)}}}}
@Composable private fun RowScope.HomeMetric(label:String,value:String,icon:androidx.compose.ui.graphics.vector.ImageVector){Surface(modifier=Modifier.weight(1f),shape=RoundedCornerShape(16.dp),color=Color.White.copy(alpha=.10f)){Column(Modifier.padding(horizontal=10.dp,vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(icon,null,Modifier.size(18.dp),tint=Color.White.copy(alpha=.9f));Spacer(Modifier.height(4.dp));Text(value,style=MaterialTheme.typography.titleMedium,color=Color.White,fontWeight=FontWeight.Bold);Text(label,style=MaterialTheme.typography.labelSmall,color=Color.White.copy(alpha=.72f))}}}
@Composable private fun SectionHeading(title:String,action:String,onClick:()->Unit){Row(Modifier.fillMaxWidth().padding(top=2.dp),verticalAlignment=Alignment.CenterVertically){Text(title,style=MaterialTheme.typography.headlineSmall,modifier=Modifier.weight(1f));TextButton(onClick=onClick,contentPadding=PaddingValues(horizontal=8.dp)){Text(action)}}}
@Composable private fun HabitHomeCard(row:HabitSummaryRow){LifeOSCard(Modifier.fillMaxWidth()){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text(row.habit.icon,style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(row.habit.name,style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${row.progressCount}/${row.goalCount} today",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};LifeOSCompletionBadge(row.isDone)};LifeOSProgress(if(row.goalCount==0)0f else row.progressCount.toFloat()/row.goalCount)}}}
@Composable private fun TaskHomeRow(title:String,checked:Boolean,onChecked:(Boolean)->Unit){LifeOSCard(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().heightIn(min=66.dp).padding(horizontal=6.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(checked=checked,onCheckedChange=onChecked);Text(title,style=MaterialTheme.typography.bodyLarge,modifier=Modifier.weight(1f),maxLines=2,overflow=TextOverflow.Ellipsis);if(checked)Text("DONE",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)}}}
@Composable private fun QuickActionsCard(onCapture:()->Unit,onNotes:()->Unit,onDiary:()->Unit,onTask:()->Unit,onHabit:()->Unit){LifeOSCard{Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){LifeOSSectionHeader("Quick actions",supportingText="Capture • Write • Track • Grow");LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=4.dp)){item{QuickAction(Icons.Filled.CameraAlt,"Capture","Photo / Video",onCapture)};item{QuickAction(Icons.Filled.NoteAlt,"Add Note","Write something",onNotes)};item{QuickAction(Icons.Filled.EditNote,"Diary","How are you?",onDiary)};item{QuickAction(Icons.Filled.AddTask,"Add Task","Stay on track",onTask)};item{QuickAction(Icons.Filled.LocalFireDepartment,"Add Habit","Build routine",onHabit)}}}}}
@Composable private fun QuickAction(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,onClick:()->Unit){Surface(onClick=onClick,shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.primaryContainer.copy(alpha=.45f),modifier=Modifier.width(118.dp)){Column(Modifier.padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally){LifeOSIconBadge(icon,Modifier.size(44.dp));Spacer(Modifier.height(7.dp));Text(title,style=MaterialTheme.typography.labelLarge,maxLines=1,overflow=TextOverflow.Ellipsis);Text(subtitle,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)}}}
@Composable private fun SpendingHomeCard(amount:Double,onClick:()->Unit){LifeOSCard(onClick=onClick){Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(50.dp).clip(CircleShape).background(Brush.linearGradient(LifeOSPrimaryGradient)),contentAlignment=Alignment.Center){Icon(Icons.Filled.AccountBalanceWallet,null,tint=Color.White)};Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text("Today's spending",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("₹${"%.0f".format(Locale.getDefault(),amount)}",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Tap to open Expenses",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Filled.ChevronRight,null,tint=MaterialTheme.colorScheme.primary)}}}
@Composable private fun LatestMemoryCard(capture:com.lifeos.app.data.db.entities.CaptureEntity?,onClick:()->Unit){LifeOSCard(onClick=onClick){Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){Surface(shape=RoundedCornerShape(16.dp),color=LifeOSVioletSoft,modifier=Modifier.size(54.dp)){Box(contentAlignment=Alignment.Center,modifier=Modifier.fillMaxSize()){Text(if(capture==null)"✨" else when(capture.type){CaptureType.PHOTO->"📷";CaptureType.VIDEO->"🎥";CaptureType.AUDIO->"🎙️"})}};Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(if(capture==null)"No memory captured yet" else captureLabel(capture.type),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);if(capture!=null){capture.caption?.takeIf{it.isNotBlank()}?.let{Text(it,style=MaterialTheme.typography.bodyMedium,maxLines=2,overflow=TextOverflow.Ellipsis)}}};Icon(Icons.Filled.ChevronRight,null,tint=MaterialTheme.colorScheme.primary)}}}
@Composable private fun LifeOSAiHomeCard(onClick:()->Unit){GradientCard(modifier=Modifier.fillMaxWidth(),dark=true,onClick=onClick){Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){LifeOSAIOrb(size=52.dp);Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)){Text("LifeOS AI",color=Color.White,style=MaterialTheme.typography.titleLarge);Text("Offline • private • based on your local life",color=Color.White.copy(alpha=.74f),style=MaterialTheme.typography.bodySmall)};Icon(Icons.Filled.ChevronRight,null,tint=Color.White)}}}
@Composable private fun HomeFloatingActions(onCapture:()->Unit,onAi:()->Unit){Box(Modifier.fillMaxSize()){Column(modifier=Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end=16.dp,bottom=14.dp),horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(10.dp)){SmallFloatingActionButton(onClick=onAi,containerColor=MaterialTheme.colorScheme.primaryContainer,contentColor=MaterialTheme.colorScheme.primary,modifier=Modifier.size(50.dp)){Icon(Icons.Filled.AutoAwesome,"Ask LifeOS AI")};ExtendedFloatingActionButton(onClick=onCapture,containerColor=MaterialTheme.colorScheme.primary,contentColor=MaterialTheme.colorScheme.onPrimary,icon={Icon(Icons.Filled.Add,null)},text={Text("Capture",fontWeight=FontWeight.SemiBold)})}}}
private fun captureLabel(type:CaptureType)=when(type){CaptureType.PHOTO->"Photo memory";CaptureType.VIDEO->"Video memory";CaptureType.AUDIO->"Audio memory"}
