package ren.pj.animatedlist

import androidx.compose.runtime.Immutable

@Immutable
data class AnimatedListTransition(
    val enter: EnterBehavior = EnterBehavior.FadeAndSlide(),
    val exit: ExitBehavior = ExitBehavior.FadeAndSlide(),
    val placement: PlacementBehavior = PlacementBehavior.Animated(),
) {
    companion object {
        val Default = AnimatedListTransition(
            enter = EnterBehavior.FadeAndSlide(),
            exit = ExitBehavior.FadeAndSlide(),
            placement = PlacementBehavior.Animated(),
        )

        val Fade = AnimatedListTransition(
            enter = EnterBehavior.Fade(),
            exit = ExitBehavior.Fade(),
            placement = PlacementBehavior.Animated(),
        )

        val SlideVertical = AnimatedListTransition(
            enter = EnterBehavior.SlideVertical(),
            exit = ExitBehavior.SlideVertical(),
            placement = PlacementBehavior.Animated(),
        )

        val None = AnimatedListTransition(
            enter = EnterBehavior.None,
            exit = ExitBehavior.None,
            placement = PlacementBehavior.None,
        )
    }
}
