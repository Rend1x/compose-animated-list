package com.rend1x.composeanimatedlist.core

/**
 * Stateless diff from a previous render snapshot [current] to the next caller [newItems] list.
 *
 * Prefer calling [com.rend1x.composeanimatedlist.core.AnimatedListRenderEngine.update] in app code;
 * this object is exposed for tests and advanced use.
 */
object AnimatedListDiffer {

    /**
     * Returns the next render list after applying one logical update.
     *
     * **Removal → Exiting:** For each row in [current] whose key is not present in [newItems] (after
     * applying [keyPolicy] to [newItems]), the output row keeps the same key and value but has
     * [PresenceState.Exiting]. Rows that are present in [newItems] follow [newItems] order. Exiting
     * rows keep their relative order and stay near their previous neighbors where possible.
     *
     * **Presence → value sync:** For each key that appears in [newItems], the output row uses the
     * element value from [newItems] (after sanitization). If the row was [PresenceState.Exiting], it
     * becomes [PresenceState.Present]. If it was [PresenceState.Entering] and the new value is
     * [equals] to the previous value, it stays [PresenceState.Entering]; otherwise it becomes
     * [PresenceState.Present]. Rows that were already [PresenceState.Present] stay [PresenceState.Present].
     *
     * **Insertion → Entering:** Each key that appears in sanitized [newItems] but had **no** row in
     * [current] is inserted as [PresenceState.Entering] at its [newItems] position.
     *
     * **Idempotence:** If [newItems] and [keySelector] describe the same logical assignment of keys
     * to values as the previous step already reflected in [current] (including [PresenceState] for
     * rows still [PresenceState.Entering] with the same value), the returned list is **equal** to
     * [current] (same size, order, key, value, and presence for each index).
     *
     * **Chaining:** Each call performs exactly one update. Repeated calls use the previous call’s
     * output as the next [current]; the result is always the same as composing the updates in order.
     *
     * **Duplicate keys in [newItems]:** Under [AnimatedListKeyPolicy.Strict], [sanitizeAnimatedListInput]
     * throws [IllegalStateException]. Under [AnimatedListKeyPolicy.LastWins], [newItems] is replaced
     * by a list with one row per key: the kept element is always the **last** occurrence of that key
     * in [newItems]. Rows are ordered by **increasing index of that last occurrence** in the original
     * [newItems] (a key whose last duplicate appears earlier comes before a key whose last duplicate
     * appears later).
     */
    fun <T> diff(
        current: List<AnimatedListItem<T>>,
        newItems: List<T>,
        keySelector: (T) -> Any,
        keyPolicy: AnimatedListKeyPolicy,
    ): List<AnimatedListItem<T>> {
        val sanitizedNewItems = AnimatedListKeys.sanitizeAnimatedListInput(newItems, keySelector, keyPolicy)
        val newKeyed = sanitizedNewItems.associateByTo(linkedMapOf(), keySelector)
        val newKeys = newKeyed.keys
        val currentByKey = current.associateBy { it.key }

        val render = sanitizedNewItems.mapTo(mutableListOf()) { item ->
            val key = keySelector(item)
            val currentItem = currentByKey[key]
            if (currentItem == null) {
                AnimatedListItem(
                    key = key,
                    value = item,
                    presence = PresenceState.Entering,
                )
            } else {
                val presence =
                    if (currentItem.presence == PresenceState.Entering && currentItem.value == item) {
                        PresenceState.Entering
                    } else {
                        PresenceState.Present
                    }
                AnimatedListItem(
                    key = key,
                    value = item,
                    presence = presence,
                )
            }
        }

        current
            .filter { it.key !in newKeys }
            .forEach { exitingItem ->
                val insertIndex = nearestFollowingSurvivorIndex(
                    current = current,
                    render = render,
                    exitingKey = exitingItem.key,
                    newKeys = newKeys,
                ) ?: render.size
                render.add(insertIndex, exitingItem.copy(presence = PresenceState.Exiting))
        }

        return render
    }
}

private fun <T> nearestFollowingSurvivorIndex(
    current: List<AnimatedListItem<T>>,
    render: List<AnimatedListItem<T>>,
    exitingKey: Any,
    newKeys: Set<Any>,
): Int? {
    val currentIndex = current.indexOfFirst { it.key == exitingKey }
    if (currentIndex < 0) return null

    val followingKey = current
        .asSequence()
        .drop(currentIndex + 1)
        .firstOrNull { it.key in newKeys }
        ?.key

    return followingKey?.let { key -> render.indexOfFirst { it.key == key }.takeIf { it >= 0 } }
}
