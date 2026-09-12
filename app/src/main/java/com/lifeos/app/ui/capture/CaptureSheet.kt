package com.lifeos.app.ui.capture

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.ui.components.LifeOSCard
import kotlinx.coroutines.launch

private enum class CaptureMode { MENU, PHOTO, VIDEO, AUDIO, CONFIRM }
private data class JustCaptured(val type: CaptureType, val filePath: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(onDismiss: () -> Unit) {
    val locator = LocalServiceLocator.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(CaptureMode.MENU) }
    var thought by remember { mutableStateOf("") }
    var captured by remember { mutableStateOf<JustCaptured?>(null) }

    fun save(type: CaptureType, path: String?, caption: String?, confirm: Boolean) {
        scope.launch {
            val today = DateTimeUtils.today()
            val time = java.time.LocalTime.now()
            locator.captureRepository.addCapture(
                type,
                path,
                caption,
                today.toEpochDay(),
                time.hour * 60 + time.minute
            )
            if (confirm) {
                captured = JustCaptured(type, path)
                mode = CaptureMode.CONFIRM
            } else {
                onDismiss()
            }
        }
    }

    // Photo, video and audio are immersive capture experiences. Audio uses the
    // same full-screen surface so it never opens as a half-height sheet.
    if (mode == CaptureMode.PHOTO || mode == CaptureMode.VIDEO || mode == CaptureMode.AUDIO || mode == CaptureMode.CONFIRM) {
        Dialog(
            onDismissRequest = { if (mode == CaptureMode.CONFIRM) onDismiss() else mode = CaptureMode.MENU },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(Modifier.fillMaxSize()) {
                when (mode) {
                    CaptureMode.PHOTO -> CameraCaptureScreen(
                        onCaptured = { save(CaptureType.PHOTO, it, null, true) },
                        onCancel = { mode = CaptureMode.MENU }
                    )
                    CaptureMode.VIDEO -> VideoCaptureScreen(
                        onCaptured = { save(CaptureType.VIDEO, it, null, true) },
                        onCancel = { mode = CaptureMode.MENU }
                    )
                    CaptureMode.AUDIO -> AudioCaptureScreen(
                        onCaptured = { save(CaptureType.AUDIO, it, null, true) },
                        onCancel = { mode = CaptureMode.MENU }
                    )
                    CaptureMode.CONFIRM -> {
                        val c = captured
                        Column(
                            Modifier.fillMaxSize().navigationBarsPadding().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            LifeOSCard {
                                Column(
                                    Modifier.fillMaxWidth().padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text("Saved to Timeline", style = MaterialTheme.typography.titleLarge)
                                            Text(
                                                when (c?.type) {
                                                    CaptureType.PHOTO -> "Photo ready"
                                                    CaptureType.VIDEO -> "Video ready"
                                                    CaptureType.AUDIO -> "Audio ready"
                                                    else -> "Thought saved"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    c?.filePath?.let {
                                        when (c.type) {
                                            CaptureType.PHOTO -> PhotoPreview(it)
                                            CaptureType.VIDEO -> VideoPreview(it)
                                            CaptureType.AUDIO -> AudioPreview(it)
                                            else -> Unit
                                        }
                                    }
                                    Text("Your original file stays on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Capture a moment", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Save a thought, photo, video or audio to your Timeline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = thought,
                onValueChange = { thought = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Jot a quick thought…") },
                minLines = 2,
                maxLines = 4
            )
            Button(
                enabled = thought.isNotBlank(),
                onClick = { save(CaptureType.THOUGHT, null, thought, true) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Save thought")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CaptureTypeButton(Icons.Filled.CameraAlt, "Photo") { mode = CaptureMode.PHOTO }
                CaptureTypeButton(Icons.Filled.Videocam, "Video") { mode = CaptureMode.VIDEO }
                CaptureTypeButton(Icons.Filled.Mic, "Audio") { mode = CaptureMode.AUDIO }
            }
        }
    }

}

@Composable
private fun RowScope.CaptureTypeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(88.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(5.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
