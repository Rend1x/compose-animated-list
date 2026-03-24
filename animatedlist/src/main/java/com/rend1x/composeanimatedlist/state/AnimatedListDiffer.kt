package com.rend1x.composeanimatedlist.state

/**
 * Computes the internal render list for [com.rend1x.composeanimatedlist.AnimatedColumn] from the
 * previous render snapshot and the latest [items] from the caller.
 *
 * **Documented guarantees** (see project README “Behavior guarantees”):
 *
 * - **Exiting retention:** Keys that disappear from [newItems] stay in the output with
 *   [PresenceState.Exiting] and keep the **relative order** they had in [current] (including among
 *   other exiting rows). Non-exiting neighbors keep their values from [newItems].
 * - **Reinsertion:** If a key is present in [newItems] while still in [current] (including as
 *   Exiting), the item becomes [PresenceState.Present] with the **latest value** from [newItems].
 *   It does **not** get [PresenceState.Entering]; only keys that were absent from [current] do.
 * - **Update ordering:** Each call is a single diff from [current] to [newItems]. Chaining calls
 *   (as [AnimatedListRenderState] does when [items] updates repeatedly) applies updates in sequence;
 *   the render list is always the result of the last applied diff.
 * - **Keys:** [newItems] must not contain duplicate keys (see [AnimatedListKeys] and README). Debug
 *   library builds throw with a clear message; release builds normalize by keeping the last item per
 *   key. [keySelector] must match [com.rend1x.composeanimatedlist.AnimatedColumn]’s `key`.
 */
internal object AnimatedListDiffer {

    fun <T> diff(
        current: List<AnimatedListItem<T>>,
        newItems: List<T>,
        keySelector: (T) -> Any,
    ): List<AnimatedListItem<T>> {
        val sanitizedNewItems = AnimatedListKeys.sanitizeAnimatedListInput(newItems, keySelector)
        val newKeyed = sanitizedNewItems.associateByTo(linkedMapOf(), keySelector)
        val newKeys = newKeyed.keys
        val currentKeys = current.mapTo(linkedSetOf()) { it.key }

        // Start from current order so exiting items stay where they used to be.
        val render: MutableList<AnimatedListItem<T>> = current.map { currentItem ->
            val updatedValue = newKeyed[currentItem.key]
            if (updatedValue == null) {
                currentItem.copy(presence = PresenceState.Exiting)
            } else {
                AnimatedListItem<T>(
                    key = currentItem.key,
                    value = updatedValue,
                    presence = PresenceState.Present,
                )
            }
        }.toMutableList()

        // Insert entering items at positions based on new list order.
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

        // Ensure already known entries that re-appeared are never kept Exiting.
        return render.map { item ->
            if (item.key in newKeys && item.presence == PresenceState.Exiting) {
                item.copy(presence = PresenceState.Present)
            } else {
                item
            }
        }
    }
}
