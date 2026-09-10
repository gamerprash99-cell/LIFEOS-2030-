package com.lifeos.app.ui.settings

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.util.NotificationHelper
import com.lifeos.app.ui.theme.LifeOSTheme
import kotlin.random.Random

private data class MathProblem(val a: Int, val b: Int, val add: Boolean) {
    val answer: Int get() = if (add) a + b else a - b
    val text: String get() = "$a ${if (add) "+" else "−"} $b = ?"
}

class AlarmChallengeActivity : ComponentActivity() {
    private var player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        startAlarmSound()
        NotificationHelper.cancelAlarmNotification(this)
        setContent {
            LifeOSTheme {
                AlarmChallengeScreen(onSolved = ::finishAlarm)
            }
        }
    }

    private fun startAlarmSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: Settings.System.DEFAULT_ALARM_ALERT_URI
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setDataSource(this@AlarmChallengeActivity, uri)
                isLooping = true
                prepare()
                start()
            }
        }.getOrNull()
    }

    private fun finishAlarm() {
        player?.stop(); player?.release(); player = null
        NotificationHelper.cancelAlarmNotification(this)
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        player?.release(); player = null
        super.onDestroy()
    }
}

@Composable
private fun AlarmChallengeScreen(onSolved: () -> Unit) {
    var problem by remember { mutableStateOf(newProblem()) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(if (input.isEmpty()) 0f else (input.length.coerceAtMost(2) / 2f), label = "answer_progress")

    BackHandler(enabled = true) { /* Alarm can only be stopped by a correct answer. */ }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(84.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Alarm, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.height(24.dp))
            Text("Good morning", style = MaterialTheme.typography.headlineLarge)
            Text("Solve this to stop your alarm", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            LifeOSMathCard(problem.text)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) { input = it; error = false } },
                label = { Text("Answer") },
                leadingIcon = { Icon(Icons.Filled.Calculate, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp))
            if (error) Text("Not quite. Try again.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(14.dp))
            Button(
                enabled = input.isNotBlank(),
                onClick = {
                    if (input.toIntOrNull() == problem.answer) {
                        onSolved()
                    } else {
                        problem = newProblem(); input = ""; error = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) { Icon(Icons.Filled.Lock, null); Spacer(Modifier.width(8.dp)); Text("Stop alarm") }
            Text("Every alarm gets a fresh + or − problem under 99.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun LifeOSMathCard(text: String) {
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp), modifier = Modifier.fillMaxWidth()) {
        AnimatedContent(text, label = "math_problem") { current ->
            Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { Text(current, style = MaterialTheme.typography.displaySmall) }
        }
    }
}

private fun newProblem(): MathProblem {
    repeat(20) {
        val a = Random.nextInt(1, 99)
        val b = Random.nextInt(1, 99)
        val add = Random.nextBoolean()
        val answer = if (add) a + b else a - b
        if (answer in 0..98) return MathProblem(a, b, add)
    }
    return MathProblem(12, 7, true)
}
