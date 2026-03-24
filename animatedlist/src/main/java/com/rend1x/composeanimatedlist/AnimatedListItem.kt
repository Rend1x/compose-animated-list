package com.rend1x.composeanimatedlist

/**
 * Item representation stored by AnimatedListState.
 * This render model is independent from the input list and can keep exiting items.
 */
internal data class AnimatedListItem<T>(
    val key: Any,
    val value: T,
    val presence: PresenceState,
)
