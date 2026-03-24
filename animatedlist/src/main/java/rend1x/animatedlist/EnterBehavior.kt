package ren.pj.animatedlist

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed interface EnterBehavior {
    data object None : EnterBehavior

    data class Fade(
        val durationMillis: Int = 220,
    ) : EnterBehavior

    data class SlideVertical(
        val offset: Dp = 16.dp,
        val durationMillis: Int = 220,
    ) : EnterBehavior

    data class FadeAndSlide(
        val offset: Dp = 16.dp,
        val durationMillis: Int = 240,
    ) : EnterBehavior
}
