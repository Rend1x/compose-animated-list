package com.rend1x.composeanimatedlist

import androidx.compose.runtime.Immutable

@Immutable
interface AnimatedItemScope {
    val phase: ItemPhase
    /** Normalized visibility along the current phase: entering 0→1, visible 1, exiting 1→0. */
    val progress: Float
}

internal data class AnimatedItemScopeImpl(
    override val phase: ItemPhase,
    override val progress: Float,
) : AnimatedItemScope
