package com.rend1x.composeanimatedlist.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rend1x.composeanimatedlist.AnimatedColumn
import com.rend1x.composeanimatedlist.animation.AnimatedItemTransitionSpec
import com.rend1x.composeanimatedlist.animation.EnterSpec
import com.rend1x.composeanimatedlist.animation.ExitSpec
import com.rend1x.composeanimatedlist.animation.PlacementBehavior
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private enum class RecordingScenario(
    val label: String,
) {
    Basic("Basic"),
    Compare("Compare"),
    Shuffle("Shuffle"),
    Orders("Orders"),
    Load("Load"),
    Variants("Variants"),
    Edges("Edges"),
}

private data class RecordingItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val accent: Color,
)

@Composable
internal fun RecordingExamplesPage(
    modifier: Modifier = Modifier,
) {
    var currentScenario by remember { mutableStateOf(RecordingScenario.Basic) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecordingScenario.entries.forEach { scenario ->
                FilterButton(
                    selected = currentScenario == scenario,
                    label = scenario.label,
                    onClick = { currentScenario = scenario },
                )
            }
        }

        when (currentScenario) {
            RecordingScenario.Basic -> BasicRecordingScenario()
            RecordingScenario.Compare -> ComparisonRecordingScenario()
            RecordingScenario.Shuffle -> ShuffleRecordingScenario()
            RecordingScenario.Orders -> OrdersRecordingScenario()
            RecordingScenario.Load -> LoadRecordingScenario()
            RecordingScenario.Variants -> VariantsRecordingScenario()
            RecordingScenario.Edges -> EdgeCasesRecordingScenario()
        }
    }
}

@Composable
private fun BasicRecordingScenario() {
    val items = rememberRecordingItems()
    var nextId by remember { mutableIntStateOf(7) }
    var autoplay by remember { mutableStateOf(false) }

    LaunchedEffect(autoplay) {
        while (autoplay && isActive) {
            delay(650)
            items.add(1, recordingItem(nextId))
            nextId++
            delay(650)
            if (items.size > 4) items.removeAt(items.lastIndex - 1)
        }
    }

    ScenarioHeader(
        title = "Basic insert / remove",
        subtitle = "Record this first: rows enter, exit, and neighbors reflow.",
        icon = Icons.Filled.Add,
    )
    PrimaryActions(
        onPlay = { autoplay = !autoplay },
        playing = autoplay,
        onReset = {
            autoplay = false
            items.resetRecordingItems()
            nextId = 7
        },
    ) {
        IconButton(onClick = {
            items.add(1, recordingItem(nextId))
            nextId++
        }) {
            Icon(Icons.Filled.Add, contentDescription = "Add item")
        }
        IconButton(
            onClick = { if (items.isNotEmpty()) items.removeAt(1.coerceAtMost(items.lastIndex)) },
            enabled = items.isNotEmpty(),
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove item")
        }
    }
    DemoAnimatedList(items)
}

