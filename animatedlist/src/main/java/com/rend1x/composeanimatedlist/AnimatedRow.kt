package com.rend1x.composeanimatedlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rend1x.composeanimatedlist.animation.AnimatedItemDefaults
import com.rend1x.composeanimatedlist.animation.AnimatedItemTransitionSpec
import com.rend1x.composeanimatedlist.animation.EnterSpec
import com.rend1x.composeanimatedlist.animation.ExitSpec
import com.rend1x.composeanimatedlist.state.AnimatedListState
import com.rend1x.composeanimatedlist.state.rememberAnimatedListState

/**
 * Row-based list with diff-driven enter/exit animations.
 *
 * [AnimatedRow] is the horizontal counterpart to [AnimatedColumn]. It uses the same diff lifecycle,
 * exiting retention, reinsertion, [AnimatedItemScope] phase/progress contract, [state], and [transitionSpec].
 * Placement animation expands/collapses each item's reserved **width**. Slide enter/exit specs use the
 * same offset values as [AnimatedColumn], applied on the horizontal axis: exit `Up` maps to negative X
 * (left), and exit `Down` maps to positive X (right).
 *
 * The item [content] lambda receives [AnimatedItemScope]. Recommended: [animatedItem] on the item content
 * with [transitionSpec] = [com.rend1x.composeanimatedlist.animation.AnimatedItemDefaults.none].
 * Advanced: [AnimatedItemScope.phase] ([ItemPhase]) for lifecycle, and [AnimatedItemScope.visibilityProgress],
 * [AnimatedItemScope.placementProgress], and [AnimatedItemScope.progress] for custom motion with a non-`none`
 * [transitionSpec]. Full semantics are on [AnimatedItemScope].
 *
 * **Update and transition semantics** match [AnimatedColumn]:
 *
 * - **Input updates:** Each time [items] or [key] changes, the internal render list is updated by
 *   diffing from the previous render snapshot. Rapid recompositions coalesce: the coroutine tied to
 *   [items] is restarted, and the last committed [items] wins for that frame's effect run.
 * - **Exiting retention:** Removed keys stay composed until their exit animation finishes (or
 *   [AnimatedListState.clearExitingNow] is used).
 * - **Reinsertion:** If a key is removed and added back before exit removal, it becomes [ItemPhase.Visible]
 *   with the new element value; it does not re-enter as [ItemPhase.Entering].
 * - **Zero-duration visibility:** [EnterSpec] / [ExitSpec] with [com.rend1x.composeanimatedlist.animation.EnterSpec.None],
 *   [com.rend1x.composeanimatedlist.animation.ExitSpec.None], or `durationMillis = 0` use tweens of length `0`;
 *   the animation completes to its target in the same effect run, then exit completion runs.
 * - **Keys:** [key] must be unique within each [items] snapshot. Debug builds of this library fail
 *   fast with a clear error; release builds keep the last item per duplicated key (see README).
 */
@Composable
fun <T> AnimatedRow(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    state: AnimatedListState = rememberAnimatedListState(),
    transitionSpec: AnimatedItemTransitionSpec = AnimatedItemDefaults.fadeSlide(),
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable AnimatedItemScope.(T) -> Unit,
) {
    AnimatedRow(
        items = items,
        key = key,
        modifier = modifier,
        state = state,
        transitionSpec = transitionSpec,
        verticalAlignment = verticalAlignment,
        content = { item, _ -> content(item) },
    )
}

/**
 * Row-based list with diff-driven enter/exit animations.
 *
 * This overload also passes the item's current internal render index to [content]. The index includes
 * retained [ItemPhase.Exiting] items until their exit animation finishes or the item is otherwise
 * cleared from the render state.
 */
@Composable
fun <T> AnimatedRow(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    state: AnimatedListState = rememberAnimatedListState(),
    transitionSpec: AnimatedItemTransitionSpec = AnimatedItemDefaults.fadeSlide(),
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable AnimatedItemScope.(T, Int) -> Unit,
) {
    AnimatedListLayout(
        items = items,
        key = key,
        modifier = modifier,
        state = state,
        transitionSpec = transitionSpec,
        verticalAlignment = verticalAlignment,
        orientation = AnimatedListOrientation.Horizontal,
        content = content,
    )
}
