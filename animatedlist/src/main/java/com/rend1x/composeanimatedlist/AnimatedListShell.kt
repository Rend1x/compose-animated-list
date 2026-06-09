package com.rend1x.composeanimatedlist

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt

internal enum class AnimatedListOrientation {
    Vertical,
    Horizontal,
}

internal data class MainAxisLayoutSize(val width: Int, val height: Int)

internal fun Modifier.animatedListShell(
    orientation: AnimatedListOrientation,
    alpha: Float,
    translation: Float,
    mainAxisSizeProgress: Float,
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val layoutSize = mainAxisLayoutSize(
        orientation = orientation,
        width = placeable.width,
        height = placeable.height,
        sizeProgress = mainAxisSizeProgress,
    )
    layout(layoutSize.width, layoutSize.height) {
        placeable.placeRelative(0, 0)
    }
}.graphicsLayer {
    this.alpha = alpha.coerceIn(0f, 1f)
    if (orientation == AnimatedListOrientation.Horizontal) {
        translationX = translation
    } else {
        translationY = translation
    }
    clip = true
}

internal fun mainAxisLayoutSize(orientation: AnimatedListOrientation, width: Int, height: Int, sizeProgress: Float): MainAxisLayoutSize {
    val sizeFraction = sizeProgress.coerceIn(0f, 1f)
    return when (orientation) {
        AnimatedListOrientation.Vertical -> MainAxisLayoutSize(
            width = width,
            height = (height * sizeFraction).roundToInt(),
        )

        AnimatedListOrientation.Horizontal -> MainAxisLayoutSize(
            width = (width * sizeFraction).roundToInt(),
            height = height,
        )
    }
}
