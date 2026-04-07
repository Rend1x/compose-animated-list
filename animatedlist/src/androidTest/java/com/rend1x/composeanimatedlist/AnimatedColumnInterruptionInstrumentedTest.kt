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
    fun `remove during entering transitions smoothly to exiting`() {
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
    fun `reinsert while exiting continues toward visible without restart`() {
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
    fun `rapid add-remove-add stabilizes to latest state`() {
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
    fun `changing transitionSpec mid-animation starts from current values`() {
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

        // Engine keeps a row Entering until the element value changes (see AnimatedListDiffer); shell
        // can still be visually at rest after enter completes.
        composeRule.onNodeWithTag("row-c").assertIsDisplayed().assertTextEquals("c:Entering")
    }

    @Test
    fun `rapid key reuse does not reset animation values`() {
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
    fun `reinserted item resumes from current visual state`() {
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