@Composable
private fun ComparisonRecordingScenario() {
    val items = rememberRecordingItems()
    var nextId by remember { mutableIntStateOf(7) }
    var autoplay by remember { mutableStateOf(false) }

    LaunchedEffect(autoplay) {
        while (autoplay && isActive) {
            delay(700)
            items.add(0, recordingItem(nextId))
            nextId++
            delay(700)
            if (items.size > 3) items.removeAt(2)
        }
    }

    ScenarioHeader(
        title = "Animated vs plain Column",
        subtitle = "Use this for the README: the same data changes on both sides.",
        icon = Icons.AutoMirrored.Filled.CompareArrows,
    )
    PrimaryActions(
        onPlay = { autoplay = !autoplay },
        playing = autoplay,
        onReset = {
            autoplay = false
            items.resetRecordingItems()
            nextId = 7
        },
    ) {
        IconButton(onClick = {
            items.add(0, recordingItem(nextId))
            nextId++
        }) {
            Icon(Icons.Filled.Add, contentDescription = "Add item")
        }
        IconButton(
            onClick = { if (items.size > 2) items.removeAt(2) },
            enabled = items.size > 2,
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove item")
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ComparisonPane(
            label = "Animated",
            modifier = Modifier.weight(1f),
        ) {
            DemoAnimatedList(
                items = items,
                compact = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        ComparisonPane(
            label = "Plain",
            modifier = Modifier.weight(1f),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items.forEach { item ->
                    DemoRow(item = item, compact = true)
                }
            }
        }
    }
}

@Composable
private fun ShuffleRecordingScenario() {
    val items = rememberRecordingItems(count = 8)
    var autoplay by remember { mutableStateOf(false) }

    LaunchedEffect(autoplay) {
        while (autoplay && isActive) {
            delay(850)
            items.shuffleByPattern()
        }
    }

    ScenarioHeader(
        title = "Stable-key shuffle",
        subtitle = "Shows placement animation when item order changes sharply.",
        icon = Icons.Filled.Shuffle,
    )
    PrimaryActions(
        onPlay = { autoplay = !autoplay },
        playing = autoplay,
        onReset = {
            autoplay = false
            items.resetRecordingItems(8)
        },
    ) {
        IconButton(onClick = { items.shuffleByPattern() }) {
            Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle")
        }
    }
    DemoAnimatedList(items)
}

@Composable
private fun OrdersRecordingScenario() {
    val items = remember {
        mutableStateListOf(
            recordingItem(1, "Order #1842", "Driver assigned", Color(0xFF2E7D32)),
            recordingItem(2, "Order #1843", "Waiting for bid", Color(0xFF1565C0)),
            recordingItem(3, "Order #1844", "Passenger nearby", Color(0xFF6A1B9A)),
            recordingItem(4, "Order #1845", "Pickup in 4 min", Color(0xFFEF6C00)),
        )
    }
    var nextId by remember { mutableIntStateOf(1846) }
    var autoplay by remember { mutableStateOf(false) }

    LaunchedEffect(autoplay) {
        while (autoplay && isActive) {
            delay(900)
            items.add(0, orderItem(nextId))
            nextId++
            delay(900)
            if (items.size > 4) items.removeAt(items.lastIndex)
        }
    }

    ScenarioHeader(
        title = "Real UI: orders",
        subtitle = "Record this when you want the library to look integrated, not synthetic.",
        icon = Icons.Filled.PlayArrow,
    )
    PrimaryActions(
        onPlay = { autoplay = !autoplay },
        playing = autoplay,
        onReset = {
            autoplay = false
            items.clear()
            items.addAll(
                listOf(
                    recordingItem(1, "Order #1842", "Driver assigned", Color(0xFF2E7D32)),
                    recordingItem(2, "Order #1843", "Waiting for bid", Color(0xFF1565C0)),
                    recordingItem(3, "Order #1844", "Passenger nearby", Color(0xFF6A1B9A)),
                    recordingItem(4, "Order #1845", "Pickup in 4 min", Color(0xFFEF6C00)),
                ),
            )
            nextId = 1846
        },
    ) {
        IconButton(onClick = {
            items.add(0, orderItem(nextId))
            nextId++
        }) {
            Icon(Icons.Filled.Add, contentDescription = "Add order")
        }
        IconButton(
            onClick = { if (items.isNotEmpty()) items.removeAt(items.lastIndex) },
            enabled = items.isNotEmpty(),
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove order")
        }
    }
    DemoAnimatedList(items)
}

@Composable
private fun LoadRecordingScenario() {
    val items = rememberRecordingItems(count = 14)
    var nextId by remember { mutableIntStateOf(15) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running && isActive) {
            delay(90)
            items.add(0, recordingItem(nextId))
            nextId++
            if (items.size > 18) {
                items.removeAt(items.lastIndex)
            }
        }
    }

    ScenarioHeader(
        title = "Many updates",
        subtitle = "Continuous insert/remove pressure for performance-oriented clips.",
        icon = Icons.Filled.Bolt,
    )
    PrimaryActions(
        onPlay = { running = !running },
        playing = running,
        onReset = {
            running = false
            items.resetRecordingItems(14)
            nextId = 15
        },
    ) {
        FilledTonalButton(onClick = {
            repeat(5) {
                items.add(0, recordingItem(nextId))
                nextId++
                if (items.size > 18) items.removeAt(items.lastIndex)
            }
        }) {
            Text("Burst")
        }
    }
    DemoAnimatedList(items, compact = true)
}

@Composable
private fun VariantsRecordingScenario() {
    val items = rememberRecordingItems()
    var nextId by remember { mutableIntStateOf(7) }
    var selectedVariant by remember { mutableStateOf(AnimationVariant.FadeSlide) }

    ScenarioHeader(
        title = "Animation variants",
        subtitle = "Switch presets, then add/remove the same row to capture different styles.",
        icon = Icons.Filled.PlayArrow,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimationVariant.entries.forEach { variant ->
            FilterButton(
                selected = selectedVariant == variant,
                label = variant.label,
                onClick = { selectedVariant = variant },
            )
        }
    }
    PrimaryActions(
        onPlay = {
            items.add(1, recordingItem(nextId))
            nextId++
        },
        playing = false,
        playIcon = Icons.Filled.Add,
        onReset = {
            items.resetRecordingItems()
            nextId = 7
        },
    ) {
        IconButton(
            onClick = { if (items.isNotEmpty()) items.removeAt(1.coerceAtMost(items.lastIndex)) },
            enabled = items.isNotEmpty(),
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove item")
        }
    }
    DemoAnimatedList(
        items = items,
        transitionSpec = selectedVariant.spec,
    )
}

@Composable
private fun EdgeCasesRecordingScenario() {
    val items = rememberRecordingItems(count = 7)
    var nextId by remember { mutableIntStateOf(8) }

    ScenarioHeader(
        title = "Edge cases",
        subtitle = "Fast removals, middle insert, clear, restore.",
        icon = Icons.Filled.ClearAll,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = {
                repeat(3) {
                    if (items.isNotEmpty()) items.removeAt(0)
                }
            },
        ) {
            Text("Remove 3")
        }
        Button(
            modifier = Modifier.weight(1f),
            onClick = {
                val index = (items.size / 2).coerceAtLeast(0)
                items.add(index, recordingItem(nextId))
                nextId++
            },
        ) {
            Text("Insert middle")
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = { items.clear() },
        ) {
            Text("Clear")
        }
        FilledTonalButton(
            modifier = Modifier.weight(1f),
            onClick = {
                items.resetRecordingItems(7)
                nextId = 8
            },
        ) {
            Text("Restore")
        }
    }
    DemoAnimatedList(items)
}

