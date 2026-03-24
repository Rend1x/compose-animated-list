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

