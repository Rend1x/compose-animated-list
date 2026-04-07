package com.rend1x.composeanimatedlist

import androidx.compose.runtime.Immutable

/**
 * Receiver for each row’s `content` lambda in [AnimatedColumn].
 *
 * **Layers:** For typical fade + slide on the row, use [animatedItem] with [AnimatedColumn] configured
 * with [com.rend1x.composeanimatedlist.animation.AnimatedItemDefaults.none] (see README “Usage”). You do not
 * need the progress properties for that path. Use **[phase]** for lifecycle-aware copy and UI, and use
 * **visibilityProgress / placementProgress / progress** when you implement custom drawing or modifiers that must
 * track the column’s own [com.rend1x.composeanimatedlist.animation.AnimatedItemTransitionSpec] (non-`none`)
 * or height channel explicitly.
 *
 * **Contract (read in order):**
 *
 * 1. **[phase]** — Coarse lifecycle: [ItemPhase.Entering], [ItemPhase.Visible], or [ItemPhase.Exiting].
 *    Use `when (phase)` for copy, icons, or conditional UI.
 * 2. **Progress trio** — Derived from the same animatables [AnimatedColumn] uses for the row shell
 *    (fade, slide, optional height). They are **normalized completion** in `0f..1f`, not raw pixels or
 *    durations. Use them to align *your* modifiers with the list’s motion.
 *
 * **What each progress measures:**
 *
 * - **[visibilityProgress]** — Completion of **fade and vertical slide** from
 *   [com.rend1x.composeanimatedlist.animation.EnterSpec] / [com.rend1x.composeanimatedlist.animation.ExitSpec]
 *   only. It does **not** reflect the row’s clip height; for that use [placementProgress]. It is **1f**
 *   when those channels are fully on-screen for the current internal presence (including after settle),
 *   except during the brief present-settle pass described below.
 * - **[placementProgress]** — Completion of the row’s **reserved height** when
 *   [com.rend1x.composeanimatedlist.animation.PlacementBehavior.Animated] is used (`0f..1f` expand/collapse);
 *   when placement is not animated, this is always **1f** so it does not block [progress].
 * - **[progress]** — `min(visibilityProgress, placementProgress)`. One number when both channels must
 *   feel “done” together (e.g. a single progress bar). Use the split properties when fade/slide and
 *   height should diverge.
 *
 * **Present-settle:** After the internal list marks a row as present, [AnimatedColumn] runs a short
 * settle animation (~120 ms) that nudges alpha, offset, and height toward their resting values. During
 * that window [phase] is already [ItemPhase.Visible], but progress values may still move toward **1f**.
 * Do not assume [ItemPhase.Visible] implies all progress fields are **1f** on every frame.
 *
 * **Stability:** Rapid [items] updates follow README “Behavior guarantees” (exiting retention,
 * reinsertion without a second enter, ordering of applied diffs).
 *
 * ## Animation interruption semantics
 *
 * When [AnimatedColumn] applies a new [items] snapshot or a new [transitionSpec] while this row’s
 * shell animation is still running, behavior follows [AnimatedColumn] KDoc and README **Animation
 * interruption semantics**. In short: **latest committed [items] wins** for phase and value; shell
 * tweens **continue from current animated values** toward the target for the new step. Reinsertion
 * during [ItemPhase.Exiting] does **not** restart as [ItemPhase.Entering]. Removal during
 * [ItemPhase.Entering] moves to [ItemPhase.Exiting] by animating from **current** values toward exit
 * targets—there is **no** guarantee that motion is a literal reversal of the enter animation, only
 * continuity toward the new targets.
 */
@Immutable
interface AnimatedItemScope {
    /**
     * Current [ItemPhase] for this key; the main public lifecycle signal. See [ItemPhase].
     */
    val phase: ItemPhase

    /**
     * Fade/slide completion for the row (`0f..1f`). See class documentation for present-settle and
     * the distinction from [placementProgress].
     */
    val visibilityProgress: Float

    /**
     * Animated row-height completion (`0f..1f`) when placement is animated; otherwise **1f**.
     */
    val placementProgress: Float

    /**
     * `min(visibilityProgress, placementProgress)` — conservative combined completion.
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