@Composable
private fun ScenarioHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PrimaryActions(
    onPlay: () -> Unit,
    playing: Boolean,
    onReset: () -> Unit,
    playIcon: ImageVector = Icons.Filled.PlayArrow,
    trailingContent: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = onPlay,
        ) {
            Icon(
                imageVector = if (playing) Icons.Filled.RestartAlt else playIcon,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(if (playing) "Stop" else "Action")
        }
        FilledTonalButton(onClick = onReset) {
            Icon(Icons.Filled.RestartAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Reset")
        }
        trailingContent()
    }
}

@Composable
private fun ComparisonPane(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
private fun DemoAnimatedList(
    items: List<RecordingItem>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    transitionSpec: AnimatedItemTransitionSpec = defaultRecordingSpec,
) {
    val renderItems = items.toList()

    AnimatedColumn(
        items = renderItems,
        key = { it.id },
        transitionSpec = transitionSpec,
        modifier = modifier.fillMaxWidth(),
    ) { item ->
        DemoRow(
            item = item,
            compact = compact,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (compact) 3.dp else 5.dp)
                .graphicsLayer {
                    alpha = 0.35f + 0.65f * visibilityProgress
                    scaleX = 0.96f + 0.04f * visibilityProgress
                    scaleY = 0.96f + 0.04f * placementProgress
                },
        )
    }
}

