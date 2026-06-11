package com.rend1x.composeanimatedlist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key as composeKey
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.rend1x.composeanimatedlist.animation.AnimatedItemDefaults
import com.rend1x.composeanimatedlist.animation.AnimatedItemTransitionSpec
import com.rend1x.composeanimatedlist.animation.EnterSpec
import com.rend1x.composeanimatedlist.animation.ExitSpec
import com.rend1x.composeanimatedlist.animation.PlacementBehavior
import com.rend1x.composeanimatedlist.animation.initialAlpha
import com.rend1x.composeanimatedlist.animation.initialOffsetDp
import com.rend1x.composeanimatedlist.animation.targetAlpha
import com.rend1x.composeanimatedlist.animation.targetOffsetDp
import com.rend1x.composeanimatedlist.animation.visibilityAnimationDurationMillis
import com.rend1x.composeanimatedlist.core.AnimatedListItem
import com.rend1x.composeanimatedlist.core.PresenceState
import com.rend1x.composeanimatedlist.state.AnimatedListRenderState
import com.rend1x.composeanimatedlist.state.AnimatedListState
import com.rend1x.composeanimatedlist.state.rememberAnimatedListState
import kotlin.math.abs
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val MIN_PRESENT_SETTLE_DURATION_MILLIS = 120
private const val PROGRESS_EPSILON = 1e-4f

/**
 * Column-based list with diff-driven enter/exit animations.
 *
 * The item [content] lambda receives [AnimatedItemScope]. Recommended: [animatedItem] on the row with
 * [transitionSpec] = [com.rend1x.composeanimatedlist.animation.AnimatedItemDefaults.none] (README “Usage”).
 * Advanced: [AnimatedItemScope.phase] ([ItemPhase]) for lifecycle, and [AnimatedItemScope.visibilityProgress],
 * [AnimatedItemScope.placementProgress], and [AnimatedItemScope.progress] for custom motion with a non-`none`
 * [transitionSpec]. Full semantics are on [AnimatedItemScope].
 *
 * **Update and transition semantics** (see README “Behavior guarantees” for full detail):
 *
 * - **Input updates:** Each time [items] or [key] changes, the internal render list is updated by
 *   diffing from the previous render snapshot. Rapid recompositions coalesce: the coroutine tied to
 *   [items] is restarted, and the last committed [items] wins for that frame’s effect run.
 * - **Exiting retention:** Removed keys stay composed until their exit animation finishes (or
 *   [AnimatedListState.clearExitingNow] is used).
 * - **Reinsertion:** If a key is removed and added back before exit removal, it becomes [ItemPhase.Visible]
 *   with the new element value—it does not re-enter as [ItemPhase.Entering].
 * - **Zero-duration visibility:** [EnterSpec] / [ExitSpec] with [com.rend1x.composeanimatedlist.animation.EnterSpec.None],
 *   [com.rend1x.composeanimatedlist.animation.ExitSpec.None], or `durationMillis = 0` use tweens of length `0`;
 *   the animation completes to its target in the same [LaunchedEffect] run, then exit completion runs.
 * - **Keys:** [key] must be unique within each [items] snapshot. Debug builds of this library fail
 *   fast with a clear error; release builds keep the last item per duplicated key (see README).
 *
 * ## Animation interruption semantics
 *
 * These rules apply when [items] or [transitionSpec] changes while a row’s shell animation is still
 * in progress (see README **Animation interruption semantics**).
 *
 * - **Latest state wins:** After each applied update, render presence and element values match the
 *   diff of the last committed [items] snapshot. Intermediate snapshots that never commit may be
 *   skipped by composition.
 * - **Continuity from current values:** When a running shell animation is replaced (presence change,
 *   or a new [transitionSpec] / channel that restarts the effect), the next tween starts from the
 *   **current** animated alpha, translation, and height progress—not from enter “initial” or other
 *   implicit resets. Targets are always the values defined for the **new** step (enter toward visible,
 *   exit toward off-screen, present-settle toward resting visible).
 * - **Reinsert during [ItemPhase.Exiting]:** The engine marks the row [ItemPhase.Visible] (not
 *   [ItemPhase.Entering]). The shell does not snap back to enter-start; it continues from current
 *   animated values toward the visible resting targets (present-settle).
 * - **Remove during [ItemPhase.Entering]:** The row becomes [ItemPhase.Exiting]. The shell animates
 *   from **current** values toward exit targets. This is **not** a guaranteed literal reversal of the
 *   enter curve; it is continuity toward the exit end state.
 * - **Changing [transitionSpec] mid-animation:** The running effect restarts; new tweens start from
 *   **current** values and run to the targets implied by the new spec and current presence.
 */
