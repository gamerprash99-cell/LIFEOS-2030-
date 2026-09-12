package com.lifeos.app.ui.home

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

/** Compatibility for the Home floating-actions helper, which is a composable
 * function rather than a BoxScope receiver but still uses Box-style alignment. */
fun Modifier.align(alignment: Alignment): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val x = when (alignment) {
        Alignment.TopStart, Alignment.CenterStart, Alignment.BottomStart -> 0
        Alignment.TopCenter, Alignment.Center, Alignment.BottomCenter -> (constraints.maxWidth - placeable.width) / 2
        else -> constraints.maxWidth - placeable.width
    }
    val y = when (alignment) {
        Alignment.TopStart, Alignment.TopCenter, Alignment.TopEnd -> 0
        Alignment.CenterStart, Alignment.Center, Alignment.CenterEnd -> (constraints.maxHeight - placeable.height) / 2
        else -> constraints.maxHeight - placeable.height
    }
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(x, y) }
}
