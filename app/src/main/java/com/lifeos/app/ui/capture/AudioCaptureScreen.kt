package com.lifeos.app.ui.capture

import android.media.MediaRecorder
import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.lifeos.app.core.util.LifeOSPermissions
import com.lifeos.app.core.util.MediaStorage
import com.lifeos.app.core.util.rememberPermissionState
import com.lifeos.app.ui.theme.LifeOSDarkHeroGradient
import com.lifeos.app.ui.theme.LifeOSPrimaryGradient
import kotlinx.coroutines.delay
import java.io.File

/** Full-screen local audio capture. Recording remains the existing MediaRecorder flow. */
@Composable
fun AudioCaptureScreen(onCaptured: (filePath: String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val micPermission = rememberPermissionState(LifeOSPermissions.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        if (micPermission.status == com.lifeos.app.core.util.PermissionStatus.NOT_YET_REQUESTED_OR_DENIABLE) micPermission.request()
    }

    if (!micPermission.isGranted) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(84.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Mic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) }
            }
            Spacer(Modifier.height(22.dp))
            Text("Microphone access", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                if (micPermission.status == com.lifeos.app.core.util.PermissionStatus.PERMANENTLY_DENIED) "Microphone access was denied. Enable it in Settings to record audio." else "LifeOS needs the microphone only while you record an audio memory.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))
            if (micPermission.status == com.lifeos.app.core.util.PermissionStatus.PERMANENTLY_DENIED) {
                Button(onClick = micPermission.openSettings) { Text("Open Settings") }
            } else {
                Button(onClick = micPermission.request) { Text("Grant permission") }
            }
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
        return
    }

    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    val infiniteTransition = rememberInfiniteTransition(label = "audio_recording")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "audio_pulse"
    )

    fun startRecording() {
        val file = MediaStorage.newAudioFile(context)
        outputFile = file
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        runCatching {
            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        }.onSuccess {
            recorder = mr
            isRecording = true
        }.onFailure {
            runCatching { mr.release() }
        }
    }

    fun stopRecording() {
        val file = outputFile
        try { recorder?.stop() } catch (_: Exception) { /* Too-short recordings are discarded below. */ }
        runCatching { recorder?.release() }
        recorder = null
        isRecording = false
        if (file?.exists() == true && file.length() > 0L) onCaptured(file.absolutePath)
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { recorder?.release() } }
    }

    LaunchedEffect(isRecording) {
        elapsedSeconds = 0
        while (isRecording) {
            delay(1000)
            elapsedSeconds++
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, "Close") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Audio memory", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Saved locally to your Timeline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.size(48.dp))
        }

        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center) {
            if (isRecording) {
                Box(Modifier.size(190.dp).scale(pulse).background(MaterialTheme.colorScheme.primary.copy(alpha = .10f), CircleShape))
            }
            Box(
                Modifier.size(142.dp).background(Brush.linearGradient(if (isRecording) LifeOSDarkHeroGradient else LifeOSPrimaryGradient), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (isRecording) Icons.Filled.GraphicEq else Icons.Filled.Mic, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(56.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            if (isRecording) String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60) else "Ready to record",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (isRecording) "Recording your voice…" else "Tap the microphone to capture a moment",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))

        Button(
            onClick = { if (isRecording) stopRecording() else startRecording() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic, null)
            Spacer(Modifier.width(8.dp))
            Text(if (isRecording) "Stop & Save" else "Start Recording", fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onCancel, modifier = Modifier.padding(bottom = 6.dp)) {
            Icon(Icons.Filled.Close, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Cancel")
        }
    }
}