@Composable
fun <T> AnimatedColumn(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    state: AnimatedListState = rememberAnimatedListState(),
    transitionSpec: AnimatedItemTransitionSpec = AnimatedItemDefaults.fadeSlide(),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable AnimatedItemScope.(T) -> Unit,
) {
    AnimatedColumn(
        items = items,
        key = key,
        modifier = modifier,
        state = state,
        transitionSpec = transitionSpec,
        horizontalAlignment = horizontalAlignment,
        content = { item, _ -> content(item) },
    )
}

/**
 * Column-based list with diff-driven enter/exit animations.
 *
 * This overload also passes the row's current internal render index to [content]. The index includes
 * retained [ItemPhase.Exiting] rows until their exit animation finishes or the row is otherwise
 * cleared from the render state.
 */
@Composable
fun <T> AnimatedColumn(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    state: AnimatedListState = rememberAnimatedListState(),
    transitionSpec: AnimatedItemTransitionSpec = AnimatedItemDefaults.fadeSlide(),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable AnimatedItemScope.(T, Int) -> Unit,
) {
    AnimatedListLayout(
        items = items,
        key = key,
        modifier = modifier,
        state = state,
        transitionSpec = transitionSpec,
        horizontalAlignment = horizontalAlignment,
        orientation = AnimatedListOrientation.Vertical,
        content = content,
    )
}

