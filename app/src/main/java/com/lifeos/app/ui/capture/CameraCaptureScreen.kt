package com.lifeos.app.ui.capture

import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
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
import com.lifeos.app.core.util.LifeOSPermissions
import com.lifeos.app.core.util.MediaStorage
import com.lifeos.app.core.util.PermissionStatus
import com.lifeos.app.core.util.rememberPermissionState

@Composable
fun CameraCaptureScreen(onCaptured:(String)->Unit,onCancel:()->Unit){
    val context=LocalContext.current; val permission=rememberPermissionState(LifeOSPermissions.CAMERA)
    LaunchedEffect(Unit){if(permission.status==PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE)permission.request()}
    if(!permission.isGranted){Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(if(permission.status==PermissionStatus.PERMANENTLY_DENIED)"Camera access is off. Enable it in Settings to capture a photo." else "Camera access is needed only for Photo capture.",style=MaterialTheme.typography.bodyMedium);Button(onClick=if(permission.status==PermissionStatus.PERMANENTLY_DENIED)permission.openSettings else permission.request,modifier=Modifier.padding(top=12.dp)){Text(if(permission.status==PermissionStatus.PERMANENTLY_DENIED)"Open Settings" else "Allow camera")};TextButton(onClick=onCancel){Text("Cancel")}};return}
    val owner=LocalLifecycleOwner.current; val imageCapture=remember{ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()}
    Box(Modifier.fillMaxSize().background(Color.Black)){
        AndroidView(factory={ctx->PreviewView(ctx).also{view->val future=ProcessCameraProvider.getInstance(ctx);future.addListener({val provider=future.get();val preview=Preview.Builder().build().also{it.setSurfaceProvider(view.surfaceProvider)};runCatching{provider.unbindAll();provider.bindToLifecycle(owner,CameraSelector.DEFAULT_BACK_CAMERA,preview,imageCapture)}.onFailure{Toast.makeText(ctx,"Camera error: ${it.message}",Toast.LENGTH_SHORT).show()}},ContextCompat.getMainExecutor(ctx))}},modifier=Modifier.fillMaxSize())
        Surface(shape=MaterialTheme.shapes.medium,color=Color.Black.copy(alpha=.5f),modifier=Modifier.align(Alignment.TopStart).padding(16.dp)){Text("Photo",color=Color.White,style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(horizontal=14.dp,vertical=9.dp))}
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(20.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onCancel){Surface(shape=CircleShape,color=Color.Black.copy(alpha=.55f)){Icon(Icons.Filled.Close,"Cancel",tint=Color.White,modifier=Modifier.padding(10.dp))}};FilledIconButton(onClick={val file=MediaStorage.newPhotoFile(context);imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(),ContextCompat.getMainExecutor(context),object:ImageCapture.OnImageSavedCallback{override fun onImageSaved(output:ImageCapture.OutputFileResults){onCaptured(file.absolutePath)};override fun onError(e:ImageCaptureException){Toast.makeText(context,"Capture failed: ${e.message}",Toast.LENGTH_SHORT).show()}})},modifier=Modifier.size(72.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=MaterialTheme.colorScheme.primary)){Icon(Icons.Filled.Camera,"Take photo",modifier=Modifier.size(32.dp))};Spacer(Modifier.size(48.dp))}
    }
}
