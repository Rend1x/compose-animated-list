package com.rend1x.composeanimatedlist

/**
 * Public lifecycle phase for an item rendered by [AnimatedColumn].
 *
 * Phases are mutually exclusive and usually advance as **Entering → Visible → Exiting** for a given key.
 * If a key is **removed** and **added back** before the exit row is dropped, it becomes **Visible**
 * again with the new data—it does **not** go through **Entering** a second time (see README “Behavior guarantees”).
 * An item in **Visible** is fully laid out and participates in the list like a normal row; **Entering**
 * and **Exiting** mean a presence transition is still in progress for that key.
 *
 * Use [AnimatedItemScope.visibilityProgress] and [AnimatedItemScope.placementProgress] (or
 * [AnimatedItemScope.progress] as their minimum) to drive custom visuals alongside these phases.
 */
enum class ItemPhase {
    /**
     * The item was just inserted; enter animations (fade, slide, and optional height) may still run.
     */
    Entering,

    /**
     * The item is settled: present in the list with no exit scheduled.
     */
    Visible,

    /**
     * The item is being removed; exit animations run before the row is dropped from composition.
     */
    Exiting,
}
