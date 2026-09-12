package com.lifeos.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private data class OnboardingPage(
    val title: String,
    val body: String,
    val illustration: Illustration,
    val chips: List<OnboardingChip> = emptyList(),
    val landing: Boolean = false
)

private data class OnboardingChip(val emoji: String, val text: String, val warm: Boolean = false)

private enum class Illustration { LANDING, BRAIN, PRIVACY, AI, CONNECTION }

private val Pages = listOf(
    OnboardingPage(
        title = "LifeOS",
        body = "Capture your life.\nUnderstand your life.",
        illustration = Illustration.LANDING,
        landing = true
    ),
    OnboardingPage(
        title = "Welcome to LifeOS",
        body = "Capture your life. Organize your life. Understand your life. Everything in one connected, local-first app.",
        illustration = Illustration.BRAIN
    ),
    OnboardingPage(
        title = "Your life. Your data.",
        body = "Notes, tasks, habits, expenses and diary entries stay on your device. Nothing is uploaded unless you explicitly export it.",
        illustration = Illustration.PRIVACY,
        chips = listOf(
            OnboardingChip("💖", "Private"),
            OnboardingChip("🧸", "Offline", warm = true)
        )
    ),
    OnboardingPage(
        title = "AI, on your terms",
        body = "Optional intelligence features run locally on your device. No external AI service is required.",
        illustration = Illustration.AI,
        chips = listOf(
            OnboardingChip("⚡", "100% Offline"),
            OnboardingChip("🌸", "Gentle Assistant", warm = true)
        )
    ),
    OnboardingPage(
        title = "Everything connects",
        body = "Your Timeline weaves together notes, tasks, habits, expenses and diary entries by date and time — one continuous story of your life.",
        illustration = Illustration.CONNECTION
    )
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onRestoreBackup: (() -> Unit)? = null,
    restoreStatus: String? = null,
    restoreInProgress: Boolean = false
) {
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = Pages[pageIndex]
    val isLast = pageIndex == Pages.lastIndex
    val view = LocalView.current

    SideEffect {
        WindowCompat.getInsetsController(view.context.findActivityWindow(), view)
            .isAppearanceLightStatusBars = true
        WindowCompat.getInsetsController(view.context.findActivityWindow(), view)
            .isAppearanceLightNavigationBars = true
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFFEEF7), Color(0xFFFDF9FF), Color(0xFFF1EEFF)),
                    radius = maxOf(maxWidth.value, maxHeight.value) * 1.15f
                )
            )
    ) {
        val compact = maxHeight < 700.dp
        Box(Modifier.fillMaxSize()) {
            DecorativeSparkles(compact)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = if (compact) 12.dp else 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!page.landing) OnboardingTopBar()
                    Spacer(Modifier.height(if (compact) 18.dp else 52.dp))
                }

            AnimatedContent(
                targetState = pageIndex,
                transitionSpec = {
                    (fadeIn(tween(180)) + slideInHorizontally(tween(260), initialOffsetX = { it / 8 }))
                        .togetherWith(fadeOut(tween(120)))
                },
                label = "onboarding_page"
            ) { index ->
                if (Pages[index].landing) {
                    LandingContent(compact)
                } else {
                    OnboardingCard(
                        page = Pages[index],
                        compact = compact,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(if (compact) 14.dp else 24.dp))
            PageIndicator(pageIndex)

            Spacer(Modifier.height(if (compact) 18.dp else 46.dp))

            Button(
                onClick = { if (page.landing) pageIndex++ else if (isLast) onFinish() else pageIndex++ },
                enabled = !restoreInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 56.dp else 62.dp)
                    .background(
                        if (page.landing) Brush.horizontalGradient(listOf(Color(0xFF9B6BFF), Color(0xFFE84FA9)))
                        else Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)),
                        RoundedCornerShape(32.dp)
                    )
                    .shadow(14.dp, RoundedCornerShape(32.dp), ambientColor = Color(0x557C3AED)),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
            ) {
                Text(
                    when {
                        page.landing -> "Start your journey"
                        isLast -> "Get started"
                        else -> "Next"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (!page.landing && !isLast) {
                    Spacer(Modifier.width(8.dp))
                    Text("→", style = MaterialTheme.typography.titleLarge)
                } else if (page.landing) {
                    Spacer(Modifier.width(8.dp))
                    Text("✨", style = MaterialTheme.typography.titleLarge)
                }
            }

            if (onRestoreBackup != null && !page.landing) {
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onRestoreBackup,
                    enabled = !restoreInProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compact) 54.dp else 60.dp),
                    shape = RoundedCornerShape(30.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = .22f)
                    )
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Restore a LifeOS backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }

                Text(
                    if (restoreInProgress) "Restoring your local JSON backup…"
                    else "Already used LifeOS? Restore your exported JSON backup here.",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isLast && !page.landing) {
                TextButton(onClick = onFinish, enabled = !restoreInProgress) {
                    Text("Skip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                }
            }

            restoreStatus?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    it,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("Backup restored")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
}

@Composable
private fun DecorativeSparkles(compact: Boolean) {
    if (compact) return
    Box(Modifier.fillMaxSize()) {
        Text("✨", modifier = Modifier.padding(start = 62.dp, top = 150.dp))
        Text("🌸", modifier = Modifier.align(Alignment.TopEnd).padding(top = 280.dp, end = 70.dp))
        Text("✨", modifier = Modifier.align(Alignment.BottomEnd).padding(end = 74.dp, bottom = 280.dp))
        Text("☁", modifier = Modifier.align(Alignment.BottomStart).padding(start = 92.dp, bottom = 160.dp), color = Color(0x6689B9B1))
    }
}

@Composable
private fun LandingContent(compact: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
    ) {
        Spacer(Modifier.height(if (compact) 10.dp else 26.dp))
        OnboardingIllustration(Illustration.LANDING, compact = false)
        Text(
            "LifeOS",
            style = if (compact) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF40245D)
        )
        Text(
            "Capture your life.\nUnderstand your life.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF71627E)
        )
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = .78f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEDE2F2)),
            shadowElevation = 4.dp
        ) {
            Text(
                "☁  100% private & cozy on your phone",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF6B5B76)
            )
        }
    }
}

