package com.rend1x.composeanimatedlist.state

import com.rend1x.composeanimatedlist.BuildConfig

/**
 * Keys returned by [com.rend1x.composeanimatedlist.AnimatedColumn]'s `key` parameter must be
 * **unique within each `items` snapshot**. Duplicate keys produce undefined diff/animation behavior
 * (e.g. multiple rows sharing a Compose key).
 *
 * **Debug builds** of this library call [sanitizeAnimatedListInput], which throws
 * [IllegalStateException] with indices when duplicates are detected.
 *
 * **Release builds** do not throw: the list is normalized by keeping the **last** occurrence of each
 * key (stable relative order among the kept items). Prefer fixing callers so debug and release match.
 */
internal object AnimatedListKeys {

    fun <T> sanitizeAnimatedListInput(
        items: List<T>,
        keySelector: (T) -> Any,
    ): List<T> {
        if (BuildConfig.DEBUG) {
            validateNoDuplicateKeys(items, keySelector)
            return items
        }
        return deduplicateByKeyLastWins(items, keySelector)
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
     * Keeps the last occurrence of each key; order matches first-seen positions of those occurrences
     * in the original list (equivalent to reversed + distinctBy key + reversed).
     */
    private fun <T> deduplicateByKeyLastWins(
        items: List<T>,
        keySelector: (T) -> Any,
    ): List<T> = items.asReversed().distinctBy { keySelector(it) }.asReversed()
}
