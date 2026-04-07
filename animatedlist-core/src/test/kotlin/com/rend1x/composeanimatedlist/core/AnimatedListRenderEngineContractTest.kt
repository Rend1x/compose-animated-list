package com.rend1x.composeanimatedlist.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for [AnimatedListRenderEngine]: observable snapshots only—no reliance on
 * diff internals or collection implementation details.
 */
class AnimatedListRenderEngineContractTest {

    private data class Row(val id: String, val payload: String)

    private val key: (Row) -> String = { it.id }

    private fun strictEngine(initialItems: List<Row> = emptyList()) =
        AnimatedListRenderEngine(
            initialItems = initialItems,
            keySelector = key,
            keyPolicy = AnimatedListKeyPolicy.Strict,
        )

    private fun lastWinsEngine(initialItems: List<Row> = emptyList()) =
        AnimatedListRenderEngine(
            initialItems = initialItems,
            keySelector = key,
            keyPolicy = AnimatedListKeyPolicy.LastWins,
        )

    @Test
    fun identicalConsecutiveUpdates_areIdempotent() {
        val engine = strictEngine(listOf(Row("a", "0")))
        engine.update(listOf(Row("a", "1"), Row("b", "1")), key)
        fun snapshot() = engine.items.map { Triple(it.key, it.value, it.presence) }
        val afterFirst = snapshot()

        engine.update(listOf(Row("a", "1"), Row("b", "1")), key)

        assertEquals(afterFirst, snapshot())
    }

    @Test
    fun newlyAddedKey_isEntering() {
        val engine = strictEngine(listOf(Row("a", "1")))
        engine.update(listOf(Row("a", "1"), Row("b", "2")), key)
        val b = engine.items.first { it.key == "b" }
        assertEquals(PresenceState.Entering, b.presence)
        assertEquals("2", b.value.payload)
    }

    @Test
    fun removedKey_isExiting_andRetainedUntilHandled() {
        val engine = strictEngine(listOf(Row("a", "1"), Row("b", "2")))
        engine.update(listOf(Row("a", "1")), key)
        assertEquals(2, engine.items.size)
        val b = engine.items.first { it.key == "b" }
        assertEquals(PresenceState.Exiting, b.presence)
        assertEquals("2", b.value.payload)
    }

    @Test
    fun `reinsert while exiting continues toward visible without restart`() {
        val engine = strictEngine(listOf(Row("a", "1")))
        engine.update(emptyList(), key)
        assertEquals(PresenceState.Exiting, engine.items.single { it.key == "a" }.presence)

        engine.update(listOf(Row("a", "fresh")), key)
        val a = engine.items.single { it.key == "a" }
        assertEquals(PresenceState.Present, a.presence)
        assertEquals("fresh", a.value.payload)
    }

    @Test
    fun `remove during entering transitions smoothly to exiting`() {
        val engine = strictEngine(listOf(Row("a", "1")))
        engine.update(listOf(Row("a", "1"), Row("b", "2")), key)
        assertEquals(PresenceState.Entering, engine.items.first { it.key == "b" }.presence)

        engine.update(listOf(Row("a", "1")), key)
        assertEquals(PresenceState.Exiting, engine.items.first { it.key == "b" }.presence)
    }

    @Test
    fun `rapid add-remove-add stabilizes to latest state`() {
        val engine = strictEngine(listOf(Row("a", "1")))
        engine.update(emptyList(), key)
        engine.update(listOf(Row("a", "2")), key)
        engine.update(emptyList(), key)
        engine.update(listOf(Row("a", "3")), key)
        val a = engine.items.single { it.key == "a" }
        assertEquals(PresenceState.Present, a.presence)
        assertEquals("3", a.value.payload)
    }

