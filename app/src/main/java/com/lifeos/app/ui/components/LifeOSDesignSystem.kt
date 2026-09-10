package com.lifeos.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.theme.LifeOSLavender
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LocalGlassColors

@Composable
fun LifeOSSectionHeader(title: String, modifier: Modifier = Modifier, supportingText: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            supportingText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
        action?.invoke()
    }
}

@Composable
fun LifeOSCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val shape = MaterialTheme.shapes.large
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = LocalGlassColors.current.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, LocalGlassColors.current.border),
        content = content
    )
}

@Composable
fun LifeOSMetricCard(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null, supportingText: String? = null) {
    LifeOSCard(modifier) {
        Column(Modifier.padding(LifeOSSpacing.cardPadding), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let { Icon(it, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary) ; Spacer(Modifier.width(7.dp)) }
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.headlineMedium)
            supportingText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun LifeOSProgress(progress: Float, modifier: Modifier = Modifier, label: String? = null) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "lifeos_progress")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        label?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth().height(8.dp), trackColor = MaterialTheme.colorScheme.surfaceVariant)
    }
}

@Composable
fun LifeOSStatusPill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: (() -> Unit)? = null) {
    AssistChip(onClick = onClick ?: {}, label = { Text(text) }, leadingIcon = icon?.let { { Icon(it, null, Modifier.size(16.dp)) } }, modifier = modifier)
}

@Composable
fun LifeOSStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium, color = LifeOSLavender.copy(alpha = 0.7f)) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LifeOSIconBadge(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Box(modifier.size(46.dp).clip(CircleShape).background(LifeOSLavender.copy(alpha = .8f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(24.dp), tint = tint)
    }
}

@Composable
fun LifeOSEmptyState(title: String, message: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Filled.Info) {
    LifeOSCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LifeOSIconBadge(icon)
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LifeOSCompletionBadge(completed: Boolean, modifier: Modifier = Modifier) {
    val tint by animateColorAsState(if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .35f), label = "completion")
    Icon(if (completed) Icons.Filled.CheckCircle else Icons.Filled.CheckCircle, contentDescription = null, tint = tint, modifier = modifier.size(24.dp))
}

@Composable
fun LifeOSLoadingState(modifier: Modifier = Modifier) { Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
