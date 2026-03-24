package com.rend1x.composeanimatedlist.state

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Duplicate-key validation runs only for **debug** builds of this module; these tests live under
 * `src/testDebug` so they are not executed against the release `BuildConfig.DEBUG = false` variant.
 */
class AnimatedListDuplicateKeysDebugTest {

    private data class Item(val id: String, val payload: String)

    private val key: (Item) -> String = { it.id }

    @Test
    fun duplicateKeysInDiff_throwWithIndices() {
        try {
            AnimatedListDiffer.diff(
                current = emptyList(),
                newItems = listOf(Item("x", "1"), Item("x", "2")),
                keySelector = key,
            )
            throw AssertionError("expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("duplicate key"))
            assertTrue(e.message!!.contains("indices 0 and 1"))
        }
    }

    @Test
    fun duplicateKeysInInitialRenderState_throw() {
        try {
            AnimatedListRenderState(
                initialItems = listOf(Item("a", "1"), Item("a", "2")),
                keySelector = key,
            )
            throw AssertionError("expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("duplicate key"))
        }
    }
}
