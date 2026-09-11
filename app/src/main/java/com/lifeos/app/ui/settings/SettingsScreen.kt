package com.lifeos.app.ui.settings

import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.BuildConfig
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.AppLockType
import com.lifeos.app.core.util.NotificationHelper
import com.lifeos.app.core.util.SettingsStore
import com.lifeos.app.core.util.rememberPermissionState
import com.lifeos.app.data.repository.BackupRepository
import com.lifeos.app.ui.components.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsStore: SettingsStore, private val backupRepository: BackupRepository) : ViewModel() {
    val appLockType = settingsStore.appLockType
    val aiFeaturesEnabled = settingsStore.aiFeaturesEnabled
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status

    fun setAiFeaturesEnabled(enabled: Boolean) = viewModelScope.launch { settingsStore.setAiFeaturesEnabled(enabled) }

    fun exportBackup(context: Context, uri: android.net.Uri) = viewModelScope.launch {
        runCatching { context.contentResolver.openOutputStream(uri)?.use { backupRepository.exportJson(it, BuildConfig.VERSION_NAME) } ?: error("Storage location could not be opened") }
            .onSuccess { _status.value = "Backup saved to your selected location" }
            .onFailure { _status.value = "Export failed: ${it.message ?: "Unknown error"}" }
    }

    fun restoreBackup(context: Context, uri: android.net.Uri) = viewModelScope.launch {
        runCatching { context.contentResolver.openInputStream(uri)?.use { backupRepository.importJson(it) } ?: error("Backup file could not be opened") }
            .onSuccess { _status.value = "Backup restored successfully. Your local data is back in LifeOS." }
            .onFailure { _status.value = "Restore failed: ${it.message ?: "Invalid backup"}" }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenAppLockSetup: () -> Unit) {
    val locator = LocalServiceLocator.current
    val context = LocalContext.current
    val vm: SettingsViewModel = viewModel(factory = LambdaViewModelFactory { SettingsViewModel(locator.settingsStore, locator.backupRepository) })
    val lock by vm.appLockType.collectAsState(initial = AppLockType.NONE)
    val ai by vm.aiFeaturesEnabled.collectAsState(initial = false)
    val status by vm.status.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { vm.exportBackup(context, it) } }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.restoreBackup(context, it) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            LifeOSSectionHeader("Privacy & security", supportingText = "Keep LifeOS protected on this device")
            LifeOSCard(modifier = Modifier.animateContentSize()) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LifeOSIconBadge(if (lock == AppLockType.NONE) Icons.Filled.LockOpen else Icons.Filled.Lock)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("App Lock", style = MaterialTheme.typography.titleLarge)
                            Text(lockLabel(lock), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenAppLockSetup, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Security, null); Spacer(Modifier.width(6.dp)); Text("Change") }
                        if (lock != AppLockType.NONE) Button(onClick = { /* opens the same secure setup, where None is available */ onOpenAppLockSetup() }, modifier = Modifier.weight(1f)) { Text("Manage") }
                    }
                }
            }

            LifeOSSectionHeader("Daily rhythm", supportingText = "Make mornings intentional")
            MorningPhotoInfoCard()

            RemindersCard()
            LifeOSSectionHeader("Intelligence", supportingText = "Everything runs locally on this device")
            LifeOSCard {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LifeOSIconBadge(Icons.Filled.AutoAwesome)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text("LifeOS Intelligence", style = MaterialTheme.typography.titleLarge); Text("Offline analysis and answers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Switch(ai, vm::setAiFeaturesEnabled)
                    }
                    Text("Diary, tasks, habits, trends, patterns and reports are analyzed locally. No external AI API or cloud service is required.", style = MaterialTheme.typography.bodySmall)
                }
            }

            LifeOSSectionHeader("Your data", supportingText = "Use Android's document picker to choose exactly where a backup lives")
            LifeOSCard {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { LifeOSIconBadge(Icons.Filled.Folder); Spacer(Modifier.width(12.dp)); Column { Text("Backup & restore", style = MaterialTheme.typography.titleLarge); Text("JSON · local only · user controlled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    Text("Export opens Android's Save dialog so you can place the backup in Downloads or another local folder. Restore opens Android's file picker on the next installation.", style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { exportLauncher.launch("lifeos-backup.json") }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.FileDownload, null); Spacer(Modifier.width(6.dp)); Text("Export") }
                        OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Restore, null); Spacer(Modifier.width(6.dp)); Text("Restore") }
                    }
                    AnimatedVisibility(status != null) { status?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) } }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

}

private fun lockLabel(lock: AppLockType) = when (lock) {
    AppLockType.NONE -> "Off · LifeOS opens immediately"
    AppLockType.BIOMETRIC -> "Biometric · Android strong biometric"
    AppLockType.PIN -> "PIN · separate LifeOS PIN protection"
}

@Composable
private fun RemindersCard() {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS) else null
    val enabled = permission?.isGranted ?: true
    LifeOSCard {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LifeOSIconBadge(Icons.Filled.Notifications); Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text("Reminders", style = MaterialTheme.typography.titleLarge); Text("Task and habit notifications", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Switch(enabled, onCheckedChange = { if (it) { permission?.request?.invoke(); NotificationHelper.ensureChannel(context) } })
            }
            Text("Permission is requested only when you turn reminders on.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MorningPhotoInfoCard() {
    LifeOSCard {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            LifeOSIconBadge(Icons.Filled.WbSunny)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text("Morning check-in", style = MaterialTheme.typography.titleLarge); Text("LifeOS can suggest a morning photo on Home. It is saved only to your local Timeline.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
