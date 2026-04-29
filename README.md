# compose-animated-list

Diff-driven animated column for Jetpack Compose. 
**Default path:** apply row visuals with [`Modifier.animatedItem`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedItemModifier.kt) and keep the column on [`AnimatedItemDefaults.none()`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/AnimatedItemTransitionSpec.kt) so motion is not applied twice. 
**Advanced path:** use [`ItemPhase`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/ItemPhase.kt) and progress on [`AnimatedItemScope`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedItemScope.kt) for custom graphics tied to the column’s own transition spec.

## Feature Demos

Short recordings from the sample app show the main cases the library is built for: basic list changes, side-by-side comparison, stable-key reorder, heavier update pressure, animation presets, and edge cases.

| Basic insert/remove | Animated vs plain |
|--------|--------|
| <video src="docs/demo/basic.webm" controls muted loop playsinline width="260"></video> | <video src="docs/demo/compare.webm" controls muted loop playsinline width="260"></video> |

| Stable-key shuffle | Many updates |
|--------|--------|
| <video src="docs/demo/shuffle.webm" controls muted loop playsinline width="260"></video> | <video src="docs/demo/load.webm" controls muted loop playsinline width="260"></video> |

| Animation variants | Edge cases |
|--------|--------|
| <video src="docs/demo/variants.webm" controls muted loop playsinline width="260"></video> | <video src="docs/demo/edges.webm" controls muted loop playsinline width="260"></video> |

## Usage (modifier-first, recommended)

[`AnimatedColumn`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedColumn.kt) still runs diffing and lifecycle; [`AnimatedItemDefaults.none()`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/AnimatedItemTransitionSpec.kt) turns off **shell** fade/slide/height so **your** modifier owns how the row appears. If you used a preset such as [`fadeSlide()`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/AnimatedItemTransitionSpec.kt) here **and** [`animatedItem`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedItemModifier.kt) on the same surface, both would drive opacity/offset and the animation would look doubled.

```kotlin
import androidx.compose.ui.unit.dp
import com.rend1x.composeanimatedlist.animatedItem

AnimatedColumn(
    items = items,
    key = { it.id },
    transitionSpec = AnimatedItemDefaults.none(),
) { item ->
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animatedItem(this, slideRevealOffset = 24.dp),
    ) {
        Text(item.title)
    }

    if (phase == ItemPhase.Exiting) {
        Text("Removing…")
    }
}
```

### API layers (quick reference)

| Layer | Use when |
|--------|-----------|
| **`Modifier.animatedItem(scope, …)`** | Normal rows: fade + slide from [`visibilityProgress`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedItemScope.kt) without hand-writing [`graphicsLayer`](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).graphicsLayer(kotlin.Function1)). Pair with **`AnimatedItemDefaults.none()`** on the column. |
| **`phase`** | Lifecycle copy, icons, conditional UI ([`ItemPhase`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/ItemPhase.kt)). |
| **`visibilityProgress` / `placementProgress` / `progress`** | Custom motion, or aligning your own [`graphicsLayer`](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).graphicsLayer(kotlin.Function1)) with the column’s **non-none** [`AnimatedItemTransitionSpec`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/AnimatedItemTransitionSpec.kt). |

### Item lifecycle

Each item’s content lambda receives [`AnimatedItemScope`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedItemScope.kt). You do **not** need the progress fields for the default fade + slide; use them for advanced customization.

| API | Meaning |
|-----|--------|
| **`phase`** | [`ItemPhase`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/ItemPhase.kt) — **primary** lifecycle: **Entering**, **Visible**, or **Exiting** (mutually exclusive). |
| **`visibilityProgress`** | `0f..1f`: fade/slide completion from enter/exit specs (**not** row clip height). **Entering:** toward `1`. **Exiting:** toward `0`. **Visible:** `1` when settled; may be `< 1` briefly during present-settle after enter (see [`AnimatedItemScope`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedItemScope.kt) KDoc). |
| **`placementProgress`** | `0f..1f` for the row’s **layout height** when placement is animated; otherwise `1f`. Same present-settle note as visibility when applicable. |
| **`progress`** | `min(visibilityProgress, placementProgress)` — use when one combined completion value is enough; prefer the split fields when fade/slide and height should diverge. |

See [`AnimatedItemScope`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedItemScope.kt) KDoc for the authoritative contract.

### Column-driven transitions (optional)

