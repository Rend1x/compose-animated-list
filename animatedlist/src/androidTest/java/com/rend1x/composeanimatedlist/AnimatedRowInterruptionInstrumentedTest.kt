package com.rend1x.composeanimatedlist

import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rend1x.composeanimatedlist.animation.AnimatedItemDefaults
import com.rend1x.composeanimatedlist.animation.AnimatedItemTransitionSpec
import com.rend1x.composeanimatedlist.animation.EnterSpec
import com.rend1x.composeanimatedlist.animation.ExitSpec
import com.rend1x.composeanimatedlist.animation.PlacementBehavior
import com.rend1x.composeanimatedlist.state.AnimatedListState
import com.rend1x.composeanimatedlist.state.rememberAnimatedListState
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnimatedRowInterruptionInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private data class RowItem(val id: String)

    @Test
    fun given_none_transition_when_items_change_then_shell_animation_state_is_skipped() {
        var items by mutableStateOf(emptyList<RowItem>())
        lateinit var listState: AnimatedListState

        composeRule.setContent {
            val state = rememberAnimatedListState()
            listState = state
            AnimatedRow(
                items = items,
                key = { it.id },
                state = state,
                transitionSpec = AnimatedItemDefaults.none(),
            ) { item ->
                BasicText(
                    text = phase.name,
                    modifier = Modifier.testTag("item-${item.id}"),
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.runOnIdle {
            items = listOf(RowItem("a"))
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("item-a").assertIsDisplayed().assertTextEquals("Visible")
        composeRule.runOnIdle {
            assertFalse(listState.isAnimating)
            items = emptyList()
        }
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag("item-a").assertCountEquals(0)
        composeRule.runOnIdle {
            assertFalse(listState.isAnimating)
        }
    }

    @Test
    fun given_indexed_overload_when_item_exits_then_render_index_includes_retained_item() {
        var items by mutableStateOf(listOf(RowItem("a"), RowItem("b"), RowItem("c")))
        val spec =
            AnimatedItemTransitionSpec(
                enter = EnterSpec.Fade(300),
                exit = ExitSpec.Fade(5_000),
                placement = PlacementBehavior.None,
            )

        composeRule.setContent {
            AnimatedRow(
                items = items,
                key = { it.id },
                transitionSpec = spec,
            ) { item, index ->
                BasicText(
                    text = "$index:${item.id}:${phase.name}",
                    modifier = Modifier.testTag("item-${item.id}"),
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        composeRule.runOnIdle {
            items = listOf(RowItem("a"), RowItem("c"))
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithTag("item-b").assertIsDisplayed().assertTextEquals("1:b:Exiting")
        composeRule.onNodeWithTag("item-c").assertIsDisplayed().assertTextEquals("2:c:Visible")
    }

    @Test
    fun given_item_exiting_when_reinserted_then_it_becomes_visible_without_restart() {
        var items by mutableStateOf(listOf(RowItem("a")))
        val spec =
            AnimatedItemTransitionSpec(
                enter = EnterSpec.Fade(5_000),
                exit = ExitSpec.Fade(5_000),
                placement = PlacementBehavior.None,
            )

        composeRule.setContent {
            AnimatedRow(
                items = items,
                key = { it.id },
                transitionSpec = spec,
            ) { item ->
                val label =
                    when (phase) {
                        ItemPhase.Entering -> "ENTERING"
                        ItemPhase.Visible -> "VISIBLE"
                        ItemPhase.Exiting -> "EXITING"
                    }
                BasicText(label, modifier = Modifier.testTag("phase-${item.id}"))
            }
        }

        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        composeRule.runOnIdle {
            items = emptyList()
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(500)

        composeRule.runOnIdle {
            items = listOf(RowItem("a"))
        }
        composeRule.advanceFramesAndWait()

        composeRule.onNodeWithTag("phase-a").assertIsDisplayed().assertTextEquals("VISIBLE")
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.advanceFramesAndWait(frameCount: Int = 3) {
        repeat(frameCount) {
            mainClock.advanceTimeByFrame()
            waitForIdle()
        }
    }
}
