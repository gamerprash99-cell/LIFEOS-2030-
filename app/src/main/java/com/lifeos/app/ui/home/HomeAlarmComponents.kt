package com.lifeos.app.ui.home

import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.reminders.AlarmScheduler
import com.lifeos.app.core.util.DailyAlarm
import com.lifeos.app.core.util.rememberPermissionState
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MorningCheckInCard(onClick:()->Unit){LifeOSCard(onClick=onClick,modifier=Modifier.animateContentSize()){Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("☀️",style=MaterialTheme.typography.headlineMedium);Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text("Morning check-in ✨",style=MaterialTheme.typography.titleLarge);Text("Take a quick photo and place it in today’s Timeline memory 📷",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}};HorizontalDivider();Row(verticalAlignment=Alignment.CenterVertically){Text("◷ Expires in 2h",color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.weight(1f));Button(onClick=onClick,shape=RoundedCornerShape(18.dp)){Icon(Icons.Filled.CameraAlt,null);Spacer(Modifier.width(8.dp));Text("Snap Photo")}}}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAlarmCard(){val locator=LocalServiceLocator.current;val context=androidx.compose.ui.platform.LocalContext.current;val alarms by locator.settingsStore.alarmTimes.collectAsState(initial=emptyList());var showTimePicker by remember{mutableStateOf(false)};var editingAlarm by remember{mutableStateOf<DailyAlarm?>(null)};val scope=rememberCoroutineScope();val notificationPermission=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU)rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)else null;val exactAllowed=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()else true
    fun saveAlarms(updated:List<DailyAlarm>){val normalized=updated.distinctBy{it.minutesSinceMidnight}.sortedBy{it.minutesSinceMidnight};scope.launch{locator.settingsStore.setAlarmTimes(normalized);AlarmScheduler.replaceDaily(context,alarms,normalized)}}
    LifeOSCard{Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("🧠",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("Math alarms 🧠✨",style=MaterialTheme.typography.titleLarge);Text("Wake your brain gently with quick puzzles",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=alarms.isNotEmpty(),onCheckedChange={checked->if(checked){if(notificationPermission!=null&&!notificationPermission.isGranted)notificationPermission.request();if(alarms.isEmpty())saveAlarms(listOf(DailyAlarm(6,0)))}else saveAlarms(emptyList())})};if(alarms.isEmpty())Text("No alarms yet. Add your first gentle brain wake-up.",color=MaterialTheme.colorScheme.onSurfaceVariant)else alarms.forEachIndexed{index,alarm->Surface(shape=RoundedCornerShape(20.dp),color=Color.White,border=BorderStroke(1.dp,com.lifeos.app.ui.theme.LifeOSLavender)){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(String.format(Locale.getDefault(),"%02d:%02d",alarm.hour,alarm.minute),style=MaterialTheme.typography.headlineMedium);Text(if(index==0)"Every day · gentle math challenge 🧩" else "Weekdays · speed arithmetic ⚡",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(onClick={editingAlarm=alarm;showTimePicker=true}){Icon(Icons.Filled.Edit,"Edit alarm")};IconButton(onClick={saveAlarms(alarms.filterNot{it.minutesSinceMidnight==alarm.minutesSinceMidnight})}){Icon(Icons.Filled.DeleteOutline,"Delete alarm")}}}};OutlinedButton(onClick={editingAlarm=null;showTimePicker=true},modifier=Modifier.fillMaxWidth().height(54.dp)){Icon(Icons.Filled.Add,null);Spacer(Modifier.width(8.dp));Text("Add new alarm")};if(alarms.isNotEmpty()&&Build.VERSION.SDK_INT>=Build.VERSION_CODES.S&&!exactAllowed)TextButton(onClick={context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply{data=android.net.Uri.parse("package:${context.packageName}")}))){Text("Allow exact alarm timing")}}}
    if(showTimePicker){val initial=editingAlarm?:DailyAlarm(6,0);val state=rememberTimePickerState(initialHour=initial.hour,initialMinute=initial.minute,is24Hour=false);AlertDialog(onDismissRequest={showTimePicker=false},title={Text(if(editingAlarm==null)"Add daily math alarm" else "Edit daily math alarm")},text={TimePicker(state)},confirmButton={TextButton(onClick={val picked=DailyAlarm(state.hour,state.minute);val updated=if(editingAlarm==null)alarms+picked else alarms.map{if(it.minutesSinceMidnight==editingAlarm!!.minutesSinceMidnight)picked else it};showTimePicker=false;editingAlarm=null;saveAlarms(updated)}){Text("Save alarm")}},dismissButton={TextButton(onClick={showTimePicker=false;editingAlarm=null}){Text("Cancel")}})}}
