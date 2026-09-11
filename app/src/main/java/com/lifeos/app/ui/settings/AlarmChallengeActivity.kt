package com.lifeos.app.ui.settings

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.util.NotificationHelper
import com.lifeos.app.ui.theme.LifeOSTheme
import kotlin.random.Random

private data class MathProblem(val a: Int, val b: Int, val add: Boolean) {
    val answer: Int get() = if (add) a + b else a - b
    val text: String get() = "$a ${if (add) "+" else "−"} $b"
}

class AlarmChallengeActivity : ComponentActivity() {
    private var player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        startAlarmSound()
        NotificationHelper.cancelAlarmNotification(this)
        setContent { LifeOSTheme { AlarmChallengeScreen(onSolved = ::finishAlarm) } }
    }

    private fun startAlarmSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: Settings.System.DEFAULT_ALARM_ALERT_URI
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
    var selected by remember { mutableStateOf<Int?>(null) }
    var wrong by remember { mutableStateOf(false) }
    val shake by animateFloatAsState(if (wrong) 1f else 0f, animationSpec = tween(180), label = "wrong_feedback")
    val options = remember(problem) { buildOptions(problem) }

    BackHandler(enabled = true) { /* Intentionally locked until the correct answer is selected. */ }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 26.dp).graphicsLayer { translationX = if (wrong) kotlin.math.sin(shake * Math.PI).toFloat() * 8f else 0f },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(.7f))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(76.dp)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Alarm, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp)) } }
            Spacer(Modifier.height(18.dp))
            Text("Good morning", style = MaterialTheme.typography.headlineLarge)
            Text("Solve the challenge to stop your alarm", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(22.dp))

            LifeOSMathCard(problem)
            Spacer(Modifier.height(18.dp))
            Text("Choose the correct answer", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { option ->
                    AnswerOption(
                        value = option,
                        selected = selected == option,
                        onClick = {
                            selected = option
                            if (option == problem.answer) onSolved()
                            else {
                                wrong = true
                                problem = newProblem()
                                selected = null
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (wrong) {
                Text("Not quite — new problem ready.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 10.dp))
            }
            Spacer(Modifier.height(16.dp))
            AssistChip(onClick = {}, label = { Text("Fresh + / − question every ring") }, leadingIcon = { Icon(Icons.Filled.Calculate, null, Modifier.size(17.dp)) })
            Spacer(Modifier.weight(1f))
            Text("Answers are always non-negative and below 99.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LifeOSMathCard(problem: MathProblem) {
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp), modifier = Modifier.fillMaxWidth()) {
        AnimatedContent(problem.text, label = "math_problem") { expression ->
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SOLVE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("$expression = ?", style = MaterialTheme.typography.displaySmall)
            }
        }
    }
}

@Composable
private fun AnswerOption(value: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, shape = RoundedCornerShape(22.dp), color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f), contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, modifier = modifier.height(82.dp), tonalElevation = 1.dp) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
            if (selected) { Spacer(Modifier.width(6.dp)); Icon(Icons.Filled.Check, null, Modifier.size(20.dp)) }
        }
    }
}

private fun newProblem(): MathProblem {
    repeat(30) {
        val add = Random.nextBoolean()
        return if (add) {
            val a = Random.nextInt(1, 50)
            val b = Random.nextInt(1, 99 - a)
            MathProblem(a, b, true)
        } else {
            val a = Random.nextInt(2, 99)
            val b = Random.nextInt(1, a)
            MathProblem(a, b, false)
        }
    }
    return MathProblem(12, 7, false)
}

private fun buildOptions(problem: MathProblem): List<Int> {
    val correct = problem.answer
    var wrong = if (correct == 0) 1 else correct + if (correct > 90) -7 else 7
    if (wrong !in 0..98 || wrong == correct) wrong = (correct + 13) % 99
    return if (Random.nextBoolean()) listOf(correct, wrong) else listOf(wrong, correct)
}
