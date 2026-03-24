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
