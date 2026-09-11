package com.lifeos.app.ui.capture

import android.os.Bundle
import android.view.WindowManager
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.theme.LifeOSTheme
import java.io.File

class VideoFullscreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val path = intent.getStringExtra(EXTRA_PATH)
        if (path.isNullOrBlank() || !File(path).exists()) { finish(); return }
        setContent { LifeOSTheme { VideoFullscreenScreen(path = path, onClose = { finish() }) } }
    }

    companion object { const val EXTRA_PATH = "video_path" }
}

@Composable
private fun VideoFullscreenScreen(path: String, onClose: () -> Unit) {
    var player by remember { mutableStateOf<VideoView?>(null) }
    var playing by remember { mutableStateOf(false) }
    var duration by remember { mutableIntStateOf(0) }
    var position by remember { mutableIntStateOf(0) }
    var controls by remember { mutableStateOf(true) }

    LaunchedEffect(player, playing) {
        while (player != null) {
            position = player?.currentPosition ?: position
            duration = player?.duration?.takeIf { it > 0 } ?: duration
            delayForControls(playing)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setVideoPath(path)
                    setOnPreparedListener { media -> duration = media.duration; media.start(); playing = true }
                    setOnCompletionListener { playing = false; position = duration }
                    player = this
                }
            },
            update = { player = it }
        )

        if (controls) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.ArrowBack, "Close video", tint = Color.White) }
                Surface(color = Color.Black.copy(alpha = .5f), shape = MaterialTheme.shapes.medium) { Text("LifeOS · Video", color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) }
            }
            Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(onClick = { seek(player, -10_000); controls = true }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = .7f))) { Icon(Icons.Filled.Replay10, "Back 10 seconds", tint = Color.White) }
                FilledIconButton(onClick = { player?.let { if (it.isPlaying) { it.pause(); playing = false } else { it.start(); playing = true } }; controls = true }, modifier = Modifier.size(68.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) { Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play or pause", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp)) }
                FilledIconButton(onClick = { seek(player, 10_000); controls = true }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = .7f))) { Icon(Icons.Filled.Forward10, "Forward 10 seconds", tint = Color.White) }
            }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 16.dp)) {
                Slider(value = if (duration > 0) position.toFloat() / duration else 0f, onValueChange = { position = (it * duration).toInt() }, onValueChangeFinished = { player?.seekTo(position) })
                Row(Modifier.fillMaxWidth()) {
                    Text(formatDurationMs(position.toLong()), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.weight(1f))
                    Text(formatDurationMs(duration.toLong()), color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.matchParentSize().then(Modifier))
    }
}

private suspend fun delayForControls(playing: Boolean) {
    if (playing) kotlinx.coroutines.delay(250) else kotlinx.coroutines.delay(500)
}

private fun seek(player: VideoView?, delta: Int) {
    player?.let { it.seekTo((it.currentPosition + delta).coerceIn(0, it.duration.coerceAtLeast(0))) }
}
