package com.rend1x.composeanimatedlist.sample

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rend1x.composeanimatedlist.AnimatedColumn
import com.rend1x.composeanimatedlist.animation.AnimatedListTransition
import com.rend1x.composeanimatedlist.animation.EnterBehavior
import com.rend1x.composeanimatedlist.animation.ExitBehavior
import com.rend1x.composeanimatedlist.animation.PlacementBehavior
import com.rend1x.composeanimatedlist.animation.VerticalDirection
import com.rend1x.composeanimatedlist.state.rememberAnimatedListState
import kotlin.random.Random

private data class DemoItem(val id: Int)

private enum class EnterKind { None, Fade, SlideVertical, FadeAndSlide }

private enum class ExitKind { None, Fade, SlideVertical, FadeAndSlide }

private enum class PlacementKind { None, Animated }

@Composable
fun SampleListScreen() {
    val items = remember {
        mutableStateListOf<DemoItem>().apply {
            addAll((1..6).map { DemoItem(it) })
        }
    }
    var nextId by remember { mutableIntStateOf(7) }

    val listState = rememberAnimatedListState()

    var enterKind by remember { mutableStateOf(EnterKind.FadeAndSlide) }
    var enterDuration by remember { mutableIntStateOf(240) }
    var enterOffsetDp by remember { mutableIntStateOf(16) }

    var exitKind by remember { mutableStateOf(ExitKind.FadeAndSlide) }
    var exitDuration by remember { mutableIntStateOf(200) }
    var exitOffsetDp by remember { mutableIntStateOf(16) }
    var exitDirection by remember { mutableStateOf(VerticalDirection.Up) }

    var placementKind by remember { mutableStateOf(PlacementKind.Animated) }
    var placementDuration by remember { mutableIntStateOf(260) }

    var horizontalCentered by remember { mutableStateOf(true) }

    val transition = remember(
        enterKind,
        enterDuration,
        enterOffsetDp,
        exitKind,
        exitDuration,
        exitOffsetDp,
        exitDirection,
        placementKind,
        placementDuration,
    ) {
        val enter = when (enterKind) {
            EnterKind.None -> EnterBehavior.None
            EnterKind.Fade -> EnterBehavior.Fade(enterDuration)
            EnterKind.SlideVertical -> EnterBehavior.SlideVertical(enterOffsetDp.dp, enterDuration)
            EnterKind.FadeAndSlide -> EnterBehavior.FadeAndSlide(enterOffsetDp.dp, enterDuration)
        }
        val exit = when (exitKind) {
            ExitKind.None -> ExitBehavior.None
            ExitKind.Fade -> ExitBehavior.Fade(exitDuration)
            ExitKind.SlideVertical -> ExitBehavior.SlideVertical(
                exitOffsetDp.dp,
                exitDirection,
                exitDuration
            )

            ExitKind.FadeAndSlide -> ExitBehavior.FadeAndSlide(
                exitOffsetDp.dp,
                exitDirection,
                exitDuration
            )
        }
        val placement = when (placementKind) {
            PlacementKind.None -> PlacementBehavior.None
            PlacementKind.Animated -> PlacementBehavior.Animated(placementDuration)
        }
        AnimatedListTransition(enter = enter, exit = exit, placement = placement)
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.screen_heading),
            style = MaterialTheme.typography.h6,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    items.add(DemoItem(nextId))
                    nextId++
                },
            ) {
                Text(stringResource(R.string.action_add))
            }
            Button(
                onClick = { if (items.isNotEmpty()) items.removeAt(items.lastIndex) },
                enabled = items.isNotEmpty(),
            ) {
                Text(stringResource(R.string.action_remove_last))
            }
        }

        Text(
            text = if (listState.isAnimating) {
                stringResource(R.string.animating_true)
            } else {
                stringResource(R.string.animating_false)
            },
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.secondary,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.section_presets),
                    style = MaterialTheme.typography.subtitle2,
                )
                PresetRow(
                    label = stringResource(R.string.preset_default),
                    onClick = {
                        enterKind = EnterKind.FadeAndSlide
                        exitKind = ExitKind.FadeAndSlide
                        placementKind = PlacementKind.Animated
                        enterDuration = 240
                        exitDuration = 200
                        placementDuration = 260
                        enterOffsetDp = 16
                        exitOffsetDp = 16
                        exitDirection = VerticalDirection.Up
                    },
                )
                PresetRow(
                    label = stringResource(R.string.preset_fade),
                    onClick = {
                        enterKind = EnterKind.Fade
                        exitKind = ExitKind.Fade
                        placementKind = PlacementKind.Animated
                        enterDuration = 220
                        exitDuration = 180
                        placementDuration = 260
                    },
                )
                PresetRow(
                    label = stringResource(R.string.preset_slide_vertical),
                    onClick = {
                        enterKind = EnterKind.SlideVertical
                        exitKind = ExitKind.SlideVertical
                        placementKind = PlacementKind.Animated
                        enterDuration = 220
                        exitDuration = 180
                        placementDuration = 260
                        enterOffsetDp = 16
                        exitOffsetDp = 16
                    },
                )
                PresetRow(
                    label = stringResource(R.string.preset_none),
                    onClick = {
                        enterKind = EnterKind.None
                        exitKind = ExitKind.None
                        placementKind = PlacementKind.None
                    },
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = stringResource(R.string.section_enter),
                    style = MaterialTheme.typography.subtitle2,
                )
                EnterKind.values().forEach { kind ->
                    RadioOptionRow(
                        selected = enterKind == kind,
                        label = stringResource(enterKindLabel(kind)),
                        onClick = { enterKind = kind },
                    )
                }
                DurationSlider(
                    label = stringResource(R.string.duration_enter_ms),
                    value = enterDuration,
                    onValueChange = { enterDuration = it },
                    enabled = enterKind != EnterKind.None,
                )
                DurationSlider(
                    label = stringResource(R.string.offset_enter_dp),
                    value = enterOffsetDp,
                    valueRange = 0..48,
                    onValueChange = { enterOffsetDp = it },
                    enabled = enterKind == EnterKind.SlideVertical || enterKind == EnterKind.FadeAndSlide,
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = stringResource(R.string.section_exit),
                    style = MaterialTheme.typography.subtitle2,
                )
                ExitKind.values().forEach { kind ->
                    RadioOptionRow(
                        selected = exitKind == kind,
                        label = stringResource(exitKindLabel(kind)),
                        onClick = { exitKind = kind },
                    )
                }
                DurationSlider(
                    label = stringResource(R.string.duration_exit_ms),
                    value = exitDuration,
                    onValueChange = { exitDuration = it },
                    enabled = exitKind != ExitKind.None,
                )
                DurationSlider(
                    label = stringResource(R.string.offset_exit_dp),
                    value = exitOffsetDp,
                    valueRange = 0..48,
                    onValueChange = { exitOffsetDp = it },
                    enabled = exitKind == ExitKind.SlideVertical || exitKind == ExitKind.FadeAndSlide,
                )
                Text(
                    text = stringResource(R.string.exit_direction),
                    style = MaterialTheme.typography.caption,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterButton(
                        selected = exitDirection == VerticalDirection.Up,
                        label = stringResource(R.string.direction_up),
                        onClick = { exitDirection = VerticalDirection.Up },
                        enabled = exitKind == ExitKind.SlideVertical || exitKind == ExitKind.FadeAndSlide,
                    )
                    FilterButton(
                        selected = exitDirection == VerticalDirection.Down,
                        label = stringResource(R.string.direction_down),
                        onClick = { exitDirection = VerticalDirection.Down },
                        enabled = exitKind == ExitKind.SlideVertical || exitKind == ExitKind.FadeAndSlide,
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = stringResource(R.string.section_placement),
                    style = MaterialTheme.typography.subtitle2,
                )
                PlacementKind.values().forEach { kind ->
                    RadioOptionRow(
                        selected = placementKind == kind,
                        label = stringResource(placementKindLabel(kind)),
                        onClick = { placementKind = kind },
                    )
                }
                DurationSlider(
                    label = stringResource(R.string.duration_placement_ms),
                    value = placementDuration,
                    onValueChange = { placementDuration = it },
                    enabled = placementKind == PlacementKind.Animated,
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = stringResource(R.string.section_list_layout),
                    style = MaterialTheme.typography.subtitle2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterButton(
                        selected = horizontalCentered,
                        label = stringResource(R.string.align_center),
                        onClick = { horizontalCentered = true },
                    )
                    FilterButton(
                        selected = !horizontalCentered,
                        label = stringResource(R.string.align_start),
                        onClick = { horizontalCentered = false },
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = stringResource(R.string.section_remove),
                    style = MaterialTheme.typography.subtitle2,
                )
                Text(
                    text = stringResource(R.string.remove_hint),
                    style = MaterialTheme.typography.caption,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RemoveModeButton(
                        label = stringResource(R.string.remove_first),
                        onClick = {
                            if (items.isNotEmpty()) items.removeAt(0)
                        },
                        enabled = items.isNotEmpty(),
                    )
                    RemoveModeButton(
                        label = stringResource(R.string.remove_random),
                        onClick = {
                            if (items.isNotEmpty()) {
                                items.removeAt(Random.nextInt(items.size))
                            }
                        },
                        enabled = items.isNotEmpty(),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RemoveModeButton(
                        label = stringResource(R.string.remove_second),
                        onClick = {
                            if (items.size >= 2) items.removeAt(1)
                        },
                        enabled = items.size >= 2,
                    )
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { listState.clearExitingNow() },
                    ) {
                        Text(stringResource(R.string.clear_exiting_now))
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.section_list),
            style = MaterialTheme.typography.subtitle2,
        )
        AnimatedColumn(
            items = items.toList(),
            key = { it.id },
            state = listState,
            transition = transition,
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (horizontalCentered) {
                Alignment.CenterHorizontally
            } else {
                Alignment.Start
            },
        ) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = 4.dp,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.item_title, item.id),
                        style = MaterialTheme.typography.subtitle1,
                    )
                    Text(
                        text = when {
                            isEntering -> stringResource(R.string.status_entering)
                            isExiting -> stringResource(R.string.status_exiting)
                            else -> stringResource(R.string.status_idle)
                        },
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetRow(
    label: String,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Text(label)
    }
}

