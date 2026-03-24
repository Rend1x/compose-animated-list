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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.rend1x.composeanimatedlist.AnimatedItemScope
import com.rend1x.composeanimatedlist.AnimatedItemScopeImpl
import com.rend1x.composeanimatedlist.AnimatedListItem
import com.rend1x.composeanimatedlist.AnimatedListTransition
import com.rend1x.composeanimatedlist.EnterBehavior
import com.rend1x.composeanimatedlist.ExitBehavior
import com.rend1x.composeanimatedlist.PlacementBehavior
import com.rend1x.composeanimatedlist.PresenceState
import com.rend1x.composeanimatedlist.VerticalDirection

/**
 * MVP composable that animates item enter/exit using internal render list state and diffing.
 * LazyColumn support is intentionally out of scope for now.
 */
@Composable
fun <T> AnimatedColumn(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    state: AnimatedListState = rememberAnimatedListState(),
    transition: AnimatedListTransition = AnimatedListTransition.Companion.Default,
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
                    placement = transition.placement,
                    transition = transition,
                    onExitFinished = { renderState.onExitAnimationFinished(renderItem.key) },
                ) {
                    val scope = AnimatedItemScopeImpl(
                        isEntering = renderItem.presence == PresenceState.Entering,
                        isExiting = renderItem.presence == PresenceState.Exiting,
                    )
                    scope.content(renderItem.value)
                }
            }
        }
    }
}

@Composable
private fun <T> ColumnScope.AnimatedColumnItem(
    item: AnimatedListItem<T>,
    listState: AnimatedListState,
    placement: PlacementBehavior,
    transition: AnimatedListTransition,
    onExitFinished: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val currentOnExitFinished by rememberUpdatedState(onExitFinished)

    val initialAlpha = remember(item.key) {
        when (item.presence) {
            PresenceState.Entering -> transition.enter.initialAlpha
            PresenceState.Present -> 1f
            PresenceState.Exiting -> 1f
        }
    }
    val initialOffsetPx = remember(item.key) {
        when (item.presence) {
            PresenceState.Entering -> with(density) { transition.enter.initialOffsetDp.toPx() }
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

    LaunchedEffect(item.presence, transition.enter, transition.exit, placement) {
        listState.onAnimationStarted()
        try {
            when (item.presence) {
                PresenceState.Entering -> {
                    alpha.snapTo(transition.enter.initialAlpha)
                    translationY.snapTo(with(density) { transition.enter.initialOffsetDp.toPx() })
                    if (placement is PlacementBehavior.Animated) {
                        sizeProgress.snapTo(0f)
                    } else {
                        sizeProgress.snapTo(1f)
                    }
                    coroutineScope {
                        launch {
                            alpha.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = transition.enter.durationMillis),
                            )
                        }
                        launch {
                            translationY.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = transition.enter.durationMillis),
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

                PresenceState.Present -> {
                    coroutineScope {
                        launch {
                            alpha.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 120),
                            )
                        }
                        launch {
                            translationY.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 120),
                            )
                        }
                        launch {
                            sizeProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 120),
                            )
                        }
                    }
                }

                PresenceState.Exiting -> {
                    coroutineScope {
                        launch {
                            alpha.animateTo(
                                targetValue = transition.exit.targetAlpha,
                                animationSpec = tween(durationMillis = transition.exit.durationMillis),
                            )
                        }
                        launch {
                            translationY.animateTo(
                                targetValue = with(density) { transition.exit.targetOffsetDp.toPx() },
                                animationSpec = tween(durationMillis = transition.exit.durationMillis),
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
        content()
    }
}

private val EnterBehavior.initialAlpha: Float
    get() = when (this) {
        EnterBehavior.None -> 1f
        is EnterBehavior.Fade -> 0f
        is EnterBehavior.SlideVertical -> 1f
        is EnterBehavior.FadeAndSlide -> 0f
    }

private val EnterBehavior.initialOffsetDp: Dp
    get() = when (this) {
        EnterBehavior.None -> 0.dp
        is EnterBehavior.Fade -> 0.dp
        is EnterBehavior.SlideVertical -> offset
        is EnterBehavior.FadeAndSlide -> offset
    }

private val EnterBehavior.durationMillis: Int
    get() = when (this) {
        EnterBehavior.None -> 0
        is EnterBehavior.Fade -> durationMillis
        is EnterBehavior.SlideVertical -> durationMillis
        is EnterBehavior.FadeAndSlide -> durationMillis
    }

private val ExitBehavior.targetAlpha: Float
    get() = when (this) {
        ExitBehavior.None -> 0f
        is ExitBehavior.Fade -> 0f
        is ExitBehavior.SlideVertical -> 1f
        is ExitBehavior.FadeAndSlide -> 0f
    }

private val ExitBehavior.targetOffsetDp: Dp
    get() = when (this) {
        ExitBehavior.None -> 0.dp
        is ExitBehavior.Fade -> 0.dp
        is ExitBehavior.SlideVertical -> {
            when (direction) {
                VerticalDirection.Up -> -offset
                VerticalDirection.Down -> offset
            }
        }
        is ExitBehavior.FadeAndSlide -> {
            when (direction) {
                VerticalDirection.Up -> -offset
                VerticalDirection.Down -> offset
            }
        }
    }

private val ExitBehavior.durationMillis: Int
    get() = when (this) {
        ExitBehavior.None -> 0
        is ExitBehavior.Fade -> durationMillis
        is ExitBehavior.SlideVertical -> durationMillis
        is ExitBehavior.FadeAndSlide -> durationMillis
    }