@Composable
internal fun <T> AnimatedListLayout(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier,
    state: AnimatedListState,
    transitionSpec: AnimatedItemTransitionSpec,
    orientation: AnimatedListOrientation,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable AnimatedItemScope.(T, Int) -> Unit,
) {
    val renderState = remember { AnimatedListRenderState(initialItems = items, keySelector = key) }
    val exitingKeys = renderState.renderItems
        .asSequence()
        .filter { it.presence == PresenceState.Exiting }
        .map { it.key }
        .toSet()

    LaunchedEffect(renderState, state) {
        state.syncRenderItems(renderState.renderItems)
    }

    LaunchedEffect(items, key) {
        renderState.update(items = items, keySelector = key)
        state.syncRenderItems(renderState.renderItems)
    }

    LaunchedEffect(renderState, state, exitingKeys) {
        state.setClearExitingCallbacks(
            callback = {
                renderState.clearExitingNow()
                state.syncRenderItems(renderState.renderItems)
            },
            keyCallback = { key ->
                renderState.clearExiting(key)
                state.syncRenderItems(renderState.renderItems)
            },
        )
    }

    fun onEnterFinished(key: Any) {
        renderState.onEnterAnimationFinished(key)
        state.syncRenderItems(renderState.renderItems)
        state.notifyEnterFinished(key)
    }

    fun onExitFinished(key: Any) {
        renderState.onExitAnimationFinished(key)
        state.syncRenderItems(renderState.renderItems)
        state.notifyExitFinished(key)
    }

    when (orientation) {
        AnimatedListOrientation.Vertical -> {
            Column(
                modifier = modifier,
                horizontalAlignment = horizontalAlignment,
            ) {
                renderState.renderItems.forEachIndexed { index, renderItem ->
                    composeKey(renderItem.key) {
                        AnimatedListItem(
                            item = renderItem,
                            index = index,
                            listState = state,
                            transitionSpec = transitionSpec,
                            orientation = orientation,
                            onEnterFinished = { onEnterFinished(renderItem.key) },
                            onExitFinished = { onExitFinished(renderItem.key) },
                            content = content,
                        )
                    }
                }
            }
        }

        AnimatedListOrientation.Horizontal -> {
            Row(
                modifier = modifier,
                verticalAlignment = verticalAlignment,
            ) {
                renderState.renderItems.forEachIndexed { index, renderItem ->
                    composeKey(renderItem.key) {
                        AnimatedListItem(
                            item = renderItem,
                            index = index,
                            listState = state,
                            transitionSpec = transitionSpec,
                            orientation = orientation,
                            onEnterFinished = { onEnterFinished(renderItem.key) },
                            onExitFinished = { onExitFinished(renderItem.key) },
                            content = content,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> AnimatedListItem(
    item: AnimatedListItem<T>,
    index: Int,
    listState: AnimatedListState,
    transitionSpec: AnimatedItemTransitionSpec,
    orientation: AnimatedListOrientation,
    onEnterFinished: () -> Unit,
    onExitFinished: () -> Unit,
    content: @Composable AnimatedItemScope.(T, Int) -> Unit,
) {
    if (transitionSpec.isNone()) {
        AnimatedListItemWithoutShell(
            item = item,
            index = index,
            onEnterFinished = onEnterFinished,
            onExitFinished = onExitFinished,
            content = content,
        )
        return
    }

    val density = LocalDensity.current
    val currentOnEnterFinished by rememberUpdatedState(onEnterFinished)
    val currentOnExitFinished by rememberUpdatedState(onExitFinished)
    val placement = transitionSpec.placement
    val enterOffsetPx = remember(transitionSpec.enter, density) {
        with(density) { transitionSpec.enter.initialOffsetDp.toPx() }
    }
    val exitOffsetPx = remember(transitionSpec.exit, density) {
        with(density) { transitionSpec.exit.targetOffsetDp.toPx() }
    }
    val placementAnimated = remember(placement) { placement is PlacementBehavior.Animated }

    // Animatables are keyed only by [item.key] so they survive recomposition and [transitionSpec]
    // changes; initial values are captured on first composition for this key (see interruption KDoc).
    val initialAlpha = remember(item.key) {
        when (item.presence) {
            PresenceState.Entering -> transitionSpec.enter.initialAlpha
            PresenceState.Present -> 1f
            PresenceState.Exiting -> 1f
        }
    }
    val initialOffsetPx = remember(item.key) {
        when (item.presence) {
            PresenceState.Entering -> with(density) { transitionSpec.enter.initialOffsetDp.toPx() }
            PresenceState.Present -> 0f
            PresenceState.Exiting -> 0f
        }
    }

    val alpha = remember(item.key) { Animatable(initialAlpha) }
    val translation = remember(item.key) { Animatable(initialOffsetPx) }
    val mainAxisSizeProgress = remember(item.key) {
        val initial = when {
            placement is PlacementBehavior.Animated && item.presence == PresenceState.Entering -> 0f
            else -> 1f
        }
        Animatable(initial)
    }

    // Keys include specs so a new transition runs when they change; continuity is preserved because
    // [alpha], [translation], and [mainAxisSizeProgress] are not reset here—only post-completion snaps apply.
    LaunchedEffect(item.presence, transitionSpec.enter, transitionSpec.exit, placement) {
        listState.onAnimationStarted()
        try {
            when (item.presence) {
                PresenceState.Entering -> animateEnter(
                    alpha = alpha,
                    translation = translation,
                    mainAxisSizeProgress = mainAxisSizeProgress,
                    enter = transitionSpec.enter,
                    placement = placement,
                ).also {
                    currentOnEnterFinished()
                }

                PresenceState.Present -> animatePresentSettle(
                    alpha = alpha,
                    translation = translation,
                    mainAxisSizeProgress = mainAxisSizeProgress,
                    durationMillis = transitionSpec.presentSettleDurationMillis(),
                )

                PresenceState.Exiting -> {
                    animateExit(
                        density = density,
                        alpha = alpha,
                        translation = translation,
                        mainAxisSizeProgress = mainAxisSizeProgress,
                        exit = transitionSpec.exit,
                        placement = placement,
                    )
                    currentOnExitFinished()
                }
            }
        } finally {
            listState.onAnimationFinished()
        }
    }

    Column(
        modifier = Modifier.animatedListShell(
            orientation = orientation,
            alpha = alpha.value,
            translation = translation.value,
            mainAxisSizeProgress = mainAxisSizeProgress.value,
        ),
    ) {
        val lifecycle = itemLifecycleProgress(
            presence = item.presence,
            enter = transitionSpec.enter,
            exit = transitionSpec.exit,
            alpha = alpha.value,
            translationY = translation.value,
            initialEnterOffsetPx = enterOffsetPx,
            exitTargetOffsetPx = exitOffsetPx,
            sizeProgress = mainAxisSizeProgress.value,
            placementAnimated = placementAnimated,
        )
        val scope = AnimatedItemScopeImpl(
            phase = item.presence.toItemPhase(),
            visibilityProgress = lifecycle.visibilityProgress,
            placementProgress = lifecycle.placementProgress,
        )
        scope.content(item.value, index)
    }
}

@Composable
private fun <T> AnimatedListItemWithoutShell(
    item: AnimatedListItem<T>,
    index: Int,
    onEnterFinished: () -> Unit,
    onExitFinished: () -> Unit,
    content: @Composable AnimatedItemScope.(T, Int) -> Unit,
) {
    val currentOnEnterFinished by rememberUpdatedState(onEnterFinished)
    val currentOnExitFinished by rememberUpdatedState(onExitFinished)

    LaunchedEffect(item.presence) {
        when (item.presence) {
            PresenceState.Entering -> currentOnEnterFinished()
            PresenceState.Present -> Unit
            PresenceState.Exiting -> currentOnExitFinished()
        }
    }

    if (item.presence == PresenceState.Exiting) return

    Column {
        val scope = AnimatedItemScopeImpl(
            phase = item.presence.toItemPhase(),
            visibilityProgress = 1f,
            placementProgress = 1f,
        )
        scope.content(item.value, index)
    }
}

private fun AnimatedItemTransitionSpec.isNone(): Boolean =
    enter == EnterSpec.None && exit == ExitSpec.None && placement == PlacementBehavior.None

private suspend fun animateEnter(
    alpha: Animatable<Float, *>,
    translation: Animatable<Float, *>,
    mainAxisSizeProgress: Animatable<Float, *>,
    enter: EnterSpec,
    placement: PlacementBehavior,
) {
    coroutineScope {
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = enter.visibilityAnimationDurationMillis),
            )
        }
        launch {
            translation.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = enter.visibilityAnimationDurationMillis),
            )
        }
        launch {
            if (placement is PlacementBehavior.Animated) {
                mainAxisSizeProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = placement.durationMillis),
                )
            } else {
                // PlacementBehavior.None: no separate height channel, but the layout still scales
                // by [sizeProgress]. Animate from the current value over the visibility duration so
                // interruptions (e.g. animated → none spec swap) do not jump height.
                mainAxisSizeProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = enter.visibilityAnimationDurationMillis),
                )
            }
        }
    }
    snapShellToVisibleRest(alpha, translation, mainAxisSizeProgress)
}

