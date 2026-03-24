package com.rend1x.composeanimatedlist

/**
 * Produces next render list:
 * - keeps removed items as Exiting
 * - marks newly added items as Entering
 * - keeps currently visible items as Present
 */
internal object AnimatedListDiffer {

    fun <T> diff(
        current: List<AnimatedListItem<T>>,
        newItems: List<T>,
        keySelector: (T) -> Any,
    ): List<AnimatedListItem<T>> {
        val newKeyed = newItems.associateByDistinctKey(keySelector)
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
        newItems.forEachIndexed { index, item ->
            val key = keySelector(item)
            if (key in currentKeys) return@forEachIndexed

            val previousKnownKey = newItems
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

            val nextKnownKey = newItems
                .subList(index + 1, newItems.size)
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

private fun <T> List<T>.associateByDistinctKey(
    keySelector: (T) -> Any,
): LinkedHashMap<Any, T> {
    val map = linkedMapOf<Any, T>()
    for (item in this) {
        val key = keySelector(item)
        check(map.put(key, item) == null) {
            "Animated list keys must be unique. Duplicated key=$key"
        }
    }
    return LinkedHashMap(map)
}
