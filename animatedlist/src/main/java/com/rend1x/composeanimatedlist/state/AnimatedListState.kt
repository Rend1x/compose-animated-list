package com.rend1x.composeanimatedlist.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rend1x.composeanimatedlist.BuildConfig
import com.rend1x.composeanimatedlist.core.AnimatedListItem
import com.rend1x.composeanimatedlist.core.AnimatedListKeyPolicy
import com.rend1x.composeanimatedlist.core.AnimatedListRenderEngine

/**
 * Optional controller for [com.rend1x.composeanimatedlist.AnimatedColumn].
 *
 * **Semantics:**
 * - [isAnimating] is true while at least one item row is running a transition coroutine (enter/exit
 *   settle or “present” smoothing after a phase change).
 * - [clearExitingNow] drops every row that is still retained after removal from the input list
 *   (exit animation not yet finished) **without** playing exit animations. It does not affect
 *   items that are still in the input list or still entering.
 */
@Stable
class AnimatedListState internal constructor() {
    private var activeAnimations: Int by mutableIntStateOf(0)
    private var clearExitingCallback: (() -> Unit)? by mutableStateOf(null)

    val isAnimating: Boolean
        get() = activeAnimations > 0

    /** Clears all exiting items immediately; see [AnimatedListState] class documentation. */
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
 * Compose adapter around [AnimatedListRenderEngine]: mirrors engine snapshots into [mutableStateOf]
 * for recomposition.
 */
internal class AnimatedListRenderState<T>(
    initialItems: List<T>,
    keySelector: (T) -> Any,
) {
    private val keyPolicy =
        if (BuildConfig.DEBUG) AnimatedListKeyPolicy.Strict else AnimatedListKeyPolicy.LastWins

    private val engine = AnimatedListRenderEngine(
        initialItems = initialItems,
        keySelector = keySelector,
        keyPolicy = keyPolicy,
    )

    var renderItems: List<AnimatedListItem<T>> by mutableStateOf(engine.items)
        private set

    fun update(
        items: List<T>,
        keySelector: (T) -> Any,
    ) {
        engine.update(items = items, keySelector = keySelector)
        renderItems = engine.items
    }

    fun onExitAnimationFinished(key: Any) {
        engine.onExitAnimationFinished(key)
        renderItems = engine.items
    }

    fun onEnterAnimationFinished(key: Any) {
        engine.onEnterAnimationFinished(key)
        renderItems = engine.items
    }

    fun clearExitingNow() {
        engine.clearExitingNow()
        renderItems = engine.items
    }
}
