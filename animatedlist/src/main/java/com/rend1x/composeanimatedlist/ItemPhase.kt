package com.rend1x.composeanimatedlist

/**
 * **Primary public lifecycle state** for one row in [AnimatedColumn].
 *
 * Read this first, then use [AnimatedItemScope.visibilityProgress], [AnimatedItemScope.placementProgress],
 * and [AnimatedItemScope.progress] to mirror motion in your own composables. Progress fields describe
 * how far the row’s built-in transition has run; they are **not** a second phase axis—[phase] is the
 * only coarse lifecycle discriminator the library exposes.
 *
 * **Typical path:** [Entering] → [Visible] → [Exiting] for a given key. If a key is removed and added
 * back before the exiting row is dropped, it returns to [Visible] with the new value and does **not**
 * become [Entering] again (see README “Behavior guarantees”).
 *
 * **Relation to progress:** While [Entering] or [Exiting], progress values move between `0f` and `1f`
 * according to the active enter/exit specs and placement. While [Visible], the row is logically in the
 * list and not exiting; [AnimatedItemScope] progress may still be below `1f` for a short **present-settle**
 * interval after enter (see [AnimatedItemScope]).
 *
 * For default row visuals, use [animatedItem] on the row (README “Usage”); [phase] and progress stay the
 * advanced customization layer.
 */
enum class ItemPhase {
    /**
     * The key is new to the render list: enter transition (fade/slide and optional height) may still run.
     */
    Entering,

    /**
     * The key is in the current input list and not scheduled for removal. Layout participates like a
     * normal row. Progress may briefly be below `1f` only during present-settle after enter.
     */
    Visible,

    /**
     * The key was removed from the input list but the row is retained until exit animation finishes or
     * the list is cleared; exit transition may still run.
     */
    Exiting,
}
