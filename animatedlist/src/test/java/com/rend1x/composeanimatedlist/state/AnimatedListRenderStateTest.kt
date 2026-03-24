package com.rend1x.composeanimatedlist.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedListRenderStateTest {

    @Test
    fun updateSequence_appliesDiffFromPreviousRenderSnapshot() {
        val state = AnimatedListRenderState(
            initialItems = listOf("a", "b", "c"),
            keySelector = { it },
        )
        state.update(listOf("a", "b"), { it })
        assertEquals(listOf("a", "b", "c"), state.renderItems.map { it.value })
        assertEquals(PresenceState.Exiting, state.renderItems[2].presence)

        state.update(listOf("a", "b", "c"), { it })
        val c = state.renderItems.first { it.value == "c" }
        assertEquals(PresenceState.Present, c.presence)
    }

    @Test
    fun clearExitingNow_dropsExitingRows() {
        val state = AnimatedListRenderState(
            initialItems = listOf("a", "b"),
            keySelector = { it },
        )
        state.update(listOf("a"), { it })
        assertEquals(2, state.renderItems.size)
        state.clearExitingNow()
        assertEquals(listOf("a"), state.renderItems.map { it.value })
    }

    @Test
    fun onExitAnimationFinished_removesExitingKey() {
        val state = AnimatedListRenderState(
            initialItems = listOf("x"),
            keySelector = { it },
        )
        state.update(emptyList(), { it })
        assertEquals(1, state.renderItems.size)
        state.onExitAnimationFinished("x")
        assertTrue(state.renderItems.isEmpty())
    }
}
