package com.rend1x.composeanimatedlist.core

/**
 * Item representation for the diff-driven render list.
 * Independent from the caller’s input list and can retain exiting rows until removed.
 */
data class AnimatedListItem<T>(
    val key: Any,
    val value: T,
    val presence: PresenceState,
)
