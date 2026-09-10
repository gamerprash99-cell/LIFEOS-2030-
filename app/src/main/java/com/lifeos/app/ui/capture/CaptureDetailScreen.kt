package com.lifeos.app.ui.capture

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.*
import com.lifeos.app.data.repository.CaptureRepository
import com.lifeos.app.ui.components.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CaptureDetailViewModel(private val captureRepository: CaptureRepository, private val captureId: String): ViewModel() {
    private val _capture = MutableStateFlow<CaptureEntity?>(null)
    val capture: StateFlow<CaptureEntity?> = _capture
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted
    fun load() = viewModelScope.launch { _capture.value = captureRepository.getById(captureId); _loaded.value = true }
    fun delete() = viewModelScope.launch { captureRepository.delete(captureId); _deleted.value = true }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureDetailScreen(captureId: String, onBack: () -> Unit) {
    val locator = LocalServiceLocator.current
    val vm: CaptureDetailViewModel = viewModel(factory = LambdaViewModelFactory { CaptureDetailViewModel(locator.captureRepository, captureId) })
    LaunchedEffect(captureId) { vm.load() }
    val capture by vm.capture.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val deleted by vm.deleted.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(deleted) { if (deleted) onBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(captureTitle(capture?.type)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                actions = { if (capture != null) IconButton(onClick = { confirm = true }) { Icon(Icons.Filled.Delete, "Delete") } }
            )
        }
    ) { padding ->
        when {
            !loaded -> LifeOSLoadingState(Modifier.fillMaxSize().padding(padding))
            capture == null -> LifeOSEmptyState("Capture not found", "It may have already been deleted.", Modifier.fillMaxWidth().padding(padding).padding(20.dp))
            else -> {
                val item = capture!!
                Column(
                    Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LifeOSCard {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Memory", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            item.filePath?.let { path ->
                                when (item.type) {
                                    CaptureType.PHOTO -> PhotoPreview(path)
                                    CaptureType.VIDEO -> VideoPreview(path)
                                    CaptureType.AUDIO -> AudioPreview(path)
                                    CaptureType.THOUGHT -> Unit
                                }
                            }
                        }
                    }
                    LifeOSCard {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("Captured", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(DateTimeUtils.formatFullDate(DateTimeUtils.epochDayToLocalDate(item.dateEpochDay)), style = MaterialTheme.typography.titleLarge)
                            Text(DateTimeUtils.formatMinutes(item.timeMinutes), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            item.caption?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp)) }
                        }
                    }
                    Text("Saved locally in your LifeOS Timeline.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(110.dp))
                }
            }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Delete this capture?") },
        text = { Text("The local file and its Timeline record will be removed. This can't be undone.") },
        confirmButton = { TextButton(onClick = { confirm = false; vm.delete() }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
    )
}

private fun captureTitle(type: CaptureType?): String = when (type) {
    CaptureType.PHOTO -> "Photo"
    CaptureType.VIDEO -> "Video"
    CaptureType.AUDIO -> "Audio"
    CaptureType.THOUGHT -> "Thought"
    null -> "Capture"
}
