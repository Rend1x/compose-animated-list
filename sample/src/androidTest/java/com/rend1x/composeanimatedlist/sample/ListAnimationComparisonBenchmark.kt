package com.rend1x.composeanimatedlist.sample

import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rend1x.composeanimatedlist.AnimatedColumn
import com.rend1x.composeanimatedlist.animatedItem
import com.rend1x.composeanimatedlist.animation.AnimatedItemDefaults
import com.rend1x.composeanimatedlist.animation.AnimatedItemTransitionSpec
import com.rend1x.composeanimatedlist.animation.EnterSpec
import com.rend1x.composeanimatedlist.animation.ExitSpec
import com.rend1x.composeanimatedlist.animation.PlacementBehavior
import com.rend1x.composeanimatedlist.core.AnimatedListKeyPolicy
import com.rend1x.composeanimatedlist.core.AnimatedListRenderEngine
import com.rend1x.composeanimatedlist.core.PresenceState
import kotlin.math.ceil
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListAnimationComparisonBenchmark {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun compareWindowedChurn() {
        val workload = windowedChurnWorkload(initialSize = 64, steps = 60)
        AnimationProfile.entries.forEach { profile ->
            logScenarioResults(
                scenarioName = "windowed-churn",
                profile = profile,
                results = measureAllImplementations(workload, profile),
            )
        }
    }

    @Test
    fun compareReinsertBurst() {
        val workload = reinsertBurstWorkload(
            initialSize = 64,
            steps = 60,
            hotSetSize = 8,
            seed = 9_001,
        )
        AnimationProfile.entries.forEach { profile ->
            logScenarioResults(
                scenarioName = "reinsert-burst",
                profile = profile,
                results = measureAllImplementations(workload, profile),
            )
        }
    }

    private fun measureAllImplementations(
        workload: UiWorkload,
        profile: AnimationProfile,
    ): List<UiScenarioResult> = listOf(
        measureScenario(ComparisonImplementation.AnimatedColumn, profile, workload),
        measureScenario(ComparisonImplementation.DefaultColumn, profile, workload),
        measureScenario(ComparisonImplementation.LazyColumn, profile, workload),
    )

    private fun measureScenario(
        implementation: ComparisonImplementation,
        profile: AnimationProfile,
        workload: UiWorkload,
        warmupRuns: Int = 1,
        measuredRuns: Int = 3,
    ): UiScenarioResult {
        val samples = ArrayList<Double>(measuredRuns)
        val semanticMetrics = semanticMetricsFor(implementation, workload)

        repeat(warmupRuns + measuredRuns) { run ->
            lateinit var controller: WorkloadController

            composeRule.activity.runOnUiThread {
                controller = WorkloadController(workload)
                composeRule.activity.setContent {
                    MaterialTheme {
                        Surface(color = Color(0xFFF5F5F5)) {
                            ComparisonHost(
                                implementation = implementation,
                                profile = profile,
                                items = controller.items,
                            )
                        }
                    }
                }
            }
            composeRule.waitForIdle()

            val updateDurations = ArrayList<Double>(workload.updates.size)
            workload.updates.forEach { snapshot ->
                val startedAt = System.nanoTime()
                composeRule.activity.runOnUiThread {
                    controller.items = snapshot
                }
                composeRule.waitForIdle()
                updateDurations += (System.nanoTime() - startedAt).toDouble()
            }

            composeRule.activity.runOnUiThread {
                composeRule.activity.setContent {
                    Surface(color = Color.Transparent) {}
                }
            }
            composeRule.waitForIdle()

            if (run >= warmupRuns) {
                samples += updateDurations.average()
            }
        }

        val sorted = samples.sorted()
        return UiScenarioResult(
            implementation = implementation,
            profile = profile,
            medianNsPerUpdate = percentile(sorted, 0.5),
            p90NsPerUpdate = percentile(sorted, 0.9),
            semanticMetrics = semanticMetrics,
        )
    }

    private fun logScenarioResults(
        scenarioName: String,
        profile: AnimationProfile,
        results: List<UiScenarioResult>,
    ) {
        val baseline = results.first { it.implementation == ComparisonImplementation.AnimatedColumn }
        Log.i(LOG_TAG, "Scenario=$scenarioName profile=${profile.label}")
        results.forEach { result ->
            Log.i(
                LOG_TAG,
                buildString {
                    append(result.implementation.label)
                    append(" medianNsPerUpdate=")
                    append(formatNs(result.medianNsPerUpdate))
                    append(" p90NsPerUpdate=")
                    append(formatNs(result.p90NsPerUpdate))
                    append(" ratioVsAnimatedColumn=")
                    append(formatRatio(result.medianNsPerUpdate / baseline.medianNsPerUpdate))
                    append(" semanticMode=")
                    append(result.semanticMetrics.mode)
                    append(" reinsertsRecovered=")
                    append(result.semanticMetrics.reinsertsRecovered)
                    append(" updatesWithExits=")
                    append(result.semanticMetrics.updatesWithExits)
                    append(" avgExtraRows=")
                    append(formatDouble(result.semanticMetrics.avgExtraRows))
                    append(" maxExtraRows=")
                    append(result.semanticMetrics.maxExtraRows)
                    append(" avgAmplification=")
                    append(formatRatio(result.semanticMetrics.avgAmplification))
                },
            )
        }
    }
}

