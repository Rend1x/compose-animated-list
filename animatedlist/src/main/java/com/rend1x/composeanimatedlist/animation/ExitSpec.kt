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
