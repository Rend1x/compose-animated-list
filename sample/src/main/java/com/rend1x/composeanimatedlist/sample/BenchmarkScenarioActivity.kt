package com.rend1x.composeanimatedlist.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rend1x.composeanimatedlist.AnimatedColumn
import com.rend1x.composeanimatedlist.animation.AnimatedItemDefaults
import com.rend1x.composeanimatedlist.animation.AnimatedItemTransitionSpec
import com.rend1x.composeanimatedlist.animation.EnterSpec
import com.rend1x.composeanimatedlist.animation.ExitSpec
import com.rend1x.composeanimatedlist.animation.PlacementBehavior
import kotlinx.coroutines.delay
import kotlin.random.Random

class BenchmarkScenarioActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scenario = requireNotNull(intent.toBenchmarkScenarioOrNull())
        setContent {
            SampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    BenchmarkScenarioScreen(scenario)
                }
            }
        }
    }
}

internal data class BenchmarkScenario(
    val implementation: BenchmarkImplementation,
    val workload: BenchmarkWorkload,
    val profile: BenchmarkProfile,
)

internal enum class BenchmarkImplementation(
    val wireValue: String,
) {
    AnimatedColumn("animated"),
    DefaultColumn("column"),
    LazyColumn("lazy");

    companion object {
        fun fromWireValue(value: String?): BenchmarkImplementation = entries.firstOrNull {
            it.wireValue == value
        } ?: AnimatedColumn
    }
}

internal enum class BenchmarkWorkload(
    val wireValue: String,
) {
    ReinsertBurst("reinsert"),
    WindowedChurn("windowed");

    companion object {
        fun fromWireValue(value: String?): BenchmarkWorkload = entries.firstOrNull {
            it.wireValue == value
        } ?: ReinsertBurst
    }
}

internal enum class BenchmarkProfile(
    val wireValue: String,
    val durationMillis: Int,
    val stepDelayMillis: Long,
    val settleDelayMillis: Long,
) {
    Default(
        wireValue = "default",
        durationMillis = 220,
        stepDelayMillis = 32L,
        settleDelayMillis = 260L,
    ),
    Fast(
        wireValue = "fast",
        durationMillis = 32,
        stepDelayMillis = 16L,
        settleDelayMillis = 48L,
    );

    companion object {
        fun fromWireValue(value: String?): BenchmarkProfile = entries.firstOrNull {
            it.wireValue == value
        } ?: Default
    }
}

private data class BenchmarkRowUi(
    val id: Int,
    val version: Int,
)

private data class BenchmarkWorkloadData(
    val initialItems: List<BenchmarkRowUi>,
    val updates: List<List<BenchmarkRowUi>>,
)

@Composable
internal fun BenchmarkScenarioScreen(
    scenario: BenchmarkScenario,
) {
    val workload = remember(scenario) { scenario.buildWorkload() }
    var items by remember(scenario) { mutableStateOf(workload.initialItems) }
    var statusText by remember(scenario) { mutableStateOf(BENCHMARK_READY_TEXT) }
    var isRunning by remember(scenario) { mutableStateOf(false) }

    LaunchedEffect(scenario, isRunning) {
        if (!isRunning) {
            return@LaunchedEffect
        }

        statusText = BENCHMARK_RUNNING_TEXT
        workload.updates.forEach { snapshot ->
            items = snapshot
            delay(scenario.profile.stepDelayMillis)
        }
        delay(scenario.profile.settleDelayMillis)
        statusText = BENCHMARK_DONE_TEXT
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
    ) {
        if (isRunning) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        } else {
            TextButton(
                onClick = { isRunning = true },
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text(
                    text = BENCHMARK_READY_TEXT,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
        BenchmarkScenarioList(
            implementation = scenario.implementation,
            profile = scenario.profile,
            items = items,
        )
    }
}

@Composable
private fun BenchmarkScenarioList(
    implementation: BenchmarkImplementation,
    profile: BenchmarkProfile,
    items: List<BenchmarkRowUi>,
) {
    when (implementation) {
        BenchmarkImplementation.AnimatedColumn -> {
            AnimatedColumn(
                items = items,
                key = { it.id },
                transitionSpec = animatedColumnSpec(profile),
                modifier = Modifier.fillMaxWidth(),
            ) { row ->
                BenchmarkScenarioRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    row = row,
                )
            }
        }

        BenchmarkImplementation.DefaultColumn -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = tween(profile.durationMillis),
                    ),
            ) {
                items.forEach { row ->
                    key(row.id) {
                        BenchmarkScenarioRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            row = row,
                        )
                    }
                }
            }
        }

        BenchmarkImplementation.LazyColumn -> {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(
                    items = items,
                    key = { it.id },
                ) { row ->
                    BenchmarkScenarioRow(
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = tween(profile.durationMillis),
                                placementSpec = tween(profile.durationMillis),
                                fadeOutSpec = tween(profile.durationMillis),
                            )
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        row = row,
                    )
                }
            }
        }
    }
}

@Composable
private fun BenchmarkScenarioRow(
    modifier: Modifier,
    row: BenchmarkRowUi,
) {
    val shade = 0.18f + (row.version % 5) * 0.07f
    Box(
        modifier = modifier
            .height(44.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = shade),
                shape = RoundedCornerShape(12.dp),
            ),
    )
}