private suspend fun animatePresentSettle(
    alpha: Animatable<Float, *>,
    translation: Animatable<Float, *>,
    mainAxisSizeProgress: Animatable<Float, *>,
    durationMillis: Int,
) {
    coroutineScope {
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMillis),
            )
        }
        launch {
            translation.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = durationMillis),
            )
        }
        launch {
            mainAxisSizeProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMillis),
            )
        }
    }
    snapShellToVisibleRest(alpha, translation, mainAxisSizeProgress)
}

private fun AnimatedItemTransitionSpec.presentSettleDurationMillis(): Int {
    val placementDuration = (placement as? PlacementBehavior.Animated)?.durationMillis ?: 0
    return maxOf(
        MIN_PRESENT_SETTLE_DURATION_MILLIS,
        enter.visibilityAnimationDurationMillis,
        placementDuration,
    )
}

private suspend fun animateExit(
    density: Density,
    alpha: Animatable<Float, *>,
    translation: Animatable<Float, *>,
    mainAxisSizeProgress: Animatable<Float, *>,
    exit: ExitSpec,
    placement: PlacementBehavior,
) {
    coroutineScope {
        launch {
            alpha.animateTo(
                targetValue = exit.targetAlpha,
                animationSpec = tween(durationMillis = exit.visibilityAnimationDurationMillis),
            )
        }
        launch {
            translation.animateTo(
                targetValue = with(density) { exit.targetOffsetDp.toPx() },
                animationSpec = tween(durationMillis = exit.visibilityAnimationDurationMillis),
            )
        }
        launch {
            if (placement is PlacementBehavior.Animated) {
                mainAxisSizeProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = placement.durationMillis),
                )
            } else {
                // Match visibility exit duration so height does not pop to 0 while fade/slide is
                // still in flight (continuity when [sizeProgress] was mid-range from a prior spec).
                mainAxisSizeProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = exit.visibilityAnimationDurationMillis),
                )
            }
        }
    }
    snapShellToExitRest(alpha, translation, mainAxisSizeProgress, exit, density)
}

/**
 * When a channel has finished its tween, we [Animatable.snapTo] exact rest targets. That is
 * intentional: it clears float drift from repeated interruptions and keeps the shell aligned with
 * discrete lifecycle boundaries. It runs only after a successful [coroutineScope] (not on
 * cancellation). Mid-flight continuity uses [Animatable.animateTo] from the **current** value—never
 * snap-to-initial at effect start; see [AnimatedColumn] KDoc.
 */
private suspend fun snapShellToVisibleRest(
    alpha: Animatable<Float, *>,
    translation: Animatable<Float, *>,
    mainAxisSizeProgress: Animatable<Float, *>,
) {
    alpha.snapTo(1f)
    translation.snapTo(0f)
    mainAxisSizeProgress.snapTo(1f)
}

