package com.rend1x.composeanimatedlist.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.rend1x.composeanimatedlist.ItemPhase
import com.rend1x.composeanimatedlist.animatedItem
import com.rend1x.composeanimatedlist.animation.AnimatedItemDefaults
import com.rend1x.composeanimatedlist.animation.AnimatedItemTransitionSpec
import com.rend1x.composeanimatedlist.animation.EnterSpec
import com.rend1x.composeanimatedlist.animation.ExitSpec
import com.rend1x.composeanimatedlist.animation.PlacementBehavior
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

private data class StressItem(val id: Int)

@Composable
fun InterruptionStressSection() {
    val stressItems = remember {
        mutableStateListOf(StressItem(1))
    }
    var nextStressId by remember { mutableIntStateOf(2) }
    var randomLoop by remember { mutableStateOf(false) }
    var ultraFastLoop by remember { mutableStateOf(false) }
    var sameKeyBurst by remember { mutableStateOf(false) }
    var showShellDebug by remember { mutableStateOf(false) }

    val stressSpec = remember {
        AnimatedItemTransitionSpec(
            enter = EnterSpec.FadeAndSlide(offset = 20.dp, durationMillis = 420),
            exit = ExitSpec.FadeAndSlide(offset = 20.dp, durationMillis = 360),
            placement = PlacementBehavior.Animated(durationMillis = 320),
        )
    }

    LaunchedEffect(randomLoop) {
        while (randomLoop && isActive) {
            delay(45)
            when (Random.nextInt(4)) {
                0 -> if (stressItems.isNotEmpty()) {
                    stressItems.removeAt(Random.nextInt(stressItems.size))
                }

                1 -> {
                    stressItems.add(StressItem(nextStressId))
                    nextStressId++
                }

                2 -> if (stressItems.isNotEmpty()) {
                    val i = Random.nextInt(stressItems.size)
                    stressItems.removeAt(i)
                }

                else -> { /* hold */
                }
            }
        }
    }

    LaunchedEffect(ultraFastLoop) {
        while (ultraFastLoop && isActive) {
            delay(16)
            when (Random.nextInt(3)) {
                0, 1 -> if (stressItems.isNotEmpty()) {
                    stressItems.removeAt(Random.nextInt(stressItems.size))
                }

                else -> {
                    stressItems.add(StressItem(nextStressId))
                    nextStressId++
                }
            }
        }
    }

    LaunchedEffect(sameKeyBurst) {
        while (sameKeyBurst && isActive) {
            delay(16)
            if (stressItems.any { it.id == 1 }) {
                stressItems.removeAll { it.id == 1 }
            } else {
                stressItems.add(0, StressItem(1))
            }
        }
    }

    Text(
        text = stringResource(R.string.section_interruption_stress),
        style = MaterialTheme.typography.titleSmall,
    )
    Text(
        text = stringResource(R.string.interruption_stress_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = {
                repeat(12) {
                    if (stressItems.isNotEmpty()) stressItems.removeAt(stressItems.lastIndex)
                    stressItems.add(StressItem(nextStressId))
                    nextStressId++
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.interruption_stress_spam))
        }
        FilledTonalButton(
            onClick = {
                if (stressItems.any { it.id == 1 }) {
                    stressItems.removeAll { it.id == 1 }
                } else {
                    stressItems.add(0, StressItem(1))
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.interruption_stress_toggle_keyed))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.interruption_stress_random_loop),
            style = MaterialTheme.typography.bodyMedium,
        )
        Switch(
            checked = randomLoop,
            onCheckedChange = {
                randomLoop = it
                if (it) {
                    ultraFastLoop = false
                    sameKeyBurst = false
                }
            },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.interruption_stress_ultra_fast_loop),
            style = MaterialTheme.typography.bodyMedium,
        )
        Switch(
            checked = ultraFastLoop,
            onCheckedChange = {
                ultraFastLoop = it
                if (it) {
                    randomLoop = false
                    sameKeyBurst = false
                }
            },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.interruption_stress_same_key_burst),
            style = MaterialTheme.typography.bodyMedium,
        )
        Switch(
            checked = sameKeyBurst,
            onCheckedChange = {
                sameKeyBurst = it
                if (it) {
                    randomLoop = false
                    ultraFastLoop = false
                }
            },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.interruption_stress_show_shell_debug),
            style = MaterialTheme.typography.bodyMedium,
        )
        Switch(
            checked = showShellDebug,
            onCheckedChange = { showShellDebug = it },
        )
    }

    AnimatedColumn(
        items = stressItems.toList(),
        key = { it.id },
        transitionSpec = stressSpec,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) { item ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .animatedItem(this),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.interruption_stress_row_title, item.id),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = when (phase) {
                        ItemPhase.Entering -> stringResource(R.string.status_entering)
                        ItemPhase.Visible -> stringResource(R.string.status_visible)
                        ItemPhase.Exiting -> stringResource(R.string.removing_label)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (showShellDebug) {
                    Text(
                        text = stringResource(
                            R.string.interruption_stress_shell_debug,
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

    Text(
        text = stringResource(R.string.interruption_stress_none_path_caption),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp),
    )
    AnimatedColumn(
        items = stressItems.toList(),
        key = { it.id },
        transitionSpec = AnimatedItemDefaults.none(),
        modifier = Modifier.fillMaxWidth(),
    ) { item ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .animatedItem(this),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Text(
                text = stringResource(R.string.interruption_stress_mirror_row, item.id),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