private fun animatedColumnSpec(profile: BenchmarkProfile): AnimatedItemTransitionSpec = AnimatedItemDefaults.fadeSlide(
    placement = PlacementBehavior.Animated(profile.durationMillis),
).copy(
    enter = EnterSpec.FadeAndSlide(durationMillis = profile.durationMillis),
    exit = ExitSpec.FadeAndSlide(durationMillis = profile.durationMillis),
)

private fun BenchmarkScenario.buildWorkload(): BenchmarkWorkloadData = when (workload) {
    BenchmarkWorkload.ReinsertBurst -> reinsertBurstWorkload(
        initialSize = 64,
        steps = 24,
        hotSetSize = 8,
        seed = 9_001,
    )

    BenchmarkWorkload.WindowedChurn -> windowedChurnWorkload(
        initialSize = 64,
        steps = 24,
    )
}

private fun windowedChurnWorkload(
    initialSize: Int,
    steps: Int,
): BenchmarkWorkloadData {
    val initialItems = List(initialSize) { index -> BenchmarkRowUi(id = index, version = 0) }
    val current = initialItems.toMutableList()
    var nextId = initialSize
    val updates = ArrayList<List<BenchmarkRowUi>>(steps)

    repeat(steps) { step ->
        current.removeAt(0)
        current.add(BenchmarkRowUi(id = nextId++, version = 0))

        if (step % 4 == 0 && current.isNotEmpty()) {
            val middle = current.lastIndex / 2
            val row = current[middle]
            current[middle] = row.copy(version = row.version + 1)
        }

        updates += current.toList()
    }

    return BenchmarkWorkloadData(
        initialItems = initialItems,
        updates = updates,
    )
}

private fun reinsertBurstWorkload(
    initialSize: Int,
    steps: Int,
    hotSetSize: Int,
    seed: Int,
): BenchmarkWorkloadData {
    val initialItems = List(initialSize) { index -> BenchmarkRowUi(id = index, version = 0) }
    val current = initialItems.toMutableList()
    val random = Random(seed)
    val inactiveHot = linkedMapOf<Int, BenchmarkRowUi>()
    val hotIds = initialItems.take(hotSetSize).map { it.id }.toSet()
    val updates = ArrayList<List<BenchmarkRowUi>>(steps)

    repeat(steps) { step ->
        val activeHotIndices = current.indices.filter { current[it].id in hotIds }
        when {
            step % 3 == 0 && activeHotIndices.isNotEmpty() -> {
                val index = activeHotIndices.random(random)
                val row = current.removeAt(index)
                inactiveHot[row.id] = row
            }

            inactiveHot.isNotEmpty() -> {
                val reinsertId = inactiveHot.keys.random(random)
                val row = inactiveHot.remove(reinsertId) ?: error("Missing hot row $reinsertId")
                current.add(
                    index = randomInsertionIndex(random, current.size),
                    element = row.copy(version = row.version + 1),
                )
            }
        }

        repeat(2) {
            if (current.isNotEmpty()) {
                val index = random.nextInt(current.size)
                val row = current[index]
                current[index] = row.copy(version = row.version + 1)
            }
        }

        if (current.size >= 2) {
            val from = random.nextInt(current.size)
            val row = current.removeAt(from)
            current.add(randomInsertionIndex(random, current.size), row)
        }

        updates += current.toList()
    }

    return BenchmarkWorkloadData(
        initialItems = initialItems,
        updates = updates,
    )
}

private fun randomInsertionIndex(
    random: Random,
    currentSize: Int,
): Int = if (currentSize == 0) {
    0
} else {
    random.nextInt(currentSize + 1)
}

internal fun Intent.toBenchmarkScenarioOrNull(): BenchmarkScenario? {
    if (!hasExtra(EXTRA_IMPLEMENTATION) || !hasExtra(EXTRA_WORKLOAD) || !hasExtra(EXTRA_PROFILE)) {
        return null
    }
    return BenchmarkScenario(
    implementation = BenchmarkImplementation.fromWireValue(getStringExtra(EXTRA_IMPLEMENTATION)),
    workload = BenchmarkWorkload.fromWireValue(getStringExtra(EXTRA_WORKLOAD)),
    profile = BenchmarkProfile.fromWireValue(getStringExtra(EXTRA_PROFILE)),
)
}

fun benchmarkScenarioIntent(
    context: Context,
    implementation: String,
    workload: String,
    profile: String,
): Intent = Intent(context, BenchmarkScenarioActivity::class.java).apply {
    putExtra(EXTRA_IMPLEMENTATION, implementation)
    putExtra(EXTRA_WORKLOAD, workload)
    putExtra(EXTRA_PROFILE, profile)
}

internal const val BENCHMARK_DONE_TEXT = "Benchmark complete"
internal const val BENCHMARK_READY_TEXT = "Benchmark ready"
internal const val BENCHMARK_RUNNING_TEXT = "Benchmark running"

internal const val EXTRA_IMPLEMENTATION = "implementation"
internal const val EXTRA_WORKLOAD = "workload"
internal const val EXTRA_PROFILE = "profile"
