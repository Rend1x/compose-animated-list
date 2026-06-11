package com.rend1x.composeanimatedlist.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rend1x.composeanimatedlist.BuildConfig
import com.rend1x.composeanimatedlist.ItemPhase
import com.rend1x.composeanimatedlist.core.AnimatedListItem
import com.rend1x.composeanimatedlist.core.AnimatedListKeyPolicy
import com.rend1x.composeanimatedlist.core.AnimatedListRenderEngine
import com.rend1x.composeanimatedlist.core.PresenceState
import com.rend1x.composeanimatedlist.toItemPhase

/**
 * Optional controller for [com.rend1x.composeanimatedlist.AnimatedColumn] and
 * [com.rend1x.composeanimatedlist.AnimatedRow].
 *
 * **Semantics:**
 * - [isAnimating] is true while at least one item row is running a transition coroutine (enter/exit
 *   settle or “present” smoothing after a phase change).
 * - [clearExitingNow] drops every row that is still retained after removal from the input list
 *   (exit animation not yet finished) **without** playing exit animations. It does not affect
 *   items that are still in the input list or still entering.
 * - [clearExiting] drops only the exiting row with the matching key.
 * - [visibleKeys] and [exitingKeys] expose the latest render snapshot keys. [visibleKeys] includes
 *   entering and visible rows; [exitingKeys] includes rows retained only for exit animations.
 */
@Stable
class AnimatedListState internal constructor() {
    private var activeAnimations: Int by mutableIntStateOf(0)
    private var clearExitingCallback: (() -> Unit)? by mutableStateOf(null)
    private var clearExitingKeyCallback: ((Any) -> Unit)? by mutableStateOf(null)
    private var onItemPhaseChanged: (key: Any, phase: ItemPhase) -> Unit = { _, _ -> }
    private var onEnterFinished: (key: Any) -> Unit = {}
    private var onExitFinished: (key: Any) -> Unit = {}
    private var phaseByKey: Map<Any, ItemPhase> = emptyMap()
    private var hasSnapshot = false

    val isAnimating: Boolean
        get() = activeAnimations > 0

    var visibleKeys: Set<Any> by mutableStateOf(emptySet())
        private set

    var exitingKeys: Set<Any> by mutableStateOf(emptySet())
        private set

    /** Clears all exiting items immediately; see [AnimatedListState] class documentation. */
    fun clearExitingNow() {
        clearExitingCallback?.invoke()
    }

    /** Clears only the exiting item with [key]. No-op when [key] is not currently exiting. */
    fun clearExiting(key: Any) {
        clearExitingKeyCallback?.invoke(key)
    }

    internal fun onAnimationStarted() {
        activeAnimations += 1
    }

    internal fun onAnimationFinished() {
        activeAnimations = (activeAnimations - 1).coerceAtLeast(0)
    }

    internal fun setClearExitingCallbacks(callback: () -> Unit, keyCallback: (Any) -> Unit) {
        clearExitingCallback = callback
        clearExitingKeyCallback = keyCallback
    }

    internal fun updateHooks(
        onItemPhaseChanged: (key: Any, phase: ItemPhase) -> Unit,
        onEnterFinished: (key: Any) -> Unit,
        onExitFinished: (key: Any) -> Unit,
    ) {
        this.onItemPhaseChanged = onItemPhaseChanged
        this.onEnterFinished = onEnterFinished
        this.onExitFinished = onExitFinished
    }

    internal fun syncRenderItems(renderItems: List<AnimatedListItem<*>>) {
        visibleKeys = renderItems
            .asSequence()
            .filterNot { it.presence == PresenceState.Exiting }
            .mapTo(linkedSetOf()) { it.key }
        exitingKeys = renderItems
            .asSequence()
            .filter { it.presence == PresenceState.Exiting }
            .mapTo(linkedSetOf()) { it.key }

        val newPhaseByKey = renderItems.associate { it.key to it.presence.toItemPhase() }
        if (hasSnapshot) {
            newPhaseByKey.forEach { (key, phase) ->
                if (phaseByKey[key] != phase) {
                    onItemPhaseChanged(key, phase)
                }
            }
        } else {
            hasSnapshot = true
        }
        phaseByKey = newPhaseByKey
    }

    internal fun notifyEnterFinished(key: Any) {
        onEnterFinished(key)
    }

    internal fun notifyExitFinished(key: Any) {
        onExitFinished(key)
    }
}

@Composable
fun rememberAnimatedListState(): AnimatedListState = rememberAnimatedListState(
    onItemPhaseChanged = { _, _ -> },
    onEnterFinished = {},
    onExitFinished = {},
)

@Composable
fun rememberAnimatedListState(
    onItemPhaseChanged: (key: Any, phase: ItemPhase) -> Unit = { _, _ -> },
    onEnterFinished: (key: Any) -> Unit = {},
    onExitFinished: (key: Any) -> Unit = {},
): AnimatedListState {
    val state = remember { AnimatedListState() }
    state.updateHooks(
        onItemPhaseChanged = onItemPhaseChanged,
        onEnterFinished = onEnterFinished,
        onExitFinished = onExitFinished,
    )
    return state
}

/**
 * Compose adapter around [AnimatedListRenderEngine]: mirrors engine snapshots into [mutableStateOf]
 * for recomposition.
 */
internal class AnimatedListRenderState<T>(initialItems: List<T>, keySelector: (T) -> Any) {
    private val keyPolicy =
        if (BuildConfig.DEBUG) AnimatedListKeyPolicy.Strict else AnimatedListKeyPolicy.LastWins

    private val engine =
        AnimatedListRenderEngine(
            initialItems = initialItems,
            keySelector = keySelector,
            keyPolicy = keyPolicy,
        )

    var renderItems: List<AnimatedListItem<T>> by mutableStateOf(engine.items)
        private set

    fun update(items: List<T>, keySelector: (T) -> Any) {
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

    fun clearExiting(key: Any) {
        engine.clearExiting(key)
        renderItems = engine.items
    }
}
