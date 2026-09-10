package com.lifeos.app.ui.capture

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File

@Composable
fun VideoFullscreenViewer(filePath:String,onDismiss:()->Unit){
    val context=LocalContext.current; val activity=context as? Activity; val previous=activity?.requestedOrientation
    LaunchedEffect(Unit){activity?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE}
    DisposableEffect(Unit){onDispose{if(activity!=null&&previous!=null)activity.requestedOrientation=previous}}
    BackHandler{onDismiss()}
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)){
        Box(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()){
            AndroidView(Modifier.fillMaxSize(),factory={ctx->VideoView(ctx).apply{layoutParams=ViewGroup.LayoutParams(-1,-1);setBackgroundColor(android.graphics.Color.BLACK);val controller=MediaController(ctx);setMediaController(controller);controller.setAnchorView(this);setVideoURI(Uri.fromFile(File(filePath)));setOnPreparedListener{it.start()}}})
            Surface(shape=MaterialTheme.shapes.medium,color=Color.Black.copy(alpha=.55f),modifier=Modifier.align(Alignment.TopStart).padding(12.dp)){
                Row(Modifier.padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Filled.FullscreenExit,null,tint=Color.White);Spacer(Modifier.width(8.dp));Text("Landscape viewer",color=Color.White,style=MaterialTheme.typography.labelLarge)}
            }
            IconButton(onClick=onDismiss,modifier=Modifier.align(Alignment.TopEnd).padding(8.dp)){Surface(shape=MaterialTheme.shapes.medium,color=Color.Black.copy(alpha=.55f)){Icon(Icons.Filled.Close,"Close",tint=Color.White,modifier=Modifier.padding(10.dp))}}
        }
    }
}
