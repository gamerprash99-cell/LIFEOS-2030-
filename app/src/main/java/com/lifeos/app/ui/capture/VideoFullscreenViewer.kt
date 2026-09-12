package com.lifeos.app.ui.capture

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VideoFullscreenViewer(filePath: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val previousOrientation = remember(activity) { activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    var player by remember(filePath) { mutableStateOf<VideoView?>(null) }
    var playing by remember(filePath) { mutableStateOf(false) }
    var duration by remember(filePath) { mutableIntStateOf(0) }
    var position by remember(filePath) { mutableIntStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE }
    DisposableEffect(Unit) {
        onDispose {
            player?.stopPlayback()
            activity?.requestedOrientation = previousOrientation
        }
    }
    LaunchedEffect(player, playing) {
        while (player != null) {
            position = player?.currentPosition ?: position
            duration = player?.duration?.takeIf { it > 0 } ?: duration
            playing = player?.isPlaying ?: playing
            delay(250)
        }
    }
    LaunchedEffect(controlsVisible, playing) {
        if (controlsVisible && playing) { delay(3200); controlsVisible = false }
    }

    BackHandler { onDismiss() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (playbackError == null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(-1, -1)
                            setBackgroundColor(android.graphics.Color.BLACK)
                            setVideoURI(Uri.fromFile(File(filePath)))
                            setOnPreparedListener { media -> duration = media.duration; media.start(); playing = true }
                            setOnCompletionListener { playing = false; position = duration; controlsVisible = true }
                            setOnErrorListener { _, _, _ -> playbackError = "This video could not be played on this device."; true }
                            player = this
                        }
                    },
                    update = { view -> player = view }
                )
            }

            Box(Modifier.matchParentSize().clickable { controlsVisible = !controlsVisible })

            AnimatedVisibility(visible = controlsVisible || playbackError != null, modifier = Modifier.fillMaxSize(), enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxSize()) {
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp)) {
                        Surface(shape = MaterialTheme.shapes.medium, color = Color.Black.copy(alpha = .62f)) { Icon(Icons.Filled.Close, "Close fullscreen video", tint = Color.White, modifier = Modifier.padding(10.dp)) }
                    }
                    Surface(color = Color.Black.copy(alpha = .58f), modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp), shape = MaterialTheme.shapes.medium) {
                        Text("LIFEOS · VIDEO", color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                    if (playbackError != null) {
                        Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Filled.ErrorOutline, null, tint = Color.White, modifier = Modifier.size(44.dp))
                            Text(playbackError!!, color = Color.White, style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = onDismiss) { Text("Close", color = Color.White) }
                        }
                    } else {
                        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            FilledIconButton(onClick = { seek(player, -10_000); controlsVisible = true }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = .72f))) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Replay10, "Back 10 seconds", tint = Color.White); Text("10", color = Color.White, style = MaterialTheme.typography.labelSmall) }
                            }
                            FilledIconButton(onClick = { player?.let { if (it.isPlaying) { it.pause(); playing = false } else { it.start(); playing = true } }; controlsVisible = true }, modifier = Modifier.size(72.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pause" else "Play", tint = Color.White, modifier = Modifier.size(34.dp))
                            }
                            FilledIconButton(onClick = { seek(player, 10_000); controlsVisible = true }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = .72f))) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Forward10, "Forward 10 seconds", tint = Color.White); Text("10", color = Color.White, style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 16.dp)) {
                            Slider(value = if (duration > 0) position.toFloat() / duration else 0f, onValueChange = { position = (it * duration).toInt() }, onValueChangeFinished = { player?.seekTo(position) })
                            Row(Modifier.fillMaxWidth()) { Text(formatDurationMs(position.toLong()), color = Color.White, style = MaterialTheme.typography.labelSmall); Spacer(Modifier.weight(1f)); Text(formatDurationMs(duration.toLong()), color = Color.White, style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
        }
    }
}

private fun seek(player: VideoView?, delta: Int) { player?.let { it.seekTo((it.currentPosition + delta).coerceIn(0, it.duration.coerceAtLeast(0))) } }
