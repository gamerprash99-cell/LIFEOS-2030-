package com.lifeos.app.ui.capture

import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.lifeos.app.core.util.*

@Composable
fun VideoCaptureScreen(onCaptured: (String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val camera = rememberPermissionState(LifeOSPermissions.CAMERA)
    val audio = rememberPermissionState(LifeOSPermissions.RECORD_AUDIO)
    LaunchedEffect(Unit) {
        if (!camera.isGranted && camera.status == PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE) camera.request()
        if (!audio.isGranted && audio.status == PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE) audio.request()
    }
    if (!camera.isGranted || !audio.isGranted) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Camera and microphone access are needed only when you record a video.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { if (!camera.isGranted) camera.request(); if (!audio.isGranted) audio.request() }, Modifier.padding(top = 12.dp)) { Text("Allow access") }
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
        return
    }

    val owner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val recorder = remember { Recorder.Builder().build() }
    val capture = remember { VideoCapture.withOutput(recorder) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var active by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var lens by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    val zoomRange = boundCamera?.cameraInfo?.zoomState?.value
    val maxZoom = zoomRange?.maxZoomRatio ?: 1f
    val minZoom = zoomRange?.minZoomRatio ?: 1f
    val zoomLevels = listOf(1f, 2f, 3f).filter { it in minZoom..maxZoom }.ifEmpty { listOf(1f.coerceIn(minZoom, maxZoom)) }

    LaunchedEffect(active) { seconds = 0; while (active) { kotlinx.coroutines.delay(1000); seconds++ } }
    DisposableEffect(Unit) { onDispose { recording?.stop() } }
    LaunchedEffect(lens) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching {
                val provider = future.get()
                val selector = CameraSelector.Builder().requireLensFacing(lens).build()
                provider.unbindAll()
                boundCamera = provider.bindToLifecycle(owner, selector, Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }, capture)
                zoom = 1f
            }.onFailure { Toast.makeText(context, "Camera error: ${it.message}", Toast.LENGTH_SHORT).show() }
        }, ContextCompat.getMainExecutor(context))
    }
    LaunchedEffect(boundCamera, zoom, minZoom, maxZoom) {
        val safeZoom = zoom.coerceIn(minZoom, maxZoom)
        if (safeZoom != zoom) zoom = safeZoom
        boundCamera?.cameraControl?.setZoomRatio(safeZoom)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize().pointerInput(boundCamera, minZoom, maxZoom) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    zoom = (zoom * zoomChange).coerceIn(minZoom, maxZoom)
                }
            }
        )
        Row(Modifier.fillMaxWidth().align(Alignment.TopStart).statusBarsPadding().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CaptureRoundButton(Icons.Filled.Close, "Close", onCancel)
            Spacer(Modifier.width(10.dp))
            Surface(shape = RoundedCornerShape(18.dp), color = Color.Black.copy(alpha = .55f)) { Row(Modifier.padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Videocam, null, tint = Color.White, modifier = Modifier.size(19.dp)); Spacer(Modifier.width(6.dp)); Text(if (active) "Recording · ${seconds}s" else "Video", color = Color.White) } }
        }
        ZoomSelector(zoom = zoom, levels = zoomLevels, onZoom = { zoom = it })
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            CaptureRoundButton(Icons.Filled.Cameraswitch, "Switch camera") { if (!active) lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK }
            FilledIconButton(onClick = {
                if (!active) {
                    val file = MediaStorage.newVideoFile(context)
                    val output = FileOutputOptions.Builder(file).build()
                    recording = capture.output.prepareRecording(context, output).withAudioEnabled().start(ContextCompat.getMainExecutor(context)) { event ->
                        when (event) {
                            is VideoRecordEvent.Start -> active = true
                            is VideoRecordEvent.Finalize -> { active = false; recording = null; if (!event.hasError()) onCaptured(file.absolutePath) else Toast.makeText(context, "Recording failed: ${event.cause?.message}", Toast.LENGTH_SHORT).show() }
                        }
                    }
                } else recording?.stop()
            }, modifier = Modifier.size(78.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)) {
                Icon(if (active) Icons.Filled.Stop else Icons.Filled.Videocam, if (active) "Stop recording" else "Record video", modifier = Modifier.size(34.dp))
            }
            CaptureRoundButton(Icons.Filled.FlipCameraAndroid, "Reset zoom") { if (!active) zoom = 1f.coerceIn(minZoom, maxZoom) }
        }
    }
}

@Composable
private fun ZoomSelector(zoom: Float, levels: List<Float>, onZoom: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 62.dp), horizontalArrangement = Arrangement.Center) {
        Surface(shape = RoundedCornerShape(22.dp), color = Color.Black.copy(alpha = .55f), modifier = Modifier.animateContentSize()) {
            Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                levels.forEach { level ->
                    val selected = kotlin.math.abs(zoom - level) < 0.05f
                    Surface(onClick = { onZoom(level) }, shape = CircleShape, color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, contentColor = Color.White, modifier = Modifier.size(42.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (level == 1f) "1×" else "${level.toInt()}×", style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureRoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    FilledIconButton(onClick = onClick, modifier = Modifier.size(52.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = .55f), contentColor = Color.White)) { Icon(icon, description, modifier = Modifier.size(25.dp)) }
}
