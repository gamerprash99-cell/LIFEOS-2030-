package com.lifeos.app.ui.capture

import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.lifeos.app.core.util.*

@Composable
fun VideoCaptureScreen(onCaptured:(String)->Unit,onCancel:()->Unit){
    val context=LocalContext.current; val camera=rememberPermissionState(LifeOSPermissions.CAMERA); val audio=rememberPermissionState(LifeOSPermissions.RECORD_AUDIO)
    LaunchedEffect(Unit){if(!camera.isGranted&&camera.status==PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE)camera.request();if(!audio.isGranted&&audio.status==PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE)audio.request()}
    if(!camera.isGranted||!audio.isGranted){Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text("Camera and microphone access are needed only when you record a video.",style=MaterialTheme.typography.bodyMedium);Button(onClick={if(!camera.isGranted)camera.request();if(!audio.isGranted)audio.request()},modifier=Modifier.padding(top=12.dp)){Text("Allow access")};TextButton(onClick=onCancel){Text("Cancel")}};return}
    val owner=LocalLifecycleOwner.current;val recorder=remember{Recorder.Builder().build()};val capture=remember{VideoCapture.withOutput(recorder)};var recording by remember{mutableStateOf<Recording?>(null)};var active by remember{mutableStateOf(false)};var seconds by remember{mutableStateOf(0)}
    LaunchedEffect(active){seconds=0;while(active){kotlinx.coroutines.delay(1000);seconds++}}
    DisposableEffect(Unit){onDispose{recording?.stop()}}
    Box(Modifier.fillMaxSize().background(Color.Black)){
        AndroidView(factory={ctx->PreviewView(ctx).also{view->val future=ProcessCameraProvider.getInstance(ctx);future.addListener({val provider=future.get();val preview=Preview.Builder().build().also{it.setSurfaceProvider(view.surfaceProvider)};runCatching{provider.unbindAll();provider.bindToLifecycle(owner,CameraSelector.DEFAULT_BACK_CAMERA,preview,capture)}.onFailure{Toast.makeText(ctx,"Camera error: ${it.message}",Toast.LENGTH_SHORT).show()}},ContextCompat.getMainExecutor(ctx))}},modifier=Modifier.fillMaxSize())
        Row(Modifier.align(Alignment.TopCenter).padding(top=16.dp).statusBarsPadding(),verticalAlignment=Alignment.CenterVertically){Surface(shape=MaterialTheme.shapes.medium,color=Color.Black.copy(alpha=.55f)){Row(Modifier.padding(horizontal=14.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Filled.Videocam,null,tint=Color.White);Spacer(Modifier.width(7.dp));Text(if(active)"Recording · ${seconds}s" else "Video",color=Color.White)}}}
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(20.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onCancel){Surface(shape=CircleShape,color=Color.Black.copy(alpha=.55f)){Icon(Icons.Filled.Close,"Cancel",tint=Color.White,modifier=Modifier.padding(10.dp))}};FilledIconButton(onClick={if(!active){val file=MediaStorage.newVideoFile(context);val output=FileOutputOptions.Builder(file).build();recording=capture.output.prepareRecording(context,output).withAudioEnabled().start(ContextCompat.getMainExecutor(context)){event->when(event){is VideoRecordEvent.Start->active=true;is VideoRecordEvent.Finalize->{active=false;recording=null;if(!event.hasError())onCaptured(file.absolutePath)else Toast.makeText(context,"Recording failed: ${event.cause?.message}",Toast.LENGTH_SHORT).show()}}}}else{recording?.stop()}},modifier=Modifier.size(76.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=if(active)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)){Icon(if(active)Icons.Filled.Stop else Icons.Filled.Videocam,if(active)"Stop recording" else "Record video",modifier=Modifier.size(32.dp))};Spacer(Modifier.size(48.dp))}
    }
}
