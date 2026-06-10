package com.rend1x.composeanimatedlist

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * **Default convenience API** for row visuals in [AnimatedColumn]: maps [AnimatedItemScope.visibilityProgress]
 * to alpha and a vertical slide-in/out in **pixels derived from [slideRevealOffset]** at the current density.
 *
 * ### When to use this (recommended)
 * - Pair [AnimatedColumn] with [com.rend1x.composeanimatedlist.animation.AnimatedItemDefaults.none] so the
 *   column shell does **not** apply its own fade/slide (only diff + lifecycle). Then apply
 *   `Modifier.animatedItem(this)` on the content you want to animate. **Otherwise** the same motion runs
 *   twice (shell + modifier) and looks over-blended.
 * - You do **not** need to read [AnimatedItemScope.visibilityProgress] manually for a standard fade + slide row.
 *
 * ### When *not* to use this (advanced)
 * - Use [AnimatedItemScope.phase] and [AnimatedItemScope.visibilityProgress] / [AnimatedItemScope.placementProgress]
 *   / [AnimatedItemScope.progress] directly in [Modifier.graphicsLayer], draw APIs, or layout when you need
 *   effects this helper does not cover (e.g. scale, custom curves, mirroring height with [placementProgress]).
 * - If you keep a non-[none][com.rend1x.composeanimatedlist.animation.AnimatedItemDefaults.none] [transitionSpec]
 *   on [AnimatedColumn], treat the shell as the primary motion; avoid stacking this modifier on the same surface
 *   unless you intentionally want compounded opacity/offset.
 *
 * ### Contract
 * - **Input:** [scope.visibilityProgress] in `0f..1f` from the column (fade/slide channel only; not row clip height).
 * - **Output:** `alpha = visibilityProgress`, `translationY = (1 - visibilityProgress) * slideRevealPx`
 *   (revealed row sits at `0`; entering/exiting rows are offset toward positive Y when offset is positive).
 *
 * @param scope Receiver scope from the [AnimatedColumn] item lambda (`this`).
 * @param slideRevealOffset Vertical offset at **fully hidden** visibility (`visibilityProgress == 0`); uses
 *   current [LocalDensity] to convert to pixels. Default matches the previous `24f` px default at ~mdpi.
 */
fun Modifier.animatedItem(scope: AnimatedItemScope, slideRevealOffset: Dp = 24.dp): Modifier =
    animatedItemFadeSlide(scope = scope, slideRevealOffset = slideRevealOffset)

/**
 * Fade + vertical slide helper for item content driven by [AnimatedItemScope.visibilityProgress].
 *
 * This is the named variant of [animatedItem]. Use either helper on a single surface, not both. It is safe
 * to combine with [animatedItemScale] when you want scale in addition to fade/slide; avoid combining with
 * another helper that also writes alpha or `translationY` on the same surface unless you intentionally want
 * compounded motion.
 *
 * @param scope Receiver scope from the [AnimatedColumn] or [AnimatedRow] item lambda (`this`).
 * @param slideRevealOffset Y offset at fully hidden visibility (`visibilityProgress == 0`).
 */
fun Modifier.animatedItemFadeSlide(scope: AnimatedItemScope, slideRevealOffset: Dp = 24.dp): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "animatedItemFadeSlide"
        properties["scope"] = scope
        properties["slideRevealOffset"] = slideRevealOffset
    },
) {
    val slideRevealPx = with(LocalDensity.current) { slideRevealOffset.toPx() }
    this.graphicsLayer {
        val values = animatedItemFadeSlideValues(scope.visibilityProgress, slideRevealPx)
        alpha = values.alpha
        translationY = values.translationY
    }
}

/**
 * Scale helper for item content driven by [AnimatedItemScope.visibilityProgress].
 *
 * This helper only writes `scaleX`/`scaleY` and [transformOrigin], so it can be combined with
 * [animatedItemFadeSlide] or [animatedItemSwipeOut] when scale is the only extra effect you need.
 *
 * @param scope Receiver scope from the [AnimatedColumn] or [AnimatedRow] item lambda (`this`).
 * @param minScale Scale at fully hidden visibility (`visibilityProgress == 0`).
 * @param maxScale Scale at fully visible visibility (`visibilityProgress == 1`).
 * @param transformOrigin Pivot used for the scale transform.
 */
fun Modifier.animatedItemScale(
    scope: AnimatedItemScope,
    minScale: Float = 0.92f,
    maxScale: Float = 1f,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
): Modifier = graphicsLayer {
    val scale = animatedItemScaleValue(
        progress = scope.visibilityProgress,
        minScale = minScale,
        maxScale = maxScale,
    )
    scaleX = scale
    scaleY = scale
    this.transformOrigin = transformOrigin
}

/**
 * Collapses the measured main-axis size using [AnimatedItemScope.placementProgress].
 *
 * Place this outside visual helpers:
 *
 * ```
 * Modifier
 *     .animatedItemCollapse(this)
 *     .animatedItemFadeSlide(this)
 * ```
 *
 * The helper clips by reporting a smaller layout size and placing the child at the origin. Use the default
 * vertical axis for [AnimatedColumn] rows; pass `horizontal = true` for [AnimatedRow] items. It is safe to
 * combine with graphics helpers because this modifier writes layout size only.
 *
 * @param scope Receiver scope from the [AnimatedColumn] or [AnimatedRow] item lambda (`this`).
 * @param horizontal Collapse width instead of height.
 */
