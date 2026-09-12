@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lifeos.app

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.AppLockType
import com.lifeos.app.ui.navigation.LifeOSNavHost
import com.lifeos.app.ui.onboarding.OnboardingScreen
import com.lifeos.app.ui.security.AppLockScreen
import com.lifeos.app.ui.theme.LifeOSDarkHeroGradient
import com.lifeos.app.ui.theme.LifeOSPrimaryBright
import com.lifeos.app.ui.theme.LifeOSTheme
import kotlinx.coroutines.delay
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
                if (uri != null) restoreScope.launch {
                    restoreStatus = runCatching {
                        contentResolver.openInputStream(uri)?.use { serviceLocator.backupRepository.importJson(it) } ?: error("Backup file could not be opened")
                        serviceLocator.settingsStore.setOnboardingComplete(true)
                    }.fold({ "Backup restored" }, { "Restore failed: ${it.message ?: "Invalid backup"}" })
                }
            }
            LifeOSTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalServiceLocator provides serviceLocator) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        LifeOSStartup {
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
}

@Composable
private fun LifeOSStartup(content: @Composable () -> Unit) {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(700); showSplash = false }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(!showSplash, enter = fadeIn(tween(220)) + scaleIn(initialScale = .99f), label = "lifeos_content") { content() }
        AnimatedVisibility(showSplash, enter = fadeIn(tween(180)), exit = fadeOut(tween(220)), label = "lifeos_splash") { LifeOSSplash() }
    }
}

@Composable
private fun LifeOSSplash() {
    val transition = rememberInfiniteTransition(label = "lifeos_splash_pulse")
    val scale by transition.animateFloat(.96f, 1.04f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "splash_scale")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(LifeOSDarkHeroGradient)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(92.dp).scale(scale).background(Brush.radialGradient(listOf(Color(0xFFD8B4FE), LifeOSPrimaryBright, Color(0xFF4C1D95))), CircleShape), contentAlignment = Alignment.Center) { Text("✦", color = Color.White, style = MaterialTheme.typography.displaySmall) }
            Text("LIFEOS", color = Color.White, style = MaterialTheme.typography.displaySmall)
            Text("Capture your life. Understand your life.", color = Color.White.copy(alpha = .76f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun OnboardingGate(onRestoreBackup: () -> Unit, restoreStatus: String?, content: @Composable () -> Unit) {
    val locator = LocalServiceLocator.current
    val scope = rememberCoroutineScope()
    val onboardingComplete by locator.settingsStore.onboardingComplete.collectAsState(initial = false)
    if (onboardingComplete) AnimatedVisibility(true, enter = fadeIn() + scaleIn(initialScale = .98f), label = "onboarding_gate") { content() }
    else OnboardingScreen(onFinish = { scope.launch { locator.settingsStore.setOnboardingComplete(true) } }, onRestoreBackup = onRestoreBackup, restoreStatus = restoreStatus)
}

@Composable
private fun AppLockGate(content: @Composable () -> Unit) {
    val locator = LocalServiceLocator.current
    val lockType by locator.settingsStore.appLockType.collectAsState(initial = AppLockType.NONE)
    var unlocked by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lockType) { unlocked = false }
    DisposableEffect(lifecycleOwner, lockType) {
        val observer = LifecycleEventObserver { _, event -> if (lockType != AppLockType.NONE && event == Lifecycle.Event.ON_STOP) unlocked = false }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    when {
        lockType == AppLockType.NONE -> content()
        unlocked -> content()
        else -> AppLockScreen(lockType = lockType, onUnlocked = { unlocked = true })
    }
}
