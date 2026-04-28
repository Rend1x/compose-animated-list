package com.rend1x.composeanimatedlist.sample

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
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
import com.rend1x.composeanimatedlist.animation.VerticalDirection
import com.rend1x.composeanimatedlist.state.rememberAnimatedListState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class DemoItem(val id: Int)

private data class TagChip(val id: Int, val label: String)

private enum class EnterKind { None, Fade, SlideVertical, FadeAndSlide }

private enum class ExitKind { None, Fade, SlideVertical, FadeAndSlide }

private enum class PlacementKind { None, Animated }

private enum class SamplePage { Basics, Semantics, Advanced, Stress }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleListScreen() {
    var currentPage by remember { mutableStateOf(SamplePage.Basics) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .width(304.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.menu_drawer_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.menu_drawer_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    )
                }
                Text(
                    text = stringResource(R.string.menu_navigation_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SamplePage.entries.forEach { page ->
                        NavigationDrawerItem(
                            selected = currentPage == page,
                            label = {
                                Text(
                                    text = stringResource(samplePageLabel(page)),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = samplePageIcon(page),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                currentPage = page
                                scope.launch { drawerState.close() }
                            },
                        )
                    }
                }
            }
        },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.screen_heading_short),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(
                                    R.string.current_page_label,
                                    stringResource(samplePageLabel(currentPage)),
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                    IconButton(
                        onClick = {
                            scope.launch { drawerState.open() }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.menu_open),
                        )
                    }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                when (currentPage) {
                    SamplePage.Basics -> BasicsExamplesPage(
                        modifier = Modifier.weight(1f),
                    )

                    SamplePage.Semantics -> SemanticsExamplesPage(
                        modifier = Modifier.weight(1f),
                    )

                    SamplePage.Advanced -> AdvancedExamplesPage(
                        modifier = Modifier.weight(1f),
                    )

                    SamplePage.Stress -> StressExamplesPage(
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BasicsExamplesPage(
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

@Composable
private fun SemanticsExamplesPage(
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
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RemoveModeButton(
                        label = stringResource(R.string.semantics_reset),
                        onClick = {
                            items.clear()
                            items.addAll(stableSeed.map(::DemoItem))
                            listState.clearExitingNow()
                        },
                        enabled = true,
                    )
                    RemoveModeButton(
                        label = stringResource(R.string.semantics_add_hot_key),
                        onClick = { addHotKey() },
                        enabled = items.none { it.id == hotKey },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RemoveModeButton(
                        label = stringResource(R.string.semantics_remove_hot_key),
                        onClick = { removeHotKey() },
                        enabled = items.any { it.id == hotKey },
                    )
                    RemoveModeButton(
                        label = stringResource(R.string.semantics_shuffle_stable),
                        onClick = {
                            val hot = items.firstOrNull { it.id == hotKey }
                            items.removeAll { it.id != hotKey }
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RemoveModeButton(
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
                    RemoveModeButton(
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

@Composable
private fun AdvancedExamplesPage(
    modifier: Modifier = Modifier,
) {
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

    val transitionSpec = remember(
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
            EnterKind.None -> EnterSpec.None
            EnterKind.Fade -> EnterSpec.Fade(enterDuration)
            EnterKind.SlideVertical -> EnterSpec.SlideVertical(enterOffsetDp.dp, enterDuration)
            EnterKind.FadeAndSlide -> EnterSpec.FadeAndSlide(enterOffsetDp.dp, enterDuration)
        }
        val exit = when (exitKind) {
            ExitKind.None -> ExitSpec.None
            ExitKind.Fade -> ExitSpec.Fade(exitDuration)
            ExitKind.SlideVertical -> ExitSpec.SlideVertical(
                exitOffsetDp.dp,
                exitDirection,
                exitDuration,
            )

            ExitKind.FadeAndSlide -> ExitSpec.FadeAndSlide(
                exitOffsetDp.dp,
                exitDirection,
                exitDuration,
            )
        }
        val placement = when (placementKind) {
            PlacementKind.None -> PlacementBehavior.None
            PlacementKind.Animated -> PlacementBehavior.Animated(placementDuration)
        }
        AnimatedItemTransitionSpec(enter = enter, exit = exit, placement = placement)
    }

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
            text = stringResource(R.string.section_advanced_path),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.advanced_path_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.section_presets),
                    style = MaterialTheme.typography.titleSmall,
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = stringResource(R.string.section_enter),
                    style = MaterialTheme.typography.titleSmall,
                )
                EnterKind.entries.forEach { kind ->
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = stringResource(R.string.section_exit),
                    style = MaterialTheme.typography.titleSmall,
                )
                ExitKind.entries.forEach { kind ->
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
                    style = MaterialTheme.typography.labelMedium,
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = stringResource(R.string.section_placement),
                    style = MaterialTheme.typography.titleSmall,
                )
                PlacementKind.entries.forEach { kind ->
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = stringResource(R.string.section_remove),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.remove_hint),
                    style = MaterialTheme.typography.bodySmall,
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
            text = stringResource(R.string.section_list_advanced),
            style = MaterialTheme.typography.titleSmall,
        )
        AnimatedColumn(
            items = items.toList(),
            key = { it.id },
            state = listState,
            transitionSpec = transitionSpec,
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
                    .graphicsLayer {
                        scaleX = 0.9f + 0.1f * visibilityProgress
                        scaleY = 0.9f + 0.1f * placementProgress
                    },
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.item_title, item.id),
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
                    if (phase != ItemPhase.Visible || progress < 1f) {
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        )
                    }
                    if (phase != ItemPhase.Visible) {
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
}

@Composable
private fun StressExamplesPage(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InterruptionStressSection()
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
            style = MaterialTheme.typography.bodyMedium,
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
            style = MaterialTheme.typography.labelMedium,
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
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
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

private fun samplePageLabel(page: SamplePage): Int = when (page) {
    SamplePage.Basics -> R.string.page_basics
    SamplePage.Semantics -> R.string.page_semantics
    SamplePage.Advanced -> R.string.page_advanced
    SamplePage.Stress -> R.string.page_stress
}

private fun samplePageIcon(page: SamplePage): ImageVector = when (page) {
    SamplePage.Basics -> Icons.Filled.Home
    SamplePage.Semantics -> Icons.Filled.AutoAwesome
    SamplePage.Advanced -> Icons.Filled.Tune
    SamplePage.Stress -> Icons.Filled.Warning
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
