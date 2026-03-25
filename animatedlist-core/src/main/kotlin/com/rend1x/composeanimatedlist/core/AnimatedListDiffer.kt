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
     * [PresenceState.Exiting]. Exiting rows stay in **the same relative order** they had in [current]
     * with respect to each other and to rows that are still keyed by [newItems].
     *
     * **Presence → value sync:** For each key that appears in [newItems], the output row uses the
     * element value from [newItems] (after sanitization). If the row was [PresenceState.Exiting], it
     * becomes [PresenceState.Present]. If it was [PresenceState.Entering] and the new value is
     * [equals] to the previous value, it stays [PresenceState.Entering]; otherwise it becomes
     * [PresenceState.Present]. Rows that were already [PresenceState.Present] stay [PresenceState.Present].
     *
     * **Insertion → Entering:** Each key that appears in sanitized [newItems] but had **no** row in
     * [current] is inserted as [PresenceState.Entering]. Insert position is deterministic: scan
     * [newItems] in order; for each new key, place it immediately after the nearest preceding key in
     * [newItems] that already exists in the working list, or immediately before the nearest following
     * such key, or append if neither exists.
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
        val currentKeys = current.mapTo(linkedSetOf()) { it.key }

        val render = current.mapTo(mutableListOf<AnimatedListItem<T>>()) { currentItem ->
            val updatedValue = newKeyed[currentItem.key]
            if (updatedValue == null) {
                currentItem.copy(presence = PresenceState.Exiting)
            } else {
                val presence =
                    if (currentItem.presence == PresenceState.Entering && currentItem.value == updatedValue) {
                        PresenceState.Entering
                    } else {
                        PresenceState.Present
                    }
                AnimatedListItem(
                    key = currentItem.key,
                    value = updatedValue,
                    presence = presence,
                )
            }
        }

        sanitizedNewItems.forEachIndexed { index, item ->
            val key = keySelector(item)
            if (key in currentKeys) return@forEachIndexed

            val previousKnownKey = sanitizedNewItems
                .subList(0, index)
                .asReversed()
                .firstNotNullOfOrNull { candidate ->
                    val candidateKey = keySelector(candidate)
                    candidateKey.takeIf { presentKey -> render.any { it.key == presentKey } }
                }

            if (previousKnownKey != null) {
                val previousIndex = render.indexOfFirst { it.key == previousKnownKey }
                render.add(
                    index = previousIndex + 1,
                    element = AnimatedListItem(
                        key = key,
                        value = item,
                        presence = PresenceState.Entering,
                    ),
                )
                return@forEachIndexed
            }

            val nextKnownKey = sanitizedNewItems
                .subList(index + 1, sanitizedNewItems.size)
                .firstNotNullOfOrNull { candidate ->
                    val candidateKey = keySelector(candidate)
                    candidateKey.takeIf { presentKey -> render.any { it.key == presentKey } }
                }

            if (nextKnownKey != null) {
                val nextIndex = render.indexOfFirst { it.key == nextKnownKey }
                render.add(
                    index = nextIndex,
                    element = AnimatedListItem(
                        key = key,
                        value = item,
                        presence = PresenceState.Entering,
                    ),
                )
            } else {
                render.add(
                    AnimatedListItem(
                        key = key,
                        value = item,
                        presence = PresenceState.Entering,
                    ),
                )
            }
        }

        return render.map { item ->
            if (item.key in newKeys && item.presence == PresenceState.Exiting) {
                item.copy(presence = PresenceState.Present)
            } else {
                item
            }
        }
    }
}
