package com.lifeos.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
            val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    restoreScope.launch {
                        restoreStatus = runCatching {
                            contentResolver.openInputStream(uri)?.use { serviceLocator.backupRepository.importJson(it) } ?: error("Backup file could not be opened")
                            serviceLocator.settingsStore.setOnboardingComplete(true)
                        }.fold({ "Backup restored" }, { "Restore failed: ${it.message ?: "Invalid backup"}" })
                    }
                }
            }

            LifeOSTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalServiceLocator provides serviceLocator) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        OnboardingGate(
                            onRestoreBackup = { restoreLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) },
                            restoreStatus = restoreStatus
                        ) { AppLockGate { LifeOSNavHost() } }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingGate(onRestoreBackup: () -> Unit, restoreStatus: String?, content: @Composable () -> Unit) {
    val locator = LocalServiceLocator.current
    val scope = rememberCoroutineScope()
    val onboardingComplete by locator.settingsStore.onboardingComplete.collectAsState(initial = false)
    if (onboardingComplete) {
        AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn(initialScale = 0.98f), label = "lifeos_startup") { content() }
    } else {
        OnboardingScreen(
            onFinish = { scope.launch { locator.settingsStore.setOnboardingComplete(true) } },
            onRestoreBackup = { onRestoreBackup() },
            restoreStatus = restoreStatus
        )
    }
}

@Composable
private fun AppLockGate(content: @Composable () -> Unit) {
    val locator = LocalServiceLocator.current
    val lockType by locator.settingsStore.appLockType.collectAsState(initial = AppLockType.NONE)
    var unlocked by remember { mutableStateOf(false) }
    when {
        lockType == AppLockType.NONE -> content()
        unlocked -> content()
        else -> AppLockScreen(lockType = lockType, onUnlocked = { unlocked = true })
    }
}