@Composable
private fun RadioOptionRow(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun DurationSlider(
    label: String,
    value: Int,
    valueRange: IntRange = 50..800,
    onValueChange: (Int) -> Unit,
    enabled: Boolean = true,
) {
    Column {
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.caption,
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            enabled = enabled,
        )
    }
}

@Composable
private fun FilterButton(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = if (selected) {
            ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = MaterialTheme.colors.onSecondary,
            )
        } else {
            ButtonDefaults.buttonColors()
        },
    ) {
        Text(label)
    }
}

@Composable
private fun RowScope.RemoveModeButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Button(
        modifier = Modifier.weight(1f),
        onClick = onClick,
        enabled = enabled,
    ) {
        Text(label)
    }
}

private fun enterKindLabel(kind: EnterKind): Int = when (kind) {
    EnterKind.None -> R.string.enter_none
    EnterKind.Fade -> R.string.enter_fade
    EnterKind.SlideVertical -> R.string.enter_slide_vertical
    EnterKind.FadeAndSlide -> R.string.enter_fade_and_slide
}

private fun exitKindLabel(kind: ExitKind): Int = when (kind) {
    ExitKind.None -> R.string.exit_none
    ExitKind.Fade -> R.string.exit_fade
    ExitKind.SlideVertical -> R.string.exit_slide_vertical
    ExitKind.FadeAndSlide -> R.string.exit_fade_and_slide
}

private fun placementKindLabel(kind: PlacementKind): Int = when (kind) {
    PlacementKind.None -> R.string.placement_none
    PlacementKind.Animated -> R.string.placement_animated
}
