package com.rend1x.composeanimatedlist.state

import com.rend1x.composeanimatedlist.ItemPhase
import org.junit.Assert.assertEquals
import org.junit.Test

class AnimatedListStateTest {
    @Test
    fun syncRenderItems_reportsPhaseChangesAndFinishCallbacks() {
        val phaseEvents = mutableListOf<Pair<Any, ItemPhase>>()
        val enterFinished = mutableListOf<Any>()
        val exitFinished = mutableListOf<Any>()
        val state = AnimatedListState()
        state.updateHooks(
            onItemPhaseChanged = { key, phase -> phaseEvents += key to phase },
            onEnterFinished = { key -> enterFinished += key },
            onExitFinished = { key -> exitFinished += key },
        )
        val renderState = AnimatedListRenderState(
            initialItems = listOf("a", "b"),
            keySelector = { it },
        )

        state.syncRenderItems(renderState.renderItems)
        renderState.update(listOf("a", "c")) { it }
        state.syncRenderItems(renderState.renderItems)
        renderState.onEnterAnimationFinished("c")
        state.syncRenderItems(renderState.renderItems)
        state.notifyEnterFinished("c")
        renderState.onExitAnimationFinished("b")
        state.syncRenderItems(renderState.renderItems)
        state.notifyExitFinished("b")

        assertEquals(
            listOf(
                "c" to ItemPhase.Entering,
                "b" to ItemPhase.Exiting,
                "c" to ItemPhase.Visible,
            ),
            phaseEvents,
        )
        assertEquals(listOf("c"), enterFinished)
        assertEquals(listOf("b"), exitFinished)
        assertEquals(setOf("a", "c"), state.visibleKeys)
        assertEquals(emptySet<Any>(), state.exitingKeys)
    }

    @Test
    fun clearExiting_clearsOnlySelectedKey() {
        val state = AnimatedListState()
        val renderState = AnimatedListRenderState(
            initialItems = listOf("a", "b", "c"),
            keySelector = { it },
        )

        renderState.update(listOf("a")) { it }
        state.syncRenderItems(renderState.renderItems)
        state.setClearExitingCallbacks(
            callback = {
                renderState.clearExitingNow()
                state.syncRenderItems(renderState.renderItems)
            },
            keyCallback = { key ->
                renderState.clearExiting(key)
                state.syncRenderItems(renderState.renderItems)
            },
        )

        state.clearExiting("b")

        assertEquals(setOf("a"), state.visibleKeys)
        assertEquals(setOf("c"), state.exitingKeys)
        assertEquals(listOf("a", "c"), renderState.renderItems.map { it.key })

        state.clearExitingNow()

        assertEquals(setOf("a"), state.visibleKeys)
        assertEquals(emptySet<Any>(), state.exitingKeys)
        assertEquals(listOf("a"), renderState.renderItems.map { it.key })
    }
}
