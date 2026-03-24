package com.rend1x.composeanimatedlist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key as composeKey
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
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
import com.rend1x.composeanimatedlist.state.AnimatedListItem
import com.rend1x.composeanimatedlist.state.AnimatedListRenderState
import com.rend1x.composeanimatedlist.state.AnimatedListState
import com.rend1x.composeanimatedlist.state.PresenceState
import com.rend1x.composeanimatedlist.state.rememberAnimatedListState
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val PresentSettleDurationMillis = 120
private const val ProgressEpsilon = 1e-4f

/**
 * Column-based list with diff-driven enter/exit animations.
 *
 * The item content lambda is [AnimatedItemScope]: [ItemPhase] plus [AnimatedItemScope.visibilityProgress],
 * [AnimatedItemScope.placementProgress], and [AnimatedItemScope.progress] (their minimum).
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
    val renderState = remember { AnimatedListRenderState(initialItems = items, keySelector = key) }
    val exitingKeys = renderState.renderItems
        .asSequence()
        .filter { it.presence == PresenceState.Exiting }
        .map { it.key }
        .toSet()

    LaunchedEffect(items, key) {
        renderState.update(items = items, keySelector = key)
    }

    LaunchedEffect(exitingKeys) {
        state.setClearExitingCallback {
            renderState.clearExitingNow()
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
    ) {
        renderState.renderItems.forEach { renderItem ->
            composeKey(renderItem.key) {
                AnimatedColumnItem(
                    item = renderItem,
                    listState = state,
                    transitionSpec = transitionSpec,
                    onExitFinished = { renderState.onExitAnimationFinished(renderItem.key) },
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun <T> ColumnScope.AnimatedColumnItem(
    item: AnimatedListItem<T>,
    listState: AnimatedListState,
    transitionSpec: AnimatedItemTransitionSpec,
    onExitFinished: () -> Unit,
    content: @Composable AnimatedItemScope.(T) -> Unit,
) {
    val density = LocalDensity.current
    val currentOnExitFinished by rememberUpdatedState(onExitFinished)
    val placement = transitionSpec.placement

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
    val translationY = remember(item.key) { Animatable(initialOffsetPx) }
    val initialSizeProgress = remember(item.key, placement) {
        when {
            placement is PlacementBehavior.Animated && item.presence == PresenceState.Entering -> 0f
            else -> 1f
        }
    }
    val sizeProgress = remember(item.key) { Animatable(initialSizeProgress) }

    LaunchedEffect(item.presence, transitionSpec.enter, transitionSpec.exit, placement) {
        listState.onAnimationStarted()
        try {
            when (item.presence) {
                PresenceState.Entering -> animateEnter(
                    density = density,
                    alpha = alpha,
                    translationY = translationY,
                    sizeProgress = sizeProgress,
                    enter = transitionSpec.enter,
                    placement = placement,
                )

                PresenceState.Present -> animatePresentSettle(
                    alpha = alpha,
                    translationY = translationY,
                    sizeProgress = sizeProgress,
                )

                PresenceState.Exiting -> {
                    animateExit(
                        density = density,
                        alpha = alpha,
                        translationY = translationY,
                        sizeProgress = sizeProgress,
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
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val width = placeable.width
                val height = (placeable.height * sizeProgress.value).roundToInt()
                layout(width, height) {
                    placeable.placeRelative(0, 0)
                }
            }
            .graphicsLayer {
                this.alpha = alpha.value
                this.translationY = translationY.value
                clip = true
            },
    ) {
        val enterOffsetPx = with(density) { transitionSpec.enter.initialOffsetDp.toPx() }
        val exitOffsetPx = with(density) { transitionSpec.exit.targetOffsetDp.toPx() }
        val placementAnimated = transitionSpec.placement is PlacementBehavior.Animated
        val lifecycle = itemLifecycleProgress(
            presence = item.presence,
            enter = transitionSpec.enter,
            exit = transitionSpec.exit,
            alpha = alpha.value,
            translationY = translationY.value,
            initialEnterOffsetPx = enterOffsetPx,
            exitTargetOffsetPx = exitOffsetPx,
            sizeProgress = sizeProgress.value,
            placementAnimated = placementAnimated,
        )
        val scope = AnimatedItemScopeImpl(
            phase = item.presence.toItemPhase(),
            visibilityProgress = lifecycle.visibilityProgress,
            placementProgress = lifecycle.placementProgress,
        )
        scope.content(item.value)
    }
}

private suspend fun animateEnter(
    density: Density,
    alpha: Animatable<Float, *>,
    translationY: Animatable<Float, *>,
    sizeProgress: Animatable<Float, *>,
    enter: EnterSpec,
    placement: PlacementBehavior,
) {
    alpha.snapTo(enter.initialAlpha)
    translationY.snapTo(with(density) { enter.initialOffsetDp.toPx() })
    if (placement is PlacementBehavior.Animated) {
        sizeProgress.snapTo(0f)
    } else {
        sizeProgress.snapTo(1f)
    }
    coroutineScope {
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = enter.visibilityAnimationDurationMillis),
            )
        }
        launch {
            translationY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = enter.visibilityAnimationDurationMillis),
            )
        }
        launch {
            if (placement is PlacementBehavior.Animated) {
                sizeProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = placement.durationMillis),
                )
            } else {
                sizeProgress.snapTo(1f)
            }
        }
    }
}

private suspend fun animatePresentSettle(
    alpha: Animatable<Float, *>,
    translationY: Animatable<Float, *>,
    sizeProgress: Animatable<Float, *>,
) {
    coroutineScope {
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = PresentSettleDurationMillis),
            )
        }
        launch {
            translationY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = PresentSettleDurationMillis),
            )
        }
        launch {
            sizeProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = PresentSettleDurationMillis),
            )
        }
    }
}

private suspend fun animateExit(
    density: Density,
    alpha: Animatable<Float, *>,
    translationY: Animatable<Float, *>,
    sizeProgress: Animatable<Float, *>,
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
            translationY.animateTo(
                targetValue = with(density) { exit.targetOffsetDp.toPx() },
                animationSpec = tween(durationMillis = exit.visibilityAnimationDurationMillis),
            )
        }
        launch {
            if (placement is PlacementBehavior.Animated) {
                sizeProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = placement.durationMillis),
                )
            } else {
                sizeProgress.snapTo(0f)
            }
        }
    }
}

private fun PresenceState.toItemPhase(): ItemPhase = when (this) {
    PresenceState.Entering -> ItemPhase.Entering
    PresenceState.Present -> ItemPhase.Visible
    PresenceState.Exiting -> ItemPhase.Exiting
}

private data class ItemLifecycleProgress(
    val visibilityProgress: Float,
    val placementProgress: Float,
)

private fun itemLifecycleProgress(
    presence: PresenceState,
    enter: EnterSpec,
    exit: ExitSpec,
    alpha: Float,
    translationY: Float,
    initialEnterOffsetPx: Float,
    exitTargetOffsetPx: Float,
    sizeProgress: Float,
    placementAnimated: Boolean,
): ItemLifecycleProgress {
    return when (presence) {
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
}

private fun enteringVisibilityProgress(
    enter: EnterSpec,
    alpha: Float,
    translationY: Float,
    initialEnterOffsetPx: Float,
): Float {
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

private fun exitingVisibilityProgress(
    exit: ExitSpec,
    alpha: Float,
    translationY: Float,
    exitTargetOffsetPx: Float,
): Float {
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

private fun placementProgressValue(
    sizeProgress: Float,
    placementAnimated: Boolean,
): Float = if (placementAnimated) sizeProgress else 1f

/** Enter: from [initialEnterOffsetPx] toward 0. */
private fun slideProgressFromTranslation(
    translationY: Float,
    referenceOffsetPx: Float,
): Float {
    val denom = abs(referenceOffsetPx)
    if (denom < ProgressEpsilon) return 1f
    return (1f - abs(translationY) / denom).coerceIn(0f, 1f)
}

/** Exit: from 0 toward [targetOffsetPx]. */
private fun slideProgressTowardTarget(
    translationY: Float,
    targetOffsetPx: Float,
): Float {
    val denom = abs(targetOffsetPx)
    if (denom < ProgressEpsilon) return 1f
    return (1f - abs(translationY) / denom).coerceIn(0f, 1f)
}

