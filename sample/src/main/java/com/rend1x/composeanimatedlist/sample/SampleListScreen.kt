package com.rend1x.composeanimatedlist.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rend1x.composeanimatedlist.AnimatedColumn

private data class DemoItem(val id: Int)

@Composable
fun SampleListScreen() {
    val items = remember {
        mutableStateListOf<DemoItem>().apply {
            addAll((1..6).map { DemoItem(it) })
        }
    }
    var nextId by remember { mutableIntStateOf(7) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                Text(stringResource(R.string.action_remove))
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedColumn(
                items = items.toList(),
                key = { it.id },
                horizontalAlignment = Alignment.CenterHorizontally,
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
}
