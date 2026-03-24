package ren.pj.animatedlist

sealed interface PlacementBehavior {
    data object None : PlacementBehavior

    data class Animated(
        val durationMillis: Int = 260,
    ) : PlacementBehavior
}
