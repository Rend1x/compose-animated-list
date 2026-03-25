package com.rend1x.composeanimatedlist.core

/**
 * Keys returned by the list’s `key` function must be **unique within each `items` snapshot**.
 * Duplicate keys produce undefined diff/animation behavior when not sanitized.
 *
 * With [AnimatedListKeyPolicy.Strict], duplicates throw [IllegalStateException] with indices.
 * With [AnimatedListKeyPolicy.LastWins], the list is normalized by keeping the **last** occurrence
 * of each key.
 */
object AnimatedListKeys {

    fun <T> sanitizeAnimatedListInput(
        items: List<T>,
        keySelector: (T) -> Any,
        policy: AnimatedListKeyPolicy,
    ): List<T> = when (policy) {
        AnimatedListKeyPolicy.Strict -> {
            validateNoDuplicateKeys(items, keySelector)
            items
        }
        AnimatedListKeyPolicy.LastWins -> deduplicateByKeyLastWins(items, keySelector)
    }

    private fun <T> validateNoDuplicateKeys(
        items: List<T>,
        keySelector: (T) -> Any,
    ) {
        val firstIndexByKey = linkedMapOf<Any, Int>()
        items.forEachIndexed { index, item ->
            val key = keySelector(item)
            val firstIndex = firstIndexByKey[key]
            if (firstIndex != null) {
                throw IllegalStateException(
                    "AnimatedColumn: duplicate key \"$key\" at indices $firstIndex and $index " +
                        "(0-based). The `key` lambda must return a unique value for each element " +
                        "in `items` for a given snapshot. " +
                        "(This validation runs in debug library builds only; release builds keep " +
                        "the last item for each duplicated key.)",
                )
            }
            firstIndexByKey[key] = index
        }
    }

    /**
     * Keeps the last occurrence of each key; result order is **increasing index of that last
     * occurrence** in [items] (equivalent to `asReversed().distinctBy(key).asReversed()`).
     */
    private fun <T> deduplicateByKeyLastWins(
        items: List<T>,
        keySelector: (T) -> Any,
    ): List<T> = items.asReversed().distinctBy { keySelector(it) }.asReversed()
}
