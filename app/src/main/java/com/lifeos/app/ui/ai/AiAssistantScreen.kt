package com.lifeos.app.ui.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.ai.AiRepository
import com.lifeos.app.core.ai.AiResult
import com.lifeos.app.core.ai.ChatMessage
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.ui.components.LifeOSAIOrb
import com.lifeos.app.ui.components.LifeOSOfflinePill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AiAssistantViewModel(private val aiRepository: AiRepository) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("assistant", "Hi! I'm your LifeOS AI Assistant — I run fully offline on your device. Ask me about your tasks, habits, spending, diary or mood."))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun send(text: String) {
        if (text.isBlank() || _busy.value) return
        val updated = _messages.value + ChatMessage("user", text.trim())
        _messages.value = updated
        viewModelScope.launch {
            _busy.value = true
            when (val result = aiRepository.chat(updated)) {
                is AiResult.Success -> _messages.value = _messages.value + ChatMessage("assistant", result.text)
                is AiResult.Error -> _messages.value = _messages.value + ChatMessage("assistant", "Sorry, I hit an error: ${result.message}")
            }
            _busy.value = false
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AiAssistantScreen() {
    val locator = LocalServiceLocator.current
    val viewModel: AiAssistantViewModel = viewModel(factory = LambdaViewModelFactory { AiAssistantViewModel(locator.aiRepository) })
    val messages by viewModel.messages.collectAsState()
    val busy by viewModel.busy.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, busy) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LifeOSAIOrb(size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("LifeOS AI", style = MaterialTheme.typography.headlineSmall)
                    Text("Your private local assistant", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LifeOSOfflinePill()
            }

            AnimatedVisibility(messages.size == 1 && !busy) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { SuggestionChip("How am I doing?", { input = "How am I doing?" }) }
                    item { SuggestionChip("Today's tasks", { input = "What are my tasks today?" }) }
                    item { SuggestionChip("My habits", { input = "How are my habits going?" }) }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(messages, key = { index, _ -> index }) { _, message ->
                    val isUser = message.role == "user"
                    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            modifier = Modifier.widthIn(max = 340.dp).animateContentSize(),
                            shape = RoundedCornerShape(22.dp),
                            color = bubbleColor,
                            tonalElevation = if (isUser) 0.dp else 1.dp,
                            shadowElevation = if (isUser) 0.dp else 1.dp
                        ) {
                            Text(
                                message.content,
                                color = textColor,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
                if (busy) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Thinking locally…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Ask LifeOS AI…") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { viewModel.send(input); input = "" },
                        enabled = input.isNotBlank() && !busy,
                        modifier = Modifier.size(52.dp)
                    ) { Icon(Icons.Filled.Send, "Send") }
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = { Icon(Icons.Filled.AutoAwesome, null, Modifier.size(16.dp)) }
    )
}
