package com.lifeos.app.ui.capture

import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
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
import com.lifeos.app.core.util.LifeOSPermissions
import com.lifeos.app.core.util.MediaStorage
import com.lifeos.app.core.util.PermissionStatus
import com.lifeos.app.core.util.rememberPermissionState

@Composable
fun CameraCaptureScreen(onCaptured: (String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val permission = rememberPermissionState(LifeOSPermissions.CAMERA)
    LaunchedEffect(Unit) { if (permission.status == PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE) permission.request() }
    if (!permission.isGranted) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(if (permission.status == PermissionStatus.PERMANENTLY_DENIED) "Camera access is off. Enable it in Settings to capture a photo." else "Camera access is needed only for Photo capture.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = if (permission.status == PermissionStatus.PERMANENTLY_DENIED) permission.openSettings else permission.request, Modifier.padding(top = 12.dp)) { Text(if (permission.status == PermissionStatus.PERMANENTLY_DENIED) "Open Settings" else "Allow camera") }
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
        return
    }

    val owner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    var lens by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    val zoomRange = boundCamera?.cameraInfo?.zoomState?.value
    val maxZoom = zoomRange?.maxZoomRatio ?: 1f
    val minZoom = zoomRange?.minZoomRatio ?: 1f
    val zoomLevels = listOf(1f, 2f, 3f).filter { it in minZoom..maxZoom }.ifEmpty { listOf(1f.coerceIn(minZoom, maxZoom)) }

    LaunchedEffect(lens) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching {
                val provider = future.get()
                val selector = CameraSelector.Builder().requireLensFacing(lens).build()
                provider.unbindAll()
                boundCamera = provider.bindToLifecycle(owner, selector, Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }, imageCapture)
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
        CaptureTopBar(title = "Photo", onClose = onCancel)
        ZoomSelector(zoom = zoom, levels = zoomLevels, onZoom = { zoom = it })
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            CaptureRoundButton(Icons.Filled.Cameraswitch, "Switch camera") { lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK }
            FilledIconButton(onClick = {
                val file = MediaStorage.newPhotoFile(context)
                imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) = onCaptured(file.absolutePath)
                    override fun onError(e: ImageCaptureException) = Toast.makeText(context, "Capture failed: ${e.message}", Toast.LENGTH_SHORT).show()
                })
            }, modifier = Modifier.size(78.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) { Icon(Icons.Filled.Camera, "Take photo", modifier = Modifier.size(34.dp)) }
            CaptureRoundButton(Icons.Filled.FlipCameraAndroid, "Reset zoom") { zoom = 1f.coerceIn(minZoom, maxZoom) }
        }
    }
}

@Composable
private fun CaptureTopBar(title: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        CaptureRoundButton(Icons.Filled.Close, "Close", onClose)
        Spacer(Modifier.width(10.dp))
        Surface(shape = RoundedCornerShape(18.dp), color = Color.Black.copy(alpha = .55f)) { Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) }
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
