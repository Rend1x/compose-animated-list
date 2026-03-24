package com.rend1x.composeanimatedlist

import androidx.compose.runtime.Immutable

@Immutable
interface AnimatedItemScope {
    val isEntering: Boolean
    val isExiting: Boolean
}

internal data class AnimatedItemScopeImpl(
    override val isEntering: Boolean,
    override val isExiting: Boolean,
) : AnimatedItemScope
