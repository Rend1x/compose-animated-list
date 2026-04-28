package com.rend1x.composeanimatedlist.core

import kotlin.math.ceil
import kotlin.random.Random

private data class BenchmarkRow(
    val id: Int,
    val version: Int,
)

private data class BenchmarkWorkload(
    val name: String,
    val initialItems: List<BenchmarkRow>,
    val updates: List<List<BenchmarkRow>>,
)

private data class BenchmarkConfig(
    val profileName: String,
    val warmupRuns: Int,
    val measuredRuns: Int,
    val exitRetentionUpdates: Int,
    val workloads: List<BenchmarkWorkload>,
)

private data class BenchmarkStats(
    val medianNsPerUpdate: Double,
    val p90NsPerUpdate: Double,
    val sink: Int,
)

private data class WorkloadMetrics(
    val totalInserted: Int,
    val totalRemoved: Int,
    val totalPositionChanged: Int,
    val totalValueChanged: Int,
    val minInputSize: Int,
    val maxInputSize: Int,
    val avgTouchedKeysPerUpdate: Double,
)

private data class AnimationValueMetrics(
    val totalExitEvents: Int,
    val updatesWithExitingRows: Int,
    val totalExitingRowsObserved: Int,
    val maxExitingRowsObserved: Int,
    val reinsertsRecoveredWithoutReenter: Int,
    val avgExtraRenderRows: Double,
    val maxExtraRenderRows: Int,
    val avgRenderAmplification: Double,
    val maxRenderAmplification: Double,
)

private enum class BenchmarkProfile {
    Smoke,
    Full,
}

private const val KEY_POLICY = "Strict"

fun main(args: Array<String>) {
    val config = benchmarkConfig(parseProfile(args))

    println("compose-animated-list core benchmarks")
    println("Profile: ${config.profileName}")
    println("Warmup runs: ${config.warmupRuns}, measured runs: ${config.measuredRuns}")
    println("Measured scope: JVM diff/render cost only. Compose frame timing is not included.")
    println("Animation-value simulation: exiting rows stay alive for ${config.exitRetentionUpdates} logical updates.")
    println()

    config.workloads.forEach { workload ->
        val workloadMetrics = analyzeWorkload(workload)
        val animationValueMetrics = analyzeAnimationValue(workload, config.exitRetentionUpdates)
        val rebuildStats = runBenchmark(config, workload, ::rebuildPresentSnapshot)
        val diffStats = runBenchmark(config, workload, ::diffWithImmediateExitClear)
        val engineStats = runBenchmark(config, workload, ::engineUpdateWithImmediateExitClear)

        println("${workload.name}  initial=${workload.initialItems.size}, updates=${workload.updates.size}, keyPolicy=$KEY_POLICY")
        println(
            "Input churn: inserts=${workloadMetrics.totalInserted}, removes=${workloadMetrics.totalRemoved}, " +
                "positionChanges=${workloadMetrics.totalPositionChanged}, valueChanges=${workloadMetrics.totalValueChanged}, " +
                "sizeRange=${workloadMetrics.minInputSize}..${workloadMetrics.maxInputSize}, " +
                "avgTouchedKeys/update=${formatDouble(workloadMetrics.avgTouchedKeysPerUpdate)}"
        )
        println(
            "Animation value: exitEvents=${animationValueMetrics.totalExitEvents}, " +
                "reinsertRecoveries=${animationValueMetrics.reinsertsRecoveredWithoutReenter}, " +
                "updatesWithExits=${animationValueMetrics.updatesWithExitingRows}/${workload.updates.size}, " +
                "avgExitingRows/update=${formatDouble(animationValueMetrics.totalExitingRowsObserved.toDouble() / workload.updates.size)}, " +
                "maxExitingRows=${animationValueMetrics.maxExitingRowsObserved}"
        )
        println(
            "Retention overhead: avgExtraRows=${formatDouble(animationValueMetrics.avgExtraRenderRows)}, " +
                "maxExtraRows=${animationValueMetrics.maxExtraRenderRows}, " +
                "avgAmplification=${formatRatio(animationValueMetrics.avgRenderAmplification)}, " +
                "maxAmplification=${formatRatio(animationValueMetrics.maxRenderAmplification)}"
        )
        println(
            headerRow(
                "Benchmark",
                "median ns/update",
                "p90 ns/update",
                "ratio vs rebuild",
            )
        )
        println(resultRow("rebuild-present", rebuildStats, rebuildStats.medianNsPerUpdate))
        println(resultRow("diff+clear", diffStats, rebuildStats.medianNsPerUpdate))
        println(resultRow("engine.update+clear", engineStats, rebuildStats.medianNsPerUpdate))
        println("sink=${rebuildStats.sink xor diffStats.sink xor engineStats.sink}")
        println()
    }
}

