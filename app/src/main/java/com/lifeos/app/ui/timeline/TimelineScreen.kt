package com.lifeos.app.ui.timeline

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.lifeos.app.data.repository.CaptureRepository

@Composable
private fun TimelineAnimatedItem(
    index: Int,
    item: TimelineItem,
    captureRepository: CaptureRepository,
    isLast: Boolean,
    onOpenCapture: () -> Unit
) {
    var visible by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.id) { visible = true }
    AnimatedContent(
        targetState = visible,
        transitionSpec = { (fadeIn(tween(220)) + slideInVertically(tween(280), initialOffsetY = { it / 6 })) togetherWith fadeOut(tween(120)) },
        label = "timeline_item_$index"
    ) { shown ->
        if (shown) TimelineMemoryCard(item, captureRepository, isLast, onOpenCapture)
    }
}
