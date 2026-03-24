package com.rend1x.composeanimatedlist.animation

import androidx.compose.runtime.Immutable

@Immutable
data class AnimatedItemTransitionSpec(
    val enter: EnterSpec,
    val exit: ExitSpec,
    val placement: PlacementBehavior = PlacementBehavior.Animated(),
)

object AnimatedItemDefaults {

    fun fade(
        placement: PlacementBehavior = PlacementBehavior.Animated(),
    ): AnimatedItemTransitionSpec = AnimatedItemTransitionSpec(
        enter = EnterSpec.Fade(),
        exit = ExitSpec.Fade(),
        placement = placement,
    )

    fun slide(
        placement: PlacementBehavior = PlacementBehavior.Animated(),
    ): AnimatedItemTransitionSpec = AnimatedItemTransitionSpec(
        enter = EnterSpec.SlideVertical(),
        exit = ExitSpec.SlideVertical(),
        placement = placement,
    )

    fun fadeSlide(
        placement: PlacementBehavior = PlacementBehavior.Animated(),
    ): AnimatedItemTransitionSpec = AnimatedItemTransitionSpec(
        enter = EnterSpec.FadeAndSlide(),
        exit = ExitSpec.FadeAndSlide(),
        placement = placement,
    )

    fun none(): AnimatedItemTransitionSpec = AnimatedItemTransitionSpec(
        enter = EnterSpec.None,
        exit = ExitSpec.None,
        placement = PlacementBehavior.None,
    )
}