private data class UiRow(
    val id: Int,
    val version: Int,
)

private data class UiWorkload(
    val initialItems: List<UiRow>,
    val updates: List<List<UiRow>>,
)

private data class UiScenarioResult(
    val implementation: ComparisonImplementation,
    val profile: AnimationProfile,
    val medianNsPerUpdate: Double,
    val p90NsPerUpdate: Double,
    val semanticMetrics: UiSemanticMetrics,
)

private data class UiSemanticMetrics(
    val mode: String,
    val reinsertsRecovered: Int,
    val updatesWithExits: Int,
    val avgExtraRows: Double,
    val maxExtraRows: Int,
    val avgAmplification: Double,
)

private class WorkloadController(
    workload: UiWorkload,
) {
    var items by mutableStateOf(workload.initialItems)
}

private enum class AnimationProfile(
    val label: String,
) {
    Default("default"),
    Fast("fast"),
}

private enum class ComparisonImplementation(
    val label: String,
) {
    AnimatedColumn("AnimatedColumn"),
    DefaultColumn("Column+animateContentSize"),
    LazyColumn("LazyColumn+animateItem"),
}

@Composable
private fun ComparisonHost(
    implementation: ComparisonImplementation,
    profile: AnimationProfile,
    items: List<UiRow>,
) {
    when (implementation) {
        ComparisonImplementation.AnimatedColumn -> {
            when (profile) {
                AnimationProfile.Default -> {
                    AnimatedColumn(
                        items = items,
                        key = { it.id },
                        transitionSpec = AnimatedItemDefaults.none(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { row ->
                        BenchmarkRowCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .animatedItem(this),
                            row = row,
                        )
                    }
                }

                AnimationProfile.Fast -> {
                    AnimatedColumn(
                        items = items,
                        key = { it.id },
                        transitionSpec = FAST_ANIMATED_COLUMN_SPEC,
                        modifier = Modifier.fillMaxWidth(),
                    ) { row ->
                        BenchmarkRowCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            row = row,
                        )
                    }
                }
            }
        }

        ComparisonImplementation.DefaultColumn -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = when (profile) {
                            AnimationProfile.Default -> tween()
                            AnimationProfile.Fast -> tween(FAST_DURATION_MS)
                        },
                    ),
            ) {
                items.forEach { row ->
                    key(row.id) {
                        BenchmarkRowCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            row = row,
                        )
                    }
                }
            }
        }

        ComparisonImplementation.LazyColumn -> {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(
                    items = items,
                    key = { it.id },
                ) { row ->
                    BenchmarkRowCard(
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = when (profile) {
                                    AnimationProfile.Default -> tween()
                                    AnimationProfile.Fast -> tween(FAST_DURATION_MS)
                                },
                                placementSpec = when (profile) {
                                    AnimationProfile.Default -> tween()
                                    AnimationProfile.Fast -> tween(FAST_DURATION_MS)
                                },
                                fadeOutSpec = when (profile) {
                                    AnimationProfile.Default -> tween()
                                    AnimationProfile.Fast -> tween(FAST_DURATION_MS)
                                },
                            )
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        row = row,
                    )
                }
            }
        }
    }
}

@Composable
private fun BenchmarkRowCard(
    modifier: Modifier,
    row: UiRow,
) {
    val shade = 0.15f + (row.version % 5) * 0.08f
    Box(
        modifier = modifier
            .height(44.dp)
            .background(
                color = MaterialTheme.colors.primary.copy(alpha = shade),
                shape = RoundedCornerShape(12.dp),
            ),
    )
}

private fun windowedChurnWorkload(
    initialSize: Int,
    steps: Int,
): UiWorkload {
    val initialItems = List(initialSize) { index -> UiRow(id = index, version = 0) }
    val current = initialItems.toMutableList()
    var nextId = initialSize
    val updates = ArrayList<List<UiRow>>(steps)

    repeat(steps) { step ->
        current.removeAt(0)
        current.add(UiRow(id = nextId++, version = 0))

        if (step % 5 == 0 && current.isNotEmpty()) {
            val middle = current.lastIndex / 2
            val row = current[middle]
            current[middle] = row.copy(version = row.version + 1)
        }

        updates += current.toList()
    }

    return UiWorkload(
        initialItems = initialItems,
        updates = updates,
    )
}