fun Modifier.animatedItemCollapse(scope: AnimatedItemScope, horizontal: Boolean = false): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val size = animatedItemCollapseSize(
        width = placeable.width,
        height = placeable.height,
        progress = scope.placementProgress,
        horizontal = horizontal,
    )
    layout(size.width, size.height) {
        placeable.placeRelative(0, 0)
    }
}.graphicsLayer {
    clip = true
}

/**
 * Material-style shared-axis Y helper driven by [AnimatedItemScope.visibilityProgress] and [AnimatedItemScope.phase].
 *
 * Entering items move from the positive Y direction when [forward] is true; exiting items move toward the
 * negative Y direction. Set [forward] to false to mirror that direction. This helper writes alpha and
 * `translationY`, so do not combine it with [animatedItemFadeSlide] on the same surface.
 *
 * @param scope Receiver scope from the [AnimatedColumn] or [AnimatedRow] item lambda (`this`).
 * @param distance Y travel at fully hidden visibility (`visibilityProgress == 0`).
 * @param forward Direction pair for enter/exit motion.
 */
fun Modifier.animatedItemSharedAxisY(scope: AnimatedItemScope, distance: Dp = 30.dp, forward: Boolean = true): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "animatedItemSharedAxisY"
        properties["scope"] = scope
        properties["distance"] = distance
        properties["forward"] = forward
    },
) {
    val distancePx = with(LocalDensity.current) { distance.toPx() }
    this.graphicsLayer {
        val values = animatedItemSharedAxisYValues(
            progress = scope.visibilityProgress,
            phase = scope.phase,
            distancePx = distancePx,
            forward = forward,
        )
        alpha = values.alpha
        translationY = values.translationY
    }
}

/**
 * Horizontal swipe-out helper for removals.
 *
 * This helper only translates exiting items. With [fade] enabled it also fades exiting items by writing alpha;
 * set [fade] to false when another helper already owns alpha. Entering and visible items are not translated by
 * this helper.
 *
 * @param scope Receiver scope from the [AnimatedColumn] or [AnimatedRow] item lambda (`this`).
 * @param offset X travel at fully hidden visibility (`visibilityProgress == 0`). Use a negative value to swipe left.
 * @param fade Whether exiting items should fade with [AnimatedItemScope.visibilityProgress].
 */
fun Modifier.animatedItemSwipeOut(scope: AnimatedItemScope, offset: Dp = 96.dp, fade: Boolean = true): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "animatedItemSwipeOut"
        properties["scope"] = scope
        properties["offset"] = offset
        properties["fade"] = fade
    },
) {
    val offsetPx = with(LocalDensity.current) { offset.toPx() }
    this.graphicsLayer {
        val values = animatedItemSwipeOutValues(
            progress = scope.visibilityProgress,
            phase = scope.phase,
            offsetPx = offsetPx,
            fade = fade,
        )
        alpha = values.alpha
        translationX = values.translationX
    }
}

internal data class AnimatedItemLayerValues(val alpha: Float, val translationX: Float = 0f, val translationY: Float = 0f)

internal data class AnimatedItemCollapseSize(val width: Int, val height: Int)

internal fun animatedItemFadeSlideValues(progress: Float, slideRevealPx: Float): AnimatedItemLayerValues {
    val p = progress.coerceIn(0f, 1f)
    return AnimatedItemLayerValues(
        alpha = p,
        translationY = (1f - p) * slideRevealPx,
    )
}

internal fun animatedItemScaleValue(progress: Float, minScale: Float, maxScale: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return minScale + (maxScale - minScale) * p
}

internal fun animatedItemCollapseSize(width: Int, height: Int, progress: Float, horizontal: Boolean): AnimatedItemCollapseSize {
    val p = progress.coerceIn(0f, 1f)
    return if (horizontal) {
        AnimatedItemCollapseSize(width = (width * p).roundToInt(), height = height)
    } else {
        AnimatedItemCollapseSize(width = width, height = (height * p).roundToInt())
    }
}

internal fun animatedItemSharedAxisYValues(
    progress: Float,
    phase: ItemPhase,
    distancePx: Float,
    forward: Boolean,
): AnimatedItemLayerValues {
    val p = progress.coerceIn(0f, 1f)
    val direction = when (phase) {
        ItemPhase.Entering -> if (forward) 1f else -1f
        ItemPhase.Visible -> 0f
        ItemPhase.Exiting -> if (forward) -1f else 1f
    }
    return AnimatedItemLayerValues(
        alpha = p,
        translationY = (1f - p) * distancePx * direction,
    )
}

internal fun animatedItemSwipeOutValues(progress: Float, phase: ItemPhase, offsetPx: Float, fade: Boolean): AnimatedItemLayerValues {
    val p = progress.coerceIn(0f, 1f)
    val exiting = phase == ItemPhase.Exiting
    return AnimatedItemLayerValues(
        alpha = if (fade && exiting) p else 1f,
        translationX = if (exiting) (1f - p) * offsetPx else 0f,
    )
}
