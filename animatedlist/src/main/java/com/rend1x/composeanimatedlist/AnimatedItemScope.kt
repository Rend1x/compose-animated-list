package com.rend1x.composeanimatedlist

import androidx.compose.runtime.Immutable

/**
 * Receiver scope for each item’s content in [AnimatedColumn].
 *
 * **Progress model:** Two values describe the transition, plus a single convenience aggregate:
 *
 * - **[visibilityProgress]** — Alpha and slide (fade/slide specs only). Ignores animated height.
 *   Use this for inner content effects that should track “how visible” the item is on screen.
 * - **[placementProgress]** — Reserved height for the row (1 when placement is not animated).
 *   Use this when you need to mirror or reason about the list’s height animation.
 * - **[progress]** — `min(visibilityProgress, placementProgress)` — overall lifecycle completion
 *   for the row. Matches the previous single-progress behavior and advances only as fast as the
 *   slowest sub-transition.
 *
 * All progress values are in `0f..1f` for **Entering** and **Exiting**, and `1f` while **Visible**.
 */
@Immutable
interface AnimatedItemScope {
    val phase: ItemPhase

    /**
     * How far the item’s **visual** enter/exit has completed: fade and slide, not row height.
     * Entering: `0 → 1`, Visible: `1`, Exiting: `1 → 0`.
     */
    val visibilityProgress: Float

    /**
     * How far the row’s **layout height** is expanded or collapsed when
     * [com.rend1x.composeanimatedlist.animation.PlacementBehavior.Animated] is used; otherwise `1f`.
     */
    val placementProgress: Float

    /**
     * Overall completion: the lesser of [visibilityProgress] and [placementProgress].
     * Prefer the split properties when you need independent fade/slide vs height behavior.
     */
    val progress: Float
}

internal data class AnimatedItemScopeImpl(
    override val phase: ItemPhase,
    override val visibilityProgress: Float,
    override val placementProgress: Float,
) : AnimatedItemScope {
    override val progress: Float
        get() = kotlin.math.min(visibilityProgress, placementProgress)
}
