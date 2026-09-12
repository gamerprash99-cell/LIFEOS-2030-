package com.lifeos.app.ui.home

import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import com.lifeos.app.core.reminders.AlarmScheduler
import com.lifeos.app.core.util.DailyAlarm
import com.lifeos.app.core.util.rememberPermissionState
import com.lifeos.app.core.di.LocalServiceLocator
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
private fun MorningCheckInCard(onClick: () -> Unit) {
    LifeOSCard(onClick = onClick, modifier = Modifier.animateContentSize()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(com.lifeos.app.ui.theme.LifeOSPrimaryGradient)), contentAlignment = Alignment.Center) { Text("☀️", style = MaterialTheme.typography.headlineMedium) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Morning check-in ✨", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold); Spacer(Modifier.weight(1f)); Surface(shape=RoundedCornerShape(999.dp),color=Color(0xFFFFEAF2)){Text("Daily",color=Color(0xFFC73572),modifier=Modifier.padding(horizontal=10.dp,vertical=6.dp))} }
                    Spacer(Modifier.height(5.dp)); Text("Take a quick photo and place it in today’s Timeline memory 📷", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = com.lifeos.app.ui.theme.LifeOSLavender.copy(alpha=.45f))
            Row(verticalAlignment = Alignment.CenterVertically) { Text("◷ Expires in 2h", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Button(onClick=onClick,shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=com.lifeos.app.ui.theme.LifeOSPrimary)){Icon(Icons.Filled.CameraAlt,null);Spacer(Modifier.width(8.dp));Text("Snap Photo",fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)} }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeAlarmCard() {
    val locator=LocalServiceLocator.current
    val context=androidx.compose.ui.platform.LocalContext.current
    val alarms by locator.settingsStore.alarmTimes.collectAsState(initial=emptyList())
    var showTimePicker by remember{mutableStateOf(false)}
    var editingAlarm by remember{mutableStateOf<DailyAlarm?>(null)}
    val scope=rememberCoroutineScope()
    val notificationPermission=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU)rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)else null
    val exactAllowed=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()else true
    fun saveAlarms(updated:List<DailyAlarm>){val normalized=updated.distinctBy{it.minutesSinceMidnight}.sortedBy{it.minutesSinceMidnight};scope.launch{locator.settingsStore.setAlarmTimes(normalized);AlarmScheduler.replaceDaily(context,alarms,normalized)}}
    LifeOSCard{Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(com.lifeos.app.ui.theme.LifeOSPrimaryGradient)),contentAlignment=Alignment.Center){Text("🧠",style=MaterialTheme.typography.headlineSmall)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("Math alarms 🧠✨",style=MaterialTheme.typography.titleLarge,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold);Text("Wake your brain gently with quick puzzles",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=alarms.isNotEmpty(),onCheckedChange={checked->if(checked){if(notificationPermission!=null&&!notificationPermission.isGranted)notificationPermission.request();if(alarms.isEmpty())saveAlarms(listOf(DailyAlarm(6,0)))}else saveAlarms(emptyList())})}
        if(alarms.isEmpty()){Surface(shape=RoundedCornerShape(20.dp),color=com.lifeos.app.ui.theme.LifeOSVioletSoft.copy(alpha=.48f),modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Filled.AddAlarm,null,tint=com.lifeos.app.ui.theme.LifeOSPrimary);Spacer(Modifier.width(10.dp));Text("No alarms yet. Add your first gentle brain wake-up.")}}}
        else {alarms.forEachIndexed{index,alarm->Surface(shape=RoundedCornerShape(24.dp),color=Color.White,border=BorderStroke(1.dp,com.lifeos.app.ui.theme.LifeOSLavender),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Surface(shape=RoundedCornerShape(17.dp),color=if(index==0)com.lifeos.app.ui.theme.LifeOSVioletSoft else Color(0xFFFFEAF2),modifier=Modifier.size(54.dp)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Icon(if(index==0)Icons.Filled.Alarm else Icons.Filled.Notifications,null,tint=if(index==0)com.lifeos.app.ui.theme.LifeOSPrimary else Color(0xFFE53E86),modifier=Modifier.size(26.dp))}};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(String.format(Locale.getDefault(),"%02d:%02d",alarm.hour,alarm.minute),style=MaterialTheme.typography.displaySmall,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold);Text(if(index==0)"Every day · gentle math challenge 🧩" else "Weekdays · speed arithmetic ⚡",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked=true,onCheckedChange={})};HorizontalDivider(Modifier.padding(vertical=12.dp),color=com.lifeos.app.ui.theme.LifeOSLavender.copy(alpha=.45f));Row(verticalAlignment=Alignment.CenterVertically){Surface(shape=RoundedCornerShape(999.dp),color=if(index==0)Color(0xFFF1ECFF) else Color(0xFFFFF4D8)){Text(if(index==0)"± Easy: 3 Puzzles" else "⚡ Medium: 5 Puzzles",color=if(index==0)com.lifeos.app.ui.theme.LifeOSPrimary else Color(0xFFC98313),modifier=Modifier.padding(horizontal=11.dp,vertical=7.dp),style=MaterialTheme.typography.labelMedium)};Spacer(Modifier.weight(1f));IconButton(onClick={editingAlarm=alarm;showTimePicker=true}){Icon(Icons.Filled.Edit,"Edit alarm",tint=com.lifeos.app.ui.theme.LifeOSPrimary)};IconButton(onClick={saveAlarms(alarms.filterNot{it.minutesSinceMidnight==alarm.minutesSinceMidnight})}){Icon(Icons.Filled.DeleteOutline,"Delete alarm",tint=Color(0xFFE05D88))}}}}}}
            OutlinedButton(onClick={editingAlarm=null;showTimePicker=true},modifier=Modifier.fillMaxWidth().height(58.dp),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.5.dp,com.lifeos.app.ui.theme.LifeOSSecondary.copy(alpha=.55f))){Icon(Icons.Filled.Add,null);Spacer(Modifier.width(8.dp));Text("Add new alarm",fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)}}
        if(alarms.isNotEmpty()&&Build.VERSION.SDK_INT>=Build.VERSION_CODES.S&&!exactAllowed)TextButton(onClick={context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply{data=android.net.Uri.parse("package:${context.packageName}")}))){Text("Allow exact alarm timing")}
    }}
    if(showTimePicker){val initial=editingAlarm?:DailyAlarm(6,0);val state=rememberTimePickerState(initialHour=initial.hour,initialMinute=initial.minute,is24Hour=false);AlertDialog(onDismissRequest={showTimePicker=false},title={Text(if(editingAlarm==null)"Add daily math alarm" else "Edit daily math alarm")},text={TimePicker(state)},confirmButton={TextButton(onClick={val picked=DailyAlarm(state.hour,state.minute);val updated=if(editingAlarm==null)alarms+picked else alarms.map{if(it.minutesSinceMidnight==editingAlarm!!.minutesSinceMidnight)picked else it};showTimePicker=false;editingAlarm=null;if(notificationPermission!=null&&!notificationPermission.isGranted)notificationPermission.request();saveAlarms(updated)}){Text("Save alarm")}},dismissButton={TextButton(onClick={showTimePicker=false;editingAlarm=null}){Text("Cancel")}})}}
