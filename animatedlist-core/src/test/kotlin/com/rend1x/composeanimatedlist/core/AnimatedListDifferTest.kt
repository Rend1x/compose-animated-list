package com.rend1x.composeanimatedlist.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AnimatedListDifferTest {

    private data class Item(val id: String, val payload: String)

    private val key: (Item) -> String = { it.id }

    private val keyPolicy = AnimatedListKeyPolicy.Strict

    @Test
    fun removedKey_staysExiting_andKeepsOrderAmongNeighbors() {
        val a = Item("a", "1")
        val b = Item("b", "1")
        val c = Item("c", "1")
        val current = listOf(
            AnimatedListItem(key(a), a, PresenceState.Present),
            AnimatedListItem(key(b), b, PresenceState.Present),
            AnimatedListItem(key(c), c, PresenceState.Present),
        )
        val out = AnimatedListDiffer.diff(
            current = current,
            newItems = listOf(a, c),
            keySelector = key,
            keyPolicy = keyPolicy,
        )
        assertEquals(listOf("a", "b", "c"), out.map { it.key })
        assertEquals(PresenceState.Present, out[0].presence)
        assertEquals(PresenceState.Exiting, out[1].presence)
        assertEquals(PresenceState.Present, out[2].presence)
    }

    @Test
    fun reinsertionWhileExiting_becomesPresentWithLatestValue_notEntering() {
        val a = Item("a", "a")
        val bOld = Item("b", "old")
        val bNew = Item("b", "new")
        val current = listOf(
            AnimatedListItem("a", a, PresenceState.Present),
            AnimatedListItem("b", bOld, PresenceState.Exiting),
        )
        val out = AnimatedListDiffer.diff(
            current = current,
            newItems = listOf(a, bNew),
            keySelector = key,
            keyPolicy = keyPolicy,
        )
        val bItem = out.first { it.key == "b" }
        assertEquals(PresenceState.Present, bItem.presence)
        assertEquals("new", bItem.value.payload)
    }

    @Test
    fun brandNewKey_isEntering() {
        val current = listOf(
            AnimatedListItem("a", Item("a", "1"), PresenceState.Present),
        )
        val b = Item("b", "1")
        val out = AnimatedListDiffer.diff(
            current = current,
            newItems = listOf(Item("a", "1"), b),
            keySelector = key,
            keyPolicy = keyPolicy,
        )
        assertEquals(
            PresenceState.Entering,
            out.first { it.key == "b" }.presence,
        )
    }

    @Test
    fun chainedDiffs_matchSequentialApply() {
        val s1 = AnimatedListDiffer.diff(
            current = listOf(
                AnimatedListItem("a", "a", PresenceState.Present),
                AnimatedListItem("b", "b", PresenceState.Present),
            ),
            newItems = listOf("a", "b"),
            keySelector = { it },
            keyPolicy = keyPolicy,
        )
        val s2 = AnimatedListDiffer.diff(
            current = s1,
            newItems = listOf("a"),
            keySelector = { it },
            keyPolicy = keyPolicy,
        )
        assertEquals(listOf("a", "b"), s2.map { it.key })
        assertEquals(PresenceState.Present, s2[0].presence)
        assertEquals(PresenceState.Exiting, s2[1].presence)
    }
}
