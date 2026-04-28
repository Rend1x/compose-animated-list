package com.rend1x.composeanimatedlist.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListAnimationMacrobenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun animated_reinsert_default() = benchmark(
        implementation = "animated",
        workload = "reinsert",
        profile = "default",
    )

    @Test
    fun column_reinsert_default() = benchmark(
        implementation = "column",
        workload = "reinsert",
        profile = "default",
    )

    @Test
    fun lazy_reinsert_default() = benchmark(
        implementation = "lazy",
        workload = "reinsert",
        profile = "default",
    )

    @Test
    fun animated_reinsert_fast() = benchmark(
        implementation = "animated",
        workload = "reinsert",
        profile = "fast",
    )

    @Test
    fun column_reinsert_fast() = benchmark(
        implementation = "column",
        workload = "reinsert",
        profile = "fast",
    )

    @Test
    fun lazy_reinsert_fast() = benchmark(
        implementation = "lazy",
        workload = "reinsert",
        profile = "fast",
    )

    @Test
    fun animated_windowed_default() = benchmark(
        implementation = "animated",
        workload = "windowed",
        profile = "default",
    )

    @Test
    fun column_windowed_default() = benchmark(
        implementation = "column",
        workload = "windowed",
        profile = "default",
    )

    @Test
    fun lazy_windowed_default() = benchmark(
        implementation = "lazy",
        workload = "windowed",
        profile = "default",
    )

    @Test
    fun animated_windowed_fast() = benchmark(
        implementation = "animated",
        workload = "windowed",
        profile = "fast",
    )

    @Test
    fun column_windowed_fast() = benchmark(
        implementation = "column",
        workload = "windowed",
        profile = "fast",
    )

    @Test
    fun lazy_windowed_fast() = benchmark(
        implementation = "lazy",
        workload = "windowed",
        profile = "fast",
    )

    private fun benchmark(
        implementation: String,
        workload: String,
        profile: String,
    ) {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            iterations = 3,
            compilationMode = CompilationMode.None(),
            setupBlock = {
                pressHome()
                killProcess()
            },
        ) {
            val launchIntent = Intent().apply {
                action = Intent.ACTION_MAIN
                setClassName(TARGET_PACKAGE, BENCHMARK_ACTIVITY)
                putExtra(EXTRA_IMPLEMENTATION, implementation)
                putExtra(EXTRA_WORKLOAD, workload)
                putExtra(EXTRA_PROFILE, profile)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            device.executeShellCommand(launchIntent.toUri(Intent.URI_INTENT_SCHEME).toAmStartCommand())
            device.wait(Until.hasObject(By.text(BENCHMARK_READY_TEXT)), 10_000)
            device.findObject(By.text(BENCHMARK_READY_TEXT))?.click()
            device.wait(Until.hasObject(By.text(BENCHMARK_DONE_TEXT)), 30_000)
        }
    }
}

private fun String.toAmStartCommand(): String = "am start -W $this"

private const val TARGET_PACKAGE = "com.rend1x.composeanimatedlist.sample"
private const val BENCHMARK_ACTIVITY = "com.rend1x.composeanimatedlist.sample.MainActivity"
private const val BENCHMARK_DONE_TEXT = "Benchmark complete"
private const val BENCHMARK_READY_TEXT = "Benchmark ready"
private const val EXTRA_IMPLEMENTATION = "implementation"
private const val EXTRA_WORKLOAD = "workload"
private const val EXTRA_PROFILE = "profile"