private fun parseProfile(args: Array<String>): BenchmarkProfile {
    val raw = args.firstOrNull { it.startsWith("--profile=") }
        ?.substringAfter('=')
        ?.trim()
        ?.lowercase()

    return when (raw) {
        null, "", "smoke" -> BenchmarkProfile.Smoke
        "full" -> BenchmarkProfile.Full
        else -> error("Unknown profile \"$raw\". Use --profile=smoke or --profile=full.")
    }
}

private fun benchmarkConfig(profile: BenchmarkProfile): BenchmarkConfig = when (profile) {
    BenchmarkProfile.Smoke -> BenchmarkConfig(
        profileName = "smoke",
        warmupRuns = 3,
        measuredRuns = 7,
        exitRetentionUpdates = 3,
        workloads = listOf(
            windowedChurnWorkload(initialSize = 128, steps = 240),
            randomChurnWorkload(initialSize = 256, steps = 260, seed = 1_337),
            reinsertBurstWorkload(initialSize = 192, steps = 240, hotSetSize = 12, seed = 9_001),
            valueUpdateWorkload(initialSize = 512, steps = 220, updatesPerStep = 24, seed = 42),
        ),
    )

    BenchmarkProfile.Full -> BenchmarkConfig(
        profileName = "full",
        warmupRuns = 5,
        measuredRuns = 12,
        exitRetentionUpdates = 4,
        workloads = listOf(
            windowedChurnWorkload(initialSize = 256, steps = 400),
            windowedChurnWorkload(initialSize = 2_048, steps = 500),
            randomChurnWorkload(initialSize = 256, steps = 420, seed = 1_337),
            randomChurnWorkload(initialSize = 2_048, steps = 500, seed = 7_777),
            reinsertBurstWorkload(initialSize = 384, steps = 420, hotSetSize = 24, seed = 9_001),
            valueUpdateWorkload(initialSize = 512, steps = 360, updatesPerStep = 32, seed = 42),
            valueUpdateWorkload(initialSize = 4_096, steps = 420, updatesPerStep = 64, seed = 99),
        ),
    )
}

private fun windowedChurnWorkload(
    initialSize: Int,
    steps: Int,
): BenchmarkWorkload {
    val initialItems = List(initialSize) { index -> BenchmarkRow(id = index, version = 0) }
    val current = initialItems.toMutableList()
    var nextId = initialSize
    val updates = ArrayList<List<BenchmarkRow>>(steps)

    repeat(steps) { step ->
        current.removeAt(0)
        current.add(BenchmarkRow(id = nextId++, version = 0))

        if (step % 5 == 0 && current.isNotEmpty()) {
            val middle = current.lastIndex / 2
            val row = current[middle]
            current[middle] = row.copy(version = row.version + 1)
        }

        updates += current.toList()
    }

    return BenchmarkWorkload(
        name = "windowed-churn/$initialSize",
        initialItems = initialItems,
        updates = updates,
    )
}

private fun randomChurnWorkload(
    initialSize: Int,
    steps: Int,
    seed: Int,
): BenchmarkWorkload {
    val initialItems = List(initialSize) { index -> BenchmarkRow(id = index, version = 0) }
    val random = Random(seed)
    val current = initialItems.toMutableList()
    val minSize = initialSize / 2
    val maxSize = initialSize + initialSize / 2
    var nextId = initialSize
    val updates = ArrayList<List<BenchmarkRow>>(steps)

    repeat(steps) {
        repeat(3) {
            when (random.nextInt(4)) {
                0 -> if (current.size < maxSize) {
                    current.add(
                        index = randomInsertionIndex(random, current.size),
                        element = BenchmarkRow(id = nextId++, version = 0),
                    )
                }

                1 -> if (current.size > minSize) {
                    current.removeAt(random.nextInt(current.size))
                }

                2 -> if (current.isNotEmpty()) {
                    val index = random.nextInt(current.size)
                    val row = current[index]
                    current[index] = row.copy(version = row.version + 1)
                }

                else -> if (current.size >= 2) {
                    val from = random.nextInt(current.size)
                    val row = current.removeAt(from)
                    current.add(randomInsertionIndex(random, current.size), row)
                }
            }
        }

        updates += current.toList()
    }

    return BenchmarkWorkload(
        name = "random-churn/$initialSize",
        initialItems = initialItems,
        updates = updates,
    )
}

