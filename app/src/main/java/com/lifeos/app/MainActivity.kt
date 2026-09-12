package com.lifeos.app

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.AppLockType
import com.lifeos.app.ui.navigation.LifeOSNavHost
import com.lifeos.app.ui.onboarding.OnboardingScreen
import com.lifeos.app.ui.security.AppLockScreen
import com.lifeos.app.ui.theme.LifeOSTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val serviceLocator = (application as LifeOSApplication).serviceLocator

        setContent {
            val darkTheme by serviceLocator.settingsStore.darkThemeEnabled.collectAsState(initial = false)
            val restoreScope = rememberCoroutineScope()
            var restoreStatus by remember { mutableStateOf<String?>(null) }
            var restoreInProgress by remember { mutableStateOf(false) }
            val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    restoreScope.launch {
                        restoreInProgress = true
                        restoreStatus = null
                        restoreStatus = runCatching {
                            contentResolver.openInputStream(uri)?.use { serviceLocator.backupRepository.importJson(it) }
                                ?: error("Backup file could not be opened")
                            serviceLocator.settingsStore.setOnboardingComplete(true)
                        }.fold(
                            { "Backup restored" },
                            { "Restore failed: ${it.message ?: "Invalid backup"}" }
                        )
                        restoreInProgress = false
                    }
                }
            }

            LifeOSTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalServiceLocator provides serviceLocator) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        OnboardingGate(
                            onRestoreBackup = { restoreLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) },
                            restoreStatus = restoreStatus,
                            restoreInProgress = restoreInProgress
                        ) { AppLockGate { LifeOSNavHost() } }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingGate(
    onRestoreBackup: () -> Unit,
    restoreStatus: String?,
    restoreInProgress: Boolean,
    content: @Composable () -> Unit
) {
    val locator = LocalServiceLocator.current
    val scope = rememberCoroutineScope()
    val onboardingComplete by locator.settingsStore.onboardingComplete.collectAsState(initial = false)
    if (onboardingComplete) {
        AnimatedVisibility(true, enter = fadeIn() + scaleIn(initialScale = .98f), label = "onboarding_gate") { content() }
    } else {
        OnboardingScreen(
            onFinish = { scope.launch { locator.settingsStore.setOnboardingComplete(true) } },
            onRestoreBackup = onRestoreBackup,
            restoreStatus = restoreStatus,
            restoreInProgress = restoreInProgress
        )
    }
}

@Composable
private fun AppLockGate(content: @Composable () -> Unit) {
    val locator = LocalServiceLocator.current
    val lockType by locator.settingsStore.appLockType.collectAsState(initial = AppLockType.NONE)
    var unlocked by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lockType) { unlocked = false }
    DisposableEffect(lifecycleOwner, lockType) {
        val observer = LifecycleEventObserver { _, event ->
            if (lockType != AppLockType.NONE && event == Lifecycle.Event.ON_STOP) {
                unlocked = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when {
        lockType == AppLockType.NONE -> content()
        unlocked -> content()
        else -> AppLockScreen(lockType = lockType, onUnlocked = { unlocked = true })
    }
}