@Composable
private fun OnboardingTopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = .72f),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE85AA9)))
                Spacer(Modifier.width(8.dp))
                Text("LIFEOS", color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OnboardingCard(page: OnboardingPage, compact: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(38.dp), ambientColor = Color(0x227C3AED))
            .border(1.dp, Color(0xFFE8DDF1), RoundedCornerShape(38.dp)),
        shape = RoundedCornerShape(38.dp),
        color = Color.White.copy(alpha = .94f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = if (compact) 22.dp else 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
        ) {
            if (page.chips.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    page.chips.forEach { chip ->
                        OnboardingChip(chip, Modifier.weight(1f, fill = false))
                    }
                }
            }

            OnboardingIllustration(page.illustration, compact)

            Text(
                page.title,
                textAlign = TextAlign.Center,
                style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF17101F)
            )
            Text(
                page.body,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF4F4858),
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            )

            if (page == Pages[2]) {
                InfoPill("🔒", "Safe on your device • Zero cloud snooping")
            }
        }
    }
}

@Composable
private fun OnboardingChip(chip: OnboardingChip, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(horizontal = 3.dp),
        shape = RoundedCornerShape(999.dp),
        color = if (chip.warm) Color(0xFFFFF4E8) else Color(0xFFEFFFF7),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (chip.warm) Color(0xFFF7D9B0) else Color(0xFFCFEFE2)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(chip.emoji)
            Spacer(Modifier.width(6.dp))
            Text(chip.text, style = MaterialTheme.typography.labelLarge, color = Color(0xFF4E4356))
        }
    }
}

@Composable
private fun InfoPill(icon: String, text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFF7F0FF),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9DDF7))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(icon)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = Color(0xFF7135BE), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PageIndicator(index: Int) {
    val landing = index == 0
    val count = if (landing) 3 else Pages.lastIndex
    val selected = if (landing) 0 else index - 1
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            Surface(
                modifier = Modifier.size(if (i == selected) 42.dp else 9.dp, 9.dp),
                shape = RoundedCornerShape(999.dp),
                color = if (i == selected) MaterialTheme.colorScheme.primary else Color(0xFFE9D6FA)
            ) {}
        }
    }
}

@Composable
private fun OnboardingIllustration(illustration: Illustration, compact: Boolean) {
    val size = if (compact) 116.dp else 142.dp
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        when (illustration) {
            Illustration.LANDING -> WelcomeIllustration()
            Illustration.BRAIN -> BrainIllustration()
            Illustration.PRIVACY -> LockIllustration()
            Illustration.AI -> RobotIllustration()
            Illustration.CONNECTION -> ConnectionIllustration()
        }
    }
}

@Composable
private fun WelcomeIllustration() {
    Box(Modifier.size(116.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(108.dp).blur(16.dp).background(Color(0x667C3AED), CircleShape))
        Box(
            Modifier
                .size(104.dp)
                .background(Brush.linearGradient(listOf(Color(0xFFA87BFF), Color(0xFFB64EEB))), CircleShape)
                .border(2.dp, Color(0xAA8B5CF6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("✦", color = Color.White, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 0.dp, bottom = 2.dp),
            shape = RoundedCornerShape(999.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Text("✨ v2.0", modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BrainIllustration() {
    Surface(
        modifier = Modifier.size(104.dp),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFFFFEFF7),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF9D5E7)),
        shadowElevation = 3.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("🧠", style = MaterialTheme.typography.displayMedium)
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(7.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 3.dp
            ) { Text("💖", modifier = Modifier.padding(5.dp)) }
        }
    }
}

@Composable
private fun LockIllustration() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🔒", style = MaterialTheme.typography.displayMedium)
    }
}

@Composable
private fun RobotIllustration() {
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.size(94.dp).blur(18.dp).background(Color(0x557C3AED), RoundedCornerShape(28.dp)))
        Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFFE9DFFF), shadowElevation = 5.dp) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("•ᴗ•", color = Color(0xFF6D25C5), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("▰▰", color = Color(0xFFB38AFB), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ConnectionIllustration() {
    Box(Modifier.size(108.dp), contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = Color(0xFFE9F7FF), shadowElevation = 2.dp) {
            Text("🔗", modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.displaySmall)
        }
    }
}

private fun android.content.Context.findActivityWindow(): android.view.Window {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context.window
        context = context.baseContext
    }
    error("Activity context required")
}
