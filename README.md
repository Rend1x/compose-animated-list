# compose-animated-list

Diff-driven animated column for Jetpack Compose: each item exposes an explicit **lifecycle** ([`ItemPhase`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/ItemPhase.kt)) and **progress** ([`AnimatedItemScope`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedItemScope.kt)) so you can build custom transitions without guessing enter/exit flags.

## Usage

```kotlin
AnimatedColumn(
    items = items,
    key = { it.id },
    transitionSpec = AnimatedItemDefaults.fadeSlide(),
) { item ->
    Card(
        modifier = Modifier.graphicsLayer {
            alpha = visibilityProgress
            translationY = (1f - visibilityProgress) * 24f
        }
    ) {
        Text(item.title)
    }

    if (phase == ItemPhase.Exiting) {
        Text("Removing…")
    }
}
```

### Item lifecycle

Each item’s content lambda receives [`AnimatedItemScope`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedItemScope.kt):

| API | Meaning |
|-----|--------|
| **`phase`** | [`ItemPhase`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/ItemPhase.kt): **Entering**, **Visible**, or **Exiting** — mutually exclusive; one key moves **Entering → Visible → Exiting**. |
| **`visibilityProgress`** | `0f..1f` for fade and slide only (not row height). **Entering:** `0 → 1`. **Visible:** `1`. **Exiting:** `1 → 0`. Use this for inner content effects (icons, text, secondary motion). |
| **`placementProgress`** | `0f..1f` for the row’s **layout height** when placement is animated; otherwise `1f`. Mirrors height expand/collapse from [`PlacementBehavior.Animated`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/PlacementBehavior.kt). |
| **`progress`** | `min(visibilityProgress, placementProgress)` — conservative “overall” completion: advances only as fast as the slower of visibility vs height. Same semantics as the previous single `progress` field. |

This split keeps the contract **stable** if you need independent fade/slide vs height behavior; use **`progress`** when one combined value is enough.

### Behavior guarantees

These rules are what the implementation and tests commit to—use them when reasoning about production behavior under load or overlapping updates.

| Topic | Guarantee |
|--------|-----------|
| **Exiting retention** | If a key disappears from `items`, that row stays in the internal render list with phase **Exiting** until its exit animation finishes and the row is removed, or until you call [`AnimatedListState.clearExitingNow()`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/state/AnimatedListState.kt). Exiting rows keep the **relative order** they had among themselves and stable neighbors (order is derived from the previous render list). |
| **Reinsertion** | If a key comes back in `items` while its row was still **Exiting**, that row becomes **Visible** with the **latest** element value. It does **not** go through **Entering** again—only keys that were not in the previous render snapshot enter as **Entering**. |
| **Zero-duration transitions** | [`EnterSpec.None`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/EnterSpec.kt) / [`ExitSpec.None`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/ExitSpec.kt) use visibility tween duration `0`. For [`Fade`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/EnterSpec.kt) / [`SlideVertical`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/EnterSpec.kt) / [`FadeAndSlide`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/EnterSpec.kt), setting `durationMillis = 0` means the same: tweens complete immediately in the same coroutine work, then exit completion runs. For [`PlacementBehavior.Animated`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/animation/PlacementBehavior.kt), `durationMillis = 0` collapses height animation the same way. After **Visible**, a short internal “present settle” (120 ms) may still run to align alpha, offset, and height—see [`AnimatedColumn`](animatedlist/src/main/java/com/rend1x/composeanimatedlist/AnimatedColumn.kt). |
| **Update ordering** | Each time `items` or `key` changes, the list diffs **from the previous internal render snapshot** to the new `items`. Rapid updates are applied in **composition order**: the `LaunchedEffect` tied to `items` restarts, so intermediate `items` values may be skipped if the composition never commits them, but every applied step is a well-formed diff. The final render state matches the last applied `items` snapshot. |
| **Keys / duplicates** | `key` must return a **unique** value for every element in `items` for that snapshot. **Debug** builds of the `animatedlist` AAR validate this and throw `IllegalStateException` with the conflicting indices. **Release** builds do not throw: duplicates are normalized by keeping the **last** occurrence of each key (order among surviving items follows that rule). Fix callers so debug and release agree. |

### Modifier helper

Optional preset that maps **`visibilityProgress`** to a simple fade + slide (useful with `AnimatedItemDefaults.none()` if you want the list to only handle diffing and height):

```kotlin
Card(modifier = Modifier.animatedItem(this)) { … }
```

### Transition presets

```kotlin
AnimatedItemDefaults.fade()
AnimatedItemDefaults.slide()
AnimatedItemDefaults.fadeSlide() // default for AnimatedColumn
AnimatedItemDefaults.none()
```

## Modules

- **`animatedlist`** — library
- **`sample`** — playground (transition tuning, tag chips, list demos; shows phase + split progress while animating)

