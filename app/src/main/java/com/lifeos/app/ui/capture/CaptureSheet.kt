package com.lifeos.app.ui.capture

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.theme.*
import kotlinx.coroutines.launch

private enum class CaptureMode { MENU, PHOTO, VIDEO, AUDIO, CONFIRM }
private data class JustCaptured(val type: CaptureType, val filePath: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(onDismiss: () -> Unit) {
    val locator = LocalServiceLocator.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(CaptureMode.MENU) }
    var thought by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("🌱 Gratitude") }
    var captured by remember { mutableStateOf<JustCaptured?>(null) }

    fun save(type: CaptureType, path: String?, caption: String?, confirm: Boolean) {
        scope.launch {
            val day = DateTimeUtils.today(); val time = java.time.LocalTime.now()
            locator.captureRepository.addCapture(type, path, caption, day.toEpochDay(), time.hour * 60 + time.minute)
            if (confirm) { captured = JustCaptured(type, path); mode = CaptureMode.CONFIRM } else onDismiss()
        }
    }

    if (mode != CaptureMode.MENU) {
        Dialog(onDismissRequest={if(mode==CaptureMode.CONFIRM)onDismiss()else mode=CaptureMode.MENU}, properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when(mode) {
                    CaptureMode.PHOTO -> CameraCaptureScreen({ save(CaptureType.PHOTO,it,null,true) }, { mode=CaptureMode.MENU })
                    CaptureMode.VIDEO -> VideoCaptureScreen({ save(CaptureType.VIDEO,it,null,true) }, { mode=CaptureMode.MENU })
                    CaptureMode.AUDIO -> AudioCaptureScreen({ save(CaptureType.AUDIO,it,null,true) }, { mode=CaptureMode.MENU })
                    CaptureMode.CONFIRM -> {
                        val c=captured
                        Column(Modifier.fillMaxSize().navigationBarsPadding().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                            LifeOSCard{Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(48.dp).background(Brush.linearGradient(LifeOSPrimaryGradient),RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center){Icon(Icons.Filled.Check,null,tint=Color.White)};Spacer(Modifier.width(12.dp));Column{Text("Saved to Timeline",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("Your memory is safe on this device.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}};c?.filePath?.let{when(c.type){CaptureType.PHOTO->PhotoPreview(it);CaptureType.VIDEO->VideoPreview(it);CaptureType.AUDIO->AudioPreview(it);else->Unit}};Text("🔒 Original file stays on-device.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Button(onClick=onDismiss,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Text("Done")}}}
                        }
                    }
                    else -> Unit
                }
            }
        }
        return
    }

    ModalBottomSheet(onDismissRequest=onDismiss,containerColor=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(topStart=32.dp,topEnd=32.dp),dragHandle={BottomSheetDefaults.DragHandle()}) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=22.dp).padding(bottom=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically){Surface(shape=RoundedCornerShape(999.dp),color=Color(0xFFFFEAF3),border=BorderStroke(1.dp,Color(0xFFFFCFE0))){Text("🌸 QUICK CAPTURE ✨",color=Color(0xFF9C39D8),fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=13.dp,vertical=8.dp))};Spacer(Modifier.weight(1f));IconButton(onClick=onDismiss){Icon(Icons.Filled.Close,"Close",tint=LifeOSPrimary)}}
            Text("Capture a moment ✨",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold)
            Text("Save a thought, photo, video or audio to your Timeline.",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value=thought,onValueChange={if(it.length<=280)thought=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Jot a quick thought, gratitude, or mood…")},minLines=5,maxLines=6,shape=RoundedCornerShape(24.dp))
            Row(verticalAlignment=Alignment.CenterVertically){listOf("🌱 Gratitude","💡 Idea","☕ Moment").forEach{value->FilterChip(selected=tag==value,onClick={tag=value},label={Text(value)},modifier=Modifier.padding(end=6.dp))};Spacer(Modifier.weight(1f));Text("😊 ${thought.length}/280",color=MaterialTheme.colorScheme.onSurfaceVariant)}
            Button(enabled=thought.isNotBlank(),onClick={save(CaptureType.THOUGHT,null,thought,true)},modifier=Modifier.fillMaxWidth().height(58.dp),shape=RoundedCornerShape(20.dp),colors=ButtonDefaults.buttonColors(containerColor=LifeOSPrimary)){Icon(Icons.Filled.Save,null);Spacer(Modifier.width(9.dp));Text("Save thought ✨",fontWeight=FontWeight.Bold)}
            Row(verticalAlignment=Alignment.CenterVertically){Text("OR ATTACH MEDIA",color=LifeOSPrimary,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text("View Gallery ↗",color=LifeOSPrimary,fontWeight=FontWeight.SemiBold)}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){CaptureTypeButton("📷","Photo","Instant 📸",Color(0xFFFFEEF5)){mode=CaptureMode.PHOTO};CaptureTypeButton("🎥","Video","Clip 🎬",Color(0xFFF2EDFF)){mode=CaptureMode.VIDEO};CaptureTypeButton("🎙","Audio","Voice 🎙",Color(0xFFEDF4FF)){mode=CaptureMode.AUDIO}}
        }
    }
}

@Composable private fun RowScope.CaptureTypeButton(icon:String,title:String,caption:String,bg:Color,onClick:()->Unit){Surface(onClick=onClick,shape=RoundedCornerShape(22.dp),color=bg,border=BorderStroke(1.dp,LifeOSLavender),modifier=Modifier.weight(1f).height(166.dp)){Column(Modifier.fillMaxSize().padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Surface(shape=CircleShape,color=Color.White,modifier=Modifier.size(58.dp)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(icon,style=MaterialTheme.typography.headlineSmall)}};Spacer(Modifier.height(9.dp));Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text(caption,style=MaterialTheme.typography.labelSmall,color=LifeOSPrimary)}}}
