package com.lifeos.app.ui.capture

import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.lifeos.app.ui.components.LifeOSCard
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun PhotoPreview(filePath: String, modifier: Modifier = Modifier) {
    if (!File(filePath).exists()) {
        MissingFileNotice(modifier)
        return
    }
    AsyncImage(
        model = filePath,
        contentDescription = "Captured photo",
        modifier = modifier.fillMaxWidth().heightIn(max = 520.dp).clip(RoundedCornerShape(24.dp)),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun VideoPreview(filePath: String, modifier: Modifier = Modifier) {
    if (!File(filePath).exists()) {
        MissingFileNotice(modifier)
        return
    }
    var playing by remember(filePath) { mutableStateOf(false) }
    var fullscreen by remember(filePath) { mutableStateOf(false) }
    val thumbnail = rememberVideoThumbnail(filePath)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(24.dp)).background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (playing) {
                AndroidView(
                    Modifier.fillMaxSize(),
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            val controller = MediaController(ctx)
                            setMediaController(controller)
                            controller.setAnchorView(this)
                            setVideoURI(Uri.fromFile(File(filePath)))
                            setOnPreparedListener { it.start() }
                        }
                    }
                )
            } else {
                thumbnail?.let { Image(it, "Video preview", Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
            }

            Surface(
                onClick = { playing = true },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = .94f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (playing) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            IconButton(
                onClick = { fullscreen = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = .55f)) {
                    Icon(Icons.Filled.Fullscreen, "Fullscreen", tint = Color.White, modifier = Modifier.padding(8.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Videocam, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Tap play to watch · fullscreen supports landscape", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (fullscreen) VideoFullscreenViewer(filePath) { fullscreen = false }
}

@Composable
fun AudioPreview(filePath: String, modifier: Modifier = Modifier) {
    if (!File(filePath).exists()) {
        MissingFileNotice(modifier)
        return
    }
    var playing by remember(filePath) { mutableStateOf(false) }
    var position by remember(filePath) { mutableStateOf(0L) }
    var player by remember(filePath) { mutableStateOf<MediaPlayer?>(null) }
    val duration = rememberMediaDurationMs(filePath)

    DisposableEffect(filePath) {
        onDispose {
            player?.release()
            player = null
        }
    }
    LaunchedEffect(playing) {
        while (playing) {
            position = player?.currentPosition?.toLong() ?: position
            delay(250)
        }
    }

    LifeOSCard(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(2.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (playing) {
                    player?.pause()
                    playing = false
                } else {
                    val p = player ?: MediaPlayer().apply {
                        setDataSource(filePath)
                        prepare()
                        setOnCompletionListener { mp ->
                            playing = false
                            position = 0L
                            mp.seekTo(0)
                        }
                    }.also { player = it }
                    p.start()
                    playing = true
                }
            }) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pause" else "Play")
            }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text("Audio recording", style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(
                    progress = { if ((duration ?: 0L) > 0) position.toFloat() / (duration ?: 1L) else 0f },
                    modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(6.dp)
                )
                Text("${formatDurationMs(position)} / ${formatDurationMs(duration)}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

@Composable
private fun MissingFileNotice(modifier: Modifier = Modifier) {
    LifeOSCard(modifier.fillMaxWidth()) {
        Text("This media file is no longer available on this device.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