If you prefer the list to own enter/exit motion, pass a non-`none` spec (e.g. [`AnimatedItemDefaults.fadeSlide()`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/AnimatedItemTransitionSpec.kt)) and **omit** [`animatedItem`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedItemModifier.kt) on the same surface, or use [`phase`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/ItemPhase.kt) / progress only for extra effects.

### Behavior guarantees

These rules are what the implementation and tests commit to—use them when reasoning about production behavior under load or overlapping updates.

| Topic | Guarantee |
|--------|-----------|
| **Exiting retention** | If a key disappears from `items`, that row stays in the internal render list with phase **Exiting** until its exit animation finishes and the row is removed, or until you call [`AnimatedListState.clearExitingNow()`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/state/AnimatedListState.kt). Exiting rows keep the **relative order** they had among themselves and stable neighbors (order is derived from the previous render list). |
| **Reinsertion** | If a key comes back in `items` while its row was still **Exiting**, that row becomes **Visible** with the **latest** element value. It does **not** go through **Entering** again—only keys that were not in the previous render snapshot enter as **Entering**. |
| **Zero-duration transitions** | [`EnterSpec.None`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/EnterSpec.kt) / [`ExitSpec.None`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/ExitSpec.kt) use visibility tween duration `0`. For [`Fade`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/EnterSpec.kt) / [`SlideVertical`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/EnterSpec.kt) / [`FadeAndSlide`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/EnterSpec.kt), setting `durationMillis = 0` means the same: tweens complete immediately in the same coroutine work, then exit completion runs. For [`PlacementBehavior.Animated`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/PlacementBehavior.kt), `durationMillis = 0` collapses height animation the same way. After **Visible**, a short internal “present settle” (120 ms) may still run to align alpha, offset, and height—see [`AnimatedColumn`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedColumn.kt). |
| **Update ordering** | Each time `items` or `key` changes, the list diffs **from the previous internal render snapshot** to the new `items`. Rapid updates are applied in **composition order**: the `LaunchedEffect` tied to `items` restarts, so intermediate `items` values may be skipped if the composition never commits them, but every applied step is a well-formed diff. The final render state matches the last applied `items` snapshot. |
| **Keys / duplicates** | `key` must return a **unique** value for every element in `items` for that snapshot. **Debug** builds of the `animatedlist` AAR validate this and throw `IllegalStateException` with the conflicting indices. **Release** builds do not throw: duplicates are normalized by keeping the **last** occurrence of each key; sanitized order is **increasing index of that last occurrence** (see **Behavior contract**). Fix callers so debug and release agree. |

### Animation interruption semantics

Rules for rapid `items` updates and `transitionSpec` changes while row shell animations are in flight. Stated here and in KDoc on [`AnimatedColumn`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedColumn.kt) / [`AnimatedItemScope`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedItemScope.kt). **Core:** `AnimatedListRenderEngineContractTest` (phase/value snapshots). **Adapter:** `AnimatedColumnInterruptionInstrumentedTest` (phases under a frozen main clock, spec swap, and multi-frame rapid updates).

| Rule | Meaning |
|------|--------|
| **Latest state wins** | After each applied update, each row’s **phase** and **element value** match the diff of the **last committed** `items` snapshot. Snapshots that never commit may be skipped. |
| **Continuity from current values** | When a shell animation is interrupted (presence changes, or `transitionSpec` restarts the effect), the next animation runs from the **current** animated alpha, translation, and height progress—**not** from enter “initial” values. Targets come from the **new** step: enter toward on-screen rest, exit toward off-screen rest, visible row toward rest after present-settle. |
| **Reinsert during Exiting** | The row becomes **Visible** with the latest value (**not** Entering again). The shell does **not** reset to enter-start; it moves from **current** values toward visible rest (present-settle). |
| **Remove during Entering** | The row becomes **Exiting**. The shell animates from **current** values toward exit targets. There is **no** guarantee of a literal **reverse** of the enter curve—only continuity **toward** the exit end state. |
| **Changing `transitionSpec` mid-animation** | The animation effect restarts; tweens begin from **current** values and run to targets implied by the new spec and current phase. |

### Transition presets

```kotlin
AnimatedItemDefaults.none()      // pair with Modifier.animatedItem for modifier-first visuals
AnimatedItemDefaults.fade()
AnimatedItemDefaults.slide()
AnimatedItemDefaults.fadeSlide() // column-owned motion; avoid stacking animatedItem on the same surface
```

