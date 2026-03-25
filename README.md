# compose-animated-list

Diff-driven animated column for Jetpack Compose. **Default path:** apply row visuals with [`Modifier.animatedItem`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedItemModifier.kt) and keep the column on [`AnimatedItemDefaults.none()`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/AnimatedItemTransitionSpec.kt) so motion is not applied twice. **Advanced path:** use [`ItemPhase`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/ItemPhase.kt) and progress on [`AnimatedItemScope`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedItemScope.kt) for custom graphics tied to the column’s own transition spec.

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
- **`animatedlist`** — Compose UI adapter over `animatedlist-core`
- **`sample`** — easy path (`none` + `animatedItem`) vs advanced (tunable `transitionSpec` + phase/progress)

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

