package com.lifeos.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.components.LifeOSIconBadge
import com.lifeos.app.ui.components.LifeOSCard

private data class OnboardingPage(val emoji: String, val title: String, val body: String)

private val PAGES = listOf(
    OnboardingPage("🧠", "Welcome to LifeOS", "Capture your life. Organize your life. Understand your life. Everything in one connected, local-first app."),
    OnboardingPage("🔒", "Your life. Your data.", "Notes, tasks, habits, expenses and diary entries stay on your device. Nothing is uploaded unless you explicitly export it."),
    OnboardingPage("🤖", "AI, on your terms", "Optional intelligence features run locally on your device. No external AI service is required."),
    OnboardingPage("🔗", "Everything connects", "Your Timeline weaves together notes, tasks, habits, expenses and diary entries by date and time — one continuous story of your life.")
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit, onRestoreBackup: (() -> Unit)? = null, restoreStatus: String? = null) {
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = PAGES[pageIndex]
    val isLast = pageIndex == PAGES.lastIndex

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(targetState = pageIndex, transitionSpec = { fadeIn(tween(220)) + slideInVertically(tween(260), initialOffsetY = { it / 8 }) }, label = "onboarding_page") { index ->
            val current = PAGES[index]
            LifeOSCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(current.emoji, style = MaterialTheme.typography.displayLarge)
                    Text(current.title, style = MaterialTheme.typography.headlineMedium)
                    Text(current.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            PAGES.indices.forEach { index ->
                Surface(shape = RoundedCornerShape(50), color = if (index == pageIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(if (index == pageIndex) 22.dp else 8.dp, 8.dp)) {}
            }
        }

        Column(Modifier.fillMaxWidth().padding(top = 26.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (isLast) onFinish() else pageIndex++ }, Modifier.fillMaxWidth()) { Text(if (isLast) "Get started" else "Next") }
            if (onRestoreBackup != null) {
                OutlinedButton(onClick = { onRestoreBackup?.invoke() }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Restore a LifeOS backup")
                }
                Text("Already used LifeOS? Restore your exported JSON backup here.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            if (!isLast) TextButton(onClick = onFinish, Modifier.fillMaxWidth()) { Text("Skip") }
            restoreStatus?.let { Text(it, color = if (it == "Backup restored") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally)) }
        }
    }
}
