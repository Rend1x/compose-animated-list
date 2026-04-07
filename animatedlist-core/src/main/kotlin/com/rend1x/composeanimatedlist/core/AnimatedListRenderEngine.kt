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
     * Applies one logical update: diffs the previous render snapshot against [items] using
     * [keySelector] and the [AnimatedListKeyPolicy] from construction. Full rules (idempotence,
     * insertion/removal, reinsert, key policy, ordering) are documented on [AnimatedListDiffer.diff].
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