private fun reinsertBurstWorkload(
    initialSize: Int,
    steps: Int,
    hotSetSize: Int,
    seed: Int,
): BenchmarkWorkload {
    val initialItems = List(initialSize) { index -> BenchmarkRow(id = index, version = 0) }
    val current = initialItems.toMutableList()
    val random = Random(seed)
    val inactiveHot = linkedMapOf<Int, BenchmarkRow>()
    val hotIds = initialItems.take(hotSetSize).map { it.id }.toSet()
    val updates = ArrayList<List<BenchmarkRow>>(steps)

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

    return BenchmarkWorkload(
        name = "reinsert-burst/$initialSize",
        initialItems = initialItems,
        updates = updates,
    )
}

private fun valueUpdateWorkload(
    initialSize: Int,
    steps: Int,
    updatesPerStep: Int,
    seed: Int,
): BenchmarkWorkload {
    val initialItems = List(initialSize) { index -> BenchmarkRow(id = index, version = 0) }
    val random = Random(seed)
    val current = initialItems.toMutableList()
    val updates = ArrayList<List<BenchmarkRow>>(steps)

    repeat(steps) {
        repeat(updatesPerStep) {
            val index = random.nextInt(current.size)
            val row = current[index]
            current[index] = row.copy(version = row.version + 1)
        }

        updates += current.toList()
    }

    return BenchmarkWorkload(
        name = "value-updates/$initialSize",
        initialItems = initialItems,
        updates = updates,
    )
}

private fun analyzeWorkload(workload: BenchmarkWorkload): WorkloadMetrics {
    var previous = workload.initialItems
    var totalInserted = 0
    var totalRemoved = 0
    var totalPositionChanged = 0
    var totalValueChanged = 0
    var minInputSize = workload.initialItems.size
    var maxInputSize = workload.initialItems.size
    var totalTouched = 0

    workload.updates.forEach { next ->
        val previousIndexById = previous.mapIndexed { index, row -> row.id to index }.toMap()
        val nextIndexById = next.mapIndexed { index, row -> row.id to index }.toMap()
        val previousById = previous.associateBy { it.id }
        val nextById = next.associateBy { it.id }

        val removed = previousById.keys - nextById.keys
        val inserted = nextById.keys - previousById.keys
        val retained = previousById.keys intersect nextById.keys
        val positionChanged = retained.count { id -> previousIndexById[id] != nextIndexById[id] }
        val valueChanged = retained.count { id -> previousById[id] != nextById[id] }

        totalInserted += inserted.size
        totalRemoved += removed.size
        totalPositionChanged += positionChanged
        totalValueChanged += valueChanged
        totalTouched += inserted.size + removed.size + positionChanged + valueChanged
        minInputSize = minOf(minInputSize, next.size)
        maxInputSize = maxOf(maxInputSize, next.size)
        previous = next
    }

    return WorkloadMetrics(
        totalInserted = totalInserted,
        totalRemoved = totalRemoved,
        totalPositionChanged = totalPositionChanged,
        totalValueChanged = totalValueChanged,
        minInputSize = minInputSize,
        maxInputSize = maxInputSize,
        avgTouchedKeysPerUpdate = totalTouched.toDouble() / workload.updates.size,
    )
}