[`AnimatedColumn`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedColumn.kt) still defaults to [`fadeSlide()`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/AnimatedItemTransitionSpec.kt) when you omit `transitionSpec`; override with `none()` for the modifier-first quick start above.

## Modules

- **`animatedlist-core`** — JVM-only diff/render engine (`AnimatedListRenderEngine`, `AnimatedListDiffer`); no Compose
- **`animatedlist`** — Compose UI adapter over `animatedlist-core` (minSdk **23**; current Compose Material and merged AndroidTest manifests require it)
- **`sample`** — easy path (`none` + `animatedItem`) vs advanced (tunable `transitionSpec` + phase/progress)

## Dependency surface

The library module keeps its published dependency surface small and stable-only:

- **API dependencies:** `androidx.compose.foundation:foundation`, `androidx.compose.runtime:runtime`, and `androidx.compose.ui:ui`. These are exposed because public APIs use `@Composable`, `Modifier`, `Alignment`, `Dp`, and `Column`/layout types.
- **Implementation dependencies:** `animatedlist-core` and `androidx.compose.animation:animation`. Consumers get them transitively when depending on `animatedlist`, but they are not part of the public API contract.
- **BOM-managed Compose versions:** Compose artifacts are versioned by `androidx.compose:compose-bom`; individual Compose module versions are intentionally not repeated.
- **Debug-only preview/tooling:** Compose Material 2, `ui-tooling-preview`, `ui-tooling`, and `ui-test-manifest` are only used by the library debug/preview source set and are not release dependencies.
- **Sample-only dependencies:** `androidx.activity:activity-compose`, `androidx.appcompat:appcompat`, `androidx.core:core-ktx`, Compose Material 3, Material icons, Espresso, AndroidX Test JUnit, and Macrobenchmark/UIAutomator are only needed by the sample, android tests, or macrobenchmark module.

Main transitive dependencies you will see from Compose include `animation-core`, `foundation-layout`, `ui-graphics`, `ui-text`, `ui-unit`, Kotlin stdlib, and AndroidX support libraries such as annotation, collection, lifecycle, savedstate, and tracing. Do not add these manually unless your app uses their APIs directly.

## Benchmarks

Use the core benchmark runner when you want to estimate the raw overhead of the diff/render engine before layering Compose animation and drawing on top:

```bash
./gradlew :animatedlist-core:runBenchmarks
./gradlew :animatedlist-core:runBenchmarks --args="--profile=full"
```

What it measures:

- **`rebuild-present`** — rebuild a plain present-only render snapshot for each update
- **`diff+clear`** — run [`AnimatedListDiffer.diff`](animatedlist-core/src/main/kotlin/com/rend1x/composeanimatedlist/core/AnimatedListDiffer.kt) and immediately clear exiting rows
- **`engine.update+clear`** — run [`AnimatedListRenderEngine.update`](animatedlist-core/src/main/kotlin/com/rend1x/composeanimatedlist/core/AnimatedListRenderEngine.kt) with the same immediate-clear policy

The workloads cover fixed-size window churn, random insert/remove/update churn, reinsert bursts on hot keys, and pure value updates across stable keys.

The report includes:

- **Cost metrics** — median and p90 **nanoseconds per update** plus a ratio against the plain rebuild baseline
- **Input churn metrics** — insert/remove/position-change/value-change counts and average touched keys per update
- **Animation-value metrics** — exit events, updates that keep exiting rows alive, reinsert recoveries without re-enter, and render-size amplification from retained exits

These numbers are intentionally **engine-only**. They answer “how expensive is the diff/lifecycle logic itself?” If you want to measure scroll smoothness, frame time, or jank in the sample/app process, add a separate Android macrobenchmark layer on top.

### UI comparison baseline

There is also an on-device comparison suite in [`sample/src/androidTest`](sample/src/androidTest/java/com/rend1x/composeanimatedlist/sample/ListAnimationComparisonBenchmark.kt) for comparing:

- **`AnimatedColumn`** with the recommended `none() + animatedItem(...)` path
- **`Column`** with the closest built-in baseline: `animateContentSize()`
- **`LazyColumn`** with the built-in item animation path: `animateItem()`

Use this when the question is not just “what does the core engine cost?” but also “how does this compare to the default Compose list animation building blocks on-device?” The suite measures `update -> waitForIdle()` timings and logs median/p90 results for each implementation on the device or emulator.

