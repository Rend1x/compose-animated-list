package com.rend1x.composeanimatedlist.animation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed interface EnterSpec {
    data object None : EnterSpec

    data class Fade(
        val durationMillis: Int = 220,
    ) : EnterSpec

    data class SlideVertical(
        val offset: Dp = 16.dp,
        val durationMillis: Int = 220,
    ) : EnterSpec

    data class FadeAndSlide(
        val offset: Dp = 16.dp,
        val durationMillis: Int = 240,
    ) : EnterSpec
}

/** Duration used for enter visibility tweens ([androidx.compose.animation.core.tween]). [None] is `0`. */
internal val EnterSpec.visibilityAnimationDurationMillis: Int
    get() = when (this) {
        EnterSpec.None -> 0
        is EnterSpec.Fade -> durationMillis
        is EnterSpec.SlideVertical -> durationMillis
        is EnterSpec.FadeAndSlide -> durationMillis
    }

internal val EnterSpec.initialAlpha: Float
    get() = when (this) {
        EnterSpec.None -> 1f
        is EnterSpec.Fade -> 0f
        is EnterSpec.SlideVertical -> 1f
        is EnterSpec.FadeAndSlide -> 0f
    }

internal val EnterSpec.initialOffsetDp: Dp
    get() = when (this) {
        EnterSpec.None -> 0.dp
        is EnterSpec.Fade -> 0.dp
        is EnterSpec.SlideVertical -> offset
        is EnterSpec.FadeAndSlide -> offset
    }
