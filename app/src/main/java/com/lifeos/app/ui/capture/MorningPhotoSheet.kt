package com.lifeos.app.ui.capture

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.CaptureType
import com.lifeos.app.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorningPhotoSheet(onDismiss: () -> Unit) {
    val locator = LocalServiceLocator.current
    val scope = rememberCoroutineScope()
    var savedPath by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (savedPath == null) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LifeOSIconBadge(Icons.Filled.WbSunny, Modifier.size(58.dp))
                Text("Morning check-in", style = MaterialTheme.typography.headlineSmall)
                Text("A quick photo becomes today's first Timeline memory. It stays on this device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.fillMaxWidth().heightIn(min = 420.dp, max = 620.dp)) {
                    CameraCaptureScreen(
                        onCaptured = { path ->
                            scope.launch {
                                val now = java.time.LocalTime.now()
                                locator.captureRepository.addCapture(
                                    type = CaptureType.PHOTO,
                                    filePath = path,
                                    caption = "Morning check-in",
                                    dateEpochDay = DateTimeUtils.today().toEpochDay(),
                                    timeMinutes = now.hour * 60 + now.minute
                                )
                                savedPath = path
                            }
                        },
                        onCancel = onDismiss
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LifeOSIconBadge(Icons.Filled.CheckCircle)
                Text("Morning memory saved", style = MaterialTheme.typography.headlineSmall)
                Text("Your photo is now in Timeline with today's date and time.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PhotoPreview(savedPath!!)
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}
