package com.lifeos.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = LifeOSPrimary,
    onPrimary = Color.White,
    primaryContainer = LifeOSLavender,
    onPrimaryContainer = LifeOSPrimaryDeep,
    secondary = LifeOSSecondary,
    onSecondary = Color.White,
    tertiary = LifeOSTertiary,
    background = LifeOSBackgroundLight,
    surface = LifeOSSurfaceLight,
    surfaceVariant = LifeOSVioletSoft,
    onBackground = LifeOSTextPrimaryLight,
    onSurface = LifeOSTextPrimaryLight,
    onSurfaceVariant = LifeOSTextSecondaryLight,
    error = LifeOSDanger,
    outline = Color(0xFFD9C9EA),
)

private val DarkColors = darkColorScheme(
    primary = LifeOSPrimaryBright,
    onPrimary = Color.White,
    primaryContainer = LifeOSPrimaryDeep,
    onPrimaryContainer = LifeOSTextPrimaryDark,
    secondary = LifeOSSecondary,
    onSecondary = Color(0xFF20102F),
    tertiary = LifeOSTertiary,
    background = LifeOSBackgroundDark,
    surface = LifeOSSurfaceDark,
    surfaceVariant = LifeOSSurfaceElevatedDark,
    onBackground = LifeOSTextPrimaryDark,
    onSurface = LifeOSTextPrimaryDark,
    onSurfaceVariant = LifeOSTextSecondaryDark,
    error = LifeOSDanger,
    outline = Color(0xFF4A3C5C),
)

data class GlassColors(val surface: Color, val border: Color)
val LocalGlassColors = staticCompositionLocalOf { GlassColors(GlassLight, GlassBorderLight) }

@Composable
fun LifeOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Dynamic color is intentionally ignored: LifeOS keeps its purple/violet identity.
    @Suppress("UNUSED_VARIABLE")
    val context = LocalContext.current
    val colorScheme = if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(
        LocalGlassColors provides if (darkTheme) GlassColors(GlassDark, GlassBorderDark) else GlassColors(GlassLight, GlassBorderLight)
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = LifeOSTypography, shapes = LifeOSShapes, content = content)
    }
}
