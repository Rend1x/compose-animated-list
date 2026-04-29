package com.rend1x.composeanimatedlist.sample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rend1x.composeanimatedlist.AnimatedColumn
import com.rend1x.composeanimatedlist.ItemPhase
import com.rend1x.composeanimatedlist.animation.AnimatedItemTransitionSpec
import com.rend1x.composeanimatedlist.animation.EnterSpec
import com.rend1x.composeanimatedlist.animation.ExitSpec
import com.rend1x.composeanimatedlist.animation.PlacementBehavior
import com.rend1x.composeanimatedlist.animation.VerticalDirection
import com.rend1x.composeanimatedlist.state.rememberAnimatedListState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun SemanticsExamplesPage(
    modifier: Modifier = Modifier,
) {
    val stableSeed = remember { listOf(1, 2, 3) }
    val hotKey = 42
    val items = remember {
        mutableStateListOf<DemoItem>().apply {
            addAll(stableSeed.map(::DemoItem))
        }
    }
    val listState = rememberAnimatedListState()
    val scope = rememberCoroutineScope()

    val transitionSpec = remember {
        AnimatedItemTransitionSpec(
            enter = EnterSpec.FadeAndSlide(offset = 28.dp, durationMillis = 1200),
            exit = ExitSpec.FadeAndSlide(
                offset = 28.dp,
                direction = VerticalDirection.Up,
                durationMillis = 1600,
            ),
            placement = PlacementBehavior.Animated(durationMillis = 900),
        )
    }

    fun addHotKey() {
        if (items.none { it.id == hotKey }) {
            items.add(1, DemoItem(hotKey))
        }
    }

    fun removeHotKey() {
        val index = items.indexOfFirst { it.id == hotKey }
        if (index >= 0) {
            items.removeAt(index)
        }
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.section_semantics_showcase),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.semantics_showcase_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SemanticsActionButton(
                        label = stringResource(R.string.semantics_reset),
                        onClick = {
                            items.clear()
                            items.addAll(stableSeed.map(::DemoItem))
                            listState.clearExitingNow()
                        },
                        enabled = true,
                        prominent = true,
                    )
                    SemanticsActionButton(
                        label = stringResource(R.string.semantics_add_hot_key),
                        onClick = { addHotKey() },
                        enabled = items.none { it.id == hotKey },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SemanticsActionButton(
                        label = stringResource(R.string.semantics_remove_hot_key),
                        onClick = { removeHotKey() },
                        enabled = items.any { it.id == hotKey },
                    )
                    SemanticsActionButton(
                        label = stringResource(R.string.semantics_shuffle_stable),
                        onClick = {
                            val hot = items.firstOrNull { it.id == hotKey }
                            items.clear()
                            items.addAll(stableSeed.shuffled().map(::DemoItem))
                            if (hot != null) {
                                items.add(1, hot)
                            }
                        },
                        enabled = true,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SemanticsActionButton(
                        label = stringResource(R.string.semantics_remove_reinsert),
                        onClick = {
                            removeHotKey()
                            scope.launch {
                                delay(420)
                                addHotKey()
                            }
                        },
                        enabled = items.any { it.id == hotKey },
                    )
                    SemanticsActionButton(
                        label = stringResource(R.string.semantics_remove_clear_exiting),
                        onClick = {
                            removeHotKey()
                            scope.launch {
                                delay(420)
                                listState.clearExitingNow()
                            }
                        },
                        enabled = items.any { it.id == hotKey },
                    )
                }
            }
        }

        Text(
            text = if (listState.isAnimating) {
                stringResource(R.string.animating_true)
            } else {
                stringResource(R.string.animating_false)
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )

        AnimatedColumn(
            items = items.toList(),
            key = { it.id },
            state = listState,
            transitionSpec = transitionSpec,
            modifier = Modifier.fillMaxWidth(),
        ) { item ->
            val phaseColor = when (phase) {
                ItemPhase.Entering -> Color(0xFF4A90E2)
                ItemPhase.Visible -> Color(0xFF34A853)
                ItemPhase.Exiting -> Color(0xFFEF6C00)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(phaseColor.copy(alpha = 0.14f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.item_title, item.id),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    color = phaseColor.copy(alpha = 0.20f),
                                    shape = RoundedCornerShape(50),
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = when (phase) {
                                    ItemPhase.Entering -> stringResource(R.string.status_entering)
                                    ItemPhase.Visible -> stringResource(R.string.status_visible)
                                    ItemPhase.Exiting -> stringResource(R.string.status_exiting)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = phaseColor,
                            )
                        }
                    }

                    Text(
                        text = if (item.id == hotKey) {
                            stringResource(R.string.semantics_hot_key_caption)
                        } else {
                            stringResource(R.string.semantics_stable_key_caption)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = phaseColor,
                    )
                    Text(
                        text = stringResource(
                            R.string.lifecycle_progress_debug,
                            visibilityProgress,
                            placementProgress,
                            progress,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

