package com.rend1x.composeanimatedlist.core

/**
 * Pure diff/lifecycle engine: maintains the render list (including exiting rows) from input updates.
 *
 * Not tied to Compose; snapshot [items] after each mutating call for UI or tests.
 */
class AnimatedListRenderEngine<T>(
    initialItems: List<T>,
    keySelector: (T) -> Any,
    private val keyPolicy: AnimatedListKeyPolicy,
) {
    var items: List<AnimatedListItem<T>> = buildInitial(initialItems, keySelector)
        private set

    /**
     * Applies one logical update: diffs the previous render snapshot against the new input list
     * [items] using [keySelector] and [AnimatedListKeyPolicy] from construction.
     *
     * **Guaranteed behavior** (see [AnimatedListDiffer.diff] for the full rules):
     *
     * - **Idempotence:** Calling [update] twice in a row with equal [items] lists and the same
     *   [keySelector] leaves this engine’s snapshot unchanged (same keys, values, and [PresenceState]
     *   for every row; see [AnimatedListRenderEngine.items]).
     * - **New keys:** Any key that appears in [newItems] but was absent from the previous snapshot
     *   is inserted as [PresenceState.Entering] (unless duplicate-key policy collapses it; see below).
     * - **Removed keys:** Any key that was in the previous snapshot but is absent from [newItems]
     *   becomes [PresenceState.Exiting] and remains in the list until [clearExitingNow] or
     *   [onExitAnimationFinished] removes it.
     * - **Reinsert while exiting:** If a key is [PresenceState.Exiting] and appears again in [newItems],
     *   that row becomes [PresenceState.Present] with the element value taken from [newItems] (it does
     *   **not** become [PresenceState.Entering] again).
     * - **Stable order:** Rows that stay in the list keep their relative order from the previous
     *   snapshot; new keys are placed according to [newItems] order relative to keys already on screen
     *   (see [AnimatedListDiffer.diff]).
     * - **Keys:** With [AnimatedListKeyPolicy.Strict], duplicate keys in [newItems] (or in the initial
     *   constructor input) throw [IllegalStateException]. With [AnimatedListKeyPolicy.LastWins],
     *   duplicates collapse to one element per key (the **last** occurrence in the input list); row
     *   order in the sanitized list follows **increasing last-occurrence index** (see
     *   [AnimatedListKeys.sanitizeAnimatedListInput]).
     */
    fun update(items: List<T>, keySelector: (T) -> Any) {
        this.items = AnimatedListDiffer.diff(
            current = this.items,
            newItems = items,
            keySelector = keySelector,
            keyPolicy = keyPolicy,
        )
    }

    fun onExitAnimationFinished(key: Any) {
        items = items.filterNot { item ->
            item.key == key && item.presence == PresenceState.Exiting
        }
    }

    fun clearExitingNow() {
        items = items.filterNot { it.presence == PresenceState.Exiting }
    }

    private fun buildInitial(
        initialItems: List<T>,
        keySelector: (T) -> Any,
    ): List<AnimatedListItem<T>> {
        val sanitized = AnimatedListKeys.sanitizeAnimatedListInput(initialItems, keySelector, keyPolicy)
        return sanitized.map { item ->
            AnimatedListItem(
                key = keySelector(item),
                value = item,
                presence = PresenceState.Present,
            )
        }
    }
}
