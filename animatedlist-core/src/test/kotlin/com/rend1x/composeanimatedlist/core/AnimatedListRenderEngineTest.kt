package com.rend1x.composeanimatedlist.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedListRenderEngineTest {

    @Test
    fun updateSequence_appliesDiffFromPreviousRenderSnapshot() {
        val engine = AnimatedListRenderEngine(
            initialItems = listOf("a", "b", "c"),
            keySelector = { it },
            keyPolicy = AnimatedListKeyPolicy.Strict,
        )
        engine.update(listOf("a", "b"), { it })
        assertEquals(listOf("a", "b", "c"), engine.items.map { it.value })
        assertEquals(PresenceState.Exiting, engine.items[2].presence)

        engine.update(listOf("a", "b", "c"), { it })
        val c = engine.items.first { it.value == "c" }
        assertEquals(PresenceState.Present, c.presence)
    }

    @Test
    fun clearExitingNow_dropsExitingRows() {
        val engine = AnimatedListRenderEngine(
            initialItems = listOf("a", "b"),
            keySelector = { it },
            keyPolicy = AnimatedListKeyPolicy.Strict,
        )
        engine.update(listOf("a"), { it })
        assertEquals(2, engine.items.size)
        engine.clearExitingNow()
        assertEquals(listOf("a"), engine.items.map { it.value })
    }

    @Test
    fun onExitAnimationFinished_removesExitingKey() {
        val engine = AnimatedListRenderEngine(
            initialItems = listOf("x"),
            keySelector = { it },
            keyPolicy = AnimatedListKeyPolicy.Strict,
        )
        engine.update(emptyList(), { it })
        assertEquals(1, engine.items.size)
        engine.onExitAnimationFinished("x")
        assertTrue(engine.items.isEmpty())
    }
}
