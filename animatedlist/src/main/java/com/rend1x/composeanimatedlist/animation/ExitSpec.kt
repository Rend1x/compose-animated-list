package com.rend1x.composeanimatedlist.animation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed interface ExitSpec {
    data object None : ExitSpec

    data class Fade(
        val durationMillis: Int = 180,
    ) : ExitSpec

    data class SlideVertical(
        val offset: Dp = 16.dp,
        val direction: VerticalDirection = VerticalDirection.Up,
        val durationMillis: Int = 180,
    ) : ExitSpec

    data class FadeAndSlide(
        val offset: Dp = 16.dp,
        val direction: VerticalDirection = VerticalDirection.Up,
        val durationMillis: Int = 200,
    ) : ExitSpec
}

/** Duration used for exit visibility tweens ([androidx.compose.animation.core.tween]). [None] is `0`. */
internal val ExitSpec.visibilityAnimationDurationMillis: Int
    get() = when (this) {
        ExitSpec.None -> 0
        is ExitSpec.Fade -> durationMillis
        is ExitSpec.SlideVertical -> durationMillis
        is ExitSpec.FadeAndSlide -> durationMillis
    }

internal val ExitSpec.targetAlpha: Float
    get() = when (this) {
        ExitSpec.None -> 0f
        is ExitSpec.Fade -> 0f
        is ExitSpec.SlideVertical -> 1f
        is ExitSpec.FadeAndSlide -> 0f
    }

internal val ExitSpec.targetOffsetDp: Dp
    get() = when (this) {
        ExitSpec.None -> 0.dp
        is ExitSpec.Fade -> 0.dp
        is ExitSpec.SlideVertical -> {
            when (direction) {
                VerticalDirection.Up -> -offset
                VerticalDirection.Down -> offset
            }
        }
        is ExitSpec.FadeAndSlide -> {
            when (direction) {
                VerticalDirection.Up -> -offset
                VerticalDirection.Down -> offset
            }
        }
    }
