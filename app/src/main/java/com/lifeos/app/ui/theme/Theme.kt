package com.lifeos.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(primary=LifeOSPrimary,onPrimary=Color.White,primaryContainer=LifeOSLavender,onPrimaryContainer=ColorCompat.Dark,secondary=LifeOSSecondary,tertiary=LifeOSTertiary,background=LifeOSBackgroundLight,surface=LifeOSSurfaceLight,surfaceVariant=LifeOSVioletSoft,onBackground=LifeOSTextPrimaryLight,onSurface=LifeOSTextPrimaryLight,onSurfaceVariant=LifeOSTextSecondaryLight,error=LifeOSDanger)
private val DarkColors = darkColorScheme(primary=ColorCompat.Lavender,onPrimary=ColorCompat.Dark,primaryContainer=LifeOSPrimaryVariant,onPrimaryContainer=ColorCompat.Lavender,secondary=ColorCompat.Pink,tertiary=ColorCompat.Lavender,background=LifeOSBackgroundDark,surface=LifeOSSurfaceDark,surfaceVariant=Color(0xFF352F3D),onBackground=LifeOSTextPrimaryDark,onSurface=LifeOSTextPrimaryDark,onSurfaceVariant=LifeOSTextSecondaryDark,error=Color(0xFFFFB4AB))

data class GlassColors(val surface: Color,val border: Color)
val LocalGlassColors = staticCompositionLocalOf { GlassColors(GlassLight,GlassBorderLight) }

@Composable
fun LifeOSTheme(darkTheme:Boolean=isSystemInDarkTheme(),dynamicColor:Boolean=false,content:@Composable()->Unit){
    val context=LocalContext.current
    val colorScheme=when{dynamicColor&&Build.VERSION.SDK_INT>=Build.VERSION_CODES.S->if(darkTheme)dynamicDarkColorScheme(context)else dynamicLightColorScheme(context);darkTheme->DarkColors;else->LightColors}
    CompositionLocalProvider(LocalGlassColors provides if(darkTheme)GlassColors(GlassDark,GlassBorderDark)else GlassColors(GlassLight,GlassBorderLight)){MaterialTheme(colorScheme=colorScheme,typography=LifeOSTypography,shapes=LifeOSShapes,content=content)}
}

private object ColorCompat { val Dark=Color(0xFF261A35);val Lavender=Color(0xFFEADDFF);val Pink=Color(0xFFFFD8E4) }
