package com.rend1x.composeanimatedlist.sample

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.rend1x.composeanimatedlist.animatedItem
import com.rend1x.composeanimatedlist.animation.AnimatedItemDefaults
import com.rend1x.composeanimatedlist.state.rememberAnimatedListState

@Composable
internal fun BasicsExamplesPage(
    modifier: Modifier = Modifier,
) {
    val items = remember {
        mutableStateListOf<DemoItem>().apply {
            addAll((1..6).map { DemoItem(it) })
        }
    }
    var nextId by remember { mutableIntStateOf(7) }

    val listState = rememberAnimatedListState()

    val activeTags = remember {
        mutableStateListOf(
            TagChip(1, "Compose"),
            TagChip(2, "Animation"),
            TagChip(3, "Lists"),
        )
    }
    var nextTagId by remember { mutableIntStateOf(4) }
    val tagPool = remember {
        listOf("Kotlin", "Diff", "UI", "Motion", "Samples")
    }
    var horizontalCentered by remember { mutableStateOf(true) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )

        Text(
            text = stringResource(R.string.section_list_layout),
            style = MaterialTheme.typography.titleSmall,
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

        Text(
            text = stringResource(R.string.section_easy_path),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.easy_path_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AnimatedColumn(
            items = items.toList(),
            key = { it.id },
            state = listState,
            transitionSpec = AnimatedItemDefaults.none(),
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
                    .padding(vertical = 4.dp)
                    .animatedItem(this),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Text(
                    text = stringResource(R.string.item_title, item.id),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Text(
            text = stringResource(R.string.section_tags_easy),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.tag_remove_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AnimatedColumn(
            items = activeTags.toList(),
            key = { it.id },
            transitionSpec = AnimatedItemDefaults.none(),
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (horizontalCentered) {
                Alignment.CenterHorizontally
            } else {
                Alignment.Start
            },
        ) { tag ->
            Surface(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .animatedItem(this)
                    .clickable {
                        activeTags.remove(tag)
                    },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shadowElevation = 0.dp,
            ) {
                Text(
                    text = tag.label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tagPool.forEach { label ->
                val already = activeTags.any { it.label == label }
                Button(
                    onClick = {
                        if (!already) {
                            activeTags.add(TagChip(nextTagId, label))
                            nextTagId++
                        }
                    },
                    enabled = !already,
                ) {
                    Text("+ $label")
                }
            }
        }
    }
}

