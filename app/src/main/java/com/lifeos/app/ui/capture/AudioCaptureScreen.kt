package com.lifeos.app.ui.capture

import android.media.MediaRecorder
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.util.LifeOSPermissions
import com.lifeos.app.core.util.MediaStorage
import com.lifeos.app.core.util.rememberPermissionState
import com.lifeos.app.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun AudioCaptureScreen(onCaptured:(String)->Unit,onCancel:()->Unit){
    val context=LocalContext.current; val permission=rememberPermissionState(LifeOSPermissions.RECORD_AUDIO)
    LaunchedEffect(Unit){if(permission.status==com.lifeos.app.core.util.PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE)permission.request()}
    if(!permission.isGranted){Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text("🎙",style=MaterialTheme.typography.displaySmall);Text("Microphone access",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("LifeOS needs the microphone only while you record an audio memory.",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(16.dp));Button(onClick=permission.request){Text("Grant permission")};TextButton(onClick=onCancel){Text("Cancel")}};return}
    var recording by remember{mutableStateOf(false)};var recorder by remember{mutableStateOf<MediaRecorder?>(null)};var output by remember{mutableStateOf<File?>(null)};var seconds by remember{mutableStateOf(0)}
    val pulse=rememberInfiniteTransition(label="audio").animateFloat(1f,1.08f,infiniteRepeatable(tween(900),RepeatMode.Reverse),label="pulse")
    fun start(){val file=MediaStorage.newAudioFile(context);output=file;val mr=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)MediaRecorder(context)else @Suppress("DEPRECATION") MediaRecorder();runCatching{mr.apply{setAudioSource(MediaRecorder.AudioSource.MIC);setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);setAudioEncoder(MediaRecorder.AudioEncoder.AAC);setOutputFile(file.absolutePath);prepare();start()}}.onSuccess{recorder=mr;recording=true}.onFailure{runCatching{mr.release()}}}
    fun stop(){val file=output;try{recorder?.stop()}catch(_:Exception){};runCatching{recorder?.release()};recorder=null;recording=false;if(file?.exists()==true&&file.length()>0L)onCaptured(file.absolutePath)}
    DisposableEffect(Unit){onDispose{runCatching{recorder?.release()}}};LaunchedEffect(recording){seconds=0;while(recording){delay(1000);seconds++}}
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFDF8FF),Color(0xFFFFF1FA)))).statusBarsPadding().navigationBarsPadding(),horizontalAlignment=Alignment.CenterHorizontally){
        Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onCancel){Icon(Icons.Filled.Close,"Close",tint=LifeOSPrimary)};Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Text("☁ Audio memory",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("• Saved locally to your Timeline ✨",color=LifeOSPrimary,style=MaterialTheme.typography.labelLarge)};Surface(shape=CircleShape,color=Color.White,border=BorderStroke(1.dp,LifeOSLavender),modifier=Modifier.size(48.dp)){Box(contentAlignment=Alignment.Center){Text("⚙")}}}
        Surface(shape=RoundedCornerShape(999.dp),color=Color.White,border=BorderStroke(1.dp,LifeOSLavender),modifier=Modifier.padding(top=26.dp)){Text("● ${String.format("%02d:%02d",seconds/60,seconds%60)}  ${if(recording)"RECORDING" else "READY"}",color=LifeOSPrimary,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=15.dp,vertical=8.dp))}
        Spacer(Modifier.weight(1f))
        Box(contentAlignment=Alignment.Center){if(recording)Box(Modifier.size(270.dp).scale(pulse.value).background(LifeOSPrimary.copy(alpha=.06f),CircleShape));Box(Modifier.size(240.dp).clip(CircleShape).border(1.dp,LifeOSLavender,CircleShape),contentAlignment=Alignment.Center){};Box(Modifier.size(150.dp).background(Brush.linearGradient(LifeOSPrimaryGradient),CircleShape),contentAlignment=Alignment.Center){Icon(if(recording)Icons.Filled.GraphicEq else Icons.Filled.Mic,null,tint=Color.White,modifier=Modifier.size(64.dp))}}
        Spacer(Modifier.height(28.dp));Text(if(recording) "Recording your voice…" else "Ready to record 🌸",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text(if(recording) "Tap again to save this memory" else "Tap the magic mic to capture your morning thoughts & sweet memories",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyLarge,modifier=Modifier.padding(horizontal=36.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp));Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){FilterChip(selected=true,onClick={},label={Text("💭 Mood: Dreamy")});FilterChip(selected=true,onClick={},label={Text("🟢 Crisp & cozy")})}
        Spacer(Modifier.weight(1f));Button(onClick={if(recording)stop()else start()},modifier=Modifier.fillMaxWidth().padding(horizontal=24.dp).height(62.dp),shape=RoundedCornerShape(22.dp),colors=ButtonDefaults.buttonColors(containerColor=LifeOSPrimary)){Icon(if(recording)Icons.Filled.Stop else Icons.Filled.Mic,null);Spacer(Modifier.width(9.dp));Text(if(recording)"Stop & Save ✦" else "Start Recording ✦",fontWeight=FontWeight.Bold)};Text("Audio notes are private & on-device  •  🔒 Encrypted",color=LifeOSPrimary.copy(alpha=.62f),style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=12.dp,bottom=6.dp))
    }
}
