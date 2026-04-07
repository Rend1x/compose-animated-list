package com.rend1x.composeanimatedlist

import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rend1x.composeanimatedlist.animation.AnimatedItemTransitionSpec
import com.rend1x.composeanimatedlist.animation.EnterSpec
import com.rend1x.composeanimatedlist.animation.ExitSpec
import com.rend1x.composeanimatedlist.animation.PlacementBehavior
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnimatedColumnInterruptionInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private data class Row(val id: String)

    @Test
    fun given_item_entering_when_removed_then_it_becomes_exiting() {
        var items by mutableStateOf(listOf(Row("a")))
        val longFade = 5_000
        val spec = AnimatedItemTransitionSpec(
            enter = EnterSpec.Fade(longFade),
            exit = ExitSpec.Fade(longFade),
            placement = PlacementBehavior.None,
        )

        composeRule.setContent {
            AnimatedColumn(
                items = items,
                key = { it.id },
                transitionSpec = spec,
            ) { item ->
                val label = when (phase) {
                    ItemPhase.Entering -> "ENTERING"
                    ItemPhase.Visible -> "VISIBLE"
                    ItemPhase.Exiting -> "EXITING"
                }
                BasicText(label, modifier = Modifier.testTag("phase-${item.id}"))
            }
        }

        composeRule.waitForIdle()
        items = listOf(Row("a"), Row("b"))
        composeRule.waitForIdle()

        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(500)

        items = listOf(Row("a"))
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithTag("phase-b").assertIsDisplayed().assertTextEquals("EXITING")
    }

    @Test
    fun given_item_exiting_when_reinserted_then_it_becomes_visible_without_restart() {
        var items by mutableStateOf(listOf(Row("a")))
        val longFade = 5_000
        val spec = AnimatedItemTransitionSpec(
            enter = EnterSpec.Fade(longFade),
            exit = ExitSpec.Fade(longFade),
            placement = PlacementBehavior.None,
        )

        composeRule.setContent {
            AnimatedColumn(
                items = items,
                key = { it.id },
                transitionSpec = spec,
            ) { item ->
                val label = when (phase) {
                    ItemPhase.Entering -> "ENTERING"
                    ItemPhase.Visible -> "VISIBLE"
                    ItemPhase.Exiting -> "EXITING"
                }
                BasicText(label, modifier = Modifier.testTag("phase-${item.id}"))
            }
        }

        composeRule.waitForIdle()
        items = emptyList()
        composeRule.waitForIdle()

        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(500)

        items = listOf(Row("a"))
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithTag("phase-a").assertIsDisplayed().assertTextEquals("VISIBLE")
    }

    @Test
    fun given_rapid_add_remove_add_when_sequence_applied_then_final_state_matches_latest() {
        var items by mutableStateOf(listOf(Row("a")))
        val spec = AnimatedItemTransitionSpec(
            enter = EnterSpec.Fade(300),
            exit = ExitSpec.Fade(300),
            placement = PlacementBehavior.None,
        )

        composeRule.setContent {
            AnimatedColumn(
                items = items,
                key = { it.id },
                transitionSpec = spec,
            ) { item ->
                BasicText(
                    text = item.id,
                    modifier = Modifier.testTag("key-${item.id}"),
                )
            }
        }

        composeRule.waitForIdle()
        items = emptyList()
        composeRule.waitForIdle()
        items = listOf(Row("a"))
        composeRule.waitForIdle()
        items = emptyList()
        composeRule.waitForIdle()
        items = listOf(Row("a"))
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("key-a").assertIsDisplayed()
    }

    @Test
    fun given_animation_in_progress_when_transition_spec_changes_then_animation_continues_from_current_values() {
        var items by mutableStateOf(listOf(Row("a"), Row("b")))
        var useShortEnter by mutableStateOf(false)
        val longSpec = AnimatedItemTransitionSpec(
            enter = EnterSpec.Fade(8_000),
            exit = ExitSpec.Fade(300),
            placement = PlacementBehavior.None,
        )
        val shortSpec = AnimatedItemTransitionSpec(
            enter = EnterSpec.Fade(120),
            exit = ExitSpec.Fade(300),
            placement = PlacementBehavior.None,
        )

        composeRule.setContent {
            AnimatedColumn(
                items = items,
                key = { it.id },
                transitionSpec = if (useShortEnter) shortSpec else longSpec,
            ) { item ->
                BasicText(
                    text = "${item.id}:${phase.name}",
                    modifier = Modifier.testTag("row-${item.id}"),
                )
            }
        }

        composeRule.waitForIdle()
        items = listOf(Row("a"), Row("b"), Row("c"))
        composeRule.waitForIdle()

        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(400)

        useShortEnter = true
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        // Differ keeps new keys Entering until value changes; shell may already look settled.
        composeRule.onNodeWithTag("row-c").assertIsDisplayed().assertTextEquals("c:Entering")
    }

    @Test
    fun given_rapid_key_reuse_when_updates_applied_then_animation_state_is_not_reset() {
        var items by mutableStateOf(listOf(Row("a"), Row("b")))
        val spec = AnimatedItemTransitionSpec(
            enter = EnterSpec.Fade(4_000),
            exit = ExitSpec.Fade(4_000),
            placement = PlacementBehavior.None,
        )

        composeRule.setContent {
            AnimatedColumn(
                items = items,
                key = { it.id },
                transitionSpec = spec,
            ) { item ->
                val label = when (phase) {
                    ItemPhase.Entering -> "ENTERING"
                    ItemPhase.Visible -> "VISIBLE"
                    ItemPhase.Exiting -> "EXITING"
                }
                BasicText(label, modifier = Modifier.testTag("phase-${item.id}"))
            }
        }

        composeRule.waitForIdle()
        repeat(25) {
            items = listOf(Row("b"))
            composeRule.mainClock.autoAdvance = false
            composeRule.mainClock.advanceTimeBy(120)
            items = listOf(Row("a"), Row("b"))
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.mainClock.autoAdvance = true
            composeRule.mainClock.advanceTimeByFrame()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("phase-a").assertTextEquals("VISIBLE")
    }

    @Test
    fun given_item_partially_exiting_when_reinserted_then_it_resumes_from_current_state() {
        var items by mutableStateOf(listOf(Row("a")))
        val spec = AnimatedItemTransitionSpec(
            enter = EnterSpec.Fade(600),
            exit = ExitSpec.Fade(600),
            placement = PlacementBehavior.None,
        )

        composeRule.setContent {
            AnimatedColumn(
                items = items,
                key = { it.id },
                transitionSpec = spec,
            ) { item ->
                val label = when (phase) {
                    ItemPhase.Entering -> "ENTERING"
                    ItemPhase.Visible -> "VISIBLE"
                    ItemPhase.Exiting -> "EXITING"
                }
                BasicText(label, modifier = Modifier.testTag("phase-${item.id}"))
            }
        }

        composeRule.waitForIdle()
        items = emptyList()
        composeRule.waitForIdle()

        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(250)

        items = listOf(Row("a"))
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithTag("phase-a").assertIsDisplayed().assertTextEquals("VISIBLE")
    }
}