The `Column` baseline is intentionally not apples-to-apples: Compose does not provide a built-in diff-driven keyed item animation primitive for plain `Column`, so `animateContentSize()` is the nearest default baseline rather than a full feature match.

### Macrobenchmark (frame metrics)

For frame-time and jank-tail analysis on a real device, run macrobenchmarks:

```bash
ANDROID_SERIAL=<device-id> ./gradlew :macrobenchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rend1x.composeanimatedlist.macrobenchmark.ListAnimationMacrobenchmark
```

Current macro suite configuration:

- `FrameTimingMetric()` only (focus on frame behavior during churn scenarios)
- `CompilationMode.None()`
- `repeatIterations = 7`
- deterministic scenario launch via benchmark extras (`implementation`, `workload`, `profile`)

Results are exported to:

- `macrobenchmark/build/outputs/connected_android_test_additional_output/benchmark/connected/.../com.rend1x.composeanimatedlist.macrobenchmark-benchmarkData.json`
- `macrobenchmark/build/outputs/connected_android_test_additional_output/benchmark/connected/.../additionaltestoutput.benchmark.message_*.txt`

### Benchmark interpretation (important)

This project should be evaluated as a **specialized semantic tool**, not as a universal “fastest list animation” replacement.

- Core runner answers: “what does diff/lifecycle logic cost?”
- UI comparison runner answers: “what does `update -> waitForIdle()` cost versus default Compose paths?”
- Macrobenchmark answers: “what frame tails (`P90/P95/P99`) do users pay under churn?”

Use all three layers together. For this library, semantic metrics such as reinsert recovery and controlled exit retention are first-class outcomes and should be interpreted alongside frame tails.

### Latest on-device snapshot (Pixel 7, API 36, `repeatIterations = 7`)

`P90 frameDurationCpuMs / P90 frameOverrunMs`:

| Scenario | AnimatedColumn | Column baseline | LazyColumn baseline |
|--------|--------|--------|--------|
| `reinsert / fast` | `110.8 / 110.4` | `65.1 / 81.9` | `85.7 / 103.7` |
| `reinsert / default` | `135.4 / 121.9` | `94.1 / 107.8` | `74.7 / 85.4` |
| `windowed / fast` | `109.5 / 97.4` | `80.9 / 91.7` | `78.5 / 95.1` |
| `windowed / default` | `85.6 / 96.3` | `81.2 / 78.7` | `102.4 / 87.6` |

Notes:

- Median (`P50`) frame duration is close across implementations (roughly `4-5 ms`); differences are mostly in tail latency.
- `cpuLocked=false` in these runs, so treat absolute values as comparative, not lab-grade.
- Practical takeaway: the library is strongest when semantic guarantees matter (reinsert continuity, controlled exiting lifecycle), not when optimizing for minimum tail latency at all costs.

### Behavior contract (`animatedlist-core`)

These are the engine-level guarantees enforced by `AnimatedListRenderEngine` / `AnimatedListDiffer` and covered by `AnimatedListRenderEngineContractTest`:

| Topic | Contract |
|--------|-----------|
| **Idempotent updates** | Two consecutive `update` calls with the same list and key selector leave the render snapshot unchanged (including rows still **Entering** if their value is unchanged). |
| **New key** | A key that appears in the new input but not in the previous snapshot is inserted as **Entering**. |
| **Removed key** | A key that disappears from the new input becomes **Exiting** and stays in the snapshot until `clearExitingNow()` or `onExitAnimationFinished(key)`. |
| **Reinsert while exiting** | If a key is **Exiting** and appears again in the new input, the row becomes **Present** with the latest value; it does **not** become **Entering** again. |
| **Order** | Relative order of retained rows follows the previous snapshot; new keys are placed deterministically from the new list’s ordering (see KDoc on `AnimatedListDiffer.diff`). |
| **clearExitingNow** | Removes every **Exiting** row and leaves **Present** and **Entering** rows unchanged. |
| **Strict keys** | Duplicate keys in one input snapshot throw `IllegalStateException`. |
| **Last-wins keys** | Duplicates collapse to the **last** occurrence per key; sanitized order is by **increasing index of that last occurrence** in the original list. |

Compose timing (`LaunchedEffect`, animation frames) is layered on top; see **Behavior guarantees** above for UI-facing rules.