private fun analyzeAnimationValue(
    workload: BenchmarkWorkload,
    exitRetentionUpdates: Int,
): AnimationValueMetrics {
    val engine = AnimatedListRenderEngine(
        initialItems = workload.initialItems,
        keySelector = BenchmarkRow::id,
        keyPolicy = AnimatedListKeyPolicy.Strict,
    )
    val exitRetention = linkedMapOf<Int, Int>()

    var totalExitEvents = 0
    var updatesWithExitingRows = 0
    var totalExitingRowsObserved = 0
    var maxExitingRowsObserved = 0
    var reinsertsRecoveredWithoutReenter = 0
    var totalExtraRows = 0.0
    var maxExtraRows = 0
    var totalAmplification = 0.0
    var maxAmplification = 1.0

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

        engine.update(nextItems, BenchmarkRow::id)

        val renderByKey = engine.items.associateBy { it.key as Int }
        val exitingAfter = engine.items
            .filter { it.presence == PresenceState.Exiting }
            .mapTo(linkedSetOf()) { it.key as Int }

        val newlyExiting = exitingAfter - exitingBefore
        newlyExiting.forEach { key ->
            exitRetention[key] = exitRetentionUpdates
        }

        val recovered = exitingBefore.count { key ->
            val item = renderByKey[key]
            item != null && item.presence == PresenceState.Present
        }

        val exitingCount = exitingAfter.size
        val extraRows = (engine.items.size - nextItems.size).coerceAtLeast(0)
        val amplification = engine.items.size.toDouble() / maxOf(nextItems.size, 1)

        totalExitEvents += newlyExiting.size
        reinsertsRecoveredWithoutReenter += recovered
        totalExitingRowsObserved += exitingCount
        maxExitingRowsObserved = maxOf(maxExitingRowsObserved, exitingCount)
        if (exitingCount > 0) {
            updatesWithExitingRows++
        }
        totalExtraRows += extraRows
        maxExtraRows = maxOf(maxExtraRows, extraRows)
        totalAmplification += amplification
        maxAmplification = maxOf(maxAmplification, amplification)
    }

    return AnimationValueMetrics(
        totalExitEvents = totalExitEvents,
        updatesWithExitingRows = updatesWithExitingRows,
        totalExitingRowsObserved = totalExitingRowsObserved,
        maxExitingRowsObserved = maxExitingRowsObserved,
        reinsertsRecoveredWithoutReenter = reinsertsRecoveredWithoutReenter,
        avgExtraRenderRows = totalExtraRows / workload.updates.size,
        maxExtraRenderRows = maxExtraRows,
        avgRenderAmplification = totalAmplification / workload.updates.size,
        maxRenderAmplification = maxAmplification,
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

private fun runBenchmark(
    config: BenchmarkConfig,
    workload: BenchmarkWorkload,
    replay: (BenchmarkWorkload) -> Int,
): BenchmarkStats {
    val samples = ArrayList<Double>(config.measuredRuns)
    var sink = 0

    repeat(config.warmupRuns + config.measuredRuns) { run ->
        val startedAt = System.nanoTime()
        sink = sink xor replay(workload)
        val elapsed = System.nanoTime() - startedAt
        if (run >= config.warmupRuns) {
            samples += elapsed.toDouble() / workload.updates.size.toDouble()
        }
    }

    val sorted = samples.sorted()
    return BenchmarkStats(
        medianNsPerUpdate = percentile(sorted, 0.5),
        p90NsPerUpdate = percentile(sorted, 0.9),
        sink = sink,
    )
}

private fun rebuildPresentSnapshot(workload: BenchmarkWorkload): Int {
    var render = workload.initialItems.map(::presentItem)
    var sink = render.size

    workload.updates.forEach { items ->
        render = items.map(::presentItem)
        sink = mixSink(sink, render)
    }

    return sink
}

private fun diffWithImmediateExitClear(workload: BenchmarkWorkload): Int {
    var render = workload.initialItems.map(::presentItem)
    var sink = render.size

    workload.updates.forEach { items ->
        render = AnimatedListDiffer.diff(
            current = render,
            newItems = items,
            keySelector = BenchmarkRow::id,
            keyPolicy = AnimatedListKeyPolicy.Strict,
        ).filterNot { it.presence == PresenceState.Exiting }
        sink = mixSink(sink, render)
    }

    return sink
}

private fun engineUpdateWithImmediateExitClear(workload: BenchmarkWorkload): Int {
    val engine = AnimatedListRenderEngine(
        initialItems = workload.initialItems,
        keySelector = BenchmarkRow::id,
        keyPolicy = AnimatedListKeyPolicy.Strict,
    )
    var sink = engine.items.size

    workload.updates.forEach { items ->
        engine.update(items, BenchmarkRow::id)
        engine.clearExitingNow()
        sink = mixSink(sink, engine.items)
    }

    return sink
}

private fun presentItem(row: BenchmarkRow): AnimatedListItem<BenchmarkRow> = AnimatedListItem(
    key = row.id,
    value = row,
    presence = PresenceState.Present,
)

private fun mixSink(
    seed: Int,
    render: List<AnimatedListItem<BenchmarkRow>>,
): Int {
    val first = render.firstOrNull()?.value?.version ?: -1
    val last = render.lastOrNull()?.value?.version ?: -1
    return ((seed * 31) + render.size) * 31 + first * 17 + last
}

private fun percentile(
    sortedValues: List<Double>,
    fraction: Double,
): Double {
    if (sortedValues.isEmpty()) return 0.0
    val index = ceil((sortedValues.size - 1) * fraction).toInt()
    return sortedValues[index.coerceIn(sortedValues.indices)]
}

private fun headerRow(
    benchmark: String,
    median: String,
    p90: String,
    ratio: String,
): String = buildString {
    append(benchmark.padEnd(22))
    append(median.padStart(18))
    append(p90.padStart(18))
    append(ratio.padStart(18))
}

private fun resultRow(
    label: String,
    stats: BenchmarkStats,
    baselineMedian: Double,
): String = buildString {
    append(label.padEnd(22))
    append(formatNs(stats.medianNsPerUpdate).padStart(18))
    append(formatNs(stats.p90NsPerUpdate).padStart(18))
    append(formatRatio(stats.medianNsPerUpdate / baselineMedian).padStart(18))
}

private fun formatNs(value: Double): String = "%,.1f".format(value)

private fun formatRatio(value: Double): String = "%.2fx".format(value)

private fun formatDouble(value: Double): String = "%,.2f".format(value)
