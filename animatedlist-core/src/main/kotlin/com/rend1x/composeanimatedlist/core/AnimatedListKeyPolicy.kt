package com.rend1x.composeanimatedlist.core

/**
 * How duplicate keys in a single `items` snapshot are handled.
 *
 * The Compose library wires [Strict] for debug and [LastWins] for release builds (see README).
 */
enum class AnimatedListKeyPolicy {
    /**
     * [AnimatedListKeys.sanitizeAnimatedListInput] throws [IllegalStateException] when duplicates are detected.
     */
    Strict,

    /**
     * Normalize by keeping the **last** occurrence of each key (stable relative order among kept items).
     */
    LastWins,
}