    @Test
    fun `multiple rapid updates produce deterministic final snapshot`() {
        fun snapshot(engine: AnimatedListRenderEngine<Row>): List<Triple<Any, String, PresenceState>> =
            engine.items.map { Triple(it.key, it.value.payload, it.presence) }

        fun stepped(): AnimatedListRenderEngine<Row> {
            val engine = strictEngine(listOf(Row("a", "0"), Row("b", "0")))
            engine.update(
                listOf(Row("a", "0"), Row("b", "0"), Row("c", "c")),
                key,
            )
            engine.update(listOf(Row("a", "0"), Row("c", "c")), key)
            engine.onExitAnimationFinished("b")
            engine.update(
                listOf(Row("a", "0"), Row("b", "new"), Row("c", "c")),
                key,
            )
            return engine
        }

        val first = snapshot(stepped())
        val second = snapshot(stepped())
        assertEquals(first, second)
    }

    @Test
    fun `repeated updates converge to stable final values`() {
        val engine = strictEngine(listOf(Row("a", "0"), Row("b", "0")))
        repeat(100) {
            engine.update(
                listOf(Row("a", "$it"), Row("b", "${it + 1}")),
                key,
            )
        }
        val a = engine.items.first { it.key == "a" }
        val b = engine.items.first { it.key == "b" }
        assertEquals(PresenceState.Present, a.presence)
        assertEquals(PresenceState.Present, b.presence)
        assertEquals("99", a.value.payload)
        assertEquals("100", b.value.payload)
    }

    @Test
    fun mixedRemoveAndInsert_producesDeterministicKeyOrder() {
        val engine = strictEngine(
            listOf(
                Row("a", "a"),
                Row("b", "b"),
                Row("c", "c"),
            ),
        )
        engine.update(
            listOf(Row("a", "a"), Row("d", "d"), Row("c", "c")),
            key,
        )
        assertEquals(
            listOf("a", "d", "b", "c"),
            engine.items.map { it.key as String },
        )
        assertEquals(PresenceState.Present, engine.items.first { it.key == "a" }.presence)
        assertEquals(PresenceState.Entering, engine.items.first { it.key == "d" }.presence)
        assertEquals(PresenceState.Exiting, engine.items.first { it.key == "b" }.presence)
        assertEquals(PresenceState.Present, engine.items.first { it.key == "c" }.presence)
    }

    @Test
    fun clearExitingNow_removesOnlyExitingRows() {
        val engine = strictEngine(listOf(Row("a", "1"), Row("b", "2")))
        engine.update(listOf(Row("a", "1")), key)
        engine.update(listOf(Row("a", "1"), Row("c", "3")), key)

        engine.clearExitingNow()

        assertEquals(setOf("a", "c"), engine.items.map { it.key }.toSet())
        assertTrue(engine.items.none { it.presence == PresenceState.Exiting })
        assertEquals(PresenceState.Entering, engine.items.first { it.key == "c" }.presence)
    }

    @Test(expected = IllegalStateException::class)
    fun strictPolicy_duplicateKeysInUpdate_throw() {
        val engine = strictEngine()
        engine.update(
            listOf(Row("x", "1"), Row("x", "2")),
            key,
        )
    }

    @Test(expected = IllegalStateException::class)
    fun strictPolicy_duplicateKeysInInitialInput_throw() {
        strictEngine(listOf(Row("x", "1"), Row("x", "2")))
    }

    @Test
    fun lastWinsPolicy_keepsLastValuePerKey_andStableSurvivorOrder() {
        val engine = lastWinsEngine()
        engine.update(
            listOf(
                Row("a", "first"),
                Row("b", "b"),
                Row("a", "last"),
            ),
            key,
        )
        assertEquals(listOf("b", "a"), engine.items.map { it.key })
        assertEquals("last", engine.items.first { it.key == "a" }.value.payload)
        assertEquals("b", engine.items.first { it.key == "b" }.value.payload)
        assertTrue(engine.items.all { it.presence == PresenceState.Entering })
    }

    @Test
    fun lastWinsPolicy_duplicateKeysInSingleUpdate_yieldOneRowWithLastPayload() {
        val engine = lastWinsEngine(listOf(Row("k", "0")))
        engine.update(
            listOf(Row("k", "1"), Row("k", "2")),
            key,
        )
        assertEquals(1, engine.items.size)
        assertEquals("2", engine.items.single().value.payload)
        assertEquals(PresenceState.Present, engine.items.single().presence)
    }
}
