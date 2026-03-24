package com.rend1x.composeanimatedlist.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rend1x.composeanimatedlist.AnimatedColumn
import com.rend1x.composeanimatedlist.animation.AnimatedItemDefaults

@Preview(
    name = "AnimatedColumn Demo",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
fun AnimatedColumnPreview() {
    MaterialTheme {
        Surface {
            AnimatedColumnPreviewScreen()
        }
    }
}

@Composable
private fun AnimatedColumnPreviewScreen() {
    var nextId by remember { mutableIntStateOf(6) }
    var items by remember {
        mutableStateOf(
            listOf(
                DemoItem(1, "Item #1"),
                DemoItem(2, "Item #2"),
                DemoItem(3, "Item #3"),
                DemoItem(4, "Item #4"),
                DemoItem(5, "Item #5"),
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    items = items + DemoItem(nextId, "Item #$nextId")
                    nextId += 1
                },
            ) {
                Text("Add")
            }
            Button(
                onClick = {
                    if (items.isNotEmpty()) {
                        items = items.drop(1)
                    }
                },
            ) {
                Text("Remove")
            }
            Button(
                onClick = { items = items.shuffled() },
            ) {
                Text("Shuffle")
            }
            Button(
                onClick = {
                    items = listOf(
                        DemoItem(1, "Item #1"),
                        DemoItem(2, "Item #2"),
                        DemoItem(3, "Item #3"),
                        DemoItem(4, "Item #4"),
                        DemoItem(5, "Item #5"),
                    )
                    nextId = 6
                },
            ) {
                Text("Reset")
            }
        }

        AnimatedColumn(
            items = items,
            key = { it.id },
            transitionSpec = AnimatedItemDefaults.fade(),
            modifier = Modifier.fillMaxWidth(),
        ) { item ->
            DemoItemCard(item = item)
        }
    }
}

@Composable
private fun DemoItemCard(item: DemoItem) {
    val containerColor = if (item.id % 2 == 0) {
        Color(0xFFE8F0FE)
    } else {
        Color(0xFFEFF7E8)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .background(color = containerColor)
                .padding(12.dp),
        ) {
            Text(text = item.title, style = MaterialTheme.typography.subtitle1)
            Text(text = "id=${item.id}", style = MaterialTheme.typography.body2)
        }
    }
}

private data class DemoItem(
    val id: Int,
    val title: String,
)
