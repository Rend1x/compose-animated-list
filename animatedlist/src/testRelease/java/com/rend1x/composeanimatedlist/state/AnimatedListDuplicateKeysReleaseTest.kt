package com.rend1x.composeanimatedlist.state

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Release builds use `BuildConfig.DEBUG = false` and normalize duplicate keys instead of throwing.
 */
class AnimatedListDuplicateKeysReleaseTest {

    private data class Item(val id: String, val payload: String)

    private val key: (Item) -> String = { it.id }

    @Test
    fun duplicateKeysInDiff_keepsLastItemPerKey() {
        val out = AnimatedListDiffer.diff(
            current = emptyList(),
            newItems = listOf(Item("x", "1"), Item("x", "2")),
            keySelector = key,
        )
        assertEquals(1, out.size)
        assertEquals("2", out.single().value.payload)
        assertEquals(PresenceState.Entering, out.single().presence)
    }

    @Test
    fun duplicateKeysInInitialRenderState_keepsLastItem() {
        val state = AnimatedListRenderState(
            initialItems = listOf(Item("a", "1"), Item("a", "2")),
            keySelector = key,
        )
        assertEquals(1, state.renderItems.size)
        assertEquals("2", state.renderItems.single().value.payload)
    }
}
