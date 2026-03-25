package com.rend1x.composeanimatedlist.core

import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedListDuplicateKeysStrictTest {

    private data class Item(val id: String, val payload: String)

    private val key: (Item) -> String = { it.id }

    private val keyPolicy = AnimatedListKeyPolicy.Strict

    @Test
    fun duplicateKeysInDiff_throwWithIndices() {
        try {
            AnimatedListDiffer.diff(
                current = emptyList(),
                newItems = listOf(Item("x", "1"), Item("x", "2")),
                keySelector = key,
                keyPolicy = keyPolicy,
            )
            throw AssertionError("expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("duplicate key"))
            assertTrue(e.message!!.contains("indices 0 and 1"))
        }
    }

    @Test
    fun duplicateKeysInInitialEngine_throw() {
        try {
            AnimatedListRenderEngine(
                initialItems = listOf(Item("a", "1"), Item("a", "2")),
                keySelector = key,
                keyPolicy = keyPolicy,
            )
            throw AssertionError("expected exception")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("duplicate key"))
        }
    }
}
