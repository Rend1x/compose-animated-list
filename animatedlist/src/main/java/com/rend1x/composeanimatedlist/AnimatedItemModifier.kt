package com.rend1x.composeanimatedlist

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
fun Modifier.animatedItem(
    scope: AnimatedItemScope,
    slideRevealOffset: Dp = 24.dp,
): Modifier = composed {
    val slideRevealPx = with(LocalDensity.current) { slideRevealOffset.toPx() }
    Modifier.graphicsLayer {
        val p = scope.visibilityProgress
        alpha = p
        translationY = (1f - p) * slideRevealPx
    }
}
