package com.lifeos.app.ui.security

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.ui.components.*
import kotlinx.coroutines.launch

private enum class SetupStep { CHOOSE, BIOMETRIC, PIN, CONFIRM_PIN, RECOVERY_QUESTION, RECOVERY_ANSWER, DONE }
private val questions = listOf(
    "What was the name of your first pet?",
    "What city were you born in?",
    "What was your childhood nickname?",
    "What's your favorite book?",
    "Custom question…"
)

@Composable
fun AppLockSetupScreen(onBack: () -> Unit) {
    val locator = LocalServiceLocator.current
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as? FragmentActivity
    var step by remember { mutableStateOf(SetupStep.CHOOSE) }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var question by remember { mutableStateOf(questions.first()) }
    var custom by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }

    val finalQuestion = if (question == "Custom question…") custom else question

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("App Lock") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedContent(targetState = step, label = "app_lock_setup_step") { current ->
                when (current) {
                    SetupStep.CHOOSE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LifeOSSectionHeader("Protect LifeOS", supportingText = "Choose how LifeOS should unlock on this device.")
                            LockOption(Icons.Filled.LockOpen, "None", "No extra LifeOS lock.") {
                                scope.launch { locator.settingsStore.disableAppLock(); step = SetupStep.DONE }
                            }
                            LockOption(Icons.Filled.Fingerprint, "Biometric", "Use Android's real fingerprint / strong biometric prompt.") {
                                error = null
                                step = SetupStep.BIOMETRIC
                            }
                            LockOption(Icons.Filled.Pin, "PIN", "Use a separate 4–6 digit LifeOS PIN with secure recovery.") {
                                pin = ""; confirm = ""; error = null; step = SetupStep.PIN
                            }
                        }
                    }
                    SetupStep.BIOMETRIC -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            LifeOSSectionHeader("Set up biometric unlock", supportingText = "Android will now show the same system biometric UI used to unlock protected apps.")
                            val pulse = rememberInfiniteTransition(label = "biometric_pulse").animateFloat(
                                1f, 1.08f,
                                infiniteRepeatable(tween(900), RepeatMode.Reverse),
                                label = "biometric_scale"
                            )
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                LifeOSIconBadge(Icons.Filled.Fingerprint, Modifier.size((82f * pulse.value).dp))
                            }
                            LifeOSCard(modifier = Modifier.animateContentSize()) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("What happens next", style = MaterialTheme.typography.titleMedium)
                                    Text("Tap Verify & enable. Android owns the biometric scan; LifeOS never sees your fingerprint or face data.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            Button(
                                onClick = {
                                    val act = activity
                                    if (act == null) {
                                        error = "Biometric verification is unavailable here."
                                    } else {
                                        locator.appLockManager.authenticate(
                                            act,
                                            onSuccess = { scope.launch { locator.settingsStore.enableBiometricLock(); step = SetupStep.DONE } },
                                            onError = { error = it },
                                            onFailed = { error = "Fingerprint not recognized. Try again." }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Fingerprint, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Verify & enable biometric")
                            }
                            TextButton(onClick = { error = null; step = SetupStep.CHOOSE }, Modifier.align(Alignment.CenterHorizontally)) { Text("Back") }
                        }
                    }
                    SetupStep.PIN -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LifeOSSectionHeader("Create your LifeOS PIN", supportingText = "Use 4–6 digits. This is separate from your phone PIN.")
                            OutlinedTextField(pin, { if (it.length <= 6 && it.all(Char::isDigit)) pin = it }, Modifier.fillMaxWidth(), label = { Text("New PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), singleLine = true)
                            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            Button(onClick = { if (pin.length < 4) error = "PIN must be 4–6 digits." else { error = null; step = SetupStep.CONFIRM_PIN } }, Modifier.fillMaxWidth()) { Text("Continue") }
                            TextButton(onClick = { step = SetupStep.CHOOSE }) { Text("Back") }
                        }
                    }
                    SetupStep.CONFIRM_PIN -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LifeOSSectionHeader("Confirm PIN")
                            OutlinedTextField(confirm, { if (it.length <= 6 && it.all(Char::isDigit)) confirm = it }, Modifier.fillMaxWidth(), label = { Text("Confirm PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), singleLine = true)
                            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            Button(onClick = { if (confirm != pin) error = "PINs do not match." else { error = null; step = SetupStep.RECOVERY_QUESTION } }, Modifier.fillMaxWidth()) { Text("Set recovery") }
                            TextButton(onClick = { step = SetupStep.PIN }) { Text("Back") }
                        }
                    }
                    SetupStep.RECOVERY_QUESTION -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LifeOSSectionHeader("Recovery question", supportingText = "Required so a forgotten PIN cannot simply disable App Lock.")
                            Box {
                                OutlinedButton(onClick = { menu = true }, Modifier.fillMaxWidth()) { Text(question, Modifier.weight(1f)); Icon(Icons.Filled.ArrowDropDown, null) }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    questions.forEach { q -> DropdownMenuItem(text = { Text(q) }, onClick = { question = q; menu = false }) }
                                }
                            }
                            if (question == "Custom question…") OutlinedTextField(custom, { custom = it }, Modifier.fillMaxWidth(), label = { Text("Custom question") })
                            Button(enabled = finalQuestion.isNotBlank(), onClick = { step = SetupStep.RECOVERY_ANSWER }, Modifier.fillMaxWidth()) { Text("Continue") }
                        }
                    }
                    SetupStep.RECOVERY_ANSWER -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LifeOSSectionHeader("Recovery answer", supportingText = "The answer is stored only as a salted hash.")
                            OutlinedTextField(answer, { answer = it }, Modifier.fillMaxWidth(), label = { Text("Your answer") }, singleLine = true)
                            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            Button(enabled = answer.isNotBlank(), onClick = { scope.launch { locator.settingsStore.enablePinLock(pin, finalQuestion, answer); step = SetupStep.DONE } }, Modifier.fillMaxWidth()) { Text("Enable PIN App Lock") }
                        }
                    }
                    SetupStep.DONE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            LifeOSCard(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { LifeOSIconBadge(Icons.Filled.CheckCircle); Text("App Lock updated", style = MaterialTheme.typography.headlineSmall); Text("Your protection settings are active.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                            Button(onClick = onBack, Modifier.fillMaxWidth()) { Text("Done") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockOption(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, onClick: () -> Unit) {
    LifeOSCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            LifeOSIconBadge(icon)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