private fun reinsertBurstWorkload(
    initialSize: Int,
    steps: Int,
    hotSetSize: Int,
    seed: Int,
): UiWorkload {
    val initialItems = List(initialSize) { index -> UiRow(id = index, version = 0) }
    val current = initialItems.toMutableList()
    val random = Random(seed)
    val inactiveHot = linkedMapOf<Int, UiRow>()
    val hotIds = initialItems.take(hotSetSize).map { it.id }.toSet()
    val updates = ArrayList<List<UiRow>>(steps)

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

    return UiWorkload(
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

private fun percentile(
    sortedValues: List<Double>,
    fraction: Double,
): Double {
    if (sortedValues.isEmpty()) return 0.0
    val index = ceil((sortedValues.size - 1) * fraction).toInt()
    return sortedValues[index.coerceIn(sortedValues.indices)]
}

private fun formatNs(value: Double): String = "%,.1f".format(value)

private fun formatRatio(value: Double): String = "%.2fx".format(value)

private fun formatDouble(value: Double): String = "%,.2f".format(value)

private fun semanticMetricsFor(
    implementation: ComparisonImplementation,
    workload: UiWorkload,
): UiSemanticMetrics = when (implementation) {
    ComparisonImplementation.AnimatedColumn -> retainedExitMetrics(workload, exitRetentionUpdates = 3)
    ComparisonImplementation.DefaultColumn,
    ComparisonImplementation.LazyColumn,
    -> UiSemanticMetrics(
        mode = "present-only",
        reinsertsRecovered = 0,
        updatesWithExits = 0,
        avgExtraRows = 0.0,
        maxExtraRows = 0,
        avgAmplification = 1.0,
    )
}

private fun retainedExitMetrics(
    workload: UiWorkload,
    exitRetentionUpdates: Int,
): UiSemanticMetrics {
    val engine = AnimatedListRenderEngine(
        initialItems = workload.initialItems,
        keySelector = UiRow::id,
        keyPolicy = AnimatedListKeyPolicy.Strict,
    )
    val exitRetention = linkedMapOf<Int, Int>()
    var reinsertsRecovered = 0
    var updatesWithExits = 0
    var totalExtraRows = 0.0
    var maxExtraRows = 0
    var totalAmplification = 0.0

    workload.updates.forEach { nextItems ->
        val finishingKeys = exitRetention
            .filterValues { remaining -> remaining <= 0 }
            .keys
            .toList()
        finishingKeys.forEach { key ->
            engine.onExitAnimationFinished(key)
            exitRetention.remove(key)
        }
        exitRetention.replaceAll { _, remaining -> remaining - 1 }

        val exitingBefore = engine.items
            .filter { it.presence == PresenceState.Exiting }
            .mapTo(linkedSetOf()) { it.key as Int }

        engine.update(nextItems, UiRow::id)

        val renderByKey = engine.items.associateBy { it.key as Int }
        val exitingAfter = engine.items
            .filter { it.presence == PresenceState.Exiting }
            .mapTo(linkedSetOf()) { it.key as Int }

        val newlyExiting = exitingAfter - exitingBefore
        newlyExiting.forEach { key ->
            exitRetention[key] = exitRetentionUpdates
        }

        reinsertsRecovered += exitingBefore.count { key ->
            renderByKey[key]?.presence == PresenceState.Present
        }

        if (exitingAfter.isNotEmpty()) {
            updatesWithExits++
        }
        val extraRows = (engine.items.size - nextItems.size).coerceAtLeast(0)
        totalExtraRows += extraRows
        maxExtraRows = maxOf(maxExtraRows, extraRows)
        totalAmplification += engine.items.size.toDouble() / maxOf(nextItems.size, 1)
    }

    return UiSemanticMetrics(
        mode = "retained-exits",
        reinsertsRecovered = reinsertsRecovered,
        updatesWithExits = updatesWithExits,
        avgExtraRows = totalExtraRows / workload.updates.size,
        maxExtraRows = maxExtraRows,
        avgAmplification = totalAmplification / workload.updates.size,
    )
}

private val FAST_ANIMATED_COLUMN_SPEC = AnimatedItemTransitionSpec(
    enter = EnterSpec.FadeAndSlide(durationMillis = FAST_DURATION_MS),
    exit = ExitSpec.FadeAndSlide(durationMillis = FAST_DURATION_MS),
    placement = PlacementBehavior.Animated(durationMillis = FAST_DURATION_MS),
)

private const val FAST_DURATION_MS = 32

private const val LOG_TAG = "ListAnimCompare"
