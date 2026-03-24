package com.rend1x.composeanimatedlist

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

private const val DefaultSlideRevealOffsetPx = 24f

/**
 * Applies a simple fade + slide-in reveal driven by [AnimatedItemScope.progress].
 *
 * Note: [AnimatedColumn] already applies the transition spec on the item container. Use this for
 * extra inner styling (or pair with [AnimatedItemDefaults.none] if you want full control).
 */
fun Modifier.animatedItem(
    scope: AnimatedItemScope,
    slideRevealOffsetPx: Float = DefaultSlideRevealOffsetPx,
): Modifier = graphicsLayer {
    alpha = scope.progress
    translationY = (1f - scope.progress) * slideRevealOffsetPx
}
