package com.lifeos.app.ui.capture

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VideoFullscreenViewer(filePath: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val previousOrientation = remember(activity) { activity?.requestedOrientation }
    var player by remember(filePath) { mutableStateOf<VideoView?>(null) }
    var playing by remember(filePath) { mutableStateOf(false) }
    var duration by remember(filePath) { mutableStateOf(0) }
    var position by remember(filePath) { mutableStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.let { previousOrientation?.let(it::setRequestedOrientation) }
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
        if (controlsVisible && playing) {
            delay(3500)
            controlsVisible = false
        }
    }

    BackHandler { onDismiss() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                Modifier.fillMaxSize(),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(-1, -1)
                        setBackgroundColor(android.graphics.Color.BLACK)
                        setVideoURI(Uri.fromFile(File(filePath)))
                        setOnPreparedListener { media ->
                            duration = media.duration
                            media.start()
                            playing = true
                        }
                        setOnCompletionListener { playing = false; position = duration }
                        player = this
                    }
                },
                update = { player = it }
            )

            Box(Modifier.matchParentSize().clickable { controlsVisible = !controlsVisible })

            AnimatedVisibility(visible = controlsVisible, modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize()) {
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp)) {
                        Surface(shape = MaterialTheme.shapes.medium, color = Color.Black.copy(alpha = .58f)) {
                            Icon(Icons.Filled.Close, "Close", tint = Color.White, modifier = Modifier.padding(10.dp))
                        }
                    }
                    Surface(
                        color = Color.Black.copy(alpha = .62f),
                        modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp),
                        shape = MaterialTheme.shapes.medium
                    ) { Text("LifeOS · Video", color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) }

                    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        FilledIconButton(onClick = { seek(player, -10_000); controlsVisible = true }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = .7f))) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Replay10, "Back 10 seconds", tint = Color.White); Text("10", color = Color.White, style = MaterialTheme.typography.labelSmall) }
                        }
                        FilledIconButton(onClick = { player?.let { if (it.isPlaying) { it.pause(); playing = false } else { it.start(); playing = true } }; controlsVisible = true }, modifier = Modifier.size(70.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pause" else "Play", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp))
                        }
                        FilledIconButton(onClick = { seek(player, 10_000); controlsVisible = true }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = .7f))) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Forward10, "Forward 10 seconds", tint = Color.White); Text("10", color = Color.White, style = MaterialTheme.typography.labelSmall) }
                        }
                    }

                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 16.dp)) {
                        Slider(value = if (duration > 0) position.toFloat() / duration else 0f, onValueChange = { value -> position = (value * duration).toInt() }, onValueChangeFinished = { player?.seekTo(position) }, modifier = Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(formatDurationMs(position.toLong()), color = Color.White, style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.weight(1f))
                            Text(formatDurationMs(duration.toLong()), color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

private fun seek(player: VideoView?, delta: Int) {
    player?.let { it.seekTo((it.currentPosition + delta).coerceIn(0, it.duration.coerceAtLeast(0))) }
}