private suspend fun snapShellToExitRest(
    alpha: Animatable<Float, *>,
    translation: Animatable<Float, *>,
    mainAxisSizeProgress: Animatable<Float, *>,
    exit: ExitSpec,
    density: Density,
) {
    alpha.snapTo(exit.targetAlpha)
    translation.snapTo(with(density) { exit.targetOffsetDp.toPx() })
    mainAxisSizeProgress.snapTo(0f)
}

internal data class ItemLifecycleProgress(val visibilityProgress: Float, val placementProgress: Float)

/** Visible to JVM tests in this module ([ItemShellProgressBoundsTest]). */
internal fun itemLifecycleProgress(
    presence: PresenceState,
    enter: EnterSpec,
    exit: ExitSpec,
    alpha: Float,
    translationY: Float,
    initialEnterOffsetPx: Float,
    exitTargetOffsetPx: Float,
    sizeProgress: Float,
    placementAnimated: Boolean,
): ItemLifecycleProgress = when (presence) {
    PresenceState.Present -> ItemLifecycleProgress(
        visibilityProgress = 1f,
        placementProgress = 1f,
    )

    PresenceState.Entering -> {
        val visibility = enteringVisibilityProgress(
            enter = enter,
            alpha = alpha,
            translationY = translationY,
            initialEnterOffsetPx = initialEnterOffsetPx,
        ).coerceIn(0f, 1f)
        val placement = placementProgressValue(
            sizeProgress = sizeProgress,
            placementAnimated = placementAnimated,
        ).coerceIn(0f, 1f)
        ItemLifecycleProgress(visibilityProgress = visibility, placementProgress = placement)
    }

    PresenceState.Exiting -> {
        val visibility = exitingVisibilityProgress(
            exit = exit,
            alpha = alpha,
            translationY = translationY,
            exitTargetOffsetPx = exitTargetOffsetPx,
        ).coerceIn(0f, 1f)
        val placement = placementProgressValue(
            sizeProgress = sizeProgress,
            placementAnimated = placementAnimated,
        ).coerceIn(0f, 1f)
        ItemLifecycleProgress(visibilityProgress = visibility, placementProgress = placement)
    }
}

internal fun enteringVisibilityProgress(enter: EnterSpec, alpha: Float, translationY: Float, initialEnterOffsetPx: Float): Float {
    val fadeP = when (enter) {
        EnterSpec.None, is EnterSpec.SlideVertical -> 1f
        is EnterSpec.Fade, is EnterSpec.FadeAndSlide -> alpha
    }
    val slideP = when (enter) {
        is EnterSpec.SlideVertical, is EnterSpec.FadeAndSlide -> {
            slideProgressFromTranslation(
                translationY = translationY,
                referenceOffsetPx = initialEnterOffsetPx,
            )
        }

        else -> 1f
    }
    return minOf(fadeP, slideP)
}

internal fun exitingVisibilityProgress(exit: ExitSpec, alpha: Float, translationY: Float, exitTargetOffsetPx: Float): Float {
    val fadeP = when (exit) {
        is ExitSpec.SlideVertical -> 1f
        ExitSpec.None, is ExitSpec.Fade, is ExitSpec.FadeAndSlide -> alpha
    }
    val slideP = when (exit) {
        is ExitSpec.SlideVertical, is ExitSpec.FadeAndSlide -> {
            slideProgressTowardTarget(
                translationY = translationY,
                targetOffsetPx = exitTargetOffsetPx,
            )
        }

        else -> 1f
    }
    return minOf(fadeP, slideP)
}

internal fun placementProgressValue(sizeProgress: Float, placementAnimated: Boolean): Float = if (placementAnimated) sizeProgress else 1f

/** Enter: from [initialEnterOffsetPx] toward 0. */
internal fun slideProgressFromTranslation(translationY: Float, referenceOffsetPx: Float): Float {
    val denom = abs(referenceOffsetPx)
    if (denom < PROGRESS_EPSILON) return 1f
    return (1f - abs(translationY) / denom).coerceIn(0f, 1f)
}

/** Exit: from 0 toward [targetOffsetPx]. */
internal fun slideProgressTowardTarget(translationY: Float, targetOffsetPx: Float): Float {
    val denom = abs(targetOffsetPx)
    if (denom < PROGRESS_EPSILON) return 1f
    return (1f - abs(translationY) / denom).coerceIn(0f, 1f)
}
