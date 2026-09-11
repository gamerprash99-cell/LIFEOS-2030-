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
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.lifeos.app.core.util.NotificationHelper
import com.lifeos.app.ui.theme.LifeOSTheme
import kotlin.random.Random

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
    var error by remember { mutableStateOf(false) }
    val options = remember(problem) { problem.options }

    BackHandler(enabled = true) { /* The alarm remains active until the correct answer is chosen. */ }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(92.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Alarm,
                        null,
                        Modifier.size(46.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("Good morning", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Solve one quick question to stop your alarm.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(22.dp))
            LifeOSMathCard(problem.text)

            Spacer(Modifier.height(16.dp))
            Text(
                "Choose the correct answer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                options.forEachIndexed { index, answer ->
                    AnswerChoice(
                        answer = answer,
                        index = index,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (answer == problem.answer) {
                                onSolved()
                            } else {
                                problem = newProblem()
                                error = true
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(visible = error) {
                Text(
                    "Not quite — a new question is ready.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Alarm keeps ringing until the correct answer is selected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AnswerChoice(
    answer: Int,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val label = if (index == 0) "A" else "B"
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f,
        label = "answer_$index"
    )
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(76.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(answer.toString(), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun LifeOSMathCard(text: String) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedContent(targetState = text, label = "math_problem") { current ->
            Box(
                Modifier.fillMaxWidth().padding(vertical = 30.dp, horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(current, style = MaterialTheme.typography.displaySmall)
            }
        }
    }
}

private data class MathProblem(val a: Int, val b: Int, val add: Boolean) {
    val answer: Int get() = if (add) a + b else a - b
    val text: String get() = "$a ${if (add) "+" else "−"} $b = ?"
    val options: List<Int>
        get() {
            val wrong = when {
                answer == 0 -> 1
                answer == 98 -> 97
                else -> if (Random.nextBoolean()) answer - 1 else answer + 1
            }
            return if (Random.nextBoolean()) listOf(answer, wrong) else listOf(wrong, answer)
        }
}

private fun newProblem(): MathProblem {
    repeat(40) {
        val a = Random.nextInt(1, 99)
        val b = Random.nextInt(1, 99)
        val add = Random.nextBoolean()
        val answer = if (add) a + b else a - b
        if (answer in 0..98) return MathProblem(a, b, add)
    }
    return MathProblem(12, 7, true)
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