@Composable
private fun DemoRow(
    item: RecordingItem,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(if (compact) 8.dp else 12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (compact) 1.dp else 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 8.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 28.dp else 38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(item.accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.id.toString().takeLast(2),
                    style = if (compact) {
                        MaterialTheme.typography.labelSmall
                    } else {
                        MaterialTheme.typography.labelLarge
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = if (compact) {
                        MaterialTheme.typography.labelLarge
                    } else {
                        MaterialTheme.typography.titleSmall
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!compact) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberRecordingItems(
    count: Int = 6,
) = remember {
    mutableStateListOf<RecordingItem>().apply {
        addAll((1..count).map(::recordingItem))
    }
}

private fun MutableList<RecordingItem>.resetRecordingItems(count: Int = 6) {
    clear()
    addAll((1..count).map(::recordingItem))
}

private fun MutableList<RecordingItem>.shuffleByPattern() {
    val reordered = mapIndexed { index, item -> index to item }
        .sortedWith(compareBy({ it.first % 2 }, { -it.first }))
        .map { it.second }
    clear()
    addAll(reordered)
}

private fun recordingItem(
    id: Int,
    title: String = "Item $id",
    subtitle: String = listOf(
        "Inserted with stable key",
        "Placement animation ready",
        "Smooth enter and exit",
        "Neighbor rows reflow",
    )[id % 4],
    accent: Color = demoAccents[id % demoAccents.size],
) = RecordingItem(id, title, subtitle, accent)

private fun orderItem(id: Int) = recordingItem(
    id = id,
    title = "Order #$id",
    subtitle = listOf(
        "New bid received",
        "Route updated",
        "Driver approaching",
        "Pickup confirmed",
    )[id % 4],
    accent = demoAccents[id % demoAccents.size],
)

private enum class AnimationVariant(
    val label: String,
    val spec: AnimatedItemTransitionSpec,
) {
    FadeSlide(
        "Fade + slide",
        AnimatedItemTransitionSpec(
            enter = EnterSpec.FadeAndSlide(offset = 18.dp, durationMillis = 280),
            exit = ExitSpec.FadeAndSlide(offset = 18.dp, durationMillis = 240),
            placement = PlacementBehavior.Animated(durationMillis = 300),
        ),
    ),
    Fade(
        "Fade",
        AnimatedItemTransitionSpec(
            enter = EnterSpec.Fade(durationMillis = 260),
            exit = ExitSpec.Fade(durationMillis = 220),
            placement = PlacementBehavior.Animated(durationMillis = 300),
        ),
    ),
    Slide(
        "Slide",
        AnimatedItemTransitionSpec(
            enter = EnterSpec.SlideVertical(offset = 24.dp, durationMillis = 260),
            exit = ExitSpec.SlideVertical(offset = 24.dp, durationMillis = 220),
            placement = PlacementBehavior.Animated(durationMillis = 300),
        ),
    ),
}

private val defaultRecordingSpec = AnimatedItemTransitionSpec(
    enter = EnterSpec.FadeAndSlide(offset = 18.dp, durationMillis = 300),
    exit = ExitSpec.FadeAndSlide(offset = 18.dp, durationMillis = 260),
    placement = PlacementBehavior.Animated(durationMillis = 320),
)

private val demoAccents = listOf(
    Color(0xFF1565C0),
    Color(0xFF2E7D32),
    Color(0xFFAD1457),
    Color(0xFF6A1B9A),
    Color(0xFFEF6C00),
    Color(0xFF00838F),
)
