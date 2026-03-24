package com.rend1x.composeanimatedlist.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Public controller for AnimatedColumn.
 */
@Stable
class AnimatedListState internal constructor() {
    private var activeAnimations: Int by mutableIntStateOf(0)
    private var clearExitingCallback: (() -> Unit)? by mutableStateOf(null)

    val isAnimating: Boolean
        get() = activeAnimations > 0

    fun clearExitingNow() {
        clearExitingCallback?.invoke()
    }

    internal fun onAnimationStarted() {
        activeAnimations += 1
    }

    internal fun onAnimationFinished() {
        activeAnimations = (activeAnimations - 1).coerceAtLeast(0)
    }

    internal fun setClearExitingCallback(callback: () -> Unit) {
        clearExitingCallback = callback
    }
}

@Composable
fun rememberAnimatedListState(): AnimatedListState = remember { AnimatedListState() }

/**
 * Holds internal render list for AnimatedColumn.
 * Render list may include exiting items that are not in the latest input anymore.
 */
internal class AnimatedListRenderState<T>(
    initialItems: List<T>,
    keySelector: (T) -> Any,
) {
    var renderItems: List<AnimatedListItem<T>> by mutableStateOf(
        value = initialItems.map { item ->
            AnimatedListItem(
                key = keySelector(item),
                value = item,
                presence = PresenceState.Present,
            )
        },
    )
        private set

    fun update(
        items: List<T>,
        keySelector: (T) -> Any,
    ) {
        renderItems = AnimatedListDiffer.diff(
            current = renderItems,
            newItems = items,
            keySelector = keySelector,
        )
    }

    fun onExitAnimationFinished(key: Any) {
        renderItems = renderItems.filterNot { item ->
            item.key == key && item.presence == PresenceState.Exiting
        }
    }

    fun clearExitingNow() {
        renderItems = renderItems.filterNot { it.presence == PresenceState.Exiting }
    }
}
