package com.lifeos.app.ui.security

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.AppLockType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class RecoveryStep { NONE, ANSWER_QUESTION, NEW_PIN, NEW_PIN_CONFIRM }

/**
 * Shown by MainActivity whenever the current AppLockType != NONE and the
 * session hasn't been unlocked yet. Handles both lock types plus a secure
 * "Forgot PIN?" recovery flow — resetting a PIN always requires answering
 * the stored recovery question first (Section 4: "must not instantly
 * disable security").
 */
@Composable
fun AppLockScreen(lockType: AppLockType, onUnlocked: () -> Unit) {
    val locator = LocalServiceLocator.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var pinInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var biometricReady by remember { mutableStateOf(activity?.let { locator.appLockManager.isBiometricAvailable() } == true) }
    var recoveryStep by remember { mutableStateOf(RecoveryStep.NONE) }
    var recoveryAnswerInput by remember { mutableStateOf("") }
    var recoveryQuestionText by remember { mutableStateOf<String?>(null) }
    var newPin by remember { mutableStateOf("") }
    var newPinConfirm by remember { mutableStateOf("") }

    LaunchedEffect(lockType, biometricReady) {
        if (lockType == AppLockType.BIOMETRIC && biometricReady && activity != null) {
            locator.appLockManager.authenticate(
                activity = activity,
                onSuccess = onUnlocked,
                onError = { message -> error = message }
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val pulse = rememberInfiniteTransition(label = "lock_pulse").animateFloat(1f, 1.06f, infiniteRepeatable(tween(850), RepeatMode.Reverse), label = "lock_scale")
        Icon(
            if (lockType == AppLockType.BIOMETRIC) Icons.Filled.Fingerprint else Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size((54f * pulse.value).dp).padding(bottom = 8.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text("LifeOS is locked", style = MaterialTheme.typography.titleLarge)

        when {
            recoveryStep == RecoveryStep.ANSWER_QUESTION -> {
                Text(recoveryQuestionText ?: "", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
                OutlinedTextField(
                    value = recoveryAnswerInput,
                    onValueChange = { recoveryAnswerInput = it },
                    label = { Text("Your answer") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)) }
                Button(
                    onClick = {
                        scope.launch {
                            if (locator.settingsStore.verifyRecoveryAnswer(recoveryAnswerInput)) {
                                error = null
                                newPin = ""
                                recoveryStep = RecoveryStep.NEW_PIN
                            } else {
                                error = "That doesn't match. Try again."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text("Verify") }
                TextButton(onClick = { recoveryStep = RecoveryStep.NONE; error = null }) { Text("Cancel") }
            }

            recoveryStep == RecoveryStep.NEW_PIN -> {
                Text("Choose a new PIN", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newPin = it },
                    label = { Text("New PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)) }
                Button(
                    onClick = {
                        if (newPin.length < 4) {
                            error = "PIN must be at least 4 digits."
                        } else {
                            error = null
                            newPinConfirm = ""
                            recoveryStep = RecoveryStep.NEW_PIN_CONFIRM
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text("Next") }
            }

            recoveryStep == RecoveryStep.NEW_PIN_CONFIRM -> {
                Text("Confirm your new PIN", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
                OutlinedTextField(
                    value = newPinConfirm,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newPinConfirm = it },
                    label = { Text("Re-enter PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)) }
                Button(
                    onClick = {
                        if (newPinConfirm != newPin) {
                            error = "PINs don't match. Try again."
                        } else {
                            scope.launch {
                                val existingQuestion = recoveryQuestionText ?: ""
                                locator.settingsStore.enablePinLock(newPin, existingQuestion, recoveryAnswerInput)
                                onUnlocked()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text("Reset PIN and unlock") }
            }

            lockType == AppLockType.PIN -> {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pinInput = it },
                    label = { Text("Enter PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)) }
                Button(
                    onClick = {
                        scope.launch {
                            if (locator.settingsStore.verifyPin(pinInput)) {
                                onUnlocked()
                            } else {
                                error = "Incorrect PIN."
                                pinInput = ""
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text("Unlock") }
                TextButton(
                    onClick = {
                        scope.launch {
                            val question = locator.settingsStore.recoveryQuestion.first()
                            if (question.isNullOrBlank()) {
                                error = "No recovery question was set up for this PIN."
                            } else {
                                recoveryQuestionText = question
                                recoveryAnswerInput = ""
                                error = null
                                recoveryStep = RecoveryStep.ANSWER_QUESTION
                            }
                        }
                    }
                ) { Text("Forgot PIN?") }
            }

            lockType == AppLockType.BIOMETRIC -> {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
                if (!biometricReady) {
                    Text(
                        "No strong biometric is enrolled on this phone yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Button(
                        onClick = {
                            activity?.let { act ->
                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                                        putExtra(
                                            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                                            BiometricManager.Authenticators.BIOMETRIC_STRONG
                                        )
                                    }
                                } else Intent(Settings.ACTION_SECURITY_SETTINGS)
                                act.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Set up fingerprint / face")
                    }
                    TextButton(
                        onClick = {
                            biometricReady = activity?.let { locator.appLockManager.isBiometricAvailable() } == true
                            if (!biometricReady) error = "Finish biometric setup on your phone, then try again."
                        }
                    ) { Text("Check again") }
                } else {
                    Button(
                        onClick = {
                            val act = activity
                            if (act != null) {
                                locator.appLockManager.authenticate(
                                    activity = act,
                                    onSuccess = onUnlocked,
                                    onError = { message -> error = message }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Unlock with biometrics")
                    }
                }
            }
        }
    }
}
