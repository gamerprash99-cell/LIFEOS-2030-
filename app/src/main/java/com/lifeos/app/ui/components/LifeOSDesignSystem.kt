package com.lifeos.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.theme.LifeOSDarkHeroGradient
import com.lifeos.app.ui.theme.LifeOSLavender
import com.lifeos.app.ui.theme.LifeOSPrimaryGradient
import com.lifeos.app.ui.theme.LifeOSSoftGradient
import com.lifeos.app.ui.theme.LifeOSSpacing
import com.lifeos.app.ui.theme.LocalGlassColors

@Composable
fun LifeOSSectionHeader(title: String, modifier: Modifier = Modifier, supportingText: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            supportingText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
        action?.invoke()
    }
}

@Composable
fun LifeOSCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val shape = MaterialTheme.shapes.large
    val interaction = remember { MutableInteractionSource() }
    Surface(modifier = modifier.animateContentSize().then(if (onClick != null) Modifier.clickable(interaction, indication = null, onClick = onClick) else Modifier), shape = shape, color = LocalGlassColors.current.surface, tonalElevation = 1.dp, shadowElevation = 2.dp, border = BorderStroke(1.dp, LocalGlassColors.current.border), content = content)
}

@Composable
fun GradientCard(modifier: Modifier = Modifier, dark: Boolean = false, onClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
    val shape = MaterialTheme.shapes.extraLarge
    val brush = if (dark) Brush.linearGradient(LifeOSDarkHeroGradient) else Brush.linearGradient(LifeOSPrimaryGradient)
    Box(modifier = modifier.clip(shape).background(brush).clickable(enabled = onClick != null, onClick = { onClick?.invoke() }).padding(1.dp)) {
        Box(Modifier.fillMaxSize().clip(shape).background(Color.Black.copy(alpha = if (dark) .12f else .04f)), content = content)
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary), contentPadding = PaddingValues(horizontal = 20.dp)) {
        icon?.let { Icon(it, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(52.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .55f)), contentPadding = PaddingValues(horizontal = 18.dp)) {
        icon?.let { Icon(it, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun LifeOSMetricCard(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null, supportingText: String? = null) {
    LifeOSCard(modifier) { Column(Modifier.padding(LifeOSSpacing.cardPadding), verticalArrangement = Arrangement.spacedBy(7.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { icon?.let { Icon(it, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(7.dp)) }; Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); supportingText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
}

@Composable
fun LifeOSProgress(progress: Float, modifier: Modifier = Modifier, label: String? = null) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "lifeos_progress")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) { label?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth().height(7.dp), trackColor = MaterialTheme.colorScheme.surfaceVariant, color = MaterialTheme.colorScheme.primary) }
}

@Composable
fun LifeOSProgressRing(progress: Float, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 88.dp, label: String = "") {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress_ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) { CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), strokeWidth = 8.dp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .18f)); CircularProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxSize(), strokeWidth = 8.dp, color = MaterialTheme.colorScheme.onPrimary); Text(label, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
}

@Composable
fun LifeOSStatusPill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: (() -> Unit)? = null) { AssistChip(onClick = onClick ?: {}, label = { Text(text) }, leadingIcon = icon?.let { { Icon(it, null, Modifier.size(16.dp)) } }, modifier = modifier) }

@Composable
fun LifeOSStatChip(label: String, value: String, modifier: Modifier = Modifier) { Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = LifeOSLavender.copy(alpha = .7f)) { Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable
fun LifeOSIconBadge(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) { Box(modifier.size(46.dp).clip(CircleShape).background(Brush.linearGradient(LifeOSSoftGradient)), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(24.dp), tint = tint) } }

@Composable
fun LifeOSAIOrb(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 96.dp) {
    val transition = rememberInfiniteTransition(label = "lifeos_ai_orb")
    val scale by transition.animateFloat(1f, 1.04f, animationSpec = androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(3000), androidx.compose.animation.core.RepeatMode.Reverse), label = "ai_orb_scale")
    val alpha by transition.animateFloat(.85f, 1f, animationSpec = androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(3000), androidx.compose.animation.core.RepeatMode.Reverse), label = "ai_orb_alpha")
    Box(modifier.size(size).scale(scale).clip(CircleShape).background(Brush.radialGradient(listOf(LifeOSLavender.copy(alpha = alpha), com.lifeos.app.ui.theme.LifeOSPrimaryBright, com.lifeos.app.ui.theme.LifeOSPrimaryDeep))), contentAlignment = Alignment.Center) { Text("✦", color = Color.White, style = MaterialTheme.typography.displaySmall) }
}

@Composable
fun LifeOSEmptyState(title: String, message: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Filled.Info) { LifeOSCard(modifier) { Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { LifeOSIconBadge(icon); Text(title, style = MaterialTheme.typography.titleMedium); Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center) } } }

@Composable
fun LifeOSLoadingState(modifier: Modifier = Modifier) { Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary); Text("Loading your local life data…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable
fun LifeOSOfflinePill(modifier: Modifier = Modifier) { Surface(modifier = modifier, shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .65f)) { Text("Offline • All data available locally", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) } }

@Composable
fun LifeOSCompletionBadge(completed: Boolean, modifier: Modifier = Modifier) { val tint by animateColorAsState(if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .35f), label = "completion"); Icon(Icons.Filled.CheckCircle, contentDescription = if (completed) "Completed" else "Not completed", tint = tint, modifier = modifier.size(24.dp)) }
