package com.lifeos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.lifeos.app.ui.theme.LocalGlassColors

@Composable
fun GlassCard(modifier:Modifier=Modifier,cornerRadius:androidx.compose.ui.unit.Dp=24.dp,content:@Composable () -> Unit){val glass=LocalGlassColors.current;val shape=RoundedCornerShape(cornerRadius);Box(modifier.shadow(5.dp,shape,ambientColor=glass.border,spotColor=glass.border).clip(shape).background(Brush.verticalGradient(listOf(glass.surface,glass.surface.copy(alpha=(glass.surface.alpha*.96f).coerceIn(.1f,1f))))).border(1.dp,glass.border,shape).padding(18.dp)){content()}}

@Composable
fun GlassChip(modifier:Modifier=Modifier,content:@Composable () -> Unit){val glass=LocalGlassColors.current;val shape=RoundedCornerShape(50);Box(modifier.clip(shape).background(glass.surface).border(1.dp,glass.border,shape).padding(horizontal=12.dp,vertical=6.dp)){content()}}
