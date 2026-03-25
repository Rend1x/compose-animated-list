package com.rend1x.composeanimatedlist.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AnimatedListDuplicateKeysLastWinsTest {

    private data class Item(val id: String, val payload: String)

    private val key: (Item) -> String = { it.id }

    private val keyPolicy = AnimatedListKeyPolicy.LastWins

    @Test
    fun duplicateKeysInDiff_keepsLastItemPerKey() {
        val out = AnimatedListDiffer.diff(
            current = emptyList(),
            newItems = listOf(Item("x", "1"), Item("x", "2")),
            keySelector = key,
            keyPolicy = keyPolicy,
        )
        assertEquals(1, out.size)
        assertEquals("2", out.single().value.payload)
        assertEquals(PresenceState.Entering, out.single().presence)
    }

    @Test
    fun duplicateKeysInInitialEngine_keepsLastItem() {
        val engine = AnimatedListRenderEngine(
            initialItems = listOf(Item("a", "1"), Item("a", "2")),
            keySelector = key,
            keyPolicy = keyPolicy,
        )
        assertEquals(1, engine.items.size)
        assertEquals("2", engine.items.single().value.payload)
    }
}
